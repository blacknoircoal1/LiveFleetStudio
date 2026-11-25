package com.keiser.studio.parser

import android.bluetooth.le.ScanResult
import com.keiser.studio.model.BikeData
import com.keiser.studio.model.BikeProtocol

/**
 * Stages SC3 / SC2 / SB20 studio bikes stream live telemetry over Bluetooth SIG Cycling Power
 * GATT (0x1818 -> 0x2A63) and ANT+ Bicycle Power (Device Type 0x0B), not passive BLE ads.
 * Passive parse returns null so raw advertising bytes are never misread as live data.
 */
object StagesPacketParser : BikePacketParser {
    const val COMPANY_ID_STAGES = 0x0151
    override val protocol: BikeProtocol = BikeProtocol.STAGES
    override fun parse(result: ScanResult): BikeData? = null
}
