package com.keiser.studio.parser

import android.bluetooth.le.ScanResult
import com.keiser.studio.model.BikeData
import com.keiser.studio.model.BikeProtocol
import kotlin.math.abs

/**
 * Decodes Stages SC3 commercial studio bikes and Stages power meters.
 * Stages uses Company ID 0x0151 or standard BLE broadcast.
 */
object StagesPacketParser : BikePacketParser {
    const val COMPANY_ID_STAGES = 0x0151

    override val protocol: BikeProtocol = BikeProtocol.STAGES

    override fun parse(result: ScanResult): BikeData? {
        val record = result.scanRecord ?: return null
        val deviceName = record.deviceName ?: result.device?.name ?: ""
        val isStages = deviceName.contains("stages", ignoreCase = true) ||
                deviceName.contains("sc3", ignoreCase = true)

        val stagesData = record.getManufacturerSpecificData(COMPANY_ID_STAGES)
        if (!isStages && stagesData == null) {
            return null
        }

        val raw = stagesData ?: record.bytes ?: return null
        if (raw.size < 4) return null

        // Extract Bike ID from device name (e.g. "Stages 12" -> 12) or Bluetooth MAC hash (1..250)
        val bikeId = extractBikeIdFromName(deviceName) ?: deriveBikeIdFromAddress(result.device?.address ?: "")

        // Parse Power and Cadence from Stages broadcast packet
        // Stages format typically contains: [flags, powerLo, powerHi, cadence, ...]
        val powerWatts: Int
        val cadenceRpm: Float

        if (raw.size >= 4) {
            val pLo = raw[1].toInt() and 0xFF
            val pHi = raw[2].toInt() and 0xFF
            powerWatts = (pHi shl 8) or pLo
            val cad = raw[3].toInt() and 0xFF
            cadenceRpm = cad.toFloat()
        } else {
            powerWatts = 0
            cadenceRpm = 0f
        }

        return BikeData(
            bikeId = bikeId,
            brand = "Stages",
            powerWatts = powerWatts.coerceIn(0, 2500),
            cadenceRpm = cadenceRpm.coerceIn(0f, 220f),
            riderName = "Stages #$bikeId",
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
