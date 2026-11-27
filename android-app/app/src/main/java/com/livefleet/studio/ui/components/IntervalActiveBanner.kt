package com.livefleet.studio.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TimerOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livefleet.studio.model.IntervalState
import com.livefleet.studio.ui.theme.*

/**
 * Streamlined Keiser-style Collapsible Interval Strip rendered directly below TopBar.
 * - Standby (before Interval 1): "Intervals Ready" + green "New Interval" button.
 * - Active (Interval N running): "Interval N" + live "Watts | RPM" + "New Interval" + "End".
 * - Post-Interval (Interval N finished): "Interval N" + static "Duration | Watts | RPM" + "New Interval".
 */
@Composable
fun IntervalActiveBanner(
    intervalState: IntervalState,
    onNextLap: () -> Unit,
    onStopInterval: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isWideOrLandscape = configuration.screenWidthDp >= 600 ||
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val lastCompleted = intervalState.completedIntervals.lastOrNull()
    val isStandby = !intervalState.isActive && lastCompleted == null

    val displayIntervalNumber = if (intervalState.isActive) {
        intervalState.currentIntervalNumber
    } else {
        lastCompleted?.intervalNumber ?: 1
    }
    val displayWatts = if (intervalState.isActive) {
        intervalState.liveClassAvgWatts
    } else {
        lastCompleted?.classAvgWatts ?: 0
    }
    val displayRpm = if (intervalState.isActive) {
        intervalState.liveClassAvgCadence.toInt()
    } else {
        lastCompleted?.classAvgCadence?.toInt() ?: 0
    }

    // Static Duration shown ONLY after the current interval finishes (never duplicates live clock)
    val endedDurationText: String? = if (!intervalState.isActive && lastCompleted != null) {
        val mins = lastCompleted.durationSeconds / 60
        val secs = lastCompleted.durationSeconds % 60
        String.format("%02d:%02d", mins, secs)
    } else {
        null
    }

    Surface(
        color = Color(0xFF0A0D14),
        border = BorderStroke(1.dp, Color(0xFF1E2638)),
        modifier = modifier.fillMaxWidth()
    ) {
        if (isWideOrLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isStandby) {
                    Column {
                        Text(
                            text = "INTERVALS READY",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF38BDF8),
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "Tap New Interval when ready to begin Interval 1",
                            fontSize = 10.5.sp,
                            color = TextMuted,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Interval",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDim,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "$displayIntervalNumber",
                                style = TabularNumTextStyle,
                                fontFamily = Dseg7FontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(24.dp)
                                .background(Color(0xFF232C3F))
                        )

                        Column {
                            Text(
                                text = "Averages",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDim,
                                maxLines = 1,
                                softWrap = false
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (endedDurationText != null) {
                                    IntervalMetricInline("Duration", endedDurationText, Color.White)
                                    Text("|", color = Color(0xFF2A3448), fontSize = 10.sp)
                                }
                                IntervalMetricInline("Watts", "$displayWatts", AccentEnergy)
                                Text("|", color = Color(0xFF2A3448), fontSize = 10.sp)
                                IntervalMetricInline("RPM", "$displayRpm", Color.White)
                            }
                        }
                    }
                }

                IntervalActionButtons(
                    isActive = intervalState.isActive,
                    onNewInterval = onNextLap,
                    onEnd = onStopInterval
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isStandby) {
                        Text(
                            text = "INTERVALS READY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF38BDF8),
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "Warmup active",
                            fontSize = 9.5.sp,
                            color = TextMuted,
                            maxLines = 1,
                            softWrap = false
                        )
                    } else {
                        Text(
                            text = "Interval $displayIntervalNumber",
                            style = TabularNumTextStyle,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (endedDurationText != null) {
                                IntervalMetricInline("Duration", endedDurationText, Color.White)
                                Text("|", color = Color(0xFF2A3448), fontSize = 9.sp)
                            }
                            IntervalMetricInline("Watts", "$displayWatts", AccentEnergy)
                            Text("|", color = Color(0xFF2A3448), fontSize = 9.sp)
                            IntervalMetricInline("RPM", "$displayRpm", Color.White)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IntervalActionButtons(
                        isActive = intervalState.isActive,
                        onNewInterval = onNextLap,
                        onEnd = onStopInterval
                    )
                }
            }
        }
    }
}

@Composable
private fun IntervalMetricInline(label: String, value: String, valueColor: Color) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            maxLines = 1,
            softWrap = false
        )
        Text(
            text = value,
            style = TabularNumTextStyle,
            fontFamily = Dseg7FontFamily,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun IntervalActionButtons(
    isActive: Boolean,
    onNewInterval: () -> Unit,
    onEnd: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onNewInterval,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2E7D47),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(999.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
            modifier = Modifier.height(28.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Timer,
                contentDescription = null,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "New Interval",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }

        if (isActive) {
            Button(
                onClick = onEnd,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF9E2A2B),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(999.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.TimerOff,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "End",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
