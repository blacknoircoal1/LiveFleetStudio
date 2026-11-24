package com.keiser.studio.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keiser.studio.ui.theme.Dseg7FontFamily
import kotlin.math.cos
import kotlin.math.sin

// Authentic Keiser M3i Zone Colors
val KeiserZone1 = Color(0xFF6B7280) // Zone 1: Recovery (Gray)
val KeiserZone2 = Color(0xFF00A3E0) // Zone 2: Endurance (Cyan/Blue)
val KeiserZone3 = Color(0xFF00C853) // Zone 3: Tempo (Vibrant Green)
val KeiserZone4 = Color(0xFFFF9100) // Zone 4: Threshold (Amber/Orange)
val KeiserZone5 = Color(0xFFD50000) // Zone 5: VO2 Max (Red)

@Composable
fun KeiserZoneGauge(
    bikeId: Int,
    ftpPercent: Int,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    // Determine Zone Number (1-5)
    val zoneNumber = when {
        ftpPercent <= 55 -> 1
        ftpPercent <= 75 -> 2
        ftpPercent <= 90 -> 3
        ftpPercent <= 105 -> 4
        else -> 5
    }

    // Compute progress fraction across the 5 zones (0.0f .. 1.0f)
    val progressFraction = calculateProgress(ftpPercent)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF060911))
            .border(1.dp, Color(0xFF1E283E), RoundedCornerShape(8.dp))
            .padding(top = if (isCompact) 3.dp else 5.dp, start = 6.dp, end = 6.dp, bottom = 3.dp)
    ) {
        // Top-Left: Bike ID
        Row(
            modifier = Modifier.align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#",
                fontSize = if (isCompact) 9.sp else 11.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF94A3B8)
            )
            Text(
                text = "$bikeId",
                fontFamily = Dseg7FontFamily,
                fontSize = if (isCompact) 11.sp else 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8)
            )
        }

        // Top-Right: Zone label and Digital Zone Number
        Column(
            modifier = Modifier.align(Alignment.TopEnd),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "ZONE",
                fontSize = if (isCompact) 7.sp else 8.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF94A3B8),
                letterSpacing = 0.5.sp
            )
            Text(
                text = "$zoneNumber",
                fontFamily = Dseg7FontFamily,
                fontSize = if (isCompact) 14.sp else 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Canvas: Tachometer colored arc and outer white progressive ladder ticks
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = if (isCompact) 4.dp else 8.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val centerX = canvasWidth / 2f
            val centerY = canvasHeight * 0.86f

            val arcRadius = (canvasWidth * 0.40f).coerceAtMost(canvasHeight * 0.70f)
            val arcStrokeWidth = if (isCompact) 9.dp.toPx() else 12.dp.toPx()
            val arcRect = androidx.compose.ui.geometry.Rect(
                left = centerX - arcRadius,
                top = centerY - arcRadius,
                right = centerX + arcRadius,
                bottom = centerY + arcRadius
            )

            // 1. Draw 5 Colored Zone Segments (180° to 360°, each 36°)
            val zones = listOf(
                Pair(180f, KeiserZone1),
                Pair(216f, KeiserZone2),
                Pair(252f, KeiserZone3),
                Pair(288f, KeiserZone4),
                Pair(324f, KeiserZone5)
            )

            zones.forEachIndexed { index, (startAngle, color) ->
                val zoneStartFrac = index * 0.2f
                val zoneColor = if (progressFraction >= zoneStartFrac) {
                    color
                } else {
                    color.copy(alpha = 0.25f)
                }

                drawArc(
                    color = zoneColor,
                    startAngle = startAngle,
                    sweepAngle = 36f,
                    useCenter = false,
                    topLeft = arcRect.topLeft,
                    size = arcRect.size,
                    style = Stroke(width = arcStrokeWidth, cap = StrokeCap.Butt)
                )
            }

            // 2. Draw Outer Segmented White Ladder Ticks
            val totalTicks = 38
            val tickInnerRadius = arcRadius + (arcStrokeWidth / 2f) + (if (isCompact) 2.dp.toPx() else 3.dp.toPx())
            val tickOuterRadius = tickInnerRadius + (if (isCompact) 4.dp.toPx() else 5.5.dp.toPx())

            for (i in 0 until totalTicks) {
                val frac = i.toFloat() / (totalTicks - 1)
                val angleDeg = 180f + (frac * 180f)
                val angleRad = Math.toRadians(angleDeg.toDouble())

                val startX = centerX + (tickInnerRadius * cos(angleRad)).toFloat()
                val startY = centerY + (tickInnerRadius * sin(angleRad)).toFloat()
                val endX = centerX + (tickOuterRadius * cos(angleRad)).toFloat()
                val endY = centerY + (tickOuterRadius * sin(angleRad)).toFloat()

                val isFilled = frac <= progressFraction
                val tickColor = if (isFilled) Color.White else Color(0x22FFFFFF)
                val tickWidth = if (isFilled) (if (isCompact) 1.5.dp.toPx() else 2.dp.toPx()) else 1.dp.toPx()

                drawLine(
                    color = tickColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = tickWidth,
                    cap = StrokeCap.Round
                )
            }

            // 3. Inner Dark Well / Center Cutout
            val innerWellRadius = arcRadius - (arcStrokeWidth / 2f) - 1.dp.toPx()
            drawCircle(
                color = Color(0xFF04060A),
                radius = innerWellRadius,
                center = Offset(centerX, centerY)
            )
        }

        // Center: 7-Segment FTP% Digital Display comfortably nested inside dark well
        val isThreeDigits = ftpPercent >= 100
        val fontSize = if (isCompact) {
            if (isThreeDigits) 15.sp else 18.sp
        } else {
            if (isThreeDigits) 19.sp else 23.sp
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = if (isCompact) 10.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$ftpPercent",
                fontFamily = Dseg7FontFamily,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                lineHeight = fontSize,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = "FTP%",
                fontSize = if (isCompact) 6.sp else 7.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF94A3B8),
                letterSpacing = 0.5.sp,
                lineHeight = if (isCompact) 7.sp else 8.sp,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * Maps FTP% into 0.0f..1.0f progress across the 5 Keiser zones.
 */
private fun calculateProgress(ftpPercent: Int): Float {
    return when {
        ftpPercent <= 0 -> 0.02f
        ftpPercent <= 55 -> (ftpPercent / 55f) * 0.2f
        ftpPercent <= 75 -> 0.2f + ((ftpPercent - 55f) / 20f) * 0.2f
        ftpPercent <= 90 -> 0.4f + ((ftpPercent - 75f) / 15f) * 0.2f
        ftpPercent <= 105 -> 0.6f + ((ftpPercent - 90f) / 15f) * 0.2f
        else -> (0.8f + ((ftpPercent - 105f) / 25f) * 0.2f).coerceAtMost(1.0f)
    }
}
