package com.fopgang.umpooling.auth

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fopgang.umpooling.data.UmEmail
import com.fopgang.umpooling.data.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AuthViewModel(
    private val gateway: AuthGateway,
    private val saved: SavedStateHandle,
    scopeOverride: CoroutineScope? = null,
) : ViewModel() {
    constructor(saved: SavedStateHandle) : this(AuthRepository(), saved)

    private val scope = scopeOverride ?: viewModelScope
    private val mutableState = MutableStateFlow(AuthUiState(access = AuthAccess.Restoring))
    val state = mutableState.asStateFlow()
    private var hasSession = false

    init {
        scope.launch {
            gateway.sessions.collect { session ->
                if (session is AuthSession.SignedIn) hasSession = true
                if (session == AuthSession.SignedOut) hasSession = false
                if (saved.get<Boolean>("auth.demo") == true) {
                    mutableState.update { it.copy(access = AuthAccess.Demo) }
                } else {
                    val pending = saved.get<String>("auth.pendingEmail")
                    val access = when (session) {
                        AuthSession.Restoring -> AuthAccess.Restoring
                        AuthSession.SignedOut -> pending?.let { AuthAccess.VerificationRequired(it) } ?: AuthAccess.SignedOut
                        is AuthSession.SignedIn -> session.user.access()
                        is AuthSession.Failure -> AuthAccess.SignedOut
                    }
                    if (access is AuthAccess.Authenticated) saved.remove<String>("auth.pendingEmail")
                    mutableState.update { it.copy(access = access,
                        issue = when (session) {
                            is AuthSession.Failure -> session.issue
                            is AuthSession.SignedIn -> if (access == AuthAccess.SignedOut) AuthIssue.INVALID_EMAIL else null
                            else -> it.issue
                        }) }
                }
            }
        }
    }

    fun submit(register: Boolean, submission: AuthSubmission) {
        if (state.value.busy || state.value.access == AuthAccess.Restoring ||
            state.value.access == AuthAccess.Demo || state.value.access is AuthAccess.Authenticated) return
        val email = UmEmail.normalize(submission.email)
        val invalid = when {
            !UmEmail.isValid(email) -> AuthIssue.INVALID_EMAIL
            submission.password.isBlank() -> AuthIssue.INVALID_FORM
            register && (submission.password.length < 8 || submission.displayName.isBlank() ||
                submission.matric.isBlank() || !submission.acceptedGuidelines) -> AuthIssue.INVALID_FORM
            else -> null
        }
        if (invalid != null) { mutableState.update { it.copy(issue = invalid, notice = null) }; return }
        action {
            val result = if (register) gateway.signUp(submission.copy(email = email)) else gateway.signIn(email, submission.password)
            result.fold(
                onSuccess = {
                    // A verified auto-confirmed account may already have arrived via sessions.
                    if (state.value.access !is AuthAccess.Authenticated) {
                        if (register) requireVerification(email)
                        else refreshVerifiedUser(email)
                    }
                },
                onFailure = { error ->
                    val issue = issue(error)
                    if (issue == AuthIssue.UNVERIFIED) requireVerification(email)
                    else mutableState.update { it.copy(issue = issue) }
                },
            )
        }
    }

    fun verify(code: String) {
        val email = (state.value.access as? AuthAccess.VerificationRequired)?.email ?: return
        if (code.length != 6 || code.any { !it.isDigit() }) {
            mutableState.update { it.copy(issue = AuthIssue.CODE) }; return
        }
        action {
            gateway.verify(email, code).fold(
                onSuccess = { refreshVerifiedUser(email) },
                onFailure = { e -> mutableState.update { it.copy(issue = issue(e)) } },
            )
        }
    }

    fun resend() {
        val email = (state.value.access as? AuthAccess.VerificationRequired)?.email ?: return
        action { gateway.resend(email).fold(
            onSuccess = { mutableState.update { it.copy(notice = AuthNotice.EMAIL_SENT) } },
            onFailure = { e -> mutableState.update { it.copy(issue = issue(e)) } },
        ) }
    }

    /** Refreshes a verification completed in a browser; a local navigation flag never verifies a user. */
    fun checkVerification() {
        val email = (state.value.access as? AuthAccess.VerificationRequired)?.email ?: return
        action { refreshVerifiedUser(email) }
    }

    fun enterDemo() {
        if (state.value.busy || state.value.access == AuthAccess.Restoring ||
            state.value.access is AuthAccess.Authenticated) return
        if (hasSession) {
            action { gateway.signOut().fold(
                onSuccess = { activateDemo() },
                onFailure = { e -> mutableState.update { it.copy(issue = issue(e)) } },
            ) }
        } else activateDemo()
    }

    private fun activateDemo() {
        saved["auth.demo"] = true
        mutableState.value = AuthUiState(access = AuthAccess.Demo)
    }

    fun logout() {
        if (state.value.access == AuthAccess.Demo && !hasSession) {
            saved["auth.demo"] = false
            saved.remove<String>("auth.pendingEmail")
            mutableState.value = AuthUiState(access = AuthAccess.SignedOut)
            return
        }
        action {
            gateway.signOut().fold(
                onSuccess = {
                    saved["auth.demo"] = false
                    saved.remove<String>("auth.pendingEmail")
                    mutableState.value = AuthUiState(access = AuthAccess.SignedOut)
                },
                onFailure = { e -> mutableState.update { it.copy(issue = issue(e)) } },
            )
        }
    }

    private fun requireVerification(email: String) {
        saved["auth.pendingEmail"] = email
        mutableState.update { it.copy(access = AuthAccess.VerificationRequired(email), issue = null) }
    }

    private suspend fun refreshVerifiedUser(email: String) {
        gateway.refreshUser().fold(
            onSuccess = { user ->
                val access = user.access()
                if (access is AuthAccess.Authenticated) {
                    saved.remove<String>("auth.pendingEmail")
                    mutableState.update { it.copy(access = access, issue = null) }
                } else {
                    requireVerification(email)
                    mutableState.update { it.copy(issue = AuthIssue.UNVERIFIED) }
                }
            },
            onFailure = { e -> mutableState.update { it.copy(issue = issue(e)) } },
        )
    }

    private fun action(block: suspend () -> Unit) {
        if (state.value.busy) return
        mutableState.update { it.copy(busy = true, issue = null, notice = null) }
        scope.launch {
            try { block() }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (error: Exception) { mutableState.update { it.copy(issue = issue(error)) } }
            finally { mutableState.update { it.copy(busy = false) } }
        }
    }

    fun dismissMessage() { mutableState.update { it.copy(issue = null, notice = null) } }

    private fun issue(error: Throwable) = (error as? AuthFailure)?.issue ?: AuthIssue.REQUEST
}
