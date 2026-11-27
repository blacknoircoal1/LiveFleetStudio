package com.livefleet.studio.model

/**
 * Instructor-configurable Center / Hero metric for bike console tiles.
 */
enum class TileHeroMetric(
    val id: String,
    val displayName: String,
    val subtitle: String
) {
    WATTS(
        id = "watts",
        displayName = "Watts (Default)",
        subtitle = "Large power readout in the center; RPM & % FTP in the footer"
    ),
    RPM(
        id = "rpm",
        displayName = "RPM / Cadence",
        subtitle = "Large RPM (or SPM on RowErg/SkiErg) in the center; Watts & % FTP in the footer"
    ),
    FTP_PERCENT(
        id = "ftp_percent",
        displayName = "% FTP",
        subtitle = "Large % FTP effort in the center; Watts & RPM in the footer"
    );

    companion object {
        fun fromId(id: String?): TileHeroMetric {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: WATTS
        }
    }
}

data class CyclingTileMetrics(
    val heroValue: String,
    val heroGhost: String,
    val heroUnitCompact: String,
    val heroUnitFull: String,
    val leftFooterValue: String,
    val leftFooterGhost: String,
    val leftFooterLabel: String,
    val rightFooterValue: String,
    val rightFooterGhost: String,
    val rightFooterLabel: String,
    val isRightFooterZoneColored: Boolean,
    val isHeroZoneColored: Boolean
)

fun resolveCyclingTileMetrics(
    bike: BikeData,
    heroMetric: TileHeroMetric
): CyclingTileMetrics {
    val wattsStr = "${bike.powerWatts}"
    val wattsGhost = if (wattsStr.length >= 4) "8888" else "888"
    val rpmStr = "${bike.cadenceRpm.toInt()}"
    val ftpStr = "${bike.ftpPercent}"
    val ftpGhost = if (bike.ftpPercent >= 1000) "8888" else "888"

    return when (heroMetric) {
        TileHeroMetric.WATTS -> CyclingTileMetrics(
            heroValue = wattsStr,
            heroGhost = wattsGhost,
            heroUnitCompact = "W",
            heroUnitFull = "WATTS",
            leftFooterValue = rpmStr,
            leftFooterGhost = "888",
            leftFooterLabel = "RPM",
            rightFooterValue = ftpStr,
            rightFooterGhost = ftpGhost,
            rightFooterLabel = "% FTP",
            isRightFooterZoneColored = true,
            isHeroZoneColored = false
        )
        TileHeroMetric.RPM -> CyclingTileMetrics(
            heroValue = rpmStr,
            heroGhost = "888",
            heroUnitCompact = "RPM",
            heroUnitFull = "RPM",
            leftFooterValue = wattsStr,
            leftFooterGhost = wattsGhost,
            leftFooterLabel = "WATTS",
            rightFooterValue = ftpStr,
            rightFooterGhost = ftpGhost,
            rightFooterLabel = "% FTP",
            isRightFooterZoneColored = true,
            isHeroZoneColored = false
        )
        TileHeroMetric.FTP_PERCENT -> CyclingTileMetrics(
            heroValue = ftpStr,
            heroGhost = ftpGhost,
            heroUnitCompact = "%",
            heroUnitFull = "% FTP",
            leftFooterValue = wattsStr,
            leftFooterGhost = wattsGhost,
            leftFooterLabel = "WATTS",
            rightFooterValue = rpmStr,
            rightFooterGhost = "888",
            rightFooterLabel = "RPM",
            isRightFooterZoneColored = false,
            isHeroZoneColored = true
        )
    }
}
