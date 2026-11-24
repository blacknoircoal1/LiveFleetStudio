package com.keiser.studio.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keiser.studio.AppConfig
import com.keiser.studio.model.BikeProtocol
import com.keiser.studio.model.FtpZone
import com.keiser.studio.model.LeaderboardViewMode
import com.keiser.studio.ui.theme.*
import com.keiser.studio.viewmodel.KeiserUiState

enum class SettingsFolder(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
) {
    BIKE_FLEET(
        title = "Bike and Brand",
        subtitle = "Select indoor bike brand (Keiser, Stages, Schwinn, Concept2, FTMS)",
        icon = Icons.AutoMirrored.Filled.DirectionsBike
    ),
    POWER_ZONES(
        title = "Power Training & Zones",
        subtitle = "Configure 5-Zone vs 7-Zone power models and FTP target thresholds",
        icon = Icons.Default.Speed
    ),
    BLUETOOTH_SCANNER(
        title = "Bluetooth Scanner",
        subtitle = "Low-latency broadcast listening, signal filters, and idle timeouts",
        icon = Icons.Default.Bluetooth
    ),
    KNOWLEDGE_BASE(
        title = "Knowledge Base & Guide",
        subtitle = "Features, what the app can do, how to use, and studio guide",
        icon = Icons.Default.Info
    )
}

/**
 * Full-Page Studio Settings Screen with Folder-Based Sub-Settings Navigation.
 * Completely replaces pop-out modals with a clean, dedicated studio interface.
 */
@Composable
fun SettingsScreen(
    uiState: KeiserUiState,
    onCloseSettings: () -> Unit,
    onSelectProtocol: (BikeProtocol) -> Unit,
    onZoneCountChange: (Int) -> Unit,
    onViewModeChange: (LeaderboardViewMode) -> Unit = {},
    onBleToggle: () -> Unit = {},
    onDemoToggle: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var activeFolder by remember { mutableStateOf<SettingsFolder?>(null) }

    // Intercept hardware and gesture back presses so user navigates back one level rather than exiting the app
    BackHandler {
        if (activeFolder != null) {
            activeFolder = null // Back to folders menu
        } else {
            onCloseSettings() // Back to Studio Leaderboard
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // TOP NAVIGATION BAR
        SettingsTopBar(
            activeFolder = activeFolder,
            onBack = {
                if (activeFolder != null) {
                    activeFolder = null // Back to folders menu
                } else {
                    onCloseSettings() // Back to Studio Leaderboard
                }
            }
        )

        HorizontalDivider(color = Color(0xFF1E2230), thickness = 1.dp)

        // CONTENT: MAIN FOLDERS DIRECTORY OR DRILL-DOWN SUB-PAGE
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            when (activeFolder) {
                null -> {
                    // MAIN DIRECTORY OF SETTINGS FOLDERS
                    FoldersDirectory(
                        uiState = uiState,
                        onOpenFolder = { folder -> activeFolder = folder }
                    )
                }
                SettingsFolder.BIKE_FLEET -> {
                    // SUB-PAGE 1: BIKE AND BRAND PROTOCOLS
                    BikeFleetSubPage(
                        selectedProtocol = uiState.selectedProtocol,
                        onSelectProtocol = onSelectProtocol
                    )
                }
                SettingsFolder.POWER_ZONES -> {
                    // SUB-PAGE 2: POWER TRAINING ZONES
                    PowerZonesSubPage(
                        zoneCount = uiState.zoneCount,
                        onZoneCountChange = onZoneCountChange
                    )
                }
                SettingsFolder.BLUETOOTH_SCANNER -> {
                    // SUB-PAGE 3: BLUETOOTH & HARDWARE SCANNER
                    BluetoothScannerSubPage(
                        isScanning = uiState.isScanning,
                        isDemoMode = uiState.isDemoMode,
                        activeBikeCount = uiState.activeBikeCount,
                        selectedProtocol = uiState.selectedProtocol,
                        onBleToggle = onBleToggle,
                        onDemoToggle = onDemoToggle
                    )
                }
                SettingsFolder.KNOWLEDGE_BASE -> {
                    // SUB-PAGE 4: KNOWLEDGE BASE & STUDIO GUIDE
                    KnowledgeBaseSubPage()
                }
            }
        }
    }
}

@Composable
private fun SettingsTopBar(
    activeFolder: SettingsFolder?,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color(0xFF080A10))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Back Button
        Surface(
            color = Color(0xFF151926),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .size(36.dp)
                .clickable { onBack() }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextMain,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Title and Breadcrumbs
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "STUDIO SETTINGS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (activeFolder == null) AccentEnergy else TextMuted
                )
                if (activeFolder != null) {
                    Text(
                        text = "›",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDim
                    )
                    Text(
                        text = activeFolder.title.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = AccentEnergy
                    )
                }
            }
            Text(
                text = activeFolder?.subtitle ?: "Fleet protocols, zone models, scanner, and guide",
                fontSize = 10.sp,
                color = TextDim,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun FoldersDirectory(
    uiState: KeiserUiState,
    onOpenFolder: (SettingsFolder) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "SETTINGS FOLDERS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = TextDim,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }

        items(SettingsFolder.entries) { folder ->
            val badgeText = when (folder) {
                SettingsFolder.BIKE_FLEET -> uiState.selectedProtocol.displayName.substringBefore(" ")
                SettingsFolder.POWER_ZONES -> "${uiState.zoneCount} ZONES"
                SettingsFolder.BLUETOOTH_SCANNER -> if (uiState.isScanning) "SCANNING" else "STANDBY"
                SettingsFolder.KNOWLEDGE_BASE -> "GUIDE"
            }

            FolderCard(
                folder = folder,
                badgeText = badgeText,
                onClick = { onOpenFolder(folder) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = AppConfig.VERSION_LABEL,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDim
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Created by ${AppConfig.CREATOR}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentEnergy
                )
            }
        }
    }
}

@Composable
private fun FolderCard(
    folder: SettingsFolder,
    badgeText: String,
    onClick: () -> Unit
) {
    Surface(
        color = Color(0xFF0C0F18),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2638)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Folder Icon Box
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF171D2D)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = folder.icon,
                    contentDescription = null,
                    tint = AccentEnergy,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Folder Title & Subtitle
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = folder.title,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = AccentEnergy.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            color = AccentEnergy,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = folder.subtitle,
                    fontSize = 11.sp,
                    color = TextMuted,
                    lineHeight = 15.sp
                )
            }

            // Right Chevron
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open Folder",
                tint = TextDim,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// ==============================================================================
// SUB-PAGE 1: BIKE FLEET PROTOCOLS
// ==============================================================================
@Composable
private fun BikeFleetSubPage(
    selectedProtocol: BikeProtocol,
    onSelectProtocol: (BikeProtocol) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "SUPPORTED FLEET BRANDS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = TextDim,
                letterSpacing = 1.sp
            )
            Text(
                text = "Select your indoor bike brand, or choose 'Auto-Detect' to scan all brands simultaneously.",
                fontSize = 11.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
            )
        }

        items(BikeProtocol.entries) { protocol ->
            val isSelected = selectedProtocol == protocol
            Surface(
                color = if (isSelected) Color(0xFF141A28) else Color(0xFF0C0F18),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    if (isSelected) 1.5.dp else 1.dp,
                    if (isSelected) AccentEnergy else Color(0xFF1E2638)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectProtocol(protocol) }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isSelected) AccentEnergy else TextDim,
                        modifier = Modifier
                            .size(22.dp)
                            .padding(top = 2.dp)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = protocol.displayName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else TextMain
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = protocol.subtitle,
                            fontSize = 11.sp,
                            color = TextMuted
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Supported: ${protocol.supportedModels}",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) AccentSuccess else TextDim
                        )
                    }
                }
            }
        }
    }
}

// ==============================================================================
// SUB-PAGE 2: POWER TRAINING ZONES
// ==============================================================================
@Composable
private fun PowerZonesSubPage(
    zoneCount: Int,
    onZoneCountChange: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "POWER TRAINING MODEL",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = TextDim,
                letterSpacing = 1.sp
            )
            Text(
                text = "Choose between the standard 5-Zone Keiser Ride By Color system or the advanced 7-Zone Coggan / Keiser Sprint training model.",
                fontSize = 11.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
            )

            // Zone Model Selection Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(5 to "5 ZONES", 7 to "7 ZONES").forEach { (count, title) ->
                    val isSelected = zoneCount == count
                    Surface(
                        color = if (isSelected) Color(0xFF141A28) else Color(0xFF0C0F18),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            if (isSelected) 1.5.dp else 1.dp,
                            if (isSelected) AccentEnergy else Color(0xFF1E2638)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onZoneCountChange(count) }
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isSelected) AccentEnergy else TextMain
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (count == 5) "Keiser Ride By Color" else "Coggan / Sprint Model",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "CURRENT ZONE BREAKDOWN ($zoneCount ZONES)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = TextDim,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        val zones = if (zoneCount == 7) {
            listOf(
                FtpZone.ZONE_1 to "< 55% Active Recovery",
                FtpZone.ZONE_2 to "56 – 75% Endurance",
                FtpZone.ZONE_3 to "76 – 90% Tempo",
                FtpZone.ZONE_4 to "91 – 105% Lactate Threshold",
                FtpZone.ZONE_5 to "106 – 120% VO2 Max",
                FtpZone.ZONE_6 to "121 – 150% Anaerobic Capacity",
                FtpZone.ZONE_7 to "> 150% Neuromuscular Power"
            )
        } else {
            listOf(
                FtpZone.ZONE_1 to "< 55% Recovery",
                FtpZone.ZONE_2 to "56 – 75% Endurance",
                FtpZone.ZONE_3 to "76 – 90% Tempo",
                FtpZone.ZONE_4 to "91 – 105% Threshold",
                FtpZone.ZONE_5 to "> 105% VO2 Max"
            )
        }

        items(zones) { (zone, desc) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0C0F18), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF1E2638), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp, 24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(zone.color),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = zone.code,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                Text(
                    text = desc,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// ==============================================================================
// SUB-PAGE 3: BLUETOOTH & STUDIO SCANNER
// ==============================================================================
@Composable
private fun BluetoothScannerSubPage(
    isScanning: Boolean,
    isDemoMode: Boolean,
    activeBikeCount: Int,
    selectedProtocol: BikeProtocol,
    onBleToggle: () -> Unit,
    onDemoToggle: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "BLUETOOTH HARDWARE ENGINE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = TextDim,
            letterSpacing = 1.sp
        )

        Surface(
            color = Color(0xFF0C0F18),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2638)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("SCANNER STATUS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Surface(
                        color = if (isScanning) AccentSuccess.copy(alpha = 0.2f) else Color(0xFF1E2638),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (isScanning) "ACTIVE SCANNING" else "STANDBY",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isScanning) AccentSuccess else TextDim,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ACTIVE FLEET BIKES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Text("$activeBikeCount Equipment in Range", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMain)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("FILTER PROTOCOL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Text(selectedProtocol.displayName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentEnergy)
                }

                Spacer(modifier = Modifier.height(2.dp))

                Button(
                    onClick = onBleToggle,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isScanning) AccentDanger else AccentPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) {
                    Text(
                        text = if (isScanning) "Stop Live Scanner" else "Start Live Scanner",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Text(
            text = "FLEET DEMO SIMULATION",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = TextDim,
            letterSpacing = 1.sp
        )

        Surface(
            color = Color(0xFF0C0F18),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2638)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("12-Rider Studio Demo", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMain)
                    Text("Simulates live fleet telemetry for testing without equipment present", fontSize = 10.sp, color = TextMuted)
                }
                Switch(
                    checked = isDemoMode,
                    onCheckedChange = onDemoToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AccentSuccess,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = BgCard
                    )
                )
            }
        }

        Text(
            text = "PASSIVE BROADCAST ADVANTAGE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = TextDim,
            letterSpacing = 1.sp
        )
        Surface(
            color = Color(0xFF0C0F18),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2638)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("• No Pairing Required: Riders never need to manually connect.", fontSize = 11.sp, color = TextMain)
                Text("• Unlimited Capacity: Reads 1, 12, 30, or 60 bikes simultaneously over open BLE advertisements.", fontSize = 11.sp, color = TextMuted)
                Text("• 10-Second Idle Timeout: Automatically dims inactive bikes when a rider finishes.", fontSize = 11.sp, color = TextMuted)
            }
        }
    }
}

// ==============================================================================
// SUB-PAGE 4: KNOWLEDGE BASE & STUDIO GUIDE
// ==============================================================================
@Composable
private fun KnowledgeBaseSubPage() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "KNOWLEDGE BASE & GUIDE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = TextDim,
                letterSpacing = 1.sp
            )
            Text(
                text = "Learn about the app features, universal bike broadcasting, power training zones, and how to operate your studio.",
                fontSize = 11.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
            )
        }

        // Section 1: What is LiveFleet Studio?
        item {
            KnowledgeSectionCard(
                title = "WHAT IS LIVEFLEET STUDIO?",
                icon = "🚴‍♂️"
            ) {
                Text(
                    text = "LiveFleet Studio is a professional, high-performance cycling and multi-erg fitness studio leaderboard and power training companion designed for cycling studios, row/ski facilities, gyms, and home training setups.",
                    fontSize = 11.sp,
                    color = TextMain,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Unlike standard fitness apps that require manual 1-to-1 Bluetooth pairing, LiveFleet Studio operates via connectionless passive Bluetooth Low Energy (BLE) broadcast listening. A single Android device, TV, or projector can monitor 1, 12, 30, or 60+ bikes, ergs, and smart trainers simultaneously without pairing limits or connection lag.",
                    fontSize = 11.sp,
                    color = TextMuted,
                    lineHeight = 16.sp
                )
            }
        }

        // Section 2: Key Features
        item {
            KnowledgeSectionCard(
                title = "KEY FEATURES & CAPABILITIES",
                icon = "✨"
            ) {
                KnowledgeBullet("Connectionless BLE Broadcasts", "Receives open telemetry packets from any bike in range without riders having to pair their bikes.")
                KnowledgeBullet("Dual Display Modes", "Switch freely between floating Tachometer Gauges (Ride By Color dial arcs) and High-Density Table view.")
                KnowledgeBullet("5-Zone & 7-Zone Power Models", "Support for Keiser Ride By Color (5 zones) or Coggan athletic sprint training (7 zones).")
                KnowledgeBullet("Group FTP Testing Engine", "Conduct studio-wide 5-minute (85% factor) or 20-minute (95% factor) FTP tests with audio-visual countdown and 80–90 RPM cadence alert.")
                KnowledgeBullet("Dynamic Fit-to-Screen Matrix", "Zero vertical scrolling: the app auto-calculates grid dimensions to display all active bikes on one screen.")
                KnowledgeBullet("Persistent Results Re-Entry", "Completed FTP results remain accessible throughout class for cool-down review via the top-bar 'Last FTP Results' button.")
                KnowledgeBullet("Dedicated Digital Stopwatch", "High-visibility 7-segment session timer with Start, Pause, and Reset for class intervals.")
            }
        }

        // Section 3: Supported Bikes
        item {
            KnowledgeSectionCard(
                title = "SUPPORTED BIKES & HARDWARE",
                icon = "📡"
            ) {
                KnowledgeBullet("Keiser M3i / M3i Studio", "Decodes authentic Keiser broadcasts including Watts, RPM, Resistance Gear, Heart Rate, and Energy.")
                KnowledgeBullet("Stages SC3 / Stages Power", "Commercial studio indoor bikes and Stages Power cranks broadcasting dual-sided watts and cadence.")
                KnowledgeBullet("Schwinn Carbon Blue / 4iiii", "Schwinn AC Performance Plus bikes equipped with 4iiii Precision power cranks.")
                KnowledgeBullet("Concept2 BikeErg / PM5", "PM5 console broadcast beacons transmitting watts, cadence, split pace, and distance.")
                KnowledgeBullet("Standard FTMS Smart Bikes", "Commercial indoor smart bikes utilizing the Bluetooth SIG Fitness Machine Service standard (Wattbike, Life Fitness, Technogym, Matrix).")
                KnowledgeBullet("Cycling Power Profile", "Third-party pedals and crank power meters (Favero Assioma, Garmin Rally, Quarq, SRM).")
            }
        }

        // Section 4: How to Use
        item {
            KnowledgeSectionCard(
                title = "HOW TO USE (QUICK START GUIDE)",
                icon = "📖"
            ) {
                KnowledgeStep(1, "Wake Up Your Fleet", "Riders start pedaling to activate their bike's Bluetooth broadcast transmitter.")
                KnowledgeStep(2, "Tap Scan in Top Bar", "Tap 'Scan' (or enable Demo mode for testing). Bikes in range stream telemetry into live tiles automatically.")
                KnowledgeStep(3, "Calibrate Rider Profiles", "Tap any bike tile to set the rider's name and individual FTP. All zone percentages immediately recalibrate to that rider.")
                KnowledgeStep(4, "Run Group FTP Tests", "Tap '⚡ FTP' in the top bar. Choose a 5-minute or 20-minute test. A 5-second countdown prepares riders, and dials highlight cadence if outside 80–90 RPM.")
                KnowledgeStep(5, "Save & Review Results", "When the test concludes, view the single-screen results leaderboard. Apply new FTPs for this session with 'Done', and re-open the results anytime via 'Last FTP Results'.")
                KnowledgeStep(6, "Dedicated Stopwatch", "Use the top-bar digital session timer to track overall class duration or individual interval sprints with Start, Pause, and Reset.")
            }
        }

        // Section 5: Creator & App Info
        item {
            Surface(
                color = Color(0xFF101420),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF263248)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LIVEFLEET STUDIO",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = AccentEnergy,
                            letterSpacing = 1.sp
                        )
                        Surface(
                            color = AccentEnergy.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "v${AppConfig.VERSION_NAME}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = AccentEnergy,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Created by KayAyyFit",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Engineered for high-intensity studio cycling, connectionless BLE fleet scalability, and zero-distraction projection displays.",
                        fontSize = 10.5.sp,
                        color = TextMuted,
                        lineHeight = 15.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun KnowledgeSectionCard(
    title: String,
    icon: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = Color(0xFF0C0F18),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2638)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = icon, fontSize = 14.sp)
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = TextDim,
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun KnowledgeBullet(title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text("•", fontSize = 12.sp, color = AccentEnergy, fontWeight = FontWeight.Bold)
        Column {
            Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMain)
            Text(text = description, fontSize = 10.5.sp, color = TextMuted, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun KnowledgeStep(stepNumber: Int, title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            color = Color(0xFF151926),
            shape = RoundedCornerShape(6.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262D42)),
            modifier = Modifier.size(22.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "$stepNumber",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = AccentEnergy
                )
            }
        }
        Column {
            Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMain)
            Text(text = description, fontSize = 10.5.sp, color = TextMuted, lineHeight = 14.sp)
        }
    }
}
