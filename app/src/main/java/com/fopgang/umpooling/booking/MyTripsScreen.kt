package com.fopgang.umpooling.booking

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fopgang.umpooling.booking.*
import com.fopgang.umpooling.ui.components.*
import com.fopgang.umpooling.ui.theme.UMPoolingTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class MyTripStatus { Pending, Confirmed, Active, Completed, Cancelled }
data class MyTripItem(
    val id: String,
    val status: MyTripStatus,
    val origin: String,
    val destination: String,
    val departure: String = "",
    val reference: String = "",
    val vehicle: String = "",
    val seats: Int? = null,
    val costPerSeat: Double? = null,
    val statusLabel: String? = null,
    val summary: String = "",
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
    var filter by rememberSaveable {mutableStateOf("All")}
    val visible=trips.filter {filter=="All" || it.status.name==filter}
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
                            Text("My Trips",Modifier.weight(1f),fontSize=29.sp,lineHeight=36.sp,fontWeight=FontWeight.ExtraBold)
                            Surface(onClick={navigate("notifications")},modifier=Modifier.size(46.dp),
                                shape=CircleShape,color=Color.White.copy(alpha=.25f),border=BorderStroke(1.dp,Color.White)) {
                                Box(contentAlignment=Alignment.Center) {
                                    FeatureIcon(FeatureSymbol.Bell,description="Notifications")
                                    Box(Modifier.align(Alignment.TopEnd).padding(top=11.dp,end=12.dp)
                                        .size(9.dp).background(Color(0xFFF36955),RoundedCornerShape(3.dp))
                                        .border(1.dp,Color.White,RoundedCornerShape(3.dp)))
                                }
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            (listOf("All")+MyTripStatus.entries.filter {s->s in listOf(MyTripStatus.Pending,MyTripStatus.Confirmed,MyTripStatus.Active) || trips.any {it.status==s}}.map {it.name}).forEach {label->
                                val count=trips.count {it.status.name==label}
                                Surface(onClick={filter=label},shape=CircleShape,
                                    color=if(filter==label) TripsInk else Color.White.copy(alpha=.5f),
                                    border=if(filter==label) null else BorderStroke(1.dp,Color.White)) {
                                    Row(Modifier.heightIn(min=34.dp).padding(horizontal=16.dp,vertical=8.dp),
                                        verticalAlignment=Alignment.CenterVertically) {
                                        Text(label,fontSize=13.sp,fontWeight=FontWeight.SemiBold,color=if(filter==label) Color.White else TripsInk)
                                        if(label!="All") Text(" $count",color=if(filter==label) Color.White else Color(0xFFCF4F35),fontSize=13.sp,fontWeight=FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    if(loading) item {LinearProgressIndicator(Modifier.fillMaxWidth())}
                    if(error!=null) item {
                        Text(error,color=MaterialTheme.colorScheme.error)
                        refresh?.let {TextButton(it) {Text("Retry")}}
                    }
                    if(visible.isEmpty() && !loading) item {
                        Surface(shape=RoundedCornerShape(25.dp),color=Color.White.copy(alpha=.7f)) {
                            Column(Modifier.fillMaxWidth().padding(24.dp)) {
                                Text("No trips yet",fontWeight=FontWeight.Bold,fontSize=17.sp)
                                Text(if(filter=="All") "Your bookings and ride offers will appear here." else "No ${filter.lowercase()} trips.")
                                TextButton({navigate("find_ride")}) {Text("Find a ride")}
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
                TripPill("Active trip",active=true)
                Text(trip.departure,color=TripsMuted,fontSize=11.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(top=4.dp))
            }
            Row(verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
                    Text("${trip.origin} → ${trip.destination}",fontSize=14.sp,lineHeight=19.sp,fontWeight=FontWeight.Bold)
                    Text(listOf(trip.vehicle,trip.seats?.let{"$it seat${if(it==1) "" else "s"}"}.orEmpty()).filter {it.isNotBlank()}.joinToString(" · "),
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
            else TextButton({navigate(trip.detailRoute)}) {Text("View trip")}
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
                TripPill(trip.statusLabel ?: when(trip.status){MyTripStatus.Confirmed->"Confirmed booking";MyTripStatus.Completed->"Completed trip";else->"Cancelled trip"},dark=true)
                Text(trip.reference,Modifier.padding(top=3.dp),color=TripsMuted,fontSize=11.sp,fontWeight=FontWeight.Bold)
            }
            Box(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(start=26.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalArrangement=Arrangement.spacedBy(4.dp)) {
                        Text(trip.origin,fontSize=14.sp,fontWeight=FontWeight.Bold)
                        Text(trip.departure,color=TripsMuted,fontSize=11.sp,fontWeight=FontWeight.SemiBold)
                    }
                    Text(trip.destination,fontSize=14.sp,fontWeight=FontWeight.Bold)
                }
                RouteDots(Modifier.matchParentSize())
            }
            HorizontalDivider(color=Color(0xFFEED4C8))
            FlowRow(Modifier.fillMaxWidth().padding(top=5.dp),horizontalArrangement=Arrangement.SpaceBetween,
                verticalArrangement=Arrangement.spacedBy(8.dp)) {
                val cost=trip.costPerSeat
                Text(buildAnnotatedString {
                    if(trip.seats!=null && cost!=null) {
                        append("${trip.seats} ${if(trip.seats==1) "seat" else "seats"} · RM${money(cost)} × ${trip.seats} = ")
                        withStyle(SpanStyle(fontWeight=FontWeight.Bold,color=TripsInk)) {append("RM${money(cost*trip.seats)}")}
                    } else append(trip.summary)
                },
                    Modifier.padding(top=8.dp),fontSize=12.sp,color=Color(0xFF6F5A52))
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    trip.chatRoute?.let {route->Surface(onClick={navigate(route)},shape=CircleShape,color=Color.White) {
                        Text("Message driver",Modifier.padding(horizontal=12.dp,vertical=9.dp),fontWeight=FontWeight.Bold,color=Color.Black,fontSize=11.sp)
                    }}
                    Surface(onClick={navigate(trip.detailRoute)},shape=CircleShape,color=TripsInk) {
                        Text(if(trip.status==MyTripStatus.Confirmed && trip.statusLabel==null) "Ticket" else "Details",
                            Modifier.padding(horizontal=12.dp,vertical=9.dp),color=Color.White,fontSize=11.sp,fontWeight=FontWeight.SemiBold)
                    }
                }
            }
            Text("Contribution is settled directly with the driver, outside the app.",color=TripsMuted,fontSize=10.sp,lineHeight=14.sp)
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
                TripPill(trip.statusLabel ?: "Pending group request")
                Text(trip.departure,color=TripsMuted,fontSize=11.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(top=4.dp))
            }
            Text("Pickup: ${trip.origin} → ${trip.destination}",fontSize=14.sp,lineHeight=19.sp,fontWeight=FontWeight.Bold)
            FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                Text(trip.summary,color=Color(0xFF6F5A52),fontSize=12.sp)
                TextButton({navigate(trip.detailRoute)},contentPadding=PaddingValues(0.dp),modifier=Modifier.heightIn(min=24.dp)) {
                    Text(if(trip.statusLabel==null) "Review offer" else "View details",color=Color(0xFFA9442F),fontWeight=FontWeight.Bold,fontSize=11.sp)
                }
            }
            Text(if(trip.statusLabel=="Your ride offer") "Manage your offer and start your trip from its details." else "No seat reserved until a driver offer is confirmed.",color=TripsMuted,fontSize=10.sp,lineHeight=14.sp)
        }
    }
}

@Composable
private fun HoldSosControl(id:String,activate:()->Unit) {
    val scope=rememberCoroutineScope()
    val action by rememberUpdatedState(activate)
    var holding by remember {mutableStateOf(false)}
    Box(Modifier.fillMaxWidth().heightIn(min=44.dp).background(
        Brush.horizontalGradient(listOf(Color(0xFF934A3D),Color(0xFF2B1713))),CircleShape)
        .semantics {
            contentDescription="Hold SOS for 3 seconds"
            customActions=listOf(CustomAccessibilityAction("Open SOS controls"){action();true})
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
            Text(if(holding) "Keep holding…" else "Hold SOS for 3 seconds",fontSize=14.sp,fontWeight=FontWeight.Bold,color=Color.White)
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
internal fun money(value:Double)=java.text.DecimalFormat("0.##").format(value)

internal fun demoTripItems()=listOf(
    MyTripItem("demo-active",MyTripStatus.Active,"Kolej Kediaman 10","FSKTM","Started 8:05 AM · Driver: Dorsett Tan",
        vehicle="Toyota Vios · WXY 1234",seats=1,detailRoute="active_trip/demo-active",chatRoute="chat/booking/demo-active",sosRoute="sos/demo-active"),
    MyTripItem("demo-confirmed",MyTripStatus.Confirmed,"Kolej Kediaman 10","Faculty of Engineering","Wed 8:15 AM","UMP–2841",
        seats=2,costPerSeat=4.0,detailRoute=BookingRoute.Ticket(DemoRideIds.CONFIRMED_BOOKING).encode(),
        chatRoute="chat/booking/"+DemoRideIds.CONFIRMED_BOOKING),
    MyTripItem("demo-pending",MyTripStatus.Pending,"Kolej Kediaman 10","FSKTM","Thu 8:00–8:30 AM",
        summary="3 passengers · No driver assigned yet",detailRoute=BookingRoute.OfferDetails(DemoRideIds.HOME_OFFER).encode()),
)

@Preview(name="My Trips",widthDp=390,heightDp=844)
@Preview(name="My Trips narrow",widthDp=320,heightDp=720,fontScale=1.3f)
@Preview(name="My Trips wide",widthDp=600,heightDp=900)
@Composable
private fun MyTripsPreview() { UMPoolingTheme {MyTripsScreen(demoTripItems(),{})} }
