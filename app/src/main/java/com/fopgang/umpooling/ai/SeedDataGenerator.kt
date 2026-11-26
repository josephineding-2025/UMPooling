package com.fopgang.umpooling.ai

import java.time.LocalDate
import java.util.UUID

/**
 * Synthetic seed data generator for Module 5 evaluation, demo mode, and offline testing.
 * Produces realistic UM commuting statistics, leaderboards, and k-anonymity validation cohorts.
 */
object SeedDataGenerator {

    val FACULTIES = listOf(
        "Computer Science (FSKTM)",
        "Faculty of Engineering",
        "Faculty of Science",
        "Faculty of Business & Economics",
        "Faculty of Medicine",
        "Faculty of Law"
    )

    val RESIDENTIAL_AREAS = listOf(
        "Tiara Damansara",
        "Pacific Star",
        "Vista Angkasa",
        "Pantai Hillpark",
        "Seksyen 17",
        "Bangsar South"
    )

    val COLLEGES = listOf(
        "Kolej Kediaman 12 (KK12)",
        "Kolej Kediaman 10 (KK10)",
        "Kolej Kediaman 11 (KK11)",
        "Kolej Kediaman 8 (KK8)",
        "Kolej Kediaman 5 (KK5)",
        "Kolej Kediaman 2 (KK2)"
    )

    /**
     * Generates a sample cohort of user impact stats with realistic tier distributions.
     */
    fun generateUserImpactStatsCohort(): List<UserImpactStats> {
        val co2Service = Co2CalculationService.instance
        val results = mutableListOf<UserImpactStats>()

        // 1. Forest Guardian (Level 5): >= 80 trips, >= 120 kg
        results.add(createUserStats("user-guardian-1", 88, 720.0, 144.0, co2Service))
        results.add(createUserStats("user-guardian-2", 95, 810.0, 162.0, co2Service))

        // 2. Tree (Level 4): >= 40 trips, >= 60 kg
        results.add(createUserStats("user-tree-1", 45, 360.0, 72.0, co2Service))
        results.add(createUserStats("user-tree-2", 52, 420.0, 84.0, co2Service))

        // 3. Sapling (Level 3): >= 15 trips, >= 20 kg
        results.add(createUserStats("user-sapling-1", 18, 145.0, 29.0, co2Service))
        results.add(createUserStats("user-sapling-2", 24, 190.0, 38.0, co2Service))

        // 4. Sprout (Level 2): >= 5 trips, >= 5 kg
        results.add(createUserStats("user-sprout-1", 8, 64.0, 12.8, co2Service))
        results.add(createUserStats("user-sprout-2", 12, 96.0, 19.2, co2Service))

        // 5. Seedling (Level 1): >= 1 trip, >= 0.5 kg
        results.add(createUserStats("user-seedling-1", 2, 16.0, 3.2, co2Service))
        results.add(createUserStats("user-seedling-2", 3, 24.0, 4.8, co2Service))

        // 6. Asymmetric edge case: 20 trips but only 18.0 kg -> Sprout (Level 2)
        results.add(createUserStats("user-asym-1", 20, 90.0, 18.0, co2Service))

        return results
    }

    private fun createUserStats(
        id: String,
        trips: Int,
        dist: Double,
        co2: Double,
        co2Service: ICo2CalculationService
    ): UserImpactStats {
        val tier = co2Service.computeGreenCommuterTier(trips, co2)
        return UserImpactStats(
            userId = id,
            totalTrips = trips,
            totalDistanceKm = dist,
            totalCo2SavedKg = co2,
            currentTierLevel = tier.level,
            currentTierName = tier.badgeName,
            updatedAt = LocalDate.now().toString()
        )
    }

    /**
     * Generates weekly aggregated records with both k >= 5 and k < 5 cohorts
     * to evaluate the staff anonymisation view.
     */
    fun generateStaffAggregatesDataset(weekStart: String = "2026-11-23"): List<StaffAnonymisedAggregate> {
        return listOf(
            // Cohort 1: Large FSKTM Tiara cohort (k = 24 >= 5) -> Unmasked
            StaffAnonymisedAggregate(
                aggregateId = UUID.randomUUID().toString(),
                weekStart = weekStart,
                faculty = "Computer Science (FSKTM)",
                residentialArea = "Tiara Damansara",
                residentialCollege = null,
                activeUserCount = 24,
                tripCount = 96,
                totalDistanceKm = 960.0,
                totalCo2SavedKg = 192.0,
                isMasked = false
            ),
            // Cohort 2: Large Engineering Vista Angkasa cohort (k = 18 >= 5) -> Unmasked
            StaffAnonymisedAggregate(
                aggregateId = UUID.randomUUID().toString(),
                weekStart = weekStart,
                faculty = "Faculty of Engineering",
                residentialArea = "Vista Angkasa",
                residentialCollege = null,
                activeUserCount = 18,
                tripCount = 72,
                totalDistanceKm = 648.0,
                totalCo2SavedKg = 129.6,
                isMasked = false
            ),
            // Cohort 3: KK12 On-Campus cohort (k = 15 >= 5) -> Unmasked
            StaffAnonymisedAggregate(
                aggregateId = UUID.randomUUID().toString(),
                weekStart = weekStart,
                faculty = null,
                residentialArea = null,
                residentialCollege = "Kolej Kediaman 12 (KK12)",
                activeUserCount = 15,
                tripCount = 60,
                totalDistanceKm = 360.0,
                totalCo2SavedKg = 72.0,
                isMasked = false
            ),
            // Cohort 4: Small Pharmacy Seksyen 17 cohort (k = 3 < 5) -> MUST BE MASKED
            StaffAnonymisedAggregate(
                aggregateId = UUID.randomUUID().toString(),
                weekStart = weekStart,
                faculty = "Faculty of Pharmacy",
                residentialArea = "Seksyen 17",
                residentialCollege = null,
                activeUserCount = 3,
                tripCount = 8,
                totalDistanceKm = 88.0,
                totalCo2SavedKg = 17.6,
                isMasked = true
            ),
            // Cohort 5: Small Law Pacific Star cohort (k = 2 < 5) -> MUST BE MASKED
            StaffAnonymisedAggregate(
                aggregateId = UUID.randomUUID().toString(),
                weekStart = weekStart,
                faculty = "Faculty of Law",
                residentialArea = "Pacific Star",
                residentialCollege = null,
                activeUserCount = 2,
                tripCount = 5,
                totalDistanceKm = 55.0,
                totalCo2SavedKg = 11.0,
                isMasked = true
            )
        )
    }

    /**
     * Generates SQL seed script content to populate Supabase directly.
     */
    fun generateSqlSeedScript(): String {
        val sb = StringBuilder()
        sb.appendLine("-- Seed Data Script for Module 5 (FR5.3) Evaluation and Demo")
        sb.appendLine("begin;")
        sb.appendLine()
        sb.appendLine("-- 1. Seed Weekly Impact Aggregates (k >= 5 and k < 5 cohorts)")
        sb.appendLine("insert into public.impact_aggregates (week_start, faculty, residential_area, residential_college, active_user_count, trip_count, total_distance_km, total_co2_saved_kg)")
        sb.appendLine("values")
        sb.appendLine("  ('2026-11-23', 'Computer Science (FSKTM)', 'Tiara Damansara', null, 24, 96, 960.00, 192.00),")
        sb.appendLine("  ('2026-11-23', 'Faculty of Engineering', 'Vista Angkasa', null, 18, 72, 648.00, 129.60),")
        sb.appendLine("  ('2026-11-23', 'Faculty of Science', 'Pacific Star', null, 14, 52, 468.00, 93.60),")
        sb.appendLine("  ('2026-11-23', 'Faculty of Business & Economics', 'Pantai Hillpark', null, 10, 38, 304.00, 60.80),")
        sb.appendLine("  ('2026-11-23', null, null, 'Kolej Kediaman 12 (KK12)', 15, 60, 360.00, 72.00),")
        sb.appendLine("  ('2026-11-23', null, null, 'Kolej Kediaman 10 (KK10)', 11, 42, 252.00, 50.40),")
        sb.appendLine("  ('2026-11-23', 'Faculty of Pharmacy', 'Seksyen 17', null, 3, 8, 88.00, 17.60),")
        sb.appendLine("  ('2026-11-23', 'Faculty of Law', 'Pacific Star', null, 2, 5, 55.00, 11.00)")
        sb.appendLine("on conflict do nothing;")
        sb.appendLine()
        sb.appendLine("commit;")
        return sb.toString()
    }
}
