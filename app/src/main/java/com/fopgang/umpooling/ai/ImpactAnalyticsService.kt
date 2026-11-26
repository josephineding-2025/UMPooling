package com.fopgang.umpooling.ai

import com.fopgang.umpooling.data.repository.PersonalImpact
import com.fopgang.umpooling.data.repository.StaffImpactCell

/**
 * Analytical service for personal sustainability insights, demo data provision,
 * and leaderboard aggregations (FR5.3).
 *
 * Real database calls for impact data are managed exclusively by
 * [com.fopgang.umpooling.data.repository.ImpactRepository].
 */
class ImpactAnalyticsService {

    companion object {
        val instance by lazy { ImpactAnalyticsService() }
    }

    /**
     * Algorithmic insights generator translating numeric CO2 savings into tangible equivalents.
     */
    fun generatePersonalInsights(totalTrips: Int, co2Kg: Double): PersonalInsight {
        val trees = (co2Kg / 10.0 * 10).toInt() / 10.0 // ~10kg CO2 per urban sapling/year
        val smartphoneCharges = (co2Kg * 120).toInt() // ~120 charges per kg CO2 avoided

        val headline = when {
            co2Kg >= 60.0 -> "Campus Sustainability Leader"
            co2Kg >= 20.0 -> "Active Green Commuter"
            co2Kg >= 5.0 -> "Eco-Conscious Commuter"
            totalTrips >= 1 -> "Emerging Eco-Driver"
            else -> "Ready for Green Commuting"
        }

        val body = if (co2Kg > 0) {
            "You have avoided %.1f kg CO₂ across %d carpool trips, equivalent to planting %.1f trees."
                .format(co2Kg, totalTrips, trees)
        } else {
            "Complete your first shared trip to start avoiding carbon emissions and unlocking Green Commuter Tiers."
        }

        val tip = when {
            totalTrips >= 5 -> "Carpooling 3+ times a week reduces your commuting footprint by up to 60% compared to solo driving."
            totalTrips >= 1 -> "Sharing rides during peak 8:00–9:00 AM rush hours helps ease UM campus traffic congestion around the Damansara gate."
            else -> "Pair up with commuters from your residential college (e.g., KK12 or KK10) to cut campus parking bottlenecks."
        }

        return PersonalInsight(
            headline = headline,
            body = body,
            equivalentTreesPlanted = trees,
            equivalentSmartphoneCharges = smartphoneCharges,
            lowCarbonTip = tip
        )
    }

    fun generatePersonalInsights(stats: PersonalImpact): PersonalInsight =
        generatePersonalInsights(stats.totalTrips ?: 0, stats.co2Kg ?: 0.0)

    fun generatePersonalInsights(stats: UserImpactStats): PersonalInsight =
        generatePersonalInsights(stats.totalTrips, stats.totalCo2SavedKg)

    /**
     * Daily / weekly / monthly CO2 emissions avoided for personal dashboard charts.
     */
    fun getPersonalDailyBreakdown(monthly: Boolean = false): List<DailyImpactBar> {
        return if (monthly) {
            listOf(
                DailyImpactBar("Week 1", 5.7f),
                DailyImpactBar("Week 2", 7.6f),
                DailyImpactBar("Week 3", 5.7f),
                DailyImpactBar("Week 4", 3.8f)
            )
        } else {
            listOf(
                DailyImpactBar("Mon", 1.9f),
                DailyImpactBar("Tue", 3.8f),
                DailyImpactBar("Wed", 0.0f),
                DailyImpactBar("Thu", 1.9f),
                DailyImpactBar("Fri", 3.8f),
                DailyImpactBar("Sat", 0.0f),
                DailyImpactBar("Sun", 0.0f)
            )
        }
    }

    /**
     * Leaderboard grouped strictly by faculty (FR5.3). Never ranks individual commuters.
     */
    fun getFacultyLeaderboard(): List<LeaderboardEntry> {
        return listOf(
            LeaderboardEntry(1, "Computer Science (FSKTM)", "faculty", 1120.5, 480, 78),
            LeaderboardEntry(2, "Faculty of Engineering", "faculty", 980.2, 410, 65),
            LeaderboardEntry(3, "Faculty of Science", "faculty", 620.0, 270, 42),
            LeaderboardEntry(4, "Faculty of Business & Economics", "faculty", 480.0, 210, 31),
            LeaderboardEntry(5, "Faculty of Arts & Social Sciences", "faculty", 310.5, 140, 22)
        )
    }

    /**
     * Leaderboard grouped strictly by residential college (FR5.3). Never ranks individual commuters.
     */
    fun getCollegeLeaderboard(): List<LeaderboardEntry> {
        return listOf(
            LeaderboardEntry(1, "Kolej Kediaman 12 (KK12)", "college", 840.0, 360, 54),
            LeaderboardEntry(2, "Kolej Kediaman 10 (KK10)", "college", 720.5, 310, 48),
            LeaderboardEntry(3, "Kolej Kediaman 11 (KK11)", "college", 510.0, 220, 35),
            LeaderboardEntry(4, "Kolej Kediaman 8 (KK8)", "college", 430.0, 190, 29),
            LeaderboardEntry(5, "Kolej Kediaman 7 (KK7)", "college", 290.0, 130, 20)
        )
    }

    /**
     * Demo Personal Impact numbers used when in demo mode or signed out.
     */
    fun getDemoPersonalImpact(userId: String = "demo-user"): PersonalImpact {
        return PersonalImpact(
            userId = userId,
            totalTrips = 14,
            distanceKm = 114.0,
            co2Kg = 22.8,
            tierLevel = 2,
            tierName = "Sprout"
        )
    }

    /**
     * Demo UserImpactStats for backward compatibility with UI components.
     */
    fun getDemoUserImpactStats(userId: String = "demo-user"): UserImpactStats {
        return UserImpactStats(
            userId = userId,
            totalTrips = 14,
            totalDistanceKm = 114.0,
            totalCo2SavedKg = 22.8,
            currentTierLevel = 2,
            currentTierName = "Sprout"
        )
    }

    /**
     * Demo Campus impact summary for community goal display.
     */
    fun getDemoCampusImpactSummary(): CampusImpactSummary {
        return CampusImpactSummary(
            totalCo2SavedKg = 3420.0,
            totalSharedRides = 1800,
            semesterGoalKg = 10000.0,
            semesterGoalPercentage = 34
        )
    }

    /**
     * Client-side privacy enforcement helper ensuring any group with activeUserCount < kMin
     * has metrics suppressed.
     */
    fun applyKAnonymity(
        aggregates: List<StaffAnonymisedAggregate>,
        kMin: Int = 5
    ): List<StaffAnonymisedAggregate> {
        return aggregates.map { aggregate ->
            if (aggregate.activeUserCount < kMin) {
                aggregate.copy(
                    totalDistanceKm = null,
                    totalCo2SavedKg = null,
                    isMasked = true
                )
            } else {
                aggregate.copy(isMasked = false)
            }
        }
    }
}
