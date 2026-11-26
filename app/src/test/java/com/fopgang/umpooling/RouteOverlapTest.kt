package com.fopgang.umpooling

import com.fopgang.umpooling.data.OfferStatus
import com.fopgang.umpooling.data.RequestStatus
import com.fopgang.umpooling.data.RideRequest
import com.fopgang.umpooling.data.TripOffer
import com.fopgang.umpooling.ui.matching.RouteOverlapEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteOverlapTest {

    @Test
    fun overlappingRouteMatchesWithinRadiusAndTimeWindow() {
        // Pickup ~150m from driver origin (near KK8), Dest ~80m from FSKTM, 10 min apart
        val matches = RouteOverlapEngine.isOverlappingRoute(
            requestOriginLat = 3.1299,
            requestOriginLng = 101.6493,
            requestDestLat = 3.1284,
            requestDestLng = 101.6505,
            driverOriginLat = 3.1308,
            driverOriginLng = 101.6503,
            driverDestLat = 3.1288,
            driverDestLng = 101.6508,
            requestTimeIso = "2026-11-25T08:10:00+08:00",
            driverTimeIso = "2026-11-25T08:20:00+08:00"
        )
        assertTrue(matches)
    }

    @Test
    fun routeOutsideRadiusOrTimeWindowDoesNotMatch() {
        // Time window > 15 min apart (40 min difference)
        val outsideTime = RouteOverlapEngine.isOverlappingRoute(
            requestOriginLat = 3.1299,
            requestOriginLng = 101.6493,
            requestDestLat = 3.1284,
            requestDestLng = 101.6505,
            driverOriginLat = 3.1299,
            driverOriginLng = 101.6493,
            driverDestLat = 3.1284,
            driverDestLng = 101.6505,
            requestTimeIso = "2026-11-25T08:00:00+08:00",
            driverTimeIso = "2026-11-25T08:40:00+08:00"
        )
        assertFalse(outsideTime)

        // Origin > 500 m apart (KK8 vs Vista Angkasa)
        val outsideOrigin = RouteOverlapEngine.isOverlappingRoute(
            requestOriginLat = 3.1299,
            requestOriginLng = 101.6493,
            requestDestLat = 3.1284,
            requestDestLng = 101.6505,
            driverOriginLat = 3.1130,
            driverOriginLng = 101.6619,
            driverDestLat = 3.1284,
            driverDestLng = 101.6505
        )
        assertFalse(outsideOrigin)
    }

    @Test
    fun driverNotificationSummaryIncludesRequiredFieldsAndCo2() {
        val alert = RouteOverlapEngine.buildDriverNotificationSummary(
            departureTime = "08:15 AM",
            destinationName = "FSKTM",
            availableSeats = 3,
            distanceKm = 5.0
        )

        assertEquals("Overlapping Ride Request", alert.title)
        assertEquals(1.0, alert.estimatedCo2Kg, 0.001) // 5.0 km * 0.20 kg/km = 1.00 kg
        assertTrue(alert.body.contains("08:15 AM"))
        assertTrue(alert.body.contains("FSKTM"))
        assertTrue(alert.body.contains("3 seat(s)"))
        assertTrue(alert.body.contains("1.00 kg CO₂ saved"))
    }

    @Test
    fun expiryAndCancellationFilterRemovesStaleAndCancelledItems() {
        val nowMs = RouteOverlapEngine.parseIsoTimestampMs("2026-11-25T10:00:00+08:00")!!

        val offers = listOf(
            TripOffer("o1", "d1", 2.0, 2, OfferStatus.OPEN, "2026-11-25T11:00:00+08:00"),      // active
            TripOffer("o2", "d1", 2.0, 2, OfferStatus.OPEN, "2026-11-25T09:00:00+08:00"),      // expired
            TripOffer("o3", "d1", 2.0, 2, OfferStatus.CANCELLED, "2026-11-25T12:00:00+08:00"), // cancelled
            TripOffer("o4", "d1", 2.0, 0, OfferStatus.OPEN, "2026-11-25T12:00:00+08:00")       // no seats
        )

        val activeOffers = RouteOverlapEngine.filterActiveOffers(offers, nowMs)
        assertEquals(listOf("o1"), activeOffers.map { it.tripOfferId })

        val requests = listOf(
            RideRequest("r1", "u1", null, RequestStatus.PENDING, null, "2026-11-25T11:30:00+08:00"), // active
            RideRequest("r2", "u1", null, RequestStatus.PENDING, null, "2026-11-25T08:30:00+08:00"), // expired
            RideRequest("r3", "u1", null, RequestStatus.ERROR, null, "2026-11-25T12:00:00+08:00")    // cancelled
        )

        val activeRequests = RouteOverlapEngine.filterActiveRequests(requests, nowMs)
        assertEquals(listOf("r1"), activeRequests.map { it.requestId })
    }
}
