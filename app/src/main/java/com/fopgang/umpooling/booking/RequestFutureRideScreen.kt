package com.fopgang.umpooling.booking

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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fopgang.umpooling.R
import com.fopgang.umpooling.ui.components.*
import com.fopgang.umpooling.ui.theme.UMPoolingTheme

private val FutureInk = Color(0xFF25211F)
private val FutureMuted = Color(0xFF75655E)
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private val FutureFont = FontFamily(*listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold, FontWeight.ExtraBold).map {
    Font(R.font.plus_jakarta_sans, weight = it, variationSettings = FontVariation.Settings(FontVariation.weight(it.weight)))
}.toTypedArray())

@Composable
fun RequestFutureRideScreen(back: () -> Unit, onViewOffer: (String) -> Unit, criteria: FindRideCriteria? = null, onPendingGroup: (String) -> Unit = {}) {
    ProvideTextStyle(TextStyle(fontFamily = FutureFont, fontSize = 14.sp, lineHeight = 19.sp, color = FutureInk)) {
        Column(Modifier.fillMaxSize().background(Color(0xFFFCEBE3)).drawWithCache {
            val white = Brush.radialGradient(listOf(Color.White, Color.Transparent), Offset.Zero, size.width * .85f)
            val peach = Brush.radialGradient(listOf(Color(0xFFF9CDBE), Color.Transparent), Offset(size.width, size.height * .25f), size.width * .8f)
            val bottom = Brush.radialGradient(listOf(Color(0xFFF8D0BF), Color.Transparent), Offset(0f, size.height), size.width * .7f)
            onDrawBehind { drawRect(white); drawRect(peach); drawRect(bottom) }
        }.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 52.dp, bottom = 24.dp)) {
            Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = CircleShape, color = Color.White.copy(alpha = .85f),
                    border = BorderStroke(1.dp, Color(0xFFF7D9CF))) {
                    IconButton(back, Modifier.size(46.dp)) { FeatureIcon(FeatureSymbol.Back, Modifier.size(20.dp), description = "Back") }
                }
                Text("Pickup groups", fontSize = 23.sp, lineHeight = 29.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(18.dp))
            Surface(Modifier.padding(start = 4.dp), shape = CircleShape, color = Color(0xFFFFB7B0).copy(alpha = .65f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = .7f)), shadowElevation = 7.dp) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FeatureIcon(FeatureSymbol.Pin, Modifier.size(16.dp), tint = Color.White)
                    Text("Your area: Kolej Kediaman 11", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(11.dp))
            Text(if (criteria == null) "Same residential area, nearby pickup points and compatible times, grouped into one car."
                else androidx.compose.ui.res.stringResource(R.string.ride_search_summary, criteria.origin.name, criteria.destination.name,
                    criteria.departureDate.rideLabel(), criteria.departureTime.rideLabel(), criteria.seats),
                Modifier.padding(horizontal = 4.dp), fontSize = 15.sp, lineHeight = 20.sp, color = Color.Black)
            Spacer(Modifier.height(15.dp))
            FutureCard {
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 17.dp), verticalAlignment = Alignment.CenterVertically) {
                    FutureStat("6", "riders nearby", Modifier.weight(1f))
                    VerticalDivider(Modifier.height(32.dp), color = Color(0xFFF0CBBB))
                    FutureStat("08:05–\n08:25", "best window", Modifier.weight(1f))
                    VerticalDivider(Modifier.height(32.dp), color = Color(0xFFF0CBBB))
                    FutureStat("RM 2.60", "each, if 4 share", Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Suggested for you", Modifier.weight(1f), fontSize = 19.sp, lineHeight = 24.sp, fontWeight = FontWeight.ExtraBold)
                Text("Fills the car, lowers cost", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FutureMuted)
            }
            Spacer(Modifier.height(13.dp))
            FutureCard(highlight = true) {
                Column(Modifier.padding(17.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Surface(shape = CircleShape, color = Color.Transparent) {
                            Text("Best match · 92%", Modifier.background(Brush.verticalGradient(listOf(Color(0xFFF9B7A1), Color(0xFFF18D72))))
                                .padding(horizontal = 12.dp, vertical = 5.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("Mon – Fri", fontSize = 13.sp, color = FutureMuted, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.padding(top = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(12.dp).background(Color(0xFFFFE0D0), CircleShape), contentAlignment = Alignment.Center) {
                                Box(Modifier.size(6.dp).background(FutureInk, CircleShape))
                            }
                            repeat(3) { Box(Modifier.padding(vertical = 2.dp).size(2.dp, 4.dp).background(Color(0xFFF3CFBF))) }
                            Box(Modifier.size(12.dp).background(Color(0xFFF28C72), RoundedCornerShape(4.dp)))
                        }
                        Column {
                            FutureStop("PICKUP POINT", "Kolej Kediaman 11, main lobby")
                            Spacer(Modifier.height(10.dp))
                            FutureStop("DESTINATION", "FSKTM, Block A")
                        }
                    }
                    Spacer(Modifier.height(15.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Departure window", color = FutureMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("08:05 – 08:25", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF973D29))
                    }
                    Spacer(Modifier.height(7.dp))
                    Row(Modifier.fillMaxWidth().height(10.dp).background(Color(0xFFF6DFD3), CircleShape)) {
                        Spacer(Modifier.weight(35f))
                        Box(Modifier.weight(20f).fillMaxHeight()
                            .background(Brush.horizontalGradient(listOf(Color(0xFFF9B69D), Color(0xFFF28C71))), CircleShape))
                        Spacer(Modifier.weight(35f))
                    }
                    Spacer(Modifier.height(3.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("07:30", "08:15", "09:00").forEach { Text(it, fontSize = 11.sp, color = FutureMuted) }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FuturePeople(listOf("HR", "NA", "KW"), addSeat = true)
                        Text("3 of 4 seats filled · 1 left", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(14.dp))
                    FutureJoin("Join this group", Modifier.fillMaxWidth(), { onViewOffer(DemoRideIds.PICKUP_GROUP) })
                }
            }
            Spacer(Modifier.height(12.dp))
            FutureCard {
                Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Kolej 3 → Main Library", Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                        Surface(shape = CircleShape, color = Color(0xFFFFE1D1)) {
                            Text("Available", Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B432C))
                        }
                    }
                    Text("Pickup: Kolej 3 bus stop · Window 08:40 – 09:00", fontSize = 13.sp, color = FutureMuted)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FuturePeople(listOf("FH", "SL"))
                        Text("2 riders waiting · No driver", Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        FutureJoin("Join", Modifier.width(61.dp), { onPendingGroup(DemoRideIds.OTHER_GROUP) }, small = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun FutureCard(highlight: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(29.dp), color = Color(0xFFFFFAF7).copy(alpha = .9f),
        border = BorderStroke(if (highlight) 1.5.dp else 1.dp, if (highlight) Color(0xFFF2AC97) else Color.White),
        shadowElevation = 7.dp) { Column(content = content) }
}
@Composable
private fun FutureStat(value: String, label: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 21.sp, lineHeight = 25.sp, fontWeight = FontWeight.ExtraBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text(label, fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.Bold, color = FutureMuted)
    }
}
@Composable
private fun FutureStop(label: String, place: String) {
    Text(label, fontSize = 12.sp, lineHeight = 16.sp, color = FutureMuted, fontWeight = FontWeight.Bold)
    Text(place, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-.4).sp)
}
@Composable
private fun FuturePeople(initials: List<String>, addSeat: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy((-5).dp)) {
        initials.forEachIndexed { index, name ->
            Surface(Modifier.size(29.dp), shape = CircleShape, color = if (index % 2 == 0) Color(0xFFFFE1D3) else Color(0xFFF9C4AF),
                border = BorderStroke(2.dp, Color.White)) {
                Box(contentAlignment = Alignment.Center) { Text(name, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            }
        }
        if (addSeat) {
            Box(
                Modifier
                    .size(29.dp)
                    .background(Color.White, CircleShape)
                    .drawBehind {
                        drawCircle(
                            color = Color(0xFFF1A48B),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 1.dp.toPx(),
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                    floatArrayOf(4.dp.toPx(), 4.dp.toPx())
                                )
                            )
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("+", fontSize = 23.sp, color = Color(0xFF9B3C28), modifier = Modifier.offset(y = (-2).dp))
            }
        }
    }
}
@Composable
private fun FutureJoin(label: String, modifier: Modifier, action: () -> Unit, small: Boolean = false) {
    Button(action, modifier.height(if (small) 38.dp else 48.dp), shape = CircleShape,
        contentPadding = PaddingValues(0.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = if (small) 0.dp else 10.dp)) {
        Text(label, fontFamily = FutureFont, fontSize = if (small) 14.sp else 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}
@Preview
@Composable
private fun FutureRidePreview() { UMPoolingTheme { RequestFutureRideScreen({}, {}) } }
