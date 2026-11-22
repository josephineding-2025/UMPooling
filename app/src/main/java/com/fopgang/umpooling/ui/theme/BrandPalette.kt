package com.fopgang.umpooling.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle

/** Semantic adaptations of the supplied light design, never a pixel inversion. */
data class BrandPalette(val dark: Boolean = false) {
    fun surface(light: Color): Color {
        if (!dark || light == Color.Unspecified || light.alpha == 0f) return light
        val solid = light.copy(alpha = 1f)
        val adapted = if (solid.luminance() < .16f) lerp(solid, Color(0xFF30231F), .35f)
            else lerp(solid, Color(0xFF171210), .90f)
        return adapted.copy(alpha = light.alpha)
    }
    fun ink(light: Color): Color {
        if (!dark || light == Color.Unspecified || light.alpha == 0f) return light
        val solid = light.copy(alpha = 1f)
        val spread = maxOf(solid.red, solid.green, solid.blue) - minOf(solid.red, solid.green, solid.blue)
        val adapted = when {
            solid.luminance() > .85f -> Color(0xFFFFF7F2)
            spread > .18f -> lerp(solid, Color.White, .62f)
            solid.luminance() < .08f -> Color(0xFFFFF1E9)
            else -> Color(0xFFE2C8BA)
        }
        return adapted.copy(alpha = light.alpha)
    }
    fun outline(light: Color): Color =
        if (!dark || light == Color.Unspecified || light.alpha == 0f) light else Color(0xFFAA8978).copy(alpha = light.alpha)
    fun style(light: TextStyle): TextStyle = light.copy(color = ink(light.color))
}
val LocalBrandPalette = staticCompositionLocalOf { BrandPalette() }
