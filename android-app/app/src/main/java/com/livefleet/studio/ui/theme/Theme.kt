package com.livefleet.studio.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AccentPrimary,
    secondary = AccentEnergy,
    tertiary = AccentSuccess,
    background = BgMain,
    surface = BgCard,
    onPrimary = TextMain,
    onSecondary = BgMain,
    onBackground = TextMain,
    onSurface = TextMain
)

@Composable
fun LiveFleetStudioTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
