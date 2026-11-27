package com.livefleet.studio.parser

import android.bluetooth.le.ScanResult
import com.livefleet.studio.model.BikeData
import com.livefleet.studio.model.BikeProtocol
import com.livefleet.studio.model.BrandType
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
    val UUID_DEVICE_INFO_SERVICE: UUID =
        UUID.fromString("CE060010-43E5-11E4-916C-0800200C9A66")
    val UUID_ERG_MACHINE_TYPE: UUID =
        UUID.fromString("CE060016-43E5-11E4-916C-0800200C9A66")
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
     * Passive BLE scan parser intentionally returns null for Concept2 PM5 advertisements so static
     * BLE advertising header bytes are never misread as live telemetry. Live Concept2 PM5 data
     * is streamed via [com.livefleet.studio.bluetooth.UniversalGattManager] (BLE GATT) or
     * [com.livefleet.studio.bluetooth.AntPlusUsbReceiver] (USB ANT+ FE-C for Group Classes).
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
     * Infers Concept2 machine subtype at BLE discovery time (Priority 2 fallback before GATT confirms):
     *  - Contains "ski"  -> "SKIERG"
     *  - Contains "row"  -> "ROWERG"
     *  - Contains "bike" -> "BIKEERG"
     *  - Plain "PM5 <Serial>" without a subtype suffix returns "" until GATT CE060016 / CE060031 confirms,
     *    preventing RowErg/SkiErg consoles from being mislabeled as BikeErg.
     */
    fun detectEquipmentType(deviceName: String): String {
        val trimmed = deviceName.trim()
        val lower = trimmed.lowercase()
        return when {
            lower.contains("skierg") || Regex("\\bski\\b").containsMatchIn(lower) || lower.contains("ski") -> "SKIERG"
            lower.contains("rowerg") || Regex("\\brow\\b").containsMatchIn(lower) || lower.contains("row") -> "ROWERG"
            lower.contains("bikeerg") || Regex("\\bbike\\b").containsMatchIn(lower) -> "BIKEERG"
            else -> ""
        }
    }

    /**
     * Maps Concept2 PM5 Erg Machine Type enum (OBJ_ERGMACHINETYPE_T from GATT CE060016 or CE060031 byte 18):
     *  - 0..15, 224 -> "ROWERG" (Static D/C/A/B/E, Dynamic, Rower Sim, MultiErg Row)
     *  - 128..143, 225 -> "SKIERG" (Static SkiErg = 128, Ski Sim = 143, MultiErg Ski = 225)
     *  - 192..207, 226 -> "BIKEERG" (BikeErg = 192, Bike Arms = 193, Bike NoArms = 194, Sim = 207)
     */
    fun equipmentTypeFromPm5Code(code: Int): String = when (code) {
        in 0..15, 224 -> "ROWERG"
        in 128..143, 225 -> "SKIERG"
        in 192..207, 226 -> "BIKEERG"
        else -> ""
    }

    /**
     * Decodes Concept2 Erg Machine Type GATT characteristic (CE060016-43E5-11E4-916C-0800200C9A66).
     */
    fun parseErgMachineType(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""
        return equipmentTypeFromPm5Code(bytes[0].toInt() and 0xFF)
    }

    /**
     * Decodes Concept2 PM5 General Status (CE060031, 19 bytes).
     * Bytes 3..5: Accumulated distance (uint24 LE, 0.1 m units) -> divide by 10 and truncate to Int metres.
     * Byte 18 (if size >= 19): Erg Machine Type (0..4=ROWERG, 5..7=SKIERG, 8/192=BIKEERG).
     */
    fun parseGeneralStatus(bytes: ByteArray, current: BikeData): BikeData {
        if (bytes.size < 6) return current
        val distTenthsMeters = (bytes[3].toInt() and 0xFF) or
            ((bytes[4].toInt() and 0xFF) shl 8) or
            ((bytes[5].toInt() and 0xFF) shl 16)
        val wholeMeters = (distTenthsMeters / 10).coerceAtLeast(0)
        val detectedFromByte = if (bytes.size >= 19) {
            equipmentTypeFromPm5Code(bytes[18].toInt() and 0xFF)
        } else {
            ""
        }
        val equipmentType = if (detectedFromByte.isNotEmpty()) detectedFromByte else current.equipmentType
        return current.copy(
            distanceMeters = wholeMeters,
            distance = wholeMeters / 1000f,
            distanceUnit = "M",
            equipmentType = equipmentType,
            brandType = BrandType.CONCEPT2,
            lastSeenTimestamp = System.currentTimeMillis(),
            isStale = false
        )
    }

    /**
     * Decodes Concept2 PM5 Additional Status 1 (CE060032, up to 19 bytes):
     *  - Bytes 3..4: Speed (uint16 LE, 0.001 m/s)
     *  - Byte 5:     Stroke Rate / Cadence (uint8, s/m or RPM)
     *  - Byte 6:     Heart Rate (uint8, bpm, 255 = invalid)
     *  - Bytes 7..8: Current Pace (uint16 LE, 0.01s per 500m for Row/Ski or per 1000m for BikeErg)
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
        val equipmentType = if (detectedFromByte.isNotEmpty()) {
            detectedFromByte
        } else {
            current.equipmentType
        }

        return current.copy(
            cadenceRpm = strokeRate.toFloat().coerceIn(0f, 220f),
            heartRateBpm = hrBpm,
            powerWatts = liveWatts,
            liveSplitSeconds = paceSec,
            equipmentType = equipmentType,
            brandType = BrandType.CONCEPT2,
            lastSeenTimestamp = System.currentTimeMillis(),
            isStale = false
        )
    }

    /**
     * Decodes Concept2 PM5 Additional Status 2 (CE060033, up to 18 bytes):
     *  - Bytes 6..7: Total Calories (uint16 LE, whole cumulative kcal)
     */
    fun parseAdditionalStatus2(bytes: ByteArray, current: BikeData): BikeData {
        if (bytes.size < 8) return current
        val totalCalories = (bytes[6].toInt() and 0xFF) or ((bytes[7].toInt() and 0xFF) shl 8)
        return current.copy(
            calories = totalCalories.coerceIn(0, 99999),
            brandType = BrandType.CONCEPT2,
            lastSeenTimestamp = System.currentTimeMillis(),
            isStale = false
        )
    }
}
