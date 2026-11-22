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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fopgang.umpooling.SupabaseClientProvider
import com.fopgang.umpooling.data.CampusLocation
import com.fopgang.umpooling.data.GeoPoint
import com.fopgang.umpooling.data.NewRideRequest
import com.fopgang.umpooling.data.repository.RideRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Screen for passengers to post a ride request (FR2.1 / FR2.2).
 * Formats preferred_time as ISO-8601 (+08:00) timestamptz and verifies GPS coordinates.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostRequestScreen(
    repository: RideRepository = remember { RideRepository() },
    onRequestCreated: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var pickupLocation by remember { mutableStateOf<CampusLocation?>(null) }
    var destinationLocation by remember { mutableStateOf<CampusLocation?>(null) }

    val defaultDate = remember { LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) }
    var preferredDate by remember { mutableStateOf(defaultDate) }
    var preferredTime by remember { mutableStateOf("08:15") }
    var isPosting by remember { mutableStateOf(false) }

    val pick = pickupLocation
    val dest = destinationLocation
    val hasValidCoordinates = pick?.lat != null && pick.lng != null && dest?.lat != null && dest.lng != null
    val unpinnedLocation = when {
        pick != null && (pick.lat == null || pick.lng == null) -> pick.name
        dest != null && (dest.lat == null || dest.lng == null) -> dest.name
        else -> null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request a Ride (Passenger)") }
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
                        text = "Ride Request Details",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Drivers heading your way will be notified to offer a ride.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LocationSelectorField(
                        label = "Pickup Point (College / Off-Campus Zone)",
                        selectedLocation = pickupLocation,
                        onLocationSelected = { pickupLocation = it }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LocationSelectorField(
                        label = "Destination (Faculty / Campus Zone)",
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

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = preferredDate,
                            onValueChange = { preferredDate = it },
                            label = { Text("Date (YYYY-MM-DD)") },
                            singleLine = true,
                            modifier = Modifier.weight(1.2f)
                        )

                        OutlinedTextField(
                            value = preferredTime,
                            onValueChange = { preferredTime = it },
                            label = { Text("Time (HH:MM)") },
                            singleLine = true,
                            modifier = Modifier.weight(0.8f)
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val user = SupabaseClientProvider.client.auth.currentUserOrNull()
                    if (user == null) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Please sign in first.")
                        }
                        return@Button
                    }
                    if (pick == null || dest == null) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Please select both pickup and destination.")
                        }
                        return@Button
                    }
                    if (!hasValidCoordinates) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Cannot request: location lacks verified GPS coordinates.")
                        }
                        return@Button
                    }

                    val isoTimestamp = "${preferredDate.trim()}T${preferredTime.trim()}:00+08:00"

                    isPosting = true
                    coroutineScope.launch {
                        val request = NewRideRequest(
                            userId = user.id,
                            pickupGeo = pick.pickupEwkt!!,
                            destinationGeo = dest.ewkt!!,
                            preferredTime = isoTimestamp
                        )

                        repository.createRideRequest(request).fold(
                            onSuccess = {
                                isPosting = false
                                snackbarHostState.showSnackbar("Ride request submitted successfully!")
                                onRequestCreated?.invoke()
                            },
                            onFailure = { err ->
                                isPosting = false
                                snackbarHostState.showSnackbar("Failed: ${err.message}")
                            }
                        )
                    }
                },
                enabled = !isPosting && hasValidCoordinates,
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
                    Icon(Icons.Default.Person, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Submit Ride Request")
                }
            }
        }
    }
}
