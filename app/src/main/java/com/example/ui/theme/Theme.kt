package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = HabitViolet,
    onPrimary = Color.White,
    primaryContainer = HabitPurpleDark,
    onPrimaryContainer = HabitVioletLight,
    secondary = HabitCyan,
    onSecondary = Color.Black,
    tertiary = HabitGold,
    onTertiary = Color.Black,
    background = DarkNavyBackground,
    onBackground = Color(0xFFF1F1F6),
    surface = DarkNavySurface,
    onSurface = Color(0xFFF1F1F6),
    surfaceVariant = DarkNavySurfaceVariant,
    onSurfaceVariant = Color(0xFFCBC9DE),
    outline = DarkNavyBorder
)

private val LightColorScheme = lightColorScheme(
    primary = HabitPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = HabitPurpleDark,
    secondary = HabitCyan,
    onSecondary = Color.White,
    tertiary = HabitGold,
    onTertiary = Color.Black,
    background = LightBackground,
    onBackground = Color(0xFF1E1B4B),
    surface = LightSurface,
    onSurface = Color(0xFF1E1B4B),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1)
)

@Composable
fun HabitFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
