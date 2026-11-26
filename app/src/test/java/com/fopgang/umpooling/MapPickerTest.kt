package com.fopgang.umpooling

import com.fopgang.umpooling.data.CampusLocation
import com.fopgang.umpooling.data.CampusLocationProvider
import com.fopgang.umpooling.data.Haversine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapPickerTest {

    @Test
    fun customPinGeneratesValidEwkt() {
        val lat = 3.1250
        val lng = 101.6550
        val customPin = CampusLocation(
            id = "custom_${System.currentTimeMillis()}",
            name = "Custom Pin ($lat, $lng)",
            category = "Custom Location",
            lat = lat,
            lng = lng,
            pickupName = "Custom Pin",
            pickupLat = lat,
            pickupLng = lng
        )

        assertNotNull(customPin.ewkt)
        assertEquals("SRID=4326;POINT(101.655 3.125)", customPin.ewkt)
    }

    @Test
    fun customPinIdentifiesNearestLandmark() {
        // Coordinate right next to FSKTM (3.12841, 101.65049)
        val testLat = 3.1285
        val testLng = 101.6506

        val nearest = CampusLocationProvider.allLocations
            .filter { it.lat != null && it.lng != null }
            .minByOrNull { Haversine.haversineKm(testLat, testLng, it.lat!!, it.lng!!) }

        assertNotNull(nearest)
        assertEquals("fsktm", nearest!!.id)
        val distanceMeters = Haversine.haversineKm(testLat, testLng, nearest.lat!!, nearest.lng!!) * 1000.0
        assertTrue("Should be within 30 meters of FSKTM", distanceMeters < 30.0)
    }
}
