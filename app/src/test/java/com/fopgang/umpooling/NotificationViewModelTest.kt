package com.fopgang.umpooling

import com.fopgang.umpooling.notifications.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Test

class NotificationViewModelTest {
    private class Fake : NotificationGateway {
        val inbox = MutableStateFlow(InboxSnapshot())
        var marks = emptySet<String>()
        var records = 0
        var resolutions = 0
        var resolveResult: Result<String> = Result.success("confirmed_ticket/booking")
        var markResult: Result<Unit> = Result.success(Unit)
        override fun observe(): Flow<InboxSnapshot> = inbox
        override suspend fun record(payload: Map<String,String>): Result<Unit> { records++; return Result.success(Unit) }
        override suspend fun resolve(payload: Map<String,String>): Result<String> { resolutions++; return resolveResult }
        override suspend fun markRead(ids: Set<String>): Result<Unit> { marks += ids; return markResult }
    }
    @Test fun validTapIsMarkedAndRoutesOnce() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val fake = Fake()
            val vm = NotificationViewModel(fake, scope)
            val routes = mutableListOf<String>()
            vm.routes.onEach { routes += it }.launchIn(scope)
            val payload = mapOf("booking_id" to "booking")
            vm.accept(payload); vm.accept(payload)
            assertEquals(1, fake.records)
            assertEquals(1, fake.resolutions)
            assertEquals(listOf("confirmed_ticket/booking"), routes)
            assertTrue(notificationIdentity(payload) in fake.marks)
            assertFalse(vm.state.value.busy)
        } finally { scope.cancel() }
    }
    @Test fun deniedDestinationDoesNotNavigateOrMarkRead() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val fake = Fake().apply { resolveResult = Result.failure(IllegalStateException("denied")) }
            val vm = NotificationViewModel(fake, scope)
            val routes = mutableListOf<String>()
            vm.routes.onEach { routes += it }.launchIn(scope)
            vm.open(mapOf("trip_id" to "private"))
            assertTrue(routes.isEmpty())
            assertTrue(fake.marks.isEmpty())
            assertNotNull(vm.state.value.error)
            assertFalse(vm.state.value.busy)
        } finally { scope.cancel() }
    }
    @Test fun markAllIncludesEarlierAndFilteredOutRows() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val fake = Fake()
            val rows = listOf(mapOf("booking_id" to "one"), mapOf("message_id" to "two"))
            fake.inbox.value = InboxSnapshot(rows)
            val vm = NotificationViewModel(fake, scope)
            vm.markAllRead()
            assertEquals(rows.map(::notificationIdentity).toSet(), fake.marks)
        } finally { scope.cancel() }
    }
    @Test fun arrivalMetadataDoesNotChangeIdentity() {
        val payload = mapOf("booking_id" to "one")
        assertEquals(notificationIdentity(payload), notificationIdentity(payload + ("_received_at" to "123")))
    }
}
