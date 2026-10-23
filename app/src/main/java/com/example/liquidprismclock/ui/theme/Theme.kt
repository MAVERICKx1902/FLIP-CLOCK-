package com.example.liquidprismclock.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LiquidDarkColorScheme = darkColorScheme(
    primary = PrismViolet,
    onPrimary = OledBlack,
    secondary = PrismBlue,
    onSecondary = OledBlack,
    tertiary = PrismCyan,
    background = OledBlack,
    onBackground = TextPrimary,
    surface = DeepNight,
    onSurface = TextPrimary,
    surfaceVariant = GlassSurface,
    onSurfaceVariant = TextSecondary,
    outline = GlassStroke,
)

/**
 * App theme. The clock chrome is OLED black; the animated PrismPalette is
 * applied directly inside the custom Canvas composables rather than through
 * Material color roles.
 */
@Composable
fun LiquidPrismTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LiquidDarkColorScheme,
        typography = LiquidTypography,
        content = content,
    )
}
