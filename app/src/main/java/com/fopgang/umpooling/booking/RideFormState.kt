package com.fopgang.umpooling.booking

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.fopgang.umpooling.R
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun locationValues(location: RideLocation): List<Any> =
    listOf(location.name, location.latitude?.toString().orEmpty(), location.longitude?.toString().orEmpty())

private fun location(values: List<Any>, index: Int) = RideLocation(
    values[index] as String,
    (values[index + 1] as String).takeIf { it.isNotEmpty() }?.toDouble(),
    (values[index + 2] as String).takeIf { it.isNotEmpty() }?.toDouble(),
)

internal val OfferRideDraftSaver = listSaver<OfferRideDraft, Any>(
    save = { locationValues(it.origin) + locationValues(it.destination) +
        listOf(it.departureDate.toEpochDay(), it.departureTime.toNanoOfDay(), it.seats, it.contributionSen) },
    restore = { OfferRideDraft(location(it, 0), location(it, 3), LocalDate.ofEpochDay(it[6] as Long),
        LocalTime.ofNanoOfDay(it[7] as Long), it[8] as Int, it[9] as Int) },
)
internal val FindRideCriteriaSaver = listSaver<FindRideCriteria, Any>(
    save = { locationValues(it.origin) + locationValues(it.destination) +
        listOf(it.departureDate.toEpochDay(), it.departureTime.toNanoOfDay(), it.seats) },
    restore = { FindRideCriteria(location(it, 0), location(it, 3), LocalDate.ofEpochDay(it[6] as Long),
        LocalTime.ofNanoOfDay(it[7] as Long), it[8] as Int) },
)

internal fun LocalDate.rideLabel(multiline: Boolean = false, locale: Locale = Locale.getDefault()): String =
    format(DateTimeFormatter.ofPattern(if (multiline) "EEE, d\nMMM" else "EEE, d MMM", locale))
internal fun LocalTime.rideLabel(locale: Locale = Locale.getDefault()): String =
    format(DateTimeFormatter.ofPattern("hh:mm a", locale))

@Composable
internal fun RideFormFeedback(issues: List<RideFormIssue>, submission: RideSubmissionState) {
    if (issues.isEmpty() && submission == RideSubmissionState.Idle) return
    Column(Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }) {
        issues.forEach { issue ->
            Text(stringResource(when (issue) {
                RideFormIssue.ORIGIN_REQUIRED -> R.string.ride_origin_required
                RideFormIssue.DESTINATION_REQUIRED -> R.string.ride_destination_required
                RideFormIssue.SAME_LOCATION -> R.string.ride_same_location
                RideFormIssue.INVALID_COORDINATES -> R.string.ride_invalid_coordinates
                RideFormIssue.DEPARTURE_IN_PAST -> R.string.ride_departure_future
                RideFormIssue.INVALID_SEATS -> R.string.ride_invalid_seats
                RideFormIssue.INVALID_CONTRIBUTION -> R.string.ride_invalid_contribution
            }), color = MaterialTheme.colorScheme.error)
        }
        when (submission) {
            RideSubmissionState.Loading -> {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(stringResource(R.string.ride_submitting), Modifier.padding(top = 4.dp))
            }
            is RideSubmissionState.Error -> Text(submission.message, color = MaterialTheme.colorScheme.error)
            RideSubmissionState.Success -> Text(stringResource(R.string.ride_submit_success))
            RideSubmissionState.Idle -> Unit
        }
    }
}
