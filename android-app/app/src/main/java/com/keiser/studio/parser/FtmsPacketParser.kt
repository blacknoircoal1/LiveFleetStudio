package com.keiser.studio.parser

import android.bluetooth.le.ScanResult
import com.keiser.studio.model.BikeData
import com.keiser.studio.model.BikeProtocol
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Standards-based Bluetooth SIG FTMS (0x1826 -> 0x2AD2 Indoor Bike Data) and
 * Cycling Power Service (0x1818 -> 0x2A63 Cycling Power Measurement) parser + brand/serial ID utilities.
 *
 * Covers real-world BLE GATT telemetry for:
 *  - Wattbike Air-Pro / Atom
 *  - Stages SC3 / SC2 / SB20
 *  - Schwinn 4iiii Precision
 *  - Life Fitness / ICG IC5 / IC6 / IC7 / IC8
 *  - Technogym Skillbike, Matrix, Body Bike Smart+, Wahoo KICKR
 *  - Favero Assioma, Garmin Rally, Quarq, SRM, PowerTap
 */
object FtmsPacketParser : BikePacketParser {
    val UUID_FTMS_SERVICE: UUID = UUID.fromString("00001826-0000-1000-8000-00805f9b34fb")
    val UUID_INDOOR_BIKE_DATA: UUID = UUID.fromString("00002AD2-0000-1000-8000-00805f9b34fb")
    val UUID_CYCLING_POWER_SERVICE: UUID = UUID.fromString("00001818-0000-1000-8000-00805f9b34fb")
    val UUID_CYCLING_POWER_MEASUREMENT: UUID = UUID.fromString("00002A63-0000-1000-8000-00805f9b34fb")
    val UUID_CYCLING_SPEED_CADENCE_SERVICE: UUID = UUID.fromString("00001816-0000-1000-8000-00805f9b34fb")

    override val protocol: BikeProtocol = BikeProtocol.FTMS_UNIVERSAL

    /**
     * Passive BLE scan parser intentionally returns null for FTMS / Cycling Power devices so static
     * BLE advertising header bytes are never misread as live telemetry. Live metrics are streamed via
     * [com.keiser.studio.bluetooth.UniversalGattManager] (BLE GATT) or
     * [com.keiser.studio.bluetooth.AntPlusUsbReceiver] (USB ANT+ FE-C & Bike Power).
     */
    override fun parse(result: ScanResult): BikeData? = null

    data class DiscoveredGattMeta(
        val brand: String,
        val equipmentType: String,
        val displayId: String,
        val stableBikeId: Int,
        val matchesProtocol: Boolean
    )

    /**
     * Classifies a BLE advertisement to determine if it is a GATT-capable bike/erg matching
     * the currently selected [protocol].
     */
    fun classifyGattDevice(
        deviceName: String,
        macAddress: String,
        serviceUuids: List<UUID>,
        hasConcept2MfrData: Boolean,
        hasStagesMfrData: Boolean,
        has4iiiiMfrData: Boolean,
        protocol: BikeProtocol
    ): DiscoveredGattMeta? {
        val lower = deviceName.lowercase()
        val hasPm5Service = serviceUuids.any {
            it == Concept2PacketParser.UUID_PM5_DISCOVERY_SERVICE ||
                it == Concept2PacketParser.UUID_ROWING_SERVICE
        }
        val hasFtmsService = serviceUuids.any { it == UUID_FTMS_SERVICE }
        val hasCpsService = serviceUuids.any {
            it == UUID_CYCLING_POWER_SERVICE || it == UUID_CYCLING_SPEED_CADENCE_SERVICE
        }

        val isConcept2 = hasPm5Service || hasConcept2MfrData ||
            lower.contains("pm5") || lower.contains("concept2") ||
            lower.contains("bikeerg") || lower.contains("rowerg") || lower.contains("skierg")

        val isStages = hasStagesMfrData ||
            lower.contains("stages") || lower.contains("sc3") ||
            lower.contains("sc2") || lower.contains("sb20")

        val isSchwinn = has4iiiiMfrData ||
            lower.contains("schwinn") || lower.contains("4iiii") ||
            lower.contains("precision") || lower.contains("echelon")

        val isWattbike = lower.contains("wattbike") || lower.contains("air-pro") ||
            lower.contains("air pro") || lower.contains("atom")

        val isOtherFtmsBrand = lower.contains("technogym") || lower.contains("skillbike") ||
            lower.contains("ic5") || lower.contains("ic6") || lower.contains("ic7") ||
            lower.contains("ic8") || lower.contains("life fitness") || lower.contains("icg") ||
            lower.contains("matrix") || lower.contains("body bike") || lower.contains("kickr") ||
            lower.contains("tacx")

        val isPowerPedalBrand = lower.contains("assioma") || lower.contains("rally") ||
            lower.contains("quarq") || lower.contains("srm") || lower.contains("powertap") ||
            lower.contains("vector") || lower.contains("favero")

        val isAnySupportedGatt = isConcept2 || isStages || isSchwinn || isWattbike ||
            isOtherFtmsBrand || isPowerPedalBrand || hasFtmsService || hasCpsService

        if (!isAnySupportedGatt) return null

        val brand = when {
            isConcept2 -> "Concept2"
            isStages -> "Stages"
            isSchwinn -> "Schwinn"
            isWattbike -> "Wattbike"
            lower.contains("ic5") || lower.contains("ic6") || lower.contains("ic7") ||
                lower.contains("ic8") || lower.contains("life fitness") || lower.contains("icg") -> "LifeFitness"
            lower.contains("technogym") || lower.contains("skillbike") -> "Technogym"
            lower.contains("matrix") -> "Matrix"
            lower.contains("body bike") -> "BodyBike"
            isPowerPedalBrand || (!hasFtmsService && hasCpsService) -> "PowerMeter"
            else -> "FTMS"
        }

        val matches = when (protocol) {
            BikeProtocol.AUTO_DETECT -> true
            BikeProtocol.KEISER -> false // Keiser uses passive 0x0102 broadcast
            BikeProtocol.CONCEPT2 -> isConcept2
            BikeProtocol.STAGES -> isStages || (!isConcept2 && !isSchwinn && !isWattbike && hasCpsService)
            BikeProtocol.SCHWINN_4IIII -> isSchwinn || (!isConcept2 && !isStages && !isWattbike && hasCpsService)
            BikeProtocol.FTMS_UNIVERSAL -> isWattbike || isOtherFtmsBrand || hasFtmsService
            BikeProtocol.POWER_METERS -> isPowerPedalBrand || hasCpsService || isStages || isSchwinn
        }
        if (!matches) return null

        val equipmentType = if (brand == "Concept2") {
            Concept2PacketParser.detectEquipmentType(deviceName)
        } else {
            ""
        }

        val displayId = extractSerialDisplayId(deviceName, macAddress)
        val stableId = toStableBikeId(displayId, macAddress)

        return DiscoveredGattMeta(
            brand = brand,
            equipmentType = equipmentType,
            displayId = displayId,
            stableBikeId = stableId,
            matchesProtocol = true
        )
    }

    /**
     * Extracts a human-readable machine ID from the BLE device name:
     *  - 5+ digit serial (e.g. "AIR-PRO 34002379", "PM5 530339622 Row") -> last 5 digits ("02379", "39622")
     *  - 1..4 digit studio bike number (e.g. "Stages 12") -> "12" (displayed as #12)
     *  - Fallback -> stable 5-digit ID derived from Bluetooth MAC address
     */
    fun extractSerialDisplayId(deviceName: String, macAddress: String): String {
        val longSerial = Regex("(\\d{5,})").find(deviceName)?.groupValues?.get(1)
        if (longSerial != null) {
            return longSerial.takeLast(5)
        }
        val shortNum = Regex("\\b(\\d{1,4})\\b").find(deviceName)?.groupValues?.get(1)
        if (shortNum != null) {
            return shortNum
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
     * Walks all optional fields strictly according to the 16-bit flags word so Wattbike Air-Pro,
     * Life Fitness IC5-8, Technogym, Matrix, and Wahoo report exact live Watts, RPM, Calories,
     * Distance, and Heart Rate.
     */
    fun parseIndoorBikeData(bytes: ByteArray, current: BikeData): BikeData {
        if (bytes.size < 2) return current

        val flags = (bytes[0].toInt() and 0xFF) or ((bytes[1].toInt() and 0xFF) shl 8)
        var offset = 2

        var cadenceRpm = current.cadenceRpm
        var distanceKm = current.distance
        var distanceUnit = current.distanceUnit
        var powerWatts = current.powerWatts
        var calories = current.calories
        var heartRate = current.heartRateBpm

        // Bit 0 == 0: Instantaneous Speed present (uint16, 0.01 km/h)
        if ((flags and 0x0001) == 0 && bytes.size >= offset + 2) {
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
        // Bit 4: Total Distance present (uint24, meters)
        if ((flags and 0x0010) != 0 && bytes.size >= offset + 3) {
            val meters = (bytes[offset].toInt() and 0xFF) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
                ((bytes[offset + 2].toInt() and 0xFF) shl 16)
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
            offset += 2
        }
        // Bit 8: Expended Energy present (Total Energy uint16 + Energy/hr uint16 + Energy/min uint8 = 5 bytes)
        if ((flags and 0x0100) != 0 && bytes.size >= offset + 5) {
            val totalEnergy = (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)
            if (totalEnergy != 0xFFFF) {
                calories = totalEnergy.coerceIn(0, 99999)
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
            powerWatts = powerWatts,
            calories = calories,
            distance = distanceKm,
            distanceUnit = distanceUnit,
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
     * Used by Stages SC3/SC2, Schwinn 4iiii Precision, Favero Assioma, Garmin Rally, Quarq, SRM,
     * and as a fallback on FTMS bikes. Computes live cadence RPM from crank revolution deltas
     * (1/1024 sec ticks).
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
        // Bit 0: Pedal Power Balance present (uint8)
        if ((flags and 0x0001) != 0 && bytes.size >= offset + 1) {
            offset += 1
        }
        // Bit 2: Accumulated Torque present (uint16)
        if ((flags and 0x0004) != 0 && bytes.size >= offset + 2) {
            offset += 2
        }
        // Bit 4: Wheel Revolution Data present (uint32 + uint16 = 6 bytes)
        if ((flags and 0x0010) != 0 && bytes.size >= offset + 6) {
            offset += 6
        }

        var cadenceRpm = current.cadenceRpm
        var newCrankRevs = prevCrankRevs
        var newCrankTime = prevCrankEventTime

        // Bit 5: Crank Revolution Data present (uint16 revs + uint16 lastCrankEventTime in 1/1024s)
        if ((flags and 0x0020) != 0 && bytes.size >= offset + 4) {
            val revs = (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)
            val eventTime = (bytes[offset + 2].toInt() and 0xFF) or ((bytes[offset + 3].toInt() and 0xFF) shl 8)

            if (prevCrankRevs != null && prevCrankEventTime != null) {
                val deltaRevs = (revs - prevCrankRevs) and 0xFFFF
                val deltaTime = (eventTime - prevCrankEventTime) and 0xFFFF
                if (deltaRevs in 1..20 && deltaTime > 0) {
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
}
