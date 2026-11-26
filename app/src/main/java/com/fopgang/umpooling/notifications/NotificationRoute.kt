package com.fopgang.umpooling.notifications

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Instant
import java.time.ZoneId

/** Lifecycle-scoped adapter. Presentation has no storage or Supabase dependencies. */
@Composable
fun LiveNotificationScreen(owner: String, back: () -> Unit, navigate: (String) -> Unit, payload: Map<String, String>? = null) {
    val context = LocalContext.current.applicationContext
    val vm: NotificationViewModel = viewModel(key = "notifications/" + owner, factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return NotificationViewModel(NotificationRepository(context, owner)) as T
        }
    })
    val state by vm.state.collectAsState()
    LaunchedEffect(vm) { vm.routes.collect { navigate(it) } }
    LaunchedEffect(payload) {
        payload?.let {
            vm.accept(it)
            if (NotificationLinks.pending.value?.payload == it) NotificationLinks.pending.value = null
        }
    }
    val entries = state.inbox.entries
    val cards = inboxCards(state.inbox)
    NotificationScreen(cards, back, navigate, { item ->
        entries.firstOrNull { notificationIdentity(it) == item.id }?.let(vm::open)
    }, vm::markAllRead, state.error)
}

internal fun inboxCards(inbox: InboxSnapshot, now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): List<NotificationItem> =
    inbox.entries.map { p ->
        val id = notificationIdentity(p)
        val received = p["_received_at"]?.toLongOrNull()?.let(Instant::ofEpochMilli)
        val earlier = received?.atZone(zone)?.toLocalDate()?.isBefore(now.atZone(zone).toLocalDate()) ?: true
        val minutes = received?.let { java.time.Duration.between(it, now).toMinutes().coerceAtLeast(0) }
        val time = when {
            received == null -> ""
            earlier -> received.atZone(zone).format(java.time.format.DateTimeFormatter.ofPattern("d MMM"))
            minutes!! < 1 -> "Now"
            minutes < 60 -> "${minutes}m"
            else -> "${minutes / 60}h"
        }
        val message = p.containsKey("message_id")
        val booking = p.containsKey("booking_id")
        val requestMatch = p.containsKey("request_id")
        val match = requestMatch || (p.containsKey("trip_offer_id") && !booking && !p.containsKey("trip_id"))
        val requestSummary = if (requestMatch) {
            val dep = p["departure_time"] ?: "Soon"
            val dest = p["destination"] ?: "UM Campus"
            val seats = p["available_seats"] ?: "1"
            val co2 = p["estimated_co2_kg"] ?: "1.00"
            "Departs $dep to $dest • $seats seat(s) • ~$co2 kg CO₂ saved"
        } else null
        NotificationItem(id,
            when { message -> "New conversation update"; booking -> "Booking update"; requestMatch -> "Overlapping Ride Request"; match -> "Ride offer update"; p.containsKey("alert_id") -> "Trip safety update"; else -> "Trip update" },
            when { message -> "Open your booking or trip conversation."; booking -> "View the latest status of your booking."; requestSummary != null -> requestSummary; match -> "View the latest ride offer details."; else -> "Open your trip for current details." },
            when { message -> NotificationCategory.Messages; match -> NotificationCategory.Matches; else -> NotificationCategory.Bookings },
            when { message -> NotificationGlyph.Message; match -> NotificationGlyph.Group; else -> NotificationGlyph.Check },
            time = time, earlier = earlier, unread = id !in inbox.read,
            action = when { message -> "Open messages"; booking -> "View booking"; requestMatch -> "View match"; match -> "View offer"; else -> "View trip" })
    }
