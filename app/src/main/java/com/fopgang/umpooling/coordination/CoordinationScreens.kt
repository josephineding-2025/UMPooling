package com.fopgang.umpooling.coordination

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fopgang.umpooling.booking.*
import com.fopgang.umpooling.chat.*
import com.fopgang.umpooling.safety.LiveSosPanel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun LiveTripsScreen(owner:String,back:()->Unit,navigate:(String)->Unit,bookingId:String?=null,tripId:String?=null,sos:Boolean=false,offerId:String?=null,sosPrepared:Boolean=false) {
 val repo=remember(owner){CoordinationRepository(owner)}
 val vm:CoordinationViewModel=viewModel(factory=object:ViewModelProvider.Factory {
  override fun <T:ViewModel> create(modelClass:Class<T>):T { @Suppress("UNCHECKED_CAST") return CoordinationViewModel(repo) as T }
 })
 val state by vm.state.collectAsState()
 val lifecycle=LocalLifecycleOwner.current.lifecycle
 LaunchedEffect(lifecycle,owner,bookingId,tripId,offerId) {lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){while(true){vm.refresh();delay(10000)}}}
 val data=state.data
 if(bookingId==null && tripId==null && offerId==null) {
  MyTripsScreen(data?.myTripItems(owner).orEmpty(),navigate,loading=state.loading,error=state.error,refresh=vm::refresh)
  return
 }
 if(bookingId!=null && data!=null && !sos) {
  val booking=data.bookings.firstOrNull {it.id==bookingId}
  val offer=data.offers.firstOrNull {it.tripOfferId==booking?.offerId}
  val trip=data.trips.firstOrNull {it.id==booking?.tripId}
  if(booking!=null && offer!=null) {
   val active=trip?.status=="in_progress" && booking.status=="confirmed"
   ConfirmedTripTicketScreen(booking.id,back,
    {navigate(if(trip!=null) "chat/trip/${trip.id}" else "chat/booking/${booking.id}")},
    {if(active) navigate("sos_trip/${trip!!.id}")},reservedSeats=booking.seats,
    details=TicketDetails(
     status=trip?.status ?: booking.status,
     departure=tripDeparture(offer.departureTime),
     pickup=offer.originZone ?: "Pickup unavailable",destination=offer.destinationZone ?: "Destination unavailable",
     driverName="Driver details unavailable",vehicle=trip?.plate ?: "Vehicle details unavailable",
     passengerName=if(booking.passengerId==owner) "You" else "Passenger",
     costPerSeat=offer.costPerSeat,canSos=active),
    onTripControls=trip?.let {{navigate("trip/${it.id}")}},loading=state.loading,error=state.error)
   return
  }
 }
 RidePage {
  Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
   RideHeader("Trip Details",back)
   if(state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
   state.error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
   RideAction("Refresh",enabled=!state.loading && !state.busy,action=vm::refresh)
   if(data!=null) {
    val bookings=data.bookings.filter {bookingId==null || it.id==bookingId}
    if(bookingId!=null && bookings.isEmpty()) Text("Booking not found or access denied.")
     val booking=if(bookingId!=null) bookings.firstOrNull() else data.bookings.firstOrNull {it.tripId==tripId && it.passengerId==owner}
     val trip=data.trips.firstOrNull {it.id==(tripId ?: booking?.tripId) || (offerId!=null && it.offerId==offerId)}
     val offer=data.offers.firstOrNull {it.tripOfferId==(trip?.offerId ?: booking?.offerId ?: offerId)}
     if(offer==null) Text("Trip context unavailable. Refresh or return to My Trips.")
     else {
      Glass {
       Text("${offer.originZone ?: "Pickup"} → ${offer.destinationZone ?: "Destination"}")
       Text("Departure: ${offer.departureTime}")
       Text("Status: ${trip?.status ?: booking?.status ?: offer.status}")
       booking?.let {Text("${it.seats} seat(s) · RM ${offer.costPerSeat} per seat · Total RM ${offer.costPerSeat*it.seats}");Text("Booking: ${it.id}")}
       trip?.let {Text("Trip: ${it.id}");Text("Vehicle: ${it.plate ?: "Not available"}")}
      }
      permittedActions(owner,offer,booking,trip).forEach {action->
       RideAction(when(action){TripAction.START->"Start trip";TripAction.CONFIRM_END->"Confirm trip ended";TripAction.COMPLETE->"Finalize completed trip";TripAction.CANCEL->"Cancel active trip"},
        enabled=!state.busy && !state.loading) {
         vm.act(action,if(action==TripAction.START) offer.tripOfferId!! else trip!!.id)
        }
      }
      if(trip!=null) RideAction("Trip conversation"){navigate("chat/trip/${trip.id}")}
      else booking?.let {RideAction("Message driver"){navigate("chat/booking/${it.id}")}}
      if(trip?.status=="in_progress" && (trip.driverId==owner || booking?.passengerId==owner)) {
       if(sos) LiveSosPanel(repo,trip,offer,back,prepared=sosPrepared)
       else RideAction("SOS"){navigate("sos_trip/${trip.id}")}
      } else if(sos) Text("SOS is available only during an active trip.")
     }
   }
  }
 }
}

@Composable
fun LiveChatScreen(owner:String,kind:ConversationKind,id:String,back:()->Unit) {
 val context=remember(kind,id){runCatching{ConversationContext(kind,id)}.getOrNull()}
 if(context==null) {FlowDestination("Conversation",back,"Invalid conversation ID.",emptyList());return}
 val vm:ChatViewModel=viewModel(factory=object:ViewModelProvider.Factory {
  override fun <T:ViewModel> create(modelClass:Class<T>):T {@Suppress("UNCHECKED_CAST") return ChatViewModel(CoordinationRepository(owner),context) as T}
 })
 val state by vm.state.collectAsState()
 val lifecycle=LocalLifecycleOwner.current.lifecycle
 LaunchedEffect(lifecycle,context){lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){while(true){vm.refresh();delay(5000)}}}
 Column {
  state.error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
  if(state.failedText!=null) Text("Your unsent message is retained.")
  ChatScreen(back,{},conversationId=kind.name+"/"+id,
   liveMessages=state.messages.map{ChatMessage(it.content,it.sentAt.orEmpty(),it.senderId==owner)},
   sending=state.sending,sendAcknowledgement=state.acknowledgement,onSendMessage=vm::send)
 }
}
