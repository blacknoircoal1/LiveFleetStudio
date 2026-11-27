package com.livefleet.studio.ui.components

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
import com.livefleet.studio.model.BikeData
import com.livefleet.studio.model.TileHeroMetric
import com.livefleet.studio.model.resolveCyclingTileMetrics
import com.livefleet.studio.ui.theme.*

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
 * - Compact top header (#Bike | Z#) + thin zone color bar for instant studio readability
 * - Single Hero Power readout (WATTS) centered for distance legibility
 * - Clean hairline-divided footer row for RPM (with FTP cadence alert) and %FTP (zone-colored)
 */
@Composable
fun StagesSc3Console(
    bike: BikeData,
    isFtpTestActive: Boolean,
    isCompact: Boolean,
    zoneCount: Int,
    sessionElapsedSeconds: Long = 0L,
    heroMetric: TileHeroMetric = TileHeroMetric.WATTS,
    modifier: Modifier = Modifier
) {
    val isCadenceAlert = false
    val alertAlpha = 1f

    val cardAlpha = if (bike.isStale) 0.45f else 1.0f
    val zoneColor = bike.ftpZone.color
    val metrics = resolveCyclingTileMetrics(bike, heroMetric)

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
        val showBezelButtons = !isVeryCompact && totalHeight >= 158.dp

        val wattsText = metrics.heroValue
        val wattsFontSize: TextUnit = (
            (minDim * 0.25f).coerceIn(if (isUltraDense) 12f else 19f, 92f) *
                if (wattsText.length >= 4) 0.78f else 1f
            ).sp
        val wattsUnitFontSize: TextUnit = (minDim * 0.068f).coerceIn(if (isUltraDense) 6.5f else 7.5f, 24f).sp
        val footerFontSize: TextUnit = (minDim * 0.135f).coerceIn(if (isUltraDense) 9.5f else 12f, 48f).sp
        val footerLabelFontSize: TextUnit = (minDim * 0.052f).coerceIn(if (isUltraDense) 6f else 6.5f, 18f).sp
        val headerFontSize: TextUnit = (minDim * 0.072f).coerceIn(if (isUltraDense) 7.5f else 8.5f, 24f).sp

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
                                    text = bike.displayId.ifEmpty { "#${bike.bikeId}" },
                                    style = TabularNumTextStyle,
                                    fontFamily = Dseg7FontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = headerFontSize,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            val idText = bike.displayId.ifEmpty { "#${bike.bikeId}" }
                            val isNarrowWithLongId = totalWidth < 112.dp && idText.length >= 5
                            Text(
                                text = when {
                                    bike.isStale -> "IDLE"
                                    showFtpCell || isNarrowWithLongId -> "Z${bike.ftpZone.zoneNumber}"
                                    else -> "Z${bike.ftpZone.zoneNumber} ${bike.ftpPercent}%"
                                },
                                style = TabularNumTextStyle,
                                fontSize = headerFontSize,
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

                // 2. HERO / METRICS AREA (Stacked centered on portrait 30-bike tiles; inline width-safe on ultra-short landscape)
                if (isUltraDense) {
                    val denseUnitSize = minOf(totalHeight.value * 0.10f, totalWidth.value * 0.08f).coerceIn(6f, 8.5f).sp
                    val leftUnit = if (metrics.leftFooterLabel == "WATTS") "W" else metrics.leftFooterLabel

                    if (totalHeight >= 66.dp) {
                        val stackedHeroSize = minOf(totalWidth.value * 0.20f, totalHeight.value * 0.21f).coerceIn(11f, 18f).sp
                        val stackedSubSize = minOf(totalWidth.value * 0.125f, totalHeight.value * 0.145f).coerceIn(8.5f, 13f).sp
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(horizontalArrangement = Arrangement.Center) {
                                Text(
                                    text = metrics.heroValue,
                                    style = TightTabularNumTextStyle,
                                    fontFamily = Dseg7FontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = stackedHeroSize,
                                    lineHeight = stackedHeroSize,
                                    color = if (metrics.isHeroZoneColored) zoneColor else Color.White,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.alignByBaseline()
                                )
                                Text(
                                    text = metrics.heroUnitCompact,
                                    style = TightTabularNumTextStyle,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = denseUnitSize,
                                    lineHeight = denseUnitSize,
                                    fontWeight = FontWeight.Black,
                                    color = StagesCyan,
                                    modifier = Modifier
                                        .padding(start = 1.5.dp)
                                        .alignByBaseline(),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Spacer(modifier = Modifier.height(1.dp))
                            Row(horizontalArrangement = Arrangement.Center) {
                                Text(
                                    text = "${metrics.leftFooterValue}$leftUnit" +
                                        if (heroMetric == TileHeroMetric.FTP_PERCENT) " • ${metrics.rightFooterValue}RPM" else "",
                                    style = TightTabularNumTextStyle,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = stackedSubSize,
                                    lineHeight = stackedSubSize,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFCBD5E1),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    } else {
                        val denseHeroSize = minOf(totalHeight.value * 0.23f, totalWidth.value * 0.125f).coerceIn(9.5f, 16f).sp
                        val denseSubSize = minOf(totalHeight.value * 0.18f, totalWidth.value * 0.105f).coerceIn(8.5f, 13.5f).sp
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row {
                                Text(
                                    text = metrics.heroValue,
                                    style = TightTabularNumTextStyle,
                                    fontFamily = Dseg7FontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = denseHeroSize,
                                    lineHeight = denseHeroSize,
                                    color = if (metrics.isHeroZoneColored) zoneColor else Color.White,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.alignByBaseline()
                                )
                                Text(
                                    text = metrics.heroUnitCompact,
                                    style = TightTabularNumTextStyle,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = denseUnitSize,
                                    lineHeight = denseUnitSize,
                                    fontWeight = FontWeight.Black,
                                    color = StagesCyan,
                                    modifier = Modifier
                                        .padding(start = 1.dp)
                                        .alignByBaseline(),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            Row {
                                Text(
                                    text = metrics.leftFooterValue,
                                    style = TightTabularNumTextStyle,
                                    fontFamily = Dseg7FontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = denseSubSize,
                                    lineHeight = denseSubSize,
                                    color = Color(0xFFCBD5E1),
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.alignByBaseline()
                                )
                                Text(
                                    text = leftUnit,
                                    style = TightTabularNumTextStyle,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = denseUnitSize,
                                    lineHeight = denseUnitSize,
                                    fontWeight = FontWeight.Bold,
                                    color = StagesInkMuted,
                                    modifier = Modifier
                                        .padding(start = 1.dp)
                                        .alignByBaseline(),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                } else {
                    // HERO CENTER READOUT
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                contentAlignment = Alignment.CenterEnd,
                                modifier = Modifier.alignByBaseline()
                            ) {
                                Text(
                                    text = metrics.heroGhost,
                                    style = TightTabularNumTextStyle,
                                    fontFamily = Dseg7FontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = wattsFontSize,
                                    lineHeight = wattsFontSize,
                                    color = StagesGhost,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = wattsText,
                                    style = TightTabularNumTextStyle,
                                    fontFamily = Dseg7FontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = wattsFontSize,
                                    lineHeight = wattsFontSize,
                                    color = if (metrics.isHeroZoneColored) zoneColor else Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Spacer(modifier = Modifier.width(if (isVeryCompact) 3.dp else 4.dp))
                            Text(
                                text = if (isVeryCompact) metrics.heroUnitCompact else metrics.heroUnitFull,
                                style = TightTabularNumTextStyle,
                                fontFamily = FontFamily.Monospace,
                                fontSize = wattsUnitFontSize,
                                lineHeight = wattsUnitFontSize,
                                fontWeight = FontWeight.Black,
                                color = StagesCyan,
                                modifier = Modifier.alignByBaseline(),
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

                    // 4. COMPACT FOOTER ROW
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = if (isVeryCompact) 2.dp else 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StagesFooterCell(
                            value = metrics.leftFooterValue,
                            ghost = metrics.leftFooterGhost,
                            label = metrics.leftFooterLabel,
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
                                    .background(StagesRule)
                            )

                            StagesFooterCell(
                                value = metrics.rightFooterValue,
                                ghost = metrics.rightFooterGhost,
                                label = metrics.rightFooterLabel,
                                fontSize = footerFontSize,
                                labelFontSize = footerLabelFontSize,
                                valueColor = if (metrics.isRightFooterZoneColored) zoneColor else Color(0xFFE2E8F0),
                                isAlert = false,
                                alertAlpha = 1f,
                                modifier = Modifier.weight(1f)
                            )
                        }
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
                    style = TabularNumTextStyle,
                    fontFamily = Dseg7FontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = effectiveFontSize,
                    color = StagesGhost,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Text(
                text = value,
                style = TabularNumTextStyle,
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
            color = if (isAlert) Color.White else StagesInkMuted,
            letterSpacing = 0.3.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}
