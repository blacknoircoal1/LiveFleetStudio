package com.keiser.studio.ui.theme

import androidx.compose.ui.graphics.Color

val BgMain = Color(0xFF000000)          // Pure OLED pitch black
val BgCard = Color(0xFF0A0C12)          // Deep black card surface
val BgCardHover = Color(0xFF121620)
val BgHeader = Color(0xFF000000)        // Pure black header
val BorderColor = Color(0xFF1E2433)     // Crisp subtle dark border

val TextMain = Color(0xFFF8FAFC)
val TextMuted = Color(0xFF94A3B8)
val TextDim = Color(0xFF64748B)

val AccentPrimary = Color(0xFF3B82F6)
val AccentEnergy = Color(0xFFF59E0B)    // Keiser Amber/Gold
val AccentDanger = Color(0xFFEF4444)
val AccentSuccess = Color(0xFF10B981)

// FTP Zone Colors (Keiser 5-zone power tachometer)
val Zone1Color = Color(0xFF6B7280) // Recovery <55% (Gray)
val Zone2Color = Color(0xFF00A3E0) // Endurance 56-75% (Cyan/Blue)
val Zone3Color = Color(0xFF00C853) // Tempo 76-90% (Vibrant Green)
val Zone4Color = Color(0xFFFF9100) // Threshold 91-105% (Amber/Orange)
val Zone5Color = Color(0xFFD50000) // VO2 Max >105% (Red)
