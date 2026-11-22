package com.fopgang.umpooling.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fopgang.umpooling.R
import com.fopgang.umpooling.ui.theme.PoolDesign
import java.text.NumberFormat

data class ImpactBar(val label: Int, val value: Float)

/** Labels and values remain accessible independently of the decorative bar heights. */
@Composable
fun ImpactBarChart(bars: List<ImpactBar>, modifier: Modifier = Modifier) {
    val largest = bars.maxOfOrNull { it.value }?.coerceAtLeast(1f) ?: 1f
    val format = NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        bars.forEachIndexed { i, bar ->
            val label = stringResource(bar.label)
            val number = format.format(bar.value)
            val description = stringResource(R.string.four_chart_description, label, number)
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) { contentDescription = description },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.fillMaxWidth().height(84.dp), contentAlignment = Alignment.BottomCenter) {
                    Box(Modifier.widthIn(max = 24.dp).fillMaxWidth().height(
                        if (bar.value == 0f) 24.dp else (24 + 60 * bar.value / largest).dp)
                        .background(if (bar.value == 0f) PoolDesign.Track else if (i % 3 == 1) PoolDesign.Strong else PoolDesign.CoralLight, CircleShape))
                }
                Text(label, style = MaterialTheme.typography.bodySmall, color = PoolDesign.Secondary, textAlign = TextAlign.Center)
                Text(number, style = MaterialTheme.typography.labelMedium, color = PoolDesign.Text)
            }
        }
    }
}

@Composable
fun TierRow(number: Int, name: String, threshold: String, current: Boolean, achieved: Boolean, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        color = Color.White.copy(alpha = if (current) .72f else .6f), shadowElevation = 5.dp,
        border = androidx.compose.foundation.BorderStroke(if (current) 1.5.dp else 1.dp, if (current) PoolDesign.Coral else Color.White)) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = CircleShape, color = if (current) PoolDesign.Coral else if (achieved) PoolDesign.Strong else PoolDesign.Track) {
                Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                    Text(number.toString(), style = MaterialTheme.typography.titleSmall, color = if (achieved && !current) Color.White else PoolDesign.Strong)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleSmall, color = PoolDesign.Text.copy(alpha = if (achieved || current) 1f else .75f))
                Text(threshold, style = MaterialTheme.typography.bodyMedium, color = PoolDesign.Secondary)
            }
            if (current) {
                Surface(color = PoolDesign.Strong, shape = CircleShape) {
                    Text(stringResource(R.string.four_you_here), Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            } else FeatureIcon(if (achieved) FeatureSymbol.Check else FeatureSymbol.Lock,
                description = stringResource(if (achieved) R.string.four_achieved else R.string.four_locked),
                tint = PoolDesign.Strong.copy(alpha = if (achieved) 1f else .7f))
        }
    }
}
