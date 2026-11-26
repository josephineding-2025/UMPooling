package com.fopgang.umpooling.ai

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalTime

class CommuterGroupingServiceTest {

    private lateinit var groupingService: CommuterGroupingService

    @Before
    fun setUp() {
        groupingService = CommuterGroupingService()
    }

    @Test
    fun groupCommuters_compatibleCandidates_createsSingleGroupWithOptimalWindow() {
        // 3 candidates in Tiara Damansara heading to FSKTM around 08:15
        val candidates = listOf(
            CommuterRideCandidate("1", "u1", "Ali", 3.1289, 101.6375, "Tiara Damansara", 3.1284, 101.6505, "FSKTM", LocalTime.of(8, 10)),
            CommuterRideCandidate("2", "u2", "Bala", 3.1290, 101.6376, "Tiara Damansara", 3.1284, 101.6505, "FSKTM", LocalTime.of(8, 15)),
            CommuterRideCandidate("3", "u3", "Chong", 3.1288, 101.6374, "Tiara Damansara", 3.1284, 101.6505, "FSKTM", LocalTime.of(8, 20))
        )

        val groups = groupingService.groupCommuters(candidates)

        assertEquals(1, groups.size)
        val group = groups.first()
        assertEquals(3, group.riderCount)
        assertEquals(3, group.totalSeatsRequested)
        assertEquals("Tiara Damansara", group.clusterPickupName)
        assertEquals("FSKTM", group.destinationName)
        // Median time is 08:15 -> window is 08:05–08:25
        assertEquals(LocalTime.of(8, 5), group.recommendedWindowStart)
        assertEquals(LocalTime.of(8, 25), group.recommendedWindowEnd)
        assertEquals("08:05–08:25", group.recommendedWindowLabel)
        assertTrue(group.matchScorePercentage in 70..98)
        assertTrue(group.estimatedCostPerRiderSen in 100..500)
        assertTrue(group.estimatedCo2SavedKg > 0.0)
    }

    @Test
    fun groupCommuters_distantPickups_doesNotClusterTogether() {
        // Ali in Tiara (3.1289, 101.6375), Bala in Vista Angkasa (3.1118, 101.6667) -> Distance is > 3 km
        val candidates = listOf(
            CommuterRideCandidate("1", "u1", "Ali", 3.1289, 101.6375, "Tiara Damansara", 3.1284, 101.6505, "FSKTM", LocalTime.of(8, 10)),
            CommuterRideCandidate("2", "u2", "Bala", 3.1118, 101.6667, "Vista Angkasa", 3.1284, 101.6505, "FSKTM", LocalTime.of(8, 10))
        )

        val groups = groupingService.groupCommuters(candidates)
        // Since neither cluster has min_samples >= 2, no cluster is formed
        assertTrue(groups.isEmpty())
    }

    @Test
    fun groupCommuters_differentDestinations_doesNotGroupTogether() {
        // Both in Tiara, but Ali goes to FSKTM (3.1284, 101.6505) and Bala goes to UMMC (3.1122, 101.6548) -> > 1.8 km apart
        val candidates = listOf(
            CommuterRideCandidate("1", "u1", "Ali", 3.1289, 101.6375, "Tiara Damansara", 3.1284, 101.6505, "FSKTM", LocalTime.of(8, 10)),
            CommuterRideCandidate("2", "u2", "Bala", 3.1289, 101.6375, "Tiara Damansara", 3.1122, 101.6548, "UMMC", LocalTime.of(8, 10))
        )

        val groups = groupingService.groupCommuters(candidates)
        assertTrue(groups.isEmpty())
    }

    @Test
    fun groupCommuters_incompatibleDepartureTimes_doesNotGroupTogether() {
        // Both in Tiara going to FSKTM, but Ali departs 08:00 and Bala departs 09:30 (> 15 min gap)
        val candidates = listOf(
            CommuterRideCandidate("1", "u1", "Ali", 3.1289, 101.6375, "Tiara Damansara", 3.1284, 101.6505, "FSKTM", LocalTime.of(8, 0)),
            CommuterRideCandidate("2", "u2", "Bala", 3.1289, 101.6375, "Tiara Damansara", 3.1284, 101.6505, "FSKTM", LocalTime.of(9, 30))
        )

        val groups = groupingService.groupCommuters(candidates)
        assertTrue(groups.isEmpty())
    }

    @Test
    fun groupCommuters_exceedingCapacity_splitsIntoSeparateGroups() {
        // 5 candidates, each requesting 1 seat (total 5 > MAX_CARPOOL_SEATS 4)
        val candidates = (1..5).map { i ->
            CommuterRideCandidate("$i", "u$i", "User $i", 3.1289, 101.6375, "Tiara", 3.1284, 101.6505, "FSKTM", LocalTime.of(8, 10))
        }

        val groups = groupingService.groupCommuters(candidates)
        // Group 1 should hold 4 seats (at max capacity), and the 5th rider is partitioned
        assertEquals(1, groups.size)
        assertEquals(4, groups.first().totalSeatsRequested)
    }

    @Test
    fun getSamplePickupGroups_returnsValidRecommendations() {
        val sampleGroups = groupingService.getSamplePickupGroups("Tiara Damansara")
        assertFalse(sampleGroups.isEmpty())
        val group = sampleGroups.first()
        assertTrue(group.riderCount >= 2)
        assertTrue(group.recommendedWindowLabel.contains("–"))
        assertTrue(group.estimatedCostPerRiderSen > 0)
    }
}
