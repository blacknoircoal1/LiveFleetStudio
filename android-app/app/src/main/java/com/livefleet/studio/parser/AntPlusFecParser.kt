package com.livefleet.studio.parser

import com.livefleet.studio.model.BikeData
import com.livefleet.studio.model.BikeProtocol
import com.livefleet.studio.model.BrandType

/**
 * Decodes BOTH ANT+ Fitness Equipment Control (FE-C, Device Type 17 / 0x11) AND
 * ANT+ Bicycle Power (Device Type 11 / 0x0B) broadcast frames received over a USB ANT+ stick
 * in Continuous Rx Scan Mode.
 *
 * Covers 10–30+ simultaneous bikes/ergs in group classes across:
 *  - Concept2 PM5 BikeErg / RowErg / SkiErg (ANT+ FE-C Pages 16, 22, 24, 25)
 *  - Stages SC3 / SC2 / SB20 (ANT+ Bike Power Page 16 / 0x10)
 *  - Schwinn MPower Echelon2 & 4iiii Precision (ANT+ Bike Power 0x0B & FE-C 0x11)
 *  - Wattbike Air-Pro / Atom, Life Fitness IC6/7, Technogym, Matrix (ANT+ FE-C & Bike Power)
 *  - Favero Assioma, Garmin Rally, Quarq, SRM (ANT+ Bike Power Page 16 / 0x10)
 */
object AntPlusFecParser {
    const val ANT_SYNC_BYTE: Byte = 0xA4.toByte()
    const val MSG_BROADCAST_DATA: Int = 0x4E
    const val DEVICE_TYPE_BIKE_POWER: Int = 0x0B // 11 = ANT+ Bicycle Power
    const val DEVICE_TYPE_FE_C: Int = 0x11       // 17 = ANT+ Fitness Equipment Control

    data class ExtendedAntFrame(
        val deviceNumber: Int,
        val deviceType: Int,
        val payload: ByteArray
    )

    fun parseExtendedBroadcast(msgId: Int, data: ByteArray): ExtendedAntFrame? {
        if (msgId != MSG_BROADCAST_DATA || data.size < 14) return null
        val extFlag = data[9].toInt() and 0xFF
        if ((extFlag and 0x80) == 0) return null

        val deviceNumber = (data[10].toInt() and 0xFF) or ((data[11].toInt() and 0xFF) shl 8)
        val deviceType = data[12].toInt() and 0x7F
        val payload = data.copyOfRange(1, 9)
        return ExtendedAntFrame(
            deviceNumber = deviceNumber,
            deviceType = deviceType,
            payload = payload
        )
    }

    /**
     * Decodes an ANT+ Bicycle Power (Device Type 0x0B) 8-byte data page.
     * Official ANT+ Bicycle Power Page 16 (0x10 — Standard Power-Only):
     *  - Byte 3:    Instantaneous Cadence (RPM, 0..254; 0xFF = invalid)
     *  - Bytes 6-7: Instantaneous Power (uint16 LE, Watts; 0xFFFF = invalid)
     */
    fun parseBikePowerPage(
        deviceNumber: Int,
        payload: ByteArray,
        existing: BikeData?,
        selectedProtocol: BikeProtocol = BikeProtocol.AUTO_DETECT
    ): BikeData? {
        if (payload.size < 8 || deviceNumber <= 0) return null
        val page = payload[0].toInt() and 0xFF
        if (page != 0x10 && page != 0x12) return null

        val displayId = deviceNumber.toString().padStart(5, '0').takeLast(5)
        val stableId = if (deviceNumber <= 250) 20000 + deviceNumber else deviceNumber
        val defaultBrand = brandForAntBike(selectedProtocol)
        val defaultBrandType = BrandType.fromBrandString(defaultBrand)
        val base = existing ?: BikeData(
            bikeId = stableId,
            displayId = displayId,
            brand = defaultBrand,
            brandType = defaultBrandType,
            macAddress = "ANT-$deviceNumber",
            riderName = "$defaultBrand $displayId",
            lastSeenTimestamp = System.currentTimeMillis(),
            isStale = false
        )

        val cadRaw = if (page == 0x10) payload[3].toInt() and 0xFF else payload[2].toInt() and 0xFF
        val rpm = if (cadRaw != 0xFF) cadRaw.toFloat().coerceIn(0f, 220f) else base.cadenceRpm
        val watts = if (page == 0x10) {
            val powerRaw = (payload[6].toInt() and 0xFF) or ((payload[7].toInt() and 0xFF) shl 8)
            if (powerRaw != 0xFFFF) powerRaw.coerceIn(0, 2500) else base.powerWatts
        } else {
            base.powerWatts
        }

        return base.copy(
            brand = existing?.brand ?: defaultBrand,
            brandType = existing?.brandType ?: defaultBrandType,
            cadenceRpm = rpm,
            powerWatts = watts,
            lastSeenTimestamp = System.currentTimeMillis(),
            isStale = false
        )
    }

    /**
     * Applies an 8-byte ANT+ FE-C (Device Type 0x11) data page to an existing (or new) [BikeData].
     */
    fun parseFecPage(
        deviceNumber: Int,
        payload: ByteArray,
        existing: BikeData?,
        selectedProtocol: BikeProtocol = BikeProtocol.CONCEPT2
    ): BikeData? {
        if (payload.size < 8 || deviceNumber <= 0) return null

        val displayId = deviceNumber.toString().padStart(5, '0').takeLast(5)
        val stableId = if (deviceNumber <= 250) 20000 + deviceNumber else deviceNumber
        val defaultBikeBrand = brandForAntBike(selectedProtocol)
        val initialBrand = if (selectedProtocol == BikeProtocol.CONCEPT2) "Concept2" else defaultBikeBrand
        val base = existing ?: BikeData(
            bikeId = stableId,
            displayId = displayId,
            brand = initialBrand,
            brandType = BrandType.fromBrandString(initialBrand),
            macAddress = "ANT-$deviceNumber",
            riderName = "ERG $displayId",
            lastSeenTimestamp = System.currentTimeMillis(),
            isStale = false
        )

        val page = payload[0].toInt() and 0xFF
        val now = System.currentTimeMillis()

        return when (page) {
            0x10 -> { // Page 16: General FE Data (byte 3 = elapsed distance in whole metres mod 256 if bit enabled, keep accumulated if FE-C distance rollover)
                val eqCode = payload[1].toInt() and 0x1F
                val eqType = when (eqCode) {
                    19 -> "ROWERG"
                    21, 25 -> "BIKEERG"
                    22, 24 -> "SKIERG"
                    else -> base.equipmentType
                }
                val resolvedBrand = when (eqType) {
                    "ROWERG", "SKIERG" -> "Concept2"
                    "BIKEERG" -> if (selectedProtocol == BikeProtocol.CONCEPT2) "Concept2" else defaultBikeBrand
                    else -> base.brand
                }
                val hrRaw = payload[6].toInt() and 0xFF
                val hr = if (hrRaw in 30..230) hrRaw.toFloat() else base.heartRateBpm
                base.copy(
                    brand = resolvedBrand,
                    brandType = BrandType.fromBrandString(resolvedBrand),
                    equipmentType = eqType,
                    heartRateBpm = hr,
                    lastSeenTimestamp = now,
                    isStale = false
                )
            }

            0x16 -> { // Page 22: Specific Rower Data (Concept2 RowErg)
                val spmRaw = payload[4].toInt() and 0xFF
                val powerRaw = (payload[5].toInt() and 0xFF) or ((payload[6].toInt() and 0xFF) shl 8)
                val spm = if (spmRaw != 0xFF) spmRaw.toFloat().coerceIn(0f, 220f) else base.cadenceRpm
                val watts = if (powerRaw != 0xFFFF) powerRaw.coerceIn(0, 2500) else base.powerWatts
                base.copy(
                    brand = "Concept2",
                    brandType = BrandType.CONCEPT2,
                    equipmentType = "ROWERG",
                    cadenceRpm = spm,
                    powerWatts = watts,
                    liveSplitSeconds = null,
                    lastSeenTimestamp = now,
                    isStale = false
                )
            }

            0x18 -> { // Page 24: Specific Nordic Skier (Concept2 SkiErg)
                val spmRaw = payload[4].toInt() and 0xFF
                val powerRaw = (payload[5].toInt() and 0xFF) or ((payload[6].toInt() and 0xFF) shl 8)
                val spm = if (spmRaw != 0xFF) spmRaw.toFloat().coerceIn(0f, 220f) else base.cadenceRpm
                val watts = if (powerRaw != 0xFFFF) powerRaw.coerceIn(0, 2500) else base.powerWatts
                base.copy(
                    brand = "Concept2",
                    brandType = BrandType.CONCEPT2,
                    equipmentType = "SKIERG",
                    cadenceRpm = spm,
                    powerWatts = watts,
                    liveSplitSeconds = null,
                    lastSeenTimestamp = now,
                    isStale = false
                )
            }

            0x19 -> { // Page 25: Specific Bike / Trainer Data (BikeErg, Wattbike, Schwinn, etc.)
                val cadRaw = payload[2].toInt() and 0xFF
                val powerRaw = (payload[5].toInt() and 0xFF) or ((payload[6].toInt() and 0xFF) shl 8)
                val rpm = if (cadRaw != 0xFF) cadRaw.toFloat().coerceIn(0f, 220f) else base.cadenceRpm
                val watts = if (powerRaw != 0xFFFF) powerRaw.coerceIn(0, 2500) else base.powerWatts
                val bikeBrand = if (selectedProtocol == BikeProtocol.CONCEPT2) "Concept2" else defaultBikeBrand
                base.copy(
                    brand = bikeBrand,
                    brandType = BrandType.fromBrandString(bikeBrand),
                    equipmentType = if (bikeBrand == "Concept2") "BIKEERG" else base.equipmentType,
                    cadenceRpm = rpm,
                    powerWatts = watts,
                    liveSplitSeconds = null,
                    lastSeenTimestamp = now,
                    isStale = false
                )
            }

            else -> null
        }
    }

    private fun brandForAntBike(protocol: BikeProtocol): String = when (protocol) {
        BikeProtocol.STAGES -> "Stages"
        BikeProtocol.SCHWINN_4IIII -> "Schwinn"
        BikeProtocol.CONCEPT2 -> "Concept2"
        BikeProtocol.FTMS_UNIVERSAL -> "Wattbike"
        BikeProtocol.POWER_METERS -> "PowerMeter"
        else -> "FTMS"
    }

    fun buildAntCommand(msgId: Int, vararg data: Int): ByteArray {
        val frame = ByteArray(data.size + 4)
        frame[0] = ANT_SYNC_BYTE
        frame[1] = data.size.toByte()
        frame[2] = msgId.toByte()
        for (i in data.indices) {
            frame[3 + i] = data[i].toByte()
        }
        var checksum = 0
        for (i in 0 until frame.size - 1) {
            checksum = checksum xor (frame[i].toInt() and 0xFF)
        }
        frame[frame.size - 1] = checksum.toByte()
        return frame
    }
}
