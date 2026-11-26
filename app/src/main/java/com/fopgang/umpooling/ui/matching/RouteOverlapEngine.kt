package com.fopgang.umpooling.ui.matching

import com.fopgang.umpooling.ai.Co2CalculationService
import com.fopgang.umpooling.ai.ICo2CalculationService
import com.fopgang.umpooling.data.Haversine
import com.fopgang.umpooling.data.OfferStatus
import com.fopgang.umpooling.data.RequestStatus
import com.fopgang.umpooling.data.RideRequest
import com.fopgang.umpooling.data.TripOffer
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

data class DriverMatchAlert(
    val title: String,
    val body: String,
    val departureTime: String,
    val destinationName: String,
    val availableSeats: Int,
    val estimatedCo2Kg: Double
)

/**
 * Pure Kotlin matching, notification formatting, and expiry/cancellation rules for FR2.2.
 * Aligns with docs/matching-rules.md (500 m origin, 300 m destination, +/- 15 min window).
 */
object RouteOverlapEngine {

    const val DEFAULT_ORIGIN_RADIUS_KM = 0.5
    const val DEFAULT_DESTINATION_RADIUS_KM = 0.3
    const val DEFAULT_TIME_WINDOW_MINUTES = 15

    /**
     * Parses an ISO-8601 timestamp (e.g., "2026-11-25T08:15:00+08:00" or "...Z") into epoch ms.
     * Uses SimpleDateFormat for full API 24+ compatibility.
     */
    fun parseIsoTimestampMs(iso: String?): Long? {
        if (iso.isNullOrBlank()) return null
        val normalized = iso.trim().replace("Z", "+00:00")
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    isLenient = false
                    if (!pattern.endsWith("XXX")) {
                        timeZone = TimeZone.getTimeZone("Asia/Kuala_Lumpur")
                    }
                }
                val date = sdf.parse(normalized)
                if (date != null) return date.time
            } catch (_: Exception) {
                // Try next format
            }
        }
        return null
    }

    /**
     * Checks whether a passenger ride request spatially and temporally overlaps a driver's route.
     */
    fun isOverlappingRoute(
        requestOriginLat: Double,
        requestOriginLng: Double,
        requestDestLat: Double,
        requestDestLng: Double,
        driverOriginLat: Double,
        driverOriginLng: Double,
        driverDestLat: Double,
        driverDestLng: Double,
        requestTimeIso: String? = null,
        driverTimeIso: String? = null,
        originRadiusKm: Double = DEFAULT_ORIGIN_RADIUS_KM,
        destRadiusKm: Double = DEFAULT_DESTINATION_RADIUS_KM,
        timeWindowMinutes: Int = DEFAULT_TIME_WINDOW_MINUTES
    ): Boolean {
        val originDist = Haversine.haversineKm(
            requestOriginLat, requestOriginLng,
            driverOriginLat, driverOriginLng
        )
        if (originDist > originRadiusKm) return false

        val destDist = Haversine.haversineKm(
            requestDestLat, requestDestLng,
            driverDestLat, driverDestLng
        )
        if (destDist > destRadiusKm) return false

        val reqMs = parseIsoTimestampMs(requestTimeIso)
        val drvMs = parseIsoTimestampMs(driverTimeIso)
        if (reqMs != null && drvMs != null) {
            val diffMinutes = abs(reqMs - drvMs) / 60_000L
            if (diffMinutes > timeWindowMinutes) return false
        }

        return true
    }

    /**
     * Formats the driver push alert payload required by FR2.2:
     * includes departure time, destination, available seats, and estimated CO2 saved.
     */
    fun buildDriverNotificationSummary(
        departureTime: String,
        destinationName: String,
        availableSeats: Int,
        distanceKm: Double,
        co2Service: ICo2CalculationService = Co2CalculationService.instance
    ): DriverMatchAlert {
        val co2Kg = co2Service.estimateCo2(distanceKm = distanceKm, passengers = 1).totalTripCo2Kg
        val co2Text = String.format(Locale.US, "%.2f", co2Kg)
        val body = "Departs $departureTime to $destinationName • $availableSeats seat(s) • ~$co2Text kg CO₂ saved"
        return DriverMatchAlert(
            title = "Overlapping Ride Request",
            body = body,
            departureTime = departureTime,
            destinationName = destinationName,
            availableSeats = availableSeats,
            estimatedCo2Kg = co2Kg
        )
    }

    /**
     * Returns true if a trip offer is cancelled, completed, full, or past its departure time.
     */
    fun isOfferExpiredOrCancelled(
        offer: TripOffer,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Boolean {
        if (offer.status != OfferStatus.OPEN || offer.availableSeats <= 0) return true
        val depMs = parseIsoTimestampMs(offer.departureTime) ?: return false
        return depMs < nowEpochMs
    }

    /**
     * Returns true if a ride request is no longer pending or its preferred time has passed.
     */
    fun isRequestExpiredOrCancelled(
        request: RideRequest,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Boolean {
        if (!request.status.equals(RequestStatus.PENDING, ignoreCase = true)) return true
        val prefMs = parseIsoTimestampMs(request.preferredTime) ?: return false
        return prefMs < nowEpochMs
    }

    fun filterActiveOffers(
        offers: List<TripOffer>,
        nowEpochMs: Long = System.currentTimeMillis()
    ): List<TripOffer> = offers.filterNot { isOfferExpiredOrCancelled(it, nowEpochMs) }

    fun filterActiveRequests(
        requests: List<RideRequest>,
        nowEpochMs: Long = System.currentTimeMillis()
    ): List<RideRequest> = requests.filterNot { isRequestExpiredOrCancelled(it, nowEpochMs) }
}
