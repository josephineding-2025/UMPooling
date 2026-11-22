package com.fopgang.umpooling.ai

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Result data class for CO2 estimation calculations.
 */
data class Co2EstimateResult(
    val totalTripCo2Kg: Double,
    val personalShareCo2Kg: Double,
    val emissionFactorUsed: Double,
    val distanceKm: Double,
    val passengerCount: Int
)

/**
 * Green Commuter Tier definitions and cumulative milestone thresholds.
 * Thresholds require BOTH trips and cumulative CO2 savings to be met.
 */
enum class GreenCommuterTier(
    val level: Int,
    val badgeName: String,
    val minTrips: Int,
    val minCo2Kg: Double
) {
    NO_BADGE(0, "No Badge", 0, 0.0),
    SEEDLING(1, "Seedling", 1, 0.5),
    SPROUT(2, "Sprout", 5, 5.0),
    SAPLING(3, "Sapling", 15, 20.0),
    TREE(4, "Tree", 40, 60.0),
    FOREST_GUARDIAN(5, "Forest Guardian", 80, 120.0);

    companion object {
        fun fromLevel(level: Int): GreenCommuterTier =
            entries.firstOrNull { it.level == level } ?: NO_BADGE
    }
}

/**
 * Progress information towards the next Green Commuter Tier.
 */
data class TierProgress(
    val currentTier: GreenCommuterTier,
    val nextTier: GreenCommuterTier?,
    val tripsToNext: Int,
    val co2KgToNext: Double,
    val progressFraction: Float
)

/**
 * Interface contract for CO2 calculation, road distance estimation, and Green Commuter Tiers.
 * Shared across Module 1 (Profile/Tiers), Module 2 (Feed/Overlap), Module 3 (Trip Completion), and Module 5 (Dashboard).
 */
interface ICo2CalculationService {
    fun estimateCo2(
        distanceKm: Double,
        passengers: Int,
        vehicleType: String = "car",
        fuelCategory: String = "petrol"
    ): Co2EstimateResult

    fun calculateHaversineDistanceKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double

    fun estimateRoadDistanceKm(
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double
    ): Double

    fun isTripEligibleForCo2(
        status: String,
        confirmedPassengers: Int,
        distanceKm: Double,
        dailyCountedTrips: Int = 0
    ): Boolean

    fun computeGreenCommuterTier(
        completedTrips: Int,
        cumulativeCo2Kg: Double
    ): GreenCommuterTier

    fun computeTierProgress(
        completedTrips: Int,
        cumulativeCo2Kg: Double
    ): TierProgress
}

/**
 * Production implementation of [ICo2CalculationService].
 * Pure Kotlin service with deterministic calculations and zero side-effects.
 */
class Co2CalculationService : ICo2CalculationService {

    companion object {
        /**
         * Standard baseline emission factor: 0.20 kg CO2e / km
         * Source: MITI Sustainability Starter Kit (referencing US EPA 2022 passenger car baseline).
         */
        const val BASELINE_EMISSION_FACTOR = 0.20

        /**
         * Standard road network circuity factor when using straight-line Haversine fallback.
         */
        const val ROAD_CIRCUITY_FACTOR = 1.3

        /**
         * Earth radius in kilometers for Haversine distance calculations.
         */
        private const val EARTH_RADIUS_KM = 6371.0

        /**
         * Minimum eligible trip distance in km for anti-gaming rules.
         */
        const val MIN_ELIGIBLE_DISTANCE_KM = 1.0

        /**
         * Maximum counted trips per user per calendar day.
         */
        const val MAX_DAILY_COUNTED_TRIPS = 2

        /**
         * Singleton instance for convenience and stub mode.
         */
        val instance: Co2CalculationService by lazy { Co2CalculationService() }

        /**
         * Deterministic stub returning default typical campus values.
         * Useful for Members 1, 2, 3, and 5 to preview UI/workflows without depending on live distance API.
         */
        fun stubEstimate(distanceKm: Double = 5.0, passengers: Int = 2): Co2EstimateResult {
            return instance.estimateCo2(distanceKm, passengers)
        }
    }

    /**
     * Resolves the emission factor based on vehicle type and fuel category.
     */
    fun resolveEmissionFactor(vehicleType: String, fuelCategory: String): Double {
        val vType = vehicleType.trim().lowercase()
        val fuel = fuelCategory.trim().lowercase()

        return when {
            vType == "motorcycle" || vType == "bike" -> 0.08
            fuel == "diesel" -> 0.22
            fuel == "hybrid" -> 0.13
            fuel == "ev" || fuel == "electric" -> 0.07
            else -> BASELINE_EMISSION_FACTOR
        }
    }

    /**
     * Estimates CO2 avoided for a carpool trip and the personal share for each occupant.
     *
     * Total Avoided = N * D * E
     * Personal Share = (N * D * E) / (N + 1)
     */
    override fun estimateCo2(
        distanceKm: Double,
        passengers: Int,
        vehicleType: String,
        fuelCategory: String
    ): Co2EstimateResult {
        if (distanceKm <= 0.0 || passengers <= 0) {
            val factor = resolveEmissionFactor(vehicleType, fuelCategory)
            return Co2EstimateResult(
                totalTripCo2Kg = 0.0,
                personalShareCo2Kg = 0.0,
                emissionFactorUsed = factor,
                distanceKm = if (distanceKm < 0.0) 0.0 else distanceKm,
                passengerCount = if (passengers < 0) 0 else passengers
            )
        }

        val factor = resolveEmissionFactor(vehicleType, fuelCategory)
        val totalCo2 = passengers * distanceKm * factor
        val personalShare = totalCo2 / (passengers + 1)

        return Co2EstimateResult(
            totalTripCo2Kg = totalCo2,
            personalShareCo2Kg = personalShare,
            emissionFactorUsed = factor,
            distanceKm = distanceKm,
            passengerCount = passengers
        )
    }

    /**
     * Great-circle distance between two latitude/longitude pairs using Haversine formula in km.
     */
    override fun calculateHaversineDistanceKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(rLat1) * cos(rLat2) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS_KM * c
    }

    /**
     * Estimates actual road distance by applying the standard 1.3 road circuity factor to Haversine distance.
     */
    override fun estimateRoadDistanceKm(
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double
    ): Double {
        val haversineKm = calculateHaversineDistanceKm(originLat, originLng, destLat, destLng)
        return haversineKm * ROAD_CIRCUITY_FACTOR
    }

    /**
     * Anti-gaming and counting eligibility rule:
     * - Trip status must be "completed"
     * - At least 1 confirmed passenger
     * - Trip distance >= 1.0 km
     * - Under daily cap (max 2 trips per user per calendar day)
     */
    override fun isTripEligibleForCo2(
        status: String,
        confirmedPassengers: Int,
        distanceKm: Double,
        dailyCountedTrips: Int
    ): Boolean {
        if (!status.equals("completed", ignoreCase = true)) return false
        if (confirmedPassengers < 1) return false
        if (distanceKm < MIN_ELIGIBLE_DISTANCE_KM) return false
        if (dailyCountedTrips >= MAX_DAILY_COUNTED_TRIPS) return false
        return true
    }

    /**
     * Computes the user's Green Commuter Tier from cumulative career statistics.
     * Both the trips threshold AND the CO2 savings threshold must be met.
     */
    override fun computeGreenCommuterTier(
        completedTrips: Int,
        cumulativeCo2Kg: Double
    ): GreenCommuterTier {
        val safeTrips = if (completedTrips < 0) 0 else completedTrips
        val safeCo2 = if (cumulativeCo2Kg < 0.0) 0.0 else cumulativeCo2Kg

        return when {
            safeTrips >= GreenCommuterTier.FOREST_GUARDIAN.minTrips &&
                    safeCo2 >= GreenCommuterTier.FOREST_GUARDIAN.minCo2Kg -> GreenCommuterTier.FOREST_GUARDIAN

            safeTrips >= GreenCommuterTier.TREE.minTrips &&
                    safeCo2 >= GreenCommuterTier.TREE.minCo2Kg -> GreenCommuterTier.TREE

            safeTrips >= GreenCommuterTier.SAPLING.minTrips &&
                    safeCo2 >= GreenCommuterTier.SAPLING.minCo2Kg -> GreenCommuterTier.SAPLING

            safeTrips >= GreenCommuterTier.SPROUT.minTrips &&
                    safeCo2 >= GreenCommuterTier.SPROUT.minCo2Kg -> GreenCommuterTier.SPROUT

            safeTrips >= GreenCommuterTier.SEEDLING.minTrips &&
                    safeCo2 >= GreenCommuterTier.SEEDLING.minCo2Kg -> GreenCommuterTier.SEEDLING

            else -> GreenCommuterTier.NO_BADGE
        }
    }

    /**
     * Calculates progress details towards the next Green Commuter Tier.
     */
    override fun computeTierProgress(
        completedTrips: Int,
        cumulativeCo2Kg: Double
    ): TierProgress {
        val safeTrips = if (completedTrips < 0) 0 else completedTrips
        val safeCo2 = if (cumulativeCo2Kg < 0.0) 0.0 else cumulativeCo2Kg
        val current = computeGreenCommuterTier(safeTrips, safeCo2)

        val next = when (current) {
            GreenCommuterTier.NO_BADGE -> GreenCommuterTier.SEEDLING
            GreenCommuterTier.SEEDLING -> GreenCommuterTier.SPROUT
            GreenCommuterTier.SPROUT -> GreenCommuterTier.SAPLING
            GreenCommuterTier.SAPLING -> GreenCommuterTier.TREE
            GreenCommuterTier.TREE -> GreenCommuterTier.FOREST_GUARDIAN
            GreenCommuterTier.FOREST_GUARDIAN -> null
        }

        if (next == null) {
            return TierProgress(
                currentTier = current,
                nextTier = null,
                tripsToNext = 0,
                co2KgToNext = 0.0,
                progressFraction = 1.0f
            )
        }

        val tripsNeeded = (next.minTrips - safeTrips).coerceAtLeast(0)
        val co2Needed = (next.minCo2Kg - safeCo2).coerceAtLeast(0.0)

        val prevTrips = current.minTrips
        val prevCo2 = current.minCo2Kg

        val tripFraction = if (next.minTrips > prevTrips) {
            ((safeTrips - prevTrips).toFloat() / (next.minTrips - prevTrips)).coerceIn(0.0f, 1.0f)
        } else 1.0f

        val co2Fraction = if (next.minCo2Kg > prevCo2) {
            ((safeCo2 - prevCo2).toFloat() / (next.minCo2Kg - prevCo2).toFloat()).coerceIn(0.0f, 1.0f)
        } else 1.0f

        // Progress is determined by the limiting factor
        val combinedFraction = minOf(tripFraction, co2Fraction)

        return TierProgress(
            currentTier = current,
            nextTier = next,
            tripsToNext = tripsNeeded,
            co2KgToNext = co2Needed,
            progressFraction = combinedFraction
        )
    }
}
