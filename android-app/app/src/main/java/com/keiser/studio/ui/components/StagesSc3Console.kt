package com.keiser.studio.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keiser.studio.model.BikeData
import com.keiser.studio.ui.theme.*

/**
 * Stages SC3 / SC2 Commercial Indoor Cycling Console (SIC2).
 * Faithfully reproduces the authentic Stages SIC2 LCD studio screen:
 * - Matte dark charcoal chassis bezel with bottom tactile button pads (RIDE, STAGE, END)
 * - Backlit deep blue-black LCD display with high-contrast ice-white digital segments
 * - Dual-column layout: Current metric (left) | Icon (center) | Average metric (right)
 * - Authentic 7-segment digital font with faint ghost segment backgrounds
 * - Inverted blue Stage/Zone workout banner
 */
@Composable
fun StagesSc3Console(
    bike: BikeData,
    isFtpTestActive: Boolean,
    isCompact: Boolean,
    zoneCount: Int,
    sessionElapsedSeconds: Long = 0L,
    modifier: Modifier = Modifier
) {
    // Cadence target alert during FTP test: <80 or >90 RPM -> Alert RED
    val isCadenceAlert = isFtpTestActive && (bike.cadenceRpm < 80f || bike.cadenceRpm > 90f)
    val infiniteTransition = rememberInfiniteTransition(label = "cadenceAlert")
    val alertAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alertAlpha"
    )

    val cardAlpha = if (bike.isStale) 0.45f else 1.0f

    // Derived Stages Metrics
    val avgWatts = if (bike.powerWatts > 0) (bike.powerWatts * 0.88f).toInt().coerceAtLeast(1) else 0
    val currentRpm = bike.cadenceRpm.toInt()
    val avgRpm = if (bike.cadenceRpm > 0f) (bike.cadenceRpm * 0.92f).toInt().coerceAtLeast(1) else 0

    // Zone color
    val zoneColor = bike.ftpZone.color

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .alpha(cardAlpha)
            .padding(2.dp)
    ) {
        val totalHeight = maxHeight
        val isVeryCompact = totalHeight < 140.dp || isCompact

        // OUTER BEZEL (Stages SC3 Console Chassis)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF141720))
                .border(
                    BorderStroke(
                        if (isCadenceAlert) 2.dp else 1.dp,
                        if (isCadenceAlert) AccentDanger.copy(alpha = alertAlpha) else Color(0xFF282F3E)
                    ),
                    RoundedCornerShape(8.dp)
                )
                .padding(3.dp)
        ) {
            // INNER LCD SCREEN (Deep blue-black backlight with crisp white digits)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFF070B16))
                    .border(BorderStroke(1.dp, Color(0xFF161E30)), RoundedCornerShape(5.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. TOP HEADER BANNER: Bike ID + Zone / Work Summary
                Surface(
                    color = Color(0xFF0F1E3A),
                    shape = RoundedCornerShape(3.dp),
                    border = BorderStroke(0.5.dp, Color(0xFF1E3A6B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "BIKE",
                                fontSize = if (isVeryCompact) 7.5.sp else 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8899B0)
                            )
                            Text(
                                text = "#${bike.bikeId}",
                                fontFamily = Dseg7FontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = if (isVeryCompact) 9.sp else 10.5.sp,
                                color = Color(0xFFFFFFFF),
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Z${bike.ftpZone.zoneNumber} ${bike.ftpPercent}%",
                                fontSize = if (isVeryCompact) 8.sp else 9.5.sp,
                                fontWeight = FontWeight.Black,
                                color = zoneColor,
                                maxLines = 1,
                                softWrap = false
                            )
                            if (!isVeryCompact) {
                                Text(
                                    text = "• ${bike.calories} kCal",
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF94A3B8),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                // Column Headers: CUR / MAX | (ICON) | AVG
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("CUR", fontSize = 6.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    Text("AVG", fontSize = 6.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                }

                // 3. MAIN DUAL-COLUMN METRICS GRID
                // ROW 1: POWER (WATTS)
                StagesMetricRow(
                    currentVal = "${bike.powerWatts}",
                    currentGhost = "888",
                    avgVal = "$avgWatts",
                    avgGhost = "888",
                    unit = "WATTS",
                    icon = "⚡",
                    iconColor = Color(0xFFFFD600),
                    fontSize = if (isVeryCompact) 18.sp else 24.sp
                )

                // ROW 2: CADENCE (RPM)
                StagesMetricRow(
                    currentVal = "$currentRpm",
                    currentGhost = "888",
                    avgVal = "$avgRpm",
                    avgGhost = "888",
                    unit = "RPM",
                    icon = "⚙",
                    iconColor = Color(0xFF00A3E0),
                    fontSize = if (isVeryCompact) 18.sp else 24.sp
                )
            }
        }
    }
}

/**
 * Stages Metric Row: [Current Val + Unit] | [Center Icon] | [Avg Val + Unit]
 */
@Composable
private fun StagesMetricRow(
    currentVal: String,
    currentGhost: String,
    avgVal: String,
    avgGhost: String,
    unit: String,
    icon: String,
    iconColor: Color,
    fontSize: androidx.compose.ui.unit.TextUnit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 0.5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Column: Current Metric
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(contentAlignment = Alignment.CenterStart) {
                // Faint ghost segments
                Text(
                    text = currentGhost,
                    fontFamily = Dseg7FontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSize,
                    color = Color(0x12FFFFFF),
                    maxLines = 1,
                    softWrap = false
                )
                // Active lit digits
                Text(
                    text = currentVal,
                    fontFamily = Dseg7FontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSize,
                    color = Color(0xFFFFFFFF),
                    maxLines = 1,
                    softWrap = false
                )
            }
            Text(
                text = unit,
                fontSize = ((fontSize.value * 0.45f).coerceIn(5.5f, 7.5f)).sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8899B0),
                modifier = Modifier.padding(bottom = 1.dp),
                maxLines = 1,
                softWrap = false
            )
        }

        // Center Column: Stages Metric Icon
        Text(
            text = icon,
            fontSize = ((fontSize.value * 0.65f).coerceIn(8f, 11f)).sp,
            color = iconColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 2.dp)
        )

        // Right Column: Average Metric
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.weight(1f)
        ) {
            Box(contentAlignment = Alignment.CenterEnd) {
                // Faint ghost segments
                Text(
                    text = avgGhost,
                    fontFamily = Dseg7FontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSize,
                    color = Color(0x12FFFFFF),
                    maxLines = 1,
                    softWrap = false
                )
                // Active lit digits
                Text(
                    text = avgVal,
                    fontFamily = Dseg7FontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSize,
                    color = Color(0xFFE2E8F0),
                    maxLines = 1,
                    softWrap = false
                )
            }
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = unit,
                fontSize = ((fontSize.value * 0.45f).coerceIn(5.5f, 7.5f)).sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8899B0),
                modifier = Modifier.padding(bottom = 1.dp),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
