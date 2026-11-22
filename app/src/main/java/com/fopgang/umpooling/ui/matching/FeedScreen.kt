package com.fopgang.umpooling.ui.matching

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fopgang.umpooling.data.CampusLocationProvider
import com.fopgang.umpooling.data.TripOffer
import com.fopgang.umpooling.data.repository.RideRepository
import kotlinx.coroutines.launch

/**
 * Basic feed listing open carpool trip offers (Sprint 1 core deliverable).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    repository: RideRepository = remember { RideRepository() },
    onOfferClicked: ((TripOffer) -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var offers by remember { mutableStateOf<List<TripOffer>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun loadFeed() {
        coroutineScope.launch {
            isLoading = true
            repository.getOpenTripOffers().fold(
                onSuccess = { list ->
                    offers = list
                    isLoading = false
                },
                onFailure = { err ->
                    isLoading = false
                    snackbarHostState.showSnackbar("Unable to load offers: ${err.message}")
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        loadFeed()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Carpool Feed") },
                actions = {
                    IconButton(onClick = { loadFeed() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh feed")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (offers.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No open ride offers right now",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Be the first to offer a ride or check back closer to peak commuting hours!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(offers, key = { it.tripOfferId ?: it.hashCode().toString() }) { offer ->
                        val origName = offer.originZone?.let { CampusLocationProvider.findById(it)?.name }
                            ?: offer.originZone ?: "Pickup Location"
                        val destName = offer.destinationZone?.let { CampusLocationProvider.findById(it)?.name }
                            ?: offer.destinationZone ?: "Destination"

                        Card(
                            onClick = { onOfferClicked?.invoke(offer) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = origName,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Icon(
                                            Icons.Default.ArrowForward,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .padding(horizontal = 6.dp)
                                                .size(14.dp)
                                        )
                                        Text(
                                            text = destName,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                    }

                                    Text(
                                        text = "RM ${String.format("%.2f", offer.costPerSeat)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.DateRange,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.size(4.dp))
                                        Text(
                                            text = offer.departureTime,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Badge {
                                        Text(text = "${offer.availableSeats} seat(s) left")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
