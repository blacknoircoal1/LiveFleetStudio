package com.livefleet.studio.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import android.util.Log
import com.livefleet.studio.model.BikeData
import com.livefleet.studio.model.BrandType
import com.livefleet.studio.model.Concept2ErgFilter
import com.livefleet.studio.parser.Concept2PacketParser
import com.livefleet.studio.parser.FtmsPacketParser
import com.livefleet.studio.util.AppDiagnosticLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Layer 3 — Serialized Unified BLE GATT Client (Max [MAX_CONCURRENT_CONNECTIONS] = 6):
 *  - Strictly serializes `connectGatt(..., TRANSPORT_LE)` one peripheral at a time
 *    (connect -> onServicesDiscovered -> machine-type read -> CCCD writes -> next queued device)
 *    to prevent Android GATT_ERROR (133).
 *  - Prioritizes whitelisted Studio Bike # machines first, then strongest RSSI.
 *  - Supports Concept2 PM5 (CE060030 + CE060016 subtype), FTMS (0x1826 -> 0x2AD2 / 0x2AD1),
 *    and Cycling Power (0x1818 -> 0x2A63).
 */
class UniversalGattManager(private val context: Context) {

    private val _bikeDataFlow = MutableSharedFlow<BikeData>(extraBufferCapacity = 64)
    val bikeDataFlow: SharedFlow<BikeData> = _bikeDataFlow.asSharedFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val packetCounter = AtomicInteger(0)

    private sealed class PendingGattOp {
        data class ReadChar(val char: BluetoothGattCharacteristic) : PendingGattOp()
        data class EnableNotify(val char: BluetoothGattCharacteristic) : PendingGattOp()
        data class WriteChar(val char: BluetoothGattCharacteristic, val data: ByteArray) : PendingGattOp()
    }

    private data class QueuedGattRequest(
        val address: String,
        val device: BluetoothDevice,
        val advertisedName: String,
        val rssi: Int,
        val isWhitelisted: Boolean,
        val meta: FtmsPacketParser.DiscoveredGattMeta
    )

    private data class GattSession(
        val macAddress: String,
        val gatt: BluetoothGatt,
        var bikeData: BikeData,
        val isPrimaryConsole: Boolean = true,
        var prevCrankRevs: Int? = null,
        var prevCrankEventTime: Int? = null,
        val pendingOps: ArrayDeque<PendingGattOp> = ArrayDeque(),
        var isSetupComplete: Boolean = false,
        var setupCompletedAtMs: Long = 0L,
        var hasSeenNonZeroTelemetry: Boolean = false,
        var lastActivityTimestampMs: Long = System.currentTimeMillis()
    )

    private val queueLock = Any()
    private val pendingQueue = mutableListOf<QueuedGattRequest>()
    private val sessions = ConcurrentHashMap<String, GattSession>()
    private val unsupportedAddresses = ConcurrentHashMap.newKeySet<String>()
    private val cooldownUntilMs = ConcurrentHashMap<String, Long>()
    private val lastProbedTimestampMs = ConcurrentHashMap<String, Long>()
    private val knownConcept2Subtypes = ConcurrentHashMap<String, String>()

    @Volatile
    var concept2ErgFilter: Concept2ErgFilter = Concept2ErgFilter.ALL
        private set

    @Volatile
    private var hasStagesPrimaryConsole: Boolean = false

    init {
        startSlotRecyclerLoop()
    }

    @Volatile
    private var inFlightAddress: String? = null
    private var inFlightTimeoutJob: Job? = null

    fun connectedCount(): Int = sessions.size

    fun drainPacketCount(): Int = packetCounter.getAndSet(0)

    fun resetPacketCount() {
        packetCounter.set(0)
    }

    fun setConcept2ErgFilter(filter: Concept2ErgFilter) {
        concept2ErgFilter = filter
        if (filter == Concept2ErgFilter.ALL) return

        synchronized(queueLock) {
            pendingQueue.removeAll { req ->
                if (req.meta.brandType != BrandType.CONCEPT2) {
                    false
                } else {
                    val eq = req.meta.equipmentType.ifEmpty { knownConcept2Subtypes[req.address].orEmpty() }
                    eq.isNotEmpty() && !LiveFleetBleScanner.matchesConcept2Filter(eq, filter)
                }
            }
        }

        sessions.values.toList().forEach { session ->
            if (session.bikeData.brandType == BrandType.CONCEPT2) {
                val eq = session.bikeData.equipmentType.ifEmpty {
                    knownConcept2Subtypes[session.macAddress].orEmpty()
                }
                if (eq.isNotEmpty() && !LiveFleetBleScanner.matchesConcept2Filter(eq, filter)) {
                    AppDiagnosticLogger.log(
                        "GATT",
                        "Releasing Concept2 ${session.macAddress} ($eq) — does not match filter=${filter.label}"
                    )
                    disconnectDevice(session.macAddress)
                }
            }
        }
    }

    fun enqueueConnectionIfNeeded(discovered: DiscoveredGattDevice) {
        val address = discovered.device.address ?: return
        if (discovered.meta.brandType == BrandType.KEISER) return
        if (unsupportedAddresses.contains(address)) return
        if (System.currentTimeMillis() < (cooldownUntilMs[address] ?: 0L)) return

        if (discovered.meta.brandType == BrandType.CONCEPT2) {
            val knownEq = discovered.meta.equipmentType.ifEmpty { knownConcept2Subtypes[address].orEmpty() }
            if (knownEq.isNotEmpty() && !LiveFleetBleScanner.matchesConcept2Filter(knownEq, concept2ErgFilter)) {
                return
            }
        }

        // Stages SC3 bikes advertise both their SIC2 console (#8) and 5-digit ANT+ crank (55617).
        // Suppress 5-digit Stages crank sensors whenever primary Stages bike-number consoles are present.
        if (discovered.meta.brandType == BrandType.STAGES) {
            if (discovered.meta.isPrimaryConsole) {
                hasStagesPrimaryConsole = true
                sessions.values.filter { it.bikeData.brandType == BrandType.STAGES && !it.isPrimaryConsole }
                    .forEach { disconnectDevice(it.macAddress) }
            } else if (hasStagesPrimaryConsole) {
                return
            }
        }

        synchronized(queueLock) {
            if (sessions.containsKey(address) || inFlightAddress == address) {
                return
            }
            if (discovered.meta.brandType == BrandType.STAGES && discovered.meta.isPrimaryConsole) {
                pendingQueue.removeAll { it.meta.brandType == BrandType.STAGES && !it.meta.isPrimaryConsole }
            }

            val existingIdx = pendingQueue.indexOfFirst { it.address == address }
            val request = QueuedGattRequest(
                address = address,
                device = discovered.device,
                advertisedName = discovered.advertisedName,
                rssi = discovered.rssi,
                isWhitelisted = discovered.isWhitelisted,
                meta = discovered.meta
            )
            if (existingIdx >= 0) {
                pendingQueue[existingIdx] = request
            } else {
                pendingQueue.add(request)
            }

            // Prioritize: whitelisted -> primary console -> least-recently-probed -> strongest RSSI
            pendingQueue.sortWith(
                compareByDescending<QueuedGattRequest> { it.isWhitelisted }
                    .thenByDescending { it.meta.isPrimaryConsole }
                    .thenBy { lastProbedTimestampMs[it.address] ?: 0L }
                    .thenByDescending { it.rssi }
            )
        }

        processNextInQueue()
    }

    @SuppressLint("MissingPermission")
    private fun processNextInQueue() {
        val nextReq: QueuedGattRequest = synchronized(queueLock) {
            if (inFlightAddress != null) return
            if (sessions.size >= MAX_CONCURRENT_CONNECTIONS) {
                pendingQueue.clear()
                return
            }
            while (pendingQueue.isNotEmpty()) {
                val candidate = pendingQueue.removeAt(0)
                if (!sessions.containsKey(candidate.address)) {
                    inFlightAddress = candidate.address
                    return@synchronized candidate
                }
            }
            return
        }

        val name = nextReq.advertisedName.ifEmpty { nextReq.device.name ?: nextReq.meta.brand }
        val formattedDisplayId = if (nextReq.meta.displayId.length >= 5) {
            nextReq.meta.displayId
        } else {
            "#${nextReq.meta.displayId}"
        }
        val initialBike = BikeData(
            bikeId = nextReq.meta.stableBikeId,
            displayId = formattedDisplayId,
            brand = nextReq.meta.brand,
            brandType = nextReq.meta.brandType,
            macAddress = nextReq.address,
            rssi = nextReq.rssi,
            isWhitelisted = nextReq.isWhitelisted,
            equipmentType = nextReq.meta.equipmentType,
            riderName = "${nextReq.meta.brand} $formattedDisplayId",
            powerWatts = 0,
            cadenceRpm = 0f,
            calories = 0,
            distanceMeters = 0,
            lastSeenTimestamp = System.currentTimeMillis(),
            isStale = false
        )

        Log.i(
            TAG,
            "Serialized GATT connecting [${nextReq.meta.brand}]: $name (${nextReq.address}, RSSI=${nextReq.rssi}, wl=${nextReq.isWhitelisted})"
        )
        AppDiagnosticLogger.log(
            "GATT",
            "Connecting [${nextReq.meta.brand}] name='$name' mac=${nextReq.address} id=$formattedDisplayId rssi=${nextReq.rssi} wl=${nextReq.isWhitelisted}"
        )

        startInFlightTimeout(nextReq.address)
        lastProbedTimestampMs[nextReq.address] = System.currentTimeMillis()

        val callback = createGattCallback(nextReq.address, initialBike)
        try {
            val gatt = nextReq.device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
            if (gatt == null) {
                finishInFlightSetup(nextReq.address)
            } else {
                sessions[nextReq.address] = GattSession(
                    macAddress = nextReq.address,
                    gatt = gatt,
                    bikeData = initialBike,
                    isPrimaryConsole = nextReq.meta.isPrimaryConsole
                )
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing BLUETOOTH_CONNECT permission for ${nextReq.address}: ${e.message}")
            finishInFlightSetup(nextReq.address)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open GATT to ${nextReq.address}: ${e.message}")
            finishInFlightSetup(nextReq.address)
        }
    }

    private fun startInFlightTimeout(address: String) {
        inFlightTimeoutJob?.cancel()
        inFlightTimeoutJob = scope.launch {
            delay(GATT_SETUP_TIMEOUT_MS)
            val stillInFlight = synchronized(queueLock) { inFlightAddress == address }
            if (stillInFlight) {
                Log.w(TAG, "Serialized GATT setup timeout for $address — advancing queue")
                val session = sessions[address]
                if (session != null && !session.isSetupComplete) {
                    cleanupSession(address, session.gatt)
                }
                finishInFlightSetup(address)
            }
        }
    }

    private fun finishInFlightSetup(address: String) {
        val shouldAdvance = synchronized(queueLock) {
            if (inFlightAddress == address) {
                inFlightAddress = null
                inFlightTimeoutJob?.cancel()
                inFlightTimeoutJob = null
                true
            } else {
                false
            }
        }
        if (shouldAdvance) {
            processNextInQueue()
        }
    }

    private fun createGattCallback(address: String, initialBike: BikeData): BluetoothGattCallback {
        return object : BluetoothGattCallback() {
            @SuppressLint("MissingPermission")
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Log.w(TAG, "GATT error ($status) on $address (${initialBike.brand}) — cleaning up")
                    AppDiagnosticLogger.warn("GATT", "GATT error status=$status mac=$address brand=${initialBike.brand} id=${initialBike.displayId}")
                    cooldownUntilMs[address] = System.currentTimeMillis() + GATT_ERROR_COOLDOWN_MS
                    cleanupSession(address, gatt)
                    finishInFlightSetup(address)
                    return
                }

                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        Log.i(TAG, "GATT connected: $address (${initialBike.brand} ${initialBike.displayId})")
                        AppDiagnosticLogger.log("GATT", "Connected mac=$address brand=${initialBike.brand} id=${initialBike.displayId}")
                        scope.launch {
                            delay(120)
                            if (!sessions.containsKey(address)) return@launch
                            try {
                                gatt.discoverServices()
                            } catch (e: Exception) {
                                Log.e(TAG, "discoverServices failed for $address: ${e.message}")
                                AppDiagnosticLogger.error("GATT", "discoverServices failed mac=$address", e)
                                cooldownUntilMs[address] = System.currentTimeMillis() + GATT_ERROR_COOLDOWN_MS
                                cleanupSession(address, gatt)
                                finishInFlightSetup(address)
                            }
                        }
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        Log.i(TAG, "GATT disconnected: $address (${initialBike.brand} ${initialBike.displayId})")
                        AppDiagnosticLogger.log("GATT", "Disconnected mac=$address brand=${initialBike.brand} id=${initialBike.displayId}")
                        cooldownUntilMs[address] = System.currentTimeMillis() + GATT_ERROR_COOLDOWN_MS
                        cleanupSession(address, gatt)
                        finishInFlightSetup(address)
                    }
                }
            }

            @SuppressLint("MissingPermission")
            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Log.w(TAG, "Service discovery failed ($status) for $address")
                    cleanupSession(address, gatt)
                    finishInFlightSetup(address)
                    return
                }

                val session = sessions[address] ?: run {
                    finishInFlightSetup(address)
                    return
                }

                val ops = mutableListOf<PendingGattOp>()
                val ftmsService = gatt.getService(FtmsPacketParser.UUID_FTMS_SERVICE)
                val indoorBikeChar = ftmsService?.getCharacteristic(FtmsPacketParser.UUID_INDOOR_BIKE_DATA)
                val rowerDataChar = ftmsService?.getCharacteristic(FtmsPacketParser.UUID_ROWER_DATA)

                // Priority 1: Concept2 PM5 Rowing Service (CE060030)
                val pm5Service = gatt.getService(Concept2PacketParser.UUID_ROWING_SERVICE)
                if (pm5Service != null || session.bikeData.brandType == BrandType.CONCEPT2) {
                    // Concept2 Machine-Type Verification:
                    // Queue GATT read of CE060016 first so SkiErg (128) / RowErg (0..15) / BikeErg (192)
                    // is confirmed authoritatively by hardware rather than guessing from 0x2AD1.
                    val c2InfoService = gatt.getService(Concept2PacketParser.UUID_DEVICE_INFO_SERVICE)
                    val ergTypeChar = c2InfoService?.getCharacteristic(Concept2PacketParser.UUID_ERG_MACHINE_TYPE)
                    if (ergTypeChar != null) {
                        ops.add(PendingGattOp.ReadChar(ergTypeChar))
                    }

                    if (pm5Service != null) {
                        listOf(
                            Concept2PacketParser.UUID_ADDITIONAL_STATUS_1,
                            Concept2PacketParser.UUID_ADDITIONAL_STATUS_2,
                            Concept2PacketParser.UUID_GENERAL_STATUS
                        ).forEach { uuid ->
                            pm5Service.getCharacteristic(uuid)?.let { ops.add(PendingGattOp.EnableNotify(it)) }
                        }
                    } else {
                        if (rowerDataChar != null) {
                            ops.add(PendingGattOp.EnableNotify(rowerDataChar))
                        } else if (indoorBikeChar != null) {
                            ops.add(PendingGattOp.EnableNotify(indoorBikeChar))
                        }
                    }
                } else {
                    // Priority 2: Bluetooth SIG FTMS (0x1826 -> 0x2AD2 Indoor Bike Data or 0x2AD1 Rower Data)
                    if (indoorBikeChar != null) {
                        ops.add(PendingGattOp.EnableNotify(indoorBikeChar))
                    } else if (rowerDataChar != null) {
                        ops.add(PendingGattOp.EnableNotify(rowerDataChar))
                    }

                    // Also subscribe to Cycling Power (0x1818 -> 0x2A63) and Cycling Speed & Cadence (0x1816 -> 0x2A5B)
                    // whenever present so Wattbike / studio bikes that omit cadence in 0x2AD2 still stream RPM.
                    val cpsService = gatt.getService(FtmsPacketParser.UUID_CYCLING_POWER_SERVICE)
                    val cpsChar = cpsService?.getCharacteristic(FtmsPacketParser.UUID_CYCLING_POWER_MEASUREMENT)
                    if (cpsChar != null) {
                        ops.add(PendingGattOp.EnableNotify(cpsChar))
                    }
                    val cscService = gatt.getService(FtmsPacketParser.UUID_CYCLING_SPEED_CADENCE_SERVICE)
                    val cscChar = cscService?.getCharacteristic(FtmsPacketParser.UUID_CSC_MEASUREMENT)
                    if (cscChar != null) {
                        ops.add(PendingGattOp.EnableNotify(cscChar))
                    }

                    // Life Fitness / FTMS Control Point (0x2AD9): request control (0x00) + start (0x07)
                    // when Indoor Bike Data is present so consoles begin streaming live telemetry.
                    if (indoorBikeChar != null) {
                        ftmsService.getCharacteristic(FtmsPacketParser.UUID_FTMS_CONTROL_POINT)?.let { ctrl ->
                            ops.add(PendingGattOp.WriteChar(ctrl, byteArrayOf(0x00)))
                            ops.add(PendingGattOp.WriteChar(ctrl, byteArrayOf(0x07)))
                        }
                    }
                }

                if (ops.isEmpty()) {
                    Log.w(TAG, "No supported fitness GATT characteristics found on $address (${session.bikeData.brand}) — blacklisting")
                    val svcList = gatt.services?.joinToString(",") { it.uuid.toString().take(8) }.orEmpty()
                    AppDiagnosticLogger.warn("GATT", "Unsupported GATT mac=$address brand=${session.bikeData.brand} services=[$svcList]")
                    unsupportedAddresses.add(address)
                    cleanupSession(address, gatt)
                    finishInFlightSetup(address)
                    return
                }
                val svcList = gatt.services?.joinToString(",") { it.uuid.toString().take(8) }.orEmpty()
                AppDiagnosticLogger.log("GATT", "Services discovered mac=$address brand=${session.bikeData.brand} ops=${ops.size} services=[$svcList]")

                synchronized(session.pendingOps) {
                    session.pendingOps.clear()
                    ops.forEach { session.pendingOps.addLast(it) }
                }

                // Wait for real non-zero telemetry before displaying a tile (prevents idle 0W ghost bikes)
                executeNextGattOp(session)
            }

            // Android 13+ (API 33+)
            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray,
                status: Int
            ) {
                handleErgMachineTypeRead(address, characteristic.uuid, value, status)
                sessions[address]?.let { executeNextGattOp(it) }
            }

            // Android 8-12 (API 26-32)
            @Deprecated("Deprecated in API 33")
            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    @Suppress("DEPRECATION")
                    val bytes = characteristic.value ?: ByteArray(0)
                    handleErgMachineTypeRead(address, characteristic.uuid, bytes, status)
                    sessions[address]?.let { executeNextGattOp(it) }
                }
            }

            @SuppressLint("MissingPermission")
            override fun onDescriptorWrite(
                gatt: BluetoothGatt,
                descriptor: BluetoothGattDescriptor,
                status: Int
            ) {
                val session = sessions[address] ?: run {
                    finishInFlightSetup(address)
                    return
                }
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Log.w(TAG, "Descriptor write failed ($status) on $address for ${descriptor.characteristic.uuid}")
                }
                executeNextGattOp(session)
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                sessions[address]?.let { executeNextGattOp(it) }
            }

            // Android 13+ (API 33+)
            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray
            ) {
                handleCharacteristicPayload(address, characteristic.uuid, value)
            }

            // Android 8-12 (API 26-32)
            @Deprecated("Deprecated in API 33")
            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
            ) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    @Suppress("DEPRECATION")
                    val bytes = characteristic.value ?: return
                    handleCharacteristicPayload(address, characteristic.uuid, bytes)
                }
            }
        }
    }

    private fun handleErgMachineTypeRead(
        address: String,
        uuid: UUID,
        value: ByteArray,
        status: Int
    ) {
        if (uuid != Concept2PacketParser.UUID_ERG_MACHINE_TYPE || status != BluetoothGatt.GATT_SUCCESS) {
            return
        }
        val session = sessions[address] ?: return
        val eqType = Concept2PacketParser.parseErgMachineType(value)
        val rawByte = if (value.isNotEmpty()) value[0].toInt() and 0xFF else -1
        AppDiagnosticLogger.log("GATT", "Concept2 CE060016 mac=$address rawByte=$rawByte resolved=${eqType.ifEmpty { "UNKNOWN" }}")
        if (eqType.isNotEmpty()) {
            knownConcept2Subtypes[address] = eqType
            if (!LiveFleetBleScanner.matchesConcept2Filter(eqType, concept2ErgFilter)) {
                AppDiagnosticLogger.log("GATT", "Concept2 mac=$address ($eqType) rejected by filter=${concept2ErgFilter.label} — freeing GATT slot")
                cooldownUntilMs[address] = System.currentTimeMillis() + 3_000L
                disconnectDevice(address)
                return
            }
            Log.i(TAG, "Concept2 CE060016 confirmed subtype for $address: $eqType")
            session.bikeData = session.bikeData.copy(
                equipmentType = eqType,
                lastSeenTimestamp = System.currentTimeMillis()
            )
            _bikeDataFlow.tryEmit(session.bikeData)
        }
    }

    @SuppressLint("MissingPermission")
    private fun executeNextGattOp(session: GattSession) {
        val nextOp = synchronized(session.pendingOps) {
            if (session.pendingOps.isEmpty()) null else session.pendingOps.removeFirst()
        }
        if (nextOp == null) {
            session.isSetupComplete = true
            session.setupCompletedAtMs = System.currentTimeMillis()
            finishInFlightSetup(session.macAddress)
            return
        }

        try {
            when (nextOp) {
                is PendingGattOp.ReadChar -> {
                    val started = session.gatt.readCharacteristic(nextOp.char)
                    if (!started) {
                        executeNextGattOp(session)
                    }
                }
                is PendingGattOp.EnableNotify -> {
                    session.gatt.setCharacteristicNotification(nextOp.char, true)
                    val cccd = nextOp.char.getDescriptor(Concept2PacketParser.UUID_CCCD)
                    if (cccd != null) {
                        val initiated = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            session.gatt.writeDescriptor(cccd, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) == 0
                        } else {
                            @Suppress("DEPRECATION")
                            cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            @Suppress("DEPRECATION")
                            session.gatt.writeDescriptor(cccd)
                        }
                        if (!initiated) {
                            executeNextGattOp(session)
                        }
                    } else {
                        executeNextGattOp(session)
                    }
                }
                is PendingGattOp.WriteChar -> {
                    val initiated = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        session.gatt.writeCharacteristic(
                            nextOp.char,
                            nextOp.data,
                            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                        ) == 0
                    } else {
                        @Suppress("DEPRECATION")
                        nextOp.char.value = nextOp.data
                        @Suppress("DEPRECATION")
                        session.gatt.writeCharacteristic(nextOp.char)
                    }
                    if (!initiated) {
                        executeNextGattOp(session)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed executing GATT op on ${session.macAddress}: ${e.message}")
            executeNextGattOp(session)
        }
    }

    private fun handleCharacteristicPayload(address: String, uuid: UUID, value: ByteArray) {
        val session = sessions[address] ?: return
        val updated = when (uuid) {
            Concept2PacketParser.UUID_ADDITIONAL_STATUS_1 ->
                Concept2PacketParser.parseAdditionalStatus1(value, session.bikeData)
            Concept2PacketParser.UUID_ADDITIONAL_STATUS_2 ->
                Concept2PacketParser.parseAdditionalStatus2(value, session.bikeData)
            Concept2PacketParser.UUID_GENERAL_STATUS ->
                Concept2PacketParser.parseGeneralStatus(value, session.bikeData)
            FtmsPacketParser.UUID_INDOOR_BIKE_DATA -> {
                val parsed = FtmsPacketParser.parseIndoorBikeData(value, session.bikeData)
                if (parsed.brandType == BrandType.CONCEPT2 && parsed.equipmentType.isEmpty()) {
                    parsed.copy(equipmentType = "BIKEERG")
                } else {
                    parsed
                }
            }
            FtmsPacketParser.UUID_ROWER_DATA ->
                FtmsPacketParser.parseRowerData(value, session.bikeData)
            FtmsPacketParser.UUID_CYCLING_POWER_MEASUREMENT -> {
                val res = FtmsPacketParser.parseCyclingPowerMeasurement(
                    bytes = value,
                    current = session.bikeData,
                    prevCrankRevs = session.prevCrankRevs,
                    prevCrankEventTime = session.prevCrankEventTime
                )
                session.prevCrankRevs = res.crankRevs
                session.prevCrankEventTime = res.crankEventTime
                res.bikeData
            }
            FtmsPacketParser.UUID_CSC_MEASUREMENT -> {
                val res = FtmsPacketParser.parseCscMeasurement(
                    bytes = value,
                    current = session.bikeData,
                    prevCrankRevs = session.prevCrankRevs,
                    prevCrankEventTime = session.prevCrankEventTime
                )
                session.prevCrankRevs = res.crankRevs
                session.prevCrankEventTime = res.crankEventTime
                res.bikeData
            }
            else -> return
        }
        session.bikeData = updated
        if (updated.brandType == BrandType.CONCEPT2 && updated.equipmentType.isNotEmpty()) {
            val prevKnown = knownConcept2Subtypes.put(address, updated.equipmentType)
            if (prevKnown != updated.equipmentType) {
                AppDiagnosticLogger.log("GATT", "Concept2 subtype from stream mac=$address eq=${updated.equipmentType}")
            }
            if (!LiveFleetBleScanner.matchesConcept2Filter(updated.equipmentType, concept2ErgFilter)) {
                AppDiagnosticLogger.log("GATT", "Concept2 mac=$address (${updated.equipmentType}) rejected by filter=${concept2ErgFilter.label} — freeing GATT slot")
                cooldownUntilMs[address] = System.currentTimeMillis() + 3_000L
                disconnectDevice(address)
                return
            }
        }
        packetCounter.incrementAndGet()
        if (updated.powerWatts > 0 || updated.cadenceRpm > 0f) {
            session.lastActivityTimestampMs = System.currentTimeMillis()
        }
        if (session.hasSeenNonZeroTelemetry || updated.powerWatts > 0 || updated.cadenceRpm > 0f) {
            if (!session.hasSeenNonZeroTelemetry) {
                AppDiagnosticLogger.log(
                    "GATT",
                    "First live telemetry mac=$address brand=${updated.brand} id=${updated.displayId} eq=${updated.equipmentType} ${updated.powerWatts}W ${updated.cadenceRpm.toInt()}RPM"
                )
            }
            session.hasSeenNonZeroTelemetry = true
            _bikeDataFlow.tryEmit(updated)
        }
    }

    private fun startSlotRecyclerLoop() {
        scope.launch {
            while (true) {
                delay(2_000)
                val now = System.currentTimeMillis()
                val hasWaiting = synchronized(queueLock) { pendingQueue.isNotEmpty() }
                if (!hasWaiting && sessions.size < MAX_CONCURRENT_CONNECTIONS - 1) continue

                sessions.values.toList().forEach { session ->
                    if (!session.isSetupComplete) return@forEach
                    val shouldRecycleIdleNew = !session.hasSeenNonZeroTelemetry &&
                        (now - session.setupCompletedAtMs >= IDLE_NEW_SLOT_RECYCLE_MS)
                    val shouldRecycleSilent = session.hasSeenNonZeroTelemetry &&
                        session.bikeData.powerWatts == 0 &&
                        session.bikeData.cadenceRpm == 0f &&
                        (now - session.lastActivityTimestampMs >= IDLE_ACTIVE_SLOT_RECYCLE_MS)
                    if (shouldRecycleIdleNew || shouldRecycleSilent) {
                        Log.i(TAG, "Recycling idle GATT slot for ${session.macAddress} (${session.bikeData.brand} ${session.bikeData.displayId})")
                        AppDiagnosticLogger.log("GATT", "Recycled idle slot mac=${session.macAddress} brand=${session.bikeData.brand} id=${session.bikeData.displayId} hadTelemetry=${session.hasSeenNonZeroTelemetry}")
                        cooldownUntilMs[session.macAddress] = now + IDLE_RECYCLE_COOLDOWN_MS
                        disconnectDevice(session.macAddress)
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnectDevice(macAddress: String) {
        if (macAddress.isBlank()) return
        synchronized(queueLock) {
            pendingQueue.removeAll { it.address.equals(macAddress, ignoreCase = true) }
        }
        val session = sessions.remove(macAddress)
        if (session != null) {
            try {
                session.gatt.disconnect()
            } catch (_: Exception) {
            }
            try {
                session.gatt.close()
            } catch (_: Exception) {
            }
        }
        finishInFlightSetup(macAddress)
    }

    @SuppressLint("MissingPermission")
    private fun cleanupSession(address: String, gatt: BluetoothGatt) {
        sessions.remove(address)
        try {
            gatt.disconnect()
        } catch (_: Exception) {
        }
        try {
            gatt.close()
        } catch (_: Exception) {
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnectAll() {
        val active = sessions.values.toList()
        synchronized(queueLock) {
            pendingQueue.clear()
            inFlightAddress = null
            inFlightTimeoutJob?.cancel()
            inFlightTimeoutJob = null
        }
        sessions.clear()
        unsupportedAddresses.clear()
        cooldownUntilMs.clear()
        lastProbedTimestampMs.clear()
        hasStagesPrimaryConsole = false
        active.forEach { session ->
            try {
                session.gatt.disconnect()
            } catch (_: Exception) {
            }
            try {
                session.gatt.close()
            } catch (_: Exception) {
            }
        }
    }

    companion object {
        private const val TAG = "UniversalGattManager"
        private const val MAX_CONCURRENT_CONNECTIONS = 6
        private const val GATT_SETUP_TIMEOUT_MS = 6_000L
        private const val GATT_ERROR_COOLDOWN_MS = 8_000L
        private const val IDLE_NEW_SLOT_RECYCLE_MS = 4_500L
        private const val IDLE_ACTIVE_SLOT_RECYCLE_MS = 10_000L
        private const val IDLE_RECYCLE_COOLDOWN_MS = 6_000L
    }
}
