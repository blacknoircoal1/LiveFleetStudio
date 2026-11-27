package com.livefleet.studio.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.livefleet.studio.data.RiderProfileRepository
import com.livefleet.studio.model.BikeData
import com.livefleet.studio.model.FtpZone
import com.livefleet.studio.ui.theme.*

/**
 * Tap-to-edit modal for calibrating a bike's Rider Name, FTP (Watts),
 * and optional hardware MAC -> Studio Bike # Whitelist lock.
 */
@Composable
fun RiderProfileEditDialog(
    bike: BikeData,
    zoneCount: Int,
    onDismiss: () -> Unit,
    onSave: (bikeId: Int, riderName: String, ftp: Int) -> Unit,
    onSetInstructorBike: (bike: BikeData, isInstructor: Boolean) -> Unit = { _, _ -> },
    onLockMachineBikeNumber: (macAddress: String, currentBikeId: Int, studioBikeNumber: Int) -> Unit = { _, _, _ -> },
    onUnlockMachine: (macAddress: String, currentBikeId: Int) -> Unit = { _, _ -> }
) {
    var nameInput by remember(bike.bikeId) { mutableStateOf(bike.riderName) }
    var ftpText by remember(bike.bikeId) { mutableStateOf(bike.ftp.toString()) }
    var isInstructorChecked by remember(bike.bikeId, bike.isInstructorBike) { mutableStateOf(bike.isInstructorBike) }
    var whitelistBikeInput by remember(bike.bikeId, bike.macAddress) {
        mutableStateOf(if (bike.bikeId in 1..250) bike.bikeId.toString() else "1")
    }

    val isRowOrSki = bike.equipmentType.equals("ROWERG", ignoreCase = true) ||
        bike.equipmentType.equals("SKIERG", ignoreCase = true)
    val parsedFtp = ftpText.toIntOrNull()
    val isFtpValid = isRowOrSki || (parsedFtp != null &&
        parsedFtp in RiderProfileRepository.MIN_FTP_WATTS..RiderProfileRepository.MAX_FTP_WATTS)

    val parsedWhitelistNum = whitelistBikeInput.toIntOrNull()
    val isWhitelistNumValid = parsedWhitelistNum != null && parsedWhitelistNum in 1..250

    val previewFtp = (parsedFtp ?: bike.ftp)
        .coerceIn(RiderProfileRepository.MIN_FTP_WATTS, RiderProfileRepository.MAX_FTP_WATTS)
    val previewPct = if (previewFtp > 0) (bike.powerWatts * 100) / previewFtp else 0
    val previewZone = FtpZone.fromFtpPercent(previewPct, zoneCount)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xFF0C0F18),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFF263046)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (bike.displayId.isNotEmpty()) "${bike.brand.uppercase()} ${bike.displayId}" else "BIKE #${bike.bikeId} PROFILE",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 0.6.sp
                        )
                        Text(
                            text = if (isRowOrSki) "Calibrate athlete name & Studio Machine # lock" else "Calibrate rider name, FTP & Studio Bike # lock",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }

                    // Live Zone Preview Pill (bikes only)
                    if (!isRowOrSki) {
                        Surface(
                            color = previewZone.color.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, previewZone.color),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${previewZone.code} • $previewPct%",
                                    style = TabularNumTextStyle,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = previewZone.color
                                )
                                Text(
                                    text = "${bike.powerWatts} W live",
                                    style = TabularNumTextStyle,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMain
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF1E2638), thickness = 1.dp)

                // Rider Name Input
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it.take(24) },
                    label = { Text("Rider Name", fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = AccentEnergy,
                        unfocusedBorderColor = Color(0xFF2B3448),
                        focusedLabelColor = AccentEnergy,
                        unfocusedLabelColor = TextMuted,
                        cursorColor = AccentEnergy
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // FTP (Watts) Input with -5 / +5 Steppers (bikes only; RowErg & SkiErg do not use FTP)
                if (!isRowOrSki) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "FTP (WATTS)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = TextDim,
                        letterSpacing = 0.8.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FtpStepButton("-5") {
                            val next = ((ftpText.toIntOrNull() ?: bike.ftp) - 5)
                                .coerceIn(RiderProfileRepository.MIN_FTP_WATTS, RiderProfileRepository.MAX_FTP_WATTS)
                            ftpText = next.toString()
                        }

                        OutlinedTextField(
                            value = ftpText,
                            onValueChange = { input ->
                                ftpText = input.filter { it.isDigit() }.take(3)
                            },
                            singleLine = true,
                            isError = !isFtpValid,
                            suffix = { Text("W", color = TextMuted, fontWeight = FontWeight.Bold) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = AccentEnergy,
                                unfocusedBorderColor = Color(0xFF2B3448),
                                errorBorderColor = AccentDanger,
                                cursorColor = AccentEnergy
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        FtpStepButton("+5") {
                            val next = ((ftpText.toIntOrNull() ?: bike.ftp) + 5)
                                .coerceIn(RiderProfileRepository.MIN_FTP_WATTS, RiderProfileRepository.MAX_FTP_WATTS)
                            ftpText = next.toString()
                        }
                    }

                    if (!isFtpValid) {
                        Text(
                            text = "Enter an FTP between ${RiderProfileRepository.MIN_FTP_WATTS} and ${RiderProfileRepository.MAX_FTP_WATTS} W",
                            fontSize = 10.sp,
                            color = AccentDanger
                        )
                    }

                    // Quick FTP Preset Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(120, 150, 180, 200, 250).forEach { preset ->
                            val isSelected = parsedFtp == preset
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) AccentEnergy else Color(0xFF151A26))
                                    .border(
                                        1.dp,
                                        if (isSelected) AccentEnergy else Color(0xFF252E42),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { ftpText = preset.toString() }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$preset",
                                    style = TabularNumTextStyle,
                                    fontSize = 10.5.sp,
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

                // Set as Instructor Bike Toggle
                Surface(
                    color = if (isInstructorChecked) AccentEnergy.copy(alpha = 0.10f) else Color(0xFF101522),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isInstructorChecked) AccentEnergy.copy(alpha = 0.8f) else Color(0xFF222A3D)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isInstructorChecked = !isInstructorChecked }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "★ Set as Instructor Bike",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isInstructorChecked) AccentEnergy else Color.White
                            )
                            Text(
                                text = "Highlights this bike on the screen with a gold border & badge",
                                fontSize = 9.5.sp,
                                color = TextMuted
                            )
                        }
                        Switch(
                            checked = isInstructorChecked,
                            onCheckedChange = { isInstructorChecked = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BgMain,
                                checkedTrackColor = AccentEnergy,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = BgCard
                            )
                        )
                    }
                }

                // Layer 2: Studio Bike # Hardware Whitelist Lock (when MAC / hardware ID is known)
                if (bike.macAddress.isNotBlank()) {
                    Surface(
                        color = Color(0xFF101522),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            1.dp,
                            if (bike.isWhitelisted) AccentSuccess.copy(alpha = 0.6f) else Color(0xFF222A3D)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "STUDIO BIKE # WHITELIST",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = TextDim,
                                        letterSpacing = 0.6.sp
                                    )
                                    Text(
                                        text = "HW ID: ${bike.macAddress}",
                                        style = TabularNumTextStyle,
                                        fontSize = 9.5.sp,
                                        color = TextMuted
                                    )
                                }
                                Surface(
                                    color = if (bike.isWhitelisted) AccentSuccess.copy(alpha = 0.2f) else Color(0xFF1A2132),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (bike.isWhitelisted) "LOCKED #${bike.bikeId}" else "UNLOCKED",
                                        style = TabularNumTextStyle,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (bike.isWhitelisted) AccentSuccess else TextMuted,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = whitelistBikeInput,
                                    onValueChange = { input ->
                                        whitelistBikeInput = input.filter { it.isDigit() }.take(3)
                                    },
                                    label = { Text("Studio #", fontSize = 10.sp) },
                                    singleLine = true,
                                    isError = !isWhitelistNumValid,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = AccentEnergy,
                                        unfocusedBorderColor = Color(0xFF2B3448),
                                        cursorColor = AccentEnergy
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                Button(
                                    onClick = {
                                        if (parsedWhitelistNum != null && isWhitelistNumValid) {
                                            onLockMachineBikeNumber(bike.macAddress, bike.bikeId, parsedWhitelistNum)
                                            onDismiss()
                                        }
                                    },
                                    enabled = isWhitelistNumValid,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AccentSuccess,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    Text("Lock Bike #", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                }

                                if (bike.isWhitelisted) {
                                    TextButton(
                                        onClick = {
                                            onUnlockMachine(bike.macAddress, bike.bikeId)
                                            onDismiss()
                                        }
                                    ) {
                                        Text("Unlock", fontSize = 11.sp, color = AccentDanger, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
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
                        Text("Cancel", color = TextMuted, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (parsedFtp != null && isFtpValid) {
                                onSave(bike.bikeId, nameInput, parsedFtp)
                                if (isInstructorChecked != bike.isInstructorBike) {
                                    onSetInstructorBike(bike, isInstructorChecked)
                                }
                                onDismiss()
                            }
                        },
                        enabled = isFtpValid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentEnergy,
                            contentColor = BgMain
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun FtpStepButton(label: String, onClick: () -> Unit) {
    Surface(
        color = Color(0xFF161C2B),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFF2B354B)),
        modifier = Modifier
            .height(48.dp)
            .width(44.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = AccentEnergy
            )
        }
    }
}
