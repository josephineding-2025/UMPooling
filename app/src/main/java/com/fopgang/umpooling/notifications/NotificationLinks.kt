package com.fopgang.umpooling.notifications

import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID

object NotificationLinks {
 data class Link(val key:String=UUID.randomUUID().toString(),val payload:Map<String,String>)
 val pending=MutableStateFlow<Link?>(null)
 private val keys=setOf("booking_id","trip_id","trip_offer_id","message_id","alert_id")
 fun accept(intent:Intent?) {
  val payload=keys.mapNotNull {key->intent?.getStringExtra(key)?.let{key to it}}.toMap()
  if(payload.isNotEmpty()) pending.value=Link(payload=payload)
 }
}
