package com.fopgang.umpooling.booking

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import com.fopgang.umpooling.R
import com.fopgang.umpooling.ui.components.FeatureIcon
import com.fopgang.umpooling.ui.components.FeatureSymbol

internal val RideInk = Color(0xFF282320)
internal val RideMuted = Color(0xFF75665F)
internal val RideAccent = Color(0xFFF38D74)
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
internal val RideFont = FontFamily(*listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold, FontWeight.ExtraBold).map {
    Font(R.font.plus_jakarta_sans, weight = it, variationSettings = FontVariation.Settings(FontVariation.weight(it.weight)))
}.toTypedArray())

@Composable
internal fun RidePage(content: @Composable ColumnScope.() -> Unit) {
    ProvideTextStyle(TextStyle(fontFamily = RideFont, fontSize = 14.sp, lineHeight = 19.sp, color = RideInk)) {
        Box(Modifier.fillMaxSize().background(Color(0xFFFDEAE2)).drawWithCache {
            val glow = Brush.radialGradient(listOf(Color(0xFFF9CDBF), Color.Transparent), Offset(size.width, size.height * .18f), size.width)
            val light = Brush.radialGradient(listOf(Color.White, Color.Transparent), Offset.Zero, size.width)
            onDrawBehind { drawRect(glow); drawRect(light) }
        }, contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 600.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                .safeDrawingPadding().imePadding(), content = content)
        }
    }
}
@Composable
internal fun Glass(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp),
        color = Color.White.copy(alpha = .48f), border = BorderStroke(1.dp, Color.White),
        shadowElevation = 0.dp) { Column(Modifier.padding(16.dp), content = content) }
}
@Composable
internal fun RideHeader(title: String, back: () -> Unit, centered: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(top = 38.dp, bottom = 0.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(shape = CircleShape, color = Color.White.copy(alpha = .85f), border = BorderStroke(1.dp, Color(0xFFF5D7CA))) {
            IconButton(back, Modifier.size(48.dp)) { FeatureIcon(FeatureSymbol.Back, description = "Back") }
        }
        Text(title, Modifier.weight(1f), textAlign = if (centered) androidx.compose.ui.text.style.TextAlign.Center else androidx.compose.ui.text.style.TextAlign.Start, fontSize = if (centered) 20.sp else 23.sp, fontWeight = FontWeight.ExtraBold)
        if (centered) Surface(shape = CircleShape, color = Color.White.copy(alpha = .85f)) { IconButton({}) { Text("•••", fontSize = 20.sp) } }
    }
}
@Composable
internal fun RideAction(label: String, modifier: Modifier = Modifier, enabled: Boolean = true, action: () -> Unit) {
    Button(action, modifier.fillMaxWidth().heightIn(min = 56.dp), enabled = enabled, shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 12.dp)) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

