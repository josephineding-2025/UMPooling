package com.fopgang.umpooling.coordination

import com.fopgang.umpooling.SupabaseClientProvider
import com.fopgang.umpooling.data.*
import com.fopgang.umpooling.data.repository.*
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.*
import java.util.UUID

@Serializable data class BookingRecord(
 @SerialName("booking_id") val id:String,
 @SerialName("trip_offer_id") val offerId:String,
 @SerialName("trip_id") val tripId:String?=null,
 @SerialName("passenger_id") val passengerId:String,
 @SerialName("seats_booked") val seats:Int,
 val status:String,
 @SerialName("booking_reference") val reference:String?=null,
 @SerialName("cost_per_seat_snapshot") val costSnapshot:Double?=null,
 @SerialName("origin_zone_snapshot") val originSnapshot:String?=null,
 @SerialName("destination_zone_snapshot") val destinationSnapshot:String?=null,
 @SerialName("departure_time_snapshot") val departureSnapshot:String?=null,
)
@Serializable data class TripRecord(
 @SerialName("trip_id") val id:String,
 @SerialName("trip_offer_id") val offerId:String,
 @SerialName("driver_id") val driverId:String,
 val status:String,
 @SerialName("vehicle_plate_snapshot") val plate:String?=null,
 @SerialName("distance_km") val distanceKm:Double?=null,
 @SerialName("confirmed_passenger_count") val passengerCount:Int?=null,
 @SerialName("co2_saved_kg") val co2SavedKg:Double?=null,
)
data class CoordinationData(val bookings:List<BookingRecord>,val offers:List<TripOffer>,val trips:List<TripRecord>)
enum class ConversationKind(val column:String) { BOOKING("booking_id"), TRIP("trip_id") }
data class ConversationContext(val kind:ConversationKind,val id:String) {
 init { require(UUID.fromString(id).toString()==id.lowercase()) }
}
enum class TripAction { START, CONFIRM_END, COMPLETE, CANCEL }
fun permittedActions(userId:String,offer:TripOffer,booking:BookingRecord?,trip:TripRecord?):Set<TripAction> = buildSet {
 if(trip!=null && (trip.offerId!=offer.tripOfferId || trip.driverId!=offer.driverId)) return@buildSet
 if(booking!=null && (booking.offerId!=offer.tripOfferId || (trip!=null && booking.tripId!=trip.id))) return@buildSet
 if(offer.driverId==userId) {
  if(trip==null && offer.status in setOf(OfferStatus.OPEN,OfferStatus.FULL)) add(TripAction.START)
  if(trip?.status==TripStatus.IN_PROGRESS) {add(TripAction.COMPLETE);add(TripAction.CANCEL)}
 } else if(booking?.passengerId==userId && booking.status=="confirmed" && trip?.status==TripStatus.IN_PROGRESS) add(TripAction.CONFIRM_END)
}
@Serializable private data class OutgoingMessage(
 @SerialName("message_id") val id:String,
 @SerialName("sender_id") val sender:String,
 val content:String,
 @SerialName("booking_id") val booking:String?=null,
 @SerialName("trip_id") val trip:String?=null,
)
interface ChatGateway {
 suspend fun messages(context:ConversationContext):Result<List<Message>>
 suspend fun send(context:ConversationContext,id:String,text:String):Result<Unit>
}
class CoordinationRepository(val owner:String):ChatGateway {
 private val client get()=SupabaseClientProvider.client
 fun checkOwner() { check(client.auth.currentUserOrNull()?.id==owner) {"Session changed. Sign in again."} }
 suspend fun load():Result<CoordinationData> = suspendRunCatching {
  checkOwner()
  val bookings=client.postgrest["bookings"].select().decodeList<BookingRecord>()
  val trips=client.postgrest["trips"].select().decodeList<TripRecord>()
  val offers=mutableMapOf<String,TripOffer>()
  client.postgrest["trip_offers"].select { filter {eq("driver_id",owner)} }.decodeList<TripOffer>()
   .forEach { offers[it.tripOfferId!!]=it }
  (bookings.map {it.offerId}+trips.map {it.offerId}).distinct().forEach { id->
   if(id !in offers) client.postgrest["trip_offers"].select {filter {eq("trip_offer_id",id)}}.decodeList<TripOffer>().firstOrNull()?.let {offers[id]=it}
  }
  CoordinationData(bookings,offers.values.toList(),trips)
 }
 suspend fun cancelBooking(id:String):Result<Unit> = suspendRunCatching {
  checkOwner()
  RideRepository().cancelBooking(id).getOrThrow()
  Unit
 }
 suspend fun act(action:TripAction,id:String):Result<Unit> = suspendRunCatching {
  checkOwner()
  UUID.fromString(id)
  val repository=TripRepository()
  when(action) {
   TripAction.START->repository.startTrip(id).getOrThrow()
   TripAction.CONFIRM_END->repository.confirmTripEnd(id).getOrThrow()
   TripAction.COMPLETE->repository.completeTrip(id).getOrThrow()
   TripAction.CANCEL->repository.cancelTrip(id).getOrThrow()
  }
  Unit
 }
 private suspend fun checkConversation(context:ConversationContext) {
  checkOwner()
  when(context.kind) {
   ConversationKind.BOOKING -> client.postgrest["bookings"].select {filter {eq("booking_id",context.id)}}.decodeSingle<BookingRecord>()
   ConversationKind.TRIP -> client.postgrest["trips"].select {filter {eq("trip_id",context.id)}}.decodeSingle<TripRecord>()
  }
 }
 override suspend fun messages(context:ConversationContext):Result<List<Message>> = suspendRunCatching {
  checkConversation(context)
  client.postgrest["messages"].select {filter {eq(context.kind.column,context.id)}}.decodeList<Message>().sortedBy {it.sentAt}
 }
 override suspend fun send(context:ConversationContext,id:String,text:String):Result<Unit> = suspendRunCatching {
  checkConversation(context); UUID.fromString(id); require(text.isNotBlank() && text.length<=4000)
  // Retry the same ID. A timed-out successful insert must not create a second message.
  val existing=client.postgrest["messages"].select {filter {eq("message_id",id)}}.decodeList<Message>()
  existing.firstOrNull()?.let { require(it.senderId==owner && it.content==text &&
   it.bookingId==context.id.takeIf {context.kind==ConversationKind.BOOKING} &&
   it.tripId==context.id.takeIf {context.kind==ConversationKind.TRIP}) }
  if(existing.isEmpty()) client.postgrest["messages"].insert(OutgoingMessage(id,owner,text,
   booking=context.id.takeIf {context.kind==ConversationKind.BOOKING},
   trip=context.id.takeIf {context.kind==ConversationKind.TRIP}))
  Unit
 }
 private suspend fun resolveMessage(id:String):String {
  UUID.fromString(id)
  val m=client.postgrest["messages"].select {filter {eq("message_id",id)}}.decodeSingle<Message>()
  return if(m.bookingId!=null) "chat/booking/${m.bookingId}" else "chat/trip/${m.tripId}"
 }
 suspend fun resolveNotification(payload:Map<String,String>):Result<String> = suspendRunCatching {
  checkOwner()
  payload["request_id"]?.let { id ->
   UUID.fromString(id)
   client.postgrest["ride_requests"].select {filter {eq("request_id",id)}}.decodeSingle<RideRequest>()
   return@suspendRunCatching "offer_ride"
  }
  if(payload.containsKey("message_id")) return@suspendRunCatching resolveMessage(payload.getValue("message_id"))
  payload["booking_id"]?.let { id->UUID.fromString(id);client.postgrest["bookings"].select {filter {eq("booking_id",id)}}.decodeSingle<BookingRecord>();return@suspendRunCatching "confirmed_ticket/$id" }
  payload["trip_id"]?.let { id->UUID.fromString(id);client.postgrest["trips"].select {filter {eq("trip_id",id)}}.decodeSingle<TripRecord>();return@suspendRunCatching "trip/$id" }
  payload["trip_offer_id"]?.let { id->UUID.fromString(id);client.postgrest["trip_offers"].select {filter {eq("trip_offer_id",id)}}.decodeSingle<TripOffer>();return@suspendRunCatching "driver_offer/$id" }
  payload["message_id"]?.let { id->
   UUID.fromString(id)
   val m=client.postgrest["messages"].select {filter {eq("message_id",id)}}.decodeSingle<Message>()
   return@suspendRunCatching if(m.bookingId!=null) "chat/booking/${m.bookingId}" else "chat/trip/${m.tripId}"
  }
  payload["alert_id"]?.let { id->
   UUID.fromString(id)
   val row=client.postgrest["sos_alerts"].select {filter {eq("alert_id",id)}}.decodeSingle<kotlinx.serialization.json.JsonObject>()
   val trip=(row["trip_id"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: error("Trip unavailable")
   UUID.fromString(trip)
   return@suspendRunCatching "trip/$trip"
  }
  error("Notification has no supported destination")
 }
}
