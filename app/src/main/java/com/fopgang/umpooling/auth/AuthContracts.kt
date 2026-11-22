package com.fopgang.umpooling.auth

import com.fopgang.umpooling.data.UmEmail
import kotlinx.coroutines.flow.Flow

data class AuthUser(val id: String, val email: String, val emailVerified: Boolean)

sealed interface AuthSession {
    data object Restoring : AuthSession
    data object SignedOut : AuthSession
    data class SignedIn(val user: AuthUser) : AuthSession
    data class Failure(val issue: AuthIssue) : AuthSession
}

sealed interface AuthAccess {
    data object Restoring : AuthAccess
    data object SignedOut : AuthAccess
    data class VerificationRequired(val email: String) : AuthAccess
    data class Authenticated(val user: AuthUser) : AuthAccess
    data object Demo : AuthAccess
}

enum class AuthIssue { INVALID_EMAIL, INVALID_FORM, CREDENTIALS, UNVERIFIED, CODE, RATE_LIMIT, CONFIGURATION, SESSION, REQUEST }
enum class AuthNotice { EMAIL_SENT }
class AuthFailure(val issue: AuthIssue) : Exception()

/** A transient event. Passwords are never saved in UI state or SavedStateHandle. */
data class AuthSubmission(
    val email: String,
    val password: String,
    val displayName: String = "",
    val matric: String = "",
    val staff: Boolean = false,
    val acceptedGuidelines: Boolean = false,
)

data class AuthUiState(
    val access: AuthAccess = AuthAccess.SignedOut,
    val busy: Boolean = false,
    val issue: AuthIssue? = null,
    val notice: AuthNotice? = null,
)

fun AuthUser.access(): AuthAccess = when {
    !UmEmail.isValid(email) -> AuthAccess.SignedOut
    !emailVerified -> AuthAccess.VerificationRequired(UmEmail.normalize(email))
    else -> AuthAccess.Authenticated(this)
}

/** Gate restored and requested routes as well as button-driven navigation. */
fun AuthAccess.destinationFor(requested: String): String = when (this) {
    AuthAccess.Restoring -> "restoring"
    AuthAccess.SignedOut -> if (requested in setOf("welcome", "login", "register")) requested else "login"
    is AuthAccess.VerificationRequired -> "verify"
    is AuthAccess.Authenticated, AuthAccess.Demo -> if (requested in setOf("welcome", "login", "register", "verify")) "home" else requested
}

interface AuthGateway {
    val sessions: Flow<AuthSession>
    suspend fun signUp(submission: AuthSubmission): Result<Unit>
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun verify(email: String, code: String): Result<Unit>
    suspend fun resend(email: String): Result<Unit>
    suspend fun refreshUser(): Result<AuthUser>
    suspend fun signOut(): Result<Unit>
}
