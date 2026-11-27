package com.livefleet.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.livefleet.studio.model.FtpResultItem
import com.livefleet.studio.ui.theme.*

@Composable
fun FtpResultsDialog(
    results: List<FtpResultItem>,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = BgCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column {
                    Text(
                        text = "Group FTP Test Results",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "Calculated using Keiser Scaling Factor • Zones Ready to Unlock",
                        fontSize = 11.sp,
                        color = AccentEnergy
                    )
                }

                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF000000), RoundedCornerShape(6.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("BIKE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = TextMuted, modifier = Modifier.weight(1f))
                    Text("AVG POWER", fontSize = 10.sp, fontWeight = FontWeight.Black, color = TextMuted, modifier = Modifier.weight(1.2f))
                    Text("NEW FTP", fontSize = 10.sp, fontWeight = FontWeight.Black, color = TextMuted, modifier = Modifier.weight(1.2f))
                    Text("DELTA", fontSize = 10.sp, fontWeight = FontWeight.Black, color = TextMuted, modifier = Modifier.weight(1f))
                }

                // Results List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(results) { res ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF000000), RoundedCornerShape(4.dp))
                                .border(1.dp, BorderColor, RoundedCornerShape(4.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Bike #${res.bikeId}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentEnergy, modifier = Modifier.weight(1f))
                            Text("${res.avgWatts} W", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1.2f))
                            Text("${res.newFtp} W", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.weight(1.2f))
                            
                            val deltaColor = if (res.delta >= 0) AccentSuccess else AccentDanger
                            val deltaSign = if (res.delta >= 0) "+${res.delta}" else "${res.delta}"
                            Text(deltaSign, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = deltaColor, modifier = Modifier.weight(1f))
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Keep Previous", color = TextMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onApply,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentEnergy,
                            contentColor = BgMain
                        )
                    ) {
                        Text("Apply & Unlock Zones", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
