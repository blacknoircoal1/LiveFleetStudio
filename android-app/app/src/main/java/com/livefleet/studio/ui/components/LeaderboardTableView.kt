package com.livefleet.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livefleet.studio.model.BikeData
import com.livefleet.studio.model.BrandType
import com.livefleet.studio.model.Concept2MetricMode
import com.livefleet.studio.ui.theme.*

/**
 * Zero-scroll Auto-Fit Studio Table View.
 * Automatically scales row heights & fonts (and splits into 2 side-by-side columns in landscape
 * when > 10 machines) so all 1–30+ machines fit on one screen without scrolling.
 */
@Composable
fun LeaderboardTableView(
    bikes: List<BikeData>,
    isFtpTestActive: Boolean,
    concept2MetricMode: Concept2MetricMode = Concept2MetricMode.METRES,
    onBikeClick: (BikeData) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val hasConcept2 = bikes.any {
        it.brandType == BrandType.CONCEPT2 || it.brand.equals("Concept2", ignoreCase = true)
    }
    val showMetresCol = hasConcept2 && concept2MetricMode == Concept2MetricMode.METRES
    val showCaloriesCol = hasConcept2 && concept2MetricMode == Concept2MetricMode.CALORIES

    val hasBike = bikes.any {
        val eq = it.equipmentType.uppercase()
        eq != "ROWERG" && eq != "SKIERG"
    }
    val showFtpAndZoneCols = bikes.isEmpty() || hasBike
    val (machineHeader, rateHeader) = resolveTableHeaders(bikes)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        val useTwoColumns = maxWidth >= 560.dp && bikes.size > 10
        val columnChunks = if (useTwoColumns) {
            val mid = (bikes.size + 1) / 2
            listOf(bikes.take(mid), bikes.drop(mid))
        } else {
            listOf(bikes)
        }

        val maxRowsInCol = columnChunks.maxOfOrNull { it.size }?.coerceAtLeast(1) ?: 1
        val headerHeight = if (maxHeight >= 500.dp) 30.dp else 22.dp
        val rowSpacing = if (maxRowsInCol >= 20) 1.dp else 2.dp
        val availableForRows = (maxHeight - headerHeight - 6.dp - (rowSpacing * (maxRowsInCol - 1))).coerceAtLeast(120.dp)
        val rowHeight = (availableForRows / maxRowsInCol).coerceAtMost(if (maxWidth >= 600.dp) 52.dp else 44.dp)

        val isDense = rowHeight < 26.dp
        val headerFontSize: TextUnit = when {
            isDense -> 8.5.sp
            maxWidth >= 600.dp -> 12.sp
            else -> 10.sp
        }
        val valueFontSize: TextUnit = (rowHeight.value * 0.48f).coerceIn(9.5f, if (maxWidth >= 600.dp) 22f else 15f).sp
        val zoneFontSize: TextUnit = (rowHeight.value * 0.40f).coerceIn(8f, 14f).sp

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            columnChunks.forEachIndexed { chunkIdx, chunkBikes ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(rowSpacing)
                ) {
                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(headerHeight)
                            .background(Color(0xFF0C0E14), RoundedCornerShape(4.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = if (isDense) 6.dp else 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (showFtpAndZoneCols) {
                            Text("ZONE", style = TightTabularNumTextStyle, fontSize = headerFontSize, fontWeight = FontWeight.Black, color = TextMuted, modifier = Modifier.weight(0.55f), maxLines = 1, softWrap = false)
                        }
                        Text(machineHeader, style = TightTabularNumTextStyle, fontSize = headerFontSize, fontWeight = FontWeight.Black, color = TextMuted, modifier = Modifier.weight(0.9f), maxLines = 1, softWrap = false)
                        Text("POWER", style = TightTabularNumTextStyle, fontSize = headerFontSize, fontWeight = FontWeight.Black, color = TextMuted, modifier = Modifier.weight(0.9f), maxLines = 1, softWrap = false)
                        Text(rateHeader, style = TightTabularNumTextStyle, fontSize = headerFontSize, fontWeight = FontWeight.Black, color = TextMuted, modifier = Modifier.weight(0.65f), maxLines = 1, softWrap = false)
                        if (showFtpAndZoneCols) {
                            Text("FTP %", style = TightTabularNumTextStyle, fontSize = headerFontSize, fontWeight = FontWeight.Black, color = TextMuted, modifier = Modifier.weight(0.8f), maxLines = 1, softWrap = false)
                        }
                        if (showMetresCol) {
                            Text("METRES", style = TightTabularNumTextStyle, fontSize = headerFontSize, fontWeight = FontWeight.Black, color = TextMuted, modifier = Modifier.weight(0.85f), maxLines = 1, softWrap = false)
                        }
                        if (showCaloriesCol) {
                            Text("KCAL", style = TightTabularNumTextStyle, fontSize = headerFontSize, fontWeight = FontWeight.Black, color = TextMuted, modifier = Modifier.weight(0.8f), maxLines = 1, softWrap = false)
                        }
                    }

                    chunkBikes.forEachIndexed { idx, bike ->
                        val zoneColor = bike.ftpZone.color
                        val zoneCode = bike.ftpZone.code
                        val isConcept2Bike = bike.brandType == BrandType.CONCEPT2 || bike.brand.equals("Concept2", ignoreCase = true)
                        val isRowOrSki = bike.equipmentType.equals("ROWERG", ignoreCase = true) ||
                            bike.equipmentType.equals("SKIERG", ignoreCase = true)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(rowHeight)
                                .background(
                                    when {
                                        bike.isInstructorBike -> AccentEnergy.copy(alpha = 0.08f)
                                        (chunkIdx + idx) % 2 == 0 -> Color(0xFF05070B)
                                        else -> Color(0xFF0A0C12)
                                    },
                                    RoundedCornerShape(4.dp)
                                )
                                .then(
                                    if (bike.isInstructorBike) {
                                        Modifier.border(1.dp, AccentEnergy.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable { onBikeClick(bike) }
                                .padding(horizontal = if (isDense) 6.dp else 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Zone Badge (bikes only; RowErg/SkiErg show "—" when mixed)
                            if (showFtpAndZoneCols) {
                                if (isRowOrSki) {
                                    Text(
                                        text = "—",
                                        style = TightTabularNumTextStyle,
                                        fontSize = zoneFontSize,
                                        color = TextDim,
                                        modifier = Modifier.weight(0.55f),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .weight(0.55f)
                                            .background(zoneColor, RoundedCornerShape(3.dp))
                                            .padding(horizontal = 3.dp, vertical = if (isDense) 0.5.dp else 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = zoneCode,
                                            style = TightTabularNumTextStyle,
                                            fontSize = zoneFontSize,
                                            lineHeight = zoneFontSize,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }

                            // Machine ID + optional INSTRUCTOR badge
                            Row(
                                modifier = Modifier.weight(0.9f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = bike.displayId.ifEmpty { "#${bike.bikeId}" },
                                    style = TightTabularNumTextStyle,
                                    fontSize = valueFontSize,
                                    lineHeight = valueFontSize,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentEnergy,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                if (bike.isInstructorBike) {
                                    Box(
                                        modifier = Modifier
                                            .background(AccentEnergy, RoundedCornerShape(2.dp))
                                            .padding(horizontal = 3.dp, vertical = 0.5.dp)
                                    ) {
                                        Text(
                                            text = if (isDense) "★" else "★ INST",
                                            style = TightTabularNumTextStyle,
                                            fontSize = (zoneFontSize.value * 0.75f).coerceIn(6.5f, 10f).sp,
                                            fontWeight = FontWeight.Black,
                                            color = BgMain,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }

                            // Power Watts
                            Text(
                                text = "${bike.powerWatts} W",
                                style = TightTabularNumTextStyle,
                                fontSize = valueFontSize,
                                lineHeight = valueFontSize,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.weight(0.9f)
                            )

                            // Cadence RPM / SPM
                            Text(
                                text = "${bike.cadenceRpm.toInt()}",
                                style = TightTabularNumTextStyle,
                                fontSize = valueFontSize,
                                lineHeight = valueFontSize,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE2E8F0),
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.weight(0.65f)
                            )

                            // FTP % (bikes only; RowErg/SkiErg show "—" when mixed)
                            if (showFtpAndZoneCols) {
                                if (isRowOrSki) {
                                    Text(
                                        text = "—",
                                        style = TightTabularNumTextStyle,
                                        fontSize = valueFontSize,
                                        lineHeight = valueFontSize,
                                        color = TextDim,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.weight(0.8f)
                                    )
                                } else {
                                    val ftpText = "${bike.ftpPercent}%"
                                    Text(
                                        text = ftpText,
                                        style = TightTabularNumTextStyle,
                                        fontFamily = Dseg7FontFamily,
                                        fontSize = if (ftpText.length >= 5) (valueFontSize.value * 0.82f).sp else valueFontSize,
                                        lineHeight = valueFontSize,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.weight(0.8f)
                                    )
                                }
                            }

                            if (showMetresCol) {
                                val metresText = if (isConcept2Bike) "${bike.distanceMeters} m" else "—"
                                Text(
                                    text = metresText,
                                    style = TightTabularNumTextStyle,
                                    fontSize = if (metresText.length >= 7) (valueFontSize.value * 0.82f).sp else valueFontSize,
                                    lineHeight = valueFontSize,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF93C5FD),
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.weight(0.85f)
                                )
                            }

                            if (showCaloriesCol) {
                                val calText = if (isConcept2Bike) "${bike.calories} kcal" else "—"
                                Text(
                                    text = calText,
                                    style = TightTabularNumTextStyle,
                                    fontSize = if (calText.length >= 8) (valueFontSize.value * 0.82f).sp else valueFontSize,
                                    lineHeight = valueFontSize,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFCD34D),
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.weight(0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun resolveTableHeaders(bikes: List<BikeData>): Pair<String, String> {
    if (bikes.isEmpty()) return "BIKE" to "RPM"
    val hasRow = bikes.any { it.equipmentType.equals("ROWERG", ignoreCase = true) }
    val hasSki = bikes.any { it.equipmentType.equals("SKIERG", ignoreCase = true) }
    val hasBike = bikes.any {
        val eq = it.equipmentType.uppercase()
        eq != "ROWERG" && eq != "SKIERG"
    }
    val allConcept2 = bikes.all { it.brandType == BrandType.CONCEPT2 }

    return when {
        hasRow && !hasSki && !hasBike -> "ROWERG" to "SPM"
        hasSki && !hasRow && !hasBike -> "SKIERG" to "SPM"
        (hasRow || hasSki) && !hasBike -> "ERG" to "SPM"
        (hasRow || hasSki) && hasBike && allConcept2 -> "ERG" to "RPM/SPM"
        (hasRow || hasSki) && hasBike -> "MACHINE" to "RPM/SPM"
        else -> "BIKE" to "RPM"
    }
}
