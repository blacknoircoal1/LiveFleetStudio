package com.livefleet.studio.parser

import android.bluetooth.le.ScanResult
import com.livefleet.studio.model.BikeData
import com.livefleet.studio.model.BikeProtocol

/**
 * Universal multi-protocol parser router.
 * Routes BLE scan packets to the selected parser or auto-detects across all supported brands.
 */
object UniversalBikeParser {

    private val allParsers: List<BikePacketParser> = listOf(
        KeiserPacketParser,
        StagesPacketParser,
        Schwinn4iiiiPacketParser,
        Concept2PacketParser,
        FtmsPacketParser,
        PowerMeterPacketParser
    )

    fun parse(result: ScanResult, protocol: BikeProtocol): BikeData? {
        return when (protocol) {
            BikeProtocol.AUTO_DETECT -> {
                // Try each brand parser in sequence until one matches
                for (parser in allParsers) {
                    val parsed = parser.parse(result)
                    if (parsed != null) return parsed
                }
                null
            }
            BikeProtocol.KEISER -> KeiserPacketParser.parse(result)
            BikeProtocol.STAGES -> StagesPacketParser.parse(result)
            BikeProtocol.SCHWINN_4IIII -> Schwinn4iiiiPacketParser.parse(result)
            BikeProtocol.CONCEPT2 -> Concept2PacketParser.parse(result)
            BikeProtocol.FTMS_UNIVERSAL -> FtmsPacketParser.parse(result)
            BikeProtocol.POWER_METERS -> PowerMeterPacketParser.parse(result)
        }
    }
}
