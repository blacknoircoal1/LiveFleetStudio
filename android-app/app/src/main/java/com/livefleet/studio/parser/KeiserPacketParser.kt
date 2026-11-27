package com.livefleet.studio.parser

import android.bluetooth.le.ScanResult
import com.livefleet.studio.model.BikeData
import com.livefleet.studio.model.BikeProtocol
import com.livefleet.studio.model.BrandType

object KeiserPacketParser : BikePacketParser {
    const val COMPANY_ID_KEISER = 0x0102

    override val protocol: BikeProtocol = BikeProtocol.KEISER

    /**
     * Layer 1 — Strict Keiser Passive BLE Signature Gatekeeper:
     *  - Passive BLE broadcast ONLY (never opens GATT).
     *  - Accept ONLY when Manufacturer Specific Data for Company ID 0x0102 is present
     *    AND total manufacturer payload length is >= 17 bytes (which Android's
     *    ScanRecord.getManufacturerSpecificData strips by 2 Company ID bytes -> >= 15 bytes).
     */
    override fun parse(result: ScanResult): BikeData? {
        val record = result.scanRecord ?: return null
        val keiserData = record.getManufacturerSpecificData(COMPANY_ID_KEISER) ?: return null
        if (keiserData.size < 15) return null

        val parsed = parse(keiserData) ?: return null
        return parsed.copy(
            brand = BrandType.KEISER.canonicalBrandName,
            brandType = BrandType.KEISER,
            macAddress = result.device?.address ?: "",
            rssi = result.rssi
        )
    }

    /**
     * Parses Keiser M3i / M3i Studio advertisement payload.
     * On Android ScanRecord.getManufacturerSpecificData(0x0102), the 2-byte company ID is stripped,
     * so Firmware Major is at index 0 and Bike ID is at index 3 (>= 15 bytes stripped == >= 17 bytes raw).
     */
    fun parse(bytes: ByteArray?): BikeData? {
        if (bytes == null || bytes.size < 15) return null

        val offset = if (bytes.size >= 17 &&
            ((bytes[0].toInt() and 0xFF) or ((bytes[1].toInt() and 0xFF) shl 8)) == COMPANY_ID_KEISER
        ) {
            2
        } else {
            0
        }

        if (bytes.size < offset + 15) return null

        val fwMajor = bytes[offset + 0].toInt() and 0xFF
        val fwMinor = bytes[offset + 1].toInt() and 0xFF
        val dataType = bytes[offset + 2].toInt() and 0xFF
        val bikeId = bytes[offset + 3].toInt() and 0xFF

        // Validate real-time data (0 = main session, 128..255 = real-time during intervals) and bike ID (1..250)
        if (dataType != 0 && dataType < 128) return null
        if (bikeId !in 1..250) return null

        val cadenceRaw = (bytes[offset + 4].toInt() and 0xFF) or ((bytes[offset + 5].toInt() and 0xFF) shl 8)
        val cadenceRpm = cadenceRaw / 10.0f

        val hrRaw = (bytes[offset + 6].toInt() and 0xFF) or ((bytes[offset + 7].toInt() and 0xFF) shl 8)
        val heartRateBpm = hrRaw / 10.0f

        val powerWatts = (bytes[offset + 8].toInt() and 0xFF) or ((bytes[offset + 9].toInt() and 0xFF) shl 8)
        val calories = (bytes[offset + 10].toInt() and 0xFF) or ((bytes[offset + 11].toInt() and 0xFF) shl 8)

        val distRaw = (bytes[offset + 12].toInt() and 0xFF) or ((bytes[offset + 13].toInt() and 0xFF) shl 8)
        val isKm = (distRaw and 0x8000) != 0
        val distance = (distRaw and 0x7FFF) / 10.0f
        val distanceUnit = if (isKm) "KM" else "MI"

        val rawGear = bytes[offset + 14].toInt() and 0xFF
        val gear = rawGear.coerceIn(1, 24)

        return BikeData(
            bikeId = bikeId,
            fwMajor = fwMajor,
            fwMinor = fwMinor,
            dataType = dataType,
            cadenceRpm = Math.round(cadenceRpm * 10f) / 10f,
            heartRateBpm = Math.round(heartRateBpm * 10f) / 10f,
            powerWatts = powerWatts,
            calories = calories,
            distance = Math.round(distance * 10f) / 10f,
            distanceMeters = Math.round(distance * if (isKm) 1000f else 1609.34f),
            distanceUnit = distanceUnit,
            gear = gear,
            brand = BrandType.KEISER.canonicalBrandName,
            brandType = BrandType.KEISER,
            lastSeenTimestamp = System.currentTimeMillis(),
            isStale = false
        )
    }
}
