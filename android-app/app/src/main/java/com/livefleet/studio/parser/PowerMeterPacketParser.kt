package com.livefleet.studio.parser

import android.bluetooth.le.ScanResult
import com.livefleet.studio.model.BikeData
import com.livefleet.studio.model.BikeProtocol

/**
 * Standard cycling power meters (Favero Assioma, Garmin Rally, Quarq, SRM, PowerTap) stream live
 * telemetry over Bluetooth SIG Cycling Power GATT (0x1818 -> 0x2A63) and ANT+ Bicycle Power
 * (Device Type 0x0B), not passive BLE ads. Passive parse returns null so raw advertising bytes
 * are never misread as live data.
 */
object PowerMeterPacketParser : BikePacketParser {
    override val protocol: BikeProtocol = BikeProtocol.POWER_METERS
    override fun parse(result: ScanResult): BikeData? = null
}
