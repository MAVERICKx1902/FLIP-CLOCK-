package com.example.liquidprismclock.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Type scale. The clock digits themselves are drawn in Canvas (not Text) so
 * they can carry liquid/chromatic effects; these styles cover labels, date and
 * the Material3 settings surfaces.
 */
val LiquidTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Light,
        fontSize = 96.sp,
        letterSpacing = (-2).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        letterSpacing = 0.4f.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        letterSpacing = 0.2f.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.3f.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 1.5f.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        letterSpacing = 0.8f.sp,
    ),
)
