package com.sxybyte.mybattery.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AppColorScheme = darkColorScheme(
    primary = NeonBlue,
    secondary = ElectricCyan,
    tertiary = ElectricCyan,
    background = DeepNavy,
    surface = PanelBlue,
    surfaceVariant = NightBlue,
    onPrimary = DeepNavy,
    onSecondary = DeepNavy,
    onBackground = MistWhite,
    onSurface = MistWhite,
    onSurfaceVariant = SoftSlate,
    outline = SoftSlate,
)

@Composable
fun MyBatteryTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content
    )
}
