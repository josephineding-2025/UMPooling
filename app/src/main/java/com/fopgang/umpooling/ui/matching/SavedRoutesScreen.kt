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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.fopgang.umpooling.SupabaseClientProvider
import com.fopgang.umpooling.data.CampusLocation
import com.fopgang.umpooling.data.CampusLocationProvider
import com.fopgang.umpooling.data.GeoPoint
import com.fopgang.umpooling.data.Haversine
import com.fopgang.umpooling.data.NewSavedRoute
import com.fopgang.umpooling.data.SavedRoute
import com.fopgang.umpooling.data.repository.RideRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

/**
 * Screen for managing frequent commuter routes and residential zones (FR2.1).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedRoutesScreen(
    repository: RideRepository = remember { RideRepository() },
    onNavigateBack: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var originLocation by remember { mutableStateOf<CampusLocation?>(null) }
    var destinationLocation by remember { mutableStateOf<CampusLocation?>(null) }
    var preferredTime by remember { mutableStateOf("08:30") }

    var savedRoutes by remember { mutableStateOf<List<SavedRoute>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    fun refreshRoutes() {
        val currentUserId = SupabaseClientProvider.client.auth.currentUserOrNull()?.id ?: return
        coroutineScope.launch {
            isLoading = true
            repository.getSavedRoutes(currentUserId).fold(
                onSuccess = { routes ->
                    savedRoutes = routes
                    isLoading = false
                },
                onFailure = {
                    isLoading = false
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        refreshRoutes()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Frequent Routes (FR2.1)") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Save Daily Route",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Save your frequent commuting path for fast offer posting and automated driver matching.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        LocationSelectorField(
                            label = "Origin (Home / College / Gate)",
                            selectedLocation = originLocation,
                            onLocationSelected = { originLocation = it }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LocationSelectorField(
                            label = "Destination (Faculty / Campus Zone)",
                            selectedLocation = destinationLocation,
                            onLocationSelected = { destinationLocation = it }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = preferredTime,
                            onValueChange = { preferredTime = it },
                            label = { Text("Preferred Time (HH:MM)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val user = SupabaseClientProvider.client.auth.currentUserOrNull()
                                if (user == null) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Please sign in first.")
                                    }
                                    return@Button
                                }
                                val orig = originLocation
                                val dest = destinationLocation
                                if (orig == null || dest == null) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Please choose both origin and destination.")
                                    }
                                    return@Button
                                }
                                if (orig.lat == null || orig.lng == null || dest.lat == null || dest.lng == null) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Cannot save route: location lacks verified GPS coordinates.")
                                    }
                                    return@Button
                                }

                                val distanceKm = Haversine.estimateRoadDistanceKm(orig.lat, orig.lng, dest.lat, dest.lng)

                                isSaving = true
                                coroutineScope.launch {
                                    val newRoute = NewSavedRoute(
                                        userId = user.id,
                                        originGeo = orig.ewkt!!,
                                        destinationGeo = dest.ewkt!!,
                                        originZone = orig.id,
                                        destinationZone = dest.id,
                                        distanceKm = distanceKm,
                                        preferredTime = preferredTime
                                    )
                                    repository.createSavedRoute(newRoute).fold(
                                        onSuccess = {
                                            isSaving = false
                                            snackbarHostState.showSnackbar("Route saved successfully!")
                                            originLocation = null
                                            destinationLocation = null
                                            refreshRoutes()
                                        },
                                        onFailure = { err ->
                                            isSaving = false
                                            snackbarHostState.showSnackbar("Failed to save: ${err.message}")
                                        }
                                    )
                                }
                            },
                            enabled = !isSaving && originLocation != null && destinationLocation != null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.size(8.dp))
                                Text("Save Frequent Route")
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Saved Routes (${savedRoutes.size})",
                    style = MaterialTheme.typography.titleMedium
                )
                HorizontalDivider(modifier = Modifier.padding(top = 4.dp), color = DividerDefaults.color)
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (savedRoutes.isEmpty()) {
                item {
                    Text(
                        text = "No saved routes yet. Add your daily home-to-faculty commute above!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                items(savedRoutes, key = { it.routeId ?: it.hashCode().toString() }) { route ->
                    val originLoc = route.originZone?.let { CampusLocationProvider.findById(it) }
                    val destLoc = route.destinationZone?.let { CampusLocationProvider.findById(it) }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Place,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.size(4.dp))
                                    Text(
                                        text = originLoc?.name ?: route.originZone ?: "Origin",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Icon(
                                        Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .size(12.dp)
                                    )
                                    Text(
                                        text = destLoc?.name ?: route.destinationZone ?: "Destination",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                if (!route.preferredTime.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Time: ${route.preferredTime}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    val rId = route.routeId ?: return@IconButton
                                    coroutineScope.launch {
                                        repository.deleteSavedRoute(rId).fold(
                                            onSuccess = {
                                                snackbarHostState.showSnackbar("Route deleted")
                                                refreshRoutes()
                                            },
                                            onFailure = { err ->
                                                snackbarHostState.showSnackbar("Delete failed: ${err.message}")
                                            }
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete Route",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
