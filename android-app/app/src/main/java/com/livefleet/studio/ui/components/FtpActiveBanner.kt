package com.livefleet.studio.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livefleet.studio.model.FtpTestState
import com.livefleet.studio.ui.theme.*

@Composable
fun FtpActiveBanner(
    ftpTest: FtpTestState,
    sessionElapsedSeconds: Long,
    onFinishEarly: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isTabletOrProjector = configuration.smallestScreenWidthDp >= 600
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isPhoneLandscape = !isTabletOrProjector && isLandscape

    // STATE 1: 5-Second Preparation Countdown ("GET READY!")
    if (ftpTest.isPreparing) {
        val prepCount = ftpTest.prepCountdownSeconds

        if (isPhoneLandscape) {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .background(Color(0xFF0F0B04))
                    .border(1.5.dp, AccentEnergy)
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(AccentEnergy, RoundedCornerShape(3.dp))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "GET READY!",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = BgMain
                        )
                    }
                    Text(
                        text = "${ftpTest.testType} FTP starts in ${prepCount}s • Suggested: 80–90 RPM",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "0$prepCount",
                        fontFamily = Dseg7FontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentEnergy
                    )
                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentDanger),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 1.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text("Cancel", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // PORTRAIT & TABLET: High-visibility 5s Countdown Banner
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .background(Color(0xFF0F0B04))
                    .border(1.5.dp, AccentEnergy)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(AccentEnergy, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "GET READY!",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = BgMain
                            )
                        }
                        Text(
                            text = "${ftpTest.testType} FTP Test (${(ftpTest.scalingFactor * 100).toInt()}% Factor)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentDanger),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text("Cancel", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "STARTING IN:",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentEnergy
                        )
                        Text(
                            text = "Ramp cadence to 80–90 RPM now!",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .background(Color(0xFF1E1400), RoundedCornerShape(6.dp))
                            .border(1.dp, AccentEnergy, RoundedCornerShape(6.dp))
                            .padding(horizontal = 14.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "$prepCount",
                            fontFamily = Dseg7FontFamily,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentEnergy
                        )
                    }
                }
            }
        }
        return
    }

    // STATE 2: Active FTP Test Banner (Dual Timers: Session & Countdown)
    val ftpMins = ftpTest.remainingSeconds / 60
    val ftpSecs = ftpTest.remainingSeconds % 60
    val ftpCountdownFormatted = "%02d:%02d".format(ftpMins, ftpSecs)

    val sessionHrs = sessionElapsedSeconds / 3600
    val sessionMins = (sessionElapsedSeconds % 3600) / 60
    val sessionSecs = sessionElapsedSeconds % 60
    val sessionFormatted = if (sessionHrs > 0) {
        String.format("%02d:%02d:%02d", sessionHrs, sessionMins, sessionSecs)
    } else {
        String.format("%02d:%02d", sessionMins, sessionSecs)
    }

    val warmupMins = ftpTest.startSessionSeconds / 60
    val warmupSecs = ftpTest.startSessionSeconds % 60
    val warmupFormatted = "%02d:%02d".format(warmupMins, warmupSecs)

    if (isPhoneLandscape) {
        // PHONE LANDSCAPE: Ultra-compact single row with dual timers
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(34.dp)
                .background(Color(0xFF000000))
                .border(1.dp, Color(0xFF4F46E5))
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: FTP badge & Target Cadence
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(AccentDanger, RoundedCornerShape(3.dp))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "FTP TEST",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
                Text(
                    text = "${ftpTest.testType} (${(ftpTest.scalingFactor * 100).toInt()}%)",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "• Suggested: 80–90 RPM",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentEnergy
                )
            }

            // Center: Dual Timers (Session Time + FTP Countdown)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Session Timer Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(Color(0xFF11141E), RoundedCornerShape(4.dp))
                        .border(1.dp, Color(0xFF262D42), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = AccentSuccess,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = "SESSION",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = TextDim,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = sessionFormatted,
                        fontFamily = Dseg7FontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // FTP Countdown Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(Color(0xFF1C132B), RoundedCornerShape(4.dp))
                        .border(1.dp, AccentEnergy.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "REMAINING",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = AccentEnergy.copy(alpha = 0.85f),
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = ftpCountdownFormatted,
                        fontFamily = Dseg7FontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentEnergy
                    )
                }
            }

            // Right: Actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = onFinishEarly,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentEnergy,
                        contentColor = BgMain
                    ),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 1.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Text("Finish", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentDanger),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 1.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Text("Cancel", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        // PORTRAIT & TABLET: Dual Timers prominently displayed
        Column(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(Color(0xFF000000))
                .border(1.5.dp, Color(0xFF4F46E5))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Row 1: Active Badge, Test Name, and Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(AccentDanger, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "FTP TEST ACTIVE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "${ftpTest.testType} FTP (${(ftpTest.scalingFactor * 100).toInt()}%)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = onFinishEarly,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentEnergy,
                            contentColor = BgMain
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text("Finish", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentDanger),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text("Cancel", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Row 2: Dual Timers Bar & Target Cadence
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dual Timers Display
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Session Timer (Showing Warmup & Elapsed)
                    Column(
                        modifier = Modifier
                            .background(Color(0xFF11141E), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFF262D42), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "SESSION ELAPSED (Warmup $warmupFormatted)",
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentSuccess
                        )
                        Text(
                            text = sessionFormatted,
                            fontFamily = Dseg7FontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // FTP Countdown Clock
                    Column(
                        modifier = Modifier
                            .background(Color(0xFF1C132B), RoundedCornerShape(6.dp))
                            .border(1.dp, AccentEnergy.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "FTP INTERVAL REMAINING",
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentEnergy
                        )
                        Text(
                            text = ftpCountdownFormatted,
                            fontFamily = Dseg7FontFamily,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentEnergy
                        )
                    }
                }

                // Suggested Cadence
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "SUGGESTED CADENCE",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDim
                    )
                    Text(
                        text = "80–90 RPM",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = AccentEnergy
                    )
                }
            }
        }
    }
}
