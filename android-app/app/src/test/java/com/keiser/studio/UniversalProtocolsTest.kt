package com.keiser.studio

import com.keiser.studio.model.BikeData
import com.keiser.studio.model.BikeProtocol
import com.keiser.studio.parser.AntPlusFecParser
import com.keiser.studio.parser.FtmsPacketParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class UniversalProtocolsTest {

    @Test
    fun testWattbikeAndMultiBrandClassificationAndSerialIds() {
        val wattbike = FtmsPacketParser.classifyGattDevice(
            deviceName = "AIR-PRO 34002379",
            macAddress = "11:22:33:44:55:66",
            serviceUuids = listOf(FtmsPacketParser.UUID_FTMS_SERVICE),
            hasConcept2MfrData = false,
            hasStagesMfrData = false,
            has4iiiiMfrData = false,
            protocol = BikeProtocol.FTMS_UNIVERSAL
        )
        assertNotNull(wattbike)
        assertEquals("Wattbike", wattbike!!.brand)
        assertEquals("02379", wattbike.displayId)

        val stages = FtmsPacketParser.classifyGattDevice(
            deviceName = "Stages 14",
            macAddress = "11:22:33:44:55:77",
            serviceUuids = listOf(FtmsPacketParser.UUID_CYCLING_POWER_SERVICE),
            hasConcept2MfrData = false,
            hasStagesMfrData = false,
            has4iiiiMfrData = false,
            protocol = BikeProtocol.STAGES
        )
        assertNotNull(stages)
        assertEquals("Stages", stages!!.brand)
        assertEquals("14", stages.displayId)

        val schwinn = FtmsPacketParser.classifyGattDevice(
            deviceName = "4iiii 9045102",
            macAddress = "11:22:33:44:55:88",
            serviceUuids = listOf(FtmsPacketParser.UUID_CYCLING_POWER_SERVICE),
            hasConcept2MfrData = false,
            hasStagesMfrData = false,
            has4iiiiMfrData = false,
            protocol = BikeProtocol.SCHWINN_4IIII
        )
        assertNotNull(schwinn)
        assertEquals("Schwinn", schwinn!!.brand)
        assertEquals("45102", schwinn.displayId)
    }

    @Test
    fun testFtmsIndoorBikeDataDecoding() {
        val base = BikeData(bikeId = 2379, displayId = "02379", brand = "Wattbike")

        // Flags:
        //   Bit 0 = 0 (Inst Speed present: 2 bytes)
        //   Bit 2 = 1 (0x0004: Inst Cadence present: 2 bytes, 0.5 RPM => 76 = 38.0 RPM)
        //   Bit 4 = 1 (0x0010: Total Distance present: 3 bytes, meters => 1450m = 1.45 KM)
        //   Bit 6 = 1 (0x0040: Inst Power present: 2 bytes, Watts => 41 W)
        //   Bit 8 = 1 (0x0100: Expended Energy present: 5 bytes => 120 kCal)
        //   Bit 9 = 1 (0x0200: Heart Rate present: 1 byte => 132 bpm)
        val flags = 0x0004 or 0x0010 or 0x0040 or 0x0100 or 0x0200
        val payload = byteArrayOf(
            (flags and 0xFF).toByte(), ((flags shr 8) and 0xFF).toByte(),
            0xE8.toByte(), 0x03.toByte(), // speed (10.00 km/h)
            76.toByte(), 0x00.toByte(),   // cadence = 76 * 0.5 = 38.0 RPM
            (1450 and 0xFF).toByte(), ((1450 shr 8) and 0xFF).toByte(), 0x00.toByte(), // 1450 m
            41.toByte(), 0x00.toByte(),   // power = 41 W
            120.toByte(), 0x00.toByte(), 0x00, 0x00, 0x00, // 120 kCal + hr/min rates
            132.toByte()                  // HR = 132 bpm
        )

        val parsed = FtmsPacketParser.parseIndoorBikeData(payload, base)
        assertEquals(41, parsed.powerWatts)
        assertEquals(38.0f, parsed.cadenceRpm, 0.01f)
        assertEquals(1.45f, parsed.distance, 0.01f)
        assertEquals(120, parsed.calories)
        assertEquals(132f, parsed.heartRateBpm, 0.01f)
    }

    @Test
    fun testCyclingPowerMeasurementDecodingWithCrankCadence() {
        val base = BikeData(bikeId = 14, displayId = "#14", brand = "Stages")

        // Flags: Bit 5 (0x0020) = Crank Revolution Data present
        // Packet 1: Power = 225 W, crankRevs = 100, crankEventTime = 2048
        val p1 = byteArrayOf(
            0x20, 0x00,
            (225 and 0xFF).toByte(), ((225 shr 8) and 0xFF).toByte(),
            100, 0x00,
            (2048 and 0xFF).toByte(), ((2048 shr 8) and 0xFF).toByte()
        )
        val r1 = FtmsPacketParser.parseCyclingPowerMeasurement(p1, base, null, null)
        assertEquals(225, r1.bikeData.powerWatts)
        assertEquals(100, r1.crankRevs)
        assertEquals(2048, r1.crankEventTime)

        // Packet 2 (1.5 seconds later = 1536 ticks, 2 crank revolutions => 80.0 RPM): Power = 240 W
        val nextTime = 2048 + 1536
        val p2 = byteArrayOf(
            0x20, 0x00,
            (240 and 0xFF).toByte(), ((240 shr 8) and 0xFF).toByte(),
            102, 0x00,
            (nextTime and 0xFF).toByte(), ((nextTime shr 8) and 0xFF).toByte()
        )
        val r2 = FtmsPacketParser.parseCyclingPowerMeasurement(p2, r1.bikeData, r1.crankRevs, r1.crankEventTime)
        assertEquals(240, r2.bikeData.powerWatts)
        assertEquals(80.0f, r2.bikeData.cadenceRpm, 0.1f)
    }

    @Test
    fun testAntPlusBikePowerPage16Decoding() {
        // ANT+ Bike Power (Device Type 0x0B), Page 16 (0x10): byte 3 = 87 RPM, bytes 6..7 = 215 W
        val payload = byteArrayOf(
            0x10, 0x05, 0xFF.toByte(), 87,
            0x00, 0x00,
            (215 and 0xFF).toByte(), ((215 shr 8) and 0xFF).toByte()
        )
        val bike = AntPlusFecParser.parseBikePowerPage(
            deviceNumber = 42190,
            payload = payload,
            existing = null,
            selectedProtocol = BikeProtocol.STAGES
        )
        assertNotNull(bike)
        assertEquals("Stages", bike!!.brand)
        assertEquals("42190", bike.displayId)
        assertEquals(87f, bike.cadenceRpm, 0.01f)
        assertEquals(215, bike.powerWatts)
    }
}
