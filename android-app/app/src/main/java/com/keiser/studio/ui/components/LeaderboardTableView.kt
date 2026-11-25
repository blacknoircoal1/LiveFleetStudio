package com.keiser.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
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
    onBikeClick: (BikeData) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isLargeDisplay = LocalConfiguration.current.smallestScreenWidthDp >= 600
    val headerFontSize = if (isLargeDisplay) 15.sp else 11.sp
    val valueFontSize = if (isLargeDisplay) 24.sp else 14.sp
    val zoneFontSize = if (isLargeDisplay) 15.sp else 10.sp
    val rowVerticalPadding = if (isLargeDisplay) 12.dp else 7.dp

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
                .padding(horizontal = 10.dp, vertical = if (isLargeDisplay) 10.dp else 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ZONE", fontSize = headerFontSize, fontWeight = FontWeight.Black, color = TextMuted, modifier = Modifier.weight(0.65f))
            Text("BIKE", fontSize = headerFontSize, fontWeight = FontWeight.Black, color = TextMuted, modifier = Modifier.weight(0.85f))
            Text("POWER", fontSize = headerFontSize, fontWeight = FontWeight.Black, color = TextMuted, modifier = Modifier.weight(1.0f))
            Text("RPM", fontSize = headerFontSize, fontWeight = FontWeight.Black, color = TextMuted, modifier = Modifier.weight(0.8f))
            Text("FTP %", fontSize = headerFontSize, fontWeight = FontWeight.Black, color = TextMuted, modifier = Modifier.weight(1.05f))
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Table Rows
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(if (isLargeDisplay) 4.dp else 2.dp)
        ) {
            itemsIndexed(bikes, key = { _, b -> b.bikeId }) { rank, bike ->
                val zoneColor = bike.ftpZone.color
                val zoneCode = bike.ftpZone.code
                val isCadenceAlert = isFtpTestActive && (bike.cadenceRpm < 80f || bike.cadenceRpm > 90f)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (rank % 2 == 0) Color(0xFF05070B) else Color(0xFF0A0C12), RoundedCornerShape(4.dp))
                        .clickable { onBikeClick(bike) }
                        .padding(horizontal = 10.dp, vertical = rowVerticalPadding),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Zone Badge
                    Box(
                        modifier = Modifier
                            .weight(0.65f)
                            .background(zoneColor, RoundedCornerShape(3.dp))
                            .padding(horizontal = 4.dp, vertical = if (isLargeDisplay) 3.dp else 1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(zoneCode, fontSize = zoneFontSize, fontWeight = FontWeight.Black, color = Color.White, maxLines = 1, softWrap = false)
                    }

                    // Bike / PM5 ID
                    Text(
                        text = bike.displayId.ifEmpty { "#${bike.bikeId}" },
                        fontSize = valueFontSize,
                        fontWeight = FontWeight.Bold,
                        color = AccentEnergy,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.weight(0.85f)
                    )

                    // Power Watts
                    Text(
                        text = "${bike.powerWatts} W",
                        fontSize = valueFontSize,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.weight(1.0f)
                    )

                    // Cadence RPM
                    if (isCadenceAlert) {
                        Box(
                            modifier = Modifier
                                .weight(0.8f)
                                .background(AccentDanger, RoundedCornerShape(3.dp))
                                .padding(horizontal = 4.dp, vertical = if (isLargeDisplay) 3.dp else 1.dp)
                        ) {
                            Text(
                                text = "${bike.cadenceRpm.toInt()} ⚠️",
                                fontSize = valueFontSize,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    } else {
                        Text(
                            text = "${bike.cadenceRpm.toInt()}",
                            fontSize = valueFontSize,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE2E8F0),
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.weight(0.8f)
                        )
                    }

                    // FTP %
                    val ftpText = "${bike.ftpPercent}%"
                    Text(
                        text = ftpText,
                        fontFamily = Dseg7FontFamily,
                        fontSize = if (ftpText.length >= 5) (valueFontSize.value * 0.82f).sp else valueFontSize,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.weight(1.05f)
                    )
                }
            }
        }
    }
}
