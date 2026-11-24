package com.keiser.studio.parser

import android.bluetooth.le.ScanResult
import android.os.ParcelUuid
import com.keiser.studio.model.BikeData
import com.keiser.studio.model.BikeProtocol
import kotlin.math.abs

/**
 * Decodes standard Bluetooth Cycling Power Service (UUID 0x1818) & Cycling Speed/Cadence (UUID 0x1816)
 * used by Favero Assioma, Garmin Rally, Quarq, SRM, and power meter pedals/cranks.
 */
object PowerMeterPacketParser : BikePacketParser {
    val UUID_CYCLING_POWER: ParcelUuid = ParcelUuid.fromString("00001818-0000-1000-8000-00805f9b34fb")
    val UUID_CYCLING_CADENCE: ParcelUuid = ParcelUuid.fromString("00001816-0000-1000-8000-00805f9b34fb")

    override val protocol: BikeProtocol = BikeProtocol.POWER_METERS

    override fun parse(result: ScanResult): BikeData? {
        val record = result.scanRecord ?: return null
        val deviceName = record.deviceName ?: result.device?.name ?: ""

        val serviceUuids = record.serviceUuids ?: emptyList()
        val hasCps = serviceUuids.any { it.uuid == UUID_CYCLING_POWER.uuid || it.uuid == UUID_CYCLING_CADENCE.uuid }
        val serviceData = record.getServiceData(UUID_CYCLING_POWER)

        val isPowerMeterName = deviceName.contains("assioma", ignoreCase = true) ||
                deviceName.contains("rally", ignoreCase = true) ||
                deviceName.contains("quarq", ignoreCase = true) ||
                deviceName.contains("srm", ignoreCase = true) ||
                deviceName.contains("power", ignoreCase = true)

        if (!hasCps && serviceData == null && !isPowerMeterName) {
            return null
        }

        val raw = serviceData ?: record.bytes ?: return null
        val bikeId = extractBikeIdFromName(deviceName) ?: deriveBikeIdFromAddress(result.device?.address ?: "")

        var powerWatts = 0
        var cadenceRpm = 0f

        if (raw.size >= 4) {
            val pLo = raw[1].toInt() and 0xFF
            val pHi = raw[2].toInt() and 0xFF
            powerWatts = ((pHi shl 8) or pLo).coerceIn(0, 2500)
            if (raw.size >= 5) {
                cadenceRpm = (raw[3].toInt() and 0xFF).toFloat().coerceIn(0f, 220f)
            }
        }

        return BikeData(
            bikeId = bikeId,
            brand = "PowerMeter",
            powerWatts = powerWatts,
            cadenceRpm = cadenceRpm,
            riderName = "PM #$bikeId",
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
