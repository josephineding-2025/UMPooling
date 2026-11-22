package com.fopgang.umpooling.booking

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.tooling.preview.Preview
import com.fopgang.umpooling.R
import com.fopgang.umpooling.ui.components.*
import com.fopgang.umpooling.ui.theme.UMPoolingTheme
import java.util.*
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToInt

private val OfferInk = Color(0xFF292422)
private val OfferMuted = Color(0xFF75675F)
private val OfferPeach = Color(0xFFF5D2C3)
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private val OfferFont = FontFamily(*listOf(FontWeight.Normal,FontWeight.Medium,FontWeight.SemiBold,FontWeight.Bold,FontWeight.ExtraBold).map {
    Font(R.font.plus_jakarta_sans,weight = it,variationSettings = FontVariation.Settings(FontVariation.weight(it.weight)))
}.toTypedArray())

/** Content-sized cards and a bounded reading width keep the reference layout usable on any window. */
@Composable
fun OfferRideScreen(
    back: () -> Unit,
    onModeChange: () -> Unit,
    onPost: (OfferRideDraft) -> Unit,
    screenState: RideFormScreenState = RideFormScreenState(),
    initialDraft: OfferRideDraft = OfferRideDraft(),
) {
    var draft by rememberSaveable(stateSaver = OfferRideDraftSaver) { mutableStateOf(initialDraft) }
    var attempted by rememberSaveable { mutableStateOf(false) }
    val issues = if (attempted) draft.validate(screenState.policy) else emptyList()
    val enabled = !screenState.isLoading && screenState.submission != RideSubmissionState.Success
    val submit: () -> Unit = {
        if (enabled) {
            attempted = true
            if (draft.validate(screenState.policy).isEmpty()) onPost(draft)
        }
    }
    val seats = draft.seats
    val amount = draft.contributionSen / 100f
    val context = LocalContext.current
    ProvideTextStyle(TextStyle(fontFamily = OfferFont,color = OfferInk,fontSize = 14.sp,lineHeight = 19.sp)) {
        Box(Modifier.fillMaxSize().background(Color(0xFFFCEAE2)).drawWithCache {
            val light = Brush.radialGradient(listOf(Color(0xFFFFFDFC),Color.Transparent),Offset.Zero,size.width*1.1f)
            val peach = Brush.radialGradient(listOf(Color(0xFFFAD0C3),Color.Transparent),Offset(size.width,size.height*.2f),size.width*.95f)
            val lower = Brush.radialGradient(listOf(Color(0xFFF9D3C4),Color.Transparent),Offset.Zero.copy(y=size.height),size.width*.8f)
            onDrawBehind { drawRect(light);drawRect(peach);drawRect(lower) }
        },contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max=600.dp).fillMaxWidth().verticalScroll(rememberScrollState()).safeDrawingPadding().imePadding()
                .padding(horizontal=24.dp).padding(top=52.dp,bottom=29.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.offerSurface(CircleShape).size(47.dp),contentAlignment=Alignment.Center) {
                        IconButton(back) { FeatureIcon(FeatureSymbol.Back,Modifier.size(20.dp),description="Back") }
                    }
                    Text("Post a ride",fontSize=23.sp,lineHeight=29.sp,fontWeight=FontWeight.ExtraBold)
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth().offerSurface(CircleShape).padding(5.dp),verticalAlignment=Alignment.CenterVertically) {
                    Box(Modifier.weight(1f).heightIn(min=38.dp).clickable(enabled=enabled,onClick=onModeChange),contentAlignment=Alignment.Center) {
                        Text("I need a ride",fontSize=14.sp,fontWeight=FontWeight.Bold)
                    }
                    Box(Modifier.weight(1f).shadow(10.dp,CircleShape,ambientColor=Color(0xFF785F52),spotColor=Color(0xFF785F52))
                        .background(Brush.verticalGradient(listOf(Color(0xFF5A514D),Color(0xFF282321))),CircleShape)
                        .border(1.dp,Color.White.copy(alpha=.4f),CircleShape).heightIn(min=38.dp).padding(5.dp),contentAlignment=Alignment.Center) {
                        Text("I'm offering a ride",textAlign=androidx.compose.ui.text.style.TextAlign.Center,fontSize=14.sp,fontWeight=FontWeight.Bold,color=Color.White)
                    }
                }
                Spacer(Modifier.height(13.dp))
                Box(Modifier.fillMaxWidth().offerSurface().padding(horizontal=17.dp,vertical=11.dp)) {
                    Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                        Canvas(Modifier.padding(top=21.dp).width(19.dp).height(90.dp)) {
                            drawCircle(OfferInk,6.dp.toPx(),Offset(size.width/2,6.dp.toPx()))
                            drawLine(OfferPeach,Offset(size.width/2,16.dp.toPx()),Offset(size.width/2,69.dp.toPx()),2.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(),4.dp.toPx())))
                            drawRoundRect(Color(0xFFF48B70),Offset(size.width/2-7.dp.toPx(),76.dp.toPx()),Size(14.dp.toPx(),14.dp.toPx()),CornerRadius(4.dp.toPx()))
                        }
                        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(11.dp)) {
                            OfferLocation("Origin",draft.origin.name,enabled) { draft=draft.copy(origin=draft.origin.withName(it)) }
                            OfferLocation("Destination",draft.destination.name,enabled) { draft=draft.copy(destination=draft.destination.withName(it)) }
                        }
                    }
                }
                Spacer(Modifier.height(13.dp))
                BoxWithConstraints {
                val compact = maxWidth < 320.dp
                Row(Modifier.height(IntrinsicSize.Min),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    OfferChoice("Date",draft.departureDate.rideLabel(multiline=true),Modifier.weight(1f),compact,enabled) {
                        DatePickerDialog(context,{_,y,m,d->
                            draft=draft.copy(departureDate=LocalDate.of(y,m+1,d))
                        },draft.departureDate.year,draft.departureDate.monthValue-1,draft.departureDate.dayOfMonth).show()
                    }
                    OfferChoice("Departure",draft.departureTime.rideLabel(),Modifier.weight(1f),compact,enabled) {
                        TimePickerDialog(context,{_,h,m->draft=draft.copy(departureTime=LocalTime.of(h,m))},draft.departureTime.hour,draft.departureTime.minute,false).show()
                    }
                }
                }
                Spacer(Modifier.height(13.dp))
                Row(Modifier.fillMaxWidth().offerSurface().heightIn(min=85.dp).padding(horizontal=20.dp,vertical=18.dp),
                    verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Available seats",fontSize=16.sp,lineHeight=20.sp,fontWeight=FontWeight.ExtraBold)
                        Row(Modifier.widthIn(max=106.dp).fillMaxWidth().padding(top=8.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                            repeat(screenState.policy.maxSeats) { Box(Modifier.weight(1f).height(8.dp).background(if(it<seats)OfferInk else Color(0xFFFCE1D5),CircleShape)) }
                        }
                    }
                    Box(Modifier.size(44.dp).offerSurface(CircleShape),contentAlignment=Alignment.Center) {
                        IconButton({draft=draft.copy(seats=(seats-1).coerceAtLeast(1))},enabled=enabled && seats>1) { Text("−",fontSize=26.sp) }
                    }
                    Text("$seats",fontSize=28.sp,lineHeight=34.sp,fontWeight=FontWeight.ExtraBold)
                    FilledIconButton({draft=draft.copy(seats=(seats+1).coerceAtMost(screenState.policy.maxSeats))},Modifier.size(46.dp),enabled=enabled && seats<screenState.policy.maxSeats,colors=IconButtonDefaults.filledIconButtonColors(containerColor=Color.Black)) {
                        Text("+",fontSize=30.sp,lineHeight=34.sp,color=Color.White)
                    }
                }
                Spacer(Modifier.height(13.dp))
                Column(Modifier.fillMaxWidth().offerSurface().padding(horizontal=19.dp,vertical=17.dp)) {
                    Text("Suggested contribution",fontSize=13.sp,lineHeight=18.sp,fontWeight=FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        Text(String.format(Locale.ENGLISH,"RM %.2f",amount),fontSize=30.sp,lineHeight=39.sp,fontWeight=FontWeight.ExtraBold)
                        Text(" per seat",Modifier.weight(1f),fontSize=14.sp,fontWeight=FontWeight.SemiBold,color=OfferMuted)
                        Box(Modifier.background(Color.White,CircleShape).padding(horizontal=12.dp,vertical=5.dp)) { Text("12.4 km",fontSize=12.sp,fontWeight=FontWeight.Bold) }
                    }
                    Text("A fair share of fuel and toll, not a fare. You can adjust it.",fontSize=13.sp,lineHeight=17.sp,color=Color(0xFF584B45))
                    Spacer(Modifier.height(10.dp))
                    GradientSlider(
                        value = amount,
                        onValueChange = { draft=draft.copy(contributionSen=(it*100).roundToInt()) },
                        valueRange = (if (screenState.policy.maxContributionSen > 200) 2f else 0f)..maxOf(.01f,screenState.policy.maxContributionSen/100f),
                        enabled = enabled && screenState.policy.maxContributionSen > 0,
                        modifier = Modifier.fillMaxWidth().height(32.dp),
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                        listOf(if (screenState.policy.maxContributionSen > 200) "RM 2" else "RM 0",
                            String.format(Locale.getDefault(),"RM %.2f suggested",minOf(400,screenState.policy.maxContributionSen)/100f),
                            String.format(Locale.getDefault(),"RM %.2f",screenState.policy.maxContributionSen/100f)).forEach { Text(it,fontSize=11.sp,fontWeight=FontWeight.Bold,color=Color(0xFF594B44)) }
                    }
                }
                Spacer(Modifier.height(21.dp))
                RideFormFeedback(issues, screenState.submission)
                Button(submit,Modifier.fillMaxWidth().heightIn(min=56.dp).shadow(16.dp,CircleShape,ambientColor=Color(0xFF796356),spotColor=Color(0xFF796356)),
                    enabled=enabled,shape=CircleShape,colors=ButtonDefaults.buttonColors(containerColor=Color.Black)) {
                    Text("Post ride",fontFamily=OfferFont,fontSize=16.sp,fontWeight=FontWeight.Bold,color=Color.White)
                    Spacer(Modifier.width(12.dp))
                    Canvas(Modifier.size(17.dp)) {
                        val path=Path().apply { moveTo(2.dp.toPx(),size.height/2);lineTo(size.width-2.dp.toPx(),size.height/2);moveTo(size.width*.55f,size.height*.2f);lineTo(size.width*.85f,size.height/2);lineTo(size.width*.55f,size.height*.8f) }
                        drawPath(path,Color.White,style=Stroke(1.5.dp.toPx(),cap=StrokeCap.Round))
                    }
                }
            }
        }
    }
}

private fun Modifier.offerSurface(shape: Shape=RoundedCornerShape(30.dp)): Modifier =
    shadow(14.dp,shape,clip=false,ambientColor=Color(0xFFAE9180).copy(alpha=.7f),spotColor=Color(0xFFAE9180).copy(alpha=.7f))
        .background(Brush.linearGradient(listOf(Color(0xFFFFFCFA).copy(alpha=.88f),Color(0xFFFDE7DE).copy(alpha=.7f))),shape)
        .border(1.dp,Color.White,shape)
        .padding(1.dp).border(.7.dp,Color(0xFFF0CEBF),shape)

@Composable
private fun OfferLocation(label:String,value:String,enabled:Boolean,change:(String)->Unit) {
    Column {
        Text(label,Modifier.padding(start=4.dp,bottom=3.dp),fontSize=13.sp,lineHeight=17.sp,fontWeight=FontWeight.Bold,color=OfferMuted)
        Box(Modifier.fillMaxWidth().offerSurface(CircleShape).heightIn(min=49.dp).padding(horizontal=18.dp,vertical=12.dp),contentAlignment=Alignment.CenterStart) {
            BasicTextField(value,change,Modifier.fillMaxWidth(),singleLine=true,enabled=enabled,textStyle=TextStyle(fontFamily=OfferFont,fontSize=16.sp,lineHeight=21.sp,fontWeight=FontWeight.SemiBold,color=OfferInk))
        }
    }
}
@Composable
private fun OfferChoice(label:String,value:String,modifier:Modifier,compact:Boolean=false,enabled:Boolean=true,click:()->Unit) {
    Row(modifier.fillMaxHeight().offerSurface().clickable(enabled=enabled,onClick=click).padding(horizontal=16.dp,vertical=9.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
        if (!compact) Box(Modifier.size(40.dp).background(Color(0xFFFCE0D2),RoundedCornerShape(14.dp)),contentAlignment=Alignment.Center) {
            if(label=="Date") FeatureIcon(FeatureSymbol.Calendar,Modifier.size(18.dp),tint=OfferInk)
            else Canvas(Modifier.size(17.dp)) {
                drawCircle(OfferInk,style=Stroke(1.5.dp.toPx()))
                drawLine(OfferInk,Offset(size.width/2,size.height/2),Offset(size.width/2,size.height*.2f),1.5.dp.toPx())
                drawLine(OfferInk,Offset(size.width/2,size.height/2),Offset(size.width*.7f,size.height*.65f),1.5.dp.toPx())
            }
        }
        Column(Modifier.weight(1f)) {
            Text(label,fontSize=11.sp,lineHeight=16.sp,fontWeight=FontWeight.Bold,color=OfferMuted)
            Text(value,fontSize=16.sp,lineHeight=19.sp,fontWeight=FontWeight.ExtraBold)
        }
    }
}
@Preview(name="Reference",widthDp=390,heightDp=844)
@Preview(name="Small phone",widthDp=320,heightDp=640,fontScale=1.15f)
@Preview(name="Tablet",widthDp=800,heightDp=1000)
@Composable
fun OfferRidePreview() { UMPoolingTheme { OfferRideScreen({},{},{}) } }

@Preview(name="Submitting",widthDp=390,heightDp=1000)
@Composable
private fun OfferRideLoadingPreview() {
    UMPoolingTheme { OfferRideScreen({}, {}, {}, screenState = RideFormScreenState(RideSubmissionState.Loading)) }
}
