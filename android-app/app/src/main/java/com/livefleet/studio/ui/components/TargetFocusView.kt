package com.livefleet.studio.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livefleet.studio.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * Instructor Target Focus View matching the Bottom-Right quadrant of Ride by Color.
 * Features a giant central target tachometer and 5 zone cue buttons.
 */
@Composable
fun TargetFocusView(
    targetZone: Int,
    avgWatts: Int,
    avgCadence: Float,
    onSelectZone: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val zoneColors = listOf(Zone1Color, Zone2Color, Zone3Color, Zone4Color, Zone5Color)
    val zoneNames = listOf("RECOVERY", "ENDURANCE", "TEMPO", "THRESHOLD", "VO2 MAX")
    val currentZoneColor = zoneColors.getOrElse(targetZone - 1) { Zone3Color }
    val currentZoneName = zoneNames.getOrElse(targetZone - 1) { "TEMPO" }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Class Average Quick Bar
        Row(
            modifier = Modifier
                .background(Color(0xFF0C0E14), RoundedCornerShape(6.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(6.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("CLASS AVERAGE:", fontSize = 11.sp, fontWeight = FontWeight.Black, color = TextDim)
            Text("$avgWatts WATTS", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("•", color = BorderColor, fontSize = 10.sp)
            Text("${avgCadence.toInt()} RPM", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMuted)
        }

        // Giant Central Tachometer Dial
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                val centerX = canvasWidth / 2f
                val centerY = canvasHeight * 0.85f

                val arcRadius = (canvasWidth * 0.40f).coerceAtMost(canvasHeight * 0.72f)
                val arcStrokeWidth = 24.dp.toPx()
                val arcRect = androidx.compose.ui.geometry.Rect(
                    left = centerX - arcRadius,
                    top = centerY - arcRadius,
                    right = centerX + arcRadius,
                    bottom = centerY + arcRadius
                )

                val zones = listOf(
                    Pair(180f, KeiserZone1),
                    Pair(216f, KeiserZone2),
                    Pair(252f, KeiserZone3),
                    Pair(288f, KeiserZone4),
                    Pair(324f, KeiserZone5)
                )

                zones.forEachIndexed { index, (startAngle, color) ->
                    val isTarget = (index + 1) == targetZone
                    val isBelowOrAt = (index + 1) <= targetZone
                    val segColor = if (isBelowOrAt) color else color.copy(alpha = 0.2f)

                    drawArc(
                        color = segColor,
                        startAngle = startAngle,
                        sweepAngle = 36f,
                        useCenter = false,
                        topLeft = arcRect.topLeft,
                        size = arcRect.size,
                        style = Stroke(width = arcStrokeWidth, cap = StrokeCap.Butt)
                    )
                }

                // Dark Inner Well
                val innerRadius = arcRadius - (arcStrokeWidth / 2f) - 2.dp.toPx()
                drawCircle(
                    color = Color(0xFF04060A),
                    radius = innerRadius,
                    center = Offset(centerX, centerY)
                )
            }

            // Big Central Zone Number & Label
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Zone",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8)
                )
                Text(
                    text = "$targetZone",
                    fontFamily = Dseg7FontFamily,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Black,
                    color = currentZoneColor,
                    lineHeight = 54.sp
                )
                Text(
                    text = "$currentZoneName (RPE)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            }
        }

        // Zone Selection Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (z in 1..5) {
                val zColor = zoneColors[z - 1]
                val isSelected = z == targetZone
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .background(
                            if (isSelected) zColor else Color(0xFF0A0C14),
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) Color.White else BorderColor,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelectZone(z) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ZONE $z",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isSelected) Color.White else TextMuted
                    )
                }
            }
        }
    }
}
