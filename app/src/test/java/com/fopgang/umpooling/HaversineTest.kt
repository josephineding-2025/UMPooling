package com.fopgang.umpooling

import com.fopgang.umpooling.data.Haversine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HaversineTest {

    @Test
    fun sameCoordinatesReturnZeroDistance() {
        val dist = Haversine.haversineKm(3.12841, 101.65049, 3.12841, 101.65049)
        assertEquals(0.0, dist, 0.0001)
    }

    @Test
    fun roadDistanceAppliesCircuityFactor() {
        val lat1 = 3.1270
        val lng1 = 101.6360
        val lat2 = 3.12841
        val lng2 = 101.65049

        val direct = Haversine.haversineKm(lat1, lng1, lat2, lng2)
        val road = Haversine.estimateRoadDistanceKm(lat1, lng1, lat2, lng2)

        assertTrue(direct > 0)
        assertTrue(road > direct)
        assertEquals(Math.round(direct * 1.3 * 100.0) / 100.0, road, 0.01)
    }

    @Test
    fun withinRadiusMatchesCorrectly() {
        // Within 500m (0.5km)
        val lat1 = 3.12841
        val lng1 = 101.65049
        // 100 meters away
        val lat2 = 3.12850
        val lng2 = 101.65100

        assertTrue(Haversine.isWithinRadius(lat1, lng1, lat2, lng2, 0.5))
    }
}
