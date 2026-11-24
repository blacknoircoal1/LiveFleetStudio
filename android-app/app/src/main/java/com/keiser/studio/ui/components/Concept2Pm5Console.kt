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
import kotlin.math.cbrt
import kotlin.math.roundToInt

// Authentic Concept2 PM5 Hardware & Backlit Monochrome LCD Palette
private val Pm5BezelDark = Color(0xFF181B21)
private val Pm5BezelInnerRim = Color(0xFF2B313C)
private val Pm5BezelButton = Color(0xFF2D333F)
private val Pm5BezelButtonBorder = Color(0xFF3E4656)

private val Pm5LcdTop = Color(0xFFDDE6EA)       // Cool slate-ice backlit LCD top
private val Pm5LcdBottom = Color(0xFFC8D4DA)    // Subtle backlit LCD bottom gradient
private val Pm5LcdBorder = Color(0xFF6B7B85)
private val Pm5LcdInk = Color(0xFF0E1722)       // Crisp dark-navy/black LCD ink
private val Pm5LcdInkMuted = Color(0xFF475866)  // Secondary LCD label ink
private val Pm5LcdGhost = Color(0x160E1722)     // Faint unlit LCD segments
private val Pm5LcdRule = Color(0xFF8FA1AC)      // Hairline PM5 grid divider
private val Pm5HeaderBar = Color(0xFF1A2530)    // Inverted PM5 top status bar

/**
 * Calculates Concept2 split time in seconds from power (watts) using the official
 * Concept2 flywheel power equation:
 *   watts = 2.80 / (splitSeconds / 500)^3  =>  splitSeconds = 500 * cbrt(2.80 / watts)
 *
 * This yields exact /1000m split pace on BikeErg and exact /500m split pace on RowErg/SkiErg.
 */
fun calculateConcept2SplitSeconds(powerWatts: Int): Double? {
    if (powerWatts <= 0) return null
    return 500.0 * cbrt(2.80 / powerWatts.toDouble())
}

/**
 * Formats Concept2 split pace as "M:SS" (compact) or "M:SS.d" (standard).
 */
fun formatConcept2SplitPace(powerWatts: Int, showTenths: Boolean = false): String {
    val totalSeconds = calculateConcept2SplitSeconds(powerWatts) ?: return if (showTenths) "-:--.-" else "-:--"
    if (showTenths) {
        val totalTenths = (totalSeconds * 10.0).roundToInt()
        val mins = totalTenths / 600
        val secs = (totalTenths % 600) / 10
        val tenths = totalTenths % 10
        return String.format("%d:%02d.%d", mins, secs, tenths)
    } else {
        val roundedSeconds = totalSeconds.roundToInt()
        val mins = roundedSeconds / 60
        val secs = roundedSeconds % 60
        return String.format("%d:%02d", mins, secs)
    }
}

/**
 * Concept2 PM5 Performance Monitor Studio Console.
 * Faithfully reproduces the iconic Concept2 PM5 backlit monochrome LCD:
 * - Matte charcoal PM5 bezel with 5 physical right-edge menu button pads & subtle wordmark
 * - Cool ice-gray/slate backlit monochrome LCD with dark-ink 7-segment readouts & ghost segments
 * - Inverted PM5 top header strip with PM5 Bike ID and erg mode label
 * - Hero Split Pace (/1000m for BikeErg, /500m for RowErg/SkiErg) derived from live watts
 * - Classic PM5 hairline-divided lower telemetry row (Watts, RPM/s/m, and kCal when spacious)
 */
@Composable
fun Concept2Pm5Console(
    bike: BikeData,
    isFtpTestActive: Boolean,
    isCompact: Boolean,
    zoneCount: Int,
    sessionElapsedSeconds: Long = 0L,
    modifier: Modifier = Modifier
) {
    val ergType = bike.equipmentType.uppercase()
    val isBikeErg = ergType == "BIKEERG"
    val isRowErg = ergType == "ROWERG"
    val isSkiErg = ergType == "SKIERG"

    val ergHeaderLabel = when {
        bike.isStale -> "IDLE"
        isBikeErg -> "BIKEERG"
        isRowErg -> "ROWERG"
        isSkiErg -> "SKIERG"
        else -> "PM5"
    }
    val ergFooterLabel = when {
        isBikeErg -> "BIKEERG • PM5"
        isRowErg -> "ROWERG • PM5"
        isSkiErg -> "SKIERG • PM5"
        else -> "PM5 MONITOR"
    }
    val paceUnit = if (isBikeErg) "/1000m" else "/500m"
    val cadenceUnit = if (isBikeErg) "RPM" else "s/m"

    // Cycling cadence target alert during FTP test applies to BikeErg (<80 or >90 RPM)
    val isCadenceAlert = isFtpTestActive && isBikeErg && (bike.cadenceRpm < 80f || bike.cadenceRpm > 90f)
    val infiniteTransition = rememberInfiniteTransition(label = "pm5CadenceAlert")
    val alertAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pm5AlertAlpha"
    )

    val cardAlpha = if (bike.isStale) 0.45f else 1.0f

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .alpha(cardAlpha)
            .padding(2.dp)
    ) {
        val totalHeight = maxHeight
        val totalWidth = maxWidth
        val isVeryCompact = totalHeight < 138.dp || totalWidth < 135.dp || isCompact
        val showTenths = !isVeryCompact && totalWidth >= 175.dp
        val showThirdMetric = !isVeryCompact && totalWidth >= 195.dp && totalHeight >= 150.dp
        val showBezelFooter = !isVeryCompact && totalHeight >= 150.dp

        val splitPaceText = formatConcept2SplitPace(bike.powerWatts, showTenths = showTenths)
        val splitPaceGhost = if (showTenths) "8:88.8" else "8:88"

        val paceFontSize: TextUnit = when {
            totalHeight < 110.dp || totalWidth < 115.dp -> 16.sp
            isVeryCompact -> 20.sp
            totalHeight >= 210.dp && totalWidth >= 240.dp -> 34.sp
            else -> 26.sp
        }
        val metricFontSize: TextUnit = when {
            totalHeight < 110.dp || totalWidth < 115.dp -> 12.sp
            isVeryCompact -> 14.sp
            totalHeight >= 210.dp && totalWidth >= 240.dp -> 20.sp
            else -> 16.sp
        }

        // OUTER PM5 BEZEL HOUSING
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(Pm5BezelDark)
                .border(
                    BorderStroke(
                        if (isCadenceAlert) 2.dp else 1.dp,
                        if (isCadenceAlert) AccentDanger.copy(alpha = alertAlpha) else Pm5BezelInnerRim
                    ),
                    RoundedCornerShape(8.dp)
                )
                .padding(
                    start = if (isVeryCompact) 3.dp else 5.dp,
                    top = if (isVeryCompact) 3.dp else 5.dp,
                    end = if (isVeryCompact) 3.dp else 4.dp,
                    bottom = if (showBezelFooter) 2.dp else if (isVeryCompact) 3.dp else 5.dp
                )
        ) {
            // LCD SCREEN + ICONIC 5-BUTTON RIGHT BEZEL COLUMN
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(if (isVeryCompact) 2.dp else 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // CLASSIC MONOCHROME BACKLIT PM5 LCD PANEL
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Brush.verticalGradient(listOf(Pm5LcdTop, Pm5LcdBottom)))
                        .border(BorderStroke(1.dp, Pm5LcdBorder), RoundedCornerShape(4.dp))
                        .padding(
                            horizontal = if (isVeryCompact) 4.dp else 6.dp,
                            vertical = if (isVeryCompact) 3.dp else 4.dp
                        ),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // 1. INVERTED PM5 HEADER BAR (Monochrome)
                    Surface(
                        color = Pm5HeaderBar,
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = if (isVeryCompact) 4.dp else 6.dp, vertical = 1.5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "PM5",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = if (isVeryCompact) 7.sp else 8.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFB8C7D1),
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "#${bike.bikeId}",
                                    fontFamily = Dseg7FontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = if (isVeryCompact) 8.5.sp else 10.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            Text(
                                text = ergHeaderLabel,
                                fontFamily = FontFamily.Monospace,
                                fontSize = if (isVeryCompact) 6.5.sp else 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (bike.isStale) Color(0xFFFCA5A5) else Color(0xFFB8C7D1),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // 2. HERO PM5 SPLIT PACE READOUT (min:sec /1000m or /500m)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(contentAlignment = Alignment.CenterEnd) {
                                Text(
                                    text = splitPaceGhost,
                                    fontFamily = Dseg7FontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = paceFontSize,
                                    color = Pm5LcdGhost,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = splitPaceText,
                                    fontFamily = Dseg7FontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = paceFontSize,
                                    color = Pm5LcdInk,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Spacer(modifier = Modifier.width(if (isVeryCompact) 2.dp else 4.dp))
                            Text(
                                text = paceUnit,
                                fontFamily = FontFamily.Monospace,
                                fontSize = if (isVeryCompact) 6.5.sp else 8.5.sp,
                                fontWeight = FontWeight.Black,
                                color = Pm5LcdInkMuted,
                                modifier = Modifier.padding(bottom = if (isVeryCompact) 1.dp else 3.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // 3. CLASSIC PM5 HORIZONTAL HAIRLINE DIVIDER
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Pm5LcdRule)
                    )

                    // 4. LOWER PM5 DATA CELLS: WATTS | RPM (s/m) [| KCAL]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = if (isVeryCompact) 2.dp else 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Power (Watts)
                        Pm5LowerMetricCell(
                            value = "${bike.powerWatts}",
                            ghost = "888",
                            label = "WATTS",
                            fontSize = metricFontSize,
                            isVeryCompact = isVeryCompact,
                            isAlert = false,
                            alertAlpha = 1f,
                            modifier = Modifier.weight(1f)
                        )

                        // Hairline vertical separator
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(if (isVeryCompact) 18.dp else 24.dp)
                                .background(Pm5LcdRule)
                        )

                        // Cadence (RPM / s/m) with FTP test cadence alert
                        Pm5LowerMetricCell(
                            value = "${bike.cadenceRpm.toInt()}",
                            ghost = "888",
                            label = cadenceUnit,
                            fontSize = metricFontSize,
                            isVeryCompact = isVeryCompact,
                            isAlert = isCadenceAlert,
                            alertAlpha = alertAlpha,
                            modifier = Modifier.weight(1f)
                        )

                        if (showThirdMetric) {
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(24.dp)
                                    .background(Pm5LcdRule)
                            )
                            Pm5LowerMetricCell(
                                value = "${bike.calories}",
                                ghost = "888",
                                label = "CAL",
                                fontSize = metricFontSize,
                                isVeryCompact = false,
                                isAlert = false,
                                alertAlpha = 1f,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // ICONIC PM5 RIGHT-EDGE 5-BUTTON COLUMN
                Column(
                    modifier = Modifier
                        .width(if (isVeryCompact) 4.dp else 7.dp)
                        .fillMaxHeight()
                        .padding(vertical = if (isVeryCompact) 6.dp else 10.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    repeat(5) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (isVeryCompact) 7.dp else 11.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(Pm5BezelButton)
                                .border(0.5.dp, Pm5BezelButtonBorder, RoundedCornerShape(1.5.dp))
                        )
                    }
                }
            }

            // SUBTLE BEZEL BRANDING FOOTER (only when tile has comfortable height)
            if (showBezelFooter) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, start = 2.dp, end = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "concept 2",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7B8798),
                        letterSpacing = 0.6.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = ergFooterLabel,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 6.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5A6577),
                        letterSpacing = 0.4.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun Pm5LowerMetricCell(
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
                    color = Pm5LcdGhost,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Text(
                text = value,
                fontFamily = Dseg7FontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                color = if (isAlert) Color.White else Pm5LcdInk,
                maxLines = 1,
                softWrap = false
            )
        }
        Text(
            text = if (isAlert) "$label !" else label,
            fontFamily = FontFamily.Monospace,
            fontSize = if (isVeryCompact) 6.sp else 7.sp,
            fontWeight = FontWeight.Black,
            color = if (isAlert) Color.White else Pm5LcdInkMuted,
            letterSpacing = 0.3.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}
