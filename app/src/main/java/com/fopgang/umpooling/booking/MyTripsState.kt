package com.fopgang.umpooling.booking

import com.fopgang.umpooling.R
import com.fopgang.umpooling.coordination.CoordinationData

import com.fopgang.umpooling.booking.CAMPUS_ZONE
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun tripDeparture(value: String, locale: Locale = Locale.getDefault()): String = runCatching {
    OffsetDateTime.parse(value).atZoneSameInstant(CAMPUS_ZONE)
        .format(DateTimeFormatter.ofPattern("EEE h:mm a", locale))
}.getOrDefault(value)

/** Never infer an active reservation from a cancelled booking or fabricate missing profile data. */
internal fun CoordinationData.myTripItems(owner: String, locale: Locale = Locale.getDefault()): List<MyTripItem> {
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
            origin=offer?.originZone,destination=offer?.destinationZone,
            departure=offer?.departureTime?.let { tripDeparture(it, locale) }.orEmpty(),reference=booking.id.take(8).uppercase(),
            vehicle=trip?.plate.orEmpty(),seats=booking.seats,costPerSeat=offer?.costPerSeat,
            statusLabelRes=if(state==MyTripStatus.Pending) R.string.trips_pending_booking else null,
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
            id="offer/"+id,status=state,origin=offer.originZone,
            destination=offer.destinationZone,
            departure=tripDeparture(offer.departureTime, locale),reference=id.take(8).uppercase(),
            vehicle=trip?.plate.orEmpty(),statusLabelRes=if(state==MyTripStatus.Pending) R.string.trips_your_offer else R.string.trips_your_trip,
            availableSeats=offer.availableSeats,costPerSeat=offer.costPerSeat,
            detailRoute="driver_offer/"+id,chatRoute=trip?.let {"chat/trip/"+it.id},
            sosRoute=trip?.takeIf {state==MyTripStatus.Active}?.let {"sos_trip/"+it.id},
        )
    }
    return passenger+driver
}
