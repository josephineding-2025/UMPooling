package com.fopgang.umpooling.booking

import androidx.compose.runtime.Composable

/** Review an offer. Only a successful reservation may open a ticket with its booking ID. */
@Composable
fun BookingConfirmationScreen(
    offerId: String,
    back: () -> Unit,
    onConfirmBooking: (String) -> Unit,
    onMessageDriver: (String) -> Unit,
    unavailable: () -> Unit,
    requestedSeats: Int = 1,
) {
    RideOfferDetailsContent(offerId, back, onConfirmBooking, onMessageDriver, unavailable, reviewing = true, requestedSeats = requestedSeats)
}
