package com.livefleet.studio.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import com.livefleet.studio.data.RiderProfileRepository
import com.livefleet.studio.model.BikeData
import com.livefleet.studio.model.BikeProtocol
import com.livefleet.studio.model.BrandType
import com.livefleet.studio.model.Concept2ErgFilter
import com.livefleet.studio.parser.Concept2PacketParser
import com.livefleet.studio.parser.FtmsPacketParser
import com.livefleet.studio.parser.KeiserPacketParser
import com.livefleet.studio.parser.Schwinn4iiiiPacketParser
import com.livefleet.studio.parser.StagesPacketParser
import com.livefleet.studio.util.AppDiagnosticLogger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.atomic.AtomicInteger

data class DiscoveredGattDevice(
    val device: BluetoothDevice,
    val advertisedName: String,
    val rssi: Int,
    val isWhitelisted: Boolean,
    val meta: FtmsPacketParser.DiscoveredGattMeta
)

class LiveFleetBleScanner(private val context: Context) {

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? get() = bluetoothManager?.adapter
    private val bleScanner: BluetoothLeScanner? get() = bluetoothAdapter?.bluetoothLeScanner

    private val _bikeDataFlow = MutableSharedFlow<BikeData>(extraBufferCapacity = 64)
    val bikeDataFlow: SharedFlow<BikeData> = _bikeDataFlow.asSharedFlow()

    private val _gattDeviceFlow = MutableSharedFlow<DiscoveredGattDevice>(extraBufferCapacity = 32)
    val gattDeviceFlow: SharedFlow<DiscoveredGattDevice> = _gattDeviceFlow.asSharedFlow()

    private val _scanFailedFlow = MutableSharedFlow<Int>(extraBufferCapacity = 8)
    val scanFailedFlow: SharedFlow<Int> = _scanFailedFlow.asSharedFlow()

    private val packetCounter = AtomicInteger(0)

    @Volatile
    var isScanning: Boolean = false
        private set

    @Volatile
    var currentProtocol: BikeProtocol = BikeProtocol.KEISER
        private set

    @Volatile
    var studioLockProximityEnabled: Boolean = false

    @Volatile
    var whitelistedHardwareKeys: Set<String> = emptySet()

    @Volatile
    var concept2ErgFilter: Concept2ErgFilter = Concept2ErgFilter.ALL

    @Volatile
    var scanStartedTimestampMs: Long = 0L
        private set

    @Volatile
    var lastPacketTimestampMs: Long = 0L
        private set

    fun setProtocol(protocol: BikeProtocol) {
        currentProtocol = protocol
    }

    fun drainPacketCount(): Int = packetCounter.getAndSet(0)

    fun resetPacketCount() {
        packetCounter.set(0)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result ?: return
            handleScanResult(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.forEach { handleScanResult(it) }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "BLE Scan failed with error code: $errorCode")
            AppDiagnosticLogger.error("BLE", "BLE Scan failed errorCode=$errorCode")
            isScanning = false
            _scanFailedFlow.tryEmit(errorCode)
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleScanResult(result: ScanResult) {
        val protocol = currentProtocol
        val record = result.scanRecord ?: return
        val device = result.device ?: return
        val macAddress = device.address ?: ""
        val normalizedMac = RiderProfileRepository.normalizeHardwareKey(macAddress)
        val isWhitelisted = normalizedMac.isNotEmpty() && whitelistedHardwareKeys.contains(normalizedMac)

        // Layer 2 — Studio Lock & RSSI Proximity Gate:
        // When enabled, ignore any non-whitelisted machine weaker than -75 dBm.
        if (studioLockProximityEnabled && !isWhitelisted && result.rssi < RSSI_PROXIMITY_THRESHOLD_DBM) {
            return
        }

        val hasKeiserMfrData = record.getManufacturerSpecificData(KeiserPacketParser.COMPANY_ID_KEISER) != null

        // Layer 1a — Passive Keiser M3i BLE Broadcast ONLY (0x0102, >= 15 stripped / 17 total bytes; never GATT)
        if (hasKeiserMfrData) {
            if (protocol == BikeProtocol.KEISER || protocol == BikeProtocol.AUTO_DETECT) {
                val keiserBike = KeiserPacketParser.parse(result)
                if (keiserBike != null) {
                    lastPacketTimestampMs = System.currentTimeMillis()
                    packetCounter.incrementAndGet()
                    AppDiagnosticLogger.logUniqueSighting(
                        "KEISER:$macAddress",
                        "BLE",
                        "Matched Keiser M3i #${keiserBike.bikeId} ($macAddress, RSSI=${result.rssi}, wl=$isWhitelisted)"
                    )
                    _bikeDataFlow.tryEmit(keiserBike.copy(isWhitelisted = isWhitelisted))
                }
            }
            return
        }

        // Layer 1b — Strict GATT Brand Gatekeeper (Concept2, Wattbike/FTMS, Stages, Schwinn 4iiii, Power Meters)
        if (protocol != BikeProtocol.KEISER) {
            val advertisedName = (record.deviceName ?: device.name ?: "").trim()
            val serviceUuids = record.serviceUuids?.map { it.uuid } ?: emptyList()

            val meta = FtmsPacketParser.classifyGattDevice(
                deviceName = advertisedName,
                macAddress = macAddress,
                serviceUuids = serviceUuids,
                hasConcept2MfrData = record.getManufacturerSpecificData(Concept2PacketParser.COMPANY_ID_CONCEPT2) != null,
                hasStagesMfrData = record.getManufacturerSpecificData(StagesPacketParser.COMPANY_ID_STAGES) != null ||
                    record.getManufacturerSpecificData(StagesPacketParser.COMPANY_ID_STAGES_SIG) != null,
                has4iiiiMfrData = record.getManufacturerSpecificData(Schwinn4iiiiPacketParser.COMPANY_ID_4IIII) != null,
                protocol = protocol,
                hasKeiserMfrData = false
            )
            if (meta != null) {
                if (meta.brandType == BrandType.CONCEPT2 &&
                    !matchesConcept2Filter(meta.equipmentType, concept2ErgFilter)
                ) {
                    AppDiagnosticLogger.logUniqueSighting(
                        "C2SKIP:$macAddress:${concept2ErgFilter.id}",
                        "BLE",
                        "Skipped Concept2 ${meta.equipmentType} '$advertisedName' ($macAddress) due to filter=${concept2ErgFilter.label}"
                    )
                    return
                }
                val mfrIds = mutableListOf<String>()
                val sparse = record.manufacturerSpecificData
                if (sparse != null) {
                    for (i in 0 until sparse.size()) {
                        mfrIds.add("0x${sparse.keyAt(i).toString(16).uppercase()}")
                    }
                }
                val uuidSummary = serviceUuids.joinToString(",") { it.toString().take(8) }
                AppDiagnosticLogger.logUniqueSighting(
                    "GATT:$macAddress:${protocol.id}",
                    "BLE",
                    "Discovered ${meta.brand} name='$advertisedName' mac=$macAddress rssi=${result.rssi} " +
                        "id=${meta.displayId} eq=${meta.equipmentType.ifEmpty { "PENDING" }} primary=${meta.isPrimaryConsole} " +
                        "mfr=$mfrIds uuids=[$uuidSummary]"
                )
                lastPacketTimestampMs = System.currentTimeMillis()
                packetCounter.incrementAndGet()
                _gattDeviceFlow.tryEmit(
                    DiscoveredGattDevice(
                        device = device,
                        advertisedName = advertisedName,
                        rssi = result.rssi,
                        isWhitelisted = isWhitelisted,
                        meta = meta
                    )
                )
            } else if (advertisedName.isNotEmpty() || serviceUuids.isNotEmpty()) {
                val uuidSummary = serviceUuids.joinToString(",") { it.toString().take(8) }
                AppDiagnosticLogger.logUniqueSighting(
                    "IGN:$macAddress:${protocol.id}",
                    "BLE",
                    "Ignored BLE device name='$advertisedName' mac=$macAddress rssi=${result.rssi} " +
                        "uuids=[$uuidSummary] under protocol=${protocol.id}"
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan(): Boolean {
        if (isScanning) return true
        val scanner = bleScanner ?: run {
            Log.w(TAG, "BluetoothLeScanner not available or Bluetooth disabled")
            return false
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setReportDelay(0L)
            .build()

        return try {
            scanner.startScan(null, settings, scanCallback)
            val now = System.currentTimeMillis()
            isScanning = true
            scanStartedTimestampMs = now
            lastPacketTimestampMs = now
            Log.i(TAG, "Unfiltered low-latency BLE scan started (protocol: ${currentProtocol.displayName})")
            AppDiagnosticLogger.clearSessionSightings()
            AppDiagnosticLogger.log("BLE", "Scan started protocol=${currentProtocol.id} ergFilter=${concept2ErgFilter.id} studioLock=$studioLockProximityEnabled")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException starting BLE scan: ${e.message}")
            isScanning = false
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error starting BLE scan: ${e.message}")
            isScanning = false
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!isScanning) return
        try {
            bleScanner?.stopScan(scanCallback)
            isScanning = false
            Log.i(TAG, "BLE scan stopped")
            AppDiagnosticLogger.log("BLE", "Scan stopped")
        } catch (e: Exception) {
            isScanning = false
            Log.e(TAG, "Error stopping BLE scan: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "LiveFleetBleScanner"
        const val RSSI_PROXIMITY_THRESHOLD_DBM = -75

        fun matchesConcept2Filter(equipmentType: String, filter: Concept2ErgFilter): Boolean {
            if (filter == Concept2ErgFilter.ALL) return true
            val eq = equipmentType.uppercase()
            if (eq.isEmpty()) return true // Unknown yet: let GATT CE060016 confirm
            return when (filter) {
                Concept2ErgFilter.ALL -> true
                Concept2ErgFilter.BIKEERG_ONLY -> eq == "BIKEERG"
                Concept2ErgFilter.ROWERG_ONLY -> eq == "ROWERG"
                Concept2ErgFilter.SKIERG_ONLY -> eq == "SKIERG"
            }
        }
    }
}
