package com.fopgang.umpooling.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Color.Black, onPrimary = Color.White,
    secondary = Nutmeg, onSecondary = Color.White,
    tertiary = Apricot, background = Peach, onBackground = Ink,
    surface = Color(0xFFFFF9F6), onSurface = Ink,
    surfaceVariant = SoftCoral, onSurfaceVariant = Muted,
    outline = Color(0xFFE6BFB2), error = Color(0xFFB63723)
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB59D), onPrimary = Color(0xFF35190F),
    secondary = Color(0xFFE8B9A3), onSecondary = Color(0xFF302018),
    tertiary = Color(0xFFF4C19E), background = Color(0xFF171210), onBackground = Color(0xFFFFF1E9),
    surface = Color(0xFF241B17), onSurface = Color(0xFFFFF1E9),
    surfaceVariant = Color(0xFF3D2B24), onSurfaceVariant = Color(0xFFE2C8BA),
    outline = Color(0xFFAA8978), error = Color(0xFFFFB4A5), onError = Color(0xFF5C1B0E)
)

@Composable
fun UMPoolingTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalBrandPalette provides BrandPalette(darkTheme)) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors, typography = Typography,
            shapes = Shapes(small = RoundedCornerShape(16.dp), medium = RoundedCornerShape(22.dp), large = RoundedCornerShape(28.dp)),
            content = content
        )
    }
}
