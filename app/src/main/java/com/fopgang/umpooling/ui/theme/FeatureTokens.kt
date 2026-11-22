package com.fopgang.umpooling.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Semantic additions from design-system.md; existing screen tokens remain unchanged. */
object PoolDesign {
    val Background = Color(0xFFFDEBE3)
    val BackgroundAlt = Color(0xFFFDE3D7)
    val Text = Color(0xFF1F1A19)
    val Strong = Color(0xFF2B2523)
    val Secondary = Color(0xFF6B5C57)
    val Muted = Color(0xFF7C6C66)
    val Coral = Color(0xFFF08F76)
    val CoralLight = Color(0xFFF8C4B0)
    val Accent = Color(0xFF8A3524)
    val Track = Color(0xFFF3E4DD)
    val DarkGradient = Brush.linearGradient(listOf(Color(0xFF615550), Strong))
    val CoralGradient = Brush.linearGradient(listOf(Color(0xFFFFA3A3), Color(0xFFFF8E88)))
    val ProgressGradient = Brush.horizontalGradient(listOf(CoralLight, Coral))
    val SpaceSmall = 8.dp
    val SpaceMedium = 16.dp
    val ScreenPadding = 20.dp
    val SectionSpacing = 24.dp
    val CardRadius = 28.dp
}
