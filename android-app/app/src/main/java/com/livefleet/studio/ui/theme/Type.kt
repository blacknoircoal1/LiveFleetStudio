package com.livefleet.studio.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.livefleet.studio.R

val Dseg7FontFamily = FontFamily(
    Font(R.font.dseg7_classic_bold, FontWeight.Bold),
    Font(R.font.dseg7_classic_bold, FontWeight.Normal)
)

/**
 * Shared TextStyle enforcing tabular monospace figures (`tnum`) so live telemetry
 * numbers never jitter horizontally during rapid updates.
 */
val TabularNumTextStyle = TextStyle(fontFeatureSettings = "tnum")

/**
 * Tabular figures with Android's extra vertical font padding stripped (`includeFontPadding = false`)
 * so tight console tiles and baseline-aligned unit labels fit cleanly without vertical bleed.
 */
val TightTabularNumTextStyle = TabularNumTextStyle.merge(
    TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
)

val Typography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Black,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 20.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 18.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp
    )
)
