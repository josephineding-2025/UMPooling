package com.fopgang.umpooling

import com.fopgang.umpooling.data.GeoPoint
import com.fopgang.umpooling.data.NewSavedRoute
import com.fopgang.umpooling.data.SavedRoute
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedRouteTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun savedRouteDeserializesCorrectly() {
        val rawJson = """
            {
                "route_id": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
                "user_id": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
                "origin_zone": "tiara_damansara",
                "destination_zone": "fsktm",
                "distance_km": 4.2,
                "preferred_time": "08:30:00",
                "created_at": "2026-11-17T08:30:00Z"
            }
        """.trimIndent()

        val route = json.decodeFromString<SavedRoute>(rawJson)
        assertEquals("9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d", route.routeId)
        assertEquals("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11", route.userId)
        assertEquals("tiara_damansara", route.originZone)
        assertEquals("fsktm", route.destinationZone)
        assertEquals(4.2, route.distanceKm)
        assertEquals("08:30:00", route.preferredTime)
    }

    @Test
    fun newSavedRouteSerializesWithEwkt() {
        val newRoute = NewSavedRoute(
            userId = "user-123",
            originGeo = GeoPoint.ewkt(101.636, 3.127),
            destinationGeo = GeoPoint.ewkt(101.65049, 3.12841),
            originZone = "tiara_damansara",
            destinationZone = "fsktm",
            preferredTime = "08:30"
        )

        val encoded = json.encodeToString(newRoute)
        assertTrue(encoded.contains("SRID=4326;POINT(101.636 3.127)"))
        assertTrue(encoded.contains("SRID=4326;POINT(101.65049 3.12841)"))
        assertTrue(encoded.contains("tiara_damansara"))
        assertTrue(encoded.contains("fsktm"))
    }
}
