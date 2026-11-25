package com.fopgang.umpooling.booking

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fopgang.umpooling.R
import com.fopgang.umpooling.data.CampusLocationProvider
import com.fopgang.umpooling.data.TripOffer
import com.fopgang.umpooling.data.repository.RideRepository
import com.fopgang.umpooling.ui.matching.LocationSelectorField
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

internal fun TripOffer.departureInstant(): Instant? =
    runCatching { OffsetDateTime.parse(departureTime).toInstant() }.getOrNull()

internal class LiveBookingViewModel : ViewModel() {
    private val repository = RideRepository()
    var offer by mutableStateOf<TripOffer?>(null)
        private set
    var working by mutableStateOf(false)
        private set
    var issue by mutableIntStateOf(0)
        private set
    var bookingId by mutableStateOf<String?>(null)
        private set

    fun refresh(offerId: String, owner: String, seats: Int) {
        if (working || bookingId != null) return
        working = true
        issue = 0
        viewModelScope.launch {
            repository.getOpenTripOffers().fold(
                onSuccess = { offers ->
                    val now = Instant.now()
                    offer = offers.firstOrNull {
                        it.tripOfferId == offerId && it.driverId != owner &&
                            it.availableSeats >= seats && it.departureInstant()?.isAfter(now) == true
                    }
                    if (offer == null) issue = R.string.live_offer_unavailable
                },
                onFailure = {
                    offer = null
                    issue = R.string.live_offer_load_failed
                }
            )
            working = false
        }
    }

    fun reserve(seats: Int, longitude: Double, latitude: Double) {
        val id = offer?.tripOfferId ?: return
        if (working || bookingId != null || issue != 0) return
        working = true
        viewModelScope.launch {
            repository.bookSeat(id, seats, longitude, latitude).fold(
                onSuccess = { bookingId = it },
                onFailure = { issue = R.string.live_booking_failed }
            )
            working = false
        }
    }
}

@Composable
internal fun LiveBookingScreen(
    offerId: String,
    owner: String,
    criteria: FindRideCriteria,
    reviewing: Boolean,
    back: () -> Unit,
    onReview: () -> Unit,
    onBooked: (String) -> Unit,
    onMyTrips: () -> Unit,
    model: LiveBookingViewModel = viewModel(),
) {
    var pickupId by rememberSaveable(offerId) {
        val origin = criteria.origin.name.trim()
        mutableStateOf(CampusLocationProvider.allLocations.firstOrNull {
            it.pickupLat != null && it.pickupLng != null &&
                (it.id.equals(origin, true) || it.name.equals(origin, true))
        }?.id)
    }
    val pickup = pickupId?.let(CampusLocationProvider::findById)
    val latitude = pickup?.pickupLat
    val longitude = pickup?.pickupLng
    val seats = criteria.seats
    val refresh = { model.refresh(offerId, owner, seats) }

    LaunchedEffect(offerId) { if (model.issue == 0 && model.offer == null) refresh() }
    LaunchedEffect(model.bookingId) { model.bookingId?.let(onBooked) }
    BackHandler(enabled = reviewing && model.working) { }

    RidePage {
        Column(
            Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            RideHeader(
                stringResource(if (reviewing) R.string.live_booking_review else R.string.trip_details),
                { if (!reviewing || !model.working) back() },
            )
            if (model.working) LinearProgressIndicator()
            if (model.issue != 0) {
                Text(stringResource(model.issue), color = MaterialTheme.colorScheme.error)
                if (model.issue == R.string.live_booking_failed) {
                    RideAction(stringResource(R.string.my_trips), action = onMyTrips)
                }
                RideAction(stringResource(R.string.live_offer_refresh), enabled = !model.working, action = refresh)
            }
            model.offer?.let { offer ->
                val origin = offer.originZone
                val destination = offer.destinationZone
                Glass {
                    Text(CampusLocationProvider.findById(origin.orEmpty())?.name ?: origin ?: stringResource(R.string.live_pickup))
                    Text(CampusLocationProvider.findById(destination.orEmpty())?.name ?: destination ?: stringResource(R.string.live_destination))
                    offer.departureInstant()?.let {
                        Text(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm").format(it.atZone(CAMPUS_ZONE)))
                    }
                    Text(stringResource(R.string.live_booking_seats, seats, offer.availableSeats))
                    Text(stringResource(R.string.live_booking_total, offer.costPerSeat * seats))
                }
                if (reviewing) {
                    LocationSelectorField(stringResource(R.string.live_pickup), pickup, { pickupId = it.id })
                    if (latitude == null || longitude == null) Text(stringResource(R.string.live_pickup_required))
                    else Text(pickup.pickupName)
                    RideAction(
                        stringResource(R.string.live_booking_confirm),
                        enabled = !model.working && model.issue == 0 && model.bookingId == null &&
                            latitude != null && longitude != null,
                    ) { if (latitude != null && longitude != null) model.reserve(seats, longitude, latitude) }
                } else {
                    RideAction(stringResource(R.string.live_booking_review), enabled = !model.working, action = onReview)
                }
            }
        }
    }
}
