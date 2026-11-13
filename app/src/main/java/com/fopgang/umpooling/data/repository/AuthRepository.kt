package com.fopgang.umpooling.data.repository

import com.fopgang.umpooling.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email

class AuthRepository {

    suspend fun signUpUMUser(email: String, pass: String): Result<Unit> {
        return try {
            if (!email.endsWith("@siswa.um.edu.my") && !email.endsWith("@um.edu.my")) {
                return Result.failure(Exception("Registration is restricted to UM email addresses."))
            }
            SupabaseClientProvider.client.auth.signUpWith(Email) {
                this.email = email
                this.password = pass
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInUser(email: String, pass: String): Result<Unit> {
        return try {
            SupabaseClientProvider.client.auth.signInWith(Email) {
                this.email = email
                this.password = pass
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
