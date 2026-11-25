package com.keiser.studio.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
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
import kotlin.math.ceil

// Pro GPS Bike Computer / Power Meter Head Unit Palette (Wahoo ELEMNT / Garmin Edge inspired)
private val HeadUnitBezel = Color(0xFF101217)
private val HeadUnitRim = Color(0xFF282E3A)
private val HeadUnitScreenTop = Color(0xFF141822)
private val HeadUnitScreenBottom = Color(0xFF0A0C12)
private val HeadUnitScreenBorder = Color(0xFF232B3B)
private val HeadUnitHeaderBg = Color(0xFF19202E)
private val HeadUnitRule = Color(0xFF242D3F)
private val HeadUnitLedOff = Color(0xFF222836)
private val HeadUnitInkMuted = Color(0xFF94A3B8)

/**
 * Pro GPS Cycling Head Unit Console for Standard Power Pedals & Cranks
 * (Favero Assioma, Garmin Rally, Quarq, SRM, PowerTap) and generic FTMS smart bikes
 * (Life Fitness IC5–8, Technogym, Matrix, Body Bike).
 *
 * Features:
 * - Matte stealth-obsidian bezel with 7 Wahoo-style glowing LED effort dots along the top rim
 * - Clean modern bold sans-serif typography (distinct from retro 7-segment indoor consoles)
 * - Header bar (Bolt + Brand/PWR + #ID | Z#)
 * - Large centered WATTS hero (4-digit safe, proportional big-screen scaling)
 * - Clean RPM | % FTP footer (switches to single-row inline layout in ultra-dense grids)
 */
@Composable
fun PowerMeterHeadUnitConsole(
    bike: BikeData,
    isFtpTestActive: Boolean,
    isCompact: Boolean,
    zoneCount: Int,
    sessionElapsedSeconds: Long = 0L,
    modifier: Modifier = Modifier
) {
    val isCadenceAlert = isFtpTestActive && (bike.cadenceRpm < 80f || bike.cadenceRpm > 90f)
    val infiniteTransition = rememberInfiniteTransition(label = "pwrCadenceAlert")
    val alertAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pwrAlertAlpha"
    )

    val cardAlpha = if (bike.isStale) 0.45f else 1.0f
    val zoneColor = bike.ftpZone.color
    val idLabel = bike.displayId.ifEmpty { "#${bike.bikeId}" }

    val brandTag = when (bike.brand.lowercase()) {
        "lifefitness" -> "ICG"
        "technogym" -> "TECHNO"
        "matrix" -> "MATRIX"
        "bodybike" -> "BODY"
        "ftms" -> "FTMS"
        else -> "PWR"
    }

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
        val isVeryCompact = isUltraDense || totalHeight < 138.dp || totalWidth < 135.dp || isCompact
        val showFtpCell = !isUltraDense && totalWidth >= 112.dp

        val wattsText = "${bike.powerWatts}"
        val wattsFontSize: TextUnit = (
            (minDim * 0.27f).coerceIn(if (isUltraDense) 12f else 20f, 96f) *
                if (wattsText.length >= 4) 0.78f else 1f
            ).sp
        val wattsUnitFontSize: TextUnit = (minDim * 0.068f).coerceIn(if (isUltraDense) 6.5f else 7.5f, 24f).sp
        val footerFontSize: TextUnit = (minDim * 0.14f).coerceIn(if (isUltraDense) 9.5f else 12.5f, 48f).sp
        val footerLabelFontSize: TextUnit = (minDim * 0.052f).coerceIn(if (isUltraDense) 6f else 6.5f, 18f).sp
        val headerFontSize: TextUnit = (minDim * 0.072f).coerceIn(if (isUltraDense) 7.5f else 8.5f, 24f).sp

        // OUTER HEAD UNIT BEZEL
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(10.dp))
                .background(HeadUnitBezel)
                .border(
                    BorderStroke(
                        if (isCadenceAlert) 2.dp else 1.dp,
                        if (isCadenceAlert) AccentDanger.copy(alpha = alertAlpha) else HeadUnitRim
                    ),
                    RoundedCornerShape(10.dp)
                )
                .padding(
                    horizontal = if (isVeryCompact) 3.dp else 5.dp,
                    vertical = if (isVeryCompact) 3.dp else 4.dp
                ),
            verticalArrangement = Arrangement.spacedBy(if (isVeryCompact) 2.5.dp else 4.dp)
        ) {
            // 1. WAHOO-STYLE TOP LED EFFORT DOTS (7 glowing zone LEDs on top bezel)
            HeadUnitLedStrip(
                ftpPercent = bike.ftpPercent,
                zoneCount = zoneCount,
                isCompact = isVeryCompact
            )

            // 2. INNER HIGH-CONTRAST HEAD UNIT DISPLAY
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Brush.verticalGradient(listOf(HeadUnitScreenTop, HeadUnitScreenBottom)))
                    .border(BorderStroke(1.dp, HeadUnitScreenBorder), RoundedCornerShape(6.dp))
                    .padding(
                        horizontal = if (isVeryCompact) 5.dp else 7.dp,
                        vertical = if (isVeryCompact) 3.dp else 5.dp
                    ),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header Row: Bolt + Tag + ID | Zone Pill
                Surface(
                    color = HeadUnitHeaderBg,
                    shape = RoundedCornerShape(4.dp),
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
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = AccentEnergy,
                                modifier = Modifier.size(if (isVeryCompact) 10.dp else 13.dp)
                            )
                            if (!isVeryCompact) {
                                Text(
                                    text = brandTag,
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = (headerFontSize.value * 0.78f).coerceIn(7f, 15f).sp,
                                    fontWeight = FontWeight.Black,
                                    color = HeadUnitInkMuted,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Text(
                                text = idLabel,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Black,
                                fontSize = headerFontSize,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Surface(
                            color = if (bike.isStale) Color(0x33EF4444) else zoneColor.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(3.dp)
                        ) {
                            Text(
                                text = when {
                                    bike.isStale -> "IDLE"
                                    showFtpCell -> "Z${bike.ftpZone.zoneNumber}"
                                    else -> "Z${bike.ftpZone.zoneNumber} ${bike.ftpPercent}%"
                                },
                                fontSize = (headerFontSize.value * 0.9f).coerceIn(7.5f, 20f).sp,
                                fontWeight = FontWeight.Black,
                                color = if (bike.isStale) Color(0xFFFCA5A5) else zoneColor,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.5.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // Metrics Area: Ultra-dense single line OR Hero WATTS + Footer RPM | % FTP
                if (isUltraDense) {
                    val denseWattsSize = (totalHeight.value * 0.22f).coerceIn(11f, 17f).sp
                    val denseRpmSize = (totalHeight.value * 0.17f).coerceIn(9.5f, 14f).sp
                    val denseUnitSize = (totalHeight.value * 0.10f).coerceIn(6f, 8.5f).sp
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = wattsText,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Black,
                                fontSize = denseWattsSize,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "W",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = denseUnitSize,
                                fontWeight = FontWeight.Black,
                                color = AccentEnergy,
                                modifier = Modifier.padding(start = 1.dp, bottom = 1.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Row(
                            modifier = if (isCadenceAlert) {
                                Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(AccentDanger.copy(alpha = alertAlpha))
                                    .padding(horizontal = 3.dp, vertical = 1.dp)
                            } else {
                                Modifier
                            },
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "${bike.cadenceRpm.toInt()}",
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = denseRpmSize,
                                color = if (isCadenceAlert) Color.White else Color(0xFFE2E8F0),
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "RPM",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = denseUnitSize,
                                fontWeight = FontWeight.Bold,
                                color = if (isCadenceAlert) Color.White else HeadUnitInkMuted,
                                modifier = Modifier.padding(start = 1.dp, bottom = 1.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                } else {
                    // Hero WATTS
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
                            Text(
                                text = wattsText,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Black,
                                fontSize = wattsFontSize,
                                color = Color.White,
                                lineHeight = wattsFontSize,
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "W",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = wattsUnitFontSize,
                                fontWeight = FontWeight.Black,
                                color = AccentEnergy,
                                modifier = Modifier.padding(bottom = if (isVeryCompact) 2.dp else 5.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(HeadUnitRule)
                    )

                    // Footer: RPM | % FTP
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = if (isVeryCompact) 2.dp else 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HeadUnitFooterCell(
                            value = "${bike.cadenceRpm.toInt()}",
                            label = "RPM",
                            fontSize = footerFontSize,
                            labelFontSize = footerLabelFontSize,
                            valueColor = Color(0xFFE2E8F0),
                            isAlert = isCadenceAlert,
                            alertAlpha = alertAlpha,
                            modifier = Modifier.weight(1f)
                        )

                        if (showFtpCell) {
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height((minDim * 0.14f).coerceIn(16f, 44f).dp)
                                    .background(HeadUnitRule)
                            )

                            HeadUnitFooterCell(
                                value = "${bike.ftpPercent}",
                                label = "% FTP",
                                fontSize = footerFontSize,
                                labelFontSize = footerLabelFontSize,
                                valueColor = zoneColor,
                                isAlert = false,
                                alertAlpha = 1f,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeadUnitLedStrip(
    ftpPercent: Int,
    zoneCount: Int,
    isCompact: Boolean
) {
    val totalDots = 7
    val maxScale = if (zoneCount == 7) 160f else 125f
    val litCount = if (ftpPercent <= 0) {
        0
    } else {
        ceil((ftpPercent.coerceAtMost(maxScale.toInt()) / maxScale) * totalDots)
            .toInt()
            .coerceIn(1, totalDots)
    }

    val ledColors = if (zoneCount == 7) {
        listOf(
            Color(0xFF6B7280),
            Color(0xFF00A3E0),
            Color(0xFF00C853),
            Color(0xFFFFD600),
            Color(0xFFFF9100),
            Color(0xFFD50000),
            Color(0xFFAA00FF)
        )
    } else {
        listOf(
            KeiserZone1,
            KeiserZone2,
            KeiserZone2,
            KeiserZone3,
            KeiserZone4,
            KeiserZone4,
            KeiserZone5
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (isCompact) 4.dp else 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until totalDots) {
            val isLit = i < litCount
            Box(
                modifier = Modifier
                    .size(if (isCompact) 4.dp else 5.5.dp)
                    .clip(CircleShape)
                    .background(if (isLit) ledColors[i] else HeadUnitLedOff)
            )
        }
    }
}

@Composable
private fun HeadUnitFooterCell(
    value: String,
    label: String,
    fontSize: TextUnit,
    labelFontSize: TextUnit,
    valueColor: Color,
    isAlert: Boolean,
    alertAlpha: Float,
    modifier: Modifier = Modifier
) {
    val effectiveFontSize = if (value.length >= 4) (fontSize.value * 0.78f).sp else fontSize
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
        Text(
            text = value,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Black,
            fontSize = effectiveFontSize,
            color = if (isAlert) Color.White else valueColor,
            maxLines = 1,
            softWrap = false
        )
        Text(
            text = if (isAlert) "$label !" else label,
            fontFamily = FontFamily.SansSerif,
            fontSize = labelFontSize,
            fontWeight = FontWeight.Bold,
            color = if (isAlert) Color.White else HeadUnitInkMuted,
            letterSpacing = 0.3.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}
