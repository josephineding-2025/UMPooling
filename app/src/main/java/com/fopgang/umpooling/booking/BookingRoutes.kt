package com.fopgang.umpooling.booking

import java.net.URLDecoder
import java.net.URLEncoder

/** Explicit fixtures for the UI demo; these are not database identifiers. */
object DemoRideIds {
    const val HOME_OFFER = "demo-offer-mid-valley"
    const val PICKUP_GROUP = "demo-offer-kolej-11"
    const val OTHER_GROUP = "demo-offer-kolej-3"
    const val CONFIRMED_BOOKING = "UMP-2841"
}

/** The resource type belongs to the route, so an offer cannot be opened as a ticket. */
sealed class BookingRoute(val resourceId: String, private val destination: String) {
    class OfferDetails(val offerId: String) : BookingRoute(offerId, "offer_details")
    class Confirmation(val offerId: String) : BookingRoute(offerId, "booking_confirmation")
    class Ticket(val bookingId: String) : BookingRoute(bookingId, "confirmed_ticket")

    init { require(resourceId.isNotBlank()) { "A route requires a resource ID" } }

    fun encode(): String = destination + "/" + URLEncoder.encode(resourceId, "UTF-8")

    companion object {
        fun decode(route: String): BookingRoute? {
            val destination = route.substringBefore('/')
            if (destination !in setOf("offer_details", "booking_confirmation", "confirmed_ticket")) return null
            if ('/' !in route) return null
            val id = try { URLDecoder.decode(route.substringAfter('/'), "UTF-8") } catch (_: IllegalArgumentException) { return null }
            if (id.isBlank()) return null
            return when (destination) {
                "offer_details" -> OfferDetails(id)
                "booking_confirmation" -> Confirmation(id)
                else -> Ticket(id)
            }
        }
    }
}
