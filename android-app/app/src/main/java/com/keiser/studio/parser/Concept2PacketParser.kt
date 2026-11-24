package com.keiser.studio.parser

import android.bluetooth.le.ScanResult
import com.keiser.studio.model.BikeData
import com.keiser.studio.model.BikeProtocol
import kotlin.math.abs

/**
 * Decodes Concept2 PM5 monitor broadcasts on BikeErg, RowErg, and SkiErg.
 * Uses Company ID 0x00CE (Concept2, Inc.) or standard PM5 broadcast beacon.
 */
object Concept2PacketParser : BikePacketParser {
    const val COMPANY_ID_CONCEPT2 = 0x00CE

    override val protocol: BikeProtocol = BikeProtocol.CONCEPT2

    override fun parse(result: ScanResult): BikeData? {
        val record = result.scanRecord ?: return null
        val deviceName = record.deviceName ?: result.device?.name ?: ""
        val isC2 = deviceName.contains("pm5", ignoreCase = true) ||
                deviceName.contains("concept2", ignoreCase = true) ||
                deviceName.contains("bikeerg", ignoreCase = true)

        val c2Data = record.getManufacturerSpecificData(COMPANY_ID_CONCEPT2)
        if (!isC2 && c2Data == null) {
            return null
        }

        val raw = c2Data ?: record.bytes ?: return null
        val bikeId = extractBikeIdFromName(deviceName) ?: deriveBikeIdFromAddress(result.device?.address ?: "")

        var powerWatts = 0
        var cadenceRpm = 0f

        if (raw.size >= 5) {
            // C2 General Status / Power Payload
            val wLo = raw[2].toInt() and 0xFF
            val wHi = raw[3].toInt() and 0xFF
            powerWatts = ((wHi shl 8) or wLo).coerceIn(0, 2500)
            val spm = raw[4].toInt() and 0xFF
            cadenceRpm = spm.toFloat().coerceIn(0f, 220f)
        }

        return BikeData(
            bikeId = bikeId,
            brand = "Concept2",
            powerWatts = powerWatts,
            cadenceRpm = cadenceRpm,
            riderName = "PM5 #$bikeId",
            lastSeenTimestamp = System.currentTimeMillis(),
            isStale = false
        )
    }

    private fun extractBikeIdFromName(name: String): Int? {
        val regex = Regex("\\b(\\d{1,3})\\b")
        val match = regex.find(name)
        val num = match?.groupValues?.get(1)?.toIntOrNull()
        return if (num != null && num in 1..250) num else null
    }

    private fun deriveBikeIdFromAddress(address: String): Int {
        if (address.isEmpty()) return 1
        return (abs(address.hashCode()) % 240) + 1
    }
}
