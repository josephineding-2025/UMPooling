package com.fopgang.umpooling.notifications

import android.content.Context
import android.content.SharedPreferences
import com.fopgang.umpooling.coordination.CoordinationRepository
import com.fopgang.umpooling.data.repository.suspendRunCatching
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private val destinationKeys = setOf("booking_id", "trip_id", "trip_offer_id", "message_id", "alert_id")
internal fun notificationIdentity(payload: Map<String, String>): String =
    payload.filterKeys { it in destinationKeys }.toSortedMap().entries.joinToString("|") { "${it.key}=${it.value}" }

data class InboxSnapshot(val entries: List<Map<String, String>> = emptyList(), val read: Set<String> = emptySet())

interface NotificationGateway {
    fun observe(): Flow<InboxSnapshot>
    suspend fun record(payload: Map<String, String>): Result<Unit>
    suspend fun markRead(ids: Set<String>): Result<Unit>
    suspend fun resolve(payload: Map<String, String>): Result<String>
}

/** Account-scoped storage and participant-checked destination resolution. */
class NotificationRepository(context: Context, private val owner: String) : NotificationGateway {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("inbox_" + owner, Context.MODE_PRIVATE)
    private val coordination = CoordinationRepository(owner)
    private val writes = Mutex()
    private fun snapshot() = InboxSnapshot(NotificationInbox.read(app, owner),
        prefs.getStringSet("read", emptySet()).orEmpty().toSet())

    override fun observe(): Flow<InboxSnapshot> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(snapshot()) }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(snapshot())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.flowOn(Dispatchers.IO)

    override suspend fun record(payload: Map<String, String>): Result<Unit> = suspendRunCatching {
        withContext(Dispatchers.IO) { NotificationInbox.record(app, owner, payload) }
    }
    override suspend fun markRead(ids: Set<String>): Result<Unit> = suspendRunCatching {
        writes.withLock {
            withContext(Dispatchers.IO) {
                val updated = prefs.getStringSet("read", emptySet()).orEmpty() + ids
                check(prefs.edit().putStringSet("read", updated).commit()) { "Read status could not be saved" }
            }
        }
    }
    override suspend fun resolve(payload: Map<String, String>) = coordination.resolveNotification(payload)
}
