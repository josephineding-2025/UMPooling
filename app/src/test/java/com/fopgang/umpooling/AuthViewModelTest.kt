package com.fopgang.umpooling

import androidx.lifecycle.SavedStateHandle
import com.fopgang.umpooling.auth.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class AuthViewModelTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    @After fun close() { scope.cancel() }

    private val verified = AuthUser("user-a", "a@siswa.um.edu.my", true)
    private fun model(gateway: FakeAuth, saved: SavedStateHandle = SavedStateHandle()) =
        AuthViewModel(gateway, saved, scope)
    private fun registration(email: String = "  A@SISWA.UM.EDU.MY ") =
        AuthSubmission(email, "password123", "Aina", "123", false, true)

    @Test fun restoredSessionsRequireVerifiedUmIdentity() {
        val gateway = FakeAuth(AuthSession.Restoring)
        val vm = model(gateway)
        assertEquals(AuthAccess.Restoring, vm.state.value.access)
        gateway.sessions.value = AuthSession.SignedIn(verified.copy(emailVerified = false))
        assertTrue(vm.state.value.access is AuthAccess.VerificationRequired)
        gateway.sessions.value = AuthSession.SignedIn(verified)
        assertEquals(AuthAccess.Authenticated(verified), vm.state.value.access)
        gateway.sessions.value = AuthSession.SignedIn(verified.copy(email = "a@gmail.com"))
        assertEquals(AuthAccess.SignedOut, vm.state.value.access)
    }

    @Test fun protectedAndRestoredRoutesCannotBypassVerification() {
        listOf("home", "profile", "confirmed_ticket/booking-a", "offer_details/offer-a", "chat").forEach {
            assertEquals("login", AuthAccess.SignedOut.destinationFor(it))
            assertEquals("verify", AuthAccess.VerificationRequired(verified.email).destinationFor(it))
            assertEquals("restoring", AuthAccess.Restoring.destinationFor(it))
        }
        assertEquals("register", AuthAccess.SignedOut.destinationFor("register"))
        assertEquals("home", AuthAccess.Authenticated(verified).destinationFor("login"))
    }

    @Test fun registrationPassesFullNormalizedPayloadAndWaitsForServerVerification() {
        val gateway = FakeAuth()
        val saved = SavedStateHandle()
        val vm = model(gateway, saved)
        vm.submit(true, registration())
        assertEquals(verified.email, gateway.registered?.email)
        assertEquals("Aina", gateway.registered?.displayName)
        assertEquals("123", gateway.registered?.matric)
        assertEquals(AuthAccess.VerificationRequired(verified.email), vm.state.value.access)
        assertEquals(setOf("auth.pendingEmail"), saved.keys())
        assertFalse(vm.state.value.busy)
    }

    @Test fun invalidEmailsAndIncompleteRegistrationNeverReachRepository() {
        val gateway = FakeAuth()
        val vm = model(gateway)
        vm.submit(true, registration("a@um.edu.my.evil.com"))
        assertEquals(AuthIssue.INVALID_EMAIL, vm.state.value.issue)
        vm.submit(true, registration().copy(acceptedGuidelines = false))
        assertEquals(AuthIssue.INVALID_FORM, vm.state.value.issue)
        assertNull(gateway.registered)
        assertEquals(0, gateway.loginCalls)
    }

    @Test fun verificationOnlyUnlocksAfterServerReturnsVerifiedUser() {
        val gateway = FakeAuth()
        val vm = model(gateway)
        vm.submit(true, registration())
        vm.verify("1")
        assertEquals(AuthIssue.CODE, vm.state.value.issue)
        assertNull(gateway.verifiedCode)
        gateway.user = verified.copy(emailVerified = false)
        vm.verify("123456")
        assertTrue(vm.state.value.access is AuthAccess.VerificationRequired)
        gateway.user = verified
        vm.verify("123456")
        assertEquals(verified.email to "123456", gateway.verifiedCode)
        assertEquals(AuthAccess.Authenticated(verified), vm.state.value.access)
    }

    @Test fun signInWithUnconfirmedEmailOpensVerificationAndCanResend() {
        val gateway = FakeAuth()
        gateway.loginResult = Result.failure(AuthFailure(AuthIssue.UNVERIFIED))
        val vm = model(gateway)
        vm.submit(false, registration())
        assertEquals(AuthAccess.VerificationRequired(verified.email), vm.state.value.access)
        vm.resend()
        assertEquals(verified.email, gateway.resentEmail)
        assertEquals(AuthNotice.EMAIL_SENT, vm.state.value.notice)
    }

    @Test fun pendingVerificationRestoresWithoutPersistingPasswords() {
        val saved = SavedStateHandle(mapOf("auth.pendingEmail" to verified.email))
        val vm = model(FakeAuth(), saved)
        assertEquals(AuthAccess.VerificationRequired(verified.email), vm.state.value.access)
        assertFalse(saved.keys().any { it.contains("password", true) })
    }

    @Test fun demoDoesNotCreateOrVerifyAnAuthenticatedSession() {
        val gateway = FakeAuth()
        val vm = model(gateway)
        vm.enterDemo()
        assertEquals(AuthAccess.Demo, vm.state.value.access)
        assertFalse(vm.state.value.access is AuthAccess.Authenticated)
        vm.submit(false, registration())
        vm.verify("123456")
        assertEquals(0, gateway.loginCalls)
        assertNull(gateway.verifiedCode)
        vm.logout()
        assertEquals(AuthAccess.SignedOut, vm.state.value.access)
        assertEquals(0, gateway.logoutCalls)
    }

    @Test fun logoutAndExpiredSessionsRemoveProtectedAccess() {
        val gateway = FakeAuth(AuthSession.SignedIn(verified))
        val vm = model(gateway)
        vm.logout()
        assertEquals(1, gateway.logoutCalls)
        assertEquals(AuthAccess.SignedOut, vm.state.value.access)
        gateway.sessions.value = AuthSession.SignedIn(verified)
        gateway.sessions.value = AuthSession.Failure(AuthIssue.SESSION)
        assertEquals(AuthAccess.SignedOut, vm.state.value.access)
        assertEquals(AuthIssue.SESSION, vm.state.value.issue)
    }

    @Test fun busyStatePreventsDuplicateRequestsAndClearsAfterFailure() {
        val gateway = FakeAuth()
        val pending = CompletableDeferred<Result<Unit>>()
        gateway.pendingLogin = pending
        val vm = model(gateway)
        vm.submit(false, registration())
        assertTrue(vm.state.value.busy)
        vm.submit(false, registration())
        assertEquals(1, gateway.loginCalls)
        pending.complete(Result.failure(AuthFailure(AuthIssue.CREDENTIALS)))
        assertFalse(vm.state.value.busy)
        assertEquals(AuthIssue.CREDENTIALS, vm.state.value.issue)
        assertEquals(AuthAccess.SignedOut, vm.state.value.access)
    }

    @Test fun demoEntryClearsAnExistingUnverifiedSession() {
        val gateway = FakeAuth(AuthSession.SignedIn(verified.copy(emailVerified = false)))
        val vm = model(gateway)
        vm.enterDemo()
        assertEquals(1, gateway.logoutCalls)
        assertEquals(AuthSession.SignedOut, gateway.sessions.value)
        assertEquals(AuthAccess.Demo, vm.state.value.access)
    }

    @Test fun restoredDemoNeverPromotesACachedSessionAndExitClearsIt() {
        val gateway = FakeAuth(AuthSession.SignedIn(verified))
        val vm = model(gateway, SavedStateHandle(mapOf("auth.demo" to true)))
        assertEquals(AuthAccess.Demo, vm.state.value.access)
        vm.logout()
        assertEquals(1, gateway.logoutCalls)
        assertEquals(AuthAccess.SignedOut, vm.state.value.access)
    }

    @Test fun refreshedSessionClearsStaleSessionErrors() {
        val gateway = FakeAuth(AuthSession.Failure(AuthIssue.SESSION))
        val vm = model(gateway)
        gateway.sessions.value = AuthSession.SignedIn(verified)
        assertEquals(AuthAccess.Authenticated(verified), vm.state.value.access)
        assertNull(vm.state.value.issue)
    }

    private class FakeAuth(initial: AuthSession = AuthSession.SignedOut) : AuthGateway {
        override val sessions = MutableStateFlow(initial)
        var user = AuthUser("user-a", "a@siswa.um.edu.my", true)
        var registered: AuthSubmission? = null
        var verifiedCode: Pair<String, String>? = null
        var resentEmail: String? = null
        var loginCalls = 0
        var logoutCalls = 0
        var loginResult = Result.success(Unit)
        var pendingLogin: CompletableDeferred<Result<Unit>>? = null
        override suspend fun signUp(submission: AuthSubmission): Result<Unit> {
            registered = submission
            return Result.success(Unit)
        }
        override suspend fun signIn(email: String, password: String): Result<Unit> {
            loginCalls++
            return pendingLogin?.await() ?: loginResult
        }
        override suspend fun verify(email: String, code: String): Result<Unit> {
            verifiedCode = email to code
            return Result.success(Unit)
        }
        override suspend fun resend(email: String): Result<Unit> {
            resentEmail = email
            return Result.success(Unit)
        }
        override suspend fun refreshUser() = Result.success(user)
        override suspend fun signOut(): Result<Unit> {
            logoutCalls++
            sessions.value = AuthSession.SignedOut
            return Result.success(Unit)
        }
    }
}
