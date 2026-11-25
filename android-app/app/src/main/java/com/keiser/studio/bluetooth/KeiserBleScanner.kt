package com.keiser.studio.bluetooth

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
import com.keiser.studio.model.BikeData
import com.keiser.studio.model.BikeProtocol
import com.keiser.studio.parser.Concept2PacketParser
import com.keiser.studio.parser.FtmsPacketParser
import com.keiser.studio.parser.KeiserPacketParser
import com.keiser.studio.parser.Schwinn4iiiiPacketParser
import com.keiser.studio.parser.StagesPacketParser
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class DiscoveredGattDevice(
    val device: BluetoothDevice,
    val advertisedName: String,
    val meta: FtmsPacketParser.DiscoveredGattMeta
)

class KeiserBleScanner(private val context: Context) {

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

    @Volatile
    var isScanning: Boolean = false
        private set

    @Volatile
    var currentProtocol: BikeProtocol = BikeProtocol.KEISER
        private set

    @Volatile
    var scanStartedTimestampMs: Long = 0L
        private set

    @Volatile
    var lastPacketTimestampMs: Long = 0L
        private set

    fun setProtocol(protocol: BikeProtocol) {
        currentProtocol = protocol
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
            isScanning = false
            _scanFailedFlow.tryEmit(errorCode)
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleScanResult(result: ScanResult) {
        val protocol = currentProtocol

        // 1. Passive Keiser M3i BLE Broadcast (0x0102 — unlimited capacity, no GATT needed)
        if (protocol == BikeProtocol.KEISER || protocol == BikeProtocol.AUTO_DETECT) {
            val keiserBike = KeiserPacketParser.parse(result)
            if (keiserBike != null) {
                lastPacketTimestampMs = System.currentTimeMillis()
                _bikeDataFlow.tryEmit(keiserBike)
                return
            }
        }

        // 2. Standards-based BLE GATT Bikes & Ergs (Concept2 PM5, FTMS 0x1826, Cycling Power 0x1818)
        if (protocol != BikeProtocol.KEISER) {
            val record = result.scanRecord ?: return
            val device = result.device ?: return
            val name = record.deviceName ?: device.name ?: ""
            val serviceUuids = record.serviceUuids?.map { it.uuid } ?: emptyList()

            val meta = FtmsPacketParser.classifyGattDevice(
                deviceName = name,
                macAddress = device.address ?: "",
                serviceUuids = serviceUuids,
                hasConcept2MfrData = record.getManufacturerSpecificData(Concept2PacketParser.COMPANY_ID_CONCEPT2) != null,
                hasStagesMfrData = record.getManufacturerSpecificData(StagesPacketParser.COMPANY_ID_STAGES) != null,
                has4iiiiMfrData = record.getManufacturerSpecificData(Schwinn4iiiiPacketParser.COMPANY_ID_4IIII) != null,
                protocol = protocol
            )
            if (meta != null) {
                lastPacketTimestampMs = System.currentTimeMillis()
                _gattDeviceFlow.tryEmit(DiscoveredGattDevice(device, name, meta))
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
        } catch (e: Exception) {
            isScanning = false
            Log.e(TAG, "Error stopping BLE scan: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "KeiserBleScanner"
    }
}
