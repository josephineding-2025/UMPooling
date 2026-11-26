package com.fopgang.umpooling.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fopgang.umpooling.R
import com.fopgang.umpooling.ui.components.*
import com.fopgang.umpooling.ui.theme.*

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private val DashboardFont = FontFamily(
    *listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold, FontWeight.ExtraBold)
        .map { weight ->
            Font(R.font.plus_jakarta_sans, weight = weight,
                variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))
        }.toTypedArray()
)

@Composable
fun SustainabilityDashboardScreen(navigate: (String) -> Unit, unavailable: () -> Unit) {
    var monthly by rememberSaveable { mutableStateOf(false) }
    val impactRepo = remember { com.fopgang.umpooling.data.repository.ImpactRepository() }
    val analyticsService = remember { com.fopgang.umpooling.ai.ImpactAnalyticsService.instance }

    var personalImpact by remember { mutableStateOf<com.fopgang.umpooling.data.repository.PersonalImpact?>(null) }
    var staffCells by remember { mutableStateOf<List<com.fopgang.umpooling.data.repository.StaffImpactCell>>(emptyList()) }

    LaunchedEffect(Unit) {
        impactRepo.personal().onSuccess { personalImpact = it }
        impactRepo.staffCells().onSuccess { staffCells = it }
    }

    val demoStats = remember { analyticsService.getDemoUserImpactStats() }
    val userStats = remember(personalImpact) {
        personalImpact?.let {
            com.fopgang.umpooling.ai.UserImpactStats(
                userId = it.userId,
                totalTrips = it.totalTrips ?: 0,
                totalDistanceKm = it.distanceKm ?: 0.0,
                totalCo2SavedKg = it.co2Kg ?: 0.0,
                currentTierLevel = it.tierLevel,
                currentTierName = it.tierName
            )
        } ?: demoStats
    }

    val personalInsight = remember(userStats) { analyticsService.generatePersonalInsights(userStats) }
    val campusSummary = remember { analyticsService.getDemoCampusImpactSummary() }
    val facultyLeaderboard = remember { analyticsService.getFacultyLeaderboard() }
    val collegeLeaderboard = remember { analyticsService.getCollegeLeaderboard() }

    val base = MaterialTheme.typography
    fun androidx.compose.ui.text.TextStyle.dashboardStyle() = copy(fontFamily = DashboardFont)
    MaterialTheme(typography = base.copy(
        headlineMedium = base.headlineMedium.dashboardStyle().copy(fontSize = 24.sp, lineHeight = 29.sp, fontWeight = FontWeight.ExtraBold),
        headlineLarge = base.headlineLarge.dashboardStyle().copy(fontWeight = FontWeight.ExtraBold),
        titleSmall = base.titleSmall.dashboardStyle().copy(fontSize = 17.sp, fontWeight = FontWeight.Bold),
        bodyMedium = base.bodyMedium.dashboardStyle(), bodySmall = base.bodySmall.dashboardStyle(),
        labelLarge = base.labelLarge.dashboardStyle().copy(fontWeight = FontWeight.Bold),
        labelMedium = base.labelMedium.dashboardStyle().copy(fontWeight = FontWeight.Bold),
        labelSmall = base.labelSmall.dashboardStyle().copy(fontWeight = FontWeight.Bold)
    )) {
    ProvideTextStyle(MaterialTheme.typography.bodyMedium.copy(fontFamily = DashboardFont)) {
    Column(Modifier.fillMaxSize().background(Color(0xFFFCEBE3))) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ImpactHero(
                tier = { navigate("tier") },
                userStats = userStats,
                insight = personalInsight
            )
            Column(Modifier.padding(horizontal = PoolDesign.ScreenPadding).padding(top = 0.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                DashboardCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(if (monthly) R.string.four_monthly_overview else R.string.four_weekly_overview),
                                    style = MaterialTheme.typography.bodyMedium, color = PoolDesign.Secondary)
                                Text(stringResource(R.string.four_co2_saved), style = MaterialTheme.typography.titleSmall, color = PoolDesign.Text)
                            }
                            PeriodSelector(monthly) { monthly = it }
                        }
                        val bars = if (monthly) listOf(
                            ImpactBar(R.string.four_week1, 5.7f), ImpactBar(R.string.four_week2, 7.6f),
                            ImpactBar(R.string.four_week3, 5.7f), ImpactBar(R.string.four_week4, 3.8f)
                        ) else listOf(
                            ImpactBar(R.string.four_mon, 1.9f), ImpactBar(R.string.four_tue, 3.8f),
                            ImpactBar(R.string.four_wed, 0f), ImpactBar(R.string.four_thu, 1.9f),
                            ImpactBar(R.string.four_fri, 3.8f), ImpactBar(R.string.four_sat, 0f), ImpactBar(R.string.four_sun, 0f)
                        )
                        ImpactBarChart(bars, Modifier.heightIn(max = 120.dp))
                    }
                }
                DashboardPair(
                    first = { DashboardMetric(stringResource(R.string.four_shared_rides), (userStats.totalTrips / 2).toString(), stringResource(R.string.four_this_week), it) },
                    second = { DashboardMetric(stringResource(R.string.four_this_month), String.format(java.util.Locale.US, "%.1f", userStats.totalCo2SavedKg), stringResource(R.string.four_kg_co2), it, dark = true) }
                )
                DashboardCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(stringResource(R.string.four_campus_impact), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = PoolDesign.Text)
                            StatusPill(stringResource(R.string.four_november))
                        }
                        DashboardPair(first = {
                            CampusMetric("${java.text.NumberFormat.getIntegerInstance().format(campusSummary.totalCo2SavedKg.toInt())} kg", stringResource(R.string.four_saved_together), it)
                        }, second = {
                            CampusMetric(java.text.NumberFormat.getIntegerInstance().format(campusSummary.totalSharedRides), stringResource(R.string.four_shared_rides), it)
                        })
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(stringResource(R.string.four_semester_goal), Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                            Text("${campusSummary.semesterGoalPercentage}%", style = MaterialTheme.typography.labelLarge)
                        }
                        GradientProgress((campusSummary.semesterGoalPercentage / 100f).coerceIn(0f, 1f), stringResource(R.string.four_goal_progress))
                    }
                }

                // Community Leaderboards (FR5.3 - Strictly Faculty & College, Never Individual)
                DashboardCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Faculty Leaderboard", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = PoolDesign.Text)
                            StatusPill("Top 3")
                        }
                        facultyLeaderboard.take(3).forEachIndexed { idx, item ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("#${idx + 1}", fontWeight = FontWeight.Bold, color = PoolDesign.Secondary, fontSize = 14.sp)
                                Text(item.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = PoolDesign.Text, maxLines = 1)
                                Text("${item.totalCo2SavedKg.toInt()} kg", fontWeight = FontWeight.ExtraBold, color = PoolDesign.Strong, fontSize = 14.sp)
                            }
                        }
                        HorizontalDivider(color = Color(0xFFF0CBBB).copy(alpha = 0.5f))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("College Leaderboard", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = PoolDesign.Text)
                            StatusPill("Top 3")
                        }
                        collegeLeaderboard.take(3).forEachIndexed { idx, item ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("#${idx + 1}", fontWeight = FontWeight.Bold, color = PoolDesign.Secondary, fontSize = 14.sp)
                                Text(item.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = PoolDesign.Text, maxLines = 1)
                                Text("${item.totalCo2SavedKg.toInt()} kg", fontWeight = FontWeight.ExtraBold, color = PoolDesign.Strong, fontSize = 14.sp)
                            }
                        }
                        Text("Grouped by cohort to protect commuter privacy (FR5.3)", style = MaterialTheme.typography.labelSmall, color = PoolDesign.Secondary)
                    }
                }
            }
        }
        CommuterNavigation("dashboard", navigate, Modifier.padding(horizontal = 14.dp).padding(top = 10.dp, bottom = 30.dp))
    }
    }
    }
}

@Composable
private fun ImpactHero(
    tier: () -> Unit,
    userStats: com.fopgang.umpooling.ai.UserImpactStats,
    insight: com.fopgang.umpooling.ai.PersonalInsight
) {
    Column(Modifier.fillMaxWidth().background(Color(0xFFFF9E9C))
        .then(Modifier.drawWithCache {
            val topCoral = Brush.radialGradient(
                listOf(Color(0xFFDD6952), Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(size.width * .91f, size.height * .12f),
                radius = size.width * .63f
            )
            val lowerGlow = Brush.radialGradient(
                listOf(Color(0xFFB66D5B).copy(alpha = .8f), Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(size.width * .15f, size.height * .94f),
                radius = size.width * .47f
            )
            val bottomPink = Brush.radialGradient(
                listOf(Color(0xFFFF8D8D), Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(size.width * .91f, size.height),
                radius = size.width * .65f
            )
            onDrawBehind { drawRect(topCoral); drawRect(lowerGlow); drawRect(bottomPink) }
        }).padding(horizontal = 24.dp).padding(top = 52.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.four_your_impact), Modifier.weight(1f), fontSize = 25.sp, fontFamily = DashboardFont, fontWeight = FontWeight.Bold, color = Color.White)
            Surface(shape = CircleShape, color = Color.White.copy(alpha = .12f), border = BorderStroke(1.dp, Color.White.copy(alpha = .6f))) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FeatureIcon(FeatureSymbol.Calendar, Modifier.size(16.dp), tint = Color.White)
                    Text(stringResource(R.string.four_date_range), style = MaterialTheme.typography.labelMedium, color = Color.White)
                }
            }
        }
        HeroPair(first = { modifier ->
            Surface(modifier.sizeIn(maxWidth = 134.dp).aspectRatio(1f), shape = CircleShape, color = Color.White.copy(alpha = .18f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = .8f))) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.four_you_saved), color = Color.White, style = MaterialTheme.typography.labelLarge)
                    val thisWeekKg = String.format(java.util.Locale.US, "%.1f", userStats.totalCo2SavedKg / 2.0)
                    Text(thisWeekKg, fontFamily = DashboardFont, fontWeight = FontWeight.ExtraBold, fontSize = 36.sp, lineHeight = 39.sp, color = Color.White)
                    Text(stringResource(R.string.four_co2_this_week), color = Color.White, style = MaterialTheme.typography.labelMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }, second = { modifier ->
            Column(modifier, verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Surface(shape = MaterialTheme.shapes.large, color = Color.White.copy(alpha = .18f), border = BorderStroke(1.dp, Color.White.copy(alpha = .6f))) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.four_compared), color = Color.White, fontSize = 13.sp, lineHeight = 17.sp)
                        Text(insight.headline, color = Color.White, fontSize = 17.sp, lineHeight = 21.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                Surface(onClick = tier, shape = CircleShape, color = PoolDesign.Coral) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 36.dp).padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FeatureIcon(FeatureSymbol.Leaf)
                        Text(insight.lowCarbonTip, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = PoolDesign.Text, maxLines = 1)
                        FeatureIcon(FeatureSymbol.Chevron, Modifier.size(16.dp))
                    }
                }
            }
        })
    }
}

@Composable
private fun CampusMetric(value: String, label: String, modifier: Modifier) {
    Surface(modifier, color = PoolDesign.BackgroundAlt.copy(alpha = .55f), shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, Color.White)) {
        Column(Modifier.padding(14.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = PoolDesign.Text)
            Text(label, style = MaterialTheme.typography.bodySmall, color = PoolDesign.Secondary)
        }
    }
}

@Composable
private fun PeriodSelector(monthly: Boolean, select: (Boolean) -> Unit) {
    Surface(shape = CircleShape, color = PoolDesign.Track) {
        Row(Modifier.selectableGroup().padding(4.dp)) {
            listOf(false to R.string.four_week_short, true to R.string.four_month_short).forEach { (value, label) ->
                val title = stringResource(if (value) R.string.four_month else R.string.four_week)
                Surface(shape = CircleShape, color = if (monthly == value) PoolDesign.Strong else Color.Transparent) {
                    Box(Modifier.size(36.dp, 28.dp)
                        .selectable(monthly == value, role = Role.RadioButton, onClick = { select(value) })
                        .semantics { contentDescription = title }, contentAlignment = Alignment.Center) {
                        Text(stringResource(label), color = if (monthly == value) Color.White else PoolDesign.Strong, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Preview(name = "Impact", widthDp = 390, heightDp = 844)
@Preview(name = "Impact compact large text", widthDp = 320, heightDp = 720, fontScale = 1.5f)
@Preview(name = "Impact wide", widthDp = 600, heightDp = 900)
@Composable
private fun ImpactPreview() { UMPoolingTheme { PeachBackground { SustainabilityDashboardScreen({}, {}) } } }

@Composable
private fun DashboardPair(modifier: Modifier = Modifier, first: @Composable (Modifier) -> Unit, second: @Composable (Modifier) -> Unit) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        first(Modifier.weight(1f))
        second(Modifier.weight(1f))
    }
}

@Composable
private fun DashboardCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, shape = RoundedCornerShape(30.dp), color = Color(0xFFFFFAF7).copy(alpha = .9f),
        border = BorderStroke(1.dp, Color.White), shadowElevation = 3.dp) { Column(content = content) }
}

@Composable
private fun HeroPair(first: @Composable (Modifier) -> Unit, second: @Composable (Modifier) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
        first(Modifier.width(134.dp))
        second(Modifier.weight(1f))
    }
}

@Composable
private fun DashboardMetric(label: String, value: String, unit: String, modifier: Modifier, dark: Boolean = false) {
    Surface(modifier.height(90.dp), shape = RoundedCornerShape(28.dp),
        color = if (dark) PoolDesign.Strong else Color(0xFFFFDFCF),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .8f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (dark) PoolDesign.BackgroundAlt else PoolDesign.Text)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(value, fontSize = 29.sp, fontWeight = FontWeight.ExtraBold, color = if (dark) Color.White else PoolDesign.Text)
                Text(unit, Modifier.padding(bottom = 3.dp), fontSize = 13.sp, color = if (dark) PoolDesign.BackgroundAlt else PoolDesign.Text)
            }
        }
    }
}
