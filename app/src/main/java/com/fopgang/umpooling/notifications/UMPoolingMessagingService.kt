package com.fopgang.umpooling.notifications

import android.app.*
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.fopgang.umpooling.SupabaseClientProvider
import com.fopgang.umpooling.coordination.AccountStorage
import io.github.jan.supabase.auth.auth

class UMPoolingMessagingService : FirebaseMessagingService() {
 override fun onNewToken(token:String) {super.onNewToken(token);FcmTokenSync.onTokenRefreshed(token)}
 override fun onMessageReceived(message:RemoteMessage) {
  val owner=runCatching{SupabaseClientProvider.client.auth.currentUserOrNull()?.id}.getOrNull() ?: return
  val payload=message.data.filterKeys {it in setOf("booking_id","trip_id","trip_offer_id","message_id","alert_id","request_id")}
  if(payload.isEmpty()) return
  runCatching {NotificationInbox.record(this,owner,payload)}
  val settings=AccountStorage(this,owner).preferences()
  if(!settings.push || (payload.containsKey("message_id") && !settings.chat)) return
  if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(this,android.Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) return
  val manager=getSystemService(NotificationManager::class.java)
  if(Build.VERSION.SDK_INT>=26) manager.createNotificationChannel(NotificationChannel("coordination","Trip coordination",NotificationManager.IMPORTANCE_DEFAULT))
  val intent=Intent(this,com.fopgang.umpooling.MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
  payload.forEach { (key,value)->intent.putExtra(key,value) }
  val pending=PendingIntent.getActivity(this,payload.hashCode(),intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  manager.notify(payload.hashCode(),NotificationCompat.Builder(this,"coordination")
   .setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("UMPooling update")
   .setContentText("Open to view your booking or trip.").setAutoCancel(true).setContentIntent(pending).build())
 }
}
