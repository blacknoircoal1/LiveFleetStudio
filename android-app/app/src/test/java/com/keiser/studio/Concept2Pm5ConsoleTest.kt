package com.keiser.studio

import com.keiser.studio.parser.Concept2PacketParser
import com.keiser.studio.ui.components.calculateConcept2SplitSeconds
import com.keiser.studio.ui.components.formatConcept2SplitPace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class Concept2Pm5ConsoleTest {

    @Test
    fun testConcept2EquipmentTypeDetection() {
        assertEquals("BIKEERG", Concept2PacketParser.detectEquipmentType("PM5 430012345 Bike"))
        assertEquals("BIKEERG", Concept2PacketParser.detectEquipmentType("BikeErg #3"))
        assertEquals("ROWERG", Concept2PacketParser.detectEquipmentType("PM5 430012345 Row"))
        assertEquals("ROWERG", Concept2PacketParser.detectEquipmentType("RowErg 7"))
        assertEquals("SKIERG", Concept2PacketParser.detectEquipmentType("PM5 430012345 Ski"))
        assertEquals("SKIERG", Concept2PacketParser.detectEquipmentType("SkiErg 2"))
        assertEquals("", Concept2PacketParser.detectEquipmentType("PM5 430012345"))
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
        // 200 W => ~120.51s => 2:00.5 (compact: 2:01)
        assertEquals(120.51, calculateConcept2SplitSeconds(200)!!, 0.05)
        assertEquals("2:00.5", formatConcept2SplitPace(200, showTenths = true))
        assertEquals("2:01", formatConcept2SplitPace(200, showTenths = false))

        // 300 W => ~105.27s => 1:45.3 (compact: 1:45)
        assertEquals("1:45.3", formatConcept2SplitPace(300, showTenths = true))
        assertEquals("1:45", formatConcept2SplitPace(300, showTenths = false))

        // 100 W => ~151.83s => 2:31.8 (compact: 2:32)
        assertEquals("2:31.8", formatConcept2SplitPace(100, showTenths = true))
        assertEquals("2:32", formatConcept2SplitPace(100, showTenths = false))
    }
}
