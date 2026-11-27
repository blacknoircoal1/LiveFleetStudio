package com.livefleet.studio.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livefleet.studio.model.BikeIntervalStats
import com.livefleet.studio.model.FtpResultItem
import com.livefleet.studio.model.IntervalState
import com.livefleet.studio.ui.theme.*

private sealed class SummaryTab {
    data object Session : SummaryTab()
    data class Interval(val index: Int) : SummaryTab()
    data object FtpTest : SummaryTab()
}

/**
 * Unified Workout Summary screen launched via "End Workout" in the clock menu.
 * Showcases overall Session stats, every completed Interval, and FTP Test results (when run).
 */
@Composable
fun IntervalSummaryScreen(
    intervalState: IntervalState,
    ftpResults: List<FtpResultItem>?,
    ftpTestType: String,
    ftpScalingFactor: Float,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLargeDisplay = LocalConfiguration.current.smallestScreenWidthDp >= 600
    val headerFontSize = if (isLargeDisplay) 14.sp else 10.sp
    val valueFontSize = if (isLargeDisplay) 20.sp else 13.sp

    val intervals = intervalState.completedIntervals
    val hasFtpResults = !ftpResults.isNullOrEmpty()

    var selectedTab by remember(intervals.size, hasFtpResults) {
        mutableStateOf<SummaryTab>(SummaryTab.Session)
    }

    val totalHrs = intervalState.sessionDurationSeconds / 3600
    val totalMins = (intervalState.sessionDurationSeconds % 3600) / 60
    val totalSecs = intervalState.sessionDurationSeconds % 60
    val totalDurationFormatted = if (totalHrs > 0) {
        String.format("%02d:%02d:%02d", totalHrs, totalMins, totalSecs)
    } else {
        String.format("%02d:%02d", totalMins, totalSecs)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Top Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0C0F17), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF262D42), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "WORKOUT SUMMARY",
                    fontSize = if (isLargeDisplay) 16.sp else 13.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = buildString {
                        append("Total Time $totalDurationFormatted")
                        append(" • ${intervals.size} Interval(s)")
                        if (hasFtpResults) append(" • $ftpTestType FTP")
                    },
                    style = TabularNumTextStyle,
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }

            Button(
                onClick = onDone,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentEnergy,
                    contentColor = BgMain
                ),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text("Done", fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
        }

        // Unified Selector Tabs: [Session] [INT 1] [INT 2]... [⚡ FTP Test]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SummaryTabChip(
                label = "Session • $totalDurationFormatted",
                selected = selectedTab is SummaryTab.Session,
                accentColor = AccentSuccess,
                onClick = { selectedTab = SummaryTab.Session }
            )

            intervals.forEachIndexed { idx, interval ->
                val durMins = interval.durationSeconds / 60
                val durSecs = interval.durationSeconds % 60
                val durText = String.format("%02d:%02d", durMins, durSecs)
                SummaryTabChip(
                    label = "INT ${interval.intervalNumber} • $durText",
                    selected = (selectedTab as? SummaryTab.Interval)?.index == idx,
                    accentColor = Color(0xFF0EA5E9),
                    onClick = { selectedTab = SummaryTab.Interval(idx) }
                )
            }

            if (hasFtpResults) {
                SummaryTabChip(
                    label = "⚡ FTP Test ($ftpTestType)",
                    selected = selectedTab is SummaryTab.FtpTest,
                    accentColor = AccentEnergy,
                    onClick = { selectedTab = SummaryTab.FtpTest }
                )
            }
        }

        when (val tab = selectedTab) {
            is SummaryTab.Session -> {
                WorkoutBikeStatsTable(
                    bannerTitle = "SESSION CLASS AVERAGE",
                    durationFormatted = totalDurationFormatted,
                    classAvgWatts = intervalState.sessionClassAvgWatts,
                    classAvgRpm = intervalState.sessionClassAvgCadence.toInt(),
                    bikeStats = intervalState.sessionBikeStats,
                    isLargeDisplay = isLargeDisplay,
                    headerFontSize = headerFontSize,
                    valueFontSize = valueFontSize
                )
            }
            is SummaryTab.Interval -> {
                val interval = intervals.getOrNull(tab.index)
                if (interval != null) {
                    val durMins = interval.durationSeconds / 60
                    val durSecs = interval.durationSeconds % 60
                    WorkoutBikeStatsTable(
                        bannerTitle = "CLASS AVERAGE • INT #${interval.intervalNumber}",
                        durationFormatted = String.format("%02d:%02d", durMins, durSecs),
                        classAvgWatts = interval.classAvgWatts,
                        classAvgRpm = interval.classAvgCadence.toInt(),
                        bikeStats = interval.bikeStats,
                        isLargeDisplay = isLargeDisplay,
                        headerFontSize = headerFontSize,
                        valueFontSize = valueFontSize
                    )
                }
            }
            is SummaryTab.FtpTest -> {
                val results = ftpResults.orEmpty()
                val classAvgTestWatts = if (results.isNotEmpty()) results.map { it.avgWatts }.average().toInt() else 0
                val classAvgNewFtp = if (results.isNotEmpty()) results.map { it.newFtp }.average().toInt() else 0

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF141005), RoundedCornerShape(8.dp))
                        .border(1.dp, AccentEnergy.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$ftpTestType FTP TEST",
                        style = TabularNumTextStyle,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = AccentEnergy
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AVG $classAvgTestWatts W",
                            style = TabularNumTextStyle,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text("•", color = TextDim, fontSize = 9.sp)
                        Text(
                            text = "NEW FTP $classAvgNewFtp W",
                            style = TabularNumTextStyle,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Black,
                            color = AccentEnergy
                        )
                    }
                }

                // FTP Results Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0C0E14), RoundedCornerShape(4.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 10.dp, vertical = if (isLargeDisplay) 9.dp else 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("BIKE", fontSize = headerFontSize, fontWeight = FontWeight.Black, color = TextMuted, modifier = Modifier.weight(0.8f), maxLines = 1, softWrap = false)
                    Text("TEST AVG", fontSize = headerFontSize, fontWeight = FontWeight.Black, color = TextMuted, modifier = Modifier.weight(0.95f), maxLines = 1, softWrap = false)
                    Text("OLD FTP", fontSize = headerFontSize, fontWeight = FontWeight.Black, color = TextMuted, modifier = Modifier.weight(0.85f), maxLines = 1, softWrap = false)
                    Text("NEW FTP", fontSize = headerFontSize, fontWeight = FontWeight.Black, color = TextMuted, modifier = Modifier.weight(0.95f), maxLines = 1, softWrap = false)
                    Text("CHANGE", fontSize = headerFontSize, fontWeight = FontWeight.Black, color = TextMuted, modifier = Modifier.weight(0.8f), maxLines = 1, softWrap = false)
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(if (isLargeDisplay) 4.dp else 2.dp)
                ) {
                    itemsIndexed(results, key = { _, r -> r.bikeId }) { idx, item ->
                        val deltaColor = if (item.delta >= 0) AccentSuccess else AccentDanger
                        val deltaText = if (item.delta >= 0) "+${item.delta} W" else "${item.delta} W"

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (idx % 2 == 0) Color(0xFF05070B) else Color(0xFF0A0C12),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = if (isLargeDisplay) 10.dp else 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "#${item.bikeId}",
                                style = TabularNumTextStyle,
                                fontSize = valueFontSize,
                                fontWeight = FontWeight.Bold,
                                color = AccentEnergy,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.weight(0.8f)
                            )
                            Text(
                                text = "${item.avgWatts} W",
                                style = TabularNumTextStyle,
                                fontSize = valueFontSize,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.weight(0.95f)
                            )
                            Text(
                                text = "${item.oldFtp} W",
                                style = TabularNumTextStyle,
                                fontSize = valueFontSize,
                                fontWeight = FontWeight.Medium,
                                color = TextMuted,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.weight(0.85f)
                            )
                            Text(
                                text = "${item.newFtp} W",
                                style = TabularNumTextStyle,
                                fontSize = valueFontSize,
                                fontWeight = FontWeight.Black,
                                color = AccentEnergy,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.weight(0.95f)
                            )
                            Text(
                                text = deltaText,
                                style = TabularNumTextStyle,
                                fontSize = valueFontSize,
                                fontWeight = FontWeight.Black,
                                color = deltaColor,
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

@Composable
private fun SummaryTabChip(
    label: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) accentColor else Color(0xFF0C0F18),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, if (selected) accentColor else Color(0xFF232A3B)),
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            style = TabularNumTextStyle,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Black,
            color = if (selected) BgMain else TextMain,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun WorkoutBikeStatsTable(
    bannerTitle: String,
    durationFormatted: String,
    classAvgWatts: Int,
    classAvgRpm: Int,
    bikeStats: List<BikeIntervalStats>,
    isLargeDisplay: Boolean,
    headerFontSize: androidx.compose.ui.unit.TextUnit,
    valueFontSize: androidx.compose.ui.unit.TextUnit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF09111D), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF1B3352), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = bannerTitle,
            style = TabularNumTextStyle,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF38BDF8)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TIME $durationFormatted",
                style = TabularNumTextStyle,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text("•", color = TextDim, fontSize = 9.sp)
            Text(
                text = "WATTS $classAvgWatts",
                style = TabularNumTextStyle,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Black,
                color = AccentEnergy
            )
            Text("•", color = TextDim, fontSize = 9.sp)
            Text(
                text = "RPM $classAvgRpm",
                style = TabularNumTextStyle,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }

    val hasCyclingStats = bikeStats.isEmpty() || bikeStats.any {
        val eq = it.equipmentType.uppercase()
        eq != "ROWERG" && eq != "SKIERG"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0C0E14), RoundedCornerShape(4.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 10.dp, vertical = if (isLargeDisplay) 9.dp else 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasCyclingStats) {
            Text("ZONE", fontSize = headerFontSize, fontWeight = FontWeight.Black, color = TextMuted, modifier = Modifier.weight(0.55f), maxLines = 1, softWrap = false)
        }
        Text(if (hasCyclingStats) "BIKE" else "ERG", fontSize = headerFontSize, fontWeight = FontWeight.Black, color = TextMuted, modifier = Modifier.weight(0.8f), maxLines = 1, softWrap = false)
        Text("AVG W", fontSize = headerFontSize, fontWeight = FontWeight.Black, color = TextMuted, modifier = Modifier.weight(0.85f), maxLines = 1, softWrap = false)
        Text("PEAK W", fontSize = headerFontSize, fontWeight = FontWeight.Black, color = TextMuted, modifier = Modifier.weight(0.85f), maxLines = 1, softWrap = false)
        Text(if (hasCyclingStats) "RPM" else "SPM", fontSize = headerFontSize, fontWeight = FontWeight.Black, color = TextMuted, modifier = Modifier.weight(0.7f), maxLines = 1, softWrap = false)
        if (hasCyclingStats) {
            Text("% FTP", fontSize = headerFontSize, fontWeight = FontWeight.Black, color = TextMuted, modifier = Modifier.weight(0.75f), maxLines = 1, softWrap = false)
        }
    }

    if (bikeStats.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No bike telemetry recorded during this segment.",
                fontSize = 12.sp,
                color = TextMuted
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(if (isLargeDisplay) 4.dp else 2.dp)
        ) {
            itemsIndexed(bikeStats, key = { _, s -> s.bikeId }) { idx, stat ->
                val isRowOrSki = stat.equipmentType.equals("ROWERG", ignoreCase = true) ||
                    stat.equipmentType.equals("SKIERG", ignoreCase = true)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (idx % 2 == 0) Color(0xFF05070B) else Color(0xFF0A0C12),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = if (isLargeDisplay) 10.dp else 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasCyclingStats) {
                        if (isRowOrSki) {
                            Text(
                                text = "—",
                                style = TabularNumTextStyle,
                                fontSize = valueFontSize,
                                color = TextDim,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.weight(0.55f)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .weight(0.55f)
                                    .background(stat.ftpZone.color, RoundedCornerShape(3.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stat.ftpZone.code,
                                    style = TabularNumTextStyle,
                                    fontSize = if (isLargeDisplay) 13.sp else 9.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Text(
                        text = stat.displayId.ifEmpty { "#${stat.bikeId}" },
                        style = TabularNumTextStyle,
                        fontSize = valueFontSize,
                        fontWeight = FontWeight.Bold,
                        color = AccentEnergy,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.weight(0.8f)
                    )

                    Text(
                        text = "${stat.avgWatts} W",
                        style = TabularNumTextStyle,
                        fontSize = valueFontSize,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.weight(0.85f)
                    )

                    Text(
                        text = "${stat.peakWatts} W",
                        style = TabularNumTextStyle,
                        fontSize = valueFontSize,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8),
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.weight(0.85f)
                    )

                    Text(
                        text = "${stat.avgCadence.toInt()}",
                        style = TabularNumTextStyle,
                        fontSize = valueFontSize,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE2E8F0),
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.weight(0.7f)
                    )

                    if (hasCyclingStats) {
                        Text(
                            text = if (isRowOrSki) "—" else "${stat.avgFtpPercent}%",
                            style = TabularNumTextStyle,
                            fontFamily = if (isRowOrSki) androidx.compose.ui.text.font.FontFamily.Default else Dseg7FontFamily,
                            fontSize = valueFontSize,
                            fontWeight = FontWeight.Bold,
                            color = if (isRowOrSki) TextDim else Color.White,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.weight(0.75f)
                        )
                    }
                }
            }
        }
    }
}
