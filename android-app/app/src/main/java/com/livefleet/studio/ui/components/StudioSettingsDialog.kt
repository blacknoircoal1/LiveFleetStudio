package com.livefleet.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.livefleet.studio.model.BikeProtocol
import com.livefleet.studio.ui.theme.*

@Composable
fun StudioSettingsDialog(
    selectedProtocol: BikeProtocol,
    zoneCount: Int,
    onSelectProtocol: (BikeProtocol) -> Unit,
    onSelectZoneCount: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0A0C14),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262D42)),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.90f)
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header: Title & Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AccentEnergy),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = BgMain,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "STUDIO FLEET SETTINGS",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = TextMain,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Universal Bike Protocol & Fleet Configuration",
                                fontSize = 9.sp,
                                color = TextMuted
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Section 1: Power Training Zone Model
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF11141E), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF1E2230), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TRAINING ZONE MODEL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = AccentEnergy
                        )
                        Text(
                            text = if (zoneCount == 7) "7 Zones (Coggan / Keiser Sprint Model)" else "5 Zones (Standard Keiser Ride By Color)",
                            fontSize = 9.sp,
                            color = TextMuted
                        )
                    }

                    Row(
                        modifier = Modifier
                            .background(Color(0xFF0C0F17), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFF262D42), RoundedCornerShape(6.dp))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        listOf(5 to "5 Zones", 7 to "7 Zones").forEach { (count, label) ->
                            val isSelected = zoneCount == count
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isSelected) AccentEnergy else Color.Transparent)
                                    .clickable { onSelectZoneCount(count) }
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    color = if (isSelected) BgMain else TextMuted
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 2: Bike Fleet Protocol List
                Text(
                    text = "SUPPORTED BIKE BRANDS & PROTOCOLS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = TextDim,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Select your fleet brand or choose 'Auto-Detect' to passively read all broadcast bikes simultaneously.",
                    fontSize = 9.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(BikeProtocol.entries, key = { it.id }) { protocol ->
                        val isSelected = selectedProtocol == protocol
                        ProtocolCard(
                            protocol = protocol,
                            isSelected = isSelected,
                            onSelect = { onSelectProtocol(protocol) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Done Action
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                ) {
                    Text(
                        text = "Save & Apply Fleet Settings",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ProtocolCard(
    protocol: BikeProtocol,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        color = if (isSelected) Color(0xFF141926) else Color(0xFF0F121C),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) AccentEnergy else Color(0xFF1E2638)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) AccentEnergy else TextDim,
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = 2.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = protocol.displayName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else TextMain
                )

                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = protocol.subtitle,
                    fontSize = 10.sp,
                    color = TextMuted,
                    lineHeight = 13.sp
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Supported: ${protocol.supportedModels}",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) AccentSuccess else TextDim
                )
            }
        }
    }
}
