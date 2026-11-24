package com.keiser.studio.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import com.keiser.studio.model.BikeData
import com.keiser.studio.model.BikeProtocol
import com.keiser.studio.parser.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class KeiserBleScanner(private val context: Context) {

    private val bluetoothManager: BluetoothManager? = 
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? get() = bluetoothManager?.adapter
    private val bleScanner: BluetoothLeScanner? get() = bluetoothAdapter?.bluetoothLeScanner

    private val _bikeDataFlow = MutableSharedFlow<BikeData>(extraBufferCapacity = 64)
    val bikeDataFlow: SharedFlow<BikeData> = _bikeDataFlow.asSharedFlow()

    private var isScanning = false
    var currentProtocol: BikeProtocol = BikeProtocol.KEISER
        private set

    fun setProtocol(protocol: BikeProtocol) {
        if (currentProtocol != protocol) {
            currentProtocol = protocol
            if (isScanning) {
                stopScan()
                startScan()
            }
        }
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
        }
    }

    private fun handleScanResult(result: ScanResult) {
        val parsed = UniversalBikeParser.parse(result, currentProtocol)
        if (parsed != null) {
            _bikeDataFlow.tryEmit(parsed)
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
            .build()

        val filters = when (currentProtocol) {
            BikeProtocol.KEISER -> listOf(
                ScanFilter.Builder()
                    .setManufacturerData(KeiserPacketParser.COMPANY_ID_KEISER, byteArrayOf())
                    .build()
            )
            BikeProtocol.STAGES -> listOf(
                ScanFilter.Builder()
                    .setManufacturerData(StagesPacketParser.COMPANY_ID_STAGES, byteArrayOf())
                    .build()
            )
            BikeProtocol.SCHWINN_4IIII -> listOf(
                ScanFilter.Builder()
                    .setManufacturerData(Schwinn4iiiiPacketParser.COMPANY_ID_4IIII, byteArrayOf())
                    .build()
            )
            BikeProtocol.CONCEPT2 -> listOf(
                ScanFilter.Builder()
                    .setManufacturerData(Concept2PacketParser.COMPANY_ID_CONCEPT2, byteArrayOf())
                    .build()
            )
            BikeProtocol.FTMS_UNIVERSAL -> listOf(
                ScanFilter.Builder()
                    .setServiceUuid(FtmsPacketParser.UUID_FTMS)
                    .build()
            )
            BikeProtocol.POWER_METERS -> listOf(
                ScanFilter.Builder()
                    .setServiceUuid(PowerMeterPacketParser.UUID_CYCLING_POWER)
                    .build()
            )
            BikeProtocol.AUTO_DETECT -> emptyList()
        }

        return try {
            scanner.startScan(filters, settings, scanCallback)
            isScanning = true
            Log.i(TAG, "Low latency BLE scan started with protocol: ${currentProtocol.displayName}")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException starting BLE scan: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error starting BLE scan: ${e.message}")
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!isScanning) return
        try {
            bleScanner?.stopScan(scanCallback)
            isScanning = false
            Log.i(TAG, "Keiser BLE scan stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping BLE scan: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "KeiserBleScanner"
    }
}
