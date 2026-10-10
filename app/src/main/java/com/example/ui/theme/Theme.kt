package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AthleticOrange,
    onPrimary = PureBlack,
    primaryContainer = AthleticOrangeDark,
    onPrimaryContainer = PureWhite,
    secondary = AthleticGold,
    onSecondary = PureBlack,
    secondaryContainer = AthleticGoldDark,
    onSecondaryContainer = PureBlack,
    tertiary = AthleticGreen,
    onTertiary = PureBlack,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    error = ErrorRed,
    onError = PureWhite
)

private val LightColorScheme = lightColorScheme(
    primary = AthleticOrangeDark,
    onPrimary = PureWhite,
    primaryContainer = AthleticOrangeLight,
    onPrimaryContainer = PureBlack,
    secondary = AthleticGoldDark,
    onSecondary = PureBlack,
    secondaryContainer = AthleticGoldLight,
    onSecondaryContainer = PureBlack,
    tertiary = AthleticGreenDark,
    onTertiary = PureWhite,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    error = ErrorRed,
    onError = PureWhite
)

@Composable
fun PushUpReelTheme(
    darkTheme: Boolean = true, // Default to sleek dark gym athletic theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
