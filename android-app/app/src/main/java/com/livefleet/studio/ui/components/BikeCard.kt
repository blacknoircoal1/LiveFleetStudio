package com.livefleet.studio.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livefleet.studio.model.BikeData
import com.livefleet.studio.ui.theme.*

@Composable
fun BikeCard(
    bike: BikeData,
    isFtpTestActive: Boolean,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    // Cadence target enforcement during FTP test: <80 or >90 RPM -> Alert RED
    val isCadenceAlert = isFtpTestActive && (bike.cadenceRpm < 80f || bike.cadenceRpm > 90f)

    // Animated cadence alert border/background if alert
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

    val cardAlpha = if (bike.isStale) 0.45f else 1.0f

    Card(
        modifier = modifier
            .fillMaxSize()
            .alpha(cardAlpha)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = if (isCompact) 4.dp else 6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Bike Number as Primary ID
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "BIKE",
                        fontSize = if (isCompact) 9.sp else 11.sp,
                        fontWeight = FontWeight.Black,
                        color = AccentEnergy,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "#${bike.bikeId}",
                        fontFamily = Dseg7FontFamily,
                        fontSize = if (isCompact) 13.sp else 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMain
                    )
                }

                if (bike.isStale) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF7F1D1D), RoundedCornerShape(3.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "STALE",
                            fontSize = 6.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFCA5A5)
                        )
                    }
                }
            }

            // Authentic Keiser M3i Zone Arc Tachometer Gauge (flexibly expands!)
            KeiserZoneGauge(
                bikeId = bike.bikeId,
                ftpPercent = bike.ftpPercent,
                isCompact = isCompact,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 2.dp)
            )

            // Primary Metrics Row: RPM (Cadence) & Watts (Power) in 7-Segment Digital Font
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Cadence (RPM) with 80-90 RPM enforcement
                val cadenceBg = if (isCadenceAlert) AccentDanger.copy(alpha = alertAlpha) else Color(0xFF000000)
                val cadenceBorder = if (isCadenceAlert) AccentDanger else BorderColor
                val cadenceColor = if (isCadenceAlert) Color.White else Color(0xFFF1F5F9)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(cadenceBg, RoundedCornerShape(4.dp))
                        .border(1.dp, cadenceBorder, RoundedCornerShape(4.dp))
                        .padding(horizontal = 3.dp, vertical = if (isCompact) 2.dp else 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isCadenceAlert) "RPM ⚠️" else "RPM",
                            fontSize = if (isCompact) 6.5.sp else 7.5.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isCadenceAlert) Color.White else TextDim,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "${bike.cadenceRpm.toInt()}",
                            fontFamily = Dseg7FontFamily,
                            fontSize = if (isCompact) 15.sp else 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = cadenceColor,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Power (Watts)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF000000), RoundedCornerShape(4.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 3.dp, vertical = if (isCompact) 2.dp else 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "WATTS",
                            fontSize = if (isCompact) 6.5.sp else 7.5.sp,
                            fontWeight = FontWeight.Black,
                            color = TextDim,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "${bike.powerWatts}",
                            fontFamily = Dseg7FontFamily,
                            fontSize = if (isCompact) 15.sp else 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}
