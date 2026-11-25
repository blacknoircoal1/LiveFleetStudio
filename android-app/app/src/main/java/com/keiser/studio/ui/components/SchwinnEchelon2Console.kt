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
import kotlin.math.ceil

// Schwinn Carbon Blue / MPower Echelon2 + 4iiii Palette
private val SchwinnCarbonBezel = Color(0xFF14151A)
private val SchwinnBezelRim = Color(0xFF2A2E39)
private val SchwinnRed = Color(0xFFE11D48)          // Signature Schwinn Racing Red
private val SchwinnCarbonBlue = Color(0xFF0EA5E9)   // Carbon Blue belt/brand accent
private val SchwinnScreenTop = Color(0xFF0C1018)
private val SchwinnScreenBottom = Color(0xFF06080D)
private val SchwinnScreenBorder = Color(0xFF1B2434)
private val SchwinnHeaderBg = Color(0xFF131823)
private val SchwinnRule = Color(0xFF1E2636)
private val SchwinnSegmentOff = Color(0xFF1A202E)
private val SchwinnInkMuted = Color(0xFF8B99B0)
private val SchwinnGhost = Color(0x14FFFFFF)

/**
 * Schwinn Carbon Blue / MPower Echelon2 Dark Studio Console (4iiii Precision).
 * - Matte carbon-dark bezel with Schwinn Racing Red hood stripe & pulsing FTP cadence alert rim
 * - High-contrast dark console screen with Carbon Blue unit highlights
 * - Header bar (#Bike | Z# %FTP) + Echelon2-style 10-segment progressive power/zone bar
 * - Single Hero WATTS readout centered for studio-distance legibility
 * - Clean hairline-divided footer row for RPM (with FTP cadence alert) and KCAL
 */
@Composable
fun SchwinnEchelon2Console(
    bike: BikeData,
    isFtpTestActive: Boolean,
    isCompact: Boolean,
    zoneCount: Int,
    sessionElapsedSeconds: Long = 0L,
    modifier: Modifier = Modifier
) {
    // Cadence target alert during FTP test: <80 or >90 RPM -> Alert RED
    val isCadenceAlert = isFtpTestActive && (bike.cadenceRpm < 80f || bike.cadenceRpm > 90f)
    val infiniteTransition = rememberInfiniteTransition(label = "schwinnCadenceAlert")
    val alertAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "schwinnAlertAlpha"
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
        val minDim = minOf(totalWidth.value, totalHeight.value)
        val isUltraDense = totalHeight < 92.dp || totalWidth < 95.dp
        val isVeryCompact = isUltraDense || totalHeight < 138.dp || totalWidth < 135.dp || isCompact
        val showFtpCell = !isUltraDense && totalWidth >= 112.dp
        val showBezelFooter = !isVeryCompact && totalHeight >= 158.dp

        val wattsText = "${bike.powerWatts}"
        val wattsFontSize: TextUnit = (
            (minDim * 0.25f).coerceIn(if (isUltraDense) 12f else 19f, 92f) *
                if (wattsText.length >= 4) 0.78f else 1f
            ).sp
        val wattsUnitFontSize: TextUnit = (minDim * 0.068f).coerceIn(if (isUltraDense) 6.5f else 7.5f, 24f).sp
        val footerFontSize: TextUnit = (minDim * 0.135f).coerceIn(if (isUltraDense) 9.5f else 12f, 48f).sp
        val footerLabelFontSize: TextUnit = (minDim * 0.052f).coerceIn(if (isUltraDense) 6f else 6.5f, 18f).sp
        val headerFontSize: TextUnit = (minDim * 0.072f).coerceIn(if (isUltraDense) 7.5f else 8.5f, 24f).sp

        // OUTER CARBON BEZEL (Schwinn MPower Echelon2 Housing)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(SchwinnCarbonBezel)
                .border(
                    BorderStroke(
                        if (isCadenceAlert) 2.dp else 1.dp,
                        if (isCadenceAlert) AccentDanger.copy(alpha = alertAlpha) else SchwinnBezelRim
                    ),
                    RoundedCornerShape(8.dp)
                )
        ) {
            // SIGNATURE SCHWINN RACING RED TOP HOOD ACCENT
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isVeryCompact) 2.dp else 3.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(SchwinnRed, SchwinnRed.copy(alpha = 0.85f), SchwinnCarbonBlue)
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(
                        start = if (isVeryCompact) 3.dp else 5.dp,
                        top = if (isVeryCompact) 3.dp else 4.dp,
                        end = if (isVeryCompact) 3.dp else 5.dp,
                        bottom = if (showBezelFooter) 2.dp else if (isVeryCompact) 3.dp else 5.dp
                    )
            ) {
                // INNER ECHELON2 DARK STUDIO SCREEN
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Brush.verticalGradient(listOf(SchwinnScreenTop, SchwinnScreenBottom)))
                        .border(BorderStroke(1.dp, SchwinnScreenBorder), RoundedCornerShape(5.dp))
                        .padding(
                            horizontal = if (isVeryCompact) 5.dp else 7.dp,
                            vertical = if (isVeryCompact) 3.dp else 5.dp
                        ),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // 1. HEADER BAR + 10-SEGMENT ECHELON2 POWER/ZONE BAR
                    Column(verticalArrangement = Arrangement.spacedBy(if (isVeryCompact) 2.5.dp else 3.5.dp)) {
                        Surface(
                            color = SchwinnHeaderBg,
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
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Crisp Schwinn Red vertical indicator bar
                                    Box(
                                        modifier = Modifier
                                            .width(2.5.dp)
                                            .height(if (isVeryCompact) 9.dp else 11.dp)
                                            .clip(RoundedCornerShape(1.dp))
                                            .background(SchwinnRed)
                                    )
                                    Text(
                                        text = bike.displayId.ifEmpty { "#${bike.bikeId}" },
                                        fontFamily = Dseg7FontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = headerFontSize,
                                        color = Color.White,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }

                                Text(
                                    text = when {
                                        bike.isStale -> "IDLE"
                                        showFtpCell -> "Z${bike.ftpZone.zoneNumber}"
                                        else -> "Z${bike.ftpZone.zoneNumber} ${bike.ftpPercent}%"
                                    },
                                    fontSize = headerFontSize,
                                    fontWeight = FontWeight.Black,
                                    color = if (bike.isStale) Color(0xFFFCA5A5) else zoneColor,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        // Iconic Echelon2 Segmented Power / Zone Bar (10 LED-style segments)
                        Echelon2SegmentedBar(
                            ftpPercent = bike.ftpPercent,
                            zoneCount = zoneCount,
                            isCompact = isVeryCompact
                        )
                    }

                    // 2. HERO / METRICS AREA (Switches to crisp single-row inline readout in ultra-dense grids)
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
                                    text = "${bike.powerWatts}",
                                    fontFamily = Dseg7FontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = denseWattsSize,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "W",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = denseUnitSize,
                                    fontWeight = FontWeight.Black,
                                    color = SchwinnCarbonBlue,
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
                                    fontFamily = Dseg7FontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = denseRpmSize,
                                    color = if (isCadenceAlert) Color.White else Color(0xFFCBD5E1),
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "RPM",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = denseUnitSize,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCadenceAlert) Color.White else SchwinnInkMuted,
                                    modifier = Modifier.padding(start = 1.dp, bottom = 1.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    } else {
                        // HERO CENTER READOUT: POWER (WATTS)
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
                                        text = if (wattsText.length >= 4) "8888" else "888",
                                        fontFamily = Dseg7FontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = wattsFontSize,
                                        color = SchwinnGhost,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Text(
                                        text = wattsText,
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
                                    fontSize = wattsUnitFontSize,
                                    fontWeight = FontWeight.Black,
                                    color = SchwinnCarbonBlue,
                                    modifier = Modifier.padding(bottom = if (isVeryCompact) 2.dp else 4.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        // 3. HAIRLINE DIVIDER
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(SchwinnRule)
                        )

                        // 4. COMPACT FOOTER ROW: RPM | %FTP
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = if (isVeryCompact) 2.dp else 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SchwinnFooterCell(
                                value = "${bike.cadenceRpm.toInt()}",
                                ghost = "888",
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
                                        .background(SchwinnRule)
                                )

                                SchwinnFooterCell(
                                    value = "${bike.ftpPercent}",
                                    ghost = if (bike.ftpPercent >= 1000) "8888" else "888",
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

                // SUBTLE BEZEL BRANDING (spacious tiles only)
                if (showBezelFooter) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 3.dp, start = 2.dp, end = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SCHWINN",
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 6.5.sp,
                            fontWeight = FontWeight.Black,
                            color = SchwinnRed.copy(alpha = 0.85f),
                            letterSpacing = 0.8.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "ECHELON2 • 4iiii",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 6.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = SchwinnCarbonBlue.copy(alpha = 0.7f),
                            letterSpacing = 0.4.sp,
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
private fun Echelon2SegmentedBar(
    ftpPercent: Int,
    zoneCount: Int,
    isCompact: Boolean
) {
    val totalSegments = 10
    val maxScale = if (zoneCount == 7) 160f else 125f
    val litCount = if (ftpPercent <= 0) {
        0
    } else {
        ceil((ftpPercent.coerceAtMost(maxScale.toInt()) / maxScale) * totalSegments)
            .toInt()
            .coerceIn(1, totalSegments)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(if (isCompact) 1.5.dp else 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until totalSegments) {
            val isLit = i < litCount
            val segColor = if (isLit) {
                segmentZoneColor(i, totalSegments, zoneCount)
            } else {
                SchwinnSegmentOff
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(if (isCompact) 3.dp else 4.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(segColor)
            )
        }
    }
}

private fun segmentZoneColor(index: Int, total: Int, zoneCount: Int): Color {
    val frac = (index + 0.5f) / total.toFloat()
    return if (zoneCount == 7) {
        when {
            frac < 1f / 7f -> Color(0xFF6B7280) // Z1 Gray
            frac < 2f / 7f -> Color(0xFF00A3E0) // Z2 Blue
            frac < 3f / 7f -> Color(0xFF00C853) // Z3 Green
            frac < 4f / 7f -> Color(0xFFFFD600) // Z4 Yellow
            frac < 5f / 7f -> Color(0xFFFF9100) // Z5 Orange
            frac < 6f / 7f -> Color(0xFFD50000) // Z6 Red
            else -> Color(0xFFAA00FF)           // Z7 Purple
        }
    } else {
        when {
            frac < 0.2f -> KeiserZone1
            frac < 0.4f -> KeiserZone2
            frac < 0.6f -> KeiserZone3
            frac < 0.8f -> KeiserZone4
            else -> KeiserZone5
        }
    }
}

@Composable
private fun SchwinnFooterCell(
    value: String,
    ghost: String,
    label: String,
    fontSize: TextUnit,
    labelFontSize: TextUnit,
    valueColor: Color,
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
        val effectiveFontSize = if (value.length >= 4) (fontSize.value * 0.78f).sp else fontSize
        Box(contentAlignment = Alignment.CenterEnd) {
            if (!isAlert) {
                Text(
                    text = ghost,
                    fontFamily = Dseg7FontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = effectiveFontSize,
                    color = SchwinnGhost,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Text(
                text = value,
                fontFamily = Dseg7FontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = effectiveFontSize,
                color = if (isAlert) Color.White else valueColor,
                maxLines = 1,
                softWrap = false
            )
        }
        Text(
            text = if (isAlert) "$label !" else label,
            fontFamily = FontFamily.Monospace,
            fontSize = labelFontSize,
            fontWeight = FontWeight.Bold,
            color = if (isAlert) Color.White else SchwinnInkMuted,
            letterSpacing = 0.3.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}
