package com.livefleet.studio.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livefleet.studio.ui.theme.*

/**
 * Full-screen 5-second countdown overlay when starting an FTP test.
 * Fills the whole screen so every rider in the studio sees the countdown.
 */
@Composable
fun FullScreenCountdownOverlay(
    countdownSeconds: Int,
    testType: String,
    scalingFactor: Float,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Top Bar inside overlay: Test info & Cancel button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(AccentEnergy, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "FTP TEST",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = BgMain
                    )
                }
                Text(
                    text = "$testType FTP (${(scalingFactor * 100).toInt()}% Factor)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E2438),
                    contentColor = Color(0xFFEF4444)
                ),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text("Cancel", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Center Content: Giant Countdown Number & Cues
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "⚡ GET READY!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = AccentEnergy,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // GIANT 7-Segment Countdown Number (Fills center screen)
            Text(
                text = "$countdownSeconds",
                fontFamily = Dseg7FontFamily,
                fontSize = 130.sp,
                fontWeight = FontWeight.Bold,
                color = if (countdownSeconds <= 2) AccentDanger else AccentEnergy,
                textAlign = TextAlign.Center,
                lineHeight = 130.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "TEST STARTS IN...",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = TextDim,
                letterSpacing = 3.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Instructions Pill
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(Color(0xFF0C0F17), RoundedCornerShape(10.dp))
                    .border(1.5.dp, AccentEnergy.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "SUGGESTED CADENCE: 80 – 90 RPM",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ramp up pedal cadence now • Settle into test gear",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentEnergy
                )
            }
        }
    }
}
