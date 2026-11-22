package com.fopgang.umpooling.ai

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class Co2CalculationServiceTest {

    private lateinit var service: Co2CalculationService

    @Before
    fun setUp() {
        service = Co2CalculationService()
    }

    // ==========================================
    // 1. Standard CO2 Calculations
    // ==========================================

    @Test
    fun standardCalculation_twoPassengersTenKm_computesCorrectCo2() {
        // N = 2 passengers, D = 10.0 km, E = 0.20 kg CO2e/km
        // Total Avoided = 2 * 10 * 0.20 = 4.00 kg
        // Personal Share = 4.00 / (2 + 1) = 1.3333... kg
        val result = service.estimateCo2(distanceKm = 10.0, passengers = 2)

        assertEquals(4.0, result.totalTripCo2Kg, 0.001)
        assertEquals(1.3333, result.personalShareCo2Kg, 0.001)
        assertEquals(0.20, result.emissionFactorUsed, 0.0001)
        assertEquals(10.0, result.distanceKm, 0.001)
        assertEquals(2, result.passengerCount)
    }

    @Test
    fun standardCalculation_onePassengerFiveKm_computesCorrectCo2() {
        // N = 1, D = 5.0, E = 0.20 -> Total = 1.0 kg, Personal Share = 0.5 kg
        val result = service.estimateCo2(distanceKm = 5.0, passengers = 1)

        assertEquals(1.0, result.totalTripCo2Kg, 0.001)
        assertEquals(0.5, result.personalShareCo2Kg, 0.001)
    }

    @Test
    fun standardCalculation_threePassengersTwentyKm_computesCorrectCo2() {
        // N = 3, D = 20.0, E = 0.20 -> Total = 12.0 kg, Personal Share = 12.0 / 4 = 3.0 kg
        val result = service.estimateCo2(distanceKm = 20.0, passengers = 3)

        assertEquals(12.0, result.totalTripCo2Kg, 0.001)
        assertEquals(3.0, result.personalShareCo2Kg, 0.001)
    }

    @Test
    fun fuelCategoryMultipliers_resolvesExpectedFactors() {
        // Diesel: 0.22
        val dieselResult = service.estimateCo2(10.0, 1, "car", "diesel")
        assertEquals(2.2, dieselResult.totalTripCo2Kg, 0.001)

        // Hybrid: 0.13
        val hybridResult = service.estimateCo2(10.0, 1, "car", "hybrid")
        assertEquals(1.3, hybridResult.totalTripCo2Kg, 0.001)

        // EV: 0.07
        val evResult = service.estimateCo2(10.0, 1, "car", "ev")
        assertEquals(0.7, evResult.totalTripCo2Kg, 0.001)

        // Motorcycle: 0.08
        val motoResult = service.estimateCo2(10.0, 1, "motorcycle", "petrol")
        assertEquals(0.8, motoResult.totalTripCo2Kg, 0.001)
    }

    // ==========================================
    // 2. Edge Cases and Zero/Negative Inputs
    // ==========================================

    @Test
    fun edgeCases_zeroPassengers_returnsZeroSavings() {
        val result = service.estimateCo2(distanceKm = 10.0, passengers = 0)
        assertEquals(0.0, result.totalTripCo2Kg, 0.0)
        assertEquals(0.0, result.personalShareCo2Kg, 0.0)
    }

    @Test
    fun edgeCases_negativePassengers_returnsZeroSavings() {
        val result = service.estimateCo2(distanceKm = 10.0, passengers = -2)
        assertEquals(0.0, result.totalTripCo2Kg, 0.0)
        assertEquals(0.0, result.personalShareCo2Kg, 0.0)
    }

    @Test
    fun edgeCases_zeroDistance_returnsZeroSavings() {
        val result = service.estimateCo2(distanceKm = 0.0, passengers = 2)
        assertEquals(0.0, result.totalTripCo2Kg, 0.0)
        assertEquals(0.0, result.personalShareCo2Kg, 0.0)
    }

    @Test
    fun edgeCases_negativeDistance_returnsZeroSavings() {
        val result = service.estimateCo2(distanceKm = -5.0, passengers = 2)
        assertEquals(0.0, result.totalTripCo2Kg, 0.0)
        assertEquals(0.0, result.personalShareCo2Kg, 0.0)
    }

    @Test
    fun stubEstimate_returnsValidDeterministicResult() {
        val stub = Co2CalculationService.stubEstimate()
        assertTrue(stub.totalTripCo2Kg > 0.0)
        assertTrue(stub.personalShareCo2Kg > 0.0)
        assertEquals(Co2CalculationService.BASELINE_EMISSION_FACTOR, stub.emissionFactorUsed, 0.0001)
    }

    // ==========================================
    // 3. Haversine & Circuity Distance Helper
    // ==========================================

    @Test
    fun haversineDistance_samePoint_returnsZero() {
        val distance = service.calculateHaversineDistanceKm(3.1284, 101.6505, 3.1284, 101.6505)
        assertEquals(0.0, distance, 0.0001)
    }

    @Test
    fun haversineDistance_tiaraToFsktm_computesRealisticDistance() {
        // Tiara Damansara approx: 3.1289, 101.6375
        // FSKTM approx: 3.1284, 101.6505
        // Distance is ~1.44 km straight-line
        val straightLine = service.calculateHaversineDistanceKm(3.1289, 101.6375, 3.1284, 101.6505)
        assertTrue("Straight line distance should be ~1.44 km", straightLine in 1.3..1.6)

        val roadDist = service.estimateRoadDistanceKm(3.1289, 101.6375, 3.1284, 101.6505)
        assertEquals(straightLine * 1.3, roadDist, 0.0001)
        assertTrue("Estimated road distance should be ~1.8 - 2.0 km", roadDist in 1.7..2.1)
    }

    // ==========================================
    // 4. Anti-Gaming & Counting Eligibility Rules
    // ==========================================

    @Test
    fun eligibility_completedTripWithValidParams_isEligible() {
        assertTrue(service.isTripEligibleForCo2(
            status = "completed",
            confirmedPassengers = 1,
            distanceKm = 1.0,
            dailyCountedTrips = 0
        ))
    }

    @Test
    fun eligibility_cancelledTrip_isNotEligible() {
        assertFalse(service.isTripEligibleForCo2(
            status = "cancelled",
            confirmedPassengers = 2,
            distanceKm = 5.0,
            dailyCountedTrips = 0
        ))
    }

    @Test
    fun eligibility_inProgressOrScheduled_isNotEligible() {
        assertFalse(service.isTripEligibleForCo2("in_progress", 1, 5.0))
        assertFalse(service.isTripEligibleForCo2("scheduled", 1, 5.0))
    }

    @Test
    fun eligibility_zeroPassengers_isNotEligible() {
        assertFalse(service.isTripEligibleForCo2(
            status = "completed",
            confirmedPassengers = 0,
            distanceKm = 5.0
        ))
    }

    @Test
    fun eligibility_distanceLessThanOneKm_isNotEligible() {
        assertFalse(service.isTripEligibleForCo2(
            status = "completed",
            confirmedPassengers = 2,
            distanceKm = 0.99
        ))
    }

    @Test
    fun eligibility_dailyCapExceeded_isNotEligible() {
        // Max 2 counted trips per day
        assertFalse(service.isTripEligibleForCo2(
            status = "completed",
            confirmedPassengers = 1,
            distanceKm = 5.0,
            dailyCountedTrips = 2
        ))
        assertFalse(service.isTripEligibleForCo2(
            status = "completed",
            confirmedPassengers = 1,
            distanceKm = 5.0,
            dailyCountedTrips = 3
        ))
    }

    // ==========================================
    // 5. Green Commuter Tier Boundary Tests
    // ==========================================

    @Test
    fun commuterTier_level0_noBadge() {
        // Below Level 1 (needs 1 trip AND 0.5 kg)
        assertEquals(GreenCommuterTier.NO_BADGE, service.computeGreenCommuterTier(0, 0.0))
        assertEquals(GreenCommuterTier.NO_BADGE, service.computeGreenCommuterTier(0, 10.0)) // 0 trips
        assertEquals(GreenCommuterTier.NO_BADGE, service.computeGreenCommuterTier(1, 0.49)) // insufficient CO2
    }

    @Test
    fun commuterTier_level1_seedling() {
        // Threshold: 1 trip, 0.5 kg
        assertEquals(GreenCommuterTier.SEEDLING, service.computeGreenCommuterTier(1, 0.5))
        assertEquals(GreenCommuterTier.SEEDLING, service.computeGreenCommuterTier(4, 4.9)) // Below Sprout
    }

    @Test
    fun commuterTier_level2_sprout() {
        // Threshold: 5 trips, 5.0 kg
        assertEquals(GreenCommuterTier.SPROUT, service.computeGreenCommuterTier(5, 5.0))
        assertEquals(GreenCommuterTier.SPROUT, service.computeGreenCommuterTier(14, 19.9)) // Below Sapling
    }

    @Test
    fun commuterTier_level3_sapling() {
        // Threshold: 15 trips, 20.0 kg
        assertEquals(GreenCommuterTier.SAPLING, service.computeGreenCommuterTier(15, 20.0))
        assertEquals(GreenCommuterTier.SAPLING, service.computeGreenCommuterTier(39, 59.9)) // Below Tree
    }

    @Test
    fun commuterTier_level4_tree() {
        // Threshold: 40 trips, 60.0 kg
        assertEquals(GreenCommuterTier.TREE, service.computeGreenCommuterTier(40, 60.0))
        assertEquals(GreenCommuterTier.TREE, service.computeGreenCommuterTier(79, 119.9)) // Below Forest Guardian
    }

    @Test
    fun commuterTier_level5_forestGuardian() {
        // Threshold: 80 trips, 120.0 kg
        assertEquals(GreenCommuterTier.FOREST_GUARDIAN, service.computeGreenCommuterTier(80, 120.0))
        assertEquals(GreenCommuterTier.FOREST_GUARDIAN, service.computeGreenCommuterTier(150, 300.0))
    }

    @Test
    fun commuterTier_asymmetricThresholds_holdsHighestWhereBothMet() {
        // 100 trips but only 19.9 kg CO2 -> SPROUT (fails 20.0 kg CO2 requirement of Sapling)
        assertEquals(GreenCommuterTier.SPROUT, service.computeGreenCommuterTier(100, 19.9))

        // 14 trips and 100.0 kg CO2 -> SPROUT (fails 15 trips requirement of Sapling)
        assertEquals(GreenCommuterTier.SPROUT, service.computeGreenCommuterTier(14, 100.0))

        // Negative defensive check
        assertEquals(GreenCommuterTier.NO_BADGE, service.computeGreenCommuterTier(-1, -5.0))
    }

    @Test
    fun tierProgress_calculatesCorrectProgressToNext() {
        // User with 0 trips, 0.0 kg -> Progress to SEEDLING (1 trip, 0.5 kg)
        val progress0 = service.computeTierProgress(0, 0.0)
        assertEquals(GreenCommuterTier.NO_BADGE, progress0.currentTier)
        assertEquals(GreenCommuterTier.SEEDLING, progress0.nextTier)
        assertEquals(1, progress0.tripsToNext)
        assertEquals(0.5, progress0.co2KgToNext, 0.001)
        assertEquals(0.0f, progress0.progressFraction, 0.01f)

        // Halfway to Seedling: 1 trip, 0.25 kg -> Current is still NO_BADGE
        val progressHalf = service.computeTierProgress(1, 0.25)
        assertEquals(GreenCommuterTier.NO_BADGE, progressHalf.currentTier)
        assertEquals(GreenCommuterTier.SEEDLING, progressHalf.nextTier)
        assertEquals(0.5f, progressHalf.progressFraction, 0.01f)

        // At max tier (Forest Guardian) -> No next tier
        val progressMax = service.computeTierProgress(85, 130.0)
        assertEquals(GreenCommuterTier.FOREST_GUARDIAN, progressMax.currentTier)
        assertNull(progressMax.nextTier)
        assertEquals(1.0f, progressMax.progressFraction, 0.01f)
    }
}
