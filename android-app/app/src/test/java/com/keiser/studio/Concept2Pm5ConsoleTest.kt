package com.keiser.studio

import com.keiser.studio.model.BikeData
import com.keiser.studio.parser.AntPlusFecParser
import com.keiser.studio.parser.Concept2PacketParser
import com.keiser.studio.ui.components.calculateConcept2SplitSeconds
import com.keiser.studio.ui.components.formatConcept2SplitPace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class Concept2Pm5ConsoleTest {

    @Test
    fun testConcept2EquipmentTypeDetection() {
        assertEquals("BIKEERG", Concept2PacketParser.detectEquipmentType("PM5 430012345 Bike"))
        assertEquals("BIKEERG", Concept2PacketParser.detectEquipmentType("BikeErg #3"))
        assertEquals("ROWERG", Concept2PacketParser.detectEquipmentType("PM5 530339622 Row"))
        assertEquals("ROWERG", Concept2PacketParser.detectEquipmentType("RowErg 7"))
        assertEquals("SKIERG", Concept2PacketParser.detectEquipmentType("PM5 430012345 Ski"))
        assertEquals("SKIERG", Concept2PacketParser.detectEquipmentType("SkiErg 2"))
        assertEquals("", Concept2PacketParser.detectEquipmentType("PM5 430012345"))
    }

    @Test
    fun testPm5LastFiveDigitIdExtraction() {
        assertEquals("39622", Concept2PacketParser.extractPm5DisplayId("PM5 530339622 Row", "AA:BB:CC:DD:EE:FF"))
        assertEquals("12345", Concept2PacketParser.extractPm5DisplayId("PM5 430012345 Ski", "AA:BB:CC:DD:EE:FF"))
        assertEquals("39622", Concept2PacketParser.toStableBikeId("39622", "AA:BB:CC:DD:EE:FF").toString())
    }

    @Test
    fun testPm5GattAdditionalStatus1And2Parsing() {
        val base = BikeData(bikeId = 39622, displayId = "39622", brand = "Concept2")

        // CE060032 (19 bytes):
        // Bytes 3..4: speed = 4149 (4.149 m/s)
        // Byte 5: stroke rate = 28 s/m
        // Byte 6: HR = 148 bpm
        // Bytes 7..8: current pace = 12051 (120.51s = 2:00.5 /500m => ~200 W)
        // Byte 18: machine type = 2 (RowErg)
        val status1 = ByteArray(19)
        status1[3] = (4149 and 0xFF).toByte()
        status1[4] = ((4149 shr 8) and 0xFF).toByte()
        status1[5] = 28.toByte()
        status1[6] = 148.toByte()
        status1[7] = (12051 and 0xFF).toByte()
        status1[8] = ((12051 shr 8) and 0xFF).toByte()
        status1[18] = 2.toByte()

        val afterStatus1 = Concept2PacketParser.parseAdditionalStatus1(status1, base)
        assertEquals(28f, afterStatus1.cadenceRpm, 0.01f)
        assertEquals(148f, afterStatus1.heartRateBpm, 0.01f)
        assertEquals(200, afterStatus1.powerWatts)
        assertEquals(120.51f, afterStatus1.liveSplitSeconds!!, 0.02f)
        assertEquals("ROWERG", afterStatus1.equipmentType)
        assertEquals("2:00.5", formatConcept2SplitPace(afterStatus1.powerWatts, showTenths = true, liveSplitSeconds = afterStatus1.liveSplitSeconds))

        // CE060033 (18 bytes): bytes 4..5 = 185 kCal
        val status2 = ByteArray(18)
        status2[4] = (185 and 0xFF).toByte()
        status2[5] = ((185 shr 8) and 0xFF).toByte()
        val afterStatus2 = Concept2PacketParser.parseAdditionalStatus2(status2, afterStatus1)
        assertEquals(185, afterStatus2.calories)
    }

    @Test
    fun testAntPlusFecRowerSkiBikePagesAndExtendedBroadcast() {
        // Build extended broadcast data for Device #39622, Device Type 0x11 (FE-C), Page 22 (Rower): 29 s/m, 235 W
        val extData = byteArrayOf(
            0x00, // channel 0
            0x16, 0xFF.toByte(), 0xFF.toByte(), 42, 29, (235 and 0xFF).toByte(), ((235 shr 8) and 0xFF).toByte(), 0x30,
            0x80.toByte(), // extended flag: Channel ID present
            (39622 and 0xFF).toByte(), ((39622 shr 8) and 0xFF).toByte(),
            0x11, // FE-C device type
            0x05  // transmission type
        )
        val frame = AntPlusFecParser.parseExtendedBroadcast(AntPlusFecParser.MSG_BROADCAST_DATA, extData)
        assertNotNull(frame)
        assertEquals(39622, frame!!.deviceNumber)
        assertEquals(AntPlusFecParser.DEVICE_TYPE_FE_C, frame.deviceType)

        val rower = AntPlusFecParser.parseFecPage(frame.deviceNumber, frame.payload, null)
        assertNotNull(rower)
        assertEquals("39622", rower!!.displayId)
        assertEquals("ROWERG", rower.equipmentType)
        assertEquals(29f, rower.cadenceRpm, 0.01f)
        assertEquals(235, rower.powerWatts)

        // Page 24 (SkiErg): 36 s/m, 190 W
        val skiPayload = byteArrayOf(0x18, 0xFF.toByte(), 0xFF.toByte(), 12, 36, 190.toByte(), 0x00, 0x30)
        val skier = AntPlusFecParser.parseFecPage(14502, skiPayload, null)
        assertNotNull(skier)
        assertEquals("14502", skier!!.displayId)
        assertEquals("SKIERG", skier.equipmentType)
        assertEquals(36f, skier.cadenceRpm, 0.01f)
        assertEquals(190, skier.powerWatts)

        // Page 25 (BikeErg): 88 RPM, 260 W
        val bikePayload = byteArrayOf(0x19, 10, 88, 0x00, 0x00, (260 and 0xFF).toByte(), ((260 shr 8) and 0xFF).toByte(), 0x30)
        val biker = AntPlusFecParser.parseFecPage(51234, bikePayload, null)
        assertNotNull(biker)
        assertEquals("51234", biker!!.displayId)
        assertEquals("BIKEERG", biker.equipmentType)
        assertEquals(88f, biker.cadenceRpm, 0.01f)
        assertEquals(260, biker.powerWatts)
    }

    @Test
    fun testZeroAndNegativeWattsReturnPlaceholder() {
        assertNull(calculateConcept2SplitSeconds(0))
        assertNull(calculateConcept2SplitSeconds(-10))
        assertEquals("-:--", formatConcept2SplitPace(0, showTenths = false))
        assertEquals("-:--.-", formatConcept2SplitPace(0, showTenths = true))
    }

    @Test
    fun testStandardConcept2SplitPaceFromWatts() {
        // Concept2 official formula: splitSeconds = 500 * cbrt(2.80 / watts)
        assertEquals(120.51, calculateConcept2SplitSeconds(200)!!, 0.05)
        assertEquals("2:00.5", formatConcept2SplitPace(200, showTenths = true))
        assertEquals("2:01", formatConcept2SplitPace(200, showTenths = false))

        assertEquals("1:45.3", formatConcept2SplitPace(300, showTenths = true))
        assertEquals("1:45", formatConcept2SplitPace(300, showTenths = false))

        assertEquals("2:31.8", formatConcept2SplitPace(100, showTenths = true))
        assertEquals("2:32", formatConcept2SplitPace(100, showTenths = false))
    }
}
