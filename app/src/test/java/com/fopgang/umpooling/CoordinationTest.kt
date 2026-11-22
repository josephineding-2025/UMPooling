package com.fopgang.umpooling

import com.fopgang.umpooling.coordination.*
import com.fopgang.umpooling.chat.ChatViewModel
import com.fopgang.umpooling.data.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class CoordinationTest {
 private val offer=TripOffer(tripOfferId="offer",driverId="driver",costPerSeat=4.0,availableSeats=2,status="open",departureTime="2026-11-18T08:15:00Z")
 private val trip=TripRecord("trip","offer","driver","in_progress")
 private val booking=BookingRecord("booking","offer","trip","passenger",1,"confirmed")
 @Test fun driverCanStartThenCompleteOrCancel() {
  assertEquals(setOf(TripAction.START),permittedActions("driver",offer,null,null))
  assertEquals(setOf(TripAction.COMPLETE,TripAction.CANCEL),permittedActions("driver",offer,null,trip))
  assertTrue(permittedActions("driver",offer,null,trip.copy(status="completed")).isEmpty())
 }
 @Test fun passengerCanOnlyConfirmTheirActiveTrip() {
  assertEquals(setOf(TripAction.CONFIRM_END),permittedActions("passenger",offer,booking,trip))
  assertTrue(permittedActions("stranger",offer,booking,trip).isEmpty())
  assertTrue(permittedActions("passenger",offer,booking.copy(status="cancelled"),trip).isEmpty())
  assertTrue(permittedActions("passenger",offer,booking.copy(tripId="different"),trip).isEmpty())
  assertTrue(permittedActions("driver",offer,null,trip.copy(offerId="different")).isEmpty())
 }
 @Test(expected=IllegalArgumentException::class) fun malformedConversationRejected() {
  ConversationContext(ConversationKind.BOOKING,"demo-booking")
 }
 @Test fun failedSendRetainsIdForRetry() {
  val scope=CoroutineScope(SupervisorJob()+Dispatchers.Unconfined)
  val ids=mutableListOf<String>()
  val gateway=object:ChatGateway {
   override suspend fun messages(context:ConversationContext)=Result.success(emptyList<Message>())
   override suspend fun send(context:ConversationContext,id:String,text:String):Result<Unit> {
    ids+=id
    return if(ids.size==1) Result.failure(IllegalStateException("offline")) else Result.success(Unit)
   }
  }
  try {
   val vm=ChatViewModel(gateway,ConversationContext(ConversationKind.BOOKING,"550e8400-e29b-41d4-a716-446655440000"),scope)
   vm.send("I am at pickup")
   assertEquals("I am at pickup",vm.state.value.failedText)
   assertEquals(0,vm.state.value.acknowledgement)
   vm.send("I am at pickup")
   assertEquals(ids[0],ids[1])
   assertNull(vm.state.value.failedText)
   assertEquals(1,vm.state.value.acknowledgement)
  } finally {scope.cancel()}
 }
 @Test fun duplicateTapsAreBlockedWhileSending() {
  val scope=CoroutineScope(SupervisorJob()+Dispatchers.Unconfined)
  val pending=CompletableDeferred<Result<Unit>>()
  var sends=0
  val gateway=object:ChatGateway {
   override suspend fun messages(context:ConversationContext)=Result.success(emptyList<Message>())
   override suspend fun send(context:ConversationContext,id:String,text:String):Result<Unit> {sends++;return pending.await()}
  }
  try {
   val vm=ChatViewModel(gateway,ConversationContext(ConversationKind.TRIP,"550e8400-e29b-41d4-a716-446655440000"),scope)
   vm.send("Hello");vm.send("Hello")
   assertEquals(1,sends)
   assertTrue(vm.state.value.sending)
   pending.complete(Result.success(Unit))
   assertFalse(vm.state.value.sending)
  } finally {scope.cancel()}
 }
}
