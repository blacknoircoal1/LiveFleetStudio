package com.keiser.studio.bluetooth

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
import com.keiser.studio.model.BikeData
import com.keiser.studio.parser.Concept2PacketParser
import com.keiser.studio.parser.FtmsPacketParser
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Unified BLE GATT Client managing up to [MAX_CONCURRENT_CONNECTIONS] simultaneous live bike/erg
 * connections over built-in phone/tablet Bluetooth across all 3 official fitness GATT standards:
 *
 *  1. Concept2 PM5 Rowing Service (CE060030 -> CE060032 / CE060033 / CE060031)
 *  2. Bluetooth SIG FTMS (0x1826 -> 0x2AD2 Indoor Bike Data: Wattbike, Life Fitness IC5-8, Technogym, Matrix, Wahoo)
 *  3. Bluetooth SIG Cycling Power (0x1818 -> 0x2A63: Stages SC3/SC2, Schwinn 4iiii, Assioma, Garmin Rally, Quarq, SRM)
 */
class UniversalGattManager(private val context: Context) {

    private val _bikeDataFlow = MutableSharedFlow<BikeData>(extraBufferCapacity = 64)
    val bikeDataFlow: SharedFlow<BikeData> = _bikeDataFlow.asSharedFlow()

    private data class GattSession(
        val macAddress: String,
        val gatt: BluetoothGatt,
        var bikeData: BikeData,
        var prevCrankRevs: Int? = null,
        var prevCrankEventTime: Int? = null,
        val subscriptionQueue: ArrayDeque<BluetoothGattCharacteristic> = ArrayDeque()
    )

    private val sessions = ConcurrentHashMap<String, GattSession>()
    private val connectingAddresses = ConcurrentHashMap.newKeySet<String>()

    fun connectedCount(): Int = sessions.size

    @SuppressLint("MissingPermission")
    fun connectIfNeeded(
        device: BluetoothDevice,
        advertisedName: String,
        meta: FtmsPacketParser.DiscoveredGattMeta
    ) {
        val address = device.address ?: return
        if (sessions.containsKey(address) || connectingAddresses.contains(address)) return
        if (sessions.size + connectingAddresses.size >= MAX_CONCURRENT_CONNECTIONS) {
            return
        }

        val name = advertisedName.ifEmpty { device.name ?: meta.brand }
        val formattedDisplayId = if (meta.displayId.length >= 5) meta.displayId else "#${meta.displayId}"
        val initialBike = BikeData(
            bikeId = meta.stableBikeId,
            displayId = formattedDisplayId,
            brand = meta.brand,
            equipmentType = meta.equipmentType,
            riderName = "${meta.brand} $formattedDisplayId",
            powerWatts = 0,
            cadenceRpm = 0f,
            calories = 0,
            lastSeenTimestamp = System.currentTimeMillis(),
            isStale = false
        )

        connectingAddresses.add(address)
        Log.i(TAG, "Connecting BLE GATT to ${meta.brand}: $name ($address) -> ID $formattedDisplayId")

        val callback = createGattCallback(address, initialBike)
        try {
            val gatt = device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
            if (gatt == null) {
                connectingAddresses.remove(address)
            } else {
                sessions[address] = GattSession(address, gatt, initialBike)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing BLUETOOTH_CONNECT permission for $address: ${e.message}")
            connectingAddresses.remove(address)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open GATT to $address: ${e.message}")
            connectingAddresses.remove(address)
        }
    }

    private fun createGattCallback(address: String, initialBike: BikeData): BluetoothGattCallback {
        return object : BluetoothGattCallback() {
            @SuppressLint("MissingPermission")
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Log.w(TAG, "GATT error ($status) on $address (${initialBike.brand}) — cleaning up")
                    cleanupSession(address, gatt)
                    return
                }

                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        connectingAddresses.remove(address)
                        Log.i(TAG, "GATT connected: $address (${initialBike.brand} ${initialBike.displayId})")
                        try {
                            gatt.discoverServices()
                        } catch (e: Exception) {
                            Log.e(TAG, "discoverServices failed for $address: ${e.message}")
                            cleanupSession(address, gatt)
                        }
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        Log.i(TAG, "GATT disconnected: $address (${initialBike.brand} ${initialBike.displayId})")
                        cleanupSession(address, gatt)
                    }
                }
            }

            @SuppressLint("MissingPermission")
            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Log.w(TAG, "Service discovery failed ($status) for $address")
                    cleanupSession(address, gatt)
                    return
                }

                val session = sessions[address] ?: return
                val charsToSubscribe = mutableListOf<BluetoothGattCharacteristic>()

                // Priority 1: Concept2 PM5 Rowing Service (CE060030)
                val pm5Service = gatt.getService(Concept2PacketParser.UUID_ROWING_SERVICE)
                if (pm5Service != null) {
                    listOf(
                        Concept2PacketParser.UUID_ADDITIONAL_STATUS_1,
                        Concept2PacketParser.UUID_ADDITIONAL_STATUS_2,
                        Concept2PacketParser.UUID_GENERAL_STATUS
                    ).forEach { uuid ->
                        pm5Service.getCharacteristic(uuid)?.let { charsToSubscribe.add(it) }
                    }
                } else {
                    // Priority 2: Bluetooth SIG FTMS (0x1826 -> 0x2AD2 Indoor Bike Data)
                    val ftmsService = gatt.getService(FtmsPacketParser.UUID_FTMS_SERVICE)
                    val indoorBikeChar = ftmsService?.getCharacteristic(FtmsPacketParser.UUID_INDOOR_BIKE_DATA)
                    if (indoorBikeChar != null) {
                        charsToSubscribe.add(indoorBikeChar)
                    } else {
                        // Priority 3: Bluetooth SIG Cycling Power (0x1818 -> 0x2A63 Measurement)
                        val cpsService = gatt.getService(FtmsPacketParser.UUID_CYCLING_POWER_SERVICE)
                        val cpsChar = cpsService?.getCharacteristic(FtmsPacketParser.UUID_CYCLING_POWER_MEASUREMENT)
                        if (cpsChar != null) {
                            charsToSubscribe.add(cpsChar)
                        }
                    }
                }

                if (charsToSubscribe.isEmpty()) {
                    Log.w(TAG, "No supported fitness GATT characteristics found on $address (${session.bikeData.brand})")
                    cleanupSession(address, gatt)
                    return
                }

                synchronized(session.subscriptionQueue) {
                    session.subscriptionQueue.clear()
                    charsToSubscribe.forEach { session.subscriptionQueue.addLast(it) }
                }

                // Emit initial connected tile immediately so the user sees it is live
                _bikeDataFlow.tryEmit(session.bikeData.copy(lastSeenTimestamp = System.currentTimeMillis()))
                subscribeNextCharacteristic(session)
            }

            @SuppressLint("MissingPermission")
            override fun onDescriptorWrite(
                gatt: BluetoothGatt,
                descriptor: BluetoothGattDescriptor,
                status: Int
            ) {
                val session = sessions[address] ?: return
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Log.w(TAG, "Descriptor write failed ($status) on $address for ${descriptor.characteristic.uuid}")
                }
                subscribeNextCharacteristic(session)
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

    @SuppressLint("MissingPermission")
    private fun subscribeNextCharacteristic(session: GattSession) {
        val nextChar = synchronized(session.subscriptionQueue) {
            if (session.subscriptionQueue.isEmpty()) null else session.subscriptionQueue.removeFirst()
        } ?: return

        try {
            session.gatt.setCharacteristicNotification(nextChar, true)
            val cccd = nextChar.getDescriptor(Concept2PacketParser.UUID_CCCD)
            if (cccd != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    session.gatt.writeDescriptor(cccd, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                } else {
                    @Suppress("DEPRECATION")
                    cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    @Suppress("DEPRECATION")
                    session.gatt.writeDescriptor(cccd)
                }
            } else {
                subscribeNextCharacteristic(session)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed subscribing to ${nextChar.uuid} on ${session.macAddress}: ${e.message}")
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
            FtmsPacketParser.UUID_INDOOR_BIKE_DATA ->
                FtmsPacketParser.parseIndoorBikeData(value, session.bikeData)
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
            else -> return
        }
        session.bikeData = updated
        _bikeDataFlow.tryEmit(updated)
    }

    @SuppressLint("MissingPermission")
    private fun cleanupSession(address: String, gatt: BluetoothGatt) {
        connectingAddresses.remove(address)
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
        sessions.clear()
        connectingAddresses.clear()
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
    }
}
