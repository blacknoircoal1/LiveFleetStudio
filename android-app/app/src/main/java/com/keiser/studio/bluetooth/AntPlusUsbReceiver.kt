package com.keiser.studio.bluetooth

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import com.keiser.studio.model.BikeData
import com.keiser.studio.model.BikeProtocol
import com.keiser.studio.parser.AntPlusFecParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Zero-dependency native Android USB Host ANT+ Receiver for Group Classes (10–30+ bikes/ergs).
 *
 * Works with standard USB-C / OTG ANT+ dongles (Dynastream / Garmin / CYCPLUS / Coospo):
 *   VID 0x0FCF, PID 0x1008 (ANTUSB2) or 0x1009 (ANTUSB-m).
 *
 * Opens the stick in ANT+ Continuous Rx Scan Mode (0x5B) with Extended Messages (0x66) enabled,
 * simultaneously decoding BOTH:
 *  - ANT+ FE-C (Device Type 0x11): Concept2 PM5 BikeErg/RowErg/SkiErg, Wattbike, Schwinn Echelon2
 *  - ANT+ Bicycle Power (Device Type 0x0B): Stages SC3/SC2, Schwinn Echelon2/4iiii, Assioma, Garmin Rally, Quarq, SRM
 */
class AntPlusUsbReceiver(private val context: Context) {

    private val usbManager: UsbManager? =
        context.getSystemService(Context.USB_SERVICE) as? UsbManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _bikeDataFlow = MutableSharedFlow<BikeData>(extraBufferCapacity = 128)
    val bikeDataFlow: SharedFlow<BikeData> = _bikeDataFlow.asSharedFlow()

    private val _statusFlow = MutableStateFlow("No USB ANT+ Dongle")
    val statusFlow: StateFlow<String> = _statusFlow.asStateFlow()

    private val _isActiveFlow = MutableStateFlow(false)
    val isActiveFlow: StateFlow<Boolean> = _isActiveFlow.asStateFlow()

    @Volatile
    private var currentProtocol: BikeProtocol = BikeProtocol.AUTO_DETECT

    private val ergStateByDeviceKey = ConcurrentHashMap<Long, BikeData>()

    private var readJob: Job? = null
    private var usbConnection: UsbDeviceConnection? = null
    private var usbInterface: UsbInterface? = null
    private var isReceiverRegistered = false
    private var wantRunning = false

    fun setProtocol(protocol: BikeProtocol) {
        currentProtocol = protocol
    }

    private val usbPermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_USB_PERMISSION -> {
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }
                    if (granted && device != null && wantRunning) {
                        openAndStartScanning(device)
                    } else if (!granted) {
                        _isActiveFlow.value = false
                        _statusFlow.value = "USB Permission Denied"
                    }
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    if (wantRunning) {
                        findAntUsbStick()?.let { requestOrStart(it) }
                    }
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    closeUsb()
                    _isActiveFlow.value = false
                    _statusFlow.value = "No USB ANT+ Dongle"
                }
            }
        }
    }

    fun start() {
        wantRunning = true
        registerReceiverIfNeeded()
        val device = findAntUsbStick()
        if (device == null) {
            _isActiveFlow.value = false
            _statusFlow.value = "No USB ANT+ Dongle"
            return
        }
        requestOrStart(device)
    }

    fun stop() {
        wantRunning = false
        closeUsb()
        unregisterReceiverIfNeeded()
        _isActiveFlow.value = false
        _statusFlow.value = if (findAntUsbStick() != null) "USB ANT+ Standby" else "No USB ANT+ Dongle"
    }

    fun release() {
        stop()
        scope.cancel()
    }

    private fun findAntUsbStick(): UsbDevice? {
        val manager = usbManager ?: return null
        return manager.deviceList.values.firstOrNull { dev ->
            dev.vendorId == VENDOR_ID_DYNASTREAM &&
                (dev.productId == PRODUCT_ID_ANTUSB2 || dev.productId == PRODUCT_ID_ANTUSB_M)
        }
    }

    private fun requestOrStart(device: UsbDevice) {
        val manager = usbManager ?: return
        if (manager.hasPermission(device)) {
            openAndStartScanning(device)
        } else {
            _isActiveFlow.value = false
            _statusFlow.value = "Waiting for USB Permission"
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                Intent(ACTION_USB_PERMISSION).setPackage(context.packageName),
                flags
            )
            manager.requestPermission(device, pendingIntent)
        }
    }

    private fun openAndStartScanning(device: UsbDevice) {
        if (readJob?.isActive == true) return
        val manager = usbManager ?: return

        val connection = manager.openDevice(device) ?: run {
            _isActiveFlow.value = false
            _statusFlow.value = "Unable to open USB ANT+ Stick"
            return
        }

        if (device.interfaceCount <= 0) {
            connection.close()
            _isActiveFlow.value = false
            _statusFlow.value = "Invalid USB ANT+ Interface"
            return
        }

        val intf = device.getInterface(0)
        if (!connection.claimInterface(intf, true)) {
            connection.close()
            _isActiveFlow.value = false
            _statusFlow.value = "Unable to claim USB ANT+ Interface"
            return
        }

        var inEndpoint: UsbEndpoint? = null
        var outEndpoint: UsbEndpoint? = null
        for (i in 0 until intf.endpointCount) {
            val ep = intf.getEndpoint(i)
            if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                if (ep.direction == UsbConstants.USB_DIR_IN) inEndpoint = ep
                else if (ep.direction == UsbConstants.USB_DIR_OUT) outEndpoint = ep
            }
        }

        if (inEndpoint == null || outEndpoint == null) {
            connection.releaseInterface(intf)
            connection.close()
            _isActiveFlow.value = false
            _statusFlow.value = "USB ANT+ Endpoints Missing"
            return
        }

        usbConnection = connection
        usbInterface = intf
        _isActiveFlow.value = true
        _statusFlow.value = "ANT+ Group Scan Active (Priority)"

        readJob = scope.launch {
            try {
                initializeAntContinuousScan(connection, outEndpoint)
                readAntFramesLoop(connection, inEndpoint)
            } catch (e: Exception) {
                Log.e(TAG, "USB ANT+ loop error: ${e.message}")
            } finally {
                _isActiveFlow.value = false
                if (wantRunning) {
                    _statusFlow.value = "USB ANT+ Disconnected"
                }
            }
        }
    }

    private suspend fun initializeAntContinuousScan(
        connection: UsbDeviceConnection,
        outEndpoint: UsbEndpoint
    ) {
        sendAntCommand(connection, outEndpoint, AntPlusFecParser.buildAntCommand(0x4A, 0x00))
        delay(500)
        sendAntCommand(
            connection,
            outEndpoint,
            AntPlusFecParser.buildAntCommand(0x46, 0x00, 0xB9, 0xA5, 0x21, 0xFB, 0xBD, 0x72, 0xC3, 0x45)
        )
        delay(80)
        sendAntCommand(connection, outEndpoint, AntPlusFecParser.buildAntCommand(0x42, 0x00, 0x40, 0x00))
        delay(80)
        sendAntCommand(connection, outEndpoint, AntPlusFecParser.buildAntCommand(0x51, 0x00, 0x00, 0x00, 0x00, 0x00))
        delay(80)
        sendAntCommand(connection, outEndpoint, AntPlusFecParser.buildAntCommand(0x45, 0x00, 57))
        delay(80)
        sendAntCommand(connection, outEndpoint, AntPlusFecParser.buildAntCommand(0x66, 0x00, 0x01))
        delay(80)
        sendAntCommand(connection, outEndpoint, AntPlusFecParser.buildAntCommand(0x5B, 0x00))
        Log.i(TAG, "USB ANT+ Continuous Rx Scan Mode opened (FE-C + Bike Power, Extended Messages ON)")
    }

    private fun sendAntCommand(
        connection: UsbDeviceConnection,
        outEndpoint: UsbEndpoint,
        frame: ByteArray
    ) {
        val written = connection.bulkTransfer(outEndpoint, frame, frame.size, 500)
        if (written < 0) {
            Log.w(TAG, "USB ANT+ command write failed (msgId=0x${(frame[2].toInt() and 0xFF).toString(16)})")
        }
    }

    private fun readAntFramesLoop(
        connection: UsbDeviceConnection,
        inEndpoint: UsbEndpoint
    ) {
        val readBuf = ByteArray(64)
        val frameBuf = ByteArray(128)
        var framePos = 0
        var expectedLen = -1

        while (scope.isActive && wantRunning) {
            val count = connection.bulkTransfer(inEndpoint, readBuf, readBuf.size, 500)
            if (count <= 0) continue

            for (i in 0 until count) {
                val b = readBuf[i]
                if (framePos == 0) {
                    if (b == AntPlusFecParser.ANT_SYNC_BYTE) {
                        frameBuf[0] = b
                        framePos = 1
                        expectedLen = -1
                    }
                    continue
                }

                if (framePos < frameBuf.size) {
                    frameBuf[framePos++] = b
                } else {
                    framePos = 0
                    expectedLen = -1
                    continue
                }

                if (framePos == 2) {
                    val dataLen = b.toInt() and 0xFF
                    if (dataLen > 64) {
                        framePos = 0
                        expectedLen = -1
                    } else {
                        expectedLen = dataLen + 4
                    }
                } else if (expectedLen > 0 && framePos == expectedLen) {
                    processCompleteAntFrame(frameBuf, expectedLen)
                    framePos = 0
                    expectedLen = -1
                }
            }
        }
    }

    private fun processCompleteAntFrame(frame: ByteArray, length: Int) {
        var xor = 0
        for (i in 0 until length - 1) {
            xor = xor xor (frame[i].toInt() and 0xFF)
        }
        val expectedChecksum = frame[length - 1].toInt() and 0xFF
        if (xor != expectedChecksum) return

        val dataLen = frame[1].toInt() and 0xFF
        val msgId = frame[2].toInt() and 0xFF
        if (3 + dataLen > length - 1) return

        val data = frame.copyOfRange(3, 3 + dataLen)
        val ext = AntPlusFecParser.parseExtendedBroadcast(msgId, data) ?: return

        val deviceKey = (ext.deviceType.toLong() shl 16) or ext.deviceNumber.toLong()
        val updated = when (ext.deviceType) {
            AntPlusFecParser.DEVICE_TYPE_FE_C -> AntPlusFecParser.parseFecPage(
                deviceNumber = ext.deviceNumber,
                payload = ext.payload,
                existing = ergStateByDeviceKey[deviceKey],
                selectedProtocol = currentProtocol
            )
            AntPlusFecParser.DEVICE_TYPE_BIKE_POWER -> AntPlusFecParser.parseBikePowerPage(
                deviceNumber = ext.deviceNumber,
                payload = ext.payload,
                existing = ergStateByDeviceKey[deviceKey],
                selectedProtocol = currentProtocol
            )
            else -> null
        } ?: return

        ergStateByDeviceKey[deviceKey] = updated
        _bikeDataFlow.tryEmit(updated)
    }

    private fun closeUsb() {
        readJob?.cancel()
        readJob = null
        try {
            usbInterface?.let { usbConnection?.releaseInterface(it) }
        } catch (_: Exception) {
        }
        try {
            usbConnection?.close()
        } catch (_: Exception) {
        }
        usbInterface = null
        usbConnection = null
        ergStateByDeviceKey.clear()
    }

    private fun registerReceiverIfNeeded() {
        if (isReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbPermissionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(usbPermissionReceiver, filter)
        }
        isReceiverRegistered = true
    }

    private fun unregisterReceiverIfNeeded() {
        if (!isReceiverRegistered) return
        try {
            context.unregisterReceiver(usbPermissionReceiver)
        } catch (_: Exception) {
        }
        isReceiverRegistered = false
    }

    companion object {
        private const val TAG = "AntPlusUsbReceiver"
        private const val ACTION_USB_PERMISSION = "com.keiser.studio.USB_ANT_PERMISSION"
        private const val VENDOR_ID_DYNASTREAM = 0x0FCF
        private const val PRODUCT_ID_ANTUSB2 = 0x1008
        private const val PRODUCT_ID_ANTUSB_M = 0x1009
    }
}
