package com.fopgang.umpooling.data.repository

import com.fopgang.umpooling.SupabaseClientProvider
import com.fopgang.umpooling.data.EmergencyContact
import com.fopgang.umpooling.data.UserProfile
import com.fopgang.umpooling.data.UserRole
import com.fopgang.umpooling.profile.ProfileForm
import com.fopgang.umpooling.profile.ProfileRules
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** The signed-in user's own profile (FR1.3). The app and the tests talk to this interface. */
interface ProfileGateway {
    suspend fun load(): Result<UserProfile>
    suspend fun save(form: ProfileForm): Result<Unit>
    suspend fun saveEmergencyContact(contact: EmergencyContact?): Result<Unit>
}

/** Blank text becomes null so a cleared field is cleared in the database. */
private fun String.orNull(): JsonPrimitive = trim().ifEmpty { null }?.let(::JsonPrimitive) ?: JsonNull

internal fun ProfileForm.toUpdate(): JsonObject {
    val vehicle = ProfileRules.needsVehicle(role)
    return buildJsonObject {
        put("display_name", displayName.orNull())
        put("faculty", faculty.orNull())
        put("residential_area", residentialArea.orNull())
        put("residential_college", residentialCollege.orNull())
        // Passengers keep no vehicle details on file.
        put("vehicle_type", if (vehicle) vehicleType.orNull() else JsonNull)
        put("vehiclePlate", if (vehicle) JsonPrimitive(ProfileRules.normalizePlate(vehiclePlate)) else JsonNull)
        put("vehicleModel", if (vehicle) vehicleModel.orNull() else JsonNull)
        put("fuel_category", if (vehicle) fuelCategory.orNull() else JsonNull)
    }
}

/**
 * Reads and updates the caller's own users row. The database only lets a client change the
 * columns listed in supabase/migrations/20261115000005_role_profile_cost_cap.sql, and the role
 * changes only through the set_my_role function.
 */
class ProfileRepository : ProfileGateway {
    private val client get() = SupabaseClientProvider.client

    private fun userId(): String =
        client.auth.currentUserOrNull()?.id ?: error("Not signed in")

    override suspend fun load(): Result<UserProfile> = suspendRunCatching {
        client.postgrest["users"]
            .select(Columns.ALL) { filter { eq("usersId", userId()) } }
            .decodeSingle<UserProfile>()
    }

    override suspend fun save(form: ProfileForm): Result<Unit> = suspendRunCatching {
        client.postgrest["users"].update(form.toUpdate()) { filter { eq("usersId", userId()) } }
        // Staff keep their role; only commuter roles are sent.
        if (form.role in UserRole.selectable) {
            client.postgrest.rpc("set_my_role", buildJsonObject { put("p_role", form.role) })
        }
        Unit
    }

    override suspend fun saveEmergencyContact(contact: EmergencyContact?): Result<Unit> = suspendRunCatching {
        val value: JsonObject? = contact?.let {
            buildJsonObject { put("name", it.name.trim()); put("phone", it.phone) }
        }
        client.postgrest["users"].update(
            buildJsonObject { put("emergency_contact", value ?: JsonNull) }
        ) { filter { eq("usersId", userId()) } }
        Unit
    }
}
