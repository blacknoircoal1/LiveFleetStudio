package com.keiser.studio

import com.keiser.studio.model.FtpZone
import com.keiser.studio.parser.KeiserPacketParser
import org.junit.Assert.*
import org.junit.Test

class KeiserPacketParserTest {

    @Test
    fun testParseAndroidStrippedPacket() {
        // Android stripped packet (without 2-byte company ID prefix)
        // Offset 0: FW Major (6)
        // Offset 1: FW Minor (98)
        // Offset 2: Data Type (255)
        // Offset 3: Bike ID (7)
        // Offset 4..5: Cadence 860 (86.0 RPM) -> 0x035C -> 0x5C, 0x03
        // Offset 6..7: HR 1500 (150.0 BPM) -> 0x05DC -> 0xDC, 0x05
        // Offset 8..9: Power 240 W -> 0x00F0 -> 0xF0, 0x00
        // Offset 10..11: Calories 310 kcal -> 0x0136 -> 0x36, 0x01
        // Offset 12..13: Distance (0x8000 | 155) = 15.5 KM -> 0x809B -> 0x9B, 0x80
        // Offset 14: Gear (16)
        val packet = byteArrayOf(
            6.toByte(), 98.toByte(), 255.toByte(), 7.toByte(),
            0x5C.toByte(), 0x03.toByte(),
            0xDC.toByte(), 0x05.toByte(),
            0xF0.toByte(), 0x00.toByte(),
            0x36.toByte(), 0x01.toByte(),
            0x9B.toByte(), 0x80.toByte(),
            16.toByte()
        )

        val parsed = KeiserPacketParser.parse(packet)
        assertNotNull(parsed)
        assertEquals(7, parsed!!.bikeId)
        assertEquals(6, parsed.fwMajor)
        assertEquals(98, parsed.fwMinor)
        assertEquals(255, parsed.dataType)
        assertEquals(86.0f, parsed.cadenceRpm, 0.01f)
        assertEquals(150.0f, parsed.heartRateBpm, 0.01f)
        assertEquals(240, parsed.powerWatts)
        assertEquals(310, parsed.calories)
        assertEquals(15.5f, parsed.distance, 0.01f)
        assertEquals("KM", parsed.distanceUnit)
        assertEquals(16, parsed.gear)
    }

    @Test
    fun testFtpZones() {
        assertEquals(FtpZone.ZONE_1, FtpZone.fromFtpPercent(50))
        assertEquals(FtpZone.ZONE_1, FtpZone.fromFtpPercent(55))
        assertEquals(FtpZone.ZONE_2, FtpZone.fromFtpPercent(56))
        assertEquals(FtpZone.ZONE_2, FtpZone.fromFtpPercent(75))
        assertEquals(FtpZone.ZONE_3, FtpZone.fromFtpPercent(76))
        assertEquals(FtpZone.ZONE_3, FtpZone.fromFtpPercent(90))
        assertEquals(FtpZone.ZONE_4, FtpZone.fromFtpPercent(91))
        assertEquals(FtpZone.ZONE_4, FtpZone.fromFtpPercent(105))
        assertEquals(FtpZone.ZONE_5, FtpZone.fromFtpPercent(106))
        assertEquals(FtpZone.ZONE_5, FtpZone.fromFtpPercent(130))
    }

    @Test
    fun testFtpCalculationScaling() {
        val avgPower = 200
        val fiveMinFtp = (avgPower * 0.85f).toInt()
        val twentyMinFtp = (avgPower * 0.95f).toInt()
        assertEquals(170, fiveMinFtp)
        assertEquals(190, twentyMinFtp)
    }
}
