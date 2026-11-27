package com.livefleet.studio.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livefleet.studio.model.BikeData
import com.livefleet.studio.model.TileHeroMetric
import com.livefleet.studio.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * Borderless Floating Dial Gauge matching the Top-Left reference style of Ride by Color.
 * Seamless on pitch black studio background, no card outlines.
 * Perfectly centered cavity text with prominent Bike Number.
 */
@Composable
fun FloatingBikeDial(
    bike: BikeData,
    isFtpTestActive: Boolean,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    zoneCount: Int = 5,
    heroMetric: TileHeroMetric = TileHeroMetric.FTP_PERCENT
) {
    val dialAlpha = if (bike.isStale) 0.35f else 1.0f

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .alpha(dialAlpha)
            .padding(horizontal = 2.dp, vertical = 1.dp)
    ) {
        val tileMinDim = minOf(maxWidth.value, maxHeight.value)
        val isUltraDense = maxHeight < 92.dp || maxWidth < 92.dp
        val readoutFontSize = (tileMinDim * 0.115f).coerceIn(
            if (isUltraDense) 8.5f else if (isCompact) 11.5f else 13.5f,
            52f
        ).sp
        val readoutUnitFontSize = (tileMinDim * 0.068f).coerceIn(
            if (isUltraDense) 6f else if (isCompact) 8f else 9.5f,
            26f
        ).sp
        val dotFontSize = (tileMinDim * 0.075f).coerceIn(if (isUltraDense) 6f else 8f, 28f).sp
        val readoutHorizontalGap = (tileMinDim * 0.03f).coerceIn(if (isUltraDense) 2f else 4f, 16f).dp
        val tightStyle = TabularNumTextStyle.merge(
            TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Floating Tachometer Circular Dial Box
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 1.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                val boxWidth = maxWidth
                val boxHeight = maxHeight

                // Mathematical positioning for cavity anchored to the wide base of the semi-circle
                val centerYRatio = 0.96f
                val arcRadiusDp = (boxWidth * 0.43f).coerceAtMost(boxHeight * 0.72f)
                val strokeWidthDp = (arcRadiusDp.value * 0.18f).coerceIn(
                    if (isUltraDense) 4.5f else if (isCompact) 7f else 9f,
                    28f
                ).dp
                val innerRadiusDp = (arcRadiusDp - (strokeWidthDp / 2f)).coerceAtLeast(12.dp)

                // Proportional font and cavity sizing so both big screens and ultra-dense 30+ grids fit cleanly
                val showFtpLabel = !isUltraDense && innerRadiusDp >= 34.dp
                val pctScale = when {
                    bike.ftpPercent >= 1000 -> 0.17f
                    bike.ftpPercent >= 100 -> 0.22f
                    else -> 0.27f
                }
                val pctFontSize = (innerRadiusDp.value * pctScale).coerceIn(
                    if (isUltraDense) 6.5f else if (isCompact) 10.5f else 14f,
                    60f
                ).sp
                val ftpFontSize = (innerRadiusDp.value * 0.125f).coerceIn(6.5f, 24f).sp
                val bikeNumFontSize = (innerRadiusDp.value * 0.27f).coerceIn(
                    if (isUltraDense) 7.5f else if (isCompact) 12.5f else 16f,
                    56f
                ).sp
                val cavityBottomPadding = (boxHeight * (1f - centerYRatio)).coerceAtLeast(1.dp)

                // 1. Draw Dial Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    val centerX = canvasWidth / 2f
                    val centerY = canvasHeight * centerYRatio

                    val arcRadius = (canvasWidth * 0.43f).coerceAtMost(canvasHeight * 0.72f)
                    val arcStrokeWidth = strokeWidthDp.toPx()
                    val arcRect = Rect(
                        left = centerX - arcRadius,
                        top = centerY - arcRadius,
                        right = centerX + arcRadius,
                        bottom = centerY + arcRadius
                    )

                    // 5-Zone or 7-Zone (6 visual color bands: Grey, Blue, Green, Orange, Red, Purple as Z6 & Z7)
                    val zones = if (zoneCount == 7) {
                        val step = 180f / 6f
                        listOf(
                            Pair(180f, Zone1Color),             // Z1: Grey
                            Pair(180f + step * 1f, Zone2Color), // Z2: Blue
                            Pair(180f + step * 2f, Zone3Color), // Z3: Green
                            Pair(180f + step * 3f, Zone4Color), // Z4: Orange
                            Pair(180f + step * 4f, Zone5Color), // Z5: Red
                            Pair(180f + step * 5f, Zone67Color) // Z6 & Z7: Purple
                        )
                    } else {
                        listOf(
                            Pair(180f, KeiserZone1),
                            Pair(216f, KeiserZone2),
                            Pair(252f, KeiserZone3),
                            Pair(288f, KeiserZone4),
                            Pair(324f, KeiserZone5)
                        )
                    }

                    val sweepAngle = 180f / zones.size
                    val zoneFrac = 1f / zones.size
                    val progressFraction = calculateProgress(bike.ftpPercent, zoneCount)

                    zones.forEachIndexed { index, (startAngle, color) ->
                        val zoneStartFrac = index * zoneFrac
                        val zoneColor = if (progressFraction >= zoneStartFrac) {
                            color
                        } else {
                            color.copy(alpha = 0.20f)
                        }

                        drawArc(
                            color = zoneColor,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = arcRect.topLeft,
                            size = arcRect.size,
                            style = Stroke(width = arcStrokeWidth, cap = StrokeCap.Butt)
                        )
                    }

                    // Outer Segmented Progressive Ladder Ticks
                    val totalTicks = 34
                    val tickInner = arcRadius + (arcStrokeWidth / 2f) + (if (isCompact) 1.5.dp.toPx() else 2.5.dp.toPx())
                    val tickOuter = tickInner + (arcRadius * 0.08f).coerceIn(3.5.dp.toPx(), 12.dp.toPx())

                    for (i in 0 until totalTicks) {
                        val frac = i.toFloat() / (totalTicks - 1)
                        val angleDeg = 180f + (frac * 180f)
                        val angleRad = Math.toRadians(angleDeg.toDouble())

                        val startX = centerX + (tickInner * cos(angleRad)).toFloat()
                        val startY = centerY + (tickInner * sin(angleRad)).toFloat()
                        val endX = centerX + (tickOuter * cos(angleRad)).toFloat()
                        val endY = centerY + (tickOuter * sin(angleRad)).toFloat()

                        val isFilled = frac <= progressFraction
                        val tickColor = if (isFilled) Color.White else Color(0x22FFFFFF)
                        val tickW = if (isFilled) (arcRadius * 0.03f).coerceIn(1.5.dp.toPx(), 4.dp.toPx()) else 1.dp.toPx()

                        drawLine(
                            color = tickColor,
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = tickW,
                            cap = StrokeCap.Round
                        )
                    }

                    // Inner Dark Well Cutout
                    val innerWellRadius = arcRadius - (arcStrokeWidth / 2f) - 1.dp.toPx()
                    drawCircle(
                        color = Color(0xFF04060A),
                        radius = innerWellRadius,
                        center = Offset(centerX, centerY)
                    )
                }

                val cavityHeroText = when (heroMetric) {
                    TileHeroMetric.WATTS -> "${bike.powerWatts}"
                    TileHeroMetric.RPM -> "${bike.cadenceRpm.toInt()}"
                    TileHeroMetric.FTP_PERCENT -> "${bike.ftpPercent}%"
                }
                val cavityHeroLabel = when (heroMetric) {
                    TileHeroMetric.WATTS -> "WATTS"
                    TileHeroMetric.RPM -> "RPM"
                    TileHeroMetric.FTP_PERCENT -> "FTP"
                }

                // 2. Base-Anchored Cavity Content: Hero Metric, Label, Big Bike Number
                Column(
                    modifier = Modifier
                        .padding(bottom = cavityBottomPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // Large Digital Hero Metric
                    Text(
                        text = cavityHeroText,
                        style = tightStyle,
                        fontFamily = Dseg7FontFamily,
                        fontSize = pctFontSize,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = pctFontSize,
                        maxLines = 1,
                        softWrap = false
                    )

                    // Metric Label (hidden in ultra-dense tiles so hero and #id never collide)
                    if (showFtpLabel) {
                        Text(
                            text = cavityHeroLabel,
                            style = tightStyle,
                            fontSize = ftpFontSize,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 1.sp,
                            lineHeight = ftpFontSize,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    // BIG BIKE NUMBER / SERIAL ID (Prominently readable from distance!)
                    Text(
                        text = bike.displayId.ifEmpty { "#${bike.bikeId}" },
                        style = tightStyle,
                        fontSize = if (bike.displayId.length >= 5) (bikeNumFontSize.value * 0.78f).sp else bikeNumFontSize,
                        fontWeight = FontWeight.Black,
                        color = AccentEnergy,
                        lineHeight = bikeNumFontSize,
                        maxLines = 1,
                        softWrap = false
                    )

                    if (bike.isStale && !isUltraDense) {
                        Text(
                            text = "IDLE",
                            style = tightStyle,
                            fontSize = (innerRadiusDp.value * 0.11f).coerceIn(6f, 16f).sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFCA5A5)
                        )
                    }
                }
            }

            val leftVal = when (heroMetric) {
                TileHeroMetric.WATTS -> "${bike.cadenceRpm.toInt()}"
                TileHeroMetric.RPM -> "${bike.powerWatts}"
                TileHeroMetric.FTP_PERCENT -> "${bike.cadenceRpm.toInt()}"
            }
            val leftUnit = when (heroMetric) {
                TileHeroMetric.WATTS -> "RPM"
                TileHeroMetric.RPM -> "W"
                TileHeroMetric.FTP_PERCENT -> "RPM"
            }
            val rightVal = when (heroMetric) {
                TileHeroMetric.WATTS -> "${bike.ftpPercent}"
                TileHeroMetric.RPM -> "${bike.ftpPercent}"
                TileHeroMetric.FTP_PERCENT -> "${bike.powerWatts}"
            }
            val rightUnit = when (heroMetric) {
                TileHeroMetric.WATTS -> "%"
                TileHeroMetric.RPM -> "%"
                TileHeroMetric.FTP_PERCENT -> "W"
            }

            // Under-Dial Live Readout: Baseline-aligned secondary metrics scaled for studio displays
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(bottom = if (isUltraDense) 1.dp else (tileMinDim * 0.02f).coerceIn(2f, 8f).dp),
                horizontalArrangement = Arrangement.spacedBy(readoutHorizontalGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = leftVal,
                        style = tightStyle,
                        fontSize = readoutFontSize,
                        lineHeight = readoutFontSize,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFE2E8F0),
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.alignByBaseline()
                    )
                    Text(
                        text = leftUnit,
                        style = tightStyle,
                        fontSize = readoutUnitFontSize,
                        lineHeight = readoutUnitFontSize,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.alignByBaseline()
                    )
                }

                Text(
                    text = "•",
                    style = tightStyle,
                    color = Color(0xFF475569),
                    fontSize = dotFontSize,
                    lineHeight = dotFontSize
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = rightVal,
                        style = tightStyle,
                        fontSize = readoutFontSize,
                        lineHeight = readoutFontSize,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.alignByBaseline()
                    )
                    Text(
                        text = rightUnit,
                        style = tightStyle,
                        fontSize = readoutUnitFontSize,
                        lineHeight = readoutUnitFontSize,
                        fontWeight = FontWeight.Black,
                        color = AccentEnergy,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.alignByBaseline()
                    )
                }
            }
        }
    }
}

private fun calculateProgress(ftpPercent: Int, zoneCount: Int): Float {
    if (ftpPercent <= 0) return 0.02f
    return if (zoneCount == 7) {
        // 6 visual bands: Grey(Z1), Blue(Z2), Green(Z3), Orange(Z4), Red(Z5), Purple(Z6 + Z7)
        val band = 1f / 6f
        when {
            ftpPercent <= 55 -> (ftpPercent / 55f) * band
            ftpPercent <= 75 -> band + ((ftpPercent - 55f) / 20f) * band
            ftpPercent <= 90 -> 2f * band + ((ftpPercent - 75f) / 15f) * band
            ftpPercent <= 105 -> 3f * band + ((ftpPercent - 90f) / 15f) * band
            ftpPercent <= 120 -> 4f * band + ((ftpPercent - 105f) / 15f) * band
            ftpPercent <= 150 -> 5f * band + ((ftpPercent - 120f) / 30f) * (band * 0.5f)
            else -> (5.5f * band + ((ftpPercent - 150f) / 50f) * (band * 0.5f)).coerceAtMost(1.0f)
        }
    } else {
        val segment = 0.2f
        when {
            ftpPercent <= 55 -> (ftpPercent / 55f) * segment
            ftpPercent <= 75 -> segment + ((ftpPercent - 55f) / 20f) * segment
            ftpPercent <= 90 -> 2f * segment + ((ftpPercent - 75f) / 15f) * segment
            ftpPercent <= 105 -> 3f * segment + ((ftpPercent - 90f) / 15f) * segment
            else -> (4f * segment + ((ftpPercent - 105f) / 25f) * segment).coerceAtMost(1.0f)
        }
    }
}
