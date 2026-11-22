package com.fopgang.umpooling.ai

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RideParserRepositoryTest {

    private lateinit var repository: RideParserRepository

    @Before
    fun setUp() {
        repository = RideParserRepository()
    }

    @Test
    fun createManualFallbackState_populatesDefaultMissingFields() {
        val state = repository.createManualFallbackState(
            rawQuery = "some unclear voice snippet",
            reason = "Custom failure reason"
        )

        assertFalse(state.isSuccess)
        assertTrue(state.requiresManualInput)
        assertTrue(state.isOriginMissing)
        assertTrue(state.isDestinationMissing)
        assertTrue(state.isTimeMissing)
        assertEquals(1, state.seats)
        assertEquals(0.0, state.confidence, 0.001)
        assertEquals("Custom failure reason", state.errorMessage)
    }

    @Test
    fun fallbackHeuristicParse_extractsLocationsAndSeats() {
        val state = repository.fallbackHeuristicParse(
            query = "Need ride from Tiara to FSKTM for 2 pax"
        )

        assertEquals("Tiara Damansara", state.origin)
        assertEquals("FSKTM", state.destination)
        assertEquals(2, state.seats)
        assertFalse(state.isOriginMissing)
        assertFalse(state.isDestinationMissing)
        assertTrue(state.isTimeMissing) // Time wasn't specified in snippet
        assertTrue(state.requiresManualInput)
    }

    @Test
    fun fallbackHeuristicParse_missingDestination_flagsProperly() {
        val state = repository.fallbackHeuristicParse(
            query = "Going from Pacific Star at 8am"
        )

        assertEquals("Pacific Star", state.origin)
        assertNull(state.destination)
        assertFalse(state.isOriginMissing)
        assertTrue(state.isDestinationMissing)
        assertTrue(state.requiresManualInput)
    }

    @Test
    fun parseRideRequest_emptyQuery_returnsManualFallback() = runBlocking {
        val result = repository.parseRideRequest("   ")

        assertTrue(result.isSuccess)
        val state = result.getOrThrow()
        assertFalse(state.isSuccess)
        assertTrue(state.requiresManualInput)
        assertTrue(state.isOriginMissing)
        assertTrue(state.isDestinationMissing)
    }
}
