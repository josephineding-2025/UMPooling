package com.fopgang.umpooling.booking

import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** A typed location can be entered by name before a place picker resolves its coordinates. */
data class RideLocation(val name: String, val latitude: Double? = null, val longitude: Double? = null) {
    fun withName(name: String) = if (name == this.name) this else RideLocation(name)
    val hasValidCoordinates: Boolean
        get() = (latitude == null && longitude == null) ||
            (latitude != null && longitude != null && latitude.isFinite() && longitude.isFinite() &&
                latitude in -90.0..90.0 && longitude in -180.0..180.0)
}

val CAMPUS_ZONE: ZoneId = ZoneId.of("Asia/Kuala_Lumpur")

data class OfferRideDraft(
    val origin: RideLocation = RideLocation("Kolej Kediaman 11"),
    val destination: RideLocation = RideLocation("Faculty of Engineering"),
    val departureDate: LocalDate = LocalDate.now(CAMPUS_ZONE).plusDays(1),
    val departureTime: LocalTime = LocalTime.of(8, 15),
    val seats: Int = 3,
    val contributionSen: Int = 400,
) {
    fun departure(): ZonedDateTime = departureDate.atTime(departureTime).atZone(CAMPUS_ZONE)
}

data class FindRideCriteria(
    val origin: RideLocation = RideLocation("Kolej Kediaman 11"),
    val destination: RideLocation = RideLocation("Faculty of Engineering"),
    val departureDate: LocalDate = LocalDate.now(CAMPUS_ZONE).plusDays(1),
    val departureTime: LocalTime = LocalTime.of(8, 15),
    val seats: Int = 2,
) {
    fun departure(): ZonedDateTime = departureDate.atTime(departureTime).atZone(CAMPUS_ZONE)
}

/** Inject the server's cap when app_config is connected. Amounts are integer sen. */
data class RideFormPolicy(val maxSeats: Int = 4, val maxContributionSen: Int = 500) {
    init { require(maxSeats > 0); require(maxContributionSen >= 0) }
}

enum class RideFormIssue { ORIGIN_REQUIRED, DESTINATION_REQUIRED, SAME_LOCATION, INVALID_COORDINATES, DEPARTURE_IN_PAST, INVALID_SEATS, INVALID_CONTRIBUTION }

sealed interface RideSubmissionState {
    data object Idle : RideSubmissionState
    data object Loading : RideSubmissionState
    data class Error(val message: String) : RideSubmissionState
    data object Success : RideSubmissionState
}

data class RideFormScreenState(
    val submission: RideSubmissionState = RideSubmissionState.Idle,
    val policy: RideFormPolicy = RideFormPolicy(),
) {
    val isLoading: Boolean get() = submission == RideSubmissionState.Loading
}

private fun validateRide(origin: RideLocation, destination: RideLocation, departure: ZonedDateTime, seats: Int, policy: RideFormPolicy, clock: Clock): List<RideFormIssue> = buildList {
    if (origin.name.isBlank()) add(RideFormIssue.ORIGIN_REQUIRED)
    if (destination.name.isBlank()) add(RideFormIssue.DESTINATION_REQUIRED)
    if (origin.name.isNotBlank() && destination.name.isNotBlank() &&
        (origin.name.trim().equals(destination.name.trim(), ignoreCase = true) ||
            (origin.latitude != null && origin.longitude != null &&
                origin.latitude == destination.latitude && origin.longitude == destination.longitude))) add(RideFormIssue.SAME_LOCATION)
    if (!origin.hasValidCoordinates || !destination.hasValidCoordinates) add(RideFormIssue.INVALID_COORDINATES)
    if (!departure.toInstant().isAfter(clock.instant())) add(RideFormIssue.DEPARTURE_IN_PAST)
    if (seats !in 1..policy.maxSeats) add(RideFormIssue.INVALID_SEATS)
}

fun OfferRideDraft.validate(policy: RideFormPolicy = RideFormPolicy(), clock: Clock = Clock.system(CAMPUS_ZONE)): List<RideFormIssue> =
    validateRide(origin, destination, departure(), seats, policy, clock) +
        if (contributionSen !in 0..policy.maxContributionSen) listOf(RideFormIssue.INVALID_CONTRIBUTION) else emptyList()

fun FindRideCriteria.validate(policy: RideFormPolicy = RideFormPolicy(), clock: Clock = Clock.system(CAMPUS_ZONE)): List<RideFormIssue> =
    validateRide(origin, destination, departure(), seats, policy.copy(maxSeats = minOf(3, policy.maxSeats)), clock)
