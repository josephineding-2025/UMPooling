package com.fopgang.umpooling.data.repository

import com.fopgang.umpooling.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PersonalImpact(
    @SerialName("user_id") val userId: String,
    @SerialName("total_trips") val totalTrips: Int? = null,
    @SerialName("total_distance_km") val distanceKm: Double? = null,
    @SerialName("total_co2_saved_kg") val co2Kg: Double? = null,
    @SerialName("current_tier_level") val tierLevel: Int = 0,
    @SerialName("current_tier_name") val tierName: String = "No Badge",
)
@Serializable
data class StaffImpactCell(
    @SerialName("week_start") val weekStart: String,
    @SerialName("group_type") val groupType: String,
    @SerialName("group_name") val groupName: String,
    @SerialName("active_user_count") val users: Int,
    @SerialName("trip_count") val trips: Int,
    @SerialName("total_distance_km") val distanceKm: Double,
    @SerialName("total_co2_saved_kg") val co2Kg: Double,
)
/** Both raw-table and view access are independently protected by database RLS. */
class ImpactRepository {
    suspend fun personal(): Result<PersonalImpact?> = suspendRunCatching {
        SupabaseClientProvider.client.postgrest["user_impact_stats"].select().decodeList<PersonalImpact>().singleOrNull()
    }
    suspend fun staffCells(): Result<List<StaffImpactCell>> = suspendRunCatching {
        SupabaseClientProvider.client.postgrest["staff_impact_dashboard"].select().decodeList<StaffImpactCell>()
    }
}
