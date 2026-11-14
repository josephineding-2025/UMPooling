package com.fopgang.umpooling.notifications

import android.util.Log
import com.fopgang.umpooling.BuildConfig
import com.fopgang.umpooling.SupabaseClientProvider
import com.google.firebase.messaging.FirebaseMessaging
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.JsonObject

/** Process-scoped work: token callbacks must outlive individual Compose screens. */
object FcmTokenSync {
    private const val TAG = "FcmTokenSync"
    // Matches UserProfile. This must be the column containing auth.users.id.
    private const val USER_ID_COLUMN = "usersId"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()
    private val configured: Boolean
        get() = BuildConfig.SUPABASE_URL.isNotBlank() &&
            BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    fun start() {
        if (!configured) {
            Log.w(TAG, "Token sync skipped: Supabase local configuration is missing.")
            return
        }
        scope.launch {
            // Covers sign-in, verified sign-up, session restoration and session refresh.
            SupabaseClientProvider.client.auth.sessionStatus.collectLatest { status ->
                if (status is SessionStatus.Authenticated) {
                    syncToken()
                }
            }
        }
    }

    fun onTokenRefreshed(token: String) {
        if (configured) {
            scope.launch { syncToken(token) }
        }
        // If signed out, the next authenticated session uploads Firebase's latest token.
    }

    private suspend fun syncToken(refreshedToken: String? = null) {
        try {
            syncMutex.withLock {
                val client = SupabaseClientProvider.client
                val userId = client.auth.currentUserOrNull()?.id ?: return
                val token = refreshedToken ?: FirebaseMessaging.getInstance().token.await()
                // An account can change while Firebase fetches its token.
                if (client.auth.currentUserOrNull()?.id != userId) return
                val updatedRows = client.postgrest["users"].update(mapOf("fcm_token" to token)) {
                    filter { eq(USER_ID_COLUMN, userId) }
                    select()
                }.decodeList<JsonObject>()
                check(updatedRows.size == 1) {
                    "Expected one user profile: check profile creation, ID column and RLS policies."
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            // Never log tokens or backend error bodies containing personal data.
            Log.w(TAG, "Token sync failed (${error.javaClass.simpleName}); retry on next authenticated session.")
        }
    }
}
