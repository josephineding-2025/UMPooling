package com.fopgang.umpooling

import com.fopgang.umpooling.booking.BookingRoute
import org.junit.Assert.*
import org.junit.Test

class BookingRoutesTest {
    @Test fun offerAndBookingRoutesRemainDistinct() {
        val details = BookingRoute.decode(BookingRoute.OfferDetails("same-id").encode())
        val review = BookingRoute.decode(BookingRoute.Confirmation("same-id").encode())
        val ticket = BookingRoute.decode(BookingRoute.Ticket("same-id").encode())
        assertTrue(details is BookingRoute.OfferDetails)
        assertTrue(review is BookingRoute.Confirmation)
        assertTrue(ticket is BookingRoute.Ticket)
    }

    @Test fun resourceIdsSurviveSavedRouteEncoding() {
        val id = "offer/one + two?中文"
        assertEquals(id, BookingRoute.decode(BookingRoute.Confirmation(id).encode())?.resourceId)
    }

    @Test fun invalidRoutesCannotOpenABookingScreen() {
        listOf("ticket", "offer_details", "offer_details/", "confirmed_ticket/%ZZ", "chat/id").forEach {
            assertNull(BookingRoute.decode(it))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun emptyBookingIdsAreRejected() {
        BookingRoute.Ticket(" ")
    }
}
