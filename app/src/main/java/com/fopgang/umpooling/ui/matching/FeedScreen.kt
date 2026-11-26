package com.fopgang.umpooling.ui.matching

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fopgang.umpooling.data.TripOffer
import com.fopgang.umpooling.data.repository.RideRepository
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

private const val FEED_PAGE_SIZE = 15

/**
 * Filterable Carpool Feed (FR2.3) with proximity sorting, destination & departure time filters,
 * pagination, and point-of-decision CO2 savings badges powered by Member 4's Co2CalculationService.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    repository: RideRepository = remember { RideRepository() },
    allowDemoFallback: Boolean = false,
    onOfferClicked: ((TripOffer) -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var rawOffers by remember { mutableStateOf<List<TripOffer>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var hasMorePages by remember { mutableStateOf(true) }
    var currentOffset by remember { mutableIntStateOf(0) }

    var showFilterPanel by remember { mutableStateOf(true) }
    var filterCriteria by remember { mutableStateOf(FeedFilterCriteria()) }

    val filteredOffers = remember(rawOffers, filterCriteria) {
        FeedFilterEngine.filterAndSort(rawOffers, filterCriteria)
    }

    val activeFilterCount = remember(filterCriteria) {
        var count = 0
        if (filterCriteria.referenceOrigin != null) count++
        if (filterCriteria.destinationLocation != null) count++
        if (filterCriteria.timeWindow != FeedTimeWindow.ALL) count++
        count
    }

    fun loadFirstPage() {
        coroutineScope.launch {
            isLoading = true
            currentOffset = 0
            repository.getOpenTripOffersPaged(limit = FEED_PAGE_SIZE, offset = 0).fold(
                onSuccess = { page ->
                    // Hide offers that are past departure, full or cancelled (FR2.2).
                    rawOffers = RouteOverlapEngine.filterActiveOffers(page)
                    hasMorePages = page.size >= FEED_PAGE_SIZE
                    currentOffset = page.size
                    isLoading = false
                },
                onFailure = { err ->
                    isLoading = false
                    snackbarHostState.showSnackbar("Unable to load offers: ${err.message}")
                }
            )
        }
    }

    fun loadNextPage() {
        if (isLoadingMore || !hasMorePages) return
        coroutineScope.launch {
            isLoadingMore = true
            repository.getOpenTripOffersPaged(limit = FEED_PAGE_SIZE, offset = currentOffset).fold(
                onSuccess = { nextPage ->
                    val existingIds = rawOffers.mapNotNull { it.tripOfferId }.toSet()
                    val uniqueNext = nextPage.filter {
                        it.tripOfferId == null || it.tripOfferId !in existingIds
                    }
                    rawOffers = rawOffers + RouteOverlapEngine.filterActiveOffers(uniqueNext)
                    hasMorePages = nextPage.size >= FEED_PAGE_SIZE
                    currentOffset += nextPage.size
                    isLoadingMore = false
                },
                onFailure = { err ->
                    isLoadingMore = false
                    snackbarHostState.showSnackbar("Unable to load more offers: ${err.message}")
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        loadFirstPage()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Carpool Feed")
                        if (!isLoading) {
                            Text(
                                text = "${filteredOffers.size} ride(s) available",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterPanel = !showFilterPanel }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Toggle filters",
                            tint = if (activeFilterCount > 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { loadFirstPage() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh feed")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedVisibility(visible = showFilterPanel) {
                FeedFilterPanel(
                    criteria = filterCriteria,
                    activeFilterCount = activeFilterCount,
                    onCriteriaChange = { filterCriteria = it },
                    onClearAll = {
                        filterCriteria = FeedFilterCriteria(sortBy = FeedSortOption.DEPARTURE_TIME)
                    }
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    rawOffers.isEmpty() -> {
                        FeedEmptyState(
                            title = "No open ride offers right now",
                            subtitle = "Be the first to offer a ride or check back closer to peak commuting hours!",
                            actionLabel = if (allowDemoFallback) "Load Sample Rides (Demo)" else null,
                            onAction = if (allowDemoFallback) {
                                {
                                    rawOffers = demoSampleOffers()
                                    hasMorePages = false
                                }
                            } else null
                        )
                    }
                    filteredOffers.isEmpty() -> {
                        FeedEmptyState(
                            title = "No rides match your filters",
                            subtitle = "Try clearing your destination or departure window filter to see all ${rawOffers.size} open rides.",
                            actionLabel = "Clear Filters",
                            onAction = {
                                filterCriteria = FeedFilterCriteria(sortBy = FeedSortOption.DEPARTURE_TIME)
                            }
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = filteredOffers,
                                key = { it.offer.tripOfferId ?: it.offer.hashCode().toString() }
                            ) { enriched ->
                                FeedOfferCard(
                                    item = enriched,
                                    onClick = { onOfferClicked?.invoke(enriched.offer) }
                                )
                            }

                            if (hasMorePages) {
                                item(key = "load_more_footer") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isLoadingMore) {
                                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                        } else {
                                            OutlinedButton(onClick = { loadNextPage() }) {
                                                Text("Load More Rides")
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
    }
}

@Composable
private fun FeedFilterPanel(
    criteria: FeedFilterCriteria,
    activeFilterCount: Int,
    onCriteriaChange: (FeedFilterCriteria) -> Unit,
    onClearAll: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LocationSelectorField(
                    label = "Near Pickup (Proximity)",
                    selectedLocation = criteria.referenceOrigin,
                    onLocationSelected = { loc ->
                        onCriteriaChange(
                            criteria.copy(
                                referenceOrigin = loc,
                                sortBy = FeedSortOption.PROXIMITY
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
                if (criteria.referenceOrigin != null) {
                    IconButton(
                        onClick = {
                            onCriteriaChange(
                                criteria.copy(
                                    referenceOrigin = null,
                                    sortBy = if (criteria.sortBy == FeedSortOption.PROXIMITY)
                                        FeedSortOption.DEPARTURE_TIME else criteria.sortBy
                                )
                            )
                        }
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear pickup filter")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LocationSelectorField(
                    label = "Destination Filter",
                    selectedLocation = criteria.destinationLocation,
                    onLocationSelected = { loc ->
                        onCriteriaChange(criteria.copy(destinationLocation = loc))
                    },
                    modifier = Modifier.weight(1f)
                )
                if (criteria.destinationLocation != null) {
                    IconButton(
                        onClick = { onCriteriaChange(criteria.copy(destinationLocation = null)) }
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear destination filter")
                    }
                }
            }

            // Departure time window chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(FeedTimeWindow.entries) { window ->
                    FilterChip(
                        selected = criteria.timeWindow == window,
                        onClick = { onCriteriaChange(criteria.copy(timeWindow = window)) },
                        label = { Text(window.label) }
                    )
                }
            }

            // Sort options & reset row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(FeedSortOption.entries) { option ->
                        val enabled = option != FeedSortOption.PROXIMITY || criteria.referenceOrigin != null
                        FilterChip(
                            selected = criteria.sortBy == option,
                            enabled = enabled,
                            onClick = { onCriteriaChange(criteria.copy(sortBy = option)) },
                            label = { Text("Sort: ${option.label}") }
                        )
                    }
                }

                if (activeFilterCount > 0) {
                    TextButton(onClick = onClearAll) {
                        Text("Reset ($activeFilterCount)")
                    }
                }
            }
        }
    }
    HorizontalDivider()
}

@Composable
private fun FeedOfferCard(
    item: EnrichedTripOffer,
    onClick: () -> Unit
) {
    val offer = item.offer
    val co2Formatted = String.format(Locale.US, "%.2f", item.co2Estimate.totalTripCo2Kg)
    val distanceFormatted = String.format(Locale.US, "%.1f", item.estimatedDistanceKm)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "From",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .width(44.dp)
                                .alignByBaseline()
                        )
                        Text(
                            text = item.originName,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.alignByBaseline()
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "To",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .width(44.dp)
                                .alignByBaseline()
                        )
                        Text(
                            text = item.destinationName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.alignByBaseline()
                        )
                    }
                }

                Text(
                    text = "RM ${String.format(Locale.US, "%.2f", offer.costPerSeat)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Point-of-decision CO2 badge + proximity distance pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "🌱 ~$co2Formatted kg CO₂ saved • $distanceFormatted km",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF1B5E20),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                item.proximityKm?.let { proxKm ->
                    val proxText = if (proxKm < 1.0) {
                        "${(proxKm * 1000).roundToInt()} m away"
                    } else {
                        String.format(Locale.US, "%.1f km away", proxKm)
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = proxText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

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
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.formattedDeparture,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Badge {
                    Text(
                        text = "${offer.availableSeats} seat(s) left",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedEmptyState(
    title: String,
    subtitle: String,
    actionLabel: String?,
    onAction: (() -> Unit)?
) {
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
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onAction,
                colors = ButtonDefaults.outlinedButtonColors()
            ) {
                Text(actionLabel)
            }
        }
    }
}

private fun demoSampleOffers(): List<TripOffer> = listOf(
    TripOffer(
        tripOfferId = "demo-1",
        driverId = "demo-driver-1",
        costPerSeat = 2.00,
        availableSeats = 3,
        departureTime = "2026-11-25T08:15:00+08:00",
        originZone = "kk8",
        destinationZone = "fsktm",
        distanceKm = 1.8
    ),
    TripOffer(
        tripOfferId = "demo-2",
        driverId = "demo-driver-2",
        costPerSeat = 3.50,
        availableSeats = 2,
        departureTime = "2026-11-25T09:00:00+08:00",
        originZone = "vista_angkasa",
        destinationZone = "fsktm",
        distanceKm = 6.2
    ),
    TripOffer(
        tripOfferId = "demo-3",
        driverId = "demo-driver-3",
        costPerSeat = 1.50,
        availableSeats = 4,
        departureTime = "2026-11-25T13:30:00+08:00",
        originZone = "kk1",
        destinationZone = "main_library",
        distanceKm = 2.4
    ),
    TripOffer(
        tripOfferId = "demo-4",
        driverId = "demo-driver-4",
        costPerSeat = 4.50,
        availableSeats = 1,
        departureTime = "2026-11-25T15:10:00+08:00",
        originZone = "pantai_panorama",
        destinationZone = "engineering",
        distanceKm = 7.8
    ),
    TripOffer(
        tripOfferId = "demo-5",
        driverId = "demo-driver-5",
        costPerSeat = 2.50,
        availableSeats = 2,
        departureTime = "2026-11-25T18:45:00+08:00",
        originZone = "prima_16",
        destinationZone = "fsktm",
        distanceKm = 4.1
    )
)
