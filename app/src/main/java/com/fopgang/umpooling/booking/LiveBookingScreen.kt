package com.fopgang.umpooling.booking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fopgang.umpooling.R
import com.fopgang.umpooling.SupabaseClientProvider
import com.fopgang.umpooling.data.CampusLocation
import com.fopgang.umpooling.data.CampusLocationProvider
import com.fopgang.umpooling.data.OfferStatus
import com.fopgang.umpooling.data.TripOffer
import com.fopgang.umpooling.data.repository.RideRepository
import com.fopgang.umpooling.ui.matching.LocationSelectorField
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

/** Signed-in flow uses persisted offer IDs and only opens tickets returned by the RPC. */
@Composable
fun LiveBookingScreen(
    owner: String, offerId: String, back: () -> Unit,
    reviewing: Boolean, onContinue: (String) -> Unit, onBooked: (String) -> Unit,
    requestedSeats: Int = 1,
) {
    val repository = remember { RideRepository() }
    val scope = rememberCoroutineScope()
    var offer by remember(offerId) { mutableStateOf<TripOffer?>(null) }
    var pickup by remember(offerId) { mutableStateOf<CampusLocation?>(null) }
    var seats by remember(offerId) { mutableIntStateOf(requestedSeats.coerceAtLeast(1)) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var failure by remember { mutableIntStateOf(0) }
    var attempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(owner, offerId, attempt) {
        loading = true
        failure = 0
        try {
            val client = SupabaseClientProvider.client
            check(client.auth.currentUserOrNull()?.id == owner)
            val loaded = client.postgrest["trip_offers"].select {
                filter { eq("trip_offer_id", offerId) }
            }.decodeSingle<TripOffer>()
            check(client.auth.currentUserOrNull()?.id == owner)
            offer = loaded
            if (pickup == null) pickup = loaded.originZone?.let(CampusLocationProvider::findById)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            failure = R.string.booking_load_failed
        } finally { loading = false }
    }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(back) { Text(stringResource(R.string.booking_back)) }
        if (loading || busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (failure != 0) {
            Text(stringResource(failure), color = MaterialTheme.colorScheme.error)
            TextButton({ attempt++ }, enabled = !busy) { Text(stringResource(R.string.booking_retry)) }
        }
        offer?.let { row ->
            Text("${row.originZone?.let(CampusLocationProvider::findById)?.name ?: row.originZone.orEmpty()} → ${row.destinationZone?.let(CampusLocationProvider::findById)?.name ?: row.destinationZone.orEmpty()}")
            Text(row.departureTime)
            Text(stringResource(R.string.booking_total, row.costPerSeat, row.costPerSeat * seats))
            if (reviewing) {
                Row {
                    OutlinedButton({ seats-- }, enabled = !busy && seats > 1) { Text("−") }
                    Text(stringResource(R.string.booking_seats, seats), Modifier.padding(12.dp))
                    OutlinedButton({ seats++ }, enabled = !busy && seats < row.availableSeats) { Text("+") }
                }
                LocationSelectorField(
                    label = stringResource(R.string.booking_pickup), selectedLocation = pickup,
                    onLocationSelected = { if (!busy) pickup = it }
                )
            }
            val point = pickup
            val eligible = row.driverId != owner && row.status == OfferStatus.OPEN &&
                row.availableSeats >= seats && seats > 0
            if (!eligible) Text(stringResource(R.string.booking_not_open))
            Button(onClick = {
                if (!reviewing) onContinue(offerId)
                else {
                    busy = true
                    failure = 0
                    scope.launch {
                        try {
                            check(SupabaseClientProvider.client.auth.currentUserOrNull()?.id == owner)
                            val id = repository.bookSeat(offerId, seats, point!!.pickupLng!!, point.pickupLat!!, row.quoteVersion).getOrThrow()
                            check(SupabaseClientProvider.client.auth.currentUserOrNull()?.id == owner)
                            onBooked(id)
                        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                        catch (_: Exception) { failure = R.string.booking_submit_failed }
                        finally { busy = false }
                    }
                }
            }, enabled = !busy && !loading && eligible &&
                (!reviewing || (point?.pickupLng != null && point.pickupLat != null))) {
                Text(stringResource(if (reviewing) R.string.booking_reserve else R.string.booking_continue))
            }
        }
    }
}
