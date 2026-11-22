package com.fopgang.umpooling.booking

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import com.fopgang.umpooling.R
import com.fopgang.umpooling.ui.components.*
import com.fopgang.umpooling.ui.theme.UMPoolingTheme
import java.util.Calendar
import java.util.Locale
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun FindRideScreen(
    back: () -> Unit,
    onModeChange: () -> Unit,
    onFind: (FindRideCriteria) -> Unit,
    screenState: RideFormScreenState = RideFormScreenState(),
    initialCriteria: FindRideCriteria = FindRideCriteria(),
    requestText: String = "",
) {
    var criteria by rememberSaveable(stateSaver = FindRideCriteriaSaver) { mutableStateOf(initialCriteria) }
    var attempted by rememberSaveable { mutableStateOf(false) }
    val issues = if (attempted) criteria.validate(screenState.policy.copy(maxSeats=minOf(3,screenState.policy.maxSeats))) else emptyList()
    val enabled = !screenState.isLoading && screenState.submission != RideSubmissionState.Success
    val seats = criteria.seats
    val passengerLimit = minOf(3, screenState.policy.maxSeats)
    val submit: () -> Unit = {
        if (enabled) {
            attempted = true
            if (criteria.validate(screenState.policy.copy(maxSeats=minOf(3,screenState.policy.maxSeats))).isEmpty()) onFind(criteria)
        }
    }
    val context = LocalContext.current
    RidePage {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            RideHeader("Find a ride", back)
            if(requestText.isNotBlank()) OutlinedTextField(requestText, {}, readOnly=true, label={Text("Voice request · Review and fill in fields")},modifier=Modifier.fillMaxWidth())
            Surface(shape = CircleShape, color = Color.White.copy(alpha = .65f), border = BorderStroke(1.dp, Color.White), shadowElevation = 0.dp) {
                Row(Modifier.fillMaxWidth().padding(5.dp)) {
                    listOf("I need a ride", "I'm offering a ride").forEachIndexed { index, title ->
                        val selected = index == 0
                        Surface(onClick = { if (!selected && enabled) onModeChange() }, enabled = enabled, modifier = Modifier.weight(1f),
                            shape = CircleShape, color = if (selected) RideInk else Color.Transparent,
                            shadowElevation = if (selected) 8.dp else 0.dp) {
                            Box(Modifier.heightIn(min = 38.dp).background(if (selected) Brush.verticalGradient(listOf(Color(0xFF5A514C), RideInk)) else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))).padding(5.dp), contentAlignment = Alignment.Center) {
                                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (selected) Color.White else RideInk)
                            }
                        }
                    }
                }
            }
            Glass {
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Column(Modifier.padding(top = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(12.dp).background(RideInk, CircleShape))
                        Canvas(Modifier.size(2.dp, 64.dp)) { drawLine(Color(0xFFF0CBBB), Offset.Zero, Offset(0f,size.height), 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(),4.dp.toPx()))) }
                        Box(Modifier.size(14.dp).background(RideAccent, RoundedCornerShape(4.dp)))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        LocationEntry("Origin", criteria.origin.name, enabled) { criteria = criteria.copy(origin = criteria.origin.withName(it)) }
                        LocationEntry("Destination", criteria.destination.name, enabled) { criteria = criteria.copy(destination = criteria.destination.withName(it)) }
                    }
                }
            }
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormChoice("Date", criteria.departureDate.rideLabel(), Icons.Default.DateRange, Modifier.weight(1f), enabled) {
                    DatePickerDialog(context, { _, y, m, d ->
                        criteria = criteria.copy(departureDate = LocalDate.of(y,m+1,d))
                    }, criteria.departureDate.year, criteria.departureDate.monthValue-1, criteria.departureDate.dayOfMonth).show()
                }
                FormChoice("Departure", criteria.departureTime.rideLabel(), Icons.Default.DateRange, Modifier.weight(1f), enabled) {
                    TimePickerDialog(context, { _, h, m -> criteria = criteria.copy(departureTime = LocalTime.of(h,m)) },criteria.departureTime.hour,criteria.departureTime.minute,false).show()
                }
            }
            Glass {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Seats", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    FilledIconButton({ criteria = criteria.copy(seats = (seats - 1).coerceAtLeast(1)) }, Modifier.size(44.dp), enabled = enabled && seats > 1, colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White, contentColor = RideInk)) { Text("−", fontSize = 26.sp, modifier = Modifier.semantics { contentDescription = "Fewer seats" }) }
                    Text("$seats", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    FilledIconButton({ criteria = criteria.copy(seats = (seats + 1).coerceAtMost(passengerLimit)) }, Modifier.size(46.dp), enabled = enabled && seats < passengerLimit, colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Black)) { Icon(Icons.Default.Add,"More seats") }
                }
            }
            Spacer(Modifier.height(40.dp))
            RideFormFeedback(issues, screenState.submission)
            RideAction("Confirm & Find Rides  →", enabled = enabled, action = submit)
        }
    }
}
@Composable
private fun LocationEntry(label: String, value: String, enabled: Boolean, change: (String) -> Unit) {
    Column {
    Text(label, Modifier.padding(start = 4.dp, bottom = 4.dp), color = RideMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    OutlinedTextField(value, change, Modifier.fillMaxWidth(), singleLine = true, enabled = enabled, shape = CircleShape,
        textStyle = TextStyle(fontFamily = RideFont, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
        colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White.copy(alpha = .65f), focusedContainerColor = Color.White, unfocusedBorderColor = Color(0xFFF2CDBF), focusedBorderColor = RideAccent))
    }
}
@Composable
private fun FormChoice(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, enabled: Boolean, click: () -> Unit) {
    Surface(onClick = click, enabled = enabled, modifier = modifier.fillMaxHeight(), shape = RoundedCornerShape(28.dp), color = Color.White.copy(alpha = .65f), border = BorderStroke(1.dp,Color.White), shadowElevation = 0.dp) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(40.dp).background(Color(0xFFFDE0D2),RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { if (label == "Departure") Canvas(Modifier.size(20.dp)) {
                drawCircle(RideInk, style = Stroke(1.5.dp.toPx()))
                drawLine(RideInk, Offset(size.width/2,size.height/2), Offset(size.width/2,size.height*.2f),1.5.dp.toPx())
                drawLine(RideInk, Offset(size.width/2,size.height/2), Offset(size.width*.7f,size.height*.65f),1.5.dp.toPx())
            } else Icon(icon,null,Modifier.size(20.dp)) }
            Column(Modifier.weight(1f)) { Text(label,color = RideMuted,fontSize = 12.sp,fontWeight = FontWeight.Bold); Text(value,fontSize = 16.sp,fontWeight = FontWeight.ExtraBold) }
        }
    }
}
@Preview(name = "Find phone",widthDp = 390,heightDp = 844)
@Preview(name = "Find landscape",widthDp = 840,heightDp = 400)
@Composable
fun FindRidePreview() { UMPoolingTheme { FindRideScreen({},{},{}) } }

@Preview(name="Submission error",widthDp=390,heightDp=1000)
@Composable
private fun FindRideErrorPreview() {
    UMPoolingTheme { FindRideScreen({}, {}, {}, screenState = RideFormScreenState(RideSubmissionState.Error("Unable to find rides. Try again."))) }
}
