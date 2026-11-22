package com.fopgang.umpooling

import androidx.compose.runtime.saveable.SaverScope
import com.fopgang.umpooling.booking.*
import java.time.*
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class RideFormContractsTest {
    private val clock = Clock.fixed(Instant.parse("2026-11-17T00:00:00Z"), CAMPUS_ZONE)
    private val valid = OfferRideDraft(
        origin = RideLocation("Kolej 11", 3.12, 101.65),
        destination = RideLocation("Engineering", 3.13, 101.66),
        departureDate = LocalDate.of(2026, 11, 18),
        departureTime = LocalTime.of(8, 15),
        seats = 3,
        contributionSen = 450,
    )
    private val scope = object : SaverScope { override fun canBeSaved(value: Any) = true }

    @Test fun completeOfferIsValidAndRetainsMoneyAndCoordinates() {
        assertTrue(valid.validate(clock = clock).isEmpty())
        assertEquals(450, valid.contributionSen)
        assertEquals(3.12, valid.origin.latitude!!, 0.0)
        assertEquals(Instant.parse("2026-11-18T00:15:00Z"), valid.departure().toInstant())
    }

    @Test fun locationsMustBeNonblankAndDifferent() {
        assertTrue(valid.copy(origin = RideLocation(" ")).validate(clock = clock).contains(RideFormIssue.ORIGIN_REQUIRED))
        assertTrue(valid.copy(destination = RideLocation("")).validate(clock = clock).contains(RideFormIssue.DESTINATION_REQUIRED))
        assertTrue(valid.copy(destination = RideLocation("  KOLEJ 11 ")).validate(clock = clock).contains(RideFormIssue.SAME_LOCATION))
        assertTrue(valid.copy(destination = valid.origin.copy(name = "Another label")).validate(clock = clock).contains(RideFormIssue.SAME_LOCATION))
    }

    @Test fun coordinatesAreOptionalUntilResolvedButCannotBePartialOrInvalid() {
        assertTrue(RideLocation("Manual entry").hasValidCoordinates)
        listOf(RideLocation("A", 3.0), RideLocation("A", 91.0, 101.0),
            RideLocation("A", Double.NaN, 101.0), RideLocation("A", 3.0, Double.POSITIVE_INFINITY)).forEach {
            assertFalse(it.hasValidCoordinates)
            assertTrue(valid.copy(origin = it).validate(clock = clock).contains(RideFormIssue.INVALID_COORDINATES))
        }
    }

    @Test fun editingAResolvedLocationClearsStaleCoordinates() {
        assertEquals(RideLocation("Library"), valid.origin.withName("Library"))
        assertEquals(valid.origin, valid.origin.withName(valid.origin.name))
    }

    @Test fun pastAndExactCurrentDeparturesAreRejectedInCampusTime() {
        val now = valid.copy(departureDate = LocalDate.of(2026, 11, 17), departureTime = LocalTime.of(8, 0))
        assertTrue(now.validate(clock = clock).contains(RideFormIssue.DEPARTURE_IN_PAST))
        assertTrue(now.copy(departureTime = LocalTime.of(7, 59)).validate(clock = clock).contains(RideFormIssue.DEPARTURE_IN_PAST))
        assertFalse(now.copy(departureTime = LocalTime.of(8, 1)).validate(clock = clock).contains(RideFormIssue.DEPARTURE_IN_PAST))
    }

    @Test fun seatsAndContributionRespectInjectedPolicy() {
        val policy = RideFormPolicy(maxSeats = 2, maxContributionSen = 450)
        assertTrue(valid.copy(seats = 2).validate(policy, clock).isEmpty())
        assertTrue(valid.validate(policy, clock).contains(RideFormIssue.INVALID_SEATS))
        assertTrue(valid.copy(seats = 0).validate(clock = clock).contains(RideFormIssue.INVALID_SEATS))
        assertTrue(valid.copy(contributionSen = 501).validate(clock = clock).contains(RideFormIssue.INVALID_CONTRIBUTION))
        assertTrue(valid.copy(contributionSen = -1).validate(clock = clock).contains(RideFormIssue.INVALID_CONTRIBUTION))
        assertFalse(valid.copy(contributionSen = 0).validate(clock = clock).contains(RideFormIssue.INVALID_CONTRIBUTION))
    }

    @Test fun findCriteriaContainsAllSearchFieldsAndValidatesSeats() {
        val criteria = FindRideCriteria(valid.origin, valid.destination, valid.departureDate, valid.departureTime, 2)
        assertTrue(criteria.validate(clock = clock).isEmpty())
        assertTrue(criteria.copy(seats = 5).validate(clock = clock).contains(RideFormIssue.INVALID_SEATS))
        assertEquals(valid.departure(), criteria.departure())
    }

    @Test fun draftsRestoreTypedDateTimeMoneyAndOptionalCoordinates() {
        val saved = with(OfferRideDraftSaver) { scope.save(valid) }
        assertNotNull(saved)
        assertEquals(valid, OfferRideDraftSaver.restore(saved!!))
        val criteria = FindRideCriteria(departureDate = valid.departureDate, departureTime = LocalTime.of(0, 5))
        val savedCriteria = with(FindRideCriteriaSaver) { scope.save(criteria) }
        assertEquals(criteria, FindRideCriteriaSaver.restore(savedCriteria!!))
    }

    @Test fun displayFormattingDoesNotChangeStoredTime() {
        val time = LocalTime.of(0, 5)
        assertEquals("12:05 AM", time.rideLabel(Locale.US))
        assertEquals(LocalTime.of(0, 5), time)
        assertTrue(LocalDate.of(2026, 11, 18).rideLabel(true, Locale.US).contains("\n"))
    }

    @Test fun loadingIsExplicitScreenState() {
        assertTrue(RideFormScreenState(RideSubmissionState.Loading).isLoading)
        assertFalse(RideFormScreenState(RideSubmissionState.Error("Retry")).isLoading)
    }
}
