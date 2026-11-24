package com.keiser.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keiser.studio.model.BikeData
import com.keiser.studio.ui.theme.*

/**
 * High-density Table View matching the Top-Right quadrant of the Ride by Color reference image.
 */
@Composable
fun LeaderboardTableView(
    bikes: List<BikeData>,
    isFtpTestActive: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        // Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0C0E14), RoundedCornerShape(4.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(4.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ZONE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = TextMuted, modifier = Modifier.weight(0.7f))
            Text("BIKE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = TextMuted, modifier = Modifier.weight(0.8f))
            Text("POWER", fontSize = 10.sp, fontWeight = FontWeight.Black, color = TextMuted, modifier = Modifier.weight(1.0f))
            Text("RPM", fontSize = 10.sp, fontWeight = FontWeight.Black, color = TextMuted, modifier = Modifier.weight(0.9f))
            Text("FTP %", fontSize = 10.sp, fontWeight = FontWeight.Black, color = TextMuted, modifier = Modifier.weight(0.9f))
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Table Rows
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            itemsIndexed(bikes, key = { _, b -> b.bikeId }) { rank, bike ->
                val zoneColor = bike.ftpZone.color
                val zoneCode = bike.ftpZone.code
                val isCadenceAlert = isFtpTestActive && (bike.cadenceRpm < 80f || bike.cadenceRpm > 90f)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (rank % 2 == 0) Color(0xFF05070B) else Color(0xFF0A0C12), RoundedCornerShape(4.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Zone Badge
                    Box(
                        modifier = Modifier
                            .weight(0.7f)
                            .background(zoneColor, RoundedCornerShape(3.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(zoneCode, fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }

                    // Bike Number
                    Text(
                        text = "#${bike.bikeId}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentEnergy,
                        modifier = Modifier.weight(0.8f)
                    )

                    // Power Watts
                    Text(
                        text = "${bike.powerWatts} W",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        modifier = Modifier.weight(1.0f)
                    )

                    // Cadence RPM
                    if (isCadenceAlert) {
                        Box(
                            modifier = Modifier
                                .weight(0.9f)
                                .background(AccentDanger, RoundedCornerShape(3.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "${bike.cadenceRpm.toInt()} ⚠️",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    } else {
                        Text(
                            text = "${bike.cadenceRpm.toInt()}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            modifier = Modifier.weight(0.9f)
                        )
                    }

                    // FTP %
                    Text(
                        text = "${bike.ftpPercent}%",
                        fontFamily = Dseg7FontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(0.9f)
                    )
                }
            }
        }
    }
}
