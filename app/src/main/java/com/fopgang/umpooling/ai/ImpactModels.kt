package com.fopgang.umpooling.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Commuter personal impact data model (FR5.3).
 */
@Serializable
data class UserImpactStats(
    @SerialName("user_id") val userId: String,
    @SerialName("total_trips") val totalTrips: Int = 0,
    @SerialName("total_distance_km") val totalDistanceKm: Double = 0.0,
    @SerialName("total_co2_saved_kg") val totalCo2SavedKg: Double = 0.0,
    @SerialName("current_tier_level") val currentTierLevel: Int = 0,
    @SerialName("current_tier_name") val currentTierName: String = "No Badge",
    @SerialName("updated_at") val updatedAt: String? = null
)

/**
 * Bar data point for daily/weekly sustainability dashboard charts.
 */
data class DailyImpactBar(
    val label: String,
    val co2Kg: Float
)

/**
 * Campus-wide community impact metrics.
 */
@Serializable
data class CampusImpactSummary(
    @SerialName("total_co2_saved_kg") val totalCo2SavedKg: Double = 3420.0,
    @SerialName("total_shared_rides") val totalSharedRides: Int = 1800,
    @SerialName("semester_goal_kg") val semesterGoalKg: Double = 10000.0,
    @SerialName("semester_goal_percentage") val semesterGoalPercentage: Int = 34
)

/**
 * Community leaderboard entry grouped strictly by faculty or residential college (FR5.3).
 * Individual commuters are never ranked to protect user privacy.
 */
@Serializable
data class LeaderboardEntry(
    val rank: Int,
    val name: String,
    val category: String, // "faculty" or "college"
    val totalCo2SavedKg: Double,
    val tripCount: Int,
    val activeUsers: Int
)

/**
 * Institutional aggregated analytics record for authorized UM staff and Sustainability Centre.
 * Fields are masked if activeUserCount < 5 (k-anonymity privacy mandate).
 */
@Serializable
data class StaffAnonymisedAggregate(
    @SerialName("aggregate_id") val aggregateId: String,
    @SerialName("week_start") val weekStart: String,
    @SerialName("faculty") val faculty: String?,
    @SerialName("residential_area") val residentialArea: String?,
    @SerialName("residential_college") val residentialCollege: String?,
    @SerialName("active_user_count") val activeUserCount: Int,
    @SerialName("trip_count") val tripCount: Int,
    @SerialName("total_distance_km") val totalDistanceKm: Double?,
    @SerialName("total_co2_saved_kg") val totalCo2SavedKg: Double?,
    val isMasked: Boolean = false
)

/**
 * Personalized insights and low-carbon commuting suggestions.
 */
data class PersonalInsight(
    val headline: String,
    val body: String,
    val equivalentTreesPlanted: Double,
    val equivalentSmartphoneCharges: Int,
    val lowCarbonTip: String
)
