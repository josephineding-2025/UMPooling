package com.fopgang.umpooling.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

data class NotificationState(
    val inbox: InboxSnapshot = InboxSnapshot(),
    val busy: Boolean = false,
    val error: String? = null,
)

/** Owns inbox operations; route effects are consumed once rather than replayed on rotation. */
class NotificationViewModel(
    private val repository: NotificationGateway,
    scopeOverride: CoroutineScope? = null,
) : ViewModel() {
    private val scope = scopeOverride ?: viewModelScope
    private val mutable = MutableStateFlow(NotificationState())
    val state = mutable.asStateFlow()
    private val destinations = Channel<String>(Channel.BUFFERED)
    val routes = destinations.receiveAsFlow()
    private val handled = mutableSetOf<Map<String, String>>()

    init {
        repository.observe().onEach { inbox -> mutable.update { it.copy(inbox = inbox) } }
            .catch { mutable.update { it.copy(error = "Unable to load notifications.") } }
            .launchIn(scope)
    }
    fun accept(payload: Map<String, String>) {
        if (!handled.add(payload.toMap())) return
        scope.launch {
            repository.record(payload).onFailure {
                mutable.update { it.copy(error = "Unable to save notification on this device.") }
            }
            open(payload)
        }
    }
    fun open(payload: Map<String, String>) {
        if (mutable.value.busy) return
        mutable.update { it.copy(busy = true, error = null) }
        scope.launch {
            try {
                repository.resolve(payload).fold({ route ->
                    repository.markRead(setOf(notificationIdentity(payload))).onFailure {
                        mutable.update { it.copy(error = "Unable to save read status.") }
                    }
                    destinations.send(route)
                }, {
                    mutable.update { it.copy(error = "Notification unavailable or access denied.") }
                })
            } finally {
                mutable.update { it.copy(busy = false) }
            }
        }
    }
    fun markAllRead() {
        val ids = state.value.inbox.entries.map(::notificationIdentity).toSet()
        scope.launch {
            repository.markRead(ids).onFailure {
                mutable.update { it.copy(error = "Unable to save read status.") }
            }
        }
    }
}
