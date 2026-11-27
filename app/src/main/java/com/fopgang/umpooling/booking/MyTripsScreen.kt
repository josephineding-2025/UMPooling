package com.fopgang.umpooling.booking

import androidx.annotation.StringRes
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.*
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fopgang.umpooling.R
import com.fopgang.umpooling.ui.components.*
import com.fopgang.umpooling.ui.theme.UMPoolingTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class MyTripStatus(@StringRes val labelRes: Int) {
    Pending(R.string.trips_pending),
    Confirmed(R.string.confirmed),
    Active(R.string.trips_active),
    Completed(R.string.trips_completed),
    Cancelled(R.string.trips_cancelled),
}
data class MyTripItem(
    val id: String,
    val status: MyTripStatus,
    val origin: String?,
    val destination: String?,
    val departure: String = "",
    val reference: String = "",
    val vehicle: String = "",
    val seats: Int? = null,
    val costPerSeat: Double? = null,
    @StringRes val statusLabelRes: Int? = null,
    val summary: String = "",
    val availableSeats: Int? = null,
    val detailRoute: String,
    val chatRoute: String? = null,
    val sosRoute: String? = null,
)

/** Responsive trip list. Resource IDs/routes remain separate from display text. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MyTripsScreen(
    trips: List<MyTripItem>,
    navigate: (String) -> Unit,
    onSos: (MyTripItem) -> Unit = {it.sosRoute?.let {route->navigate(route+"?prepared=true")}},
    loading: Boolean = false,
    error: String? = null,
    refresh: (() -> Unit)? = null,
) {
    var filter by rememberSaveable { mutableStateOf<MyTripStatus?>(null) }
    val visible=trips.filter {filter == null || it.status == filter}
        .sortedBy {when(it.status) {MyTripStatus.Active->0;MyTripStatus.Confirmed->1;MyTripStatus.Pending->2;else->3}}
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp,
        LocalTextStyle provides LocalTextStyle.current.copy(fontFamily=RideFont,fontSize=13.sp,lineHeight=18.sp,color=TripsInk)) {
        Box(Modifier.fillMaxSize().background(Color(0xFFFCE9E0)).drawWithCache {
            val top=Brush.linearGradient(listOf(Color(0xFFFFFCF9),Color(0xFFFBD5CA)),Offset.Zero,Offset(size.width,size.height*.24f))
            val light=Brush.radialGradient(listOf(Color(0xFFFDECE3),Color.Transparent),Offset(0f,size.height*.68f),size.width*1.5f)
            onDrawBehind {drawRect(top);drawRect(light)}
        }) {
            Column(Modifier.fillMaxSize()) {
                LazyColumn(Modifier.weight(1f).fillMaxWidth(),
                    contentPadding=PaddingValues(start=20.dp,end=20.dp,top=38.dp,bottom=24.dp),
                    verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    item {
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                            Text(stringResource(R.string.my_trips),Modifier.weight(1f),fontSize=29.sp,lineHeight=36.sp,fontWeight=FontWeight.ExtraBold)
                            Surface(onClick={navigate("notifications")},modifier=Modifier.size(46.dp),
                                shape=CircleShape,color=Color.White.copy(alpha=.25f),border=BorderStroke(1.dp,Color.White)) {
                                Box(contentAlignment=Alignment.Center) {
                                    FeatureIcon(FeatureSymbol.Bell,description=stringResource(R.string.notifications))
                                    Box(Modifier.align(Alignment.TopEnd).padding(top=11.dp,end=12.dp)
                                        .size(9.dp).background(Color(0xFFF36955),RoundedCornerShape(3.dp))
                                        .border(1.dp,Color.White,RoundedCornerShape(3.dp)))
                                }
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            (listOf(null) + MyTripStatus.entries.filter { status ->
                                status in listOf(MyTripStatus.Pending, MyTripStatus.Confirmed, MyTripStatus.Active) || trips.any { it.status == status }
                            }).forEach { status ->
                                val count=trips.count {it.status == status}
                                Surface(onClick={filter = status},shape=CircleShape,
                                    color=if(filter == status) TripsInk else Color.White.copy(alpha=.5f),
                                    border=if(filter == status) null else BorderStroke(1.dp,Color.White)) {
                                    Row(Modifier.heightIn(min=34.dp).padding(horizontal=16.dp,vertical=8.dp),
                                        verticalAlignment=Alignment.CenterVertically) {
                                        Text(stringResource(status?.labelRes ?: R.string.ui_all),fontSize=13.sp,fontWeight=FontWeight.SemiBold,color=if(filter == status) Color.White else TripsInk)
                                        if(status != null) Text(" $count",color=if(filter == status) Color.White else Color(0xFFCF4F35),fontSize=13.sp,fontWeight=FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    if(loading) item {LinearProgressIndicator(Modifier.fillMaxWidth())}
                    if(error!=null) item {
                        Text(error,color=MaterialTheme.colorScheme.error)
                        refresh?.let {TextButton(it) {Text(stringResource(R.string.booking_retry))}}
                    }
                    if(visible.isEmpty() && !loading) item {
                        Surface(shape=RoundedCornerShape(25.dp),color=Color.White.copy(alpha=.7f)) {
                            Column(Modifier.fillMaxWidth().padding(24.dp)) {
                                Text(stringResource(R.string.trips_empty),fontWeight=FontWeight.Bold,fontSize=17.sp)
                                Text(stringResource(if (filter == null) R.string.trips_empty_all else R.string.trips_empty_filter))
                                TextButton({navigate("find_ride")}) {Text(stringResource(R.string.find_ride))}
                            }
                        }
                    }
                    items(visible,key={it.id}) {trip->
                        when(trip.status) {
                            MyTripStatus.Active -> ActiveTripCard(trip,navigate) {onSos(trip)}
                            MyTripStatus.Pending -> PendingTripCard(trip,navigate)
                            else -> BookingTripCard(trip,navigate)
                        }
                    }
                }
                CommuterNavigation("my_trips",navigate,Modifier.padding(horizontal=14.dp).padding(bottom=30.dp))
            }
        }
    }
}

private val TripsInk=Color(0xFF272322)
private val TripsMuted=Color(0xFF805F55)

@Composable
private fun TripPill(label:String, dark:Boolean=false, active:Boolean=false) {
    Surface(shape=CircleShape,color=when {active->Color(0xFFF8755E);dark->Color(0xFF322B28);else->Color.White}) {
        Row(Modifier.padding(horizontal=10.dp,vertical=4.dp),verticalAlignment=Alignment.CenterVertically) {
            if(active) {Box(Modifier.size(6.dp).background(Color.White,CircleShape));Spacer(Modifier.width(6.dp))}
            Text(label,fontWeight=FontWeight.Bold,fontSize=11.sp,lineHeight=14.sp,
                color=if(dark || active) Color.White else TripsMuted)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveTripCard(trip:MyTripItem,navigate:(String)->Unit,sos:()->Unit) {
    Surface(onClick={navigate(trip.detailRoute)},shape=RoundedCornerShape(26.dp),
        color=Color.White.copy(alpha=.75f),border=BorderStroke(1.dp,Color.White)) {
        Column(Modifier.padding(15.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
                verticalArrangement=Arrangement.spacedBy(6.dp)) {
                TripPill(stringResource(R.string.trips_active_label),active=true)
                Text(trip.departure,color=TripsMuted,fontSize=11.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(top=4.dp))
            }
            Row(verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
                    Text("${trip.originLabel()} → ${trip.destinationLabel()}",fontSize=14.sp,lineHeight=19.sp,fontWeight=FontWeight.Bold)
                    Text(listOf(trip.vehicle,trip.seats?.let { pluralStringResource(R.plurals.home_seats, it, it) }.orEmpty()).filter {it.isNotBlank()}.joinToString(" · "),
                        color=TripsMuted,fontSize=11.sp)
                }
                trip.chatRoute?.let {route->
                    Surface(onClick={navigate(route)},modifier=Modifier.padding(start=8.dp).size(38.dp),
                        shape=CircleShape,color=Color(0xFFFDDACD)) {
                        Box(contentAlignment=Alignment.Center) {MessageOutline()}
                    }
                }
            }
            if(trip.sosRoute!=null) HoldSosControl(trip.id,sos)
            else TextButton({navigate(trip.detailRoute)}) {Text(stringResource(R.string.ui_view_trip))}
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BookingTripCard(trip:MyTripItem,navigate:(String)->Unit) {
    Surface(onClick={navigate(trip.detailRoute)},shape=RoundedCornerShape(26.dp),
        color=Color.White.copy(alpha=.78f),border=BorderStroke(1.dp,Color.White)) {
        Column(Modifier.padding(15.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
            FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalArrangement=Arrangement.spacedBy(6.dp)) {
                TripPill(stringResource(trip.statusLabelRes ?: when (trip.status) {
                    MyTripStatus.Confirmed -> R.string.trips_confirmed_label
                    MyTripStatus.Completed -> R.string.trips_completed_label
                    else -> R.string.trips_cancelled_label
                }),dark=true)
                Text(trip.reference,Modifier.padding(top=3.dp),color=TripsMuted,fontSize=11.sp,fontWeight=FontWeight.Bold)
            }
            Box(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(start=26.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalArrangement=Arrangement.spacedBy(4.dp)) {
                        Text(trip.originLabel(),fontSize=14.sp,fontWeight=FontWeight.Bold)
                        Text(trip.departure,color=TripsMuted,fontSize=11.sp,fontWeight=FontWeight.SemiBold)
                    }
                    Text(trip.destinationLabel(),fontSize=14.sp,fontWeight=FontWeight.Bold)
                }
                RouteDots(Modifier.matchParentSize())
            }
            HorizontalDivider(color=Color(0xFFEED4C8))
            FlowRow(Modifier.fillMaxWidth().padding(top=5.dp),horizontalArrangement=Arrangement.SpaceBetween,
                verticalArrangement=Arrangement.spacedBy(8.dp)) {
                val cost=trip.costPerSeat
                val summary = trip.summaryLabel()
                val breakdown = if (trip.seats != null && cost != null) {
                    stringResource(R.string.trips_price_breakdown,
                        pluralStringResource(R.plurals.home_seats, trip.seats, trip.seats), money(cost), trip.seats)
                } else null
                Text(buildAnnotatedString {
                    if (breakdown != null && trip.seats != null && cost != null) {
                        append(breakdown)
                        append(" ")
                        withStyle(SpanStyle(fontWeight=FontWeight.Bold,color=TripsInk)) {append("RM${money(cost*trip.seats)}")}
                    } else append(summary)
                },
                    Modifier.padding(top=8.dp),fontSize=12.sp,color=Color(0xFF6F5A52))
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    trip.chatRoute?.let {route->Surface(onClick={navigate(route)},shape=CircleShape,color=Color.White) {
                        Text(stringResource(R.string.trips_message_driver),Modifier.padding(horizontal=12.dp,vertical=9.dp),fontWeight=FontWeight.Bold,color=Color.Black,fontSize=11.sp)
                    }}
                    Surface(onClick={navigate(trip.detailRoute)},shape=CircleShape,color=TripsInk) {
                        Text(stringResource(if (trip.status == MyTripStatus.Confirmed && trip.statusLabelRes == null) R.string.trips_ticket else R.string.ui_details),
                            Modifier.padding(horizontal=12.dp,vertical=9.dp),color=Color.White,fontSize=11.sp,fontWeight=FontWeight.SemiBold)
                    }
                }
            }
            Text(stringResource(R.string.trips_contribution_note),color=TripsMuted,fontSize=10.sp,lineHeight=14.sp)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PendingTripCard(trip:MyTripItem,navigate:(String)->Unit) {
    val shape=RoundedCornerShape(26.dp)
    Surface(onClick={navigate(trip.detailRoute)},shape=shape,color=Color.White.copy(alpha=.65f),
        modifier=Modifier.drawWithCache {
            val outline=shape.createOutline(size,layoutDirection,this)
            onDrawWithContent {
                drawContent()
                drawOutline(outline,Color(0xFFE8A38F),style=Stroke(1.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(),3.dp.toPx()))))
            }
        }) {
        Column(Modifier.padding(15.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalArrangement=Arrangement.spacedBy(6.dp)) {
                TripPill(stringResource(trip.statusLabelRes ?: R.string.trips_pending_group))
                Text(trip.departure,color=TripsMuted,fontSize=11.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(top=4.dp))
            }
            Text(stringResource(R.string.trips_pickup_route, trip.originLabel(), trip.destinationLabel()),fontSize=14.sp,lineHeight=19.sp,fontWeight=FontWeight.Bold)
            FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                Text(trip.summaryLabel(),color=Color(0xFF6F5A52),fontSize=12.sp)
                TextButton({navigate(trip.detailRoute)},contentPadding=PaddingValues(0.dp),modifier=Modifier.heightIn(min=24.dp)) {
                    Text(stringResource(if (trip.statusLabelRes == null) R.string.trips_review_offer else R.string.ui_view_details),color=Color(0xFFA9442F),fontWeight=FontWeight.Bold,fontSize=11.sp)
                }
            }
            Text(stringResource(if (trip.statusLabelRes == R.string.trips_your_offer) R.string.trips_manage_offer else R.string.trips_no_seat_reserved),color=TripsMuted,fontSize=10.sp,lineHeight=14.sp)
        }
    }
}

@Composable
private fun HoldSosControl(id:String,activate:()->Unit) {
    val holdLabel = stringResource(R.string.trips_hold_sos)
    val openLabel = stringResource(R.string.trips_open_sos)
    val scope=rememberCoroutineScope()
    val action by rememberUpdatedState(activate)
    var holding by remember {mutableStateOf(false)}
    Box(Modifier.fillMaxWidth().heightIn(min=44.dp).background(
        Brush.horizontalGradient(listOf(Color(0xFF934A3D),Color(0xFF2B1713))),CircleShape)
        .semantics {
            contentDescription=holdLabel
            customActions=listOf(CustomAccessibilityAction(openLabel){action();true})
        }
        .pointerInput(id) {
            detectTapGestures(onPress={
                holding=true
                val job=scope.launch {delay(3000);action()}
                try {tryAwaitRelease()} finally {job.cancel();holding=false}
            })
        }.padding(horizontal=16.dp,vertical=11.dp),contentAlignment=Alignment.Center) {
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)) {
            Text("⚠",color=Color.White,fontSize=16.sp)
            Text(if(holding) stringResource(R.string.trips_keep_holding) else holdLabel,fontSize=14.sp,fontWeight=FontWeight.Bold,color=Color.White)
        }
    }
}

@Composable
private fun MessageOutline() {
    Canvas(Modifier.size(19.dp)) {
        val p=Path().apply {
            moveTo(size.width*.18f,size.height*.75f);cubicTo(0f,size.height*.1f,size.width,size.height*.05f,size.width*.8f,size.height*.65f)
            quadraticTo(size.width*.65f,size.height*.95f,size.width*.33f,size.height*.8f)
            lineTo(size.width*.15f,size.height*.85f);close()
        }
        drawPath(p,Color(0xFFBC442D),style=Stroke(1.5.dp.toPx(),cap=StrokeCap.Round))
    }
}
@Composable
private fun RouteDots(modifier:Modifier) {
    Canvas(modifier) {
        val x=7.5.dp.toPx()
        drawCircle(Color(0xFFF36D57),5.dp.toPx(),Offset(x,7.dp.toPx()),style=Stroke(3.dp.toPx()))
        val pinY=size.height-7.dp.toPx()
        drawLine(Color(0xFFE5B2A1),Offset(x,15.dp.toPx()),Offset(x,pinY-8.dp.toPx()),1.5.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(),3.dp.toPx())))
        val p=Path().apply {moveTo(x,pinY+5.dp.toPx());cubicTo(x-7.dp.toPx(),pinY-3.dp.toPx(),x-5.dp.toPx(),pinY-6.dp.toPx(),x,pinY-6.dp.toPx());cubicTo(x+5.dp.toPx(),pinY-6.dp.toPx(),x+7.dp.toPx(),pinY-3.dp.toPx(),x,pinY+5.dp.toPx())}
        drawPath(p,TripsInk)
        drawCircle(Color.White,1.dp.toPx(),Offset(x,pinY-3.dp.toPx()))
    }
}
@Composable
private fun MyTripItem.originLabel() = origin ?: stringResource(R.string.trips_pickup_missing)

@Composable
private fun MyTripItem.destinationLabel() = destination ?: stringResource(R.string.trips_destination_missing)

@Composable
private fun MyTripItem.summaryLabel(): String {
    val count = availableSeats
    val cost = costPerSeat
    return when {
        count != null && cost != null -> stringResource(
            R.string.trips_offer_summary,
            pluralStringResource(R.plurals.trips_seats_available, count, count),
            money(cost),
        )
        statusLabelRes == R.string.trips_pending_booking -> stringResource(R.string.trips_awaiting_booking)
        else -> summary
    }
}

internal fun money(value:Double)=java.text.DecimalFormat("0.##").format(value)

@Composable
internal fun demoTripItems()=listOf(
    MyTripItem("demo-active",MyTripStatus.Active,"Kolej Kediaman 10","FSKTM",stringResource(R.string.trips_demo_started),
        vehicle="Toyota Vios · WXY 1234",seats=1,detailRoute="active_trip/demo-active",chatRoute="chat/booking/demo-active",sosRoute="sos/demo-active"),
    MyTripItem("demo-confirmed",MyTripStatus.Confirmed,"Kolej Kediaman 10",stringResource(R.string.trips_demo_faculty),stringResource(R.string.trips_demo_wed),"UMP–2841",
        seats=2,costPerSeat=4.0,detailRoute=BookingRoute.Ticket(DemoRideIds.CONFIRMED_BOOKING).encode(),
        chatRoute="chat/booking/"+DemoRideIds.CONFIRMED_BOOKING),
    MyTripItem("demo-pending",MyTripStatus.Pending,"Kolej Kediaman 10","FSKTM",stringResource(R.string.trips_demo_thu),
        summary=stringResource(R.string.trips_demo_summary),detailRoute=BookingRoute.OfferDetails(DemoRideIds.HOME_OFFER).encode()),
)

@Preview(name="My Trips",widthDp=390,heightDp=844)
@Preview(name="My Trips narrow",widthDp=320,heightDp=720,fontScale=1.3f)
@Preview(name="My Trips wide",widthDp=600,heightDp=900)
@Composable
private fun MyTripsPreview() { UMPoolingTheme {MyTripsScreen(demoTripItems(),{})} }
