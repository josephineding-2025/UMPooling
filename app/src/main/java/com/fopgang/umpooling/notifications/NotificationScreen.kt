package com.fopgang.umpooling.notifications

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fopgang.umpooling.booking.RideFont
import com.fopgang.umpooling.booking.BookingRoute
import com.fopgang.umpooling.booking.DemoRideIds
import com.fopgang.umpooling.R
import com.fopgang.umpooling.ui.components.FigmaIcon
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import com.fopgang.umpooling.ui.components.FeatureIcon
import com.fopgang.umpooling.ui.components.FeatureSymbol
import com.fopgang.umpooling.ui.theme.UMPoolingTheme

enum class NotificationCategory { Matches, Bookings, Messages }
enum class NotificationGlyph { Group, Heart, Car, Check, Star, Message }
data class NotificationItem(
    val id: String,
    val title: String,
    val detail: String,
    val category: NotificationCategory,
    val glyph: NotificationGlyph,
    val time: String = "",
    val earlier: Boolean = false,
    val unread: Boolean = false,
    val action: String? = null,
    val badge: String? = null,
)
private val Ink = Color(0xFF242020)
private val Muted = Color(0xFF705B55)
private val Coral = Color(0xFFEE604B)

/** Presentation shared by the account inbox and explicitly separated demo content. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotificationScreen(
    entries: List<NotificationItem>,
    back: () -> Unit,
    navigate: (String) -> Unit,
    open: (NotificationItem) -> Unit,
    markAllRead: () -> Unit,
    error: String? = null,
    modifier: Modifier = Modifier,
) {
    var selected by rememberSaveable { mutableStateOf("All") }
    val filtered = entries.filter { selected == "All" || it.category.name == selected }
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp, LocalTextStyle provides LocalTextStyle.current.copy(
        fontFamily = RideFont, color = Ink, fontSize = 13.sp, lineHeight = 17.sp)) {
        Box(modifier.fillMaxSize().background(Color(0xFFFCE9E0)).drawWithCache {
            val top = Brush.linearGradient(listOf(Color(0xFFFFFCFA), Color(0xFFFBD4C8)),
                Offset.Zero, Offset(size.width, size.height * .22f))
            val glow = Brush.radialGradient(listOf(Color(0xFFFF9392), Color.Transparent),
                Offset(size.width * .58f, size.height * .59f), size.width * .82f)
            val bottom = Brush.radialGradient(listOf(Color(0xFFFDEDE5), Color.Transparent),
                Offset(size.width * .1f, size.height), size.width * 1.1f)
            onDrawBehind { drawRect(top); drawRect(glow); drawRect(bottom) }
        }) {
            Column(Modifier.fillMaxSize()) {
                LazyColumn(Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 43.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Box(Modifier.fillMaxWidth().heightIn(min = 46.dp), contentAlignment = Alignment.Center) {
                            Text("Notifications", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                            Surface(onClick = back, modifier = Modifier.align(Alignment.CenterStart).size(46.dp),
                                shape = CircleShape, color = Color.White.copy(alpha = .82f), border = BorderStroke(1.dp, Color.White)) {
                                Box(contentAlignment = Alignment.Center) { FeatureIcon(FeatureSymbol.Back, description = "Back") }
                            }
                            Surface(onClick = { navigate("notification_settings") },
                                modifier = Modifier.align(Alignment.CenterEnd).size(46.dp).semantics {contentDescription = "Notification settings"},
                                shape = CircleShape, color = Color.White.copy(alpha = .25f), border = BorderStroke(1.dp, Color.White)) {
                                Box(contentAlignment = Alignment.Center) { SettingsGlyph() }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("All", "Matches", "Bookings", "Messages").forEach { label ->
                                Surface(onClick = { selected = label }, shape = CircleShape,
                                    color = if(selected == label) Ink else Color.White.copy(alpha = .48f),
                                    border = if(selected == label) null else BorderStroke(1.dp, Color.White)) {
                                    Box(Modifier.heightIn(min = 36.dp).padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                                        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                            color = if(selected == label) Color.White else Ink)
                                    }
                                }
                            }
                        }
                    }
                    if(error != null) item { Text(error, color = MaterialTheme.colorScheme.error) }
                    if(filtered.isEmpty()) item {
                        Surface(shape = RoundedCornerShape(24.dp), color = Color.White.copy(alpha = .65f)) {
                            Column(Modifier.fillMaxWidth().padding(24.dp)) {
                                Text("You're all caught up", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(if(entries.isEmpty()) "Your ride updates will appear here." else "No notifications in this category.",
                                    color = Muted)
                            }
                        }
                    }
                    val today = filtered.filterNot { it.earlier }
                    val earlier = filtered.filter { it.earlier }
                    if(today.isNotEmpty()) {
                        item {
                            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                verticalArrangement = Arrangement.Center) {
                                Text("Today", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 2.dp))
                                TextButton(markAllRead, modifier = Modifier.height(24.dp), enabled = entries.any { it.unread }, contentPadding = PaddingValues(0.dp)) {
                                    Text("Mark all as read", color = Color(0xFFCB492D), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        items(today, key = {it.id}) { NotificationCard(it) {open(it)} }
                    }
                    if(earlier.isNotEmpty()) {
                        item { Text("Earlier", Modifier.padding(top = 14.dp), fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                        items(earlier, key = {it.id}) { NotificationCard(it) {open(it)} }
                    }
                }
                com.fopgang.umpooling.ui.components.CommuterNavigation("notifications", navigate,
                    Modifier.padding(horizontal = 14.dp).padding(bottom = 24.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NotificationCard(item: NotificationItem, open: () -> Unit) {
    Surface(onClick = open, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(25.dp),
        color = Color.White.copy(alpha = .67f), border = BorderStroke(1.dp, Color.White)) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NotificationIcon(item.glyph)
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Text(item.title, Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 18.sp)
                    if(item.time.isNotEmpty()) Text(item.time, Modifier.padding(start = 6.dp), color = Color(0xFF83655D), fontSize = 12.sp)
                    if(item.unread) Box(Modifier.padding(start = 12.dp, top = 5.dp).size(8.dp).background(Coral, CircleShape))
                }
                Text(item.detail, color = Muted, fontSize = 13.sp, lineHeight = 17.sp)
                if(item.action != null || item.badge != null) FlowRow(
                    Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    item.action?.let { label ->
                        Surface(onClick = open, shape = CircleShape, color = Color.Black) {
                            Text(label, Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, lineHeight = 12.sp)
                        }
                    }
                    item.badge?.let { label ->
                        Surface(shape = CircleShape, color = Color.White.copy(alpha = .9f), modifier = Modifier.padding(top = 3.dp)) {
                            Text(label, Modifier.padding(horizontal = 9.dp, vertical = 2.dp), color = Muted,
                                fontWeight = FontWeight.SemiBold, fontSize = 10.sp, lineHeight = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationIcon(glyph: NotificationGlyph) {
    val bg = when(glyph) {
        NotificationGlyph.Group -> Brush.linearGradient(listOf(Color(0xFFFF8A70), Color(0xFFF36854)))
        NotificationGlyph.Heart -> Brush.linearGradient(listOf(Ink, Ink))
        else -> Brush.linearGradient(listOf(Color(0xFFFDDCD0), Color(0xFFFDDCD0)))
    }
    Box(Modifier.size(42.dp).background(bg, CircleShape), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(22.dp)) {
            val color = if(glyph in listOf(NotificationGlyph.Group, NotificationGlyph.Heart)) Color.White else Color(0xFFE65239)
            val s = size.width / 24f
            fun path(block: Path.() -> Unit) = drawPath(Path().apply(block), color, style = Stroke(1.7f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            withTransform({scale(s,s,Offset.Zero)}) {
                when(glyph) {
                    NotificationGlyph.Heart -> path {moveTo(12f,20f);cubicTo(9f,18f,3f,14f,3f,8f);cubicTo(3f,2f,10f,2f,12f,7f);cubicTo(15f,1f,22f,3f,21f,9f);cubicTo(21f,14f,15f,19f,12f,20f)}
                    NotificationGlyph.Check -> path {moveTo(5f,12f);lineTo(10f,17f);lineTo(19f,7f)}
                    NotificationGlyph.Car -> path {moveTo(4f,14f);lineTo(7f,7f);lineTo(19f,7f);lineTo(21f,14f);close();moveTo(3f,14f);lineTo(22f,14f);lineTo(22f,17f);lineTo(3f,17f);close()}
                    NotificationGlyph.Group -> {
                        drawCircle(color,3f,Offset(9f,8f),style=Stroke(1.7f))
                        path {moveTo(2f,20f);cubicTo(2f,12f,16f,12f,16f,20f);moveTo(17f,5f);cubicTo(23f,4f,23f,12f,18f,12f);moveTo(19f,15f);cubicTo(22f,16f,22f,18f,22f,20f)}
                    }
                    NotificationGlyph.Message -> path {moveTo(3f,4f);lineTo(21f,4f);lineTo(21f,17f);lineTo(10f,17f);lineTo(5f,21f);lineTo(5f,17f);lineTo(3f,17f);close()}
                    NotificationGlyph.Star -> {
                        val star=Path()
                        repeat(10) {i ->
                            val a=Math.PI * i/5-Math.PI/2
                            val r=if(i%2==0)10f else 4.6f
                            val x=12f+r*kotlin.math.cos(a).toFloat();val y=12f+r*kotlin.math.sin(a).toFloat()
                            if(i==0) star.moveTo(x,y) else star.lineTo(x,y)
                        }
                        star.close();drawPath(star,color)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsGlyph() {
    Canvas(Modifier.size(20.dp)) {
        val stroke = 1.5.dp.toPx()
        drawLine(Ink,Offset(size.width*.18f,size.height*.3f),Offset(size.width*.82f,size.height*.3f),stroke,StrokeCap.Round)
        drawLine(Ink,Offset(size.width*.18f,size.height*.7f),Offset(size.width*.82f,size.height*.7f),stroke,StrokeCap.Round)
        drawCircle(Color(0xFFFCE9E0),3.dp.toPx(),Offset(size.width*.62f,size.height*.3f))
        drawCircle(Ink,2.dp.toPx(),Offset(size.width*.62f,size.height*.3f),style=Stroke(stroke))
        drawCircle(Color(0xFFFCE9E0),3.dp.toPx(),Offset(size.width*.38f,size.height*.7f))
        drawCircle(Ink,2.dp.toPx(),Offset(size.width*.38f,size.height*.7f),style=Stroke(stroke))
    }
}

@Composable
fun DemoNotificationScreen(back: () -> Unit, navigate: (String) -> Unit) {
    var read by rememberSaveable {mutableStateOf(false)}
    val items = demoNotifications().map {it.copy(unread=it.unread && !read)}
    NotificationScreen(items,back,navigate,{ item ->
        navigate(when(item.id) {
            "group" -> BookingRoute.Confirmation(DemoRideIds.HOME_OFFER).encode()
            "saved" -> BookingRoute.OfferDetails(DemoRideIds.HOME_OFFER).encode()
            "request" -> "my_trips"
            "booking" -> BookingRoute.Ticket(DemoRideIds.CONFIRMED_BOOKING).encode()
            else -> "review"
        })
    },{read=true})
}

private fun demoNotifications() = listOf(
    NotificationItem("group","Driver found for your group","Kolej Kediaman 10 → FSKTM · Thu 8:00 AM",NotificationCategory.Matches,NotificationGlyph.Group,"2m",unread=true,action="Confirm reservation",badge="2 seats left"),
    NotificationItem("saved","New ride on your saved route","Kolej Kediaman 10 → Fac. of Engineering · Wed 8:15 AM",NotificationCategory.Matches,NotificationGlyph.Heart,"14m",unread=true,action="View offer",badge="3 seats · RM4"),
    NotificationItem("request","Passenger request on your route","Pantai Dalam → UM · Fri 8:00 AM",NotificationCategory.Bookings,NotificationGlyph.Car,"1h",action="View request",badge="1 seat requested"),
    NotificationItem("booking","Booking confirmed · UMP–2841","2 seats × RM4 = RM8 · Wed 8:15 AM",NotificationCategory.Bookings,NotificationGlyph.Check,"Yesterday",earlier=true),
    NotificationItem("review","Rate your trip with Dorsett","Trip completed · 2.4 kg CO₂ saved so far this week",NotificationCategory.Bookings,NotificationGlyph.Star,"Mon",earlier=true),
)

@Preview(name="Notifications",widthDp=390,heightDp=844)
@Preview(name="Notifications narrow",widthDp=320,heightDp=720,fontScale=1.3f)
@Preview(name="Notifications wide",widthDp=600,heightDp=900)
@Composable
private fun NotificationPreview() { UMPoolingTheme { DemoNotificationScreen({}, {}) } }
