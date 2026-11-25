package com.keiser.studio.ui.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keiser.studio.model.BikeData
import com.keiser.studio.ui.theme.*
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
    zoneCount: Int = 5
) {
    // Cadence target enforcement during FTP test: <80 or >90 RPM -> Alert RED
    val isCadenceAlert = isFtpTestActive && (bike.cadenceRpm < 80f || bike.cadenceRpm > 90f)

    val infiniteTransition = rememberInfiniteTransition(label = "cadenceAlert")
    val alertAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alertAlpha"
    )

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
            if (isUltraDense) 6.5f else if (isCompact) 8f else 9.5f,
            26f
        ).sp
        val dotFontSize = (tileMinDim * 0.075f).coerceIn(if (isUltraDense) 6.5f else 8f, 28f).sp
        val readoutHorizontalGap = (tileMinDim * 0.03f).coerceIn(if (isUltraDense) 2f else 4f, 16f).dp

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
                contentAlignment = Alignment.TopCenter
            ) {
                val boxWidth = maxWidth
                val boxHeight = maxHeight

                // Mathematical positioning for cavity center
                val centerYRatio = 0.94f
                val arcRadiusDp = (boxWidth * 0.44f).coerceAtMost(boxHeight * 0.76f)
                val strokeWidthDp = (arcRadiusDp.value * 0.18f).coerceIn(
                    if (isUltraDense) 5f else if (isCompact) 7f else 9f,
                    28f
                ).dp
                val innerRadiusDp = arcRadiusDp - (strokeWidthDp / 2f)

                // Vertical center of the inner semi-circle cavity in dp
                val cavityCenterYDp = (boxHeight * centerYRatio) - (innerRadiusDp * 0.48f)

                // Proportional font and cavity sizing so both big screens and ultra-dense 30+ grids fit cleanly
                val showFtpLabel = !isUltraDense && innerRadiusDp >= 34.dp
                val pctScale = when {
                    bike.ftpPercent >= 1000 -> 0.19f
                    bike.ftpPercent >= 100 -> 0.25f
                    else -> 0.32f
                }
                val pctFontSize = (innerRadiusDp.value * pctScale).coerceIn(
                    if (isUltraDense) 8.5f else if (isCompact) 11f else 14f,
                    60f
                ).sp
                val ftpFontSize = (innerRadiusDp.value * 0.13f).coerceIn(6.5f, 24f).sp
                val bikeNumFontSize = (innerRadiusDp.value * 0.30f).coerceIn(
                    if (isUltraDense) 9.5f else if (isCompact) 13.5f else 16f,
                    56f
                ).sp

                // Estimated height of Column in dp to center it cleanly inside the cavity
                val colHeightEstimate = (
                    pctFontSize.value +
                        (if (showFtpLabel) ftpFontSize.value else 0f) +
                        bikeNumFontSize.value +
                        (if (isUltraDense) 2f else 4f)
                    ).dp
                val topOffsetDp = (cavityCenterYDp - (colHeightEstimate / 2f)).coerceAtLeast(1.dp)

                // 1. Draw Dial Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    val centerX = canvasWidth / 2f
                    val centerY = canvasHeight * centerYRatio

                    val arcRadius = (canvasWidth * 0.44f).coerceAtMost(canvasHeight * 0.76f)
                    val arcStrokeWidth = strokeWidthDp.toPx()
                    val arcRect = Rect(
                        left = centerX - arcRadius,
                        top = centerY - arcRadius,
                        right = centerX + arcRadius,
                        bottom = centerY + arcRadius
                    )

                    // 5-Zone or 7-Zone Arc Segments (180° to 360°)
                    val zones = if (zoneCount == 7) {
                        val step = 180f / 7f
                        listOf(
                            Pair(180f, Color(0xFF6B7280)),              // Z1: Gray
                            Pair(180f + step * 1f, Color(0xFF00A3E0)), // Z2: Blue
                            Pair(180f + step * 2f, Color(0xFF00C853)), // Z3: Green
                            Pair(180f + step * 3f, Color(0xFFFFD600)), // Z4: Yellow
                            Pair(180f + step * 4f, Color(0xFFFF9100)), // Z5: Orange
                            Pair(180f + step * 5f, Color(0xFFD50000)), // Z6: Red
                            Pair(180f + step * 6f, Color(0xFFAA00FF))  // Z7: Purple
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

                // 2. Centered Cavity Content: Percentage, FTP, Big Bike Number
                Column(
                    modifier = Modifier
                        .offset(y = topOffsetDp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // Large Digital Percentage
                    Text(
                        text = "${bike.ftpPercent}%",
                        fontFamily = Dseg7FontFamily,
                        fontSize = pctFontSize,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = pctFontSize,
                        maxLines = 1,
                        softWrap = false
                    )

                    // FTP Label (hidden in ultra-dense tiles so % and #id never collide)
                    if (showFtpLabel) {
                        Text(
                            text = "FTP",
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
                            fontSize = (innerRadiusDp.value * 0.11f).coerceIn(6f, 16f).sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFCA5A5)
                        )
                    }
                }
            }

            // Under-Dial Live Readout: High-visibility RPM • WATTS scaled for studio displays
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(bottom = (tileMinDim * 0.02f).coerceIn(2f, 8f).dp),
                horizontalArrangement = Arrangement.spacedBy(readoutHorizontalGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cadence RPM (turns red if cadence is outside target during test)
                if (isCadenceAlert) {
                    Row(
                        modifier = Modifier
                            .background(AccentDanger.copy(alpha = alertAlpha), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "${bike.cadenceRpm.toInt()}",
                            fontSize = readoutFontSize,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "RPM ⚠️",
                            fontSize = readoutUnitFontSize,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 1.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "${bike.cadenceRpm.toInt()}",
                            fontSize = readoutFontSize,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFE2E8F0),
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "RPM",
                            fontSize = readoutUnitFontSize,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            modifier = Modifier.padding(bottom = 1.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Text("•", color = Color(0xFF475569), fontSize = dotFontSize)

                // Power Watts
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "${bike.powerWatts}",
                        fontSize = readoutFontSize,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "W",
                        fontSize = readoutUnitFontSize,
                        fontWeight = FontWeight.Black,
                        color = AccentEnergy,
                        modifier = Modifier.padding(bottom = 1.dp),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

private fun calculateProgress(ftpPercent: Int, zoneCount: Int): Float {
    if (ftpPercent <= 0) return 0.02f
    return if (zoneCount == 7) {
        val segment = 1f / 7f
        when {
            ftpPercent <= 55 -> (ftpPercent / 55f) * segment
            ftpPercent <= 75 -> segment + ((ftpPercent - 55f) / 20f) * segment
            ftpPercent <= 90 -> 2f * segment + ((ftpPercent - 75f) / 15f) * segment
            ftpPercent <= 105 -> 3f * segment + ((ftpPercent - 90f) / 15f) * segment
            ftpPercent <= 120 -> 4f * segment + ((ftpPercent - 105f) / 15f) * segment
            ftpPercent <= 150 -> 5f * segment + ((ftpPercent - 120f) / 30f) * segment
            else -> (6f * segment + ((ftpPercent - 150f) / 50f) * segment).coerceAtMost(1.0f)
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
