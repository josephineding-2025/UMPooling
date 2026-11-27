package com.fopgang.umpooling.booking

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.fopgang.umpooling.ui.theme.UMPoolingTheme

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

@Preview
@Composable
private fun BookingConfirmationPreview() {
    UMPoolingTheme {
        BookingConfirmationScreen(
            offerId = DemoRideIds.HOME_OFFER,
            back = {},
            onConfirmBooking = {},
            onMessageDriver = {},
            unavailable = {}
        )
    }
}

