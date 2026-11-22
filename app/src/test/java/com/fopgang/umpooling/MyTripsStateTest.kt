package com.fopgang.umpooling

import com.fopgang.umpooling.coordination.*
import com.fopgang.umpooling.booking.*
import com.fopgang.umpooling.data.TripOffer
import org.junit.Assert.*
import org.junit.Test

class MyTripsStateTest {
    private val offer=TripOffer("offer","driver",4.0,2,"full","2026-11-18T00:15:00Z","College","Faculty")
    private val booking=BookingRecord("booking","offer","trip","passenger",2,"confirmed")
    private val trip=TripRecord("trip","offer","driver","in_progress","WXY 1234")

    @Test fun activeBookingKeepsTripAndChatContext() {
        val item=CoordinationData(listOf(booking),listOf(offer),listOf(trip)).myTripItems("passenger").single()
        assertEquals(MyTripStatus.Active,item.status)
        assertEquals("sos_trip/trip",item.sosRoute)
        assertEquals("chat/trip/trip",item.chatRoute)
        assertEquals("confirmed_ticket/booking",item.detailRoute)
        assertEquals("WXY 1234",item.vehicle)
    }
    @Test fun cancelledBookingNeverExposesActiveSos() {
        val item=CoordinationData(listOf(booking.copy(status="cancelled")),listOf(offer),listOf(trip))
            .myTripItems("passenger").single()
        assertEquals(MyTripStatus.Cancelled,item.status)
        assertNull(item.sosRoute)
    }
    @Test fun driverOfferUsesOfferIdAndDoesNotClaimPassengerBooking() {
        val item=CoordinationData(emptyList(),listOf(offer),emptyList()).myTripItems("driver").single()
        assertEquals("driver_offer/offer",item.detailRoute)
        assertEquals("Your ride offer",item.statusLabel)
        assertEquals(MyTripStatus.Pending,item.status)
        assertNull(item.chatRoute)
        assertNull(item.sosRoute)
    }
    @Test fun missingOfferDoesNotFabricateRouteOrPrice() {
        val item=CoordinationData(listOf(booking.copy(tripId=null)),emptyList(),emptyList())
            .myTripItems("passenger").single()
        assertEquals(MyTripStatus.Confirmed,item.status)
        assertEquals("Pickup unavailable",item.origin)
        assertNull(item.costPerSeat)
        assertEquals("chat/booking/booking",item.chatRoute)
    }
}
