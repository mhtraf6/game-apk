package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryGold,
    secondary = SupportTeal,
    tertiary = GoldenAccent,
    background = DarkBg,
    surface = DarkSurface,
    onPrimary = Color(0xFF0F111A),
    onSecondary = TextWhite,
    onTertiary = Color(0xFF0F111A),
    onBackground = TextWhite,
    onSurface = TextWhite,
    primaryContainer = DarkGoldContainer,
    error = IncorrectRed,
    surfaceVariant = LightCard
)

// Fallback Light Color Scheme using gold variations
private val LightColorScheme = lightColorScheme(
    primary = GoldenAccent,
    secondary = SupportTeal,
    tertiary = PrimaryGold,
    background = Color(0xFFF7F8FA),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1C1D24),
    onSurface = Color(0xFF1C1D24),
    primaryContainer = Color(0xFFFEF9E7),
    error = IncorrectRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme for a premium gaming experience
    dynamicColor: Boolean = false, // Use our handcrafted colors instead of system colors
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
