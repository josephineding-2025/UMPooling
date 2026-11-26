package com.fopgang.umpooling.ai

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ImpactRepositoryTest {

    private lateinit var impactRepo: ImpactRepository

    @Before
    fun setUp() {
        impactRepo = ImpactRepository()
    }

    @Test
    fun generatePersonalInsights_calculatesRealisticEquivalencies() {
        // User with 22.8 kg CO2 saved
        val stats = UserImpactStats(
            userId = "user-1",
            totalTrips = 14,
            totalDistanceKm = 114.0,
            totalCo2SavedKg = 22.8,
            currentTierLevel = 2,
            currentTierName = "Sprout"
        )

        val insight = impactRepo.generatePersonalInsights(stats)

        assertEquals("Active Green Commuter", insight.headline)
        assertTrue(insight.body.contains("22.8 kg"))
        assertEquals(2.3, insight.equivalentTreesPlanted, 0.1)
        assertTrue(insight.equivalentSmartphoneCharges > 2000)
        assertTrue(insight.lowCarbonTip.isNotBlank())
    }

    @Test
    fun facultyAndCollegeLeaderboards_strictlyNonIndividual() {
        val facultyLeaderboard = impactRepo.getFacultyLeaderboard()
        assertFalse(facultyLeaderboard.isEmpty())
        // Verify all entries are faculties, sorted descending by CO2
        assertTrue(facultyLeaderboard.all { it.category == "faculty" })
        assertEquals("Computer Science (FSKTM)", facultyLeaderboard.first().name)
        assertTrue(facultyLeaderboard[0].totalCo2SavedKg >= facultyLeaderboard[1].totalCo2SavedKg)

        val collegeLeaderboard = impactRepo.getCollegeLeaderboard()
        assertFalse(collegeLeaderboard.isEmpty())
        assertTrue(collegeLeaderboard.all { it.category == "college" })
        assertEquals("Kolej Kediaman 12 (KK12)", collegeLeaderboard.first().name)
    }

    @Test
    fun applyKAnonymity_masksRecordsBelowFiveUsers() {
        val rawAggregates = listOf(
            StaffAnonymisedAggregate(
                aggregateId = "agg-1",
                weekStart = "2026-11-23",
                faculty = "FSKTM",
                residentialArea = "Tiara Damansara",
                residentialCollege = null,
                activeUserCount = 12, // >= 5 -> Should NOT be masked
                tripCount = 45,
                totalDistanceKm = 450.0,
                totalCo2SavedKg = 90.0
            ),
            StaffAnonymisedAggregate(
                aggregateId = "agg-2",
                weekStart = "2026-11-23",
                faculty = "Faculty of Pharmacy",
                residentialArea = "Seksyen 17",
                residentialCollege = null,
                activeUserCount = 3, // < 5 -> MUST be masked under k=5 privacy mandate
                tripCount = 6,
                totalDistanceKm = 60.0,
                totalCo2SavedKg = 12.0
            ),
            StaffAnonymisedAggregate(
                aggregateId = "agg-3",
                weekStart = "2026-11-23",
                faculty = "Faculty of Science",
                residentialArea = null,
                residentialCollege = "KK12",
                activeUserCount = 5, // Exactly 5 -> Should NOT be masked
                tripCount = 20,
                totalDistanceKm = 180.0,
                totalCo2SavedKg = 36.0
            )
        )

        val protected = impactRepo.applyKAnonymity(rawAggregates)

        // agg-1 (12 users): Not masked
        assertFalse(protected[0].isMasked)
        assertEquals(90.0, protected[0].totalCo2SavedKg!!, 0.001)
        assertEquals(450.0, protected[0].totalDistanceKm!!, 0.001)

        // agg-2 (3 users): Masked
        assertTrue(protected[1].isMasked)
        assertNull(protected[1].totalCo2SavedKg)
        assertNull(protected[1].totalDistanceKm)
        assertEquals(3, protected[1].activeUserCount) // User count remains for statistical tallying

        // agg-3 (5 users): Boundary satisfied
        assertFalse(protected[2].isMasked)
        assertEquals(36.0, protected[2].totalCo2SavedKg!!, 0.001)
    }

    @Test
    fun getDemoUserImpactStats_computesValidTier() {
        val demo = impactRepo.getDemoUserImpactStats("demo")
        assertEquals(14, demo.totalTrips)
        assertEquals(22.8, demo.totalCo2SavedKg, 0.001)
        // 14 trips, 22.8 kg -> Sprout (Level 2) because Sapling requires 15 trips
        assertEquals(2, demo.currentTierLevel)
        assertEquals("Sprout", demo.currentTierName)
    }
}
