package com.livefleet.studio.ui.components

import android.content.res.Configuration
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livefleet.studio.model.BikeProtocol
import com.livefleet.studio.model.Concept2ErgFilter
import com.livefleet.studio.model.IntervalState
import com.livefleet.studio.model.LeaderboardViewMode
import com.livefleet.studio.model.SortOrder
import com.livefleet.studio.ui.theme.*
import com.livefleet.studio.viewmodel.LiveFleetUiState

@Composable
fun TopBar(
    uiState: LiveFleetUiState,
    onSortChange: (SortOrder) -> Unit,
    onDemoToggle: (Boolean) -> Unit,
    onBleToggle: () -> Unit,
    onStartFtpTest: (minutes: Int, factor: Float) -> Unit,
    onViewModeChange: (LeaderboardViewMode) -> Unit,
    onToggleSessionTimer: () -> Unit,
    onEndWorkout: () -> Unit,
    onReopenSummary: () -> Unit = {},
    onStopwatchClick: () -> Unit = {},
    onViewLastFtpResults: () -> Unit = {},
    onZoneCountChange: (Int) -> Unit = {},
    onConcept2ErgFilterChange: (Concept2ErgFilter) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var viewMenuExpanded by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var ergFilterMenuExpanded by remember { mutableStateOf(false) }
    var ftpMenuExpanded by remember { mutableStateOf(false) }

    val hasLastFtpResults = uiState.ftpTest.lastCompletedResults != null
    val configuration = LocalConfiguration.current
    val isWideOrLandscape = configuration.screenWidthDp >= 600 ||
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Color(0xFF000000))
            .statusBarsPadding()
            .border(1.dp, Color(0xFF1E2230))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (isWideOrLandscape) {
            // LANDSCAPE / TABLET / DEX (2 sleek rows):
            // Row 1: Brand | 1 Hz Status Badge | Action Controls | Unified Dual-Clock + Stopwatch (Far Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TopBarBrandTitle()

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    contentAlignment = Alignment.CenterStart
                ) {
                    LiveScanStatusBadge(
                        isScanning = uiState.isScanning,
                        activeCount = uiState.activeBikeCount,
                        packetsPerSecond = uiState.packetsPerSecond
                    )
                }

                TopBarActionControls(
                    uiState = uiState,
                    hasLastFtpResults = hasLastFtpResults,
                    ftpMenuExpanded = ftpMenuExpanded,
                    onFtpExpand = { ftpMenuExpanded = true },
                    onFtpDismiss = { ftpMenuExpanded = false },
                    onDemoToggle = onDemoToggle,
                    onViewLastFtpResults = onViewLastFtpResults,
                    onStartFtpTest = onStartFtpTest,
                    onOpenSettings = onOpenSettings
                )

                // Unified Dual-Clock & Stopwatch Button pinned to the far right
                SessionTimerAndStopwatch(
                    sessionSeconds = uiState.sessionElapsedSeconds,
                    isSessionRunning = uiState.isSessionTimerRunning,
                    hasFtpResults = hasLastFtpResults,
                    intervalState = uiState.intervalState,
                    onToggleSession = onToggleSessionTimer,
                    onEndWorkout = onEndWorkout,
                    onReopenSummary = onReopenSummary,
                    onStopwatchClick = onStopwatchClick
                )
            }

            // Row 2: View & Sort Dropdowns (Left) | Class Average directly under Timer (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ViewAndSortDropdowns(
                    viewMode = uiState.viewMode,
                    sortOrder = uiState.sortOrder,
                    showErgFilter = uiState.selectedProtocol == BikeProtocol.CONCEPT2,
                    concept2ErgFilter = uiState.concept2ErgFilter,
                    viewMenuExpanded = viewMenuExpanded,
                    sortMenuExpanded = sortMenuExpanded,
                    ergFilterMenuExpanded = ergFilterMenuExpanded,
                    onViewExpand = { viewMenuExpanded = true },
                    onViewDismiss = { viewMenuExpanded = false },
                    onViewSelect = onViewModeChange,
                    onSortExpand = { sortMenuExpanded = true },
                    onSortDismiss = { sortMenuExpanded = false },
                    onSortSelect = onSortChange,
                    onErgFilterExpand = { ergFilterMenuExpanded = true },
                    onErgFilterDismiss = { ergFilterMenuExpanded = false },
                    onErgFilterSelect = onConcept2ErgFilterChange
                )

                ClassAveragePill(
                    averagePower = uiState.averagePower,
                    averageCadence = uiState.averageCadence,
                    isStacked = false
                )
            }
        } else {
            // PORTRAIT (3 uncrowded rows):
            // Row 1: Brand Title (Left) | FTP & Settings Controls (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TopBarBrandTitle()

                TopBarActionControls(
                    uiState = uiState,
                    hasLastFtpResults = hasLastFtpResults,
                    ftpMenuExpanded = ftpMenuExpanded,
                    onFtpExpand = { ftpMenuExpanded = true },
                    onFtpDismiss = { ftpMenuExpanded = false },
                    onDemoToggle = onDemoToggle,
                    onViewLastFtpResults = onViewLastFtpResults,
                    onStartFtpTest = onStartFtpTest,
                    onOpenSettings = onOpenSettings
                )
            }

            // Row 2: 1 Hz Live Scan Status Badge (Left) | Unified Dual-Clock + Stopwatch (Far Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    contentAlignment = Alignment.CenterStart
                ) {
                    LiveScanStatusBadge(
                        isScanning = uiState.isScanning,
                        activeCount = uiState.activeBikeCount,
                        packetsPerSecond = uiState.packetsPerSecond
                    )
                }

                SessionTimerAndStopwatch(
                    sessionSeconds = uiState.sessionElapsedSeconds,
                    isSessionRunning = uiState.isSessionTimerRunning,
                    hasFtpResults = hasLastFtpResults,
                    intervalState = uiState.intervalState,
                    onToggleSession = onToggleSessionTimer,
                    onEndWorkout = onEndWorkout,
                    onReopenSummary = onReopenSummary,
                    onStopwatchClick = onStopwatchClick
                )
            }

            // Row 3: View & Sort Dropdowns (Left) | Class Average directly under Timer (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ViewAndSortDropdowns(
                    viewMode = uiState.viewMode,
                    sortOrder = uiState.sortOrder,
                    showErgFilter = uiState.selectedProtocol == BikeProtocol.CONCEPT2,
                    concept2ErgFilter = uiState.concept2ErgFilter,
                    viewMenuExpanded = viewMenuExpanded,
                    sortMenuExpanded = sortMenuExpanded,
                    ergFilterMenuExpanded = ergFilterMenuExpanded,
                    onViewExpand = { viewMenuExpanded = true },
                    onViewDismiss = { viewMenuExpanded = false },
                    onViewSelect = onViewModeChange,
                    onSortExpand = { sortMenuExpanded = true },
                    onSortDismiss = { sortMenuExpanded = false },
                    onSortSelect = onSortChange,
                    onErgFilterExpand = { ergFilterMenuExpanded = true },
                    onErgFilterDismiss = { ergFilterMenuExpanded = false },
                    onErgFilterSelect = onConcept2ErgFilterChange
                )

                ClassAveragePill(
                    averagePower = uiState.averagePower,
                    averageCadence = uiState.averageCadence,
                    isStacked = true
                )
            }
        }
    }
}

@Composable
private fun TopBarBrandTitle() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(AccentEnergy),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                contentDescription = "LiveFleet Studio",
                tint = BgMain,
                modifier = Modifier.size(14.dp)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "LIVEFLEET ",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = TextMain,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = "STUDIO",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = AccentEnergy,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun TopBarActionControls(
    uiState: LiveFleetUiState,
    hasLastFtpResults: Boolean,
    ftpMenuExpanded: Boolean,
    onFtpExpand: () -> Unit,
    onFtpDismiss: () -> Unit,
    onDemoToggle: (Boolean) -> Unit,
    onViewLastFtpResults: () -> Unit,
    onStartFtpTest: (minutes: Int, factor: Float) -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (uiState.isDemoMode) {
            Surface(
                color = AccentSuccess.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, AccentSuccess.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .height(26.dp)
                    .clickable { onDemoToggle(false) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DEMO ✕",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = AccentSuccess,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        Box {
            Button(
                onClick = onFtpExpand,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentEnergy,
                    contentColor = BgMain
                ),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(26.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "FTP",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    softWrap = false
                )
            }
            FtpDropdown(
                expanded = ftpMenuExpanded,
                onDismiss = onFtpDismiss,
                onStartTest = onStartFtpTest
            )
        }

        Surface(
            color = Color(0xFF161B26),
            shape = RoundedCornerShape(5.dp),
            modifier = Modifier
                .size(26.dp)
                .clickable { onOpenSettings() }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Studio Settings",
                    tint = TextMuted,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun SessionTimerAndStopwatch(
    sessionSeconds: Long,
    isSessionRunning: Boolean,
    hasFtpResults: Boolean,
    intervalState: IntervalState,
    onToggleSession: () -> Unit,
    onEndWorkout: () -> Unit,
    onReopenSummary: () -> Unit,
    onStopwatchClick: () -> Unit
) {
    var timerMenuExpanded by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // Unified Dual Clock Capsule (Interval | Total Session) or Single Session Clock
        // Tapping the clock opens a clean Pause/Resume & Reset menu (zero extra buttons in bar).
        Box {
            Row(
                modifier = Modifier
                    .height(26.dp)
                    .background(Color(0xFF0C0F17), RoundedCornerShape(6.dp))
                    .border(
                        1.dp,
                        if (isSessionRunning || intervalState.isActive) {
                            AccentSuccess.copy(alpha = 0.5f)
                        } else {
                            Color(0xFF262D42)
                        },
                        RoundedCornerShape(6.dp)
                    )
                    .clickable { timerMenuExpanded = true }
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (intervalState.isActive) {
                    Text(
                        text = formatSessionClock(intervalState.elapsedSeconds),
                        style = TabularNumTextStyle,
                        fontFamily = Dseg7FontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSessionRunning) Color.White else AccentEnergy,
                        letterSpacing = 0.6.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(14.dp)
                            .background(Color(0xFF334155))
                    )
                    Text(
                        text = formatSessionClock(sessionSeconds),
                        style = TabularNumTextStyle,
                        fontFamily = Dseg7FontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.6.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                } else {
                    Text(
                        text = formatSessionClock(sessionSeconds),
                        style = TabularNumTextStyle,
                        fontFamily = Dseg7FontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSessionRunning) Color.White else AccentEnergy,
                        letterSpacing = 0.8.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            DropdownMenu(
                expanded = timerMenuExpanded,
                onDismissRequest = { timerMenuExpanded = false }
            ) {
                DropdownMenuItem(
                    leadingIcon = {
                        Icon(
                            imageVector = if (isSessionRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = if (isSessionRunning) AccentEnergy else AccentSuccess
                        )
                    },
                    text = {
                        Text(
                            text = when {
                                isSessionRunning -> "Pause Timer"
                                sessionSeconds > 0L -> "Resume Timer"
                                else -> "Start Timer"
                            },
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    onClick = {
                        timerMenuExpanded = false
                        onToggleSession()
                    }
                )
                if (!isSessionRunning && sessionSeconds == 0L && intervalState.hasCompletedSummary) {
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Flag,
                                contentDescription = null,
                                tint = AccentEnergy
                            )
                        },
                        text = {
                            Text(
                                text = "View Last Summary",
                                fontWeight = FontWeight.Bold,
                                color = AccentEnergy,
                                maxLines = 1,
                                softWrap = false
                            )
                        },
                        onClick = {
                            timerMenuExpanded = false
                            onReopenSummary()
                        }
                    )
                }
                if (sessionSeconds > 0L || intervalState.isActive ||
                    (!intervalState.hasCompletedSummary && (intervalState.completedIntervals.isNotEmpty() || hasFtpResults))
                ) {
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Flag,
                                contentDescription = null,
                                tint = AccentDanger
                            )
                        },
                        text = {
                            Text(
                                text = "End Workout",
                                fontWeight = FontWeight.Bold,
                                color = AccentDanger,
                                maxLines = 1,
                                softWrap = false
                            )
                        },
                        onClick = {
                            timerMenuExpanded = false
                            onEndWorkout()
                        }
                    )
                }
            }
        }

        // Dedicated Stopwatch Drawer Button (Starts Interval #1 or toggles strip)
        val isHighlighted = intervalState.isActive || intervalState.isStripExpanded
        Surface(
            color = if (isHighlighted) Color(0xFF9E2A2B) else Color(0xFF161B26),
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(
                1.dp,
                if (isHighlighted) Color(0xFFDC2626) else Color(0xFF283044)
            ),
            modifier = Modifier
                .height(26.dp)
                .clickable { onStopwatchClick() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Timer,
                    contentDescription = "Interval Stopwatch",
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
                if (intervalState.isActive || intervalState.completedIntervals.isNotEmpty()) {
                    Icon(
                        imageVector = if (intervalState.isStripExpanded) {
                            Icons.Rounded.ExpandLess
                        } else {
                            Icons.Rounded.ExpandMore
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ClassAveragePill(
    averagePower: Int,
    averageCadence: Float,
    isStacked: Boolean
) {
    if (isStacked) {
        Column(
            modifier = Modifier
                .background(Color(0xFF0C0F17), RoundedCornerShape(6.dp))
                .border(1.dp, Color(0xFF262D42), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "CLASS AVERAGE",
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Black,
                color = TextDim,
                letterSpacing = 0.5.sp,
                maxLines = 1,
                softWrap = false
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LabelThenValue("WATTS", "$averagePower", AccentEnergy)
                Text("•", color = TextDim, fontSize = 8.sp, maxLines = 1, softWrap = false)
                LabelThenValue("RPM", "${averageCadence.toInt()}", TextMuted)
            }
        }
    } else {
        Row(
            modifier = Modifier
                .height(26.dp)
                .background(Color(0xFF0C0F17), RoundedCornerShape(6.dp))
                .border(1.dp, Color(0xFF262D42), RoundedCornerShape(6.dp))
                .padding(horizontal = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "CLASS AVERAGE",
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Black,
                color = TextDim,
                letterSpacing = 0.5.sp,
                maxLines = 1,
                softWrap = false
            )
            LabelThenValue("WATTS", "$averagePower", AccentEnergy)
            Text("•", color = TextDim, fontSize = 9.sp, maxLines = 1, softWrap = false)
            LabelThenValue("RPM", "${averageCadence.toInt()}", TextMuted)
        }
    }
}

@Composable
private fun LabelThenValue(label: String, value: String, labelColor: Color) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = label,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Black,
            color = labelColor,
            maxLines = 1,
            softWrap = false
        )
        Text(
            text = value,
            style = TabularNumTextStyle,
            fontFamily = Dseg7FontFamily,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = TextMain,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun ViewAndSortDropdowns(
    viewMode: LeaderboardViewMode,
    sortOrder: SortOrder,
    showErgFilter: Boolean,
    concept2ErgFilter: Concept2ErgFilter,
    viewMenuExpanded: Boolean,
    sortMenuExpanded: Boolean,
    ergFilterMenuExpanded: Boolean,
    onViewExpand: () -> Unit,
    onViewDismiss: () -> Unit,
    onViewSelect: (LeaderboardViewMode) -> Unit,
    onSortExpand: () -> Unit,
    onSortDismiss: () -> Unit,
    onSortSelect: (SortOrder) -> Unit,
    onErgFilterExpand: () -> Unit,
    onErgFilterDismiss: () -> Unit,
    onErgFilterSelect: (Concept2ErgFilter) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box {
            TopBarFilterDropdownTrigger(
                label = viewMode.displayName,
                onClick = onViewExpand
            )
            ViewModeDropdown(
                expanded = viewMenuExpanded,
                onDismiss = onViewDismiss,
                onSelect = onViewSelect
            )
        }

        Box {
            TopBarFilterDropdownTrigger(
                label = "Sort: ${sortOrder.displayName.substringBefore(" ")}",
                onClick = onSortExpand
            )
            SortDropdown(
                expanded = sortMenuExpanded,
                onDismiss = onSortDismiss,
                onSelect = onSortSelect
            )
        }

        if (showErgFilter) {
            Box {
                TopBarFilterDropdownTrigger(
                    label = concept2ErgFilter.label,
                    onClick = onErgFilterExpand
                )
                DropdownMenu(
                    expanded = ergFilterMenuExpanded,
                    onDismissRequest = onErgFilterDismiss
                ) {
                    Concept2ErgFilter.entries.forEach { filter ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = filter.label,
                                    fontWeight = if (filter == concept2ErgFilter) FontWeight.Black else FontWeight.Medium,
                                    color = if (filter == concept2ErgFilter) AccentEnergy else TextMain,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            onClick = {
                                onErgFilterSelect(filter)
                                onErgFilterDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveScanStatusBadge(
    isScanning: Boolean,
    activeCount: Int,
    packetsPerSecond: Int
) {
    val infiniteTransition = rememberInfiniteTransition(label = "liveScanDot")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val statusColor = if (isScanning) AccentSuccess else TextDim
    val bgColor = if (isScanning) AccentSuccess.copy(alpha = 0.12f) else Color(0xFF0E121B)
    val borderColor = if (isScanning) AccentSuccess.copy(alpha = 0.45f) else Color(0xFF232A3B)

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.height(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .alpha(if (isScanning) pulseAlpha else 0.7f)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Text(
                text = if (isScanning) {
                    "SCANNING • $activeCount active • $packetsPerSecond pkt/s"
                } else {
                    "STANDBY • $activeCount active • 0 pkt/s"
                },
                style = TabularNumTextStyle,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = if (isScanning) AccentSuccess else TextMuted,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun TopBarFilterDropdownTrigger(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        color = BgCard,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier
            .height(26.dp)
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain,
                maxLines = 1,
                softWrap = false
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun ViewModeDropdown(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onSelect: (LeaderboardViewMode) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        LeaderboardViewMode.entries.forEach { mode ->
            DropdownMenuItem(
                text = { Text(mode.displayName, maxLines = 1, softWrap = false) },
                onClick = {
                    onSelect(mode)
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun SortDropdown(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onSelect: (SortOrder) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        SortOrder.entries.forEach { order ->
            DropdownMenuItem(
                text = { Text(order.displayName, maxLines = 1, softWrap = false) },
                onClick = {
                    onSelect(order)
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun FtpDropdown(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onStartTest: (minutes: Int, factor: Float) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = {
                Column {
                    Text("5-Minute FTP Test", fontWeight = FontWeight.Bold)
                    Text("0.85x Factor • Suggested: 80–90 RPM", fontSize = 11.sp, color = TextMuted)
                }
            },
            onClick = {
                onStartTest(5, 0.85f)
                onDismiss()
            }
        )
        DropdownMenuItem(
            text = {
                Column {
                    Text("20-Minute FTP Test", fontWeight = FontWeight.Bold)
                    Text("0.95x Factor • Suggested: 80–90 RPM", fontSize = 11.sp, color = TextMuted)
                }
            },
            onClick = {
                onStartTest(20, 0.95f)
                onDismiss()
            }
        )
    }
}

private fun formatSessionClock(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hrs > 0) {
        String.format("%02d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format("%02d:%02d", mins, secs)
    }
}
