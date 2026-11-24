package com.keiser.studio.ui.screens

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keiser.studio.AppConfig
import com.keiser.studio.model.BikeData
import com.keiser.studio.model.BikeProtocol
import com.keiser.studio.model.LeaderboardViewMode
import com.keiser.studio.model.SortOrder
import com.keiser.studio.ui.components.*
import com.keiser.studio.ui.theme.*
import com.keiser.studio.viewmodel.KeiserUiState

@Composable
fun LeaderboardScreen(
    uiState: KeiserUiState,
    onSortChange: (SortOrder) -> Unit,
    onDemoToggle: (Boolean) -> Unit,
    onBleToggle: () -> Unit,
    onStartFtpTest: (minutes: Int, factor: Float) -> Unit,
    onFinishFtpEarly: () -> Unit,
    onCancelFtpTest: () -> Unit,
    onDismissFtpResults: () -> Unit,
    onViewLastFtpResults: () -> Unit = {},
    onViewModeChange: (LeaderboardViewMode) -> Unit,
    onZoneCountChange: (Int) -> Unit = {},
    onToggleSessionTimer: () -> Unit,
    onResetSessionTimer: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onCloseSettings: () -> Unit = {},
    onSelectProtocol: (BikeProtocol) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current

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

    // 2. FULL-SCREEN FTP RESULTS SCREEN (ALL BIKES VISIBLE WITHOUT SCROLLING, RE-OPENABLE ANYTIME)
    val shouldShowResults = uiState.ftpTest.isViewingResults &&
        (uiState.ftpTest.pendingResults != null || uiState.ftpTest.lastCompletedResults != null)
    if (shouldShowResults) {
        BackHandler { onDismissFtpResults() }
        val results = uiState.ftpTest.pendingResults ?: uiState.ftpTest.lastCompletedResults ?: emptyList()
        FtpResultsScreen(
            results = results,
            testType = uiState.ftpTest.testType,
            scalingFactor = uiState.ftpTest.scalingFactor,
            onDone = onDismissFtpResults,
            modifier = modifier
        )
        return
    }

    // 3. FULL-PAGE STUDIO SETTINGS SCREEN (FOLDER-BASED SUB-SETTINGS)
    if (uiState.isSettingsOpen) {
        SettingsScreen(
            uiState = uiState,
            onCloseSettings = onCloseSettings,
            onSelectProtocol = onSelectProtocol,
            onZoneCountChange = onZoneCountChange,
            onViewModeChange = onViewModeChange,
            onBleToggle = onBleToggle,
            onDemoToggle = onDemoToggle,
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
                onOpenSettings = onOpenSettings,
                onToggleSessionTimer = onToggleSessionTimer,
                onResetSessionTimer = onResetSessionTimer
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF000000))
                    .navigationBarsPadding()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = AppConfig.FOOTER_CREDIT,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF475569)
                )
            }
        },
        containerColor = Color(0xFF000000),
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF000000))
        ) {
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
                    onEnableDemo = { onDemoToggle(true) },
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

                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                for (r in 0 until rows) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        for (c in 0 until columns) {
                                            val index = r * columns + c
                                            if (index < count) {
                                                val bike = uiState.bikes[index]
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                ) {
                                                    val isStages = uiState.selectedProtocol == BikeProtocol.STAGES ||
                                                        (uiState.selectedProtocol == BikeProtocol.AUTO_DETECT && bike.brand.equals("Stages", ignoreCase = true))
                                                    val isConcept2 = uiState.selectedProtocol == BikeProtocol.CONCEPT2 ||
                                                        (uiState.selectedProtocol == BikeProtocol.AUTO_DETECT && bike.brand.equals("Concept2", ignoreCase = true))
                                                    val isSchwinn = uiState.selectedProtocol == BikeProtocol.SCHWINN_4IIII ||
                                                        (uiState.selectedProtocol == BikeProtocol.AUTO_DETECT && bike.brand.equals("Schwinn", ignoreCase = true))

                                                    when {
                                                        isStages -> {
                                                            StagesSc3Console(
                                                                bike = bike,
                                                                isFtpTestActive = uiState.ftpTest.isActive && !uiState.ftpTest.isPreparing,
                                                                isCompact = isCompact,
                                                                zoneCount = uiState.zoneCount,
                                                                sessionElapsedSeconds = uiState.sessionElapsedSeconds,
                                                                modifier = Modifier.fillMaxSize()
                                                            )
                                                        }
                                                        isConcept2 -> {
                                                            Concept2Pm5Console(
                                                                bike = bike,
                                                                isFtpTestActive = uiState.ftpTest.isActive && !uiState.ftpTest.isPreparing,
                                                                isCompact = isCompact,
                                                                zoneCount = uiState.zoneCount,
                                                                sessionElapsedSeconds = uiState.sessionElapsedSeconds,
                                                                modifier = Modifier.fillMaxSize()
                                                            )
                                                        }
                                                        isSchwinn -> {
                                                            SchwinnEchelon2Console(
                                                                bike = bike,
                                                                isFtpTestActive = uiState.ftpTest.isActive && !uiState.ftpTest.isPreparing,
                                                                isCompact = isCompact,
                                                                zoneCount = uiState.zoneCount,
                                                                sessionElapsedSeconds = uiState.sessionElapsedSeconds,
                                                                modifier = Modifier.fillMaxSize()
                                                            )
                                                        }
                                                        else -> {
                                                            FloatingBikeDial(
                                                                bike = bike,
                                                                isFtpTestActive = uiState.ftpTest.isActive && !uiState.ftpTest.isPreparing,
                                                                isCompact = isCompact,
                                                                zoneCount = uiState.zoneCount,
                                                                modifier = Modifier.fillMaxSize()
                                                            )
                                                        }
                                                    }
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.weight(1f))
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
                            isFtpTestActive = uiState.ftpTest.isActive && !uiState.ftpTest.isPreparing
                        )
                    }
                }
            }
        }
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
    onEnableDemo: () -> Unit,
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

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onToggleScan,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isScanning) AccentDanger else AccentPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isScanning) "Stop Scan" else "Start Scan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                OutlinedButton(
                    onClick = onEnableDemo,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMain),
                    border = BorderStroke(1.dp, BorderColor),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Enable Demo Mode",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
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
            waitingTitle = "Waiting for Fleet Broadcasts...",
            idleTitle = "No Fleet Equipment Detected",
            scanDescription = "Actively monitoring for all supported brands (Keiser, Stages, Schwinn/4iiii, Concept2, FTMS). Start pedaling or rowing to transmit live telemetry.",
            idleDescription = "Tap 'Start Scan' to passively listen for any supported indoor bike or erg, or enable Demo Mode to simulate a 12-rider studio session."
        )
        BikeProtocol.KEISER -> ProtocolEmptyInfo(
            icon = "🚴‍♂️",
            brandPill = "KEISER M3i FLEET",
            waitingTitle = "Waiting for Keiser M3i Broadcasts...",
            idleTitle = "No Keiser M3i Bikes Detected",
            scanDescription = "Actively scanning for authentic Keiser 0x0102 broadcasts. Pedal the bike to awaken the computer and transmit telemetry.",
            idleDescription = "Tap 'Start Scan' to passively listen for Keiser M3i bikes in range, or enable Demo Mode to simulate a 12-rider studio session."
        )
        BikeProtocol.STAGES -> ProtocolEmptyInfo(
            icon = "🚴‍♂️",
            brandPill = "STAGES SC3 FLEET",
            waitingTitle = "Waiting for Stages SC3 Broadcasts...",
            idleTitle = "No Stages SC3 Bikes Detected",
            scanDescription = "Actively scanning for Stages 0x0151 studio broadcasts with dual-sided power & cadence. Spin the cranks to awaken the power meter and transmit telemetry.",
            idleDescription = "Tap 'Start Scan' to passively listen for Stages SC3 / SC2 bikes in range, or enable Demo Mode to simulate a 12-rider studio session."
        )
        BikeProtocol.SCHWINN_4IIII -> ProtocolEmptyInfo(
            icon = "🚴‍♂️",
            brandPill = "SCHWINN CARBON BLUE • 4iiii",
            waitingTitle = "Waiting for Schwinn / 4iiii Broadcasts...",
            idleTitle = "No Schwinn Carbon Blue Bikes Detected",
            scanDescription = "Actively scanning for 4iiii 0x0182 crank broadcasts. Spin the cranks to awaken the 4iiii power meter and transmit telemetry.",
            idleDescription = "Tap 'Start Scan' to passively listen for Schwinn 4iiii power cranks, or enable Demo Mode to simulate a 12-rider Echelon2 studio session."
        )
        BikeProtocol.CONCEPT2 -> ProtocolEmptyInfo(
            icon = "🚴‍♂️",
            brandPill = "CONCEPT2 BIKEERG • PM5",
            waitingTitle = "Waiting for Concept2 PM5 Broadcasts...",
            idleTitle = "No Concept2 PM5 Monitors Detected",
            scanDescription = "Actively scanning for Concept2 PM5 Bluetooth beacons (BikeErg, RowErg, SkiErg). Ensure wireless is enabled in the PM5 monitor menu.",
            idleDescription = "Tap 'Start Scan' to passively listen for Concept2 PM5 monitors in range, or enable Demo Mode to simulate a 12-rider PM5 studio session."
        )
        BikeProtocol.FTMS_UNIVERSAL -> ProtocolEmptyInfo(
            icon = "🚴‍♂️",
            brandPill = "BLUETOOTH FTMS FLEET",
            waitingTitle = "Waiting for FTMS Smart Bike Broadcasts...",
            idleTitle = "No FTMS Smart Bikes Detected",
            scanDescription = "Actively scanning for Fitness Machine Service (UUID 0x1826) broadcasts (Wattbike, Life Fitness, Technogym, Matrix). Start pedaling to transmit live data.",
            idleDescription = "Tap 'Start Scan' to passively listen for FTMS indoor bikes, or enable Demo Mode to simulate a 12-rider studio session."
        )
        BikeProtocol.POWER_METERS -> ProtocolEmptyInfo(
            icon = "⚡",
            brandPill = "BLE POWER METERS",
            waitingTitle = "Waiting for Power Meter Broadcasts...",
            idleTitle = "No Cycling Power Meters Detected",
            scanDescription = "Actively scanning for standard Cycling Power Profile (UUID 0x1818) broadcasts (Assioma, Garmin, Quarq, SRM). Spin pedals or cranks to awaken the meter.",
            idleDescription = "Tap 'Start Scan' to passively listen for standard cycling power meters, or enable Demo Mode to simulate a 12-rider studio session."
        )
    }
}
