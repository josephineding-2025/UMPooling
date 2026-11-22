package com.fopgang.umpooling.booking

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fopgang.umpooling.R
import com.fopgang.umpooling.ui.components.FigmaIcon
import com.fopgang.umpooling.ui.theme.UMPoolingTheme

private val TicketInk = Color(0xFF242424)
private val RouteRed = Color(0xFFEF4438)
private val TicketWhite = Color(0xFFFFF8F6)

@Composable
fun RideMatchScreen(offerId: String, back: () -> Unit, onRequestJoin: (String) -> Unit,
    onMessageDriver: (String) -> Unit, unavailable: () -> Unit) {
    RideOfferDetailsContent(offerId, back, onRequestJoin, onMessageDriver, unavailable)
}

@Composable
internal fun RideOfferDetailsContent(
    offerId: String, back: () -> Unit, onRequestJoin: (String) -> Unit,
    onMessageDriver: (String) -> Unit, unavailable: () -> Unit, reviewing: Boolean = false, requestedSeats: Int = 1,
) {
    var saved by rememberSaveable(offerId) { mutableStateOf(false) }
    ProvideTextStyle(MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.SansSerif, lineHeight = 16.sp)) {
    Column(
        Modifier.fillMaxSize().background(Color(0xFFFCE5DF)).drawWithCache {
            val glow = Brush.radialGradient(
                listOf(Color(0xFFFF9997), Color.Transparent),
                Offset(size.width * .43f, size.height * .48f), size.width * .72f
            )
            onDrawBehind { drawRect(glow) }
        }.verticalScroll(rememberScrollState())
    ) {
        Box(Modifier.fillMaxWidth().height(398.dp)) {
            TicketRoute(Modifier.fillMaxWidth().height(295.dp).offset(y = 88.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 60.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = CircleShape, color = TicketWhite) {
                    IconButton(back, Modifier.size(44.dp)) {
                        FigmaIcon(R.drawable.login_frame, description = stringResource(R.string.back))
                    }
                }
                Text(
                    if (reviewing) "Confirm booking" else stringResource(R.string.trip_details), Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TicketInk
                )
                Surface(shape = CircleShape, color = TicketWhite) {
                    IconButton(unavailable, Modifier.size(44.dp)) {
                        FigmaIcon(R.drawable.ticket_frame1, description = stringResource(R.string.see_all))
                    }
                }
            }
        }
        Surface(
            Modifier.fillMaxWidth(), shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
            color = Color.White.copy(alpha = .28f),
            border = androidx.compose.foundation.BorderStroke(.75.dp, Color.White.copy(alpha = .65f))
        ) {
            Column(Modifier.padding(horizontal = 20.dp).padding(top = 33.dp, bottom = 54.dp)) {
                Surface(Modifier.padding(start = 4.dp), shape = CircleShape, color = Color(0xFFFCD1AE)) {
                    Text(if (reviewing) "Booking review" else "Ride Match", Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        color = Color(0xFFB6511C), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(7.dp))
                Surface(shape = RoundedCornerShape(25.dp), color = TicketWhite.copy(alpha = .86f)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp).padding(bottom = 0.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(66.dp, 62.dp).background(
                                    Brush.linearGradient(listOf(Color(0xFFBC957A), Color(0xFF77523F))), CircleShape
                                ), contentAlignment = Alignment.Center
                            ) { Text("DT", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold) }
                            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                                Text("Daniel Tan", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TicketInk)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = CircleShape, color = Color(0xFFFBD8A2)) {
                                        Text("★ 4.9", Modifier.padding(horizontal = 10.dp, vertical = 1.dp),
                                            fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TicketInk)
                                    }
                                    Text("(128 rides)", Modifier.padding(start = 8.dp), fontSize = 13.sp, color = Color(0xFF4D4D4D))
                                }
                            }
                            Surface(shape = CircleShape, color = Color.Black) {
                                IconButton({ onMessageDriver(offerId) }, Modifier.size(46.dp)) {
                                    TicketLineIcon("chat", Color.White, modifier = Modifier.size(24.dp).semantics { contentDescription = "Message driver" })
                                }
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth().heightIn(min = 34.dp), verticalAlignment = Alignment.CenterVertically) {
                            TicketMeta("calendar", "Today, Nov 3\n2:00 PM", Modifier.weight(1.25f))
                            VerticalDivider(Modifier.height(34.dp), color = Color(0xFFEBCDC7))
                            TicketMeta("person", "2 seats\nleft", Modifier.weight(1f))
                            VerticalDivider(Modifier.height(34.dp), color = Color(0xFFEBCDC7))
                            TicketMeta("coins", "RM 6\nper seat", Modifier.weight(1f))
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Surface(shape = RoundedCornerShape(25.dp), color = TicketWhite.copy(alpha = .86f)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Ride Details", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TicketInk)
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Canvas(Modifier.size(25.dp, 56.dp)) {
                                val x = 12.dp.toPx()
                                val top = 7.dp.toPx()
                                val bottom = 40.dp.toPx()
                                for (i in 0..6) {
                                    val y = top + 7.dp.toPx() + i * 2.5.dp.toPx()
                                    drawLine(RouteRed, Offset(x, y), Offset(x, y + 1.3.dp.toPx()), 1.3.dp.toPx())
                                }
                                drawCircle(TicketInk, 5.dp.toPx(), Offset(x, top), style = Stroke(2.dp.toPx()))
                                val pin = Path().apply {
                                    moveTo(x, bottom + 6.dp.toPx())
                                    cubicTo(x - 15.dp.toPx(), bottom - 11.dp.toPx(), x + 15.dp.toPx(), bottom - 11.dp.toPx(), x, bottom + 6.dp.toPx())
                                }
                                drawPath(pin, RouteRed)
                                drawCircle(Color.White, 2.dp.toPx(), Offset(x, bottom - 4.dp.toPx()))
                            }
                            Column(Modifier.weight(1f).padding(start = 11.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                                TicketStop(stringResource(R.string.um_campus), "2:00 PM")
                                TicketStop(stringResource(R.string.mid_valley), "2:30 PM")
                            }
                        }
                    }
                }
                if(reviewing) Text("$requestedSeats seat(s) · RM 4.00 × $requestedSeats = RM ${requestedSeats * 4}.00", Modifier.padding(vertical=12.dp), color=TicketInk)
                Spacer(Modifier.height(13.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Button(
                        { onRequestJoin(offerId) }, Modifier.weight(1f).height(51.dp), shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 12.dp)
                    ) { Text(if (reviewing) "Confirm booking" else "Request to join", fontSize = 17.sp, fontWeight = FontWeight.Bold) }
                    Surface(shape = CircleShape, color = TicketWhite) {
                        IconToggleButton(saved, { saved = it }, Modifier.size(48.dp)) {
                            TicketLineIcon("heart", if (saved) RouteRed else TicketInk, saved,
                                Modifier.size(24.dp).semantics { contentDescription = if (saved) "Unsave ride" else "Save ride" })
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun TicketRoute(modifier: Modifier) {
    BoxWithConstraints(modifier) {
        Image(painterResource(R.drawable.ticket_trip_details_selection1), null, Modifier.matchParentSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.FillBounds)
        Canvas(Modifier.matchParentSize()) {
            val path = Path().apply {
                moveTo(size.width * .1615f, size.height * .802f)
                cubicTo(size.width * .40f, size.height * .69f, size.width * .43f, size.height * .68f, size.width * .51f, size.height * .52f)
                cubicTo(size.width * .60f, size.height * .39f, size.width * .60f, size.height * .36f, size.width * .628f, size.height * .261f)
            }
            drawPath(path, Brush.linearGradient(listOf(RouteRed, Color(0xFFF4A641)),
                Offset(size.width * .16f, size.height * .8f), Offset(size.width * .63f, size.height * .26f)),
                style = Stroke(3.5.dp.toPx(), cap = StrokeCap.Round))
            listOf(Offset(size.width * .1615f, size.height * .802f), Offset(size.width * .628f, size.height * .261f)).forEach {
                drawCircle(RouteRed, 10.dp.toPx(), it)
                drawCircle(Color.White, 6.dp.toPx(), it)
            }
        }
        Image(painterResource(R.drawable.ticket_group), null,
            Modifier.offset(maxWidth * .416f, 154.dp).size(22.dp, 38.dp).rotate(40f))
        Surface(Modifier.align(Alignment.TopEnd).padding(end = 13.dp, top = 51.dp).width(118.dp),
            shape = CircleShape, color = Color.White) {
            Row(Modifier.padding(horizontal = 13.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                FigmaIcon(R.drawable.ticket_frame2, Modifier.size(20.dp))
                Column(Modifier.padding(start = 10.dp)) {
                    Text("UM", fontSize = 14.sp, fontWeight = FontWeight.Bold, lineHeight = 14.sp)
                    Text("(Main\nCampus)", fontSize = 11.sp, lineHeight = 11.sp)
                }
            }
        }
        Surface(Modifier.align(Alignment.BottomStart).padding(start = 22.dp, bottom = 11.dp),
            shape = CircleShape, color = Color.White) {
            Row(Modifier.padding(horizontal = 13.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                FigmaIcon(R.drawable.ticket_frame3, Modifier.size(20.dp))
                Text("Mid Valley\nMegamall", Modifier.padding(start = 10.dp), fontSize = 14.sp,
                    fontWeight = FontWeight.Bold, lineHeight = 14.sp)
            }
        }
    }
}

@Composable
private fun TicketMeta(icon: String, label: String, modifier: Modifier) {
    Row(modifier.padding(horizontal = 1.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TicketLineIcon(icon, modifier = Modifier.size(22.dp))
        Text(label, fontSize = 13.sp, lineHeight = 16.sp, color = TicketInk)
    }
}

@Composable
private fun TicketStop(place: String, time: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(place, Modifier.weight(1f), fontSize = 15.sp, lineHeight = 18.sp, color = TicketInk)
        Text(time, Modifier.padding(start = 8.dp), fontSize = 13.sp, color = Color(0xFF4D4D4D))
    }
}

@Composable
private fun TicketLineIcon(kind: String, color: Color = TicketInk, filled: Boolean = false, modifier: Modifier = Modifier.size(24.dp)) {
    Canvas(modifier) {
        scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
            val stroke = Stroke(1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val p = Path()
            when (kind) {
                "heart" -> {
                    p.moveTo(12f, 21f)
                    p.cubicTo(9f, 19f, 2f, 13f, 3f, 7f)
                    p.cubicTo(4f, 2f, 10f, 2f, 12f, 6f)
                    p.cubicTo(15f, 2f, 21f, 3f, 21f, 8f)
                    p.cubicTo(21f, 13f, 15f, 19f, 12f, 21f)
                    p.close()
                    drawPath(p, color, style = if (filled) androidx.compose.ui.graphics.drawscope.Fill else stroke)
                }
                "chat" -> {
                    p.moveTo(5f, 4f); p.lineTo(19f, 4f); p.quadraticBezierTo(21f, 4f, 21f, 6f)
                    p.lineTo(21f, 14f); p.quadraticBezierTo(21f, 16f, 19f, 16f)
                    p.lineTo(11f, 16f); p.lineTo(6f, 20f); p.lineTo(6f, 16f)
                    p.lineTo(5f, 16f); p.quadraticBezierTo(3f, 16f, 3f, 14f)
                    p.lineTo(3f, 6f); p.quadraticBezierTo(3f, 4f, 5f, 4f)
                    drawPath(p, color, style = stroke)
                }
                "person" -> {
                    drawCircle(color, 4f, Offset(12f, 7f), style = stroke)
                    p.moveTo(4f, 21f); p.cubicTo(4f, 12f, 20f, 12f, 20f, 21f); p.close()
                    drawPath(p, color, style = stroke)
                }
                "coins" -> {
                    drawOval(color, Offset(5f, 3f), androidx.compose.ui.geometry.Size(14f, 6f), style = stroke)
                    p.moveTo(5f, 6f); p.lineTo(5f, 19f); p.cubicTo(5f, 23f, 19f, 23f, 19f, 19f); p.lineTo(19f, 6f)
                    p.moveTo(5f, 12f); p.cubicTo(5f, 16f, 19f, 16f, 19f, 12f)
                    drawPath(p, color, style = stroke)
                }
                else -> {
                    drawRoundRect(color, Offset(3f, 5f), androidx.compose.ui.geometry.Size(18f, 17f),
                        androidx.compose.ui.geometry.CornerRadius(2f), style = stroke)
                    drawLine(color, Offset(3f, 10f), Offset(21f, 10f), 1.8f)
                    drawLine(color, Offset(7f, 3f), Offset(7f, 8f), 1.8f)
                    drawLine(color, Offset(17f, 3f), Offset(17f, 8f), 1.8f)
                }
            }
        }
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun RideMatchPreview() { UMPoolingTheme { RideMatchScreen(DemoRideIds.HOME_OFFER, {}, {}, {}, {}) } }
