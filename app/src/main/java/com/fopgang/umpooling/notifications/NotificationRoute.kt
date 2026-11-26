package com.fopgang.umpooling.notifications

import android.content.res.Resources
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.fopgang.umpooling.R
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
    val locale = LocalConfiguration.current.locales[0]
    val cards = inboxCards(state.inbox, LocalContext.current.resources, locale)
    NotificationScreen(cards, back, navigate, { item ->
        entries.firstOrNull { notificationIdentity(it) == item.id }?.let(vm::open)
    }, vm::markAllRead, state.error?.let { stringResource(it) })
}

internal fun inboxCards(inbox: InboxSnapshot, resources: Resources, locale: java.util.Locale, now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): List<NotificationItem> =
    inbox.entries.map { p ->
        val id = notificationIdentity(p)
        val received = p["_received_at"]?.toLongOrNull()?.let(Instant::ofEpochMilli)
        val earlier = received?.atZone(zone)?.toLocalDate()?.isBefore(now.atZone(zone).toLocalDate()) ?: true
        val minutes = received?.let { java.time.Duration.between(it, now).toMinutes().coerceAtLeast(0) }
        val time = when {
            received == null -> ""
            earlier -> received.atZone(zone).format(java.time.format.DateTimeFormatter.ofPattern("d MMM", locale))
            minutes!! < 1 -> resources.getString(R.string.notif_now)
            minutes < 60 -> resources.getString(R.string.notif_minutes, minutes)
            else -> resources.getString(R.string.notif_hours, minutes / 60)
        }
        val message = p.containsKey("message_id")
        val booking = p.containsKey("booking_id")
        val requestMatch = p.containsKey("request_id")
        val match = requestMatch || (p.containsKey("trip_offer_id") && !booking && !p.containsKey("trip_id"))
        val requestSummary = if (requestMatch) {
            val dep = p["departure_time"] ?: resources.getString(R.string.notif_soon)
            val dest = p["destination"] ?: resources.getString(R.string.notif_campus)
            val seats = p["available_seats"]?.toIntOrNull() ?: 1
            val co2 = p["estimated_co2_kg"] ?: "1.00"
            resources.getString(R.string.notif_request_summary, dep, dest,
                resources.getQuantityString(R.plurals.home_seats, seats, seats), co2)
        } else null
        NotificationItem(id,
            when { message -> resources.getString(R.string.notif_message_title); booking -> resources.getString(R.string.notif_booking_title); requestMatch -> resources.getString(R.string.notif_request_title); match -> resources.getString(R.string.notif_offer_title); p.containsKey("alert_id") -> resources.getString(R.string.notif_safety_title); else -> resources.getString(R.string.notif_trip_title) },
            when { message -> resources.getString(R.string.notif_message_body); booking -> resources.getString(R.string.notif_booking_body); requestSummary != null -> requestSummary; match -> resources.getString(R.string.notif_offer_body); else -> resources.getString(R.string.notif_trip_body) },
            when { message -> NotificationCategory.Messages; match -> NotificationCategory.Matches; else -> NotificationCategory.Bookings },
            when { message -> NotificationGlyph.Message; match -> NotificationGlyph.Group; else -> NotificationGlyph.Check },
            time = time, earlier = earlier, unread = id !in inbox.read,
            action = when { message -> resources.getString(R.string.notif_open_messages); booking -> resources.getString(R.string.notif_view_booking); requestMatch -> resources.getString(R.string.notif_view_match); match -> resources.getString(R.string.notif_view_offer); else -> resources.getString(R.string.ui_view_trip) })
    }
