package com.fopgang.umpooling.ai

import org.junit.Assert.*
import org.junit.Test

class SeedDataGeneratorTest {

    @Test
    fun generateUserImpactStatsCohort_coversAllTiers() {
        val cohort = SeedDataGenerator.generateUserImpactStatsCohort()
        assertFalse(cohort.isEmpty())

        // Ensure Level 5 (Forest Guardian), Level 4 (Tree), Level 3 (Sapling), Level 2 (Sprout), Level 1 (Seedling) are generated
        val levels = cohort.map { it.currentTierLevel }.toSet()
        assertTrue(levels.contains(5))
        assertTrue(levels.contains(4))
        assertTrue(levels.contains(3))
        assertTrue(levels.contains(2))
        assertTrue(levels.contains(1))
    }

    @Test
    fun generateStaffAggregatesDataset_containsBothMaskedAndUnmaskedCohorts() {
        val dataset = SeedDataGenerator.generateStaffAggregatesDataset("2026-11-23")
        assertFalse(dataset.isEmpty())

        val unmasked = dataset.filter { !it.isMasked }
        val masked = dataset.filter { it.isMasked }

        assertFalse(unmasked.isEmpty())
        assertFalse(masked.isEmpty())

        // All unmasked cohorts must have activeUserCount >= 5
        assertTrue(unmasked.all { it.activeUserCount >= 5 })
        // All masked cohorts must have activeUserCount < 5
        assertTrue(masked.all { it.activeUserCount < 5 })
    }

    @Test
    fun generateSqlSeedScript_isWellFormedSql() {
        val sql = SeedDataGenerator.generateSqlSeedScript()
        assertTrue(sql.contains("insert into public.impact_aggregates"))
        assertTrue(sql.contains("commit;"))
    }
}
