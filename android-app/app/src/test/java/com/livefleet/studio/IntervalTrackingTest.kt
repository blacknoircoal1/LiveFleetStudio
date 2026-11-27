package com.livefleet.studio

import com.livefleet.studio.model.FtpZone
import com.livefleet.studio.model.IntervalAccumulator
import com.livefleet.studio.model.computeBikeIntervalStats
import org.junit.Assert.assertEquals
import org.junit.Test

class IntervalTrackingTest {

    @Test
    fun testComputeBikeIntervalStatsCalculatesAvgPeakRpmAndZone() {
        val acc = IntervalAccumulator(
            bikeId = 4,
            displayId = "#4",
            riderName = "Bike #4",
            ftp = 200,
            wattSum = 220L + 260L + 240L,
            cadenceSum = 85.0 + 90.0 + 89.0,
            peakWatts = 260,
            sampleCount = 3
        )

        val stats = computeBikeIntervalStats(acc, zoneCount = 5)
        assertEquals(4, stats.bikeId)
        assertEquals(240, stats.avgWatts)
        assertEquals(260, stats.peakWatts)
        assertEquals(88.0f, stats.avgCadence, 0.1f)
        assertEquals(120, stats.avgFtpPercent)
        assertEquals(FtpZone.ZONE_5, stats.ftpZone)
    }

    @Test
    fun testComputeBikeIntervalStatsHandlesZeroSamplesSafely() {
        val acc = IntervalAccumulator(
            bikeId = 1,
            displayId = "#1",
            riderName = "Bike #1",
            ftp = 200
        )

        val stats = computeBikeIntervalStats(acc, zoneCount = 5)
        assertEquals(0, stats.avgWatts)
        assertEquals(0, stats.peakWatts)
        assertEquals(0f, stats.avgCadence, 0.01f)
        assertEquals(0, stats.avgFtpPercent)
        assertEquals(FtpZone.ZONE_1, stats.ftpZone)
    }
}
