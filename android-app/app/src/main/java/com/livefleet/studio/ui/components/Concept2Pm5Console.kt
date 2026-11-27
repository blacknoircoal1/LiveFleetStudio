package com.livefleet.studio.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livefleet.studio.model.BikeData
import com.livefleet.studio.model.Concept2MetricMode
import com.livefleet.studio.model.TileHeroMetric
import com.livefleet.studio.ui.theme.*
import kotlin.math.pow
import kotlin.math.roundToInt

// Authentic Concept2 PM5 Monochrome Backlit LCD Palette
private val Pm5BezelDark = Color(0xFF1A1D22)
private val Pm5BezelInnerRim = Color(0xFF2D333D)
private val Pm5BezelButton = Color(0xFF3E4552)
private val Pm5BezelButtonBorder = Color(0xFF566070)

private val Pm5LcdTop = Color(0xFFD6E2EC)
private val Pm5LcdBottom = Color(0xFFC8D7E4)
private val Pm5LcdBorder = Color(0xFF7B8C9D)
private val Pm5HeaderBar = Color(0xFF25323F)
private val Pm5LcdInk = Color(0xFF141D26)
private val Pm5LcdInkMuted = Color(0xFF3F5162)
private val Pm5LcdRule = Color(0xFF8FA2B4)

/**
 * Computes Concept2 500m/1000m split pace in seconds from power (watts) using
 * Concept2's official cubic ergometer formula: Watts = 2.80 / (split / 500)^3.
 */
fun calculateConcept2SplitSeconds(powerWatts: Int): Double? {
    if (powerWatts <= 0) return null
    val pacePerMeter = (2.80 / powerWatts.toDouble()).pow(1.0 / 3.0)
    return pacePerMeter * 500.0
}

/**
 * Formats Concept2 split pace as "M:SS" (compact) or "M:SS.d" (standard).
 */
fun formatConcept2SplitPace(
    powerWatts: Int,
    showTenths: Boolean = false,
    liveSplitSeconds: Float? = null
): String {
    val totalSeconds = liveSplitSeconds?.toDouble()
        ?: calculateConcept2SplitSeconds(powerWatts)
        ?: return if (showTenths) "-:--.-" else "-:--"
    if (totalSeconds <= 0.0) return if (showTenths) "-:--.-" else "-:--"
    if (showTenths) {
        val totalTenths = (totalSeconds * 10.0).roundToInt()
        val mins = totalTenths / 600
        val secs = (totalTenths % 600) / 10
        val tenths = totalTenths % 10
        return String.format("%d:%02d.%d", mins, secs, tenths)
    } else {
        val roundedSeconds = totalSeconds.roundToInt()
        val mins = roundedSeconds / 60
        val secs = roundedSeconds % 60
        return String.format("%d:%02d", mins, secs)
    }
}

/**
 * Concept2 PM5 Performance Monitor Studio Console.
 * Matches the 3 authentic PM5 hardware Units screens:
 *  1. Pace & Metres: Split Pace (/500m or /1000m) -> Metres (m) -> Stroke Rate (s/m or RPM)
 *  2. Watts & Avg:   Live Watts (watt) -> Avg Watts (ave watt) -> Stroke Rate (s/m or RPM)
 *  3. Cal/hr & Cal:  Calories/hr (Cal/hr) -> Total Calories (Cal) -> Stroke Rate (s/m or RPM)
 */
@Composable
fun Concept2Pm5Console(
    bike: BikeData,
    isFtpTestActive: Boolean,
    isCompact: Boolean,
    zoneCount: Int,
    sessionElapsedSeconds: Long = 0L,
    concept2MetricMode: Concept2MetricMode = Concept2MetricMode.METRES,
    heroMetric: TileHeroMetric = TileHeroMetric.WATTS,
    modifier: Modifier = Modifier
) {
    val resolvedErgType = bike.equipmentType.uppercase().ifEmpty { "BIKEERG" }
    val isBikeErg = resolvedErgType == "BIKEERG"
    val isRowErg = resolvedErgType == "ROWERG"
    val isSkiErg = resolvedErgType == "SKIERG"

    val ergHeaderLabel = when {
        bike.isStale -> "IDLE"
        isRowErg -> "RowErg"
        isSkiErg -> "SkiErg"
        else -> "BikeErg"
    }
    val ergFooterLabel = when {
        isRowErg -> "ROWERG • PM5"
        isSkiErg -> "SKIERG • PM5"
        else -> "BIKEERG • PM5"
    }
    val paceUnit = if (isBikeErg) "/1000m" else "/500m"
    val cadenceUnit = if (isBikeErg) "RPM" else "s/m"

    val cardAlpha = if (bike.isStale) 0.45f else 1.0f

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .alpha(cardAlpha)
            .padding(2.dp)
    ) {
        val totalHeight = maxHeight
        val totalWidth = maxWidth
        val minDim = minOf(totalWidth.value, totalHeight.value)
        val isUltraDense = totalHeight < 92.dp || totalWidth < 95.dp
        val isNarrowTile = totalWidth < 125.dp
        val isVeryCompact = isUltraDense || totalHeight < 138.dp || totalWidth < 135.dp || isCompact
        val showTenths = !isVeryCompact && totalWidth >= 175.dp
        val showBezelFooter = !isVeryCompact && totalHeight >= 158.dp
        val compactErgLabel = when {
            bike.isStale -> "IDLE"
            isNarrowTile && isRowErg -> "ROW"
            isNarrowTile && isSkiErg -> "SKI"
            isNarrowTile -> "BIKE"
            else -> ergHeaderLabel
        }

        val splitPaceText = formatConcept2SplitPace(
            powerWatts = bike.powerWatts,
            showTenths = showTenths,
            liveSplitSeconds = bike.liveSplitSeconds
        )
        val pm5IdLabel = bike.displayId.ifEmpty { "#${bike.bikeId}" }

        val heroFontSize: TextUnit = minOf(minDim * 0.21f, totalWidth.value * 0.175f)
            .coerceIn(if (isUltraDense) 11f else 15f, 72f).sp
        val heroUnitSize: TextUnit = (minDim * 0.056f).coerceIn(if (isUltraDense) 5.5f else 7f, 20f).sp
        val subFontSize: TextUnit = minOf(minDim * 0.145f, totalWidth.value * 0.13f)
            .coerceIn(if (isUltraDense) 9f else 11.5f, 46f).sp
        val subUnitSize: TextUnit = (minDim * 0.05f).coerceIn(if (isUltraDense) 5.5f else 6.5f, 17f).sp
        val bottomRateSize: TextUnit = minOf(minDim * 0.14f, totalWidth.value * 0.125f)
            .coerceIn(if (isUltraDense) 8.5f else 11f, 44f).sp
        val headerFontSize: TextUnit = (minDim * 0.068f).coerceIn(if (isUltraDense) 7f else 8f, 22f).sp

        val compactPaceUnit = if (isUltraDense && totalWidth < 105.dp) {
            if (isBikeErg) "/1k" else "/500"
        } else {
            paceUnit
        }

        val calPerHr = if (bike.caloriesPerHour > 0) {
            bike.caloriesPerHour
        } else if (bike.powerWatts > 0) {
            (bike.powerWatts * 3.4416 + 300.0).roundToInt()
        } else {
            0
        }
        val avgWattsVal = if (bike.avgWatts > 0) bike.avgWatts else bike.powerWatts

        // Resolve the 3 authentic PM5 hardware Units modes
        val heroValue: String
        val heroUnit: String
        val midValue: String
        val midUnit: String
        when (concept2MetricMode) {
            Concept2MetricMode.METRES -> {
                heroValue = splitPaceText
                heroUnit = compactPaceUnit
                midValue = "${bike.distanceMeters}"
                midUnit = "m"
            }
            Concept2MetricMode.WATTS -> {
                heroValue = "${bike.powerWatts}"
                heroUnit = if (isNarrowTile) "W" else "watt"
                midValue = "$avgWattsVal"
                midUnit = if (isNarrowTile) "avgW" else "ave watt"
            }
            Concept2MetricMode.CALORIES -> {
                heroValue = "$calPerHr"
                heroUnit = if (isNarrowTile) "C/hr" else "Cal/hr"
                midValue = "${bike.calories}"
                midUnit = "Cal"
            }
        }

        // OUTER PM5 BEZEL HOUSING
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(Pm5BezelDark)
                .border(BorderStroke(1.dp, Pm5BezelInnerRim), RoundedCornerShape(8.dp))
                .padding(
                    start = if (isVeryCompact) 3.dp else 5.dp,
                    top = if (isVeryCompact) 3.dp else 5.dp,
                    end = if (isVeryCompact) 3.dp else 4.dp,
                    bottom = if (showBezelFooter) 2.dp else if (isVeryCompact) 3.dp else 5.dp
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(if (isVeryCompact) 2.dp else 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // CLASSIC MONOCHROME BACKLIT PM5 LCD PANEL
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Brush.verticalGradient(listOf(Pm5LcdTop, Pm5LcdBottom)))
                        .border(BorderStroke(1.dp, Pm5LcdBorder), RoundedCornerShape(4.dp))
                        .padding(
                            horizontal = if (isVeryCompact) 4.dp else 6.dp,
                            vertical = if (isVeryCompact) 2.5.dp else 4.dp
                        ),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // 1. INVERTED PM5 HEADER BAR (Monochrome)
                    Surface(
                        color = Pm5HeaderBar,
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = if (isVeryCompact) 4.dp else 6.dp, vertical = 1.5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                if (!isNarrowTile) {
                                    Text(
                                        text = "PM5",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = if (isVeryCompact) 7.sp else 8.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFB8C7D1),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                Text(
                                    text = pm5IdLabel,
                                    style = TightTabularNumTextStyle,
                                    fontFamily = Dseg7FontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = headerFontSize,
                                    lineHeight = headerFontSize,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            Text(
                                text = compactErgLabel,
                                style = TightTabularNumTextStyle,
                                fontFamily = FontFamily.Monospace,
                                fontSize = (headerFontSize.value * 0.85f).coerceIn(6.5f, 18f).sp,
                                fontWeight = FontWeight.Bold,
                                color = if (bike.isStale) Color(0xFFFCA5A5) else Color(0xFFB8C7D1),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    if (isUltraDense) {
                        // Ultra-dense 30-erg card: Hero readout centered + clean bottom strip (midValue | rate)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(horizontalArrangement = Arrangement.Center) {
                                Text(
                                    text = heroValue,
                                    style = TightTabularNumTextStyle,
                                    fontFamily = Dseg7FontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = heroFontSize,
                                    lineHeight = heroFontSize,
                                    color = Pm5LcdInk,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.alignByBaseline()
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = heroUnit,
                                    style = TightTabularNumTextStyle,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = heroUnitSize,
                                    lineHeight = heroUnitSize,
                                    fontWeight = FontWeight.Black,
                                    color = Pm5LcdInkMuted,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.alignByBaseline()
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.8.dp)
                                .background(Pm5LcdRule)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 1.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$midValue$midUnit",
                                style = TightTabularNumTextStyle,
                                fontFamily = FontFamily.Monospace,
                                fontSize = subUnitSize,
                                lineHeight = subUnitSize,
                                fontWeight = FontWeight.Bold,
                                color = Pm5LcdInkMuted,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "${bike.cadenceRpm.toInt()}$cadenceUnit",
                                style = TightTabularNumTextStyle,
                                fontFamily = FontFamily.Monospace,
                                fontSize = subUnitSize,
                                lineHeight = subUnitSize,
                                fontWeight = FontWeight.Black,
                                color = Pm5LcdInk,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    } else {
                        // Authentic PM5 3-band LCD layout matching Concept2 hardware Units screen:
                        // Band 1: Hero Metric (Pace OR Watt OR Cal/hr)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1.2f),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = heroValue,
                                    style = TightTabularNumTextStyle,
                                    fontFamily = Dseg7FontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = heroFontSize,
                                    lineHeight = heroFontSize,
                                    color = Pm5LcdInk,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.alignByBaseline()
                                )
                                Spacer(modifier = Modifier.width(if (isVeryCompact) 2.dp else 4.dp))
                                Text(
                                    text = heroUnit,
                                    style = TightTabularNumTextStyle,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = heroUnitSize,
                                    lineHeight = heroUnitSize,
                                    fontWeight = FontWeight.Black,
                                    color = Pm5LcdInkMuted,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.alignByBaseline()
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Pm5LcdRule)
                        )

                        // Band 2: Secondary Metric (Metres OR Ave Watt OR Total Cal)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.9f),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Row(horizontalArrangement = Arrangement.End) {
                                Text(
                                    text = midValue,
                                    style = TightTabularNumTextStyle,
                                    fontFamily = Dseg7FontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = subFontSize,
                                    lineHeight = subFontSize,
                                    color = Pm5LcdInk,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.alignByBaseline()
                                )
                                Spacer(modifier = Modifier.width(if (isVeryCompact) 2.dp else 4.dp))
                                Text(
                                    text = midUnit,
                                    style = TightTabularNumTextStyle,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = subUnitSize,
                                    lineHeight = subUnitSize,
                                    fontWeight = FontWeight.Bold,
                                    color = Pm5LcdInkMuted,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.alignByBaseline()
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Pm5LcdRule)
                        )

                        // Band 3: Bottom Row with vertical divider matching PM5 (Left: Stroke Rate s/m or RPM)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.85f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(0.45f),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Text(
                                    text = "${bike.cadenceRpm.toInt()}",
                                    style = TightTabularNumTextStyle,
                                    fontFamily = Dseg7FontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = bottomRateSize,
                                    lineHeight = bottomRateSize,
                                    color = Pm5LcdInk,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.alignByBaseline()
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = cadenceUnit,
                                    style = TightTabularNumTextStyle,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = subUnitSize,
                                    lineHeight = subUnitSize,
                                    fontWeight = FontWeight.Black,
                                    color = Pm5LcdInkMuted,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.alignByBaseline()
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .fillMaxHeight()
                                    .background(Pm5LcdRule)
                            )

                            Box(
                                modifier = Modifier
                                    .weight(0.55f)
                                    .padding(start = 4.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Text(
                                    text = if (concept2MetricMode == Concept2MetricMode.METRES) {
                                        "${bike.powerWatts} W"
                                    } else {
                                        "${bike.distanceMeters} m"
                                    },
                                    style = TightTabularNumTextStyle,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = subUnitSize,
                                    lineHeight = subUnitSize,
                                    fontWeight = FontWeight.Bold,
                                    color = Pm5LcdInkMuted,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                // ICONIC PM5 RIGHT-EDGE 5-BUTTON COLUMN (hidden on narrow 30-machine tiles)
                if (!isNarrowTile) {
                    Column(
                        modifier = Modifier
                            .width(if (isVeryCompact) 4.dp else 7.dp)
                            .fillMaxHeight()
                            .padding(vertical = if (isVeryCompact) 6.dp else 10.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        repeat(5) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (isVeryCompact) 7.dp else 11.dp)
                                    .clip(RoundedCornerShape(1.5.dp))
                                    .background(Pm5BezelButton)
                                    .border(0.5.dp, Pm5BezelButtonBorder, RoundedCornerShape(1.5.dp))
                            )
                        }
                    }
                }
            }

            if (showBezelFooter) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, start = 2.dp, end = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "concept 2",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF9CA3AF),
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = ergFooterLabel,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 6.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6B7280),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
