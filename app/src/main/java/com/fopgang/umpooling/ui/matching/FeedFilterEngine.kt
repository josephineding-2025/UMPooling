package com.fopgang.umpooling.ui.matching

import com.fopgang.umpooling.ai.Co2CalculationService
import com.fopgang.umpooling.ai.Co2EstimateResult
import com.fopgang.umpooling.ai.ICo2CalculationService
import com.fopgang.umpooling.data.CampusLocation
import com.fopgang.umpooling.data.CampusLocationProvider
import com.fopgang.umpooling.data.Haversine
import com.fopgang.umpooling.data.TripOffer
import java.util.Locale

enum class FeedTimeWindow(val label: String) {
    ALL("All Times"),
    MORNING("Morning (6AM–12PM)"),
    AFTERNOON("Afternoon (12PM–6PM)"),
    EVENING("Evening (6PM–11PM)")
}

enum class FeedSortOption(val label: String) {
    PROXIMITY("Nearest Pickup"),
    DEPARTURE_TIME("Earliest Time"),
    CO2_SAVED("Most CO₂ Saved"),
    LOWEST_COST("Lowest Cost")
}

data class FeedFilterCriteria(
    val referenceOrigin: CampusLocation? = null,
    val destinationLocation: CampusLocation? = null,
    val timeWindow: FeedTimeWindow = FeedTimeWindow.ALL,
    val sortBy: FeedSortOption = FeedSortOption.DEPARTURE_TIME,
    val maxOriginRadiusKm: Double? = null
)

data class EnrichedTripOffer(
    val offer: TripOffer,
    val originName: String,
    val destinationName: String,
    val proximityKm: Double?,
    val estimatedDistanceKm: Double,
    val co2Estimate: Co2EstimateResult,
    val formattedDeparture: String
)

/**
 * Pure Kotlin filtering, sorting, proximity ranking, and CO2 estimation engine for FR2.3.
 * Integrates Member 4's [ICo2CalculationService] to display point-of-decision CO2 savings.
 */
object FeedFilterEngine {

    private const val DEFAULT_CAMPUS_TRIP_KM = 5.0
    private val ISO_TIME_REGEX = Regex("""T(\d{2}):(\d{2})""")
    private val PLAIN_TIME_REGEX = Regex("""^(\d{2}):(\d{2})""")
    private val ISO_DATE_REGEX = Regex("""^(\d{4})-(\d{2})-(\d{2})""")

    private val MONTH_NAMES = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    /**
     * Extracts 24-hour clock hour [0..23] from ISO-8601 or HH:mm strings without API 26 dependencies.
     */
    fun extractHour24(timestamp: String): Int? {
        val match = ISO_TIME_REGEX.find(timestamp) ?: PLAIN_TIME_REGEX.find(timestamp.trim())
        return match?.groupValues?.getOrNull(1)?.toIntOrNull()?.takeIf { it in 0..23 }
    }

    /**
     * Formats an ISO-8601 departure timestamp into a readable commuter label (e.g. "08:15 AM • 20 Nov").
     */
    fun formatDepartureDisplay(timestamp: String): String {
        val timeMatch = ISO_TIME_REGEX.find(timestamp) ?: PLAIN_TIME_REGEX.find(timestamp.trim())
        if (timeMatch == null) return timestamp

        val hour24 = timeMatch.groupValues[1].toIntOrNull() ?: return timestamp
        val minute = timeMatch.groupValues[2]
        val amPm = if (hour24 < 12) "AM" else "PM"
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        val formattedTime = String.format(Locale.US, "%d:%s %s", hour12, minute, amPm)

        val dateMatch = ISO_DATE_REGEX.find(timestamp)
        return if (dateMatch != null) {
            val monthIdx = (dateMatch.groupValues[2].toIntOrNull() ?: 1) - 1
            val day = dateMatch.groupValues[3].toIntOrNull() ?: 1
            val monthStr = MONTH_NAMES.getOrElse(monthIdx) { "" }
            if (monthStr.isNotEmpty()) "$formattedTime • $day $monthStr" else formattedTime
        } else {
            formattedTime
        }
    }

    /**
     * Checks whether a departure timestamp falls inside the selected [FeedTimeWindow].
     */
    fun matchesTimeWindow(timestamp: String, window: FeedTimeWindow): Boolean {
        if (window == FeedTimeWindow.ALL) return true
        val hour = extractHour24(timestamp) ?: return true
        return when (window) {
            FeedTimeWindow.ALL -> true
            FeedTimeWindow.MORNING -> hour in 6..11
            FeedTimeWindow.AFTERNOON -> hour in 12..17
            FeedTimeWindow.EVENING -> hour in 18..23
        }
    }

    /**
     * Resolves trip road distance in km, preferring [TripOffer.distanceKm] and falling back
     * to Haversine road-circuity estimation from campus coordinates via [ICo2CalculationService].
     */
    fun resolveTripDistanceKm(
        offer: TripOffer,
        co2Service: ICo2CalculationService = Co2CalculationService.instance
    ): Double {
        offer.distanceKm?.let { if (it > 0.0) return it }

        val origLoc = offer.originZone?.let { CampusLocationProvider.findById(it) }
        val destLoc = offer.destinationZone?.let { CampusLocationProvider.findById(it) }
        if (origLoc?.lat != null && origLoc.lng != null && destLoc?.lat != null && destLoc.lng != null) {
            val estimated = co2Service.estimateRoadDistanceKm(
                origLoc.lat, origLoc.lng,
                destLoc.lat, destLoc.lng
            )
            if (estimated > 0.0) return estimated
        }

        return DEFAULT_CAMPUS_TRIP_KM
    }

    /**
     * Computes point-of-decision CO2 savings for joining this offer as 1 passenger.
     */
    fun estimateOfferCo2(
        offer: TripOffer,
        co2Service: ICo2CalculationService = Co2CalculationService.instance
    ): Co2EstimateResult {
        val distanceKm = resolveTripDistanceKm(offer, co2Service)
        return co2Service.estimateCo2(
            distanceKm = distanceKm,
            passengers = 1
        )
    }

    /**
     * Enriches, filters, and sorts a list of [TripOffer] items according to [FeedFilterCriteria].
     */
    fun filterAndSort(
        offers: List<TripOffer>,
        criteria: FeedFilterCriteria,
        co2Service: ICo2CalculationService = Co2CalculationService.instance
    ): List<EnrichedTripOffer> {
        val refOrigin = criteria.referenceOrigin
        val targetDest = criteria.destinationLocation

        val enriched = offers.asSequence()
            .filter { offer ->
                targetDest == null || offer.destinationZone.equals(targetDest.id, ignoreCase = true)
            }
            .filter { offer ->
                matchesTimeWindow(offer.departureTime, criteria.timeWindow)
            }
            .map { offer ->
                val origLoc = offer.originZone?.let { CampusLocationProvider.findById(it) }
                val destLoc = offer.destinationZone?.let { CampusLocationProvider.findById(it) }

                val proximityKm = if (
                    refOrigin?.lat != null && refOrigin.lng != null &&
                    origLoc?.lat != null && origLoc.lng != null
                ) {
                    Haversine.haversineKm(refOrigin.lat, refOrigin.lng, origLoc.lat, origLoc.lng)
                } else {
                    null
                }

                val distKm = resolveTripDistanceKm(offer, co2Service)
                val co2 = co2Service.estimateCo2(distanceKm = distKm, passengers = 1)

                EnrichedTripOffer(
                    offer = offer,
                    originName = origLoc?.name ?: offer.originZone ?: "Pickup Location",
                    destinationName = destLoc?.name ?: offer.destinationZone ?: "Destination",
                    proximityKm = proximityKm,
                    estimatedDistanceKm = distKm,
                    co2Estimate = co2,
                    formattedDeparture = formatDepartureDisplay(offer.departureTime)
                )
            }
            .filter { item ->
                val maxR = criteria.maxOriginRadiusKm
                maxR == null || item.proximityKm == null || item.proximityKm <= maxR
            }
            .toList()

        return when (criteria.sortBy) {
            FeedSortOption.PROXIMITY -> enriched.sortedWith(
                compareBy<EnrichedTripOffer> { it.proximityKm ?: Double.MAX_VALUE }
                    .thenBy { it.offer.departureTime }
            )
            FeedSortOption.DEPARTURE_TIME -> enriched.sortedBy { it.offer.departureTime }
            FeedSortOption.CO2_SAVED -> enriched.sortedByDescending { it.co2Estimate.totalTripCo2Kg }
            FeedSortOption.LOWEST_COST -> enriched.sortedBy { it.offer.costPerSeat }
        }
    }
}
