package com.livefleet.studio

import com.livefleet.studio.model.BikeData
import com.livefleet.studio.model.BikeProtocol
import com.livefleet.studio.model.BrandType
import com.livefleet.studio.parser.AntPlusFecParser
import com.livefleet.studio.parser.Concept2PacketParser
import com.livefleet.studio.parser.FtmsPacketParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

        // Stages SIC2 console "#008" -> "#8" (primary console) vs 5-digit ANT+ crank "55617" (secondary)
        val stagesConsole008 = FtmsPacketParser.classifyGattDevice(
            deviceName = "Stages 008",
            macAddress = "11:22:33:44:55:78",
            serviceUuids = listOf(FtmsPacketParser.UUID_CYCLING_POWER_SERVICE),
            hasConcept2MfrData = false,
            hasStagesMfrData = true,
            has4iiiiMfrData = false,
            protocol = BikeProtocol.STAGES
        )
        assertNotNull(stagesConsole008)
        assertEquals("8", stagesConsole008!!.displayId)
        assertEquals(true, stagesConsole008.isPrimaryConsole)

        val stagesCrank55617 = FtmsPacketParser.classifyGattDevice(
            deviceName = "Stages 55617",
            macAddress = "11:22:33:44:55:79",
            serviceUuids = listOf(FtmsPacketParser.UUID_CYCLING_POWER_SERVICE),
            hasConcept2MfrData = false,
            hasStagesMfrData = true,
            has4iiiiMfrData = false,
            protocol = BikeProtocol.STAGES
        )
        assertNotNull(stagesCrank55617)
        assertEquals("55617", stagesCrank55617!!.displayId)
        assertEquals(false, stagesCrank55617.isPrimaryConsole)

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
    fun testLayer1GatekeeperStrictRejections() {
        // Wattbike mode must reject PM5, Concept2, Stages, and Keiser M3 even if advertising FTMS/CPS
        val pm5OnFtms = FtmsPacketParser.classifyGattDevice(
            deviceName = "PM5 530339622",
            macAddress = "11:22:33:44:55:01",
            serviceUuids = listOf(FtmsPacketParser.UUID_FTMS_SERVICE),
            hasConcept2MfrData = false,
            hasStagesMfrData = false,
            has4iiiiMfrData = false,
            protocol = BikeProtocol.FTMS_UNIVERSAL
        )
        assertNull(pm5OnFtms)

        val m3OnFtms = FtmsPacketParser.classifyGattDevice(
            deviceName = "M3 #12",
            macAddress = "11:22:33:44:55:02",
            serviceUuids = listOf(FtmsPacketParser.UUID_FTMS_SERVICE),
            hasConcept2MfrData = false,
            hasStagesMfrData = false,
            has4iiiiMfrData = false,
            protocol = BikeProtocol.AUTO_DETECT,
            hasKeiserMfrData = true
        )
        assertNull(m3OnFtms)

        // Blank packet with no name, UUID, or mfr data must be dropped
        val emptyPacket = FtmsPacketParser.classifyGattDevice(
            deviceName = "   ",
            macAddress = "11:22:33:44:55:03",
            serviceUuids = emptyList(),
            hasConcept2MfrData = false,
            hasStagesMfrData = false,
            has4iiiiMfrData = false,
            protocol = BikeProtocol.AUTO_DETECT
        )
        assertNull(emptyPacket)

        // Wattbike UUID detection in Auto-Detect
        val wattbikeUuid = FtmsPacketParser.classifyGattDevice(
            deviceName = "Wattbike 98765",
            macAddress = "11:22:33:44:55:04",
            serviceUuids = listOf(FtmsPacketParser.UUID_WATTBIKE_SERVICE),
            hasConcept2MfrData = false,
            hasStagesMfrData = false,
            has4iiiiMfrData = false,
            protocol = BikeProtocol.AUTO_DETECT
        )
        assertNotNull(wattbikeUuid)
        assertEquals(BrandType.WATTBIKE, wattbikeUuid!!.brandType)

        // Stages manufacturer ID 0x0264
        val stagesMfr = FtmsPacketParser.classifyGattDevice(
            deviceName = "Bike 22",
            macAddress = "11:22:33:44:55:05",
            serviceUuids = emptyList(),
            hasConcept2MfrData = false,
            hasStagesMfrData = true,
            has4iiiiMfrData = false,
            protocol = BikeProtocol.STAGES
        )
        assertNotNull(stagesMfr)
        assertEquals(BrandType.STAGES, stagesMfr!!.brandType)

        // Concept2 PM5 plain serial stays unconfirmed ("") until CE060016 / CE060031 confirms subtype
        val pm5Plain = FtmsPacketParser.classifyGattDevice(
            deviceName = "PM5 430012345",
            macAddress = "11:22:33:44:55:06",
            serviceUuids = listOf(Concept2PacketParser.UUID_PM5_DISCOVERY_SERVICE),
            hasConcept2MfrData = false,
            hasStagesMfrData = false,
            has4iiiiMfrData = false,
            protocol = BikeProtocol.CONCEPT2
        )
        assertNotNull(pm5Plain)
        assertEquals(BrandType.CONCEPT2, pm5Plain!!.brandType)
        assertEquals("", pm5Plain.equipmentType)
    }

    @Test
    fun testConcept2GeneralStatusByte18IdentifiesRowErg() {
        val base = BikeData(bikeId = 39622, displayId = "39622", brand = "Concept2", brandType = BrandType.CONCEPT2)
        // 19-byte CE060031 General Status: distance = 12500 tenths (1250 m), byte 18 = 1 (RowErg Static C)
        val payload = ByteArray(19)
        payload[3] = (12500 and 0xFF).toByte()
        payload[4] = ((12500 shr 8) and 0xFF).toByte()
        payload[5] = ((12500 shr 16) and 0xFF).toByte()
        payload[18] = 1

        val parsed = Concept2PacketParser.parseGeneralStatus(payload, base)
        assertEquals(1250, parsed.distanceMeters)
        assertEquals("ROWERG", parsed.equipmentType)
    }

    @Test
    fun testCscMeasurementDecodingForWattbikeRpm() {
        val base = BikeData(bikeId = 2379, displayId = "02379", brand = "Wattbike", powerWatts = 190)
        // Flags: Bit 1 (0x02) = Crank Revolution Data present
        val p1 = byteArrayOf(0x02, 50, 0x00, 0x00, 0x04) // revs=50, time=1024
        val r1 = FtmsPacketParser.parseCscMeasurement(p1, base, null, null)
        // 1 second later (1024 ticks), 1.5s? Let's do 2 revs in 1536 ticks (1.5s) => 80.0 RPM
        val p2 = byteArrayOf(0x02, 52, 0x00, 0x00, 0x0A) // revs=52, time=2560
        val r2 = FtmsPacketParser.parseCscMeasurement(p2, r1.bikeData, r1.crankRevs, r1.crankEventTime)
        assertEquals(80.0f, r2.bikeData.cadenceRpm, 0.1f)
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
        assertEquals(1450, parsed.distanceMeters)
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
