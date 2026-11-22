package com.fopgang.umpooling.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fopgang.umpooling.R
import com.fopgang.umpooling.ui.theme.PoolDesign

@Composable
fun FeatureCard(modifier: Modifier = Modifier, dark: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    if (dark) Surface(modifier, shape = RoundedCornerShape(PoolDesign.CardRadius),
        color = PoolDesign.Strong, contentColor = Color.White, shadowElevation = 12.dp,
        border = BorderStroke(1.dp, Color.White.copy(alpha = .35f))) {
        Column(Modifier.background(PoolDesign.DarkGradient), content = content)
    } else GlassCard(modifier, radius = PoolDesign.CardRadius, opacity = .78f, content = content)
}

/** Stack paired cards on narrow screens or when larger text needs the available width. */
@Composable
fun AdaptivePair(
    modifier: Modifier = Modifier,
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit
) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier.fillMaxWidth()) {
        if (maxWidth < 340.dp || fontScale > 1.25f) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                first(Modifier.fillMaxWidth())
                second(Modifier.fillMaxWidth())
            }
        } else Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            first(Modifier.weight(1f))
            second(Modifier.weight(1f))
        }
    }
}

@Composable
fun MetricCard(label: String, value: String, unit: String, modifier: Modifier = Modifier, dark: Boolean = false) {
    FeatureCard(modifier, dark) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(label, style = MaterialTheme.typography.titleSmall,
                color = if (dark) PoolDesign.CoralLight else PoolDesign.Strong)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(value, style = MaterialTheme.typography.headlineLarge,
                    color = if (dark) Color.White else PoolDesign.Text)
                Text(unit, Modifier.padding(bottom = 3.dp).weight(1f), style = MaterialTheme.typography.bodyMedium,
                    color = if (dark) PoolDesign.BackgroundAlt else PoolDesign.Strong)
            }
        }
    }
}

@Composable
fun GradientProgress(progress: Float, description: String, modifier: Modifier = Modifier, dark: Boolean = false) {
    val fraction = progress.coerceIn(0f, 1f)
    Box(modifier.fillMaxWidth().height(10.dp).semantics {
        contentDescription = description
        progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
    }.background(if (dark) Color.White.copy(alpha = .2f) else PoolDesign.Track, CircleShape)) {
        if (fraction > 0f) Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(PoolDesign.ProgressGradient, CircleShape))
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    dark: Boolean = false
) {
    Row(modifier.fillMaxWidth().heightIn(min = 68.dp)
        .toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
        .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall,
                color = (if (dark) Color.White else PoolDesign.Text).copy(alpha = if (enabled) 1f else .55f))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium,
                color = (if (dark) PoolDesign.CoralLight else PoolDesign.Secondary).copy(alpha = if (enabled) 1f else .55f))
        }
        Switch(checked, onCheckedChange = null, enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = PoolDesign.Strong,
                checkedThumbColor = Color(0xFFFFFAF7), uncheckedTrackColor = Color(0xFFE3CEC4),
                uncheckedThumbColor = Color.White, uncheckedBorderColor = Color.Transparent))
    }
}

@Composable
fun FeatureBackHeader(title: String, back: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(shape = CircleShape, color = Color.White.copy(alpha = .75f), border = BorderStroke(1.dp, Color.White)) {
            IconButton(back) { FeatureIcon(FeatureSymbol.Back, description = stringResource(R.string.back)) }
        }
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, color = PoolDesign.Text)
    }
}
