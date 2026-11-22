package com.fopgang.umpooling.notifications

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Store identifiers, not private lock-screen message bodies. RLS is checked when opening. */
object NotificationInbox {
 @Synchronized fun read(context:Context,owner:String):List<Map<String,String>> = runCatching {
  val a=JSONArray(context.getSharedPreferences("inbox_"+owner,0).getString("links","[]"))
  List(a.length()){i->val o=a.getJSONObject(i);o.keys().asSequence().associateWith{o.getString(it)}}
 }.getOrDefault(emptyList())
 @Synchronized fun record(context:Context,owner:String,payload:Map<String,String>) {
  val identity=notificationIdentity(payload)
  val existing=read(context,owner)
  val received=existing.firstOrNull {notificationIdentity(it)==identity}?.get("_received_at")
  val row=payload + ("_received_at" to (received ?: System.currentTimeMillis().toString()))
  val rows=(listOf(row)+existing.filterNot {notificationIdentity(it)==identity}).take(100)
  check(context.getSharedPreferences("inbox_"+owner,0).edit().putString("links",JSONArray(rows.map{JSONObject(it)}).toString()).commit())
 }
}
