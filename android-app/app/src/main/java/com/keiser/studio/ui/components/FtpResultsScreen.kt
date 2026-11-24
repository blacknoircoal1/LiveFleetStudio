package com.keiser.studio.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keiser.studio.model.FtpResultItem
import com.keiser.studio.ui.theme.*

/**
 * Full-screen FTP Test Results view where ALL bikes are visible on one screen
 * without scrolling. Clean, fresh-start approach with no saving or previous buttons.
 */
@Composable
fun FtpResultsScreen(
    results: List<FtpResultItem>,
    testType: String,
    scalingFactor: Float,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isTabletOrProjector = configuration.smallestScreenWidthDp >= 600

    val classAvgWatts = if (results.isNotEmpty()) results.map { it.avgWatts }.average().toInt() else 0
    val classAvgFtp = if (results.isNotEmpty()) results.map { it.newFtp }.average().toInt() else 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .statusBarsPadding()
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Top Header: Responsive for portrait phone vs landscape/tablet
        if (!isLandscape && !isTabletOrProjector) {
            // PORTRAIT PHONE: 2 clean lines so Done button is ALWAYS visible and numbers NEVER wrap vertically!
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0C0F17), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF262D42), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // Line 1: Title & Badge on Left, Done Button on Right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "FTP TEST RESULTS",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Box(
                            modifier = Modifier
                                .background(AccentEnergy, RoundedCornerShape(3.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = testType,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = BgMain
                            )
                        }
                    }

                    // Done Button - prominent and always accessible
                    Button(
                        onClick = onDone,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentEnergy,
                            contentColor = BgMain
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = "Done",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Line 2: Session Subtitle & Class Averages Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF141926), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Keiser Scaling: ${(scalingFactor * 100).toInt()}%",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDim,
                        softWrap = false,
                        maxLines = 1
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "CLASS AVG:",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = TextDim,
                            softWrap = false,
                            maxLines = 1
                        )
                        Text(
                            text = "$classAvgWatts",
                            fontFamily = Dseg7FontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            softWrap = false,
                            maxLines = 1
                        )
                        Text(
                            text = "W",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDim,
                            softWrap = false,
                            maxLines = 1
                        )
                        Text("•", color = TextDim, fontSize = 8.sp)
                        Text(
                            text = "FTP:",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = TextDim,
                            softWrap = false,
                            maxLines = 1
                        )
                        Text(
                            text = "$classAvgFtp",
                            fontFamily = Dseg7FontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentEnergy,
                            softWrap = false,
                            maxLines = 1
                        )
                        Text(
                            text = "W",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentEnergy,
                            softWrap = false,
                            maxLines = 1
                        )
                    }
                }
            }
        } else {
            // LANDSCAPE & TABLET: Single wide horizontal line
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0C0F17), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF262D42), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Title
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "FTP TEST RESULTS",
                            fontSize = if (isTabletOrProjector) 16.sp else 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Box(
                            modifier = Modifier
                                .background(AccentEnergy, RoundedCornerShape(3.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = testType,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = BgMain
                            )
                        }
                    }
                    Text(
                        text = "Keiser Scaling Factor: ${(scalingFactor * 100).toInt()}% • Session Complete",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDim
                    )
                }

                // Center: Class Averages
                Row(
                    modifier = Modifier
                        .background(Color(0xFF141926), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CLASS AVG:",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = TextDim,
                        softWrap = false,
                        maxLines = 1
                    )
                    Text(
                        text = "$classAvgWatts",
                        fontFamily = Dseg7FontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        softWrap = false,
                        maxLines = 1
                    )
                    Text(
                        text = "W",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDim,
                        softWrap = false,
                        maxLines = 1
                    )
                    Text("•", color = TextDim, fontSize = 8.sp)
                    Text(
                        text = "FTP:",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = TextDim,
                        softWrap = false,
                        maxLines = 1
                    )
                    Text(
                        text = "$classAvgFtp",
                        fontFamily = Dseg7FontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentEnergy,
                        softWrap = false,
                        maxLines = 1
                    )
                    Text(
                        text = "W",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentEnergy,
                        softWrap = false,
                        maxLines = 1
                    )
                }

                // Right: Done Button
                Button(
                    onClick = onDone,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentEnergy,
                        contentColor = BgMain
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text(
                        text = "Done",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // All Bikes Fitted on the Screen with Zero Scrolling
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val count = results.size
            val (columns, rows) = calculateOptimalGrid(count, isLandscape)
            val isCompact = rows >= 4 || (isLandscape && rows >= 3) || count >= 12

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (r in 0 until rows) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (c in 0 until columns) {
                            val index = r * columns + c
                            if (index < count) {
                                val item = results[index]
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                ) {
                                    SingleBikeResultCard(
                                        result = item,
                                        isCompact = isCompact,
                                        modifier = Modifier.fillMaxSize()
                                    )
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
}

/**
 * Compact Single Bike Result Card designed to fit in a non-scrolling matrix.
 */
@Composable
private fun SingleBikeResultCard(
    result: FtpResultItem,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    val deltaColor = if (result.delta >= 0) AccentSuccess else AccentDanger
    val deltaSign = if (result.delta >= 0) "+${result.delta}" else "${result.delta}"

    Column(
        modifier = modifier
            .background(Color(0xFF0A0D15), RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFF1E2638), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top: Bike Number (Prominent!)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#${result.bikeId}",
                fontSize = if (isCompact) 14.sp else 17.sp,
                fontWeight = FontWeight.Black,
                color = AccentEnergy
            )
            // Delta Pill
            Box(
                modifier = Modifier
                    .background(deltaColor.copy(alpha = 0.2f), RoundedCornerShape(3.dp))
                    .border(1.dp, deltaColor.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "$deltaSign W",
                    fontSize = if (isCompact) 8.sp else 10.sp,
                    fontWeight = FontWeight.Black,
                    color = deltaColor
                )
            }
        }

        // Center: New Calculated FTP
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "NEW FTP",
                fontSize = if (isCompact) 7.sp else 8.5.sp,
                fontWeight = FontWeight.Black,
                color = TextDim,
                letterSpacing = 0.5.sp
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "${result.newFtp}",
                    fontFamily = Dseg7FontFamily,
                    fontSize = if (isCompact) 16.sp else 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "W",
                    fontSize = if (isCompact) 9.sp else 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDim,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }

        // Bottom: Sustained Test Average
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TEST AVG",
                fontSize = if (isCompact) 6.5.sp else 8.sp,
                fontWeight = FontWeight.Bold,
                color = TextDim
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "${result.avgWatts}",
                    fontFamily = Dseg7FontFamily,
                    fontSize = if (isCompact) 10.sp else 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )
                Text(
                    text = "W",
                    fontSize = if (isCompact) 7.sp else 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDim,
                    modifier = Modifier.padding(bottom = 1.dp)
                )
            }
        }
    }
}

/**
 * Calculates (columns, rows) such that all [count] items fit on one screen without scrolling.
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
