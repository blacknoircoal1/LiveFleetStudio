package com.livefleet.studio.model

data class FtpResultItem(
    val bikeId: Int,
    val riderName: String,
    val avgWatts: Int,
    val factor: Float,
    val newFtp: Int,
    val oldFtp: Int,
    val delta: Int
)

data class FtpTestState(
    val isActive: Boolean = false,
    val isPreparing: Boolean = false, // True during initial 5-second countdown
    val prepCountdownSeconds: Int = 0, // 5... 4... 3... 2... 1
    val durationSeconds: Int = 300,
    val remainingSeconds: Int = 300,
    val startSessionSeconds: Long = 0L,
    val scalingFactor: Float = 0.85f,
    val testType: String = "5-Min",
    val samples: Map<Int, List<Int>> = emptyMap(),
    val pendingResults: List<FtpResultItem>? = null,
    val lastCompletedResults: List<FtpResultItem>? = null,
    val isViewingResults: Boolean = false
)
