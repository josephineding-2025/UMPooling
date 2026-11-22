package com.fopgang.umpooling.booking

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import com.fopgang.umpooling.ui.theme.UMPoolingTheme

@Composable
private fun TicketMap(pickup:String="UM\n(Main\nCampus)",destination:String="Mid Valley\nMegamall") {
    val mapHeight = 290.dp
    Box(Modifier.fillMaxWidth().height(mapHeight)) {
        // Background dots and center glow
        Canvas(Modifier.fillMaxSize()) {
            // Glow
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFFF7C3B1).copy(alpha = 0.8f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.6f),
                    radius = size.width * 0.4f
                ),
                center = Offset(size.width * 0.5f, size.height * 0.6f)
            )

            // Dots
            val dotColor = Color(0xFFEAA89C).copy(alpha = 0.45f)
            val dotRadius = 2.dp.toPx()
            val spacing = 11.dp.toPx()
            for (x in 0..size.width.toInt() step spacing.toInt()) {
                for (y in 0..size.height.toInt() step spacing.toInt()) {
                    drawCircle(color = dotColor, radius = dotRadius, center = Offset(x.toFloat(), y.toFloat()))
                }
            }

            // Path
            val startX = size.width * 0.15f
            val startY = size.height * 0.75f
            val endX = size.width * 0.65f
            val endY = size.height * 0.21f

            val path = Path().apply {
                moveTo(startX, startY)
                cubicTo(
                    size.width * 0.40f, size.height * 0.64f,
                    size.width * 0.53f, size.height * 0.49f,
                    endX, endY
                )
            }
            drawPath(path, brush = Brush.linearGradient(listOf(Color(0xFFEF4438), Color(0xFFF4B653)), Offset(startX, startY), Offset(endX, endY)), style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))

            // Start/End circles
            drawCircle(Color.White, radius = 8.dp.toPx(), center = Offset(startX, startY))
            drawCircle(RideAccent, radius = 8.dp.toPx(), center = Offset(startX, startY), style = Stroke(4.dp.toPx()))

            drawCircle(Color.White, radius = 8.dp.toPx(), center = Offset(endX, endY))
            drawCircle(RideAccent, radius = 8.dp.toPx(), center = Offset(endX, endY), style = Stroke(4.dp.toPx()))

            // Car indicator
            val carX = size.width * 0.44f
            val carY = size.height * 0.55f
            rotate(degrees = 40f, pivot = Offset(carX, carY)) {
                val carW = 17.dp.toPx()
                val carH = 34.dp.toPx()
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(carX - carW / 2, carY - carH / 2),
                    size = Size(carW, carH),
                    cornerRadius = CornerRadius(carW / 2, carW / 2)
                )
                drawRoundRect(
                    color = Color(0xFF2C3246),
                    topLeft = Offset(carX - carW / 2, carY - carH / 2),
                    size = Size(carW, carH * 0.35f),
                    cornerRadius = CornerRadius(carW / 2, carW / 2)
                )
            }
        }

        // Labels
        Surface(
            Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 20.dp),
            shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 0.dp
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Canvas(Modifier.size(20.dp)) {
                    val stroke = Stroke(1.5.dp.toPx())
                    drawRoundRect(RideAccent, style = stroke, topLeft = Offset(size.width * 0.15f, size.height * 0.3f), size = Size(size.width * 0.7f, size.height * 0.7f), cornerRadius = CornerRadius(4.dp.toPx()))
                    val handlePath = Path().apply {
                        moveTo(size.width * 0.35f, size.height * 0.3f)
                        quadraticTo(size.width * 0.5f, -size.height * 0.1f, size.width * 0.65f, size.height * 0.3f)
                    }
                    drawPath(handlePath, RideAccent, style = stroke)
                }
                Text(destination, fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.ExtraBold)
            }
        }

        Surface(
            Modifier.align(Alignment.TopEnd).padding(top = 34.dp, end = 13.dp),
            shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 0.dp
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Canvas(Modifier.size(20.dp)) {
                    val stroke = Stroke(1.5.dp.toPx())
                    val path = Path().apply {
                        moveTo(size.width * 0.5f, size.height * 0.1f)
                        lineTo(size.width * 0.1f, size.height * 0.45f)
                        lineTo(size.width * 0.2f, size.height * 0.45f)
                        lineTo(size.width * 0.2f, size.height * 0.9f)
                        lineTo(size.width * 0.8f, size.height * 0.9f)
                        lineTo(size.width * 0.8f, size.height * 0.45f)
                        lineTo(size.width * 0.9f, size.height * 0.45f)
                        close()
                    }
                    drawPath(path, RideAccent, style = stroke)
                }
                Text(pickup, fontSize = 12.sp, lineHeight = 12.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
fun ConfirmedTripTicketScreen(bookingId: String, back: () -> Unit, messageDriver: (String) -> Unit, sos: (String) -> Unit, reservedSeats: Int = 1,
    details: TicketDetails? = null, onTripControls: (() -> Unit)? = null, loading:Boolean=false,error:String?=null) {
    RidePage {
        Box(Modifier.padding(horizontal = 20.dp)) { RideHeader("Trip Details",back,true) }
        if(loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let {Text(it,Modifier.padding(horizontal=20.dp),color=MaterialTheme.colorScheme.error)}
        if(details==null) TicketMap() else TicketMap(details.pickup,details.destination)
        Surface(shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp), color = Color(0xFFFFD6D1).copy(alpha = .6f)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(shape = CircleShape, color = Color(0xFFFAD0AC)) { Text(details?.status ?: "✓ Seat Confirmed",Modifier.padding(horizontal = 14.dp,vertical = 7.dp),color = Color(0xFFB65C28),fontWeight = FontWeight.Bold,fontSize = 15.sp) }
                Surface(shape = RoundedCornerShape(28.dp),color = Color(0xFFFFF4F1)) {
                    Column(Modifier.padding(22.dp),verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Departure",color = RideMuted,fontSize = 12.sp,fontWeight = FontWeight.Bold)
                                Text(details?.departure ?: "Mon, 2 Nov · 08:15",fontSize = 22.sp,fontWeight = FontWeight.ExtraBold)
                            }
                            Surface(shape = CircleShape,color = RideInk,shadowElevation = 0.dp) { Text(details?.status ?: "Confirmed",Modifier.padding(10.dp),color = Color.White,fontSize = 12.sp,fontWeight = FontWeight.Bold) }
                        }
                        TicketStop("PICKUP",details?.pickup ?: "Kolej Kediaman 11, main lobby",false)
                        TicketStop(if(details==null) "DESTINATION · ETA 08:35" else "DESTINATION",details?.destination ?: "Faculty of Engineering, Block A",true)
                        Canvas(Modifier.fillMaxWidth().height(12.dp)) {
                            drawLine(Color(0xFFF0CBBB),Offset(0f,size.height/2),Offset(size.width,size.height/2),2.dp.toPx(),pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(),4.dp.toPx())))
                        }
                        TicketPerson(if(details==null) "HR" else "D",details?.driverName ?: "Hafiz Rahman",details?.vehicle ?: "Driver · Grey Myvi · WXY 4821",false,verified=details==null)
                        TicketPerson(if(details==null) "AS" else "P",details?.passengerName ?: "Aina Sofea (you)",if(details==null) "Passenger · Faculty of Engineering" else "Passenger",true,verified=details==null)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Glass(Modifier.weight(1f)) { Text("Seat",color = RideMuted,fontSize = 12.sp); Text(if(details==null && reservedSeats==1) "1 of 3 · Rear left" else "$reservedSeats seats reserved",fontSize = 16.sp,fontWeight = FontWeight.ExtraBold) }
                            Glass(Modifier.weight(1f)) { Text("Suggested contribution",color = Color(0xFFA34E36),fontSize = 12.sp,fontWeight = FontWeight.Bold); Text("RM ${String.format(java.util.Locale.getDefault(),"%.2f",details?.costPerSeat ?: 4.0)}",fontSize = 18.sp,fontWeight = FontWeight.ExtraBold); Text("Total RM ${String.format(java.util.Locale.getDefault(),"%.2f",reservedSeats*(details?.costPerSeat ?: 4.0))}",fontSize=12.sp) }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Canvas(Modifier.weight(1f).height(36.dp)) {
                                var x = 0f
                                var index = 0
                                while(x < size.width) {
                                    val bar = (if(index % 4 == 0) 3f else if(index % 3 == 0) 1f else 2f).dp.toPx()
                                    drawRect(RideInk,Offset(x,0f),Size(bar,size.height))
                                    x += bar + (if(index % 3 == 0) 3f else 2f).dp.toPx()
                                    index++
                                }
                            }
                            Text(bookingId,color = RideMuted,fontSize = 12.sp,fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    RideAction("Message driver",Modifier.weight(1.7f)) { messageDriver(bookingId) }
                    OutlinedButton({ sos(bookingId) },enabled=details?.canSos ?: true,modifier=Modifier.weight(1f).heightIn(min = 54.dp),shape = CircleShape,border = BorderStroke(1.dp,RideAccent)) { Text("SOS",color = Color(0xFFAB4934),fontSize = 17.sp,fontWeight = FontWeight.ExtraBold) }
                }
                onTripControls?.let { RideAction("Trip controls",action=it) }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
@Composable
private fun TicketStop(label: String, place: String, destination: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.padding(top = 5.dp).size(14.dp).background(if(destination) RideAccent else RideInk,if(destination) RoundedCornerShape(4.dp) else CircleShape))
        Column { Text(label,color = RideMuted,fontSize = 11.sp,fontWeight = FontWeight.Bold); Text(place,fontSize = 16.sp,fontWeight = FontWeight.ExtraBold) }
    }
}
@Composable
private fun TicketPerson(initials: String, name: String, description: String, passenger: Boolean, verified:Boolean=true) {
    Row(verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(44.dp).background(if(passenger) Color.Black else Color(0xFFFCE0D2),CircleShape),contentAlignment = Alignment.Center) { Text(initials,color = if(passenger) Color.White else RideInk,fontWeight = FontWeight.Bold) }
        Column(Modifier.weight(1f)) { Text(name,fontSize = 15.sp,fontWeight = FontWeight.ExtraBold); Text(description,color = RideMuted,fontSize = 12.sp,lineHeight = 16.sp) }
        if(verified) Surface(shape = CircleShape,color = Color(0xFFFFE2D3)) { Text("Verified",Modifier.padding(8.dp),fontSize = 11.sp,fontWeight = FontWeight.Bold,color = Color(0xFF84452D)) }
    }
}
@Preview(name = "Ticket phone",widthDp = 390,heightDp = 1068)
@Preview(name = "Ticket small",widthDp = 320,heightDp = 640)
@Composable
fun ConfirmedTicketPreview() { UMPoolingTheme { ConfirmedTripTicketScreen(DemoRideIds.CONFIRMED_BOOKING,{},{},{}) } }
