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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keiser.studio.model.BikeData
import com.keiser.studio.ui.theme.*

// Authentic Stages SC3 / SIC2 Console Palette
private val StagesBezelDark = Color(0xFF141821)
private val StagesBezelRim = Color(0xFF283142)
private val StagesLcdTop = Color(0xFF0A1428)
private val StagesLcdBottom = Color(0xFF050B16)
private val StagesLcdBorder = Color(0xFF1A2D4F)
private val StagesHeaderBg = Color(0xFF0F2040)
private val StagesRule = Color(0xFF192E52)
private val StagesCyan = Color(0xFF38BDF8)
private val StagesInkMuted = Color(0xFF8EA4C2)
private val StagesGhost = Color(0x14FFFFFF)

/**
 * Stages SC3 / SC2 Commercial Indoor Cycling Console (SIC2).
 * Clean, studio-readable SIC2 layout:
 * - Matte charcoal SC3 chassis bezel with pulsing FTP cadence alert rim
 * - Deep midnight-blue backlit LCD screen with crisp ice-white 7-segment digits
 * - Compact top header (#Bike | Z# %FTP) + thin zone color bar for instant studio readability
 * - Single Hero Power readout (WATTS) centered for distance legibility
 * - Clean hairline-divided footer row for RPM (with FTP cadence alert) and kCal
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
    val infiniteTransition = rememberInfiniteTransition(label = "stagesCadenceAlert")
    val alertAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "stagesAlertAlpha"
    )

    val cardAlpha = if (bike.isStale) 0.45f else 1.0f
    val zoneColor = bike.ftpZone.color

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .alpha(cardAlpha)
            .padding(2.dp)
    ) {
        val totalHeight = maxHeight
        val totalWidth = maxWidth
        val isVeryCompact = totalHeight < 138.dp || totalWidth < 135.dp || isCompact
        val showKcalCell = totalWidth >= 112.dp
        val showBezelButtons = !isVeryCompact && totalHeight >= 158.dp

        val wattsFontSize: TextUnit = when {
            totalHeight < 108.dp || totalWidth < 110.dp -> 19.sp
            isVeryCompact -> 24.sp
            totalHeight >= 210.dp && totalWidth >= 230.dp -> 40.sp
            else -> 32.sp
        }
        val footerFontSize: TextUnit = when {
            totalHeight < 108.dp || totalWidth < 110.dp -> 11.5.sp
            isVeryCompact -> 13.5.sp
            totalHeight >= 210.dp && totalWidth >= 230.dp -> 20.sp
            else -> 16.sp
        }

        // OUTER BEZEL (Stages SC3 Console Chassis)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(StagesBezelDark)
                .border(
                    BorderStroke(
                        if (isCadenceAlert) 2.dp else 1.dp,
                        if (isCadenceAlert) AccentDanger.copy(alpha = alertAlpha) else StagesBezelRim
                    ),
                    RoundedCornerShape(8.dp)
                )
                .padding(
                    start = if (isVeryCompact) 3.dp else 5.dp,
                    top = if (isVeryCompact) 3.dp else 5.dp,
                    end = if (isVeryCompact) 3.dp else 5.dp,
                    bottom = if (showBezelButtons) 3.dp else if (isVeryCompact) 3.dp else 5.dp
                )
        ) {
            // INNER SIC2 BACKLIT LCD SCREEN
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Brush.verticalGradient(listOf(StagesLcdTop, StagesLcdBottom)))
                    .border(BorderStroke(1.dp, StagesLcdBorder), RoundedCornerShape(5.dp))
                    .padding(
                        horizontal = if (isVeryCompact) 5.dp else 7.dp,
                        vertical = if (isVeryCompact) 3.dp else 5.dp
                    ),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. TOP HEADER BAR + THIN ZONE ACCENT BAR
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Surface(
                        color = StagesHeaderBg,
                        shape = RoundedCornerShape(3.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = if (isVeryCompact) 4.dp else 6.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                if (!isVeryCompact) {
                                    Text(
                                        text = "SC3",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = StagesCyan,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                Text(
                                    text = "#${bike.bikeId}",
                                    fontFamily = Dseg7FontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = if (isVeryCompact) 9.sp else 10.5.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            Text(
                                text = if (bike.isStale) "IDLE" else "Z${bike.ftpZone.zoneNumber} ${bike.ftpPercent}%",
                                fontSize = if (isVeryCompact) 8.sp else 9.5.sp,
                                fontWeight = FontWeight.Black,
                                color = if (bike.isStale) Color(0xFFFCA5A5) else zoneColor,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Thin Zone Color Accent Strip
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isVeryCompact) 2.dp else 2.5.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(zoneColor)
                    )
                }

                // 2. HERO CENTER READOUT: POWER (WATTS)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(contentAlignment = Alignment.CenterEnd) {
                            Text(
                                text = "888",
                                fontFamily = Dseg7FontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = wattsFontSize,
                                color = StagesGhost,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "${bike.powerWatts}",
                                fontFamily = Dseg7FontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = wattsFontSize,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Spacer(modifier = Modifier.width(if (isVeryCompact) 3.dp else 4.dp))
                        Text(
                            text = if (isVeryCompact) "W" else "WATTS",
                            fontFamily = FontFamily.Monospace,
                            fontSize = if (isVeryCompact) 7.5.sp else 9.sp,
                            fontWeight = FontWeight.Black,
                            color = StagesCyan,
                            modifier = Modifier.padding(bottom = if (isVeryCompact) 2.dp else 4.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // 3. HAIRLINE LCD DIVIDER
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(StagesRule)
                )

                // 4. COMPACT FOOTER ROW: RPM | kCal
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = if (isVeryCompact) 2.dp else 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StagesFooterCell(
                        value = "${bike.cadenceRpm.toInt()}",
                        ghost = "888",
                        label = "RPM",
                        fontSize = footerFontSize,
                        isVeryCompact = isVeryCompact,
                        isAlert = isCadenceAlert,
                        alertAlpha = alertAlpha,
                        modifier = Modifier.weight(1f)
                    )

                    if (showKcalCell) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(if (isVeryCompact) 16.dp else 22.dp)
                                .background(StagesRule)
                        )

                        StagesFooterCell(
                            value = "${bike.calories}",
                            ghost = "888",
                            label = "KCAL",
                            fontSize = footerFontSize,
                            isVeryCompact = isVeryCompact,
                            isAlert = false,
                            alertAlpha = 1f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // SUBTLE TACTILE BEZEL BUTTON STRIP (only on spacious tiles)
            if (showBezelButtons) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 3.dp, start = 4.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("RIDE", "STAGE", "END").forEach { btnLabel ->
                        Text(
                            text = btnLabel,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 6.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF56657F),
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StagesFooterCell(
    value: String,
    ghost: String,
    label: String,
    fontSize: TextUnit,
    isVeryCompact: Boolean,
    isAlert: Boolean,
    alertAlpha: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .then(
                if (isAlert) {
                    Modifier
                        .padding(horizontal = 2.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(AccentDanger.copy(alpha = alertAlpha))
                        .padding(vertical = 1.dp)
                } else {
                    Modifier.padding(horizontal = 2.dp)
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (!isAlert) {
                Text(
                    text = ghost,
                    fontFamily = Dseg7FontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSize,
                    color = StagesGhost,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Text(
                text = value,
                fontFamily = Dseg7FontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                color = if (isAlert) Color.White else Color(0xFFE2E8F0),
                maxLines = 1,
                softWrap = false
            )
        }
        Text(
            text = if (isAlert) "$label !" else label,
            fontFamily = FontFamily.Monospace,
            fontSize = if (isVeryCompact) 6.sp else 7.sp,
            fontWeight = FontWeight.Bold,
            color = if (isAlert) Color.White else StagesInkMuted,
            letterSpacing = 0.3.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}
