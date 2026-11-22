package com.fopgang.umpooling.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fopgang.umpooling.coordination.AccountStorage
import com.fopgang.umpooling.notifications.NotificationPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class AccountUiState(
    val preferences: NotificationPreferences,
    val routes: List<String>,
    val saveFailed: Boolean = false,
)

/** Serialized account-scoped writes survive screen navigation and never outlive logout's graph. */
class AccountViewModel(private val storage: AccountStorage) : ViewModel() {
    private val mutable = MutableStateFlow(AccountUiState(storage.preferences(), storage.routes()))
    val state = mutable.asStateFlow()
    private val writes = Mutex()

    fun savePreferences(preferences: NotificationPreferences) = save {
        storage.savePreferences(preferences)
        mutable.update { it.copy(preferences = preferences) }
    }
    fun saveRoutes(routes: List<String>) = save {
        storage.saveRoutes(routes)
        mutable.update { it.copy(routes = routes) }
    }
    fun dismissError() { mutable.update { it.copy(saveFailed = false) } }
    private fun save(block: suspend () -> Unit) {
        viewModelScope.launch {
            writes.withLock {
                try { block() }
                catch (e: CancellationException) { throw e }
                catch (_: Exception) { mutable.update { it.copy(saveFailed = true) } }
            }
        }
    }
}
