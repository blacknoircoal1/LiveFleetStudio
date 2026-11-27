package com.livefleet.studio.parser

import android.bluetooth.le.ScanResult
import com.livefleet.studio.model.BikeData
import com.livefleet.studio.model.BikeProtocol
import com.livefleet.studio.model.BrandType
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Layer 1 Brand Gatekeeper & Bluetooth SIG FTMS (0x1826 -> 0x2AD2 / 0x2AD1) +
 * Cycling Power Service (0x1818 -> 0x2A63) parser + serial ID utilities.
 */
object FtmsPacketParser : BikePacketParser {
    val UUID_FTMS_SERVICE: UUID = UUID.fromString("00001826-0000-1000-8000-00805f9b34fb")
    val UUID_INDOOR_BIKE_DATA: UUID = UUID.fromString("00002AD2-0000-1000-8000-00805f9b34fb")
    val UUID_ROWER_DATA: UUID = UUID.fromString("00002AD1-0000-1000-8000-00805f9b34fb")
    val UUID_FTMS_CONTROL_POINT: UUID = UUID.fromString("00002AD9-0000-1000-8000-00805f9b34fb")
    val UUID_CYCLING_POWER_SERVICE: UUID = UUID.fromString("00001818-0000-1000-8000-00805f9b34fb")
    val UUID_CYCLING_POWER_MEASUREMENT: UUID = UUID.fromString("00002A63-0000-1000-8000-00805f9b34fb")
    val UUID_CYCLING_SPEED_CADENCE_SERVICE: UUID = UUID.fromString("00001816-0000-1000-8000-00805f9b34fb")
    val UUID_CSC_MEASUREMENT: UUID = UUID.fromString("00002A5B-0000-1000-8000-00805f9b34fb")
    val UUID_WATTBIKE_SERVICE: UUID = UUID.fromString("b4cc1223-bc02-4cae-adb9-1217ad286353")

    override val protocol: BikeProtocol = BikeProtocol.FTMS_UNIVERSAL

    /**
     * Passive BLE scan parser intentionally returns null for FTMS / Cycling Power devices so static
     * BLE advertising header bytes are never misread as live telemetry.
     */
    override fun parse(result: ScanResult): BikeData? = null

    data class DiscoveredGattMeta(
        val brand: String,
        val brandType: BrandType,
        val equipmentType: String,
        val displayId: String,
        val stableBikeId: Int,
        val matchesProtocol: Boolean,
        val isPrimaryConsole: Boolean = true
    )

    /**
     * Layer 1 — Strict Brand Signature Matching before opening any GATT connection:
     *  - Drops packets with no valid name, service UUID, or manufacturer ID.
     *  - KEISER: passive broadcast only (never opens GATT).
     *  - CONCEPT2: advertisedName startsWith("PM5") or contains("Concept2") or PM5 UUID (CE060000/CE060030).
     *  - STAGES: advertisedName startsWith("Stages") or manufacturer ID 0x0264.
     *  - WATTBIKE / FTMS: advertisedName startsWith("Wattbike"/"AIR-PRO") or contains("Atom") or
     *    Wattbike UUID (b4cc1223-...) or FTMS 0x1826, while explicitly rejecting "PM5", "Concept2",
     *    "Stages", and "M3" even if they advertise FTMS (0x1826) or CPS (0x1818).
     */
    fun classifyGattDevice(
        deviceName: String,
        macAddress: String,
        serviceUuids: List<UUID>,
        hasConcept2MfrData: Boolean,
        hasStagesMfrData: Boolean,
        has4iiiiMfrData: Boolean,
        protocol: BikeProtocol,
        hasKeiserMfrData: Boolean = false
    ): DiscoveredGattMeta? {
        val trimmedName = deviceName.trim()
        val hasAnyMfrData = hasConcept2MfrData || hasStagesMfrData || has4iiiiMfrData || hasKeiserMfrData
        if (trimmedName.isEmpty() && serviceUuids.isEmpty() && !hasAnyMfrData) {
            return null
        }

        // KEISER is passive BLE broadcast ONLY — never open a GATT connection.
        if (hasKeiserMfrData || trimmedName.startsWith("M3", ignoreCase = true)) {
            return null
        }

        val lower = trimmedName.lowercase()
        // Reject non-cycling gym floor cardio equipment (treadmills, ellipticals, stair climbers)
        // that also advertise FTMS 0x1826 (e.g. Life Fitness cardio floor) to avoid ghost connections.
        val isNonCyclingCardio = lower.contains("treadmill") ||
            lower.contains("elliptical") ||
            lower.contains("crosstrainer") ||
            lower.contains("cross trainer") ||
            lower.contains("arc trainer") ||
            lower.contains("climber") ||
            lower.contains("powermill") ||
            lower.contains("stair") ||
            lower.contains("symbio") ||
            lower.contains("discover")
        if (isNonCyclingCardio) {
            return null
        }
        val hasPm5Service = serviceUuids.any {
            it == Concept2PacketParser.UUID_PM5_DISCOVERY_SERVICE ||
                it == Concept2PacketParser.UUID_ROWING_SERVICE
        }
        val hasWattbikeUuid = serviceUuids.any { it == UUID_WATTBIKE_SERVICE }
        val hasFtmsService = serviceUuids.any { it == UUID_FTMS_SERVICE }
        val hasCpsService = serviceUuids.any {
            it == UUID_CYCLING_POWER_SERVICE || it == UUID_CYCLING_SPEED_CADENCE_SERVICE
        }

        // Strict Layer 1 brand signatures
        val isConcept2 = trimmedName.startsWith("PM5", ignoreCase = true) ||
            lower.contains("concept2") ||
            hasPm5Service ||
            hasConcept2MfrData

        val isStages = trimmedName.startsWith("Stages", ignoreCase = true) ||
            trimmedName.startsWith("SIC", ignoreCase = true) ||
            trimmedName.startsWith("SC3", ignoreCase = true) ||
            trimmedName.startsWith("SC2", ignoreCase = true) ||
            hasStagesMfrData

        val hasExcludedBrandForWattbike = trimmedName.startsWith("PM5", ignoreCase = true) ||
            lower.contains("pm5") ||
            lower.contains("concept2") ||
            trimmedName.startsWith("Stages", ignoreCase = true) ||
            lower.contains("stages") ||
            trimmedName.startsWith("M3", ignoreCase = true) ||
            isConcept2 ||
            isStages

        val isWattbike = !hasExcludedBrandForWattbike && (
            trimmedName.startsWith("Wattbike", ignoreCase = true) ||
                trimmedName.startsWith("AIR-PRO", ignoreCase = true) ||
                lower.contains("atom") ||
                hasWattbikeUuid
            )

        val isSchwinn = !isConcept2 && !isStages && !isWattbike && (
            has4iiiiMfrData ||
                lower.contains("schwinn") ||
                lower.contains("4iiii") ||
                lower.contains("precision") ||
                lower.contains("echelon")
            )

        val isOtherFtmsBrand = !hasExcludedBrandForWattbike && !isSchwinn && (
            lower.contains("technogym") || lower.contains("skillbike") ||
                lower.contains("ic5") || lower.contains("ic6") || lower.contains("ic7") ||
                lower.contains("ic8") || lower.contains("life fitness") || lower.contains("icg") ||
                lower.contains("matrix") || lower.contains("body bike") || lower.contains("kickr") ||
                lower.contains("tacx")
            )

        val isPowerPedalBrand = !hasExcludedBrandForWattbike && !isSchwinn && !isWattbike && (
            lower.contains("assioma") || lower.contains("rally") ||
                lower.contains("quarq") || lower.contains("srm") || lower.contains("powertap") ||
                lower.contains("vector") || lower.contains("favero")
            )

        val (brand, brandType) = when {
            isConcept2 -> "Concept2" to BrandType.CONCEPT2
            isStages -> "Stages" to BrandType.STAGES
            isSchwinn -> "Schwinn" to BrandType.SCHWINN
            isWattbike -> "Wattbike" to BrandType.WATTBIKE
            lower.contains("ic5") || lower.contains("ic6") || lower.contains("ic7") ||
                lower.contains("ic8") || lower.contains("life fitness") || lower.contains("icg") ->
                "LifeFitness" to BrandType.FTMS
            lower.contains("technogym") || lower.contains("skillbike") -> "Technogym" to BrandType.FTMS
            lower.contains("matrix") -> "Matrix" to BrandType.FTMS
            lower.contains("body bike") -> "BodyBike" to BrandType.FTMS
            isPowerPedalBrand || (!hasFtmsService && hasCpsService && !hasExcludedBrandForWattbike) ->
                "PowerMeter" to BrandType.POWER_METER
            hasFtmsService && !hasExcludedBrandForWattbike -> "FTMS" to BrandType.FTMS
            else -> return null
        }

        val matches = when (protocol) {
            BikeProtocol.AUTO_DETECT -> true
            BikeProtocol.KEISER -> false
            BikeProtocol.CONCEPT2 -> isConcept2
            BikeProtocol.STAGES -> isStages
            BikeProtocol.SCHWINN_4IIII -> isSchwinn
            BikeProtocol.FTMS_UNIVERSAL -> !hasExcludedBrandForWattbike && (isWattbike || isOtherFtmsBrand || hasFtmsService)
            BikeProtocol.POWER_METERS -> !hasExcludedBrandForWattbike && !isWattbike && (isPowerPedalBrand || hasCpsService || isSchwinn)
        }
        if (!matches) return null

        val equipmentType = if (brandType == BrandType.CONCEPT2) {
            Concept2PacketParser.detectEquipmentType(trimmedName)
        } else {
            ""
        }

        val displayId = extractSerialDisplayId(trimmedName, macAddress)
        val stableId = if (brandType == BrandType.CONCEPT2) {
            Concept2PacketParser.toStableBikeId(displayId, macAddress)
        } else {
            toStableBikeId(displayId, macAddress)
        }
        val isPrimaryConsole = if (brandType == BrandType.STAGES) {
            stableId in 1..999
        } else {
            true
        }

        return DiscoveredGattMeta(
            brand = brand,
            brandType = brandType,
            equipmentType = equipmentType,
            displayId = displayId,
            stableBikeId = stableId,
            matchesProtocol = true,
            isPrimaryConsole = isPrimaryConsole
        )
    }

    /**
     * Extracts a human-readable machine ID from the BLE device name:
     *  - 5+ digit serial (e.g. "AIR-PRO 34002379", "PM5 530339622 Row") -> last 5 digits ("02379", "39622")
     *  - 1..4 digit studio bike number (e.g. "Stages 008", "Stages 12") -> "8", "12" (displayed as #8, #12)
     *  - Fallback -> stable 5-digit ID derived from Bluetooth MAC address
     */
    fun extractSerialDisplayId(deviceName: String, macAddress: String): String {
        val longSerial = Regex("(\\d{5,})").find(deviceName)?.groupValues?.get(1)
        if (longSerial != null) {
            return longSerial.takeLast(5)
        }
        val shortNum = Regex("\\b(\\d{1,4})\\b").find(deviceName)?.groupValues?.get(1)
        if (shortNum != null) {
            return shortNum.trimStart('0').ifEmpty { shortNum }
        }
        val cleanMac = macAddress.replace(":", "")
        if (cleanMac.length >= 4) {
            return (abs(cleanMac.hashCode()) % 90000 + 10000).toString()
        }
        return "00001"
    }

    fun toStableBikeId(displayId: String, macAddress: String): Int {
        val numeric = displayId.toIntOrNull()
        if (numeric != null && numeric > 0) {
            return numeric
        }
        return 10000 + (abs(macAddress.hashCode()) % 89999)
    }

    /**
     * Decodes official Bluetooth SIG FTMS Indoor Bike Data characteristic (UUID 0x2AD2).
     */
    fun parseIndoorBikeData(bytes: ByteArray, current: BikeData): BikeData {
        if (bytes.size < 2) return current

        val flags = (bytes[0].toInt() and 0xFF) or ((bytes[1].toInt() and 0xFF) shl 8)
        var offset = 2

        var cadenceRpm = current.cadenceRpm
        var speedKmh = current.speedKmh
        var liveSplitSeconds = current.liveSplitSeconds
        var distanceKm = current.distance
        var distanceMeters = current.distanceMeters
        var distanceUnit = current.distanceUnit
        var powerWatts = current.powerWatts
        var avgWatts = current.avgWatts
        var calories = current.calories
        var caloriesPerHour = current.caloriesPerHour
        var heartRate = current.heartRateBpm

        // Bit 0 == 0: Instantaneous Speed present (uint16, 0.01 km/h)
        if ((flags and 0x0001) == 0 && bytes.size >= offset + 2) {
            val rawSpeed = (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)
            speedKmh = (rawSpeed * 0.01f).coerceIn(0f, 120f)
            if (speedKmh > 0.5f) {
                liveSplitSeconds = 3600f / speedKmh
            } else if (powerWatts == 0) {
                liveSplitSeconds = null
            }
            offset += 2
        }
        // Bit 1: Average Speed present (uint16)
        if ((flags and 0x0002) != 0 && bytes.size >= offset + 2) {
            offset += 2
        }
        // Bit 2: Instantaneous Cadence present (uint16, 0.5 RPM)
        if ((flags and 0x0004) != 0 && bytes.size >= offset + 2) {
            val rawCad = (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)
            cadenceRpm = (rawCad * 0.5f).coerceIn(0f, 220f)
            offset += 2
        }
        // Bit 3: Average Cadence present (uint16)
        if ((flags and 0x0008) != 0 && bytes.size >= offset + 2) {
            offset += 2
        }
        // Bit 4: Total Distance present (uint24, whole meters)
        if ((flags and 0x0010) != 0 && bytes.size >= offset + 3) {
            val meters = (bytes[offset].toInt() and 0xFF) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
                ((bytes[offset + 2].toInt() and 0xFF) shl 16)
            distanceMeters = meters.coerceAtLeast(0)
            distanceKm = ((meters / 1000f) * 100f).roundToInt() / 100f
            distanceUnit = "KM"
            offset += 3
        }
        // Bit 5: Resistance Level present (sint16)
        if ((flags and 0x0020) != 0 && bytes.size >= offset + 2) {
            offset += 2
        }
        // Bit 6: Instantaneous Power present (sint16, Watts)
        if ((flags and 0x0040) != 0 && bytes.size >= offset + 2) {
            val rawPower = ((bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)).toShort().toInt()
            powerWatts = rawPower.coerceIn(0, 2500)
            offset += 2
        }
        // Bit 7: Average Power present (sint16)
        if ((flags and 0x0080) != 0 && bytes.size >= offset + 2) {
            val rawAvgPower = ((bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)).toShort().toInt()
            avgWatts = rawAvgPower.coerceIn(0, 2500)
            offset += 2
        }
        // Bit 8: Expended Energy present (Total Energy uint16 + Energy/hr uint16 + Energy/min uint8 = 5 bytes)
        if ((flags and 0x0100) != 0 && bytes.size >= offset + 5) {
            val totalEnergy = (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)
            val energyPerHour = (bytes[offset + 2].toInt() and 0xFF) or ((bytes[offset + 3].toInt() and 0xFF) shl 8)
            if (totalEnergy != 0xFFFF) {
                calories = totalEnergy.coerceIn(0, 99999)
            }
            if (energyPerHour != 0xFFFF) {
                caloriesPerHour = energyPerHour.coerceIn(0, 99999)
            }
            offset += 5
        }
        // Bit 9: Heart Rate present (uint8, bpm)
        if ((flags and 0x0200) != 0 && bytes.size >= offset + 1) {
            val hrRaw = bytes[offset].toInt() and 0xFF
            if (hrRaw in 30..230) {
                heartRate = hrRaw.toFloat()
            }
        }

        return current.copy(
            cadenceRpm = cadenceRpm,
            speedKmh = speedKmh,
            liveSplitSeconds = liveSplitSeconds,
            powerWatts = powerWatts,
            avgWatts = avgWatts,
            calories = calories,
            caloriesPerHour = caloriesPerHour,
            distance = distanceKm,
            distanceMeters = distanceMeters,
            distanceUnit = distanceUnit,
            heartRateBpm = heartRate,
            lastSeenTimestamp = System.currentTimeMillis(),
            isStale = false
        )
    }

    /**
     * Decodes official Bluetooth SIG FTMS Rower Data characteristic (UUID 0x2AD1),
     * used as FTMS verification / telemetry for Concept2 RowErg / SkiErg when 0x2AD1 is exposed.
     */
    fun parseRowerData(bytes: ByteArray, current: BikeData): BikeData {
        if (bytes.size < 2) return current

        val flags = (bytes[0].toInt() and 0xFF) or ((bytes[1].toInt() and 0xFF) shl 8)
        var offset = 2

        var strokeRate = current.cadenceRpm
        var distanceMeters = current.distanceMeters
        var liveSplitSeconds = current.liveSplitSeconds
        var powerWatts = current.powerWatts
        var calories = current.calories
        var heartRate = current.heartRateBpm

        // Bit 0 == 0: More Data == 0 -> Stroke Rate (uint8, 0.5 s/m) + Stroke Count (uint16) present
        if ((flags and 0x0001) == 0 && bytes.size >= offset + 3) {
            val rawSpm = bytes[offset].toInt() and 0xFF
            strokeRate = (rawSpm * 0.5f).coerceIn(0f, 220f)
            offset += 3 // 1 byte stroke rate + 2 bytes stroke count
        }
        // Bit 1: Average Stroke Rate present (uint8)
        if ((flags and 0x0002) != 0 && bytes.size >= offset + 1) {
            offset += 1
        }
        // Bit 2: Total Distance present (uint24, whole metres)
        if ((flags and 0x0004) != 0 && bytes.size >= offset + 3) {
            val meters = (bytes[offset].toInt() and 0xFF) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
                ((bytes[offset + 2].toInt() and 0xFF) shl 16)
            distanceMeters = meters.coerceAtLeast(0)
            offset += 3
        }
        // Bit 3: Instantaneous Pace present (uint16, seconds per 500m)
        if ((flags and 0x0008) != 0 && bytes.size >= offset + 2) {
            val paceSec = (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)
            liveSplitSeconds = if (paceSec in 1..6000) paceSec.toFloat() else null
            offset += 2
        }
        // Bit 4: Average Pace present (uint16)
        if ((flags and 0x0010) != 0 && bytes.size >= offset + 2) {
            offset += 2
        }
        // Bit 5: Instantaneous Power present (sint16, Watts)
        if ((flags and 0x0020) != 0 && bytes.size >= offset + 2) {
            val rawPower = ((bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)).toShort().toInt()
            powerWatts = rawPower.coerceIn(0, 2500)
            offset += 2
        }
        // Bit 6: Average Power present (sint16)
        if ((flags and 0x0040) != 0 && bytes.size >= offset + 2) {
            offset += 2
        }
        // Bit 7: Resistance Level present (sint16)
        if ((flags and 0x0080) != 0 && bytes.size >= offset + 2) {
            offset += 2
        }
        // Bit 8: Expended Energy present (5 bytes: Total Energy uint16 + /hr uint16 + /min uint8)
        if ((flags and 0x0100) != 0 && bytes.size >= offset + 5) {
            val totalEnergy = (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)
            if (totalEnergy != 0xFFFF) {
                calories = totalEnergy.coerceIn(0, 99999)
            }
            offset += 5
        }
        // Bit 9: Heart Rate present (uint8)
        if ((flags and 0x0200) != 0 && bytes.size >= offset + 1) {
            val hrRaw = bytes[offset].toInt() and 0xFF
            if (hrRaw in 30..230) {
                heartRate = hrRaw.toFloat()
            }
        }

        val resolvedType = if (current.equipmentType == "SKIERG") "SKIERG" else "ROWERG"
        return current.copy(
            equipmentType = resolvedType,
            cadenceRpm = strokeRate,
            powerWatts = powerWatts,
            liveSplitSeconds = liveSplitSeconds,
            calories = calories,
            distanceMeters = distanceMeters,
            distance = distanceMeters / 1000f,
            distanceUnit = "M",
            heartRateBpm = heartRate,
            lastSeenTimestamp = System.currentTimeMillis(),
            isStale = false
        )
    }

    data class CyclingPowerDecodeResult(
        val bikeData: BikeData,
        val crankRevs: Int?,
        val crankEventTime: Int?
    )

    /**
     * Decodes official Bluetooth SIG Cycling Power Measurement characteristic (UUID 0x2A63).
     */
    fun parseCyclingPowerMeasurement(
        bytes: ByteArray,
        current: BikeData,
        prevCrankRevs: Int?,
        prevCrankEventTime: Int?
    ): CyclingPowerDecodeResult {
        if (bytes.size < 4) return CyclingPowerDecodeResult(current, prevCrankRevs, prevCrankEventTime)

        val flags = (bytes[0].toInt() and 0xFF) or ((bytes[1].toInt() and 0xFF) shl 8)
        val rawPower = ((bytes[2].toInt() and 0xFF) or ((bytes[3].toInt() and 0xFF) shl 8)).toShort().toInt()
        val powerWatts = rawPower.coerceIn(0, 2500)

        var offset = 4
        if ((flags and 0x0001) != 0 && bytes.size >= offset + 1) {
            offset += 1
        }
        if ((flags and 0x0004) != 0 && bytes.size >= offset + 2) {
            offset += 2
        }
        if ((flags and 0x0010) != 0 && bytes.size >= offset + 6) {
            offset += 6
        }

        var cadenceRpm = current.cadenceRpm
        var newCrankRevs = prevCrankRevs
        var newCrankTime = prevCrankEventTime

        if ((flags and 0x0020) != 0 && bytes.size >= offset + 4) {
            val revs = (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)
            val eventTime = (bytes[offset + 2].toInt() and 0xFF) or ((bytes[offset + 3].toInt() and 0xFF) shl 8)

            if (prevCrankRevs != null && prevCrankEventTime != null) {
                val deltaRevs = (revs - prevCrankRevs) and 0xFFFF
                val deltaTime = (eventTime - prevCrankEventTime) and 0xFFFF
                if (deltaRevs in 1..64 && deltaTime > 0) {
                    val rpm = (deltaRevs.toFloat() * 60f * 1024f) / deltaTime.toFloat()
                    cadenceRpm = ((rpm * 10f).roundToInt() / 10f).coerceIn(0f, 220f)
                } else if (deltaRevs == 0 && powerWatts == 0) {
                    cadenceRpm = 0f
                }
            }
            newCrankRevs = revs
            newCrankTime = eventTime
        } else if (powerWatts == 0) {
            cadenceRpm = 0f
        }

        return CyclingPowerDecodeResult(
            bikeData = current.copy(
                powerWatts = powerWatts,
                cadenceRpm = cadenceRpm,
                lastSeenTimestamp = System.currentTimeMillis(),
                isStale = false
            ),
            crankRevs = newCrankRevs,
            crankEventTime = newCrankTime
        )
    }

    /**
     * Decodes official Bluetooth SIG Cycling Speed & Cadence Measurement (UUID 0x2A5B).
     * Used alongside FTMS (0x2AD2) or Cycling Power (0x2A63) when Wattbike / studio bikes
     * stream crank revolutions (RPM) on 0x1816.
     */
    fun parseCscMeasurement(
        bytes: ByteArray,
        current: BikeData,
        prevCrankRevs: Int?,
        prevCrankEventTime: Int?
    ): CyclingPowerDecodeResult {
        if (bytes.isEmpty()) return CyclingPowerDecodeResult(current, prevCrankRevs, prevCrankEventTime)

        val flags = bytes[0].toInt() and 0xFF
        var offset = 1

        // Bit 0: Wheel Revolution Data Present (uint32 + uint16 = 6 bytes)
        if ((flags and 0x01) != 0 && bytes.size >= offset + 6) {
            offset += 6
        }

        var cadenceRpm = current.cadenceRpm
        var newCrankRevs = prevCrankRevs
        var newCrankTime = prevCrankEventTime

        // Bit 1: Crank Revolution Data Present (uint16 crankRevs + uint16 lastCrankEventTime in 1/1024s)
        if ((flags and 0x02) != 0 && bytes.size >= offset + 4) {
            val revs = (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)
            val eventTime = (bytes[offset + 2].toInt() and 0xFF) or ((bytes[offset + 3].toInt() and 0xFF) shl 8)

            if (prevCrankRevs != null && prevCrankEventTime != null) {
                val deltaRevs = (revs - prevCrankRevs) and 0xFFFF
                val deltaTime = (eventTime - prevCrankEventTime) and 0xFFFF
                if (deltaRevs in 1..64 && deltaTime > 0) {
                    val rpm = (deltaRevs.toFloat() * 60f * 1024f) / deltaTime.toFloat()
                    cadenceRpm = ((rpm * 10f).roundToInt() / 10f).coerceIn(0f, 220f)
                } else if (deltaRevs == 0 && current.powerWatts == 0) {
                    cadenceRpm = 0f
                }
            }
            newCrankRevs = revs
            newCrankTime = eventTime
        }

        return CyclingPowerDecodeResult(
            bikeData = current.copy(
                cadenceRpm = cadenceRpm,
                lastSeenTimestamp = System.currentTimeMillis(),
                isStale = false
            ),
            crankRevs = newCrankRevs,
            crankEventTime = newCrankTime
        )
    }
}
