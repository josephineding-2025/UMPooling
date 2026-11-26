package com.fopgang.umpooling

import com.fopgang.umpooling.data.CampusLocationProvider
import com.fopgang.umpooling.data.TripOffer
import com.fopgang.umpooling.ui.matching.FeedFilterCriteria
import com.fopgang.umpooling.ui.matching.FeedFilterEngine
import com.fopgang.umpooling.ui.matching.FeedSortOption
import com.fopgang.umpooling.ui.matching.FeedTimeWindow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedFilterTest {

    private val sampleOffers = listOf(
        TripOffer(
            tripOfferId = "offer-1",
            driverId = "driver-1",
            costPerSeat = 2.00,
            availableSeats = 2,
            departureTime = "2026-11-25T08:15:00+08:00",
            originZone = "kk1",
            destinationZone = "fsktm",
            distanceKm = 2.5
        ),
        TripOffer(
            tripOfferId = "offer-2",
            driverId = "driver-2",
            costPerSeat = 4.00,
            availableSeats = 3,
            departureTime = "2026-11-25T14:30:00+08:00",
            originZone = "vista_angkasa",
            destinationZone = "main_library",
            distanceKm = 6.0
        ),
        TripOffer(
            tripOfferId = "offer-3",
            driverId = "driver-3",
            costPerSeat = 3.00,
            availableSeats = 1,
            departureTime = "2026-11-25T19:45:00+08:00",
            originZone = "prima_16",
            destinationZone = "fsktm",
            distanceKm = null // Should fall back to Haversine road estimate
        )
    )

    @Test
    fun destinationFilterMatchesTargetDestination() {
        val fsktm = CampusLocationProvider.findById("fsktm")
        assertNotNull(fsktm)

        val result = FeedFilterEngine.filterAndSort(
            offers = sampleOffers,
            criteria = FeedFilterCriteria(destinationLocation = fsktm)
        )

        assertEquals(2, result.size)
        assertEquals(listOf("offer-1", "offer-3"), result.map { it.offer.tripOfferId })
    }

    @Test
    fun timeWindowFilterSeparatesMorningAfternoonEvening() {
        val morning = FeedFilterEngine.filterAndSort(
            offers = sampleOffers,
            criteria = FeedFilterCriteria(timeWindow = FeedTimeWindow.MORNING)
        )
        assertEquals(listOf("offer-1"), morning.map { it.offer.tripOfferId })

        val afternoon = FeedFilterEngine.filterAndSort(
            offers = sampleOffers,
            criteria = FeedFilterCriteria(timeWindow = FeedTimeWindow.AFTERNOON)
        )
        assertEquals(listOf("offer-2"), afternoon.map { it.offer.tripOfferId })

        val evening = FeedFilterEngine.filterAndSort(
            offers = sampleOffers,
            criteria = FeedFilterCriteria(timeWindow = FeedTimeWindow.EVENING)
        )
        assertEquals(listOf("offer-3"), evening.map { it.offer.tripOfferId })
    }

    @Test
    fun proximitySortRanksNearestPickupFirst() {
        // User is near Section 16 / Prima 16 -> offer-3 (prima_16) should be closest, then offer-1 (kk1), then offer-2 (vista_angkasa)
        val prima16 = CampusLocationProvider.findById("prima_16")
        assertNotNull(prima16)

        val result = FeedFilterEngine.filterAndSort(
            offers = sampleOffers,
            criteria = FeedFilterCriteria(
                referenceOrigin = prima16,
                sortBy = FeedSortOption.PROXIMITY
            )
        )

        assertEquals(listOf("offer-3", "offer-1", "offer-2"), result.map { it.offer.tripOfferId })
        assertEquals(0.0, result[0].proximityKm!!, 0.001)
        assertTrue(result[1].proximityKm!! < result[2].proximityKm!!)
    }

    @Test
    fun co2EstimationUsesCo2CalculationServiceAndFallback() {
        val result = FeedFilterEngine.filterAndSort(
            offers = sampleOffers,
            criteria = FeedFilterCriteria(sortBy = FeedSortOption.CO2_SAVED)
        )

        // offer-2 has 6.0 km -> 1 * 6.0 * 0.20 = 1.20 kg CO2 avoided
        val offer2 = result.first { it.offer.tripOfferId == "offer-2" }
        assertEquals(1.20, offer2.co2Estimate.totalTripCo2Kg, 0.001)

        // offer-3 had null distanceKm -> fallback road distance > 0 and positive CO2 estimate
        val offer3 = result.first { it.offer.tripOfferId == "offer-3" }
        assertTrue(offer3.estimatedDistanceKm > 0.0)
        assertTrue(offer3.co2Estimate.totalTripCo2Kg > 0.0)

        // Sorted descending by CO2 saved -> offer-2 (1.20 kg) must be first
        assertEquals("offer-2", result.first().offer.tripOfferId)
    }

    @Test
    fun formatDepartureDisplayFormatsIsoTimestamp() {
        assertEquals("8:15 AM • 25 Nov", FeedFilterEngine.formatDepartureDisplay("2026-11-25T08:15:00+08:00"))
        assertEquals("7:45 PM • 25 Nov", FeedFilterEngine.formatDepartureDisplay("2026-11-25T19:45:00+08:00"))
    }
}
