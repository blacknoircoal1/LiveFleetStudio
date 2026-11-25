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

// Authentic Wattbike Air-Pro "Power Focus" Console Palette
private val WattbikeBezelDark = Color(0xFF121418)
private val WattbikeBezelRim = Color(0xFF2B303A)
private val WattbikeRed = Color(0xFFE11D48)           // Signature Wattbike Red accent
private val WattbikeScreenTop = Color(0xFF1F3E6E)     // Iconic Power Focus Royal-Steel Blue
private val WattbikeScreenBottom = Color(0xFF102240)
private val WattbikeScreenBorder = Color(0xFF2D538A)
private val WattbikeHeaderBg = Color(0xFF0C182E)
private val WattbikeRule = Color(0xFF2F5488)
private val WattbikeInkMuted = Color(0xFFA8C0DE)

/**
 * Wattbike Air-Pro / Atom "Power Focus" Studio Console.
 * Styled directly after the Wattbike Air-Pro touchscreen display:
 * - Matte charcoal bezel with Wattbike Red accent pill & pulsing FTP cadence alert rim
 * - Deep Royal-Steel Blue "Power Focus" display panel
 * - Header bar (AIR-PRO <last-5-digits> | Z# %FTP) + thin zone accent strip
 * - Giant high-contrast white WATTS hero scaled proportionally for big screens
 * - Clean CADENCE | KCAL footer (switches to single-row inline readout in ultra-dense grids)
 */
@Composable
fun WattbikeAirProConsole(
    bike: BikeData,
    isFtpTestActive: Boolean,
    isCompact: Boolean,
    zoneCount: Int,
    sessionElapsedSeconds: Long = 0L,
    modifier: Modifier = Modifier
) {
    val isCadenceAlert = isFtpTestActive && (bike.cadenceRpm < 80f || bike.cadenceRpm > 90f)
    val infiniteTransition = rememberInfiniteTransition(label = "wattbikeCadenceAlert")
    val alertAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wattbikeAlertAlpha"
    )

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
        val isVeryCompact = isUltraDense || totalHeight < 138.dp || totalWidth < 135.dp || isCompact
        val showFtpCell = !isUltraDense && totalWidth >= 112.dp
        val showBezelFooter = !isVeryCompact && totalHeight >= 158.dp

        val wattsText = "${bike.powerWatts}"
        val wattsFontSize: TextUnit = (
            (minDim * 0.27f).coerceIn(if (isUltraDense) 12f else 20f, 96f) *
                if (wattsText.length >= 4) 0.78f else 1f
            ).sp
        val wattsUnitFontSize: TextUnit = (minDim * 0.068f).coerceIn(if (isUltraDense) 6.5f else 7.5f, 24f).sp
        val footerFontSize: TextUnit = (minDim * 0.135f).coerceIn(if (isUltraDense) 9.5f else 12f, 48f).sp
        val footerLabelFontSize: TextUnit = (minDim * 0.052f).coerceIn(if (isUltraDense) 6f else 6.5f, 18f).sp
        val headerFontSize: TextUnit = (minDim * 0.072f).coerceIn(if (isUltraDense) 7.5f else 8.5f, 24f).sp

        // OUTER WATTBIKE BEZEL
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(WattbikeBezelDark)
                .border(
                    BorderStroke(
                        if (isCadenceAlert) 2.dp else 1.dp,
                        if (isCadenceAlert) AccentDanger.copy(alpha = alertAlpha) else WattbikeBezelRim
                    ),
                    RoundedCornerShape(8.dp)
                )
                .padding(
                    start = if (isVeryCompact) 3.dp else 5.dp,
                    top = if (isVeryCompact) 3.dp else 5.dp,
                    end = if (isVeryCompact) 3.dp else 5.dp,
                    bottom = if (showBezelFooter) 2.dp else if (isVeryCompact) 3.dp else 5.dp
                )
        ) {
            // INNER POWER FOCUS ROYAL-BLUE SCREEN
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Brush.verticalGradient(listOf(WattbikeScreenTop, WattbikeScreenBottom)))
                    .border(BorderStroke(1.dp, WattbikeScreenBorder), RoundedCornerShape(5.dp))
                    .padding(
                        horizontal = if (isVeryCompact) 5.dp else 7.dp,
                        vertical = if (isVeryCompact) 3.dp else 5.dp
                    ),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. HEADER BAR + ZONE ACCENT STRIP
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Surface(
                        color = WattbikeHeaderBg,
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
                                        text = "AIR-PRO",
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = (headerFontSize.value * 0.78f).coerceIn(7f, 15f).sp,
                                        fontWeight = FontWeight.Black,
                                        color = WattbikeInkMuted,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                Text(
                                    text = idLabel,
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

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isVeryCompact) 2.dp else 2.5.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(zoneColor)
                    )
                }

                // 2. POWER FOCUS HERO / ULTRA-DENSE INLINE ROW
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
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Black,
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
                                color = WattbikeInkMuted,
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
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "RPM",
                                fontFamily = FontFamily.Monospace,
                                fontSize = denseUnitSize,
                                fontWeight = FontWeight.Bold,
                                color = if (isCadenceAlert) Color.White else WattbikeInkMuted,
                                modifier = Modifier.padding(start = 1.dp, bottom = 1.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                } else {
                    // Big Clean Power Focus Readout
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (!isVeryCompact) {
                            Text(
                                text = "POWER FOCUS",
                                fontSize = (minDim * 0.044f).coerceIn(6.5f, 12f).sp,
                                fontWeight = FontWeight.Bold,
                                color = WattbikeInkMuted,
                                letterSpacing = 0.8.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "${bike.powerWatts}",
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
                                color = WattbikeInkMuted,
                                modifier = Modifier.padding(bottom = if (isVeryCompact) 2.dp else 5.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Hairline divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(WattbikeRule)
                    )

                    // Footer: CADENCE | %FTP
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = if (isVeryCompact) 2.dp else 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WattbikeFooterCell(
                            value = "${bike.cadenceRpm.toInt()}",
                            label = "CADENCE",
                            fontSize = footerFontSize,
                            labelFontSize = footerLabelFontSize,
                            valueColor = Color.White,
                            isAlert = isCadenceAlert,
                            alertAlpha = alertAlpha,
                            modifier = Modifier.weight(1f)
                        )

                        if (showFtpCell) {
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height((minDim * 0.14f).coerceIn(16f, 44f).dp)
                                    .background(WattbikeRule)
                            )

                            WattbikeFooterCell(
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

            // SUBTLE WATTBIKE BEZEL FOOTER (spacious tiles only)
            if (showBezelFooter) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 3.dp, start = 4.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "wattbike",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 0.6.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                    Surface(
                        color = WattbikeRed,
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Text(
                            text = "POWER",
                            fontSize = 6.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
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
private fun WattbikeFooterCell(
    value: String,
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
            color = if (isAlert) Color.White else WattbikeInkMuted,
            letterSpacing = 0.3.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}
