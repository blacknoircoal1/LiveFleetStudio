package com.livefleet.studio.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.livefleet.studio.AppConfig
import com.livefleet.studio.data.RiderProfileRepository
import com.livefleet.studio.model.BikeProtocol
import com.livefleet.studio.model.Concept2ErgFilter
import com.livefleet.studio.model.Concept2MetricMode
import com.livefleet.studio.model.FtpZone
import com.livefleet.studio.model.LeaderboardViewMode
import com.livefleet.studio.model.TileHeroMetric
import com.livefleet.studio.ui.theme.*
import com.livefleet.studio.util.AppDiagnosticLogger
import com.livefleet.studio.viewmodel.LiveFleetUiState

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
    TILE_LAYOUT(
        title = "Tile Layout & Metrics",
        subtitle = "Customize tile readouts and hardware units screen with live preview",
        icon = Icons.Default.DashboardCustomize
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
    uiState: LiveFleetUiState,
    onCloseSettings: () -> Unit,
    onSelectProtocol: (BikeProtocol) -> Unit,
    onZoneCountChange: (Int) -> Unit,
    onDefaultFtpChange: (Int) -> Unit = {},
    onViewModeChange: (LeaderboardViewMode) -> Unit = {},
    onBleToggle: () -> Unit = {},
    onDemoToggle: (Boolean) -> Unit = {},
    onDemoBikeCountChange: (Int) -> Unit = {},
    onToggleStudioLockProximity: (Boolean) -> Unit = {},
    onConcept2MetricModeChange: (Concept2MetricMode) -> Unit = {},
    onConcept2ErgFilterChange: (Concept2ErgFilter) -> Unit = {},
    onTileHeroMetricChange: (TileHeroMetric) -> Unit = {},
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
                        concept2MetricMode = uiState.concept2MetricMode,
                        concept2ErgFilter = uiState.concept2ErgFilter,
                        onSelectProtocol = onSelectProtocol,
                        onConcept2MetricModeChange = onConcept2MetricModeChange,
                        onConcept2ErgFilterChange = onConcept2ErgFilterChange
                    )
                }
                SettingsFolder.POWER_ZONES -> {
                    // SUB-PAGE 2: POWER TRAINING ZONES
                    PowerZonesSubPage(
                        zoneCount = uiState.zoneCount,
                        defaultFtp = uiState.defaultFtp,
                        onZoneCountChange = onZoneCountChange,
                        onDefaultFtpChange = onDefaultFtpChange
                    )
                }
                SettingsFolder.TILE_LAYOUT -> {
                    TileLayoutSubPage(
                        selectedProtocol = uiState.selectedProtocol,
                        selectedHeroMetric = uiState.tileHeroMetric,
                        concept2MetricMode = uiState.concept2MetricMode,
                        onSelectHeroMetric = onTileHeroMetricChange,
                        onConcept2MetricModeChange = onConcept2MetricModeChange
                    )
                }
                SettingsFolder.BLUETOOTH_SCANNER -> {
                    // SUB-PAGE 3: BLUETOOTH & HARDWARE SCANNER
                    BluetoothScannerSubPage(
                        isScanning = uiState.isScanning,
                        isDemoMode = uiState.isDemoMode,
                        demoBikeCount = uiState.demoBikeCount,
                        activeBikeCount = uiState.activeBikeCount,
                        packetsPerSecond = uiState.packetsPerSecond,
                        selectedProtocol = uiState.selectedProtocol,
                        antPlusStatus = uiState.antPlusStatus,
                        studioLockProximityEnabled = uiState.studioLockProximityEnabled,
                        whitelistedCount = uiState.whitelistedMachines.size,
                        onBleToggle = onBleToggle,
                        onDemoToggle = onDemoToggle,
                        onDemoBikeCountChange = onDemoBikeCountChange,
                        onToggleStudioLockProximity = onToggleStudioLockProximity
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
    uiState: LiveFleetUiState,
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
                SettingsFolder.POWER_ZONES -> "${uiState.zoneCount}Z • ${uiState.defaultFtp}W"
                SettingsFolder.TILE_LAYOUT -> when (uiState.selectedProtocol) {
                    BikeProtocol.CONCEPT2 -> uiState.concept2MetricMode.label.uppercase()
                    else -> when (uiState.tileHeroMetric) {
                        TileHeroMetric.WATTS -> "WATTS"
                        TileHeroMetric.RPM -> "RPM"
                        TileHeroMetric.FTP_PERCENT -> "% FTP"
                    }
                }
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
    concept2MetricMode: Concept2MetricMode,
    concept2ErgFilter: Concept2ErgFilter,
    onSelectProtocol: (BikeProtocol) -> Unit,
    onConcept2MetricModeChange: (Concept2MetricMode) -> Unit,
    onConcept2ErgFilterChange: (Concept2ErgFilter) -> Unit
) {
    var infoProtocol by remember { mutableStateOf<BikeProtocol?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                text = "Select your indoor bike brand, or choose Auto-Detect to scan all brands at once. Switching brands purges open GATT sessions automatically. Tap ? for details.",
                fontSize = 11.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
            )
        }

        items(BikeProtocol.entries) { protocol ->
            val isSelected = selectedProtocol == protocol
            Surface(
                color = if (isSelected) Color(0xFF141A28) else Color(0xFF0C0F18),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(
                    if (isSelected) 1.5.dp else 1.dp,
                    if (isSelected) AccentEnergy else Color(0xFF1E2638)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectProtocol(protocol) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isSelected) AccentEnergy else TextDim,
                        modifier = Modifier.size(20.dp)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = protocol.displayName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else TextMain,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = protocol.supportedModels,
                            fontSize = 10.sp,
                            color = if (isSelected) AccentSuccess else TextDim,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Compact ? Info Button for connection details & setup tip
                    Surface(
                        color = Color(0xFF182032),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2B3752)),
                        modifier = Modifier
                            .size(28.dp)
                            .clickable { infoProtocol = protocol }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "?",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = AccentEnergy
                            )
                        }
                    }
                }

                if (isSelected && (protocol == BikeProtocol.CONCEPT2 || protocol == BikeProtocol.AUTO_DETECT)) {
                    HorizontalDivider(
                        color = Color(0xFF1E2638),
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "CONCEPT2 MACHINE FILTER",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Black,
                            color = TextDim,
                            letterSpacing = 0.8.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Concept2ErgFilter.entries.forEach { filter ->
                                val isFilterSelected = concept2ErgFilter == filter
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isFilterSelected) AccentPrimary else Color(0xFF0C0F18))
                                        .border(
                                            1.dp,
                                            if (isFilterSelected) AccentPrimary else Color(0xFF262D42),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable { onConcept2ErgFilterChange(filter) }
                                        .padding(vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = filter.label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isFilterSelected) Color.White else TextMain,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Keeps Bluetooth GATT slots dedicated only to your chosen Concept2 class type (e.g., Row-Only or Ski-Only).",
                            fontSize = 9.5.sp,
                            color = TextMuted,
                            lineHeight = 13.sp
                        )
                    }
                }
            }
        }

    }

    infoProtocol?.let { protocol ->
        ProtocolConnectionInfoDialog(
            protocol = protocol,
            onDismiss = { infoProtocol = null }
        )
    }
}

@Composable
private fun ProtocolConnectionInfoDialog(
    protocol: BikeProtocol,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xFF0C0F18),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF263046)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = protocol.displayName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        color = AccentEnergy.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "INFO",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = AccentEnergy,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF1E2638), thickness = 1.dp)

                ProtocolInfoRow("CONNECTION", protocol.broadcastType, AccentEnergy)
                ProtocolInfoRow("PROTOCOL / UUID", protocol.companyOrServiceUuid, TextMain)
                ProtocolInfoRow("SUPPORTED MODELS", protocol.supportedModels, AccentSuccess)
                ProtocolInfoRow("SETUP TIP", protocol.setupTip, TextMuted)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentEnergy,
                            contentColor = BgMain
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text("Got It", fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProtocolInfoRow(label: String, value: String, valueColor: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Black,
            color = TextDim,
            letterSpacing = 0.8.sp
        )
        Text(
            text = value,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor,
            lineHeight = 16.sp
        )
    }
}

// ==============================================================================
// SUB-PAGE 2: POWER TRAINING ZONES
// ==============================================================================
@Composable
private fun PowerZonesSubPage(
    zoneCount: Int,
    defaultFtp: Int,
    onZoneCountChange: (Int) -> Unit,
    onDefaultFtpChange: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "DEFAULT STUDIO FTP",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = TextDim,
                letterSpacing = 1.sp
            )
            Text(
                text = "Used for any bike without an individual FTP set. Tap any bike tile on the leaderboard to override its FTP.",
                fontSize = 11.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
            )

            Surface(
                color = Color(0xFF0C0F18),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2638)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            DefaultFtpAdjustChip("-10") {
                                onDefaultFtpChange(
                                    (defaultFtp - 10).coerceIn(
                                        RiderProfileRepository.MIN_FTP_WATTS,
                                        RiderProfileRepository.MAX_FTP_WATTS
                                    )
                                )
                            }
                            DefaultFtpAdjustChip("-5") {
                                onDefaultFtpChange(
                                    (defaultFtp - 5).coerceIn(
                                        RiderProfileRepository.MIN_FTP_WATTS,
                                        RiderProfileRepository.MAX_FTP_WATTS
                                    )
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "$defaultFtp",
                                fontFamily = Dseg7FontFamily,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentEnergy
                            )
                            Text(
                                text = "W",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = TextMuted,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            DefaultFtpAdjustChip("+5") {
                                onDefaultFtpChange(
                                    (defaultFtp + 5).coerceIn(
                                        RiderProfileRepository.MIN_FTP_WATTS,
                                        RiderProfileRepository.MAX_FTP_WATTS
                                    )
                                )
                            }
                            DefaultFtpAdjustChip("+10") {
                                onDefaultFtpChange(
                                    (defaultFtp + 10).coerceIn(
                                        RiderProfileRepository.MIN_FTP_WATTS,
                                        RiderProfileRepository.MAX_FTP_WATTS
                                    )
                                )
                            }
                        }
                    }

                    // Quick Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(120, 150, 180, 200, 250).forEach { preset ->
                            val isSelected = defaultFtp == preset
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) AccentEnergy else Color(0xFF151926))
                                    .border(
                                        1.dp,
                                        if (isSelected) AccentEnergy else Color(0xFF262D42),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { onDefaultFtpChange(preset) }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${preset}W",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isSelected) BgMain else TextMain,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "POWER TRAINING MODEL",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = TextDim,
                letterSpacing = 1.sp
            )
            Text(
                text = "Choose between the 7-Zone Keiser M3i / Coggan model (Grey, Blue, Green, Orange, Red, and Purple for Z6 & Z7) or the classic 5-Zone Ride By Color system.",
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
                                text = if (count == 5) "5-Zone Ride By Color" else "Keiser M3i / 7-Zone",
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
                text = "ZONE BREAKDOWN ($zoneCount ZONES @ ${defaultFtp}W FTP)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = TextDim,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        val w55 = (defaultFtp * 55) / 100
        val w75 = (defaultFtp * 75) / 100
        val w90 = (defaultFtp * 90) / 100
        val w105 = (defaultFtp * 105) / 100
        val w120 = (defaultFtp * 120) / 100
        val w150 = (defaultFtp * 150) / 100

        val zones = if (zoneCount == 7) {
            listOf(
                Triple(FtpZone.ZONE_1, "≤ 55% Active Recovery", "≤ ${w55}W"),
                Triple(FtpZone.ZONE_2, "56 – 75% Endurance", "${w55 + 1} – ${w75}W"),
                Triple(FtpZone.ZONE_3, "76 – 90% Tempo", "${w75 + 1} – ${w90}W"),
                Triple(FtpZone.ZONE_4_7Z, "91 – 105% Lactate Threshold", "${w90 + 1} – ${w105}W"),
                Triple(FtpZone.ZONE_5_7Z, "106 – 120% VO2 Max", "${w105 + 1} – ${w120}W"),
                Triple(FtpZone.ZONE_6, "121 – 150% Anaerobic (Purple)", "${w120 + 1} – ${w150}W"),
                Triple(FtpZone.ZONE_7, "> 150% Neuromuscular (Purple)", "> ${w150}W")
            )
        } else {
            listOf(
                Triple(FtpZone.ZONE_1, "≤ 55% Recovery", "≤ ${w55}W"),
                Triple(FtpZone.ZONE_2, "56 – 75% Endurance", "${w55 + 1} – ${w75}W"),
                Triple(FtpZone.ZONE_3, "76 – 90% Tempo", "${w75 + 1} – ${w90}W"),
                Triple(FtpZone.ZONE_4, "91 – 105% Threshold", "${w90 + 1} – ${w105}W"),
                Triple(FtpZone.ZONE_5, "> 105% VO2 Max", "> ${w105}W")
            )
        }

        items(zones) { (zone, desc, wattRange) ->
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
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = wattRange,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = zone.color
                )
            }
        }
    }
}

@Composable
private fun DefaultFtpAdjustChip(label: String, onClick: () -> Unit) {
    Surface(
        color = Color(0xFF151926),
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262D42)),
        modifier = Modifier
            .height(34.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = AccentEnergy
            )
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
    demoBikeCount: Int,
    activeBikeCount: Int,
    packetsPerSecond: Int,
    selectedProtocol: BikeProtocol,
    antPlusStatus: String,
    studioLockProximityEnabled: Boolean,
    whitelistedCount: Int,
    onBleToggle: () -> Unit,
    onDemoToggle: (Boolean) -> Unit,
    onDemoBikeCountChange: (Int) -> Unit,
    onToggleStudioLockProximity: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
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
                                text = if (isScanning) "SCANNING • $packetsPerSecond pkt/s" else "STANDBY",
                                style = TabularNumTextStyle,
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
                        Text(
                            "$activeBikeCount Equipment in Range",
                            style = TabularNumTextStyle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMain
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("FILTER PROTOCOL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        Text(selectedProtocol.displayName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentEnergy)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("USB ANT+ FE-C", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        Text(
                            text = antPlusStatus,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (antPlusStatus.contains("Active", ignoreCase = true)) AccentSuccess else TextDim
                        )
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
        }

        item {
            Text(
                text = "STUDIO LOCK & PROXIMITY FILTER",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = TextDim,
                letterSpacing = 1.sp
            )

            Surface(
                color = Color(0xFF0C0F18),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2638)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text("Studio Lock & RSSI Gate (-75 dBm)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMain)
                        Text(
                            "Ignores non-whitelisted machines weaker than -75 dBm ($whitelistedCount locked studio bike(s)). Tap any tile to lock its MAC to a Studio Bike #.",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                    Switch(
                        checked = studioLockProximityEnabled,
                        onCheckedChange = onToggleStudioLockProximity,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AccentSuccess,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = BgCard
                        )
                    )
                }
            }
        }

        item {
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text("${demoBikeCount}-Rider Studio Demo", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMain)
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

                    HorizontalDivider(color = Color(0xFF1E2638))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Demo Bikes", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = Color(0xFF161B26),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .size(28.dp)
                                    .clickable { onDemoBikeCountChange(demoBikeCount - 1) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("-", fontSize = 14.sp, fontWeight = FontWeight.Black, color = TextMain)
                                }
                            }
                            Text(
                                text = "$demoBikeCount",
                                style = TabularNumTextStyle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = AccentEnergy,
                                modifier = Modifier.widthIn(min = 24.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Surface(
                                color = Color(0xFF161B26),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .size(28.dp)
                                    .clickable { onDemoBikeCountChange(demoBikeCount + 1) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("+", fontSize = 14.sp, fontWeight = FontWeight.Black, color = TextMain)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            DiagnosticLogCard()
        }

        item {
            Text(
                text = "HARDENED TELEMETRY GUARANTEES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = TextDim,
                letterSpacing = 1.sp
            )
            Surface(
                color = Color(0xFF0C0F18),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2638)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("• Strict 3-Layer Brand Gatekeeper: Rejects mismatched brands & serialized 6-device GATT queue prevents GATT_ERROR 133.", fontSize = 11.sp, color = TextMain)
                    Text("• Zero Ghost Connections: Clean session wipe on Start Scan and full GATT close on app exit or brand switch.", fontSize = 11.sp, color = TextMuted)
                    Text("• 10-Second Stale Auto-Purge: Any machine silent for 10s (including GATT close) clears off the active grid.", fontSize = 11.sp, color = TextMuted)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
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
                text = "Learn about the LiveFleet Studio Group Cycling app, universal bike broadcasting, power training zones, and how to run a class.",
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
                    text = "LiveFleet Studio is a Group Cycling leaderboard and multi-brand studio display built for indoor cycling classes, instructor-led rides, intervals, and group FTP testing.",
                    fontSize = 11.sp,
                    color = TextMain,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "It combines three real-world wireless transports — Passive BLE Broadcasts, Active BLE GATT, and Native USB ANT+ — so one Android phone, tablet, or HDMI TV/DeX display can run everything from a single home erg to a 30+ bike commercial studio.",
                    fontSize = 11.sp,
                    color = TextMuted,
                    lineHeight = 16.sp
                )
            }
        }

        // Section 2: Connection Types Comparison Table
        item {
            KnowledgeSectionCard(
                title = "CONNECTION TYPES & CAPACITY",
                icon = "📶"
            ) {
                Text(
                    text = "Different bike brands transmit data using different wireless protocols. LiveFleet Studio chooses the right transport automatically:",
                    fontSize = 10.5.sp,
                    color = TextMuted,
                    lineHeight = 15.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                ConnectionTypesTable()
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Auto-Priority: When a USB-C ANT+ dongle is plugged in, ANT+ automatically takes priority for non-Keiser bikes so you get 30+ bike capacity with zero duplicate tiles. Unplugging it falls back to built-in BLE GATT automatically.",
                    fontSize = 10.sp,
                    color = AccentEnergy,
                    lineHeight = 14.sp
                )
            }
        }

        // Section 3: Key Features
        item {
            KnowledgeSectionCard(
                title = "KEY FEATURES & CAPABILITIES",
                icon = "✨"
            ) {
                KnowledgeBullet("Native Per-Brand Consoles", "Each brand displays in its own authentic studio layout: Keiser Ride By Color Dial, Stages SC3 LCD, Schwinn Echelon2, Concept2 PM5 (auto-detects BikeErg / RowErg / SkiErg), Wattbike Air-Pro, and Pro Power Head Unit.")
                KnowledgeBullet("Last-5-Digit Serial Matching", "Long serial numbers (e.g. PM5 530339622 or AIR-PRO 34002379) automatically show their last 5 digits (39622 / 02379) so riders spot their machine immediately.")
                KnowledgeBullet("Tap-to-Edit FTP & Studio Default", "Tap any bike tile or table row to set Rider Name and FTP (with live zone preview), or set a Default Studio FTP under Power Training & Zones.")
                KnowledgeBullet("Big-Screen & 30+ Bike Auto-Fit", "Proportional scaling makes Watts and RPM crisp from across the room on HDMI TVs / Samsung DeX, while automatically switching to ultra-dense auto-fit for 20–30+ bike grids with zero scrolling.")
                KnowledgeBullet("3-Layer Brand Gatekeeper & Studio Lock", "Validates exact brand signatures, locks hardware MAC addresses to fixed Studio Bike #s, and filters out weak main-floor broadcasts (< -75 dBm).")
                KnowledgeBullet("10-Second Stale Purge & Clean GATT Queue", "Serialized GATT connections prevent GATT_ERROR 133, while silent machines automatically close GATT handles and clear after 10 seconds.")
                KnowledgeBullet("Concept2 PM5 Metres & Calories", "Reads official Concept2 CE060016 / FTMS subtype badges (BikeErg / RowErg / SkiErg) and shows whole-number Metres, Calories, or Both.")
                KnowledgeBullet("Unified Dual Clock & Interval Strip", "Tap the stopwatch icon beside the session clock to start Interval #1 and expand/collapse the Interval Strip (Interval | Total clock, New Interval, End, and on-demand per-bike Summary).")
                KnowledgeBullet("5-Zone / 7-Zone & Group FTP Tests", "Supports 5-Zone Keiser or 7-Zone Coggan models, plus studio-wide 5-min (85%) or 20-min (95%) FTP tests with calm suggested 80–90 RPM banner guidance and saved results.")
            }
        }

        // Section 4: Supported Bikes & Hardware
        item {
            KnowledgeSectionCard(
                title = "SUPPORTED BIKES & HARDWARE",
                icon = "📡"
            ) {
                KnowledgeBullet("Keiser M3i / M3i Studio", "Passive BLE broadcast (0x0102) — unlimited 30+ bikes with zero pairing or extra hardware.")
                KnowledgeBullet("Stages SC3 / SC2 / SB20", "BLE GATT Cycling Power (0x1818) for 1–6 bikes, or USB ANT+ Bicycle Power (0x0B) for 30+ studio bikes.")
                KnowledgeBullet("Schwinn Carbon Blue / Echelon2 / 4iiii", "4iiii cranks connect via BLE GATT (0x1818) or ANT+. Standard MPower Echelon2 consoles are ANT+ only (use a USB-C ANT+ dongle).")
                KnowledgeBullet("Concept2 BikeErg / RowErg / SkiErg (PM5)", "BLE GATT (CE060030) for 1–6 ergs via PM5 Connect menu, or USB ANT+ FE-C for 10–30+ Group Class ergs.")
                KnowledgeBullet("Wattbike Air-Pro / Atom & FTMS Bikes", "BLE GATT FTMS (0x1826 -> 0x2AD2) and USB ANT+ FE-C/Power (Wattbike, Life Fitness IC5–8, Technogym, Matrix, Body Bike).")
                KnowledgeBullet("Power Pedals & Cranks", "BLE GATT Cycling Power (0x1818 -> 0x2A63) and USB ANT+ Power (Favero Assioma, Garmin Rally, Quarq, SRM, PowerTap).")
            }
        }

        // Section 5: How to Use
        item {
            KnowledgeSectionCard(
                title = "HOW TO USE (QUICK START GUIDE)",
                icon = "📖"
            ) {
                KnowledgeStep(1, "Select Your Bike Brand", "Open Settings -> Bike and Brand to pick your studio equipment (or Auto-Detect All Brands). Tap '?' on any brand for quick setup tips.")
                KnowledgeStep(2, "Start Scanning", "Tap 'Start Scan' on the main screen (ensure Bluetooth and Location Services are ON). To preview layouts without bikes, toggle Demo Mode under Settings -> Bluetooth Scanner.")
                KnowledgeStep(3, "Calibrate Rider FTP", "Tap any bike tile (or Table row) to set that rider's Name and FTP so zones match the bike screen, or adjust the Default Studio FTP in Power Training & Zones.")
                KnowledgeStep(4, "Project to a Big Screen", "Connect your phone via USB-C HDMI (Samsung DeX recommended for edge-to-edge 16:9). Bikes default to Bike # order so tiles stay in fixed studio positions.")
                KnowledgeStep(5, "Run Group FTP Tests & Timers", "Use '⚡ FTP TEST' for 5- or 20-minute tests with countdown and cadence alerts, and the 7-segment Session Timer for class intervals.")
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
                        text = "Created by ${AppConfig.CREATOR}",
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
private fun ConnectionTypesTable() {
    data class ConnRow(
        val type: String,
        val usedFor: String,
        val capacity: String,
        val accent: Color
    )

    val rows = listOf(
        ConnRow(
            type = "Passive BLE\n(0x0102)",
            usedFor = "Keiser M3i / Studio (open broadcast, no pairing)",
            capacity = "30+ bikes\nBuilt-in BT",
            accent = AccentEnergy
        ),
        ConnRow(
            type = "BLE GATT\n(CE06 / 1826 / 1818)",
            usedFor = "Concept2 PM5, Wattbike/FTMS, Stages, 4iiii, Pedals",
            capacity = "1–6 bikes\nBuilt-in BT",
            accent = AccentPrimary
        ),
        ConnRow(
            type = "USB ANT+\n(FE-C & Power)",
            usedFor = "Concept2 Group Classes, Stages, Schwinn Echelon2, Wattbike, Pedals",
            capacity = "30+ bikes\nUSB dongle",
            accent = AccentSuccess
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF232C40), RoundedCornerShape(8.dp))
            .background(Color(0xFF080A10))
    ) {
        // Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF131826))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TRANSPORT",
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = TextDim,
                modifier = Modifier.weight(0.95f)
            )
            Text(
                text = "USED FOR",
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = TextDim,
                modifier = Modifier.weight(1.45f)
            )
            Text(
                text = "CAPACITY",
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = TextDim,
                modifier = Modifier.weight(0.8f)
            )
        }

        rows.forEachIndexed { idx, row ->
            HorizontalDivider(color = Color(0xFF1C2334), thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (idx % 2 == 0) Color(0xFF0A0D14) else Color(0xFF0E121C))
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = row.type,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = row.accent,
                    lineHeight = 13.sp,
                    modifier = Modifier.weight(0.95f).padding(end = 6.dp)
                )
                Text(
                    text = row.usedFor,
                    fontSize = 9.5.sp,
                    color = TextMain,
                    lineHeight = 13.sp,
                    modifier = Modifier.weight(1.45f).padding(end = 6.dp)
                )
                Text(
                    text = row.capacity,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    lineHeight = 13.sp,
                    modifier = Modifier.weight(0.8f)
                )
            }
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

@Composable
private fun DiagnosticLogCard() {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showLogDialog by remember { mutableStateOf(false) }
    var logSummary by remember { mutableStateOf(AppDiagnosticLogger.getLogSummary()) }
    var logPreviewText by remember { mutableStateOf("") }

    Column {
        Text(
            text = "APP & HARDWARE DIAGNOSTIC LOG",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = TextDim,
            letterSpacing = 1.sp
        )
        Surface(
            color = Color(0xFF0C0F18),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2638)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("Session & Connection Log", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMain)
                        Text(
                            "Records BLE/GATT/ANT+ connections, machine names, timers, settings & errors (512 KB rolling).",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                    Surface(
                        color = Color(0xFF151926),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = logSummary,
                            style = TabularNumTextStyle,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = AccentEnergy,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            logSummary = AppDiagnosticLogger.getLogSummary()
                            logPreviewText = AppDiagnosticLogger.readRecentLogText()
                            showLogDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Text("View Log", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }

                    OutlinedButton(
                        onClick = {
                            val file = AppDiagnosticLogger.getCurrentLogFile()
                            if (file != null && file.exists()) {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "LiveFleet Studio Diagnostic Log")
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Diagnostic Log"))
                            }
                        },
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2B3752)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Text("Share .log", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMain, maxLines = 1, softWrap = false)
                    }

                    OutlinedButton(
                        onClick = {
                            AppDiagnosticLogger.clearLogs()
                            logSummary = "0.0 KB"
                            logPreviewText = "Diagnostic logs cleared."
                        },
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentDanger.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Clear", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentDanger, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }

    if (showLogDialog) {
        Dialog(onDismissRequest = { showLogDialog = false }) {
            Surface(
                color = Color(0xFF0A0D16),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF263046)),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.82f)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DIAGNOSTIC LOG ($logSummary)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = AccentEnergy
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(logPreviewText))
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Copy", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentPrimary)
                            }
                            TextButton(
                                onClick = { showLogDialog = false },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Close", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFF1E2638), thickness = 1.dp)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color(0xFF05070B), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF1A2030), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = logPreviewText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color(0xFFCBD5E1),
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TileLayoutSubPage(
    selectedProtocol: BikeProtocol,
    selectedHeroMetric: TileHeroMetric,
    concept2MetricMode: Concept2MetricMode,
    onSelectHeroMetric: (TileHeroMetric) -> Unit,
    onConcept2MetricModeChange: (Concept2MetricMode) -> Unit
) {
    val showSpinSection = selectedProtocol != BikeProtocol.CONCEPT2
    val showErgUnitsSection = selectedProtocol == BikeProtocol.CONCEPT2 ||
        selectedProtocol == BikeProtocol.FTMS_UNIVERSAL ||
        selectedProtocol == BikeProtocol.AUTO_DETECT

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                color = Color(0xFF0E1320),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF232C42)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACTIVE EQUIPMENT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = TextDim,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = selectedProtocol.displayName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentEnergy
                    )
                }
            }
        }

        if (showSpinSection) {
            item {
                Text(
                    text = if (showErgUnitsSection) "1. SPIN BIKES — BIG CENTER READOUT" else "SPIN BIKE TILE LAYOUT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = TextDim,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Tap which metric sits in the large center of each bike tile. The remaining metrics move cleanly to the footer.",
                    fontSize = 11.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
                )

                Surface(
                    color = Color(0xFF0C0F18),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2638)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Live Mini Spin Tile Preview
                        MiniSpinTilePreview(heroMetric = selectedHeroMetric)

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TileHeroMetric.entries.forEach { metric ->
                                val isSelected = metric == selectedHeroMetric
                                Surface(
                                    color = if (isSelected) Color(0xFF141B2D) else Color(0xFF080A10),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        if (isSelected) 1.5.dp else 1.dp,
                                        if (isSelected) AccentEnergy else Color(0xFF1E2638)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectHeroMetric(metric) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = metric.displayName,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) AccentEnergy else Color.White
                                            )
                                            Text(
                                                text = metric.subtitle,
                                                fontSize = 9.5.sp,
                                                color = TextMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Icon(
                                            imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = null,
                                            tint = if (isSelected) AccentEnergy else TextDim,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showErgUnitsSection) {
            item {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (showSpinSection) "2. CONCEPT2 PM5 & WATTBIKE — UNITS SCREEN" else "CONCEPT2 & WATTBIKE UNITS SCREEN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = TextDim,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Matches the physical 4-row Units button on Concept2 PM5 and Wattbike monitors.",
                    fontSize = 11.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
                )

                Surface(
                    color = Color(0xFF0C0F18),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2638)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Live Mini 4-Row PM5 / Wattbike Screen Preview
                            MiniErgScreenPreview(mode = concept2MetricMode)

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Concept2MetricMode.entries.forEach { mode ->
                                    val selected = concept2MetricMode == mode
                                    Surface(
                                        color = if (selected) Color(0xFF141B2D) else Color(0xFF080A10),
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(
                                            if (selected) 1.5.dp else 1.dp,
                                            if (selected) AccentEnergy else Color(0xFF1E2638)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onConcept2MetricModeChange(mode) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = mode.label,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selected) AccentEnergy else Color.White
                                            )
                                            Icon(
                                                imageVector = if (selected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                                contentDescription = null,
                                                tint = if (selected) AccentEnergy else TextDim,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Text(
                            text = "Note: RowErg & SkiErg use Split Pace (/500m) and SPM without FTP or Power Zones, since FTP applies to BikeErg and cycling bikes.",
                            fontSize = 10.sp,
                            color = TextMuted,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniSpinTilePreview(heroMetric: TileHeroMetric) {
    val heroValue = when (heroMetric) {
        TileHeroMetric.WATTS -> "215"
        TileHeroMetric.RPM -> "86"
        TileHeroMetric.FTP_PERCENT -> "98%"
    }
    val heroLabel = when (heroMetric) {
        TileHeroMetric.WATTS -> "WATTS"
        TileHeroMetric.RPM -> "RPM"
        TileHeroMetric.FTP_PERCENT -> "FTP"
    }
    val footerText = when (heroMetric) {
        TileHeroMetric.WATTS -> "86 RPM • 98%"
        TileHeroMetric.RPM -> "215 W • 98%"
        TileHeroMetric.FTP_PERCENT -> "86 RPM • 215 W"
    }

    Column(
        modifier = Modifier
            .width(106.dp)
            .background(Color(0xFF05070C), RoundedCornerShape(10.dp))
            .border(1.dp, AccentEnergy.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("PREVIEW", fontSize = 7.5.sp, fontWeight = FontWeight.Black, color = TextDim, letterSpacing = 0.8.sp)
        Text(
            text = heroValue,
            style = TightTabularNumTextStyle,
            fontFamily = Dseg7FontFamily,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            softWrap = false
        )
        Text(heroLabel, fontSize = 8.sp, fontWeight = FontWeight.Black, color = TextMuted)
        Text("#12", fontSize = 12.sp, fontWeight = FontWeight.Black, color = AccentEnergy)
        HorizontalDivider(color = Color(0xFF1A2030), thickness = 1.dp)
        Text(
            text = footerText,
            style = TabularNumTextStyle,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = TextMain,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun MiniErgScreenPreview(mode: Concept2MetricMode) {
    val (row2Val, row2Unit, row3Val, row3Unit) = when (mode) {
        Concept2MetricMode.METRES -> listOf("1:58", "/500m", "410", "m")
        Concept2MetricMode.WATTS -> listOf("245", "watt", "218", "ave")
        Concept2MetricMode.CALORIES -> listOf("1140", "Cal/hr", "184", "Cal")
    }

    Column(
        modifier = Modifier
            .width(106.dp)
            .background(Color(0xFFD7E2EA), RoundedCornerShape(8.dp))
            .border(2.dp, Color(0xFF283244), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text("1:34", style = TabularNumTextStyle, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
        HorizontalDivider(color = Color(0xFF94A3B8), thickness = 0.8.dp)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(row2Val, style = TabularNumTextStyle, fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A), maxLines = 1, softWrap = false)
            Text(row2Unit, fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155), maxLines = 1, softWrap = false)
        }
        HorizontalDivider(color = Color(0xFF94A3B8), thickness = 0.8.dp)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(row3Val, style = TabularNumTextStyle, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), maxLines = 1, softWrap = false)
            Text(row3Unit, fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155), maxLines = 1, softWrap = false)
        }
        HorizontalDivider(color = Color(0xFF94A3B8), thickness = 0.8.dp)
        Text("26 s/m", style = TabularNumTextStyle, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
    }
}
