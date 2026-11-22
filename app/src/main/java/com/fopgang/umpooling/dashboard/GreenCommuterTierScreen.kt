package com.fopgang.umpooling.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fopgang.umpooling.R
import com.fopgang.umpooling.ui.components.*
import com.fopgang.umpooling.ui.theme.*

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private val TierFont = FontFamily(*listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold, FontWeight.ExtraBold).map {
    Font(R.font.plus_jakarta_sans, weight = it, variationSettings = FontVariation.Settings(FontVariation.weight(it.weight)))
}.toTypedArray())

@Composable
fun GreenCommuterTierScreen(back: () -> Unit) {
    ProvideTextStyle(androidx.compose.ui.text.TextStyle(fontFamily = TierFont, color = PoolDesign.Text)) {
        Column(Modifier.fillMaxSize().background(Color(0xFFFFF2EA)).drawWithCache {
            val top = Brush.radialGradient(listOf(Color(0xFFF5C3B0), Color.Transparent), Offset.Zero, size.width * .65f)
            val right = Brush.radialGradient(listOf(Color(0xFFF5C7B7), Color.Transparent), Offset(size.width, size.height * .55f), size.width * .65f)
            val bottom = Brush.radialGradient(listOf(Color(0xFFFBD8C8), Color.Transparent), Offset(0f, size.height), size.width * .75f)
            onDrawBehind { drawRect(top); drawRect(right); drawRect(bottom) }
        }.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 52.dp, bottom = 40.dp)) {
            Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = CircleShape, color = Color.White.copy(alpha = .55f),
                    border = BorderStroke(1.dp, Color.White), shadowElevation = 3.dp) {
                    IconButton(back, Modifier.size(46.dp)) {
                        FeatureIcon(FeatureSymbol.Back, Modifier.size(20.dp), description = stringResource(R.string.back))
                    }
                }
                Text(stringResource(R.string.four_tier_title), fontSize = 21.sp, lineHeight = 26.sp,
                    fontWeight = FontWeight.ExtraBold, letterSpacing = (-.6).sp)
            }
            Spacer(Modifier.height(15.dp))
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp), color = Color.Transparent,
                border = BorderStroke(1.dp, Color(0xFF9E9791)), shadowElevation = 16.dp) {
                Column(Modifier.background(Brush.linearGradient(listOf(Color(0xFF62544D), Color(0xFF352F2C))))
                    .padding(18.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(stringResource(R.string.four_your_tier), fontSize = 13.sp, lineHeight = 18.sp, color = Color(0xFFE8C4B5), fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.four_sprout), fontSize = 28.sp, lineHeight = 34.sp, color = Color.White, fontWeight = FontWeight.ExtraBold)
                        }
                        SeedlingIllustration(Modifier.size(55.dp))
                        Spacer(Modifier.weight(1f))
                        Surface(shape = CircleShape, color = Color(0xFFF9C3AC)) {
                            Text(stringResource(R.string.four_level), Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.four_next), fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(stringResource(R.string.four_tier_progress), fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(Modifier.height(6.dp))
                    GradientProgress(13f / 15f, stringResource(R.string.four_tier_progress_label), dark = true)
                }
            }
            Spacer(Modifier.height(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                val names = listOf(R.string.four_seedling, R.string.four_sprout, R.string.four_sapling, R.string.four_tree, R.string.four_forest)
                val thresholds = listOf(R.string.four_threshold1, R.string.four_threshold2, R.string.four_threshold3, R.string.four_threshold4, R.string.four_threshold5)
                names.forEachIndexed { index, name ->
                    val current = index == 1
                    val locked = index > 1
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(27.dp),
                        color = Color.White.copy(alpha = if (current) .4f else .72f),
                        border = BorderStroke(if (current) 1.5.dp else 1.dp, if (current) Color(0xFFF3AC95) else Color(0xFFF3D7CC)),
                        shadowElevation = 3.dp) {
                        Row(Modifier.heightIn(min = 63.dp).padding(horizontal = 17.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(38.dp).background(
                                if (index == 0) PoolDesign.Strong else if (current) Color(0xFFF4A38A) else Color(0xFFF4E5DD), CircleShape),
                                contentAlignment = Alignment.Center) {
                                Text((index + 1).toString(), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold,
                                    color = if (index == 0) Color.White else if (locked) Color(0xFF91817A) else PoolDesign.Text)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(name), fontSize = 16.sp, lineHeight = 19.sp, fontWeight = FontWeight.ExtraBold,
                                    color = if (locked) Color(0xFF544F4C) else PoolDesign.Text, letterSpacing = (-.4).sp)
                                Text(stringResource(thresholds[index]), fontSize = 13.sp, lineHeight = 16.sp,
                                    fontWeight = FontWeight.SemiBold, color = if (locked) Color(0xFF93837A) else PoolDesign.Secondary)
                            }
                            if (current) Surface(shape = CircleShape, color = PoolDesign.Strong) {
                                Text(stringResource(R.string.four_you_here), Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            } else FeatureIcon(if (locked) FeatureSymbol.Lock else FeatureSymbol.Check, Modifier.size(16.dp),
                                description = stringResource(if (locked) R.string.four_locked else R.string.four_achieved),
                                tint = if (locked) Color(0xFF544F4C) else PoolDesign.Strong)
                        }
                    }
                }
            }
            Spacer(Modifier.height(36.dp))
            Text(stringResource(R.string.four_tier_note), Modifier.padding(horizontal = 4.dp),
                fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, color = PoolDesign.Secondary)
        }
    }
}

@Preview(name = "Tier", widthDp = 390, heightDp = 844)
@Preview(name = "Tier compact large text", widthDp = 320, heightDp = 720, fontScale = 1.5f)
@Preview(name = "Tier wide", widthDp = 600, heightDp = 900)
@Composable
private fun TierPreview() { UMPoolingTheme { GreenCommuterTierScreen({}) } }
