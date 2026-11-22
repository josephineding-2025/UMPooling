package com.fopgang.umpooling.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.fopgang.umpooling.R
import com.fopgang.umpooling.ui.theme.PoolDesign

enum class FeatureSymbol { Back, Pin, Calendar, Bell, Lock, Check, Leaf, Chevron, Phone, Send }

@Composable
fun FeatureIcon(symbol: FeatureSymbol, modifier: Modifier = Modifier, tint: Color = PoolDesign.Strong, description: String? = null) {
    val resource = when (symbol) {
        FeatureSymbol.Back -> R.drawable.login_frame
        FeatureSymbol.Pin -> R.drawable.home_frame9
        FeatureSymbol.Calendar -> R.drawable.home_frame2
        FeatureSymbol.Bell -> R.drawable.home_frame13
        FeatureSymbol.Lock -> R.drawable.login_frame2
        FeatureSymbol.Check -> R.drawable.profile_frame7
        FeatureSymbol.Leaf -> R.drawable.profile_frame8
        FeatureSymbol.Chevron -> R.drawable.profile_frame1
        else -> null
    }
    if (resource != null) Icon(painterResource(resource), description, modifier.size(22.dp), tint = tint)
    else Canvas(modifier.size(24.dp).then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier)) {
        scale(size.width / 24f, size.height / 24f, pivot = androidx.compose.ui.geometry.Offset.Zero) {
            val path = Path()
            if (symbol == FeatureSymbol.Send) {
                path.moveTo(3f, 10f); path.lineTo(21f, 3f); path.lineTo(14f, 21f)
                path.lineTo(10f, 14f); path.close()
                path.moveTo(10f, 14f); path.lineTo(21f, 3f)
            } else {
                path.moveTo(5f, 3f); path.lineTo(9f, 3f); path.lineTo(11f, 8f)
                path.lineTo(8f, 10f); path.cubicTo(9f, 13f, 11f, 15f, 14f, 16f)
                path.lineTo(16f, 13f); path.lineTo(21f, 15f); path.lineTo(21f, 19f)
                path.cubicTo(21f, 23f, 13f, 20f, 8f, 15f)
                path.cubicTo(3f, 10f, 1f, 3f, 5f, 3f); path.close()
            }
            drawPath(path, tint, style = Stroke(1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** Decorative seedling, rendered natively without a platform-dependent emoji font. */
@Composable
fun SeedlingIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier.size(56.dp)) {
        scale(size.width / 56f, size.height / 56f, pivot = androidx.compose.ui.geometry.Offset.Zero) {
            val stem = Path().apply { moveTo(29f, 54f); cubicTo(30f, 39f, 30f, 25f, 20f, 14f); moveTo(29f, 37f); quadraticTo(35f, 25f, 45f, 27f) }
            drawPath(stem, Color(0xFF5A8F32), style = Stroke(2f, cap = StrokeCap.Round))
            val left = Path().apply { moveTo(22f, 20f); cubicTo(8f, 23f, 5f, 10f, 4f, 3f); cubicTo(19f, 4f, 25f, 10f, 22f, 20f) }
            val right = Path().apply { moveTo(31f, 29f); cubicTo(35f, 16f, 48f, 19f, 55f, 26f); cubicTo(46f, 31f, 39f, 36f, 31f, 29f) }
            drawPath(left, Color(0xFF89C848)); drawPath(right, Color(0xFF89C848))
        }
    }
}
