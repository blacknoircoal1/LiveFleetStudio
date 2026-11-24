package com.keiser.studio.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keiser.studio.model.LeaderboardViewMode
import com.keiser.studio.model.SortOrder
import com.keiser.studio.ui.theme.*
import com.keiser.studio.viewmodel.KeiserUiState

@Composable
fun TopBar(
    uiState: KeiserUiState,
    onSortChange: (SortOrder) -> Unit,
    onDemoToggle: (Boolean) -> Unit,
    onBleToggle: () -> Unit,
    onStartFtpTest: (minutes: Int, factor: Float) -> Unit,
    onViewModeChange: (LeaderboardViewMode) -> Unit,
    onToggleSessionTimer: () -> Unit,
    onResetSessionTimer: () -> Unit,
    onViewLastFtpResults: () -> Unit = {},
    onZoneCountChange: (Int) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var ftpMenuExpanded by remember { mutableStateOf(false) }

    val hasLastFtpResults = uiState.ftpTest.lastCompletedResults != null

    val configuration = LocalConfiguration.current
    val isTabletOrProjector = configuration.smallestScreenWidthDp >= 600
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isPhoneLandscape = !isTabletOrProjector && isLandscape

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Color(0xFF000000))
            .statusBarsPadding()
            .border(1.dp, Color(0xFF1E2230))
            .padding(horizontal = 10.dp, vertical = if (isPhoneLandscape) 4.dp else 6.dp),
        verticalArrangement = Arrangement.spacedBy(if (isPhoneLandscape) 4.dp else 6.dp)
    ) {
        if (isPhoneLandscape) {
            // PHONE LANDSCAPE: 2 sleek, unhurried rows
            // Row 1: Brand (left) | Dedicated Session Timer (center) | Controls: Demo, Scan, FTP (right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Brand
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(AccentEnergy),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                            contentDescription = "LiveFleet Studio",
                            tint = BgMain,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "LIVEFLEET ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = TextMain
                        )
                        Text(
                            text = "STUDIO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = AccentEnergy
                        )
                    }
                }

                // Center: Dedicated Session Timer
                DedicatedSessionTimerRow(
                    seconds = uiState.sessionElapsedSeconds,
                    isRunning = uiState.isSessionTimerRunning,
                    onToggle = onToggleSessionTimer,
                    onReset = onResetSessionTimer,
                    isCompact = true,
                    modifier = Modifier.wrapContentWidth().height(26.dp)
                )

                // Controls: FTP, Results, Settings (Demo and Scan live in Empty State & Settings)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Discreet exit badge if Demo Mode is currently active
                    if (uiState.isDemoMode) {
                        Surface(
                            color = AccentSuccess.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentSuccess.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .height(22.dp)
                                .clickable { onDemoToggle(false) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "DEMO ✕",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = AccentSuccess
                                )
                            }
                        }
                    }

                    // If completed FTP results exist in this session, show quick access button
                    if (hasLastFtpResults) {
                        Button(
                            onClick = onViewLastFtpResults,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E1400),
                                contentColor = AccentEnergy
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentEnergy),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 1.dp),
                            modifier = Modifier.height(22.dp)
                        ) {
                            Text(
                                text = "📊 Results",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Box {
                        Button(
                            onClick = { ftpMenuExpanded = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentEnergy,
                                contentColor = BgMain
                            ),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 1.dp),
                            modifier = Modifier.height(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "FTP",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        FtpDropdown(
                            expanded = ftpMenuExpanded,
                            hasLastResults = hasLastFtpResults,
                            onViewLastResults = onViewLastFtpResults,
                            onDismiss = { ftpMenuExpanded = false },
                            onStartTest = onStartFtpTest
                        )
                    }

                    // Settings Button (Fleet Protocol & Preferences)
                    Surface(
                        color = Color(0xFF161B26),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { onOpenSettings() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Studio Settings",
                                tint = TextMuted,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            // Row 2: Left: View Tabs [Gauges | Table] & Zone System [5Z | 7Z]
            //        Right: Studio Stats [BIKES 12 • AVG 213 W • RPM 82] & Sort Dropdown
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: View Mode Tabs
                ViewModeSelector(
                    currentMode = uiState.viewMode,
                    onModeSelect = onViewModeChange,
                    isCompact = true
                )

                // Right: Studio Stats & Sort
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF11141E), RoundedCornerShape(4.dp))
                            .border(1.dp, Color(0xFF262D42), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CompactStatItem("BIKES", "${uiState.activeBikeCount}")
                        Text("•", color = TextDim, fontSize = 8.sp, maxLines = 1, softWrap = false)
                        CompactStatItem("AVG", "${uiState.averagePower}", "W")
                        Text("•", color = TextDim, fontSize = 8.sp, maxLines = 1, softWrap = false)
                        CompactStatItem("RPM", "${uiState.averageCadence}")
                    }

                    // Sort Dropdown
                    Box {
                        Surface(
                            color = BgCard,
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                            modifier = Modifier
                                .height(22.dp)
                                .clickable { sortMenuExpanded = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 5.dp)
                            ) {
                                Text(
                                    text = "Sort: ${uiState.sortOrder.displayName.substringBefore(" ")}",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMain,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                        SortDropdown(
                            expanded = sortMenuExpanded,
                            onDismiss = { sortMenuExpanded = false },
                            onSelect = onSortChange
                        )
                    }
                }
            }
        } else {
            // PORTRAIT & TABLET: 3 structured lines preventing any overcrowding
            
            // LINE 1: Brand & Studio Controls (Demo, Scan, FTP)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Brand Logo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(AccentEnergy),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                            contentDescription = "LiveFleet Studio",
                            tint = BgMain,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "LIVEFLEET ",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = TextMain
                            )
                            Text(
                                text = "STUDIO",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = AccentEnergy
                            )
                        }
                        Text(
                            text = "MULTI-ERG TELEMETRY",
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDim,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Controls: FTP, Results, Settings (Demo and Scan live in Empty State & Settings)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Discreet exit badge if Demo Mode is currently active
                    if (uiState.isDemoMode) {
                        Surface(
                            color = AccentSuccess.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentSuccess.copy(alpha = 0.5f)),
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
                                    color = AccentSuccess
                                )
                            }
                        }
                    }

                    // If completed FTP results exist in this session, show quick access button
                    if (hasLastFtpResults) {
                        Button(
                            onClick = onViewLastFtpResults,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E1400),
                                contentColor = AccentEnergy
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentEnergy),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text(
                                text = "📊 Results",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // FTP Test Button
                    Box {
                        Button(
                            onClick = { ftpMenuExpanded = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentEnergy,
                                contentColor = BgMain
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "FTP TEST",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        FtpDropdown(
                            expanded = ftpMenuExpanded,
                            hasLastResults = hasLastFtpResults,
                            onViewLastResults = onViewLastFtpResults,
                            onDismiss = { ftpMenuExpanded = false },
                            onStartTest = onStartFtpTest
                        )
                    }

                    // Settings Button (Fleet Protocol & Preferences)
                    Surface(
                        color = Color(0xFF161B26),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { onOpenSettings() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Studio Settings",
                                tint = TextMuted,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            // LINE 2: DEDICATED TIMER LINE ROW (Eliminates overcrowding, prominent start/pause/reset)
            DedicatedSessionTimerRow(
                seconds = uiState.sessionElapsedSeconds,
                isRunning = uiState.isSessionTimerRunning,
                onToggle = onToggleSessionTimer,
                onReset = onResetSessionTimer,
                isCompact = false,
                modifier = Modifier.fillMaxWidth()
            )

            if (!isTabletOrProjector) {
                // PHONE PORTRAIT: LINE 3 = Tabs & Sort, LINE 4 = Full-Width Class Averages (Never cramped, RPM 100% visible)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // View Mode Tabs [ Gauges | Table ]
                        ViewModeSelector(
                            currentMode = uiState.viewMode,
                            onModeSelect = onViewModeChange,
                            isCompact = true
                        )
                    }

                    // Sort Dropdown
                    Box {
                        Surface(
                            color = BgCard,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                            modifier = Modifier
                                .height(26.dp)
                                .clickable { sortMenuExpanded = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            ) {
                                Text(
                                    text = "Sort: ${uiState.sortOrder.displayName.substringBefore(" ")}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMain,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                        SortDropdown(
                            expanded = sortMenuExpanded,
                            onDismiss = { sortMenuExpanded = false },
                            onSelect = onSortChange
                        )
                    }
                }

                // LINE 4: Full-Width Studio Class Averages Bar (Spans full phone width with zero text wrapping)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0C0F17), RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0xFF1E2230), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactStatItem("BIKES", "${uiState.activeBikeCount}")
                    Text("•", color = TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    CompactStatItem("AVG POWER", "${uiState.averagePower}", "W")
                    Text("•", color = TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    CompactStatItem("AVG CADENCE", "${uiState.averageCadence}", "RPM")
                }
            } else {
                // TABLET / PROJECTOR: Wide screen (600dp+), keep all on Line 3 comfortably
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ViewModeSelector(
                            currentMode = uiState.viewMode,
                            onModeSelect = onViewModeChange,
                            isCompact = false
                        )
                    }

                    // Studio Class Averages
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF11141E), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFF262D42), RoundedCornerShape(6.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CompactStatItem("BIKES", "${uiState.activeBikeCount}")
                        Text("•", color = TextDim, fontSize = 9.sp, maxLines = 1, softWrap = false)
                        CompactStatItem("AVG POWER", "${uiState.averagePower}", "W")
                        Text("•", color = TextDim, fontSize = 9.sp, maxLines = 1, softWrap = false)
                        CompactStatItem("AVG CADENCE", "${uiState.averageCadence}", "RPM")
                    }

                    // Sort Dropdown
                    Box {
                        Surface(
                            color = BgCard,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                            modifier = Modifier
                                .height(26.dp)
                                .clickable { sortMenuExpanded = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = "Sort: ${uiState.sortOrder.displayName.substringBefore(" ")}",
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
                        SortDropdown(
                            expanded = sortMenuExpanded,
                            onDismiss = { sortMenuExpanded = false },
                            onSelect = onSortChange
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dedicated line row for the Class Session Timer.
 * Does NOT start automatically; user taps "Start" when ready!
 */
@Composable
private fun DedicatedSessionTimerRow(
    seconds: Long,
    isRunning: Boolean,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    isCompact: Boolean,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    val timeFormatted = if (hrs > 0) {
        String.format("%02d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format("%02d:%02d", mins, secs)
    }

    Row(
        modifier = modifier
            .background(Color(0xFF0C0F17), RoundedCornerShape(6.dp))
            .border(
                1.dp,
                if (isRunning) AccentSuccess.copy(alpha = 0.5f) else Color(0xFF262D42),
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = if (isCompact) 8.dp else 10.dp, vertical = if (isCompact) 2.dp else 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Timer Label & Icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = if (isRunning) AccentSuccess else AccentEnergy,
                modifier = Modifier.size(if (isCompact) 13.dp else 16.dp)
            )
            Text(
                text = "SESSION TIMER",
                fontSize = if (isCompact) 8.sp else 9.sp,
                fontWeight = FontWeight.Black,
                color = if (isRunning) AccentSuccess else TextDim,
                letterSpacing = 0.5.sp
            )
        }

        // Center: Big 7-Segment Digital Clock
        Text(
            text = timeFormatted,
            fontFamily = Dseg7FontFamily,
            fontSize = if (isCompact) 13.sp else 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (isRunning) Color.White else AccentEnergy,
            letterSpacing = 1.sp
        )

        // Right: Start / Pause & Reset Actions
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Start / Pause Button
            Button(
                onClick = onToggle,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) AccentEnergy else AccentSuccess,
                    contentColor = if (isRunning) BgMain else Color.White
                ),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 1.dp),
                modifier = Modifier.height(24.dp)
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = if (isRunning) "Pause" else "Start",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
            }

            // Reset Button (available when paused or seconds > 0)
            if (seconds > 0) {
                Surface(
                    color = Color(0xFF1E2638),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .height(24.dp)
                        .clickable { onReset() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Timer",
                            tint = TextMuted,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "Reset",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}

/**
 * View selector with only [Gauges | Table] (Target removed per user request).
 */
@Composable
private fun ViewModeSelector(
    currentMode: LeaderboardViewMode,
    onModeSelect: (LeaderboardViewMode) -> Unit,
    isCompact: Boolean
) {
    Row(
        modifier = Modifier
            .background(Color(0xFF11141E), RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFF262D42), RoundedCornerShape(6.dp))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LeaderboardViewMode.values().forEach { mode ->
            val isSelected = mode == currentMode
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isSelected) AccentPrimary else Color.Transparent)
                    .clickable { onModeSelect(mode) }
                    .padding(horizontal = if (isCompact) 6.dp else 10.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mode.displayName,
                    fontSize = if (isCompact) 9.sp else 10.sp,
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                    color = if (isSelected) Color.White else TextMuted
                )
            }
        }
    }
}

@Composable
private fun CompactStatItem(label: String, value: String, unit: String = "") {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = label,
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            color = TextDim,
            maxLines = 1,
            softWrap = false
        )
        Text(
            text = value,
            fontFamily = Dseg7FontFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = TextMain,
            maxLines = 1,
            softWrap = false
        )
        if (unit.isNotEmpty()) {
            Text(
                text = unit,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                maxLines = 1,
                softWrap = false
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
        SortOrder.values().forEach { order ->
            DropdownMenuItem(
                text = { Text(order.displayName) },
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
    hasLastResults: Boolean = false,
    onViewLastResults: () -> Unit = {},
    onDismiss: () -> Unit,
    onStartTest: (minutes: Int, factor: Float) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        if (hasLastResults) {
            DropdownMenuItem(
                text = {
                    Column {
                        Text("📊 View Last FTP Results", fontWeight = FontWeight.Black, color = AccentEnergy)
                        Text("Review completed class test", fontSize = 11.sp, color = TextMuted)
                    }
                },
                onClick = {
                    onViewLastResults()
                    onDismiss()
                }
            )
            HorizontalDivider(color = BorderColor)
        }
        DropdownMenuItem(
            text = {
                Column {
                    Text("5-Minute FTP Test", fontWeight = FontWeight.Bold)
                    Text("0.85x Factor • 80-90 RPM", fontSize = 11.sp, color = TextMuted)
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
                    Text("0.95x Factor • 80-90 RPM", fontSize = 11.sp, color = TextMuted)
                }
            },
            onClick = {
                onStartTest(20, 0.95f)
                onDismiss()
            }
        )
    }
}
