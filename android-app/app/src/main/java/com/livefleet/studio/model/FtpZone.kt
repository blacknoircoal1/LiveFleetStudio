package com.livefleet.studio.model

import androidx.compose.ui.graphics.Color

enum class FtpZone(
    val code: String,
    val label: String,
    val color: Color,
    val zoneNumber: Int
) {
    ZONE_1("Z1", "Z1 Recovery", Color(0xFF6B7280), 1),
    ZONE_2("Z2", "Z2 Endurance", Color(0xFF00A3E0), 2),
    ZONE_3("Z3", "Z3 Tempo", Color(0xFF00C853), 3),
    ZONE_4("Z4", "Z4 Threshold", Color(0xFFFF9100), 4),
    ZONE_4_7Z("Z4", "Z4 Threshold", Color(0xFFFF9100), 4),
    ZONE_5("Z5", "Z5 VO2 Max", Color(0xFFD50000), 5),
    ZONE_5_7Z("Z5", "Z5 VO2 Max", Color(0xFFD50000), 5),
    ZONE_6("Z6", "Z6 Anaerobic", Color(0xFFAA00FF), 6),
    ZONE_7("Z7", "Z7 Neuromuscular", Color(0xFFAA00FF), 7);

    companion object {
        fun fromFtpPercent(pct: Int, zoneCount: Int = 5): FtpZone {
            return if (zoneCount == 7) {
                when {
                    pct <= 55 -> ZONE_1
                    pct <= 75 -> ZONE_2
                    pct <= 90 -> ZONE_3
                    pct <= 105 -> ZONE_4_7Z
                    pct <= 120 -> ZONE_5_7Z
                    pct <= 150 -> ZONE_6
                    else -> ZONE_7
                }
            } else {
                when {
                    pct <= 55 -> ZONE_1
                    pct <= 75 -> ZONE_2
                    pct <= 90 -> ZONE_3
                    pct <= 105 -> ZONE_4
                    else -> ZONE_5
                }
            }
        }
    }
}
