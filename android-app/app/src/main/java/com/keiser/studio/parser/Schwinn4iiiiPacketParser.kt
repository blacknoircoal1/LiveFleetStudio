package com.keiser.studio.parser

import android.bluetooth.le.ScanResult
import com.keiser.studio.model.BikeData
import com.keiser.studio.model.BikeProtocol

/**
 * Schwinn AC Performance Plus / Carbon Blue (MPower Echelon2 & 4iiii Precision) stream live
 * telemetry over ANT+ Bicycle Power (0x0B) / ANT+ FE-C (0x11) and 4iiii BLE GATT Cycling Power
 * (0x1818 -> 0x2A63), not passive BLE ads. Passive parse returns null so raw advertising bytes
 * are never misread as live data.
 */
object Schwinn4iiiiPacketParser : BikePacketParser {
    const val COMPANY_ID_4IIII = 0x0182
    override val protocol: BikeProtocol = BikeProtocol.SCHWINN_4IIII
    override fun parse(result: ScanResult): BikeData? = null
}
