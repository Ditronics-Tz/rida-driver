package com.example.ridedriver.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RidaLightColorScheme = lightColorScheme(
    primary = RidaMintDark,
    onPrimary = Color.White,
    primaryContainer = RidaMintDark,
    secondary = RidaDark,
    background = RidaLightBg,
    surface = RidaLightBg,
    surfaceVariant = RidaInputBg,
    onBackground = RidaTitleDark,
    onSurface = RidaTitleDark,
    outline = RidaBorder
)

private val RidaDarkColorScheme = darkColorScheme(
    primary = RidaMint,
    onPrimary = RidaDark,
    background = RidaOnboardingBg,
    surface = RidaOnboardingBg,
    onBackground = Color.White,
    onSurface = Color.White,
    outline = RidaSubtleOnboarding
)

@Composable
fun RidaDriverTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Force light for auth screens, but allow dark for onboarding via override.
    // We simply use light scheme by default; individual screens set bg explicitly.
    val scheme = if (darkTheme) RidaDarkColorScheme else RidaLightColorScheme
    MaterialTheme(
        colorScheme = scheme,
        typography = RidaTypography,
        content = content
    )
}

// For screens that need explicitly light or dark without system dependence
@Composable
fun RidaLightTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RidaLightColorScheme,
        typography = RidaTypography,
        content = content
    )
}

@Composable
fun RidaDarkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RidaDarkColorScheme,
        typography = RidaTypography,
        content = content
    )
}
