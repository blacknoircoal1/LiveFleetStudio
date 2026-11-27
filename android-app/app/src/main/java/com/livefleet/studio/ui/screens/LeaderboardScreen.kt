package com.livefleet.studio.ui.screens

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livefleet.studio.model.BikeData
import com.livefleet.studio.model.BikeProtocol
import com.livefleet.studio.model.BrandType
import com.livefleet.studio.model.Concept2ErgFilter
import com.livefleet.studio.model.Concept2MetricMode
import com.livefleet.studio.model.LeaderboardViewMode
import com.livefleet.studio.model.SortOrder
import com.livefleet.studio.model.TileHeroMetric
import com.livefleet.studio.ui.components.*
import com.livefleet.studio.ui.theme.*
import com.livefleet.studio.viewmodel.LiveFleetUiState

@Composable
fun LeaderboardScreen(
    uiState: LiveFleetUiState,
    onSortChange: (SortOrder) -> Unit,
    onDemoToggle: (Boolean) -> Unit,
    onDemoBikeCountChange: (Int) -> Unit = {},
    onBleToggle: () -> Unit,
    onStartFtpTest: (minutes: Int, factor: Float) -> Unit,
    onFinishFtpEarly: () -> Unit,
    onCancelFtpTest: () -> Unit,
    onDismissFtpResults: () -> Unit,
    onViewLastFtpResults: () -> Unit = {},
    onViewModeChange: (LeaderboardViewMode) -> Unit,
    onZoneCountChange: (Int) -> Unit = {},
    onUpdateRiderProfile: (bikeId: Int, name: String, ftp: Int) -> Unit = { _, _, _ -> },
    onSetInstructorBike: (bike: BikeData, isInstructor: Boolean) -> Unit = { _, _ -> },
    onLockMachineBikeNumber: (macAddress: String, currentBikeId: Int, studioBikeNumber: Int) -> Unit = { _, _, _ -> },
    onUnlockMachine: (macAddress: String, currentBikeId: Int) -> Unit = { _, _ -> },
    onToggleStudioLockProximity: (Boolean) -> Unit = {},
    onConcept2MetricModeChange: (Concept2MetricMode) -> Unit = {},
    onConcept2ErgFilterChange: (Concept2ErgFilter) -> Unit = {},
    onTileHeroMetricChange: (TileHeroMetric) -> Unit = {},
    onDefaultFtpChange: (Int) -> Unit = {},
    onToggleSessionTimer: () -> Unit,
    onEndWorkout: () -> Unit,
    onStopwatchClick: () -> Unit = {},
    onStartOrNextInterval: () -> Unit = {},
    onStopInterval: () -> Unit = {},
    onFinishWorkoutSummary: () -> Unit = {},
    onReopenSummary: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onCloseSettings: () -> Unit = {},
    onSelectProtocol: (BikeProtocol) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    var editingBikeId by remember { mutableStateOf<Int?>(null) }

    // 1. FULL-SCREEN 5-SECOND COUNTDOWN OVERLAY
    if (uiState.ftpTest.isPreparing) {
        BackHandler { onCancelFtpTest() }
        FullScreenCountdownOverlay(
            countdownSeconds = uiState.ftpTest.prepCountdownSeconds,
            testType = uiState.ftpTest.testType,
            scalingFactor = uiState.ftpTest.scalingFactor,
            onCancel = onCancelFtpTest,
            modifier = modifier
        )
        return
    }

    // 2. FULL-SCREEN UNIFIED WORKOUT SUMMARY SCREEN (SESSION + INTERVALS + FTP TEST)
    if (uiState.intervalState.isViewingSummary) {
        BackHandler { onFinishWorkoutSummary() }
        IntervalSummaryScreen(
            intervalState = uiState.intervalState,
            ftpResults = uiState.ftpTest.lastCompletedResults,
            ftpTestType = uiState.ftpTest.testType,
            ftpScalingFactor = uiState.ftpTest.scalingFactor,
            onDone = onFinishWorkoutSummary,
            modifier = modifier
        )
        return
    }

    // 4. FULL-PAGE STUDIO SETTINGS SCREEN (FOLDER-BASED SUB-SETTINGS)
    if (uiState.isSettingsOpen) {
        SettingsScreen(
            uiState = uiState,
            onCloseSettings = onCloseSettings,
            onSelectProtocol = onSelectProtocol,
            onZoneCountChange = onZoneCountChange,
            onDefaultFtpChange = onDefaultFtpChange,
            onViewModeChange = onViewModeChange,
            onBleToggle = onBleToggle,
            onDemoToggle = onDemoToggle,
            onDemoBikeCountChange = onDemoBikeCountChange,
            onToggleStudioLockProximity = onToggleStudioLockProximity,
            onConcept2MetricModeChange = onConcept2MetricModeChange,
            onConcept2ErgFilterChange = onConcept2ErgFilterChange,
            onTileHeroMetricChange = onTileHeroMetricChange,
            modifier = modifier
        )
        return
    }

    // 4. MAIN STUDIO LEADERBOARD SCREEN
    Scaffold(
        topBar = {
            TopBar(
                uiState = uiState,
                onSortChange = onSortChange,
                onDemoToggle = onDemoToggle,
                onBleToggle = onBleToggle,
                onStartFtpTest = onStartFtpTest,
                onViewLastFtpResults = onViewLastFtpResults,
                onViewModeChange = onViewModeChange,
                onZoneCountChange = onZoneCountChange,
                onConcept2ErgFilterChange = onConcept2ErgFilterChange,
                onOpenSettings = onOpenSettings,
                onToggleSessionTimer = onToggleSessionTimer,
                onEndWorkout = onEndWorkout,
                onReopenSummary = onReopenSummary,
                onStopwatchClick = onStopwatchClick
            )
        },
        containerColor = Color(0xFF000000),
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .background(Color(0xFF000000))
        ) {
            // Collapsible Freestyle Interval Strip
            if (uiState.intervalState.isStripExpanded) {
                IntervalActiveBanner(
                    intervalState = uiState.intervalState,
                    onNextLap = onStartOrNextInterval,
                    onStopInterval = onStopInterval
                )
            }

            // Group FTP Test Active Banner (Only during the actual test, after 5s prep)
            if (uiState.ftpTest.isActive && !uiState.ftpTest.isPreparing) {
                FtpActiveBanner(
                    ftpTest = uiState.ftpTest,
                    sessionElapsedSeconds = uiState.sessionElapsedSeconds,
                    onFinishEarly = onFinishFtpEarly,
                    onCancel = onCancelFtpTest
                )
            }

            // Main Content Area based on selected ViewMode
            if (uiState.bikes.isEmpty()) {
                EmptyStateView(
                    protocol = uiState.selectedProtocol,
                    isScanning = uiState.isScanning,
                    onToggleScan = onBleToggle
                )
            } else {
                when (uiState.viewMode) {
                    LeaderboardViewMode.GAUGES -> {
                        // Main View: Borderless Floating Dials matching Top-Left quadrant of Ride by Color
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            val count = uiState.bikes.size
                            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                            val (columns, rows) = calculateOptimalGrid(count, isLandscape)
                            val isCompact = rows >= 4 || (isLandscape && rows >= 3) || count >= 12
                            val spacing = 2.dp
                            val tileWidth = (maxWidth - spacing * (columns - 1)) / columns

                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(spacing)
                            ) {
                                for (r in 0 until rows) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterHorizontally)
                                    ) {
                                        for (c in 0 until columns) {
                                            val index = r * columns + c
                                            if (index < count) {
                                                val bike = uiState.bikes[index]
                                                Box(
                                                    modifier = Modifier
                                                        .width(tileWidth)
                                                        .fillMaxHeight()
                                                        .then(
                                                            if (bike.isInstructorBike) {
                                                                Modifier
                                                                    .background(
                                                                        AccentEnergy.copy(alpha = 0.08f),
                                                                        RoundedCornerShape(8.dp)
                                                                    )
                                                                    .border(
                                                                        1.5.dp,
                                                                        AccentEnergy.copy(alpha = 0.85f),
                                                                        RoundedCornerShape(8.dp)
                                                                    )
                                                            } else {
                                                                Modifier
                                                            }
                                                        )
                                                        .clickable { editingBikeId = bike.bikeId }
                                                ) {
                                                    when (bike.brandType) {
                                                        BrandType.STAGES -> {
                                                            StagesSc3Console(
                                                                bike = bike,
                                                                isFtpTestActive = uiState.ftpTest.isActive && !uiState.ftpTest.isPreparing,
                                                                isCompact = isCompact,
                                                                zoneCount = uiState.zoneCount,
                                                                sessionElapsedSeconds = uiState.sessionElapsedSeconds,
                                                                heroMetric = uiState.tileHeroMetric,
                                                                modifier = Modifier.fillMaxSize()
                                                            )
                                                        }
                                                        BrandType.CONCEPT2 -> {
                                                            Concept2Pm5Console(
                                                                bike = bike,
                                                                isFtpTestActive = uiState.ftpTest.isActive && !uiState.ftpTest.isPreparing,
                                                                isCompact = isCompact,
                                                                zoneCount = uiState.zoneCount,
                                                                sessionElapsedSeconds = uiState.sessionElapsedSeconds,
                                                                concept2MetricMode = uiState.concept2MetricMode,
                                                                heroMetric = uiState.tileHeroMetric,
                                                                modifier = Modifier.fillMaxSize()
                                                            )
                                                        }
                                                        BrandType.SCHWINN -> {
                                                            SchwinnEchelon2Console(
                                                                bike = bike,
                                                                isFtpTestActive = uiState.ftpTest.isActive && !uiState.ftpTest.isPreparing,
                                                                isCompact = isCompact,
                                                                zoneCount = uiState.zoneCount,
                                                                sessionElapsedSeconds = uiState.sessionElapsedSeconds,
                                                                heroMetric = uiState.tileHeroMetric,
                                                                modifier = Modifier.fillMaxSize()
                                                            )
                                                        }
                                                        BrandType.WATTBIKE -> {
                                                            WattbikeAirProConsole(
                                                                bike = bike,
                                                                isFtpTestActive = uiState.ftpTest.isActive && !uiState.ftpTest.isPreparing,
                                                                isCompact = isCompact,
                                                                zoneCount = uiState.zoneCount,
                                                                sessionElapsedSeconds = uiState.sessionElapsedSeconds,
                                                                displayMode = uiState.concept2MetricMode,
                                                                heroMetric = uiState.tileHeroMetric,
                                                                modifier = Modifier.fillMaxSize()
                                                            )
                                                        }
                                                        BrandType.FTMS, BrandType.POWER_METER -> {
                                                            PowerMeterHeadUnitConsole(
                                                                bike = bike,
                                                                isFtpTestActive = uiState.ftpTest.isActive && !uiState.ftpTest.isPreparing,
                                                                isCompact = isCompact,
                                                                zoneCount = uiState.zoneCount,
                                                                sessionElapsedSeconds = uiState.sessionElapsedSeconds,
                                                                heroMetric = uiState.tileHeroMetric,
                                                                modifier = Modifier.fillMaxSize()
                                                            )
                                                        }
                                                        BrandType.KEISER -> {
                                                            FloatingBikeDial(
                                                                bike = bike,
                                                                isFtpTestActive = uiState.ftpTest.isActive && !uiState.ftpTest.isPreparing,
                                                                isCompact = isCompact,
                                                                zoneCount = uiState.zoneCount,
                                                                heroMetric = uiState.tileHeroMetric,
                                                                modifier = Modifier.fillMaxSize()
                                                            )
                                                        }
                                                    }

                                                    if (bike.isInstructorBike) {
                                                        Surface(
                                                            color = AccentEnergy,
                                                            shape = RoundedCornerShape(4.dp),
                                                            modifier = Modifier
                                                                .align(Alignment.TopCenter)
                                                                .padding(top = 1.dp)
                                                        ) {
                                                            Text(
                                                                text = "★ INSTRUCTOR",
                                                                fontSize = if (isCompact) 7.sp else 8.5.sp,
                                                                fontWeight = FontWeight.Black,
                                                                color = BgMain,
                                                                maxLines = 1,
                                                                softWrap = false,
                                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    LeaderboardViewMode.TABLE -> {
                        // Table View: Dense Leaderboard matching Top-Right quadrant of Ride by Color
                        LeaderboardTableView(
                            bikes = uiState.bikes,
                            isFtpTestActive = uiState.ftpTest.isActive && !uiState.ftpTest.isPreparing,
                            concept2MetricMode = uiState.concept2MetricMode,
                            onBikeClick = { bike -> editingBikeId = bike.bikeId }
                        )
                    }
                }
            }
        }
    }

    // Tap-to-edit Rider Name, FTP calibration & Studio Bike # whitelist lock dialog
    val editingBike = editingBikeId?.let { id -> uiState.bikes.find { it.bikeId == id } }
    if (editingBike != null) {
        RiderProfileEditDialog(
            bike = editingBike,
            zoneCount = uiState.zoneCount,
            onDismiss = { editingBikeId = null },
            onSave = { bikeId, name, ftp ->
                onUpdateRiderProfile(bikeId, name, ftp)
            },
            onSetInstructorBike = onSetInstructorBike,
            onLockMachineBikeNumber = onLockMachineBikeNumber,
            onUnlockMachine = onUnlockMachine
        )
    }
}

/**
 * Calculates (columns, rows) such that all [count] bikes fit in view on a single screen without scrolling.
 */
private fun calculateOptimalGrid(count: Int, isLandscape: Boolean): Pair<Int, Int> {
    if (count <= 1) return Pair(1, 1)
    return if (isLandscape) {
        when {
            count <= 2 -> Pair(2, 1)
            count <= 4 -> Pair(4, 1)
            count <= 6 -> Pair(3, 2)
            count <= 8 -> Pair(4, 2)
            count <= 12 -> Pair(6, 2)
            count <= 16 -> Pair(6, 3)
            count <= 20 -> Pair(5, 4)
            else -> Pair(6, (count + 5) / 6)
        }
    } else {
        when {
            count <= 2 -> Pair(1, 2)
            count <= 4 -> Pair(2, 2)
            count <= 6 -> Pair(2, 3)
            count <= 8 -> Pair(2, 4)
            count <= 12 -> Pair(3, 4)
            count <= 16 -> Pair(4, 4)
            count <= 20 -> Pair(4, 5)
            else -> Pair(4, (count + 3) / 4)
        }
    }
}

@Composable
private fun EmptyStateView(
    protocol: BikeProtocol,
    isScanning: Boolean,
    onToggleScan: () -> Unit
) {
    val info = getProtocolEmptyInfo(protocol)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.dp)
        ) {
            Text(
                text = info.icon,
                fontSize = 44.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = (if (isScanning) AccentSuccess else AccentEnergy).copy(alpha = 0.15f),
                border = BorderStroke(1.dp, (if (isScanning) AccentSuccess else AccentEnergy).copy(alpha = 0.45f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isScanning) AccentSuccess else AccentEnergy)
                    )
                    Text(
                        text = if (isScanning) "SCANNING: ${info.brandPill}" else info.brandPill,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isScanning) AccentSuccess else AccentEnergy,
                        letterSpacing = 0.8.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Text(
                text = if (isScanning) info.waitingTitle else info.idleTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = TextMain,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isScanning) info.scanDescription else info.idleDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onToggleScan,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isScanning) AccentDanger else AccentPrimary
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
            ) {
                Text(
                    text = if (isScanning) "Stop Scan" else "Start Scan",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

private data class ProtocolEmptyInfo(
    val icon: String,
    val brandPill: String,
    val waitingTitle: String,
    val idleTitle: String,
    val scanDescription: String,
    val idleDescription: String
)

private fun getProtocolEmptyInfo(protocol: BikeProtocol): ProtocolEmptyInfo {
    return when (protocol) {
        BikeProtocol.AUTO_DETECT -> ProtocolEmptyInfo(
            icon = "🚴‍♂️",
            brandPill = "ALL FLEET BRANDS",
            waitingTitle = "Waiting for Fleet Equipment...",
            idleTitle = "No Fleet Equipment Detected",
            scanDescription = "Actively listening for all supported brands (Keiser, Stages, Schwinn, Concept2, Wattbike/FTMS, Power Pedals). Start pedaling or rowing to transmit live telemetry.",
            idleDescription = "Tap 'Start Scan' to connect to any supported indoor bike, erg, or power meter in range."
        )
        BikeProtocol.KEISER -> ProtocolEmptyInfo(
            icon = "🚴‍♂️",
            brandPill = "KEISER M3i FLEET",
            waitingTitle = "Waiting for Keiser M3i Broadcasts...",
            idleTitle = "No Keiser M3i Bikes Detected",
            scanDescription = "Actively scanning for passive Keiser 0x0102 broadcasts. Pedal the bike to wake its computer and transmit live telemetry.",
            idleDescription = "Tap 'Start Scan' to passively listen for Keiser M3i bikes in range — no pairing required."
        )
        BikeProtocol.STAGES -> ProtocolEmptyInfo(
            icon = "🚴‍♂️",
            brandPill = "STAGES SC3 FLEET",
            waitingTitle = "Waiting for Stages SC3 Bikes...",
            idleTitle = "No Stages SC3 Bikes Detected",
            scanDescription = "Actively listening for Stages BLE Cycling Power (0x1818) and USB ANT+ broadcasts. Spin the cranks to wake the power meter.",
            idleDescription = "Tap 'Start Scan' to connect to Stages SC3 / SC2 bikes via Bluetooth (1–6) or USB ANT+ (30+)."
        )
        BikeProtocol.SCHWINN_4IIII -> ProtocolEmptyInfo(
            icon = "🚴‍♂️",
            brandPill = "SCHWINN CARBON BLUE • 4iiii",
            waitingTitle = "Waiting for Schwinn / 4iiii Bikes...",
            idleTitle = "No Schwinn Carbon Blue Bikes Detected",
            scanDescription = "Actively listening for 4iiii BLE Cycling Power (0x1818) and USB ANT+ (MPower Echelon2 & 4iiii). Spin the cranks to wake the meter.",
            idleDescription = "Tap 'Start Scan' to connect to Schwinn 4iiii cranks via Bluetooth or MPower Echelon2 consoles via USB ANT+."
        )
        BikeProtocol.CONCEPT2 -> ProtocolEmptyInfo(
            icon = "🚴‍♂️",
            brandPill = "CONCEPT2 BIKEERG • PM5",
            waitingTitle = "Waiting for Concept2 PM5 Monitors...",
            idleTitle = "No Concept2 PM5 Monitors Detected",
            scanDescription = "Actively listening for Concept2 PM5 BLE GATT ('Connect -> Ready for App Connection') and USB ANT+ Group Class beacons.",
            idleDescription = "Tap 'Start Scan' to connect to Concept2 BikeErg, RowErg, or SkiErg PM5 monitors."
        )
        BikeProtocol.FTMS_UNIVERSAL -> ProtocolEmptyInfo(
            icon = "🚴‍♂️",
            brandPill = "WATTBIKE & FTMS FLEET",
            waitingTitle = "Waiting for Wattbike / FTMS Bikes...",
            idleTitle = "No Wattbike or FTMS Bikes Detected",
            scanDescription = "Actively listening for FTMS (UUID 0x1826) and USB ANT+ broadcasts (Wattbike Air-Pro/Atom, Life Fitness IC5–8, Technogym, Matrix).",
            idleDescription = "Tap 'Start Scan' to connect to Wattbike and standard Bluetooth FTMS smart bikes."
        )
        BikeProtocol.POWER_METERS -> ProtocolEmptyInfo(
            icon = "⚡",
            brandPill = "POWER PEDALS & CRANKS",
            waitingTitle = "Waiting for Power Meters...",
            idleTitle = "No Cycling Power Meters Detected",
            scanDescription = "Actively listening for Bluetooth Cycling Power (UUID 0x1818) and USB ANT+ Bike Power (Assioma, Garmin Rally, Quarq, SRM).",
            idleDescription = "Tap 'Start Scan' to connect to standard cycling power pedals and crank meters."
        )
    }
}
