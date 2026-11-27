package com.fopgang.umpooling.ui.matching

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fopgang.umpooling.R
import com.fopgang.umpooling.booking.Glass
import com.fopgang.umpooling.booking.RideAccent
import com.fopgang.umpooling.booking.RideFont
import com.fopgang.umpooling.booking.RideInk
import com.fopgang.umpooling.booking.RideMuted
import com.fopgang.umpooling.data.TripOffer
import com.fopgang.umpooling.data.repository.RideRepository
import com.fopgang.umpooling.ui.components.FeatureIcon
import com.fopgang.umpooling.ui.components.FeatureSymbol
import com.fopgang.umpooling.ui.components.LoadingState
import com.fopgang.umpooling.ui.theme.UMPoolingTheme
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

private const val FEED_PAGE_SIZE = 15

@Composable
private fun FeedPage(content: @Composable BoxScope.() -> Unit) {
    ProvideTextStyle(TextStyle(fontFamily = RideFont, fontSize = 14.sp, lineHeight = 19.sp, color = RideInk)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xFFFDEAE2))
                .drawWithCache {
                    val glow = Brush.radialGradient(
                        listOf(Color(0xFFF9CDBF), Color.Transparent),
                        Offset(size.width, size.height * .18f),
                        size.width
                    )
                    val light = Brush.radialGradient(
                        listOf(Color.White, Color.Transparent),
                        Offset.Zero,
                        size.width
                    )
                    onDrawBehind {
                        drawRect(glow)
                        drawRect(light)
                    }
                },
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize()
                    .safeDrawingPadding(),
                content = content
            )
        }
    }
}

/**
 * Filterable Carpool Feed (FR2.3) styled to match target design spec.
 */
@Composable
fun FeedScreen(
    repository: RideRepository = remember { RideRepository() },
    allowDemoFallback: Boolean = false,
    onOfferClicked: ((TripOffer) -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    onNeedRide: (() -> Unit)? = null,
    onOfferRide: (() -> Unit)? = null,
    offerFilter: (TripOffer) -> Boolean = { true },
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val loadErrorMessage = stringResource(R.string.live_offer_load_failed)

    var rawOffers by remember {
        mutableStateOf<List<TripOffer>>(if (allowDemoFallback) demoSampleOffers() else emptyList())
    }
    var isLoading by remember { mutableStateOf(false) }
    var loadFailed by remember { mutableStateOf(false) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var hasMorePages by remember { mutableStateOf(!allowDemoFallback) }
    var currentOffset by remember { mutableIntStateOf(0) }

    var showFilterPanel by remember { mutableStateOf(true) }
    var filterCriteria by remember { mutableStateOf(FeedFilterCriteria()) }

    val eligibleOffers = rawOffers.filter(offerFilter)
    val filteredOffers = remember(eligibleOffers, filterCriteria) {
        FeedFilterEngine.filterAndSort(eligibleOffers, filterCriteria)
    }

    val activeFilterCount = remember(filterCriteria) {
        var count = 0
        if (filterCriteria.referenceOrigin != null) count++
        if (filterCriteria.destinationLocation != null) count++
        if (filterCriteria.timeWindow != FeedTimeWindow.ALL) count++
        count
    }

    fun loadFirstPage() {
        if (isLoading || isLoadingMore) return
        isLoading = true
        loadFailed = false
        coroutineScope.launch {
            repository.getOpenTripOffersPaged(limit = FEED_PAGE_SIZE, offset = 0).fold(
                onSuccess = { page ->
                    rawOffers = RouteOverlapEngine.filterActiveOffers(page)
                    hasMorePages = page.size >= FEED_PAGE_SIZE
                    currentOffset = page.size
                },
                onFailure = { loadFailed = true }
            )
            isLoading = false
        }
    }

    fun loadNextPage() {
        if (isLoading || isLoadingMore || !hasMorePages) return
        isLoadingMore = true
        coroutineScope.launch {
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
                onFailure = {
                    isLoadingMore = false
                    snackbarHostState.showSnackbar(loadErrorMessage)
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        if (!allowDemoFallback) {
            loadFirstPage()
        }
    }

    FeedPage {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    onClick = { onBack?.invoke() },
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
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.available_rides),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = RideInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "${filteredOffers.size} rides match",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = RideMuted,
                        textAlign = TextAlign.Center
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = if (showFilterPanel || activeFilterCount > 0) Color(0xFFFDE0D2) else Color.White.copy(alpha = .85f),
                        border = BorderStroke(1.dp, Color(0xFFF5D7CA))
                    ) {
                        IconButton(
                            onClick = { showFilterPanel = !showFilterPanel },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Toggle filters",
                                tint = RideInk
                            )
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = .85f),
                        border = BorderStroke(1.dp, Color(0xFFF5D7CA))
                    ) {
                        IconButton(
                            onClick = ::loadFirstPage,
                            enabled = !isLoading && !isLoadingMore,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.live_offer_refresh),
                                tint = RideInk
                            )
                        }
                    }
                }
            }

            // Mode Selector Pill (Available Rides / Need a ride / Offer a ride)
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = .65f),
                border = BorderStroke(1.dp, Color.White),
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(5.dp)) {
                    listOf(stringResource(R.string.available_rides), stringResource(R.string.need_a_ride), stringResource(R.string.offer_a_ride)).forEachIndexed { index, title ->
                        val selected = index == 0
                        Surface(
                            onClick = {
                                if (!selected) {
                                    when (index) {
                                        1 -> onNeedRide?.invoke() ?: onBack?.invoke()
                                        2 -> onOfferRide?.invoke() ?: onBack?.invoke()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = CircleShape,
                            color = if (selected) RideInk else Color.Transparent,
                            shadowElevation = if (selected) 6.dp else 0.dp
                        ) {
                            Box(
                                Modifier.heightIn(min = 38.dp)
                                    .background(
                                        if (selected) Brush.verticalGradient(
                                            listOf(
                                                Color(0xFF5A514C),
                                                RideInk
                                            )
                                        ) else Brush.verticalGradient(
                                            listOf(Color.Transparent, Color.Transparent)
                                        )
                                    )
                                    .padding(vertical = 5.dp, horizontal = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    title,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.White else RideInk,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Filter Panel
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

            // Main Content Area
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading -> {
                        LoadingState(Modifier.align(Alignment.Center))
                    }
                    loadFailed -> {
                        FeedEmptyState(
                            title = loadErrorMessage,
                            subtitle = null,
                            actionLabel = stringResource(R.string.live_offer_refresh),
                            onAction = ::loadFirstPage,
                        )
                    }
                    eligibleOffers.isEmpty() && !hasMorePages -> {
                        FeedEmptyState(
                            title = stringResource(R.string.live_offer_empty),
                            subtitle = stringResource(R.string.live_offer_empty_hint),
                            actionLabel = if (allowDemoFallback) "Load Sample Rides (Demo)" else null,
                            onAction = if (allowDemoFallback) {
                                {
                                    rawOffers = demoSampleOffers()
                                    hasMorePages = false
                                }
                            } else null
                        )
                    }
                    filteredOffers.isEmpty() && !hasMorePages -> {
                        FeedEmptyState(
                            title = "No rides match your filters",
                            subtitle = "Try clearing your destination or departure window filter to see all ${eligibleOffers.size} open rides.",
                            actionLabel = "Clear Filters",
                            onAction = {
                                filterCriteria = FeedFilterCriteria(sortBy = FeedSortOption.DEPARTURE_TIME)
                            }
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                            .padding(top = 8.dp, bottom = 20.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isLoadingMore) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(28.dp),
                                                color = RideInk
                                            )
                                        } else {
                                            val remainingCount = (rawOffers.size - currentOffset).coerceAtLeast(0)
                                            val footerText = if (remainingCount > 0) "Load more rides ($remainingCount left)" else "Load more rides"
                                            Button(
                                                onClick = { loadNextPage() },
                                                shape = CircleShape,
                                                colors = ButtonDefaults.buttonColors(containerColor = RideInk),
                                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                                                modifier = Modifier.heightIn(min = 48.dp)
                                            ) {
                                                Text(
                                                    footerText,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 12.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LocationSelectorField(
            label = stringResource(R.string.near_pickup_proximity),
            selectedLocation = criteria.referenceOrigin,
            onLocationSelected = { loc ->
                onCriteriaChange(
                    criteria.copy(
                        referenceOrigin = loc,
                        sortBy = FeedSortOption.PROXIMITY
                    )
                )
            }
        )

        LocationSelectorField(
            label = stringResource(R.string.destination_filter),
            selectedLocation = criteria.destinationLocation,
            onLocationSelected = { loc ->
                onCriteriaChange(criteria.copy(destinationLocation = loc))
            }
        )

        // Time window chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(FeedTimeWindow.entries) { window ->
                val selected = criteria.timeWindow == window
                Surface(
                    onClick = { onCriteriaChange(criteria.copy(timeWindow = window)) },
                    shape = CircleShape,
                    color = if (selected) RideInk else Color.White.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, if (selected) RideInk else Color.White),
                    shadowElevation = if (selected) 4.dp else 0.dp
                ) {
                    Text(
                        text = stringResource(window.labelRes),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) Color.White else RideInk,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Sort option pill bar
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.65f),
            border = BorderStroke(1.dp, Color.White),
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FeedSortOption.entries.forEach { option ->
                    val enabled = option != FeedSortOption.PROXIMITY || criteria.referenceOrigin != null
                    val selected = criteria.sortBy == option
                    Surface(
                        onClick = { if (enabled) onCriteriaChange(criteria.copy(sortBy = option)) },
                        enabled = enabled,
                        shape = CircleShape,
                        color = if (selected) Color.White else Color.Transparent,
                        shadowElevation = if (selected) 2.dp else 0.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .heightIn(min = 36.dp)
                                .padding(vertical = 4.dp, horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(option.labelRes),
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (selected) RideInk else if (enabled) RideInk else RideMuted.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedOfferCard(
    item: EnrichedTripOffer,
    onClick: () -> Unit
) {
    val offer = item.offer
    val co2Formatted = String.format(Locale.US, "%.1f", item.co2Estimate.totalTripCo2Kg)
    val distanceFormatted = String.format(Locale.US, "%.1f", item.estimatedDistanceKm)
    val driverName = driverDisplayName(offer.driverId)
    val initials = driverInitials(driverName)
    val rating = driverRating(offer.driverId)
    val originShort = formatShortLocation(item.originName)
    val destShort = formatShortLocation(item.destinationName)
    val timeOnly = extractTimeOnly(item.formattedDeparture)
    val costInt = offer.costPerSeat.roundToInt()

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = Color.White.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, Color.White),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Driver Avatar
            Surface(
                shape = CircleShape,
                color = Color(0xFFFDE8E0),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = initials,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = RideInk
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Main Details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "$originShort → $destShort",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = RideInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$driverName · $timeOnly · ${offer.availableSeats} seat${if (offer.availableSeats > 1) "s" else ""}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = RideMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "-$co2Formatted kg CO2 · $distanceFormatted km away",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Rating & Price
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.height(58.dp)
            ) {
                Text(
                    text = "★ $rating",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = RideInk
                )
                Text(
                    text = "RM $costInt",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = RideInk
                )
            }
        }
    }
}

private fun formatShortLocation(name: String): String {
    val clean = name.split("(")[0].trim()
    return when {
        clean.contains("11", ignoreCase = true) || clean.contains("Kesebelas", ignoreCase = true) -> "Kolej 11"
        clean.contains("8", ignoreCase = true) || clean.contains("Kelapan", ignoreCase = true) -> "Kolej 8"
        clean.contains("1", ignoreCase = true) || clean.contains("Pertama", ignoreCase = true) -> "Kolej 1"
        clean.contains("3", ignoreCase = true) || clean.contains("Ketiga", ignoreCase = true) -> "Kolej 3"
        clean.contains("fsktm", ignoreCase = true) || clean.contains("Computer Science", ignoreCase = true) -> "FSKTM"
        clean.contains("main_library", ignoreCase = true) || clean.contains("Main Library", ignoreCase = true) || clean.contains("Perpustakaan", ignoreCase = true) -> "Main Library"
        clean.contains("main_gate", ignoreCase = true) || clean.contains("Main Gate", ignoreCase = true) -> "UM Main Gate"
        clean.contains("engineering", ignoreCase = true) -> "Engineering"
        clean.contains("vista", ignoreCase = true) -> "Vista Angkasa"
        else -> clean.take(18)
    }
}

private fun driverDisplayName(driverId: String): String {
    return when (driverId) {
        "demo-driver-1" -> "Hafiz R."
        "demo-driver-2" -> "Nurul A."
        "demo-driver-3" -> "Farid H."
        "demo-driver-4" -> "Aisyah R."
        "demo-driver-5" -> "Zain B."
        else -> {
            val parts = driverId.replace(Regex("""[^a-zA-Z\s]"""), " ").trim().split(Regex("""\s+"""))
            if (parts.size >= 2) "${parts[0].replaceFirstChar { it.uppercase(Locale.US) }} ${parts[1].take(1).uppercase(Locale.US)}."
            else if (parts.isNotEmpty() && parts[0].isNotBlank()) parts[0].replaceFirstChar { it.uppercase(Locale.US) }
            else "Driver"
        }
    }
}

private fun driverInitials(name: String): String {
    val parts = name.trim().split(Regex("""\s+"""))
    return if (parts.size >= 2) {
        "${parts[0].take(1)}${parts[1].take(1)}".uppercase(Locale.US)
    } else if (name.length >= 2) {
        name.take(2).uppercase(Locale.US)
    } else "UM"
}

private fun driverRating(driverId: String): String {
    return when (driverId) {
        "demo-driver-1" -> "4.9"
        "demo-driver-2" -> "4.8"
        "demo-driver-3" -> "4.7"
        "demo-driver-4" -> "4.6"
        "demo-driver-5" -> "4.9"
        else -> {
            val hash = kotlin.math.abs(driverId.hashCode())
            val rating = 4.5 + (hash % 5) * 0.1
            String.format(Locale.US, "%.1f", rating)
        }
    }
}

private fun extractTimeOnly(formattedDeparture: String): String {
    val parts = formattedDeparture.split("•", "·", " ")
    return parts.firstOrNull { it.contains(":") } ?: formattedDeparture.take(5)
}

@Composable
private fun FeedEmptyState(
    title: String,
    subtitle: String?,
    actionLabel: String?,
    onAction: (() -> Unit)?
) {
    Glass(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Place,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = RideAccent
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = RideInk,
                textAlign = TextAlign.Center
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = RideMuted,
                    textAlign = TextAlign.Center
                )
            }
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onAction,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = RideInk),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Text(actionLabel, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

private fun demoSampleOffers(): List<TripOffer> = listOf(
    TripOffer(
        tripOfferId = "demo-1",
        driverId = "demo-driver-1",
        costPerSeat = 4.00,
        availableSeats = 2,
        departureTime = "2026-11-25T08:15:00+08:00",
        originZone = "kk11",
        destinationZone = "fsktm",
        distanceKm = 0.3
    ),
    TripOffer(
        tripOfferId = "demo-2",
        driverId = "demo-driver-2",
        costPerSeat = 3.00,
        availableSeats = 1,
        departureTime = "2026-11-25T08:40:00+08:00",
        originZone = "kk11",
        destinationZone = "main_library",
        distanceKm = 0.3
    ),
    TripOffer(
        tripOfferId = "demo-4",
        driverId = "demo-driver-4",
        costPerSeat = 2.00,
        availableSeats = 2,
        departureTime = "2026-11-25T12:20:00+08:00",
        originZone = "kk11",
        destinationZone = "main_gate",
        distanceKm = 0.3
    ),
    TripOffer(
        tripOfferId = "demo-3",
        driverId = "demo-driver-3",
        costPerSeat = 2.00,
        availableSeats = 2,
        departureTime = "2026-11-25T09:00:00+08:00",
        originZone = "kk3",
        destinationZone = "main_library",
        distanceKm = 0.9
    )
)

@Preview(name = "Feed Screen", widthDp = 390, heightDp = 844)
@Composable
private fun FeedScreenPreview() {
    UMPoolingTheme {
        FeedScreen(allowDemoFallback = true)
    }
}
