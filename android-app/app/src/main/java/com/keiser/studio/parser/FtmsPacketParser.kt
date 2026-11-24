package com.keiser.studio.parser

import android.bluetooth.le.ScanResult
import android.os.ParcelUuid
import com.keiser.studio.model.BikeData
import com.keiser.studio.model.BikeProtocol
import kotlin.math.abs

/**
 * Decodes standard Bluetooth FTMS (Fitness Machine Service, UUID 0x1826) indoor bikes:
 * Wattbike, Life Fitness IC5/IC6/IC7, Technogym Skillbike, Matrix, Body Bike Smart+, Wahoo, Tacx.
 */
object FtmsPacketParser : BikePacketParser {
    val UUID_FTMS: ParcelUuid = ParcelUuid.fromString("00001826-0000-1000-8000-00805f9b34fb")

    override val protocol: BikeProtocol = BikeProtocol.FTMS_UNIVERSAL

    override fun parse(result: ScanResult): BikeData? {
        val record = result.scanRecord ?: return null
        val deviceName = record.deviceName ?: result.device?.name ?: ""

        val serviceUuids = record.serviceUuids ?: emptyList()
        val hasFtmsService = serviceUuids.any { it.uuid == UUID_FTMS.uuid }
        val serviceData = record.getServiceData(UUID_FTMS)

        val isFtmsName = deviceName.contains("wattbike", ignoreCase = true) ||
                deviceName.contains("technogym", ignoreCase = true) ||
                deviceName.contains("ic5", ignoreCase = true) ||
                deviceName.contains("ic6", ignoreCase = true) ||
                deviceName.contains("ic7", ignoreCase = true) ||
                deviceName.contains("matrix", ignoreCase = true) ||
                deviceName.contains("body bike", ignoreCase = true) ||
                deviceName.contains("kickr", ignoreCase = true)

        if (!hasFtmsService && serviceData == null && !isFtmsName) {
            return null
        }

        val raw = serviceData ?: record.bytes ?: return null
        val bikeId = extractBikeIdFromName(deviceName) ?: deriveBikeIdFromAddress(result.device?.address ?: "")

        var powerWatts = 0
        var cadenceRpm = 0f

        // Standard FTMS Indoor Bike Data characteristic structure in service data:
        // [flags 16-bit, ... speed 16-bit, cadence 16-bit (0.5 RPM), power 16-bit (watts)]
        if (raw.size >= 8) {
            val flags = (raw[0].toInt() and 0xFF) or ((raw[1].toInt() and 0xFF) shl 8)
            var offset = 2
            // If More Data (bit 0) or Avg Speed Present etc.
            if ((flags and 0x01) == 0 && raw.size >= offset + 2) {
                // Instantaneous Speed present (2 bytes)
                offset += 2
            }
            if ((flags and 0x04) != 0 && raw.size >= offset + 2) {
                // Instantaneous Cadence present (2 bytes, 0.5 RPM)
                val cadRaw = (raw[offset].toInt() and 0xFF) or ((raw[offset + 1].toInt() and 0xFF) shl 8)
                cadenceRpm = cadRaw * 0.5f
                offset += 2
            }
            if ((flags and 0x40) != 0 && raw.size >= offset + 2) {
                // Instantaneous Power present (2 bytes, signed int)
                val pRaw = (raw[offset].toInt() and 0xFF) or ((raw[offset + 1].toInt() and 0xFF) shl 8)
                powerWatts = pRaw.coerceIn(0, 2500)
            }
        }

        val brand = when {
            deviceName.contains("wattbike", ignoreCase = true) -> "Wattbike"
            deviceName.contains("ic", ignoreCase = true) -> "LifeFitness"
            deviceName.contains("technogym", ignoreCase = true) -> "Technogym"
            else -> "FTMS"
        }

        return BikeData(
            bikeId = bikeId,
            brand = brand,
            powerWatts = powerWatts,
            cadenceRpm = cadenceRpm,
            riderName = "$brand #$bikeId",
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
