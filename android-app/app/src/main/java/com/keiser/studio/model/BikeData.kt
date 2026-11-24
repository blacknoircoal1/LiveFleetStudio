package com.keiser.studio.model

data class BikeData(
    val bikeId: Int,
    val fwMajor: Int = 6,
    val fwMinor: Int = 98,
    val dataType: Int = 255,
    val cadenceRpm: Float = 0f,
    val heartRateBpm: Float = 0f,
    val powerWatts: Int = 0,
    val calories: Int = 0,
    val distance: Float = 0f,
    val distanceUnit: String = "MI",
    val gear: Int = 1,
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val isStale: Boolean = false,
    val riderName: String = "Rider #$bikeId",
    val brand: String = "Keiser",
    val equipmentType: String = "",
    val ftp: Int = 200,
    val ftpPercent: Int = 0,
    val ftpZone: FtpZone = FtpZone.ZONE_1
)
