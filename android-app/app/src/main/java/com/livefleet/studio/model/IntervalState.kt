package com.livefleet.studio.model

import kotlin.math.roundToInt

data class BikeIntervalStats(
    val bikeId: Int,
    val displayId: String,
    val riderName: String,
    val avgWatts: Int,
    val peakWatts: Int,
    val avgCadence: Float,
    val avgFtpPercent: Int,
    val ftpZone: FtpZone,
    val equipmentType: String = ""
)

data class CompletedInterval(
    val intervalNumber: Int,
    val durationSeconds: Long,
    val startSessionSeconds: Long,
    val classAvgWatts: Int,
    val classAvgCadence: Float,
    val bikeStats: List<BikeIntervalStats>
)

data class IntervalState(
    val isActive: Boolean = false,
    val isStripExpanded: Boolean = false,
    val currentIntervalNumber: Int = 0,
    val elapsedSeconds: Long = 0L,
    val startSessionSeconds: Long = 0L,
    val liveClassAvgWatts: Int = 0,
    val liveClassAvgCadence: Float = 0f,
    val completedIntervals: List<CompletedInterval> = emptyList(),
    val sessionDurationSeconds: Long = 0L,
    val sessionClassAvgWatts: Int = 0,
    val sessionClassAvgCadence: Float = 0f,
    val sessionBikeStats: List<BikeIntervalStats> = emptyList(),
    val isViewingSummary: Boolean = false,
    val hasCompletedSummary: Boolean = false
)

data class IntervalAccumulator(
    val bikeId: Int,
    val displayId: String,
    val riderName: String,
    val ftp: Int,
    var wattSum: Long = 0L,
    var cadenceSum: Double = 0.0,
    var peakWatts: Int = 0,
    var sampleCount: Int = 0,
    val equipmentType: String = ""
)

fun computeBikeIntervalStats(
    acc: IntervalAccumulator,
    zoneCount: Int
): BikeIntervalStats {
    val count = acc.sampleCount.coerceAtLeast(1)
    val avgWatts = (acc.wattSum.toDouble() / count).roundToInt()
    val avgCad = ((acc.cadenceSum / count) * 10.0).roundToInt() / 10f
    val avgFtpPct = if (acc.ftp > 0) (avgWatts * 100) / acc.ftp else 0
    return BikeIntervalStats(
        bikeId = acc.bikeId,
        displayId = acc.displayId,
        riderName = acc.riderName,
        avgWatts = avgWatts,
        peakWatts = acc.peakWatts,
        avgCadence = avgCad,
        avgFtpPercent = avgFtpPct,
        ftpZone = FtpZone.fromFtpPercent(avgFtpPct, zoneCount),
        equipmentType = acc.equipmentType
    )
}
