package com.fopgang.umpooling.ui.matching

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import com.fopgang.umpooling.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fopgang.umpooling.SupabaseClientProvider
import com.fopgang.umpooling.data.CampusLocation
import com.fopgang.umpooling.data.GeoPoint
import com.fopgang.umpooling.data.Haversine
import com.fopgang.umpooling.data.NewTripOffer
import com.fopgang.umpooling.data.repository.RideRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Screen for drivers to post a ride offer (FR2.1 / FR3.1).
 * Loads the configurable server cost cap, ISO-8601 (+08:00) timestamptz format,
 * Haversine x 1.3 road distance estimation, and blocks unpinned locations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostOfferScreen(
    repository: RideRepository = remember { RideRepository() },
    onOfferCreated: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    var contributionCap by remember { mutableStateOf<Double?>(null) }
    var capFailed by remember { mutableStateOf(false) }
    var capAttempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(repository, capAttempt) {
        capFailed = false
        repository.maxCostPerSeat().fold(
            onSuccess = { contributionCap = it },
            onFailure = { capFailed = true }
        )
    }
    val snackbarHostState = remember { SnackbarHostState() }

    var originLocation by remember { mutableStateOf<CampusLocation?>(null) }
    var destinationLocation by remember { mutableStateOf<CampusLocation?>(null) }
    
    // Default to today's date in Malaysia timezone
    val defaultDate = remember { LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) }
    var departureDate by remember { mutableStateOf(defaultDate) }
    var departureTime by remember { mutableStateOf("08:15") }
    
    var seats by remember { mutableIntStateOf(3) }
    var costInput by remember { mutableStateOf("3.00") }
    var isPosting by remember { mutableStateOf(false) }

    // Validation checks
    val orig = originLocation
    val dest = destinationLocation
    val hasValidCoordinates = orig?.lat != null && orig.lng != null && dest?.lat != null && dest.lng != null
    val unpinnedLocation = when {
        orig != null && (orig.lat == null || orig.lng == null) -> orig.name
        dest != null && (dest.lat == null || dest.lng == null) -> dest.name
        else -> null
    }

    // Dynamic distance calculation using Haversine x 1.3
    val estimatedDistanceKm = remember(orig, dest) {
        if (hasValidCoordinates && orig?.lat != null && orig.lng != null && dest?.lat != null && dest.lng != null) {
            Haversine.estimateRoadDistanceKm(orig.lat, orig.lng, dest.lat, dest.lng)
        } else null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Offer a Ride (Driver)") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Trip Route Details",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    LocationSelectorField(
                        label = "Starting Point / Pickup Area",
                        selectedLocation = originLocation,
                        onLocationSelected = { originLocation = it }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LocationSelectorField(
                        label = "Destination Campus Zone",
                        selectedLocation = destinationLocation,
                        onLocationSelected = { destinationLocation = it }
                    )

                    if (unpinnedLocation != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "⚠️ '$unpinnedLocation' has no fixed GPS coordinates. Please select a defined campus zone or college.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    if (estimatedDistanceKm != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Estimated road distance: $estimatedDistanceKm km (Haversine × 1.3)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = departureDate,
                            onValueChange = { departureDate = it },
                            label = { Text("Date (YYYY-MM-DD)") },
                            singleLine = true,
                            modifier = Modifier.weight(1.2f)
                        )

                        OutlinedTextField(
                            value = departureTime,
                            onValueChange = { departureTime = it },
                            label = { Text("Time (HH:MM)") },
                            singleLine = true,
                            modifier = Modifier.weight(0.8f)
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Seats & Cost-Share (FR3.1)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Available Seats:")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = { if (seats > 1) seats-- },
                                modifier = Modifier.size(40.dp)
                            ) { Text("-") }
                            Text(
                                text = "$seats",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            OutlinedButton(
                                onClick = { if (seats < 6) seats++ },
                                modifier = Modifier.size(40.dp)
                            ) { Text("+") }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = costInput,
                        onValueChange = { costInput = it },
                        label = { Text("Cost Share Per Seat (RM)") },
                        supportingText = { Text(if (contributionCap == null) stringResource(R.string.booking_cap_loading) else stringResource(R.string.booking_cap_amount, contributionCap!!)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (capFailed) androidx.compose.material3.TextButton(onClick = { capAttempt++ }) { Text(stringResource(R.string.booking_cap_retry)) }
            Button(
                onClick = {
                    val user = runCatching {
                        SupabaseClientProvider.client.auth.currentUserOrNull()
                    }.getOrElse { err ->
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(err.message ?: "Database not configured.")
                        }
                        return@Button
                    }
                    if (user == null) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Please sign in first.")
                        }
                        return@Button
                    }
                    if (orig == null || dest == null) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Please pick both origin and destination.")
                        }
                        return@Button
                    }
                    if (!hasValidCoordinates) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Cannot post: location lacks verified GPS coordinates.")
                        }
                        return@Button
                    }
                    val parsedCost = costInput.toDoubleOrNull()
                    if (parsedCost == null || !parsedCost.isFinite() || parsedCost < 0.0 || parsedCost > (contributionCap ?: -1.0)) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Contribution is invalid or exceeds the configured cap.")
                        }
                        return@Button
                    }

                    // Format ISO-8601 timestamptz with Malaysia timezone +08:00
                    val isoTimestamp = "${departureDate.trim()}T${departureTime.trim()}:00+08:00"

                    isPosting = true
                    coroutineScope.launch {
                        val offer = NewTripOffer(
                            driverId = user.id,
                            originGeo = orig.ewkt!!,
                            destinationGeo = dest.ewkt!!,
                            costPerSeat = parsedCost,
                            availableSeats = seats,
                            departureTime = isoTimestamp,
                            originZone = orig.id,
                            destinationZone = dest.id,
                            distanceKm = estimatedDistanceKm
                        )

                        repository.createTripOffer(offer).fold(
                            onSuccess = {
                                isPosting = false
                                snackbarHostState.showSnackbar("Trip offer published successfully!")
                                onOfferCreated?.invoke()
                            },
                            onFailure = { err ->
                                isPosting = false
                                snackbarHostState.showSnackbar("Failed: ${err.message}")
                            }
                        )
                    }
                },
                enabled = !isPosting && hasValidCoordinates && contributionCap != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isPosting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Publish Trip Offer")
                }
            }
        }
    }
}
