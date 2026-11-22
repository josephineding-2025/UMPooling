package com.fopgang.umpooling.coordination

import android.content.Context
import com.fopgang.umpooling.notifications.NotificationPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

/** Private, account-scoped device persistence. Demo data is isolated from signed-in accounts. */
class AccountStorage(context:Context,account:String) {
 private val prefs=context.applicationContext.getSharedPreferences("account_"+account,Context.MODE_PRIVATE)
 fun routes():List<String> = runCatching {
  val a=JSONArray(prefs.getString("routes","[]"));List(a.length()){a.getString(it)}
 }.getOrDefault(emptyList())
 suspend fun saveRoutes(routes:List<String>)=withContext(Dispatchers.IO) {
  check(prefs.edit().putString("routes",JSONArray(routes).toString()).commit()) {"Routes could not be saved"}
 }
 fun preferences()=NotificationPreferences(
  prefs.getBoolean("push",true),prefs.getBoolean("requests",true),prefs.getBoolean("confirmations",true),
  prefs.getBoolean("grouping",true),prefs.getBoolean("reminders",true),prefs.getBoolean("chat",true),
  prefs.getBoolean("reviews",true),prefs.getBoolean("quiet",false),prefs.getInt("reminderMinutes",30))
 suspend fun savePreferences(p:NotificationPreferences)=withContext(Dispatchers.IO) {
  check(prefs.edit().putBoolean("push",p.push).putBoolean("requests",p.requests)
   .putBoolean("confirmations",p.confirmations).putBoolean("grouping",p.grouping)
   .putBoolean("reminders",p.reminders).putBoolean("chat",p.chat).putBoolean("reviews",p.reviews)
   .putBoolean("quiet",p.quiet).putInt("reminderMinutes",p.reminderMinutes).commit()) {"Settings could not be saved"}
 }
}
