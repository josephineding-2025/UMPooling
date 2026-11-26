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
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import com.fopgang.umpooling.SupabaseClientProvider
import com.fopgang.umpooling.data.CampusLocation
import com.fopgang.umpooling.data.CampusLocationProvider
import com.fopgang.umpooling.data.GeoPoint
import com.fopgang.umpooling.data.NewRideRequest
import com.fopgang.umpooling.data.repository.RideRepository
import com.fopgang.umpooling.ui.components.*
import com.fopgang.umpooling.ui.matching.LocationSelectorField
import com.fopgang.umpooling.ui.theme.UMPoolingTheme
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

@Composable
fun FindRideScreen(
    back: () -> Unit = {},
    onModeChange: () -> Unit = {},
    onFind: (FindRideCriteria) -> Unit = {},
    screenState: RideFormScreenState = RideFormScreenState(),
    initialCriteria: FindRideCriteria = FindRideCriteria(),
    requestText: String = "",
    repository: RideRepository = remember { RideRepository() },
    onRequestCreated: (() -> Unit)? = null,
    onAvailableRides: (() -> Unit)? = null,
    onOfferRide: (() -> Unit)? = null,
) {
    var criteria by rememberSaveable(stateSaver = FindRideCriteriaSaver) { mutableStateOf(initialCriteria) }
    var attempted by rememberSaveable { mutableStateOf(false) }

    var pickupLocation by remember {
        mutableStateOf<CampusLocation?>(
            CampusLocationProvider.allLocations.firstOrNull {
                it.name.equals(criteria.origin.name, ignoreCase = true) || it.id.equals(criteria.origin.name, ignoreCase = true)
            }
        )
    }
    var destinationLocation by remember {
        mutableStateOf<CampusLocation?>(
            CampusLocationProvider.allLocations.firstOrNull {
                it.name.equals(criteria.destination.name, ignoreCase = true) || it.id.equals(criteria.destination.name, ignoreCase = true)
            }
        )
    }

    val coroutineScope = rememberCoroutineScope()
    var isPosting by remember { mutableStateOf(false) }
    var submissionState by remember { mutableStateOf(screenState.submission) }

    val issues = if (attempted) criteria.validate(screenState.policy.copy(maxSeats = minOf(3, screenState.policy.maxSeats))) else emptyList()
    val enabled = !screenState.isLoading && !isPosting && submissionState != RideSubmissionState.Success
    val seats = criteria.seats
    val passengerLimit = minOf(3, screenState.policy.maxSeats)

    val submit: () -> Unit = {
        if (enabled) {
            attempted = true
            val validationIssues = criteria.validate(screenState.policy.copy(maxSeats = minOf(3, screenState.policy.maxSeats)))
            if (validationIssues.isEmpty()) {
                val user = SupabaseClientProvider.client.auth.currentUserOrNull()
                val pick = pickupLocation ?: CampusLocationProvider.allLocations.firstOrNull { it.name.equals(criteria.origin.name, ignoreCase = true) }
                val dest = destinationLocation ?: CampusLocationProvider.allLocations.firstOrNull { it.name.equals(criteria.destination.name, ignoreCase = true) }

                val origLat = criteria.origin.latitude
                val origLng = criteria.origin.longitude
                val destLat = criteria.destination.latitude
                val destLng = criteria.destination.longitude

                val pickupGeo = pick?.pickupEwkt ?: pick?.ewkt ?: (
                    if (origLat != null && origLng != null)
                        GeoPoint.ewkt(origLng, origLat)
                    else null
                )
                val destGeo = dest?.ewkt ?: (
                    if (destLat != null && destLng != null)
                        GeoPoint.ewkt(destLng, destLat)
                    else null
                )

                if (user != null && pickupGeo != null) {
                    isPosting = true
                    submissionState = RideSubmissionState.Loading
                    coroutineScope.launch {
                        val isoTimestamp = String.format(
                            Locale.US,
                            "%04d-%02d-%02dT%02d:%02d:00+08:00",
                            criteria.departureDate.year,
                            criteria.departureDate.monthValue,
                            criteria.departureDate.dayOfMonth,
                            criteria.departureTime.hour,
                            criteria.departureTime.minute
                        )
                        val request = NewRideRequest(
                            userId = user.id,
                            pickupGeo = pickupGeo,
                            destinationGeo = destGeo,
                            preferredTime = isoTimestamp,
                            requestedSeats = criteria.seats
                        )
                        repository.createRideRequest(request).fold(
                            onSuccess = {
                                isPosting = false
                                submissionState = RideSubmissionState.Success
                                onFind(criteria)
                                onRequestCreated?.invoke()
                            },
                            onFailure = { err ->
                                isPosting = false
                                submissionState = RideSubmissionState.Error(err.message ?: "Failed to post ride request")
                            }
                        )
                    }
                } else {
                    onFind(criteria)
                    onRequestCreated?.invoke()
                }
            }
        }
    }
    val context = LocalContext.current
    RidePage {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    onClick = back,
                    shape = CircleShape,
                    color = Color.White.copy(alpha = .85f),
                    border = BorderStroke(1.dp, Color(0xFFF5D7CA))
                ) {
                    Box(
                        modifier = Modifier.size(44.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        FeatureIcon(FeatureSymbol.Back, description = "Back")
                    }
                }
                Text(
                    "Need a ride",
                    modifier = Modifier.weight(1f),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = RideInk,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(Modifier.size(44.dp))
            }
            if (requestText.isNotBlank()) OutlinedTextField(requestText, {}, readOnly = true, label = { Text("Voice request · Review and fill in fields") }, modifier = Modifier.fillMaxWidth())
            Surface(shape = CircleShape, color = Color.White.copy(alpha = .65f), border = BorderStroke(1.dp, Color.White), shadowElevation = 0.dp) {
                Row(Modifier.fillMaxWidth().padding(5.dp)) {
                    listOf("Available Rides", "Need a ride", "Offer a ride").forEachIndexed { index, title ->
                        val selected = index == 1
                        Surface(
                            onClick = {
                                if (!selected && enabled) {
                                    when (index) {
                                        0 -> onAvailableRides?.invoke() ?: onModeChange()
                                        2 -> onOfferRide?.invoke() ?: onModeChange()
                                    }
                                }
                            },
                            enabled = enabled,
                            modifier = Modifier.weight(1f),
                            shape = CircleShape,
                            color = if (selected) RideInk else Color.Transparent,
                            shadowElevation = if (selected) 6.dp else 0.dp
                        ) {
                            Box(
                                Modifier.heightIn(min = 38.dp)
                                    .background(if (selected) Brush.verticalGradient(listOf(Color(0xFF5A514C), RideInk)) else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent)))
                                    .padding(vertical = 5.dp, horizontal = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.White else RideInk,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
            Glass {
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Column(Modifier.padding(top = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(12.dp).background(RideInk, CircleShape))
                        Canvas(Modifier.size(2.dp, 64.dp)) { drawLine(Color(0xFFF0CBBB), Offset.Zero, Offset(0f, size.height), 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))) }
                        Box(Modifier.size(14.dp).background(RideAccent, RoundedCornerShape(4.dp)))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        LocationSelectorField(
                            label = "Origin",
                            selectedLocation = pickupLocation,
                            onLocationSelected = { loc ->
                                pickupLocation = loc
                                criteria = criteria.copy(origin = RideLocation(loc.name, loc.lat, loc.lng))
                            }
                        )
                        LocationSelectorField(
                            label = "Destination",
                            selectedLocation = destinationLocation,
                            onLocationSelected = { loc ->
                                destinationLocation = loc
                                criteria = criteria.copy(destination = RideLocation(loc.name, loc.lat, loc.lng))
                            }
                        )
                    }
                }
            }
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormChoice("Date", criteria.departureDate.rideLabel(), Icons.Default.DateRange, Modifier.weight(1f), enabled) {
                    DatePickerDialog(
                        context,
                        { _, y, m, d ->
                            criteria = criteria.copy(departureDate = LocalDate.of(y, m + 1, d))
                        },
                        criteria.departureDate.year,
                        criteria.departureDate.monthValue - 1,
                        criteria.departureDate.dayOfMonth
                    ).show()
                }
                FormChoice("Departure", criteria.departureTime.rideLabel(), Icons.Default.DateRange, Modifier.weight(1f), enabled) {
                    TimePickerDialog(
                        context,
                        { _, h, m -> criteria = criteria.copy(departureTime = LocalTime.of(h, m)) },
                        criteria.departureTime.hour,
                        criteria.departureTime.minute,
                        false
                    ).show()
                }
            }
            Glass {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Seats", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    FilledIconButton(
                        { criteria = criteria.copy(seats = (seats - 1).coerceAtLeast(1)) },
                        Modifier.size(44.dp),
                        enabled = enabled && seats > 1,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White, contentColor = RideInk)
                    ) {
                        Text("−", fontSize = 26.sp, modifier = Modifier.semantics { contentDescription = "Fewer seats" })
                    }
                    Text("$seats", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    FilledIconButton(
                        { criteria = criteria.copy(seats = (seats + 1).coerceAtMost(passengerLimit)) },
                        Modifier.size(46.dp),
                        enabled = enabled && seats < passengerLimit,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Black)
                    ) {
                        Icon(Icons.Default.Add, "More seats")
                    }
                }
            }
            Spacer(Modifier.height(40.dp))
            RideFormFeedback(issues, submissionState)
            RideAction(
                if (isPosting) "Posting Request..." else "Confirm & Find Rides  →",
                enabled = enabled,
                action = submit
            )
        }
    }
}

@Composable
private fun FormChoice(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, enabled: Boolean, click: () -> Unit) {
    Surface(onClick = click, enabled = enabled, modifier = modifier.fillMaxHeight(), shape = RoundedCornerShape(28.dp), color = Color.White.copy(alpha = .65f), border = BorderStroke(1.dp, Color.White), shadowElevation = 0.dp) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(40.dp).background(Color(0xFFFDE0D2), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                if (label == "Departure") Canvas(Modifier.size(20.dp)) {
                    drawCircle(RideInk, style = Stroke(1.5.dp.toPx()))
                    drawLine(RideInk, Offset(size.width / 2, size.height / 2), Offset(size.width / 2, size.height * .2f), 1.5.dp.toPx())
                    drawLine(RideInk, Offset(size.width / 2, size.height / 2), Offset(size.width * .7f, size.height * .65f), 1.5.dp.toPx())
                } else Icon(icon, null, Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(label, color = RideMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Preview(name = "Find phone", widthDp = 390, heightDp = 844)
@Preview(name = "Find landscape", widthDp = 840, heightDp = 400)
@Composable
fun FindRidePreview() { UMPoolingTheme { FindRideScreen({}, {}, {}) } }

@Preview(name = "Submission error", widthDp = 390, heightDp = 1000)
@Composable
private fun FindRideErrorPreview() {
    UMPoolingTheme { FindRideScreen({}, {}, {}, screenState = RideFormScreenState(RideSubmissionState.Error("Unable to find rides. Try again."))) }
}
