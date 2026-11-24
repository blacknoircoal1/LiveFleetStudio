package com.keiser.studio.parser

import android.bluetooth.le.ScanResult
import com.keiser.studio.model.BikeData
import com.keiser.studio.model.BikeProtocol
import kotlin.math.abs

/**
 * Decodes Schwinn AC Performance Plus bikes equipped with 4iiii Precision power cranks.
 * Uses Company ID 0x0182 (4iiii Innovations).
 */
object Schwinn4iiiiPacketParser : BikePacketParser {
    const val COMPANY_ID_4IIII = 0x0182

    override val protocol: BikeProtocol = BikeProtocol.SCHWINN_4IIII

    override fun parse(result: ScanResult): BikeData? {
        val record = result.scanRecord ?: return null
        val deviceName = record.deviceName ?: result.device?.name ?: ""
        val isSchwinnOr4iiii = deviceName.contains("schwinn", ignoreCase = true) ||
                deviceName.contains("4iiii", ignoreCase = true) ||
                deviceName.contains("precision", ignoreCase = true)

        val fourIiiiData = record.getManufacturerSpecificData(COMPANY_ID_4IIII)
        if (!isSchwinnOr4iiii && fourIiiiData == null) {
            return null
        }

        val raw = fourIiiiData ?: record.bytes ?: return null
        if (raw.size < 4) return null

        val bikeId = extractBikeIdFromName(deviceName) ?: deriveBikeIdFromAddress(result.device?.address ?: "")

        val pLo = raw[1].toInt() and 0xFF
        val pHi = raw[2].toInt() and 0xFF
        val powerWatts = ((pHi shl 8) or pLo).coerceIn(0, 2500)
        val cad = raw[3].toInt() and 0xFF
        val cadenceRpm = cad.toFloat().coerceIn(0f, 220f)

        return BikeData(
            bikeId = bikeId,
            brand = "Schwinn",
            powerWatts = powerWatts,
            cadenceRpm = cadenceRpm,
            riderName = "Schwinn #$bikeId",
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
