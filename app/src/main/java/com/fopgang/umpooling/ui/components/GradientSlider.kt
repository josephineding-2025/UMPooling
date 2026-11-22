package com.fopgang.umpooling.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * A controlled Material slider with a peach-to-charcoal track and raised round thumb.
 *
 * The caller owns [value]. Use [onValueChangeFinished] to commit changes after a drag.
 * Material supplies touch, keyboard, accessibility, range and step handling.
 * Apply a content description or state description through [modifier] when needed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradientSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    activeStartColor: Color = Color(0xFFF6A58E),
    activeEndColor: Color = Color(0xFF352E2A),
    inactiveTrackColor: Color = Color(0xFFD8C1B9),
    thumbTopColor: Color = Color.White,
    thumbBottomColor: Color = Color(0xFFFAEBE4),
    thumbBorderColor: Color = Color(0xFFE8B6A3),
    trackHeight: Dp = 8.dp,
    thumbSize: Dp = 29.dp,
) {
    require(valueRange.start.isFinite() && valueRange.endInclusive.isFinite() &&
        valueRange.endInclusive > valueRange.start) { "valueRange must be finite and increasing" }
    require(steps >= 0) { "steps must be non-negative" }
    require(trackHeight > 0.dp && thumbSize > 0.dp) { "Slider dimensions must be positive" }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val opacity = if (enabled) 1f else .38f
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        valueRange = valueRange,
        enabled = enabled,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        thumb = {
            Box(
                Modifier.size(thumbSize)
                    .shadow(if (enabled) 6.dp else 0.dp, CircleShape)
                    .background(
                        Brush.verticalGradient(listOf(thumbTopColor.copy(alpha = opacity), thumbBottomColor.copy(alpha = opacity))),
                        CircleShape,
                    )
                    .border(1.dp, thumbBorderColor.copy(alpha = opacity), CircleShape)
            )
        },
        track = { state ->
            Canvas(Modifier.fillMaxWidth().height(trackHeight)) {
                val fraction = ((state.value - valueRange.start) /
                    (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
                val radius = CornerRadius(size.height / 2)
                scale(scaleX = if (rtl) -1f else 1f, scaleY = 1f) {
                    drawRoundRect(inactiveTrackColor.copy(alpha = opacity), cornerRadius = radius)
                    if (fraction > 0f) {
                        val activeWidth = size.width * fraction
                        drawRoundRect(
                            Brush.horizontalGradient(
                                listOf(activeStartColor.copy(alpha = opacity), activeEndColor.copy(alpha = opacity)),
                                endX = activeWidth,
                            ),
                            size = Size(activeWidth, size.height),
                            cornerRadius = radius,
                        )
                    }
                    drawLine(
                        Color.White.copy(alpha = .8f * opacity),
                        Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx(),
                    )
                }
            }
        },
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFFEAE3, widthDp = 240)
@Composable
private fun GradientSliderPreview() {
    GradientSlider(.5f, {}, Modifier.fillMaxWidth().padding(horizontal = 16.dp))
}
