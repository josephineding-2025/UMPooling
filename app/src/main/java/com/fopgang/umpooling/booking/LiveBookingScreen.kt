package com.fopgang.umpooling.booking

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fopgang.umpooling.R
import com.fopgang.umpooling.SupabaseClientProvider
import com.fopgang.umpooling.data.CampusLocation
import com.fopgang.umpooling.data.CampusLocationProvider
import com.fopgang.umpooling.data.OfferStatus
import com.fopgang.umpooling.data.TripOffer
import com.fopgang.umpooling.data.repository.RideRepository
import com.fopgang.umpooling.ui.matching.LocationSelectorField
import com.fopgang.umpooling.ui.theme.UMPoolingTheme
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
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

    var selectedPickup by mutableStateOf<CampusLocation?>(null)

    fun refresh(offerId: String, owner: String) {
        if (working || bookingId != null) return
        working = true
        issue = 0
        viewModelScope.launch {
            try {
                val client = SupabaseClientProvider.client
                check(client.auth.currentUserOrNull()?.id == owner)
                val loaded = client.postgrest["trip_offers"].select {
                    filter { eq("trip_offer_id", offerId) }
                }.decodeSingle<TripOffer>()
                check(client.auth.currentUserOrNull()?.id == owner)
                offer = loaded
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                offer = null
                issue = R.string.live_offer_load_failed
            } finally {
                working = false
            }
        }
    }

    fun reserve(owner: String, seats: Int, longitude: Double, latitude: Double) {
        val row = offer ?: return
        val id = row.tripOfferId ?: return
        if (working || bookingId != null || issue != 0 || !row.canBook(owner, seats)) return
        working = true
        viewModelScope.launch {
            try {
                val client = SupabaseClientProvider.client
                check(client.auth.currentUserOrNull()?.id == owner)
                val idFromServer = repository.bookSeat(id, seats, longitude, latitude, row.quoteVersion).getOrThrow()
                check(client.auth.currentUserOrNull()?.id == owner)
                bookingId = idFromServer
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                issue = R.string.live_booking_failed
            } finally {
                working = false
            }
        }
    }
}

private fun TripOffer.canBook(owner: String, seats: Int): Boolean =
    tripOfferId != null && driverId != owner && status == OfferStatus.OPEN &&
        seats > 0 && availableSeats >= seats && departureInstant()?.isAfter(Instant.now()) == true

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
    LiveBookingScreenContent(
        offerId = offerId,
        owner = owner,
        criteria = criteria,
        reviewing = reviewing,
        back = back,
        onReview = onReview,
        onBooked = onBooked,
        onMyTrips = onMyTrips,
        offer = model.offer,
        working = model.working,
        issue = model.issue,
        bookingId = model.bookingId,
        selectedPickup = model.selectedPickup,
        onRefresh = { model.refresh(offerId, owner) },
        onSelectPickup = { model.selectedPickup = it },
        onReserve = { seats, longitude, latitude -> model.reserve(owner, seats, longitude, latitude) },
    )
}

@Composable
internal fun LiveBookingScreenContent(
    offerId: String,
    owner: String,
    criteria: FindRideCriteria,
    reviewing: Boolean,
    back: () -> Unit,
    onReview: () -> Unit,
    onBooked: (String) -> Unit,
    onMyTrips: () -> Unit,
    offer: TripOffer?,
    working: Boolean,
    issue: Int,
    bookingId: String?,
    selectedPickup: CampusLocation?,
    onRefresh: () -> Unit,
    onSelectPickup: (CampusLocation?) -> Unit,
    onReserve: (seats: Int, longitude: Double, latitude: Double) -> Unit,
) {
    var seats by rememberSaveable(offerId) { mutableIntStateOf(criteria.seats.coerceAtLeast(1)) }
    val origin = criteria.origin.name.trim()
    val searchPickup = CampusLocationProvider.allLocations.firstOrNull {
        it.pickupLat != null && it.pickupLng != null &&
            (it.id.equals(origin, true) || it.name.equals(origin, true))
    }
    val pickup = selectedPickup ?: searchPickup
        ?: offer?.originZone?.let(CampusLocationProvider::findById)
    val latitude = pickup?.pickupLat
    val longitude = pickup?.pickupLng

    LaunchedEffect(owner, offerId) { if (issue == 0 && offer == null) onRefresh() }
    LaunchedEffect(bookingId) { bookingId?.let(onBooked) }
    BackHandler(enabled = reviewing && working) { }

    RidePage {
        Column(
            Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            RideHeader(
                stringResource(if (reviewing) R.string.live_booking_review else R.string.trip_details),
                { if (!reviewing || !working) back() },
            )
            if (working) LinearProgressIndicator()
            if (issue != 0) {
                Text(stringResource(issue), color = MaterialTheme.colorScheme.error)
                if (issue == R.string.live_booking_failed) {
                    RideAction(stringResource(R.string.my_trips), action = onMyTrips)
                }
                RideAction(stringResource(R.string.live_offer_refresh), enabled = !working, action = onRefresh)
            }
            offer?.let { offer ->
                val eligible = offer.canBook(owner, seats)
                val origin = offer.originZone
                val destination = offer.destinationZone
                Glass {
                    Text(CampusLocationProvider.findById(origin.orEmpty())?.name ?: origin ?: stringResource(R.string.live_pickup))
                    Text(CampusLocationProvider.findById(destination.orEmpty())?.name ?: destination ?: stringResource(R.string.live_destination))
                    offer.departureInstant()?.let {
                        Text(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm").format(it.atZone(CAMPUS_ZONE)))
                    }
                    Text(stringResource(R.string.live_booking_seats, seats, offer.availableSeats))
                    Text(stringResource(R.string.booking_total, offer.costPerSeat, offer.costPerSeat * seats))
                }
                if (!eligible) Text(stringResource(R.string.booking_not_open), color = MaterialTheme.colorScheme.error)
                if (reviewing) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { seats-- }, enabled = !working && seats > 1) { Text("−") }
                        OutlinedButton(onClick = { seats++ }, enabled = !working && seats < offer.availableSeats) { Text("+") }
                    }
                    LocationSelectorField(stringResource(R.string.booking_pickup), pickup, {
                        if (!working) onSelectPickup(it)
                    })
                    if (latitude == null || longitude == null) Text(stringResource(R.string.live_pickup_required))
                    else Text(pickup.pickupName)
                    RideAction(
                        stringResource(R.string.live_booking_confirm),
                        enabled = !working && issue == 0 && bookingId == null &&
                            eligible && latitude != null && longitude != null,
                    ) { if (latitude != null && longitude != null) onReserve(seats, longitude, latitude) }
                } else {
                    RideAction(stringResource(R.string.live_booking_review), enabled = !working && eligible, action = onReview)
                }
            }
        }
    }
}

@Preview(name = "Live Booking Review", widthDp = 390, heightDp = 844)
@Preview(name = "Live Booking Details", widthDp = 390, heightDp = 844)
@Composable
private fun LiveBookingScreenPreview() {
    val sampleOffer = TripOffer(
        tripOfferId = "demo-1",
        driverId = "demo-driver-1",
        costPerSeat = 2.50,
        availableSeats = 3,
        status = OfferStatus.OPEN,
        departureTime = "2099-01-01T08:15:00+08:00",
        originZone = "kk11",
        destinationZone = "engineering",
    )
    UMPoolingTheme {
        LiveBookingScreenContent(
            offerId = "demo-1",
            owner = "demo-user",
            criteria = FindRideCriteria(),
            reviewing = true,
            back = {},
            onReview = {},
            onBooked = {},
            onMyTrips = {},
            offer = sampleOffer,
            working = false,
            issue = 0,
            bookingId = null,
            selectedPickup = null,
            onRefresh = {},
            onSelectPickup = {},
            onReserve = { _, _, _ -> },
        )
    }
}
