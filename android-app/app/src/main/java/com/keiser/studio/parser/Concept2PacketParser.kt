package com.keiser.studio.parser

import android.bluetooth.le.ScanResult
import com.keiser.studio.model.BikeData
import com.keiser.studio.model.BikeProtocol
import java.util.UUID
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Concept2 PM5 identification and BLE GATT telemetry decoder (BikeErg, RowErg, SkiErg).
 *
 * NOTE: Concept2 PM5 monitors do NOT broadcast live watts/pace/stroke-rate in passive BLE
 * advertising packets — they advertise presence ("PM5 <serial> Row/Ski/Bike") and stream
 * live metrics over BLE GATT Rowing Service (CE060030-43E5-11E4-916C-0800200C9A66).
 */
object Concept2PacketParser : BikePacketParser {
    const val COMPANY_ID_CONCEPT2 = 0x00CE

    val UUID_PM5_DISCOVERY_SERVICE: UUID =
        UUID.fromString("CE060000-43E5-11E4-916C-0800200C9A66")
    val UUID_ROWING_SERVICE: UUID =
        UUID.fromString("CE060030-43E5-11E4-916C-0800200C9A66")
    val UUID_GENERAL_STATUS: UUID =
        UUID.fromString("CE060031-43E5-11E4-916C-0800200C9A66")
    val UUID_ADDITIONAL_STATUS_1: UUID =
        UUID.fromString("CE060032-43E5-11E4-916C-0800200C9A66")
    val UUID_ADDITIONAL_STATUS_2: UUID =
        UUID.fromString("CE060033-43E5-11E4-916C-0800200C9A66")
    val UUID_CCCD: UUID =
        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    override val protocol: BikeProtocol = BikeProtocol.CONCEPT2

    /**
     * Checks whether a BLE ScanResult is a Concept2 PM5 monitor.
     */
    fun isConcept2Device(result: ScanResult): Boolean {
        val record = result.scanRecord ?: return false
        val deviceName = record.deviceName ?: result.device?.name ?: ""
        val matchesName = deviceName.contains("pm5", ignoreCase = true) ||
            deviceName.contains("concept2", ignoreCase = true) ||
            deviceName.contains("bikeerg", ignoreCase = true) ||
            deviceName.contains("rowerg", ignoreCase = true) ||
            deviceName.contains("skierg", ignoreCase = true)
        if (matchesName) return true

        val serviceUuids = record.serviceUuids ?: emptyList()
        if (serviceUuids.any { it.uuid == UUID_PM5_DISCOVERY_SERVICE || it.uuid == UUID_ROWING_SERVICE }) {
            return true
        }
        return record.getManufacturerSpecificData(COMPANY_ID_CONCEPT2) != null
    }

    /**
     * Passive BLE scan parser intentionally returns null for Concept2 PM5 advertisements so static
     * BLE advertising header bytes are never misread as live telemetry. Live Concept2 PM5 data
     * is streamed via [com.keiser.studio.bluetooth.Concept2GattManager] (BLE GATT) or
     * [com.keiser.studio.bluetooth.AntPlusUsbReceiver] (USB ANT+ FE-C for Group Classes).
     */
    override fun parse(result: ScanResult): BikeData? = null

    /**
     * Extracts the last 5 digits of the PM5 serial number from a BLE name such as
     * "PM5 530339622 Row" -> "39622". Falls back to a stable 5-digit ID derived from MAC address.
     */
    fun extractPm5DisplayId(deviceName: String, macAddress: String): String {
        val longNumber = Regex("(\\d{5,})").find(deviceName)?.groupValues?.get(1)
        if (longNumber != null) {
            return longNumber.takeLast(5)
        }
        val shortNumber = Regex("\\b(\\d{1,4})\\b").find(deviceName)?.groupValues?.get(1)
        if (shortNumber != null) {
            return shortNumber
        }
        val cleanMac = macAddress.replace(":", "")
        if (cleanMac.length >= 4) {
            return (abs(cleanMac.hashCode()) % 90000 + 10000).toString()
        }
        return "00001"
    }

    /**
     * Converts a PM5 display ID (e.g. "39622") into a stable positive integer bikeId that does
     * not collide with standard studio bike numbers 1..250.
     */
    fun toStableBikeId(displayId: String, macAddress: String): Int {
        val numeric = displayId.toIntOrNull()
        if (numeric != null && numeric > 0) {
            return if (numeric <= 250) 10000 + numeric else numeric
        }
        return 10000 + (abs(macAddress.hashCode()) % 89999)
    }

    /**
     * Infers the Concept2 erg model ("BIKEERG", "ROWERG", "SKIERG", or "" if unspecified)
     * from the PM5 BLE advertisement name (e.g. "PM5 530339622 Row", "PM5 430012345 Bike",
     * "PM5 430012345 Ski", "BikeErg", "RowErg", "SkiErg").
     */
    fun detectEquipmentType(deviceName: String): String {
        val lower = deviceName.lowercase()
        return when {
            lower.contains("bikeerg") || Regex("\\bbike\\b").containsMatchIn(lower) -> "BIKEERG"
            lower.contains("rowerg") || Regex("\\brow\\b").containsMatchIn(lower) -> "ROWERG"
            lower.contains("skierg") || Regex("\\bski\\b").containsMatchIn(lower) -> "SKIERG"
            else -> ""
        }
    }

    /**
     * Maps Concept2 PM5 Erg Machine Type enum (byte 18 of CE060032) to our equipmentType label.
     * Official Concept2 values:
     *   0..5, 7, 8 = Model A-E / Dynamic / Slides RowErg
     *   16..18     = SkiErg
     *   192        = BikeErg
     */
    fun equipmentTypeFromPm5Code(code: Int): String = when (code) {
        192 -> "BIKEERG"
        in 16..18 -> "SKIERG"
        in 0..8 -> "ROWERG"
        else -> ""
    }

    /**
     * Decodes Concept2 PM5 General Status (CE060031, 19 bytes).
     * Updates distance (meters -> km) while preserving live metrics.
     */
    fun parseGeneralStatus(bytes: ByteArray, current: BikeData): BikeData {
        if (bytes.size < 6) return current
        val distTenthsMeters = (bytes[3].toInt() and 0xFF) or
            ((bytes[4].toInt() and 0xFF) shl 8) or
            ((bytes[5].toInt() and 0xFF) shl 16)
        val distMeters = distTenthsMeters / 10.0f
        return current.copy(
            distance = (distMeters / 1000f * 100f).roundToInt() / 100f,
            distanceUnit = "KM",
            lastSeenTimestamp = System.currentTimeMillis(),
            isStale = false
        )
    }

    /**
     * Decodes Concept2 PM5 Additional Status 1 (CE060032, up to 19 bytes):
     *  - Bytes 3..4: Speed (uint16, 0.001 m/s)
     *  - Byte 5:     Stroke Rate / Cadence (uint8, s/m or RPM)
     *  - Byte 6:     Heart Rate (uint8, bpm, 255 = invalid)
     *  - Bytes 7..8: Current Pace (uint16, 0.01s per 500m for Row/Ski or per 1000m for BikeErg)
     *  - Bytes 16..17: Total Average Power (uint16, Watts) [optional, if size >= 18]
     *  - Byte 18:    Erg Machine Type [optional, if size >= 19]
     */
    fun parseAdditionalStatus1(bytes: ByteArray, current: BikeData): BikeData {
        if (bytes.size < 9) return current

        val speedRaw = (bytes[3].toInt() and 0xFF) or ((bytes[4].toInt() and 0xFF) shl 8)
        val strokeRate = bytes[5].toInt() and 0xFF
        val hrRaw = bytes[6].toInt() and 0xFF
        val paceHundredths = (bytes[7].toInt() and 0xFF) or ((bytes[8].toInt() and 0xFF) shl 8)

        val hrBpm = if (hrRaw in 30..230) hrRaw.toFloat() else current.heartRateBpm

        // Current Pace is in 0.01s increments (0 or 0xFFFF means stopped/no pace)
        val paceSec = if (paceHundredths in 1..60000) paceHundredths / 100.0f else null

        // Derive instantaneous watts directly from Concept2 official equation:
        //   watts = 2.80 / (splitSeconds / 500)^3
        // Falls back to flywheel speed (watts = 2.80 * speed^3) if pace is zero while speed > 0.
        val liveWatts = when {
            paceSec != null && paceSec > 10f -> {
                val pacePerMeter = paceSec.toDouble() / 500.0
                (2.80 / pacePerMeter.pow(3.0)).roundToInt().coerceIn(0, 2500)
            }
            speedRaw > 0 -> {
                val speedMps = speedRaw / 1000.0
                (2.80 * speedMps.pow(3.0)).roundToInt().coerceIn(0, 2500)
            }
            else -> 0
        }

        val detectedFromByte = if (bytes.size >= 19) {
            equipmentTypeFromPm5Code(bytes[18].toInt() and 0xFF)
        } else {
            ""
        }
        val equipmentType = current.equipmentType.ifEmpty { detectedFromByte }

        return current.copy(
            cadenceRpm = strokeRate.toFloat().coerceIn(0f, 220f),
            heartRateBpm = hrBpm,
            powerWatts = liveWatts,
            liveSplitSeconds = paceSec,
            equipmentType = equipmentType,
            lastSeenTimestamp = System.currentTimeMillis(),
            isStale = false
        )
    }

    /**
     * Decodes Concept2 PM5 Additional Status 2 (CE060033, up to 18 bytes):
     *  - Bytes 4..5: Total Calories (uint16, cumulative kCal)
     *  - Bytes 8..9: Split/Interval Avg Power (uint16, Watts)
     */
    fun parseAdditionalStatus2(bytes: ByteArray, current: BikeData): BikeData {
        if (bytes.size < 6) return current
        val totalCalories = (bytes[4].toInt() and 0xFF) or ((bytes[5].toInt() and 0xFF) shl 8)
        return current.copy(
            calories = totalCalories.coerceIn(0, 99999),
            lastSeenTimestamp = System.currentTimeMillis(),
            isStale = false
        )
    }
}
