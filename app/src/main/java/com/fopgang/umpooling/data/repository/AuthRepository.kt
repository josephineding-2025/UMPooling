package com.fopgang.umpooling.data.repository

import com.fopgang.umpooling.BuildConfig
import com.fopgang.umpooling.SupabaseClientProvider
import com.fopgang.umpooling.auth.*
import com.fopgang.umpooling.data.UmEmail
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AuthRepository : AuthGateway {
    private val configured get() = BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()
    private val auth get() = SupabaseClientProvider.client.auth

    override val sessions: Flow<AuthSession> = flow {
        if (!configured) {
            emit(AuthSession.Failure(AuthIssue.CONFIGURATION))
            return@flow
        }
        try {
            auth.sessionStatus.collect { status ->
                emit(when (status) {
                    SessionStatus.Initializing -> AuthSession.Restoring
                    is SessionStatus.NotAuthenticated -> AuthSession.SignedOut
                    is SessionStatus.RefreshFailure -> AuthSession.Failure(AuthIssue.SESSION)
                    is SessionStatus.Authenticated -> status.session.user?.let { AuthSession.SignedIn(it.toAuthUser()) }
                        ?: AuthSession.Failure(AuthIssue.SESSION)
                })
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { emit(AuthSession.Failure(AuthIssue.SESSION)) }
    }

    override suspend fun signUp(submission: AuthSubmission): Result<Unit> = request {
        requireUmEmail(submission.email)
        auth.signUpWith(Email) {
            email = UmEmail.normalize(submission.email)
            password = submission.password
            // These are descriptive metadata, never a privileged usersRole assignment.
            data = buildJsonObject {
                put("display_name", submission.displayName.trim())
                put("matric_id", submission.matric.trim())
                put("affiliation", if (submission.staff) "staff" else "student")
            }
        }
    }

    override suspend fun signIn(email: String, password: String): Result<Unit> = request {
        requireUmEmail(email)
        auth.signInWith(Email) { this.email = UmEmail.normalize(email); this.password = password }
    }

    override suspend fun verify(email: String, code: String): Result<Unit> = request {
        requireUmEmail(email)
        auth.verifyEmailOtp(OtpType.Email.SIGNUP, UmEmail.normalize(email), code)
    }

    override suspend fun resend(email: String): Result<Unit> = request {
        requireUmEmail(email)
        auth.resendEmail(OtpType.Email.SIGNUP, UmEmail.normalize(email))
    }

    override suspend fun refreshUser(): Result<AuthUser> = request {
        auth.retrieveUserForCurrentSession(updateSession = true).toAuthUser()
    }

    override suspend fun signOut(): Result<Unit> = request {
        com.fopgang.umpooling.notifications.FcmTokenSync.signOut()
    }

    // Preserve the existing repository entry points for other feature integrations.
    suspend fun signUpUMUser(email: String, pass: String): Result<Unit> =
        signUp(AuthSubmission(email, pass))
    suspend fun signInUser(email: String, pass: String): Result<Unit> = signIn(email, pass)

    private fun requireUmEmail(email: String) {
        if (!UmEmail.isValid(email)) throw AuthFailure(AuthIssue.INVALID_EMAIL)
    }

    private suspend fun <T> request(block: suspend () -> T): Result<T> {
        if (!configured) return Result.failure(AuthFailure(AuthIssue.CONFIGURATION))
        return try { Result.success(block()) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: AuthFailure) { Result.failure(failure) }
        catch (error: Exception) {
            val issue = when ((error as? AuthRestException)?.let { it.errorCode?.value ?: it.error }) {
                "email_not_confirmed" -> AuthIssue.UNVERIFIED
                "invalid_credentials" -> AuthIssue.CREDENTIALS
                "otp_expired", "otp_disabled" -> AuthIssue.CODE
                "over_request_rate_limit", "over_email_send_rate_limit" -> AuthIssue.RATE_LIMIT
                else -> AuthIssue.REQUEST
            }
            // Do not expose backend response bodies, tokens or personal data.
            Result.failure(AuthFailure(issue))
        }
    }
}

private fun UserInfo.toAuthUser() = AuthUser(id, email.orEmpty(), emailConfirmedAt != null)
