package com.fopgang.umpooling.profile

import com.fopgang.umpooling.ai.Co2CalculationService
import com.fopgang.umpooling.ai.GreenCommuterTier
import com.fopgang.umpooling.data.repository.PersonalImpact
import java.util.Locale

/** What the profile header shows: totals, tier and how far the next tier is (FR1.3). */
data class ImpactSummary(
    val trips: Int,
    val co2Kg: Double,
    val rating: Double?,
    val tier: GreenCommuterTier,
    val next: GreenCommuterTier?,
    val tripsToNext: Int,
    val co2KgToNext: Double,
) {
    val tripsText: String get() = trips.toString()
    val co2Text: String get() = String.format(Locale.US, "%.1f", co2Kg)
    /** A dash until the user has been rated (Member 6's ratings feed trust_rating). */
    val ratingText: String get() = rating?.let { String.format(Locale.US, "%.1f", it) } ?: "-"

    companion object {
        /**
         * Totals come from user_impact_stats, written by the server when a trip completes.
         * The server never lowers a tier, so the higher of the stored tier and the tier the
         * totals earn is shown. Thresholds are Member 4's GreenCommuterTier values.
         */
        fun from(impact: PersonalImpact?, trustRating: Double?): ImpactSummary {
            val trips = (impact?.totalTrips ?: 0).coerceAtLeast(0)
            val co2 = (impact?.co2Kg ?: 0.0).coerceAtLeast(0.0)
            val earned = Co2CalculationService.instance.computeGreenCommuterTier(trips, co2)
            val stored = GreenCommuterTier.fromLevel(impact?.tierLevel ?: 0)
            val tier = if (stored.level > earned.level) stored else earned
            val next = GreenCommuterTier.entries.firstOrNull { it.level == tier.level + 1 }
            return ImpactSummary(
                trips = trips,
                co2Kg = co2,
                rating = trustRating?.takeIf { it > 0.0 },
                tier = tier,
                next = next,
                tripsToNext = next?.let { (it.minTrips - trips).coerceAtLeast(0) } ?: 0,
                co2KgToNext = next?.let { (it.minCo2Kg - co2).coerceAtLeast(0.0) } ?: 0.0,
            )
        }
    }
}
