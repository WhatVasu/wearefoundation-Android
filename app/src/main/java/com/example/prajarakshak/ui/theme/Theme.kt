package com.example.prajarakshak.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA8C5FF),
    onPrimary = Color(0xFF102E5B),
    primaryContainer = Color(0xFF244777),
    onPrimaryContainer = Color(0xFFD8E5FF),
    secondary = Color(0xFF83D2D1),
    onSecondary = Color(0xFF003737),
    secondaryContainer = Color(0xFF164B4B),
    onSecondaryContainer = Color(0xFFBDECE9),
    tertiary = Color(0xFFFFC58C),
    onTertiary = Color(0xFF482900),
    tertiaryContainer = Color(0xFF684117),
    onTertiaryContainer = Color(0xFFFFDDB8),
    background = Color(0xFF101923),
    onBackground = Color(0xFFE1E9F2),
    surface = Color(0xFF172330),
    onSurface = Color(0xFFE1E9F2),
    surfaceVariant = Color(0xFF293746),
    onSurfaceVariant = Color(0xFFBBC8D6),
    outline = Color(0xFF8493A2)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF173B73),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD9E6FF),
    onPrimaryContainer = Color(0xFF102E5B),
    secondary = Color(0xFF087E83),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD2F0EE),
    onSecondaryContainer = Color(0xFF073F42),
    tertiary = Color(0xFF9A5A10),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDDB8),
    onTertiaryContainer = Color(0xFF633800),
    background = Color(0xFFF4F7FB),
    onBackground = Color(0xFF182638),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF182638),
    surfaceVariant = Color(0xFFE9EFF6),
    onSurfaceVariant = Color(0xFF4F6073),
    outline = Color(0xFF8998A8)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun PrajaRakshakTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}