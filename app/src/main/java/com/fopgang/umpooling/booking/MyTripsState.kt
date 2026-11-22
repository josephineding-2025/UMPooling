package com.fopgang.umpooling.booking

import com.fopgang.umpooling.coordination.CoordinationData

import com.fopgang.umpooling.booking.CAMPUS_ZONE
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun tripDeparture(value:String):String = runCatching {
    OffsetDateTime.parse(value).atZoneSameInstant(CAMPUS_ZONE)
        .format(DateTimeFormatter.ofPattern("EEE h:mm a",Locale.getDefault()))
}.getOrDefault(value)

/** Never infer an active reservation from a cancelled booking or fabricate missing profile data. */
internal fun CoordinationData.myTripItems(owner:String):List<MyTripItem> {
    fun status(booking:String?,trip:String?,offer:String?) = when {
        booking=="cancelled" || trip=="cancelled" || offer=="cancelled" -> MyTripStatus.Cancelled
        trip=="in_progress" -> MyTripStatus.Active
        trip=="completed" || offer=="completed" -> MyTripStatus.Completed
        booking=="confirmed" -> MyTripStatus.Confirmed
        else -> MyTripStatus.Pending
    }
    val passenger=bookings.filter {it.passengerId==owner}.map {booking->
        val offer=offers.firstOrNull {it.tripOfferId==booking.offerId}
        val trip=trips.firstOrNull {it.id==booking.tripId}
        val state=status(booking.status,trip?.status,offer?.status)
        MyTripItem(
            id="booking/"+booking.id,status=state,
            origin=offer?.originZone ?: "Pickup unavailable",destination=offer?.destinationZone ?: "Destination unavailable",
            departure=offer?.departureTime?.let(::tripDeparture).orEmpty(),reference=booking.id.take(8).uppercase(),
            vehicle=trip?.plate.orEmpty(),seats=booking.seats,costPerSeat=offer?.costPerSeat,
            statusLabel=if(state==MyTripStatus.Pending) "Pending booking" else null,
            summary="Awaiting booking confirmation",
            detailRoute="confirmed_ticket/"+booking.id,
            chatRoute=if(trip!=null) "chat/trip/"+trip.id else "chat/booking/"+booking.id,
            sosRoute=trip?.takeIf {state==MyTripStatus.Active}?.let {"sos_trip/"+it.id},
        )
    }
    val driver=offers.filter {it.driverId==owner}.mapNotNull {offer->
        val id=offer.tripOfferId ?: return@mapNotNull null
        val trip=trips.firstOrNull {it.offerId==id}
        val state=status(null,trip?.status,offer.status)
        MyTripItem(
            id="offer/"+id,status=state,origin=offer.originZone ?: "Pickup unavailable",
            destination=offer.destinationZone ?: "Destination unavailable",
            departure=tripDeparture(offer.departureTime),reference=id.take(8).uppercase(),
            vehicle=trip?.plate.orEmpty(),statusLabel=if(state==MyTripStatus.Pending) "Your ride offer" else "Your trip",
            summary="${offer.availableSeats} seats available · RM${money(offer.costPerSeat)} per seat",
            detailRoute="driver_offer/"+id,chatRoute=trip?.let {"chat/trip/"+it.id},
            sosRoute=trip?.takeIf {state==MyTripStatus.Active}?.let {"sos_trip/"+it.id},
        )
    }
    return passenger+driver
}
