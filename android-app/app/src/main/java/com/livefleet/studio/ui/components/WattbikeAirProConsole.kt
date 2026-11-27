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
import kotlin.math.roundToInt

// Authentic Wattbike Performance Monitor Palette (matching hardware photos)
private val WattbikeBezelDark = Color(0xFF141519)
private val WattbikeBezelRim = Color(0xFF282B33)
private val WattbikeRed = Color(0xFFDC2626)
private val WattbikeLcdTop = Color(0xFF9FB0A6)
private val WattbikeLcdBottom = Color(0xFF8CA095)
private val WattbikeInk = Color(0xFF111815)
private val WattbikeInkMuted = Color(0xFF314039)
private val WattbikeRule = Color(0xFF6A7D73)

/**
 * Wattbike Performance Monitor Studio Console.
 * Matches the 3 authentic Wattbike hardware Units views:
 *  1. Pace & Metres: Top-right RPM | Hero 02:30 /1km | Bottom 501 m
 *  2. Watts & Avg:   Top-right RPM | Hero 31 Watt    | Bottom 42 avg Watt
 *  3. Cal/hr & Cal:  Top-right RPM | Hero 314 kcal/hour | Bottom 11 total kcal
 */
@Composable
fun WattbikeAirProConsole(
    bike: BikeData,
    isFtpTestActive: Boolean,
    isCompact: Boolean,
    zoneCount: Int,
    sessionElapsedSeconds: Long = 0L,
    displayMode: Concept2MetricMode = Concept2MetricMode.METRES,
    heroMetric: TileHeroMetric = TileHeroMetric.WATTS,
    modifier: Modifier = Modifier
) {
    val cardAlpha = if (bike.isStale) 0.45f else 1.0f
    val zoneColor = bike.ftpZone.color
    val idLabel = bike.displayId.ifEmpty { "#${bike.bikeId}" }

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
        val showBezelFooter = !isVeryCompact && totalHeight >= 158.dp

        val headerFontSize: TextUnit = (minDim * 0.068f).coerceIn(if (isUltraDense) 7f else 8f, 22f).sp
        val rpmFontSize: TextUnit = minOf(minDim * 0.125f, totalWidth.value * 0.11f)
            .coerceIn(if (isUltraDense) 8.5f else 11f, 38f).sp
        val heroFontSize: TextUnit = minOf(minDim * 0.22f, totalWidth.value * 0.18f)
            .coerceIn(if (isUltraDense) 11f else 15f, 74f).sp
        val heroUnitSize: TextUnit = (minDim * 0.055f).coerceIn(if (isUltraDense) 5.5f else 7f, 18f).sp
        val subFontSize: TextUnit = minOf(minDim * 0.15f, totalWidth.value * 0.135f)
            .coerceIn(if (isUltraDense) 9f else 11.5f, 46f).sp
        val subUnitSize: TextUnit = (minDim * 0.05f).coerceIn(if (isUltraDense) 5.5f else 6.5f, 16f).sp

        val speedKmh = when {
            bike.speedKmh > 0.5f -> bike.speedKmh
            bike.powerWatts > 0 -> 16f + (bike.powerWatts / 15f)
            else -> 0f
        }
        val split1kmText = if (speedKmh > 0.5f) {
            val secs = (3600f / speedKmh).roundToInt()
            String.format("%02d:%02d", secs / 60, secs % 60)
        } else {
            "--:--"
        }

        val calPerHr = if (bike.caloriesPerHour > 0) {
            bike.caloriesPerHour
        } else if (bike.powerWatts > 0) {
            (bike.powerWatts * 3.6 + 300.0).roundToInt()
        } else {
            0
        }
        val avgWattsVal = if (bike.avgWatts > 0) bike.avgWatts else bike.powerWatts

        val heroValue: String
        val heroUnit: String
        val subValue: String
        val subUnit: String
        when (displayMode) {
            Concept2MetricMode.METRES -> {
                heroValue = split1kmText
                heroUnit = "/1km"
                subValue = "${bike.distanceMeters}"
                subUnit = "m"
            }
            Concept2MetricMode.WATTS -> {
                heroValue = "${bike.powerWatts}"
                heroUnit = if (isNarrowTile) "W" else "Watt"
                subValue = "$avgWattsVal"
                subUnit = if (isNarrowTile) "avgW" else "avg Watt"
            }
            Concept2MetricMode.CALORIES -> {
                heroValue = "$calPerHr"
                heroUnit = if (isNarrowTile) "kcal/h" else "kcal/hour"
                subValue = "${bike.calories}"
                subUnit = if (isNarrowTile) "kcal" else "total kcal"
            }
        }

        // OUTER WATTBIKE BEZEL WITH SIGNATURE RED LCD FRAME
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(WattbikeBezelDark)
                .border(BorderStroke(1.dp, WattbikeBezelRim), RoundedCornerShape(8.dp))
                .padding(
                    start = if (isVeryCompact) 3.dp else 5.dp,
                    top = if (isVeryCompact) 3.dp else 5.dp,
                    end = if (isVeryCompact) 3.dp else 5.dp,
                    bottom = if (showBezelFooter) 2.dp else if (isVeryCompact) 3.dp else 5.dp
                )
        ) {
            // INNER BACKLIT MONOCHROME WATTBIKE LCD WITH RED BORDER
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Brush.verticalGradient(listOf(WattbikeLcdTop, WattbikeLcdBottom)))
                    .border(BorderStroke(if (isUltraDense) 1.dp else 1.5.dp, WattbikeRed), RoundedCornerShape(4.dp))
                    .padding(
                        horizontal = if (isVeryCompact) 4.dp else 6.dp,
                        vertical = if (isVeryCompact) 2.5.dp else 4.dp
                    ),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. TOP BAR: #ID + Zone Pill on Left | Live RPM on Right (matching Wattbike LCD)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Surface(
                            color = WattbikeInk,
                            shape = RoundedCornerShape(2.dp)
                        ) {
                            Text(
                                text = idLabel,
                                style = TightTabularNumTextStyle,
                                fontFamily = FontFamily.Monospace,
                                fontSize = headerFontSize,
                                lineHeight = headerFontSize,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        if (!isNarrowTile) {
                            Surface(
                                color = if (bike.isStale) WattbikeRed else zoneColor,
                                shape = RoundedCornerShape(2.dp)
                            ) {
                                Text(
                                    text = if (bike.isStale) "IDLE" else "Z${bike.ftpZone.zoneNumber}",
                                    style = TightTabularNumTextStyle,
                                    fontSize = (headerFontSize.value * 0.85f).coerceIn(6.5f, 16f).sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Row {
                        Text(
                            text = "${bike.cadenceRpm.toInt()}",
                            style = TightTabularNumTextStyle,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Black,
                            fontSize = rpmFontSize,
                            lineHeight = rpmFontSize,
                            color = WattbikeInk,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.alignByBaseline()
                        )
                        Spacer(modifier = Modifier.width(1.5.dp))
                        Text(
                            text = "rpm",
                            style = TightTabularNumTextStyle,
                            fontFamily = FontFamily.Monospace,
                            fontSize = subUnitSize,
                            lineHeight = subUnitSize,
                            fontWeight = FontWeight.Bold,
                            color = WattbikeInkMuted,
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
                        .background(WattbikeRule)
                )

                // 2. HERO READOUT (02:30 /1km OR 31 Watt OR 314 kcal/hour)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.2f),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Row(horizontalArrangement = Arrangement.End) {
                        Text(
                            text = heroValue,
                            style = TightTabularNumTextStyle,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Black,
                            fontSize = heroFontSize,
                            lineHeight = heroFontSize,
                            color = WattbikeInk,
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
                            fontWeight = FontWeight.Bold,
                            color = WattbikeInkMuted,
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
                        .background(WattbikeRule)
                )

                // 3. BOTTOM READOUT (501 m OR 42 avg Watt OR 11 total kcal)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.9f),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Row(horizontalArrangement = Arrangement.End) {
                        Text(
                            text = subValue,
                            style = TightTabularNumTextStyle,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Black,
                            fontSize = subFontSize,
                            lineHeight = subFontSize,
                            color = WattbikeInk,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.alignByBaseline()
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = subUnit,
                            style = TightTabularNumTextStyle,
                            fontFamily = FontFamily.Monospace,
                            fontSize = subUnitSize,
                            lineHeight = subUnitSize,
                            fontWeight = FontWeight.Bold,
                            color = WattbikeInkMuted,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.alignByBaseline()
                        )
                    }
                }
            }

            if (showBezelFooter) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, start = 4.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "wattbike",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Black,
                        color = WattbikeRed,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "${bike.ftpPercent}% FTP",
                        style = TightTabularNumTextStyle,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 6.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9CA3AF),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
