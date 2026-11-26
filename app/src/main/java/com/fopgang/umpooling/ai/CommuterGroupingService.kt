package com.fopgang.umpooling.ai

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Commuter request candidate evaluated for pooling groups.
 */
data class CommuterRideCandidate(
    val id: String,
    val userId: String,
    val userName: String = "Commuter",
    val pickupLat: Double,
    val pickupLng: Double,
    val pickupName: String,
    val destLat: Double,
    val destLng: Double,
    val destName: String,
    val preferredTime: LocalTime,
    val seatsRequested: Int = 1
)

/**
 * Recommended pickup group aggregating nearby commuters (FR5.2).
 */
data class PickupGroupRecommendation(
    val groupId: String,
    val clusterPickupName: String,
    val clusterPickupLat: Double,
    val clusterPickupLng: Double,
    val destinationName: String,
    val destinationLat: Double,
    val destinationLng: Double,
    val recommendedWindowStart: LocalTime,
    val recommendedWindowEnd: LocalTime,
    val recommendedWindowLabel: String,
    val riderCount: Int,
    val totalSeatsRequested: Int,
    val estimatedCostPerRiderSen: Int,
    val estimatedCo2SavedKg: Double,
    val matchScorePercentage: Int,
    val members: List<CommuterRideCandidate>
)

/**
 * Service providing spatial clustering (DBSCAN) and multi-factor compatibility
 * grouping for UM carpoolers from the same residential area (FR5.2).
 */
class CommuterGroupingService(
    private val co2Service: ICo2CalculationService = Co2CalculationService.instance
) {

    companion object {
        /**
         * DBSCAN epsilon radius for pickup proximity: 500 metres (0.5 km).
         */
        const val PICKUP_EPSILON_KM = 0.50

        /**
         * Minimum sample count to form a dense DBSCAN cluster.
         */
        const val MIN_SAMPLES = 2

        /**
         * Maximum allowed distance between destinations to share drop-off: 300 metres (0.3 km).
         */
        const val DESTINATION_MAX_DISTANCE_KM = 0.30

        /**
         * Maximum time gap between preferred departure times: 15 minutes.
         */
        const val MAX_TIME_DIFFERENCE_MINUTES = 15L

        /**
         * Standard carpool capacity for grouping recommendations.
         */
        const val MAX_CARPOOL_SEATS = 4

        /**
         * Baseline per-vehicle cost cap in sen (RM 5.00).
         */
        const val MAX_OFFER_COST_SEN = 500

        val instance: CommuterGroupingService by lazy { CommuterGroupingService() }
    }

    /**
     * Finds and groups compatible ride candidates into high-occupancy carpool clusters.
     */
    fun groupCommuters(candidates: List<CommuterRideCandidate>): List<PickupGroupRecommendation> {
        if (candidates.size < MIN_SAMPLES) return emptyList()

        // 1. Cluster pickup locations using DBSCAN with Haversine distance
        val pickupClusters = clusterPointsDbscan(candidates, PICKUP_EPSILON_KM, MIN_SAMPLES)
        val recommendations = mutableListOf<PickupGroupRecommendation>()
        var groupCounter = 1

        for (cluster in pickupClusters) {
            // 2. Sub-group by destination proximity (<= 300m)
            val destinationSubgroups = partitionByDestination(cluster, DESTINATION_MAX_DISTANCE_KM)

            for (destGroup in destinationSubgroups) {
                // 3. Sub-group by departure time window (<= 15 minutes) and capacity (<= 4 seats)
                val timeCompatibleGroups = partitionByTimeAndCapacity(destGroup, MAX_TIME_DIFFERENCE_MINUTES, MAX_CARPOOL_SEATS)

                for (finalGroup in timeCompatibleGroups) {
                    if (finalGroup.size >= MIN_SAMPLES) {
                        recommendations.add(buildRecommendation("group-$groupCounter", finalGroup))
                        groupCounter++
                    }
                }
            }
        }

        return recommendations.sortedByDescending { it.matchScorePercentage }
    }

    /**
     * DBSCAN clustering implementation on pickup coordinates.
     */
    internal fun clusterPointsDbscan(
        points: List<CommuterRideCandidate>,
        epsKm: Double,
        minPts: Int
    ): List<List<CommuterRideCandidate>> {
        val clusters = mutableListOf<MutableList<CommuterRideCandidate>>()
        val visited = mutableSetOf<String>()
        val clustered = mutableSetOf<String>()

        for (point in points) {
            if (visited.contains(point.id)) continue
            visited.add(point.id)

            val neighbors = getNeighbors(point, points, epsKm)
            if (neighbors.size < minPts) {
                // Noise / isolated commuter for now
                continue
            }

            val currentCluster = mutableListOf(point)
            clustered.add(point.id)

            val queue = ArrayDeque(neighbors.filter { it.id != point.id })
            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                if (!visited.contains(current.id)) {
                    visited.add(current.id)
                    val currentNeighbors = getNeighbors(current, points, epsKm)
                    if (currentNeighbors.size >= minPts) {
                        for (neighbor in currentNeighbors) {
                            if (!queue.contains(neighbor) && !clustered.contains(neighbor.id)) {
                                queue.add(neighbor)
                            }
                        }
                    }
                }
                if (!clustered.contains(current.id)) {
                    clustered.add(current.id)
                    currentCluster.add(current)
                }
            }

            clusters.add(currentCluster)
        }

        return clusters
    }

    private fun getNeighbors(
        target: CommuterRideCandidate,
        all: List<CommuterRideCandidate>,
        epsKm: Double
    ): List<CommuterRideCandidate> {
        return all.filter {
            co2Service.calculateHaversineDistanceKm(
                target.pickupLat, target.pickupLng,
                it.pickupLat, it.pickupLng
            ) <= epsKm
        }
    }

    /**
     * Partitions candidates into groups whose destinations are within [maxDestKm].
     */
    internal fun partitionByDestination(
        candidates: List<CommuterRideCandidate>,
        maxDestKm: Double
    ): List<List<CommuterRideCandidate>> {
        val groups = mutableListOf<MutableList<CommuterRideCandidate>>()

        for (candidate in candidates) {
            var matchedGroup = groups.firstOrNull { group ->
                val centroidLat = group.map { it.destLat }.average()
                val centroidLng = group.map { it.destLng }.average()
                co2Service.calculateHaversineDistanceKm(
                    candidate.destLat, candidate.destLng,
                    centroidLat, centroidLng
                ) <= maxDestKm
            }

            if (matchedGroup == null) {
                matchedGroup = mutableListOf()
                groups.add(matchedGroup)
            }
            matchedGroup.add(candidate)
        }

        return groups.filter { it.isNotEmpty() }
    }

    /**
     * Partitions candidates by departure time proximity (<= [maxDiffMinutes]) and capacity (<= [maxSeats]).
     */
    internal fun partitionByTimeAndCapacity(
        candidates: List<CommuterRideCandidate>,
        maxDiffMinutes: Long,
        maxSeats: Int
    ): List<List<CommuterRideCandidate>> {
        val sorted = candidates.sortedBy { it.preferredTime }
        val result = mutableListOf<MutableList<CommuterRideCandidate>>()

        for (candidate in sorted) {
            var added = false
            for (group in result) {
                val groupSeats = group.sumOf { it.seatsRequested }
                if (groupSeats + candidate.seatsRequested <= maxSeats) {
                    val groupEarliest = group.minOf { it.preferredTime }
                    val groupLatest = group.maxOf { it.preferredTime }
                    val diffWithEarliest = abs(ChronoUnit.MINUTES.between(candidate.preferredTime, groupEarliest))
                    val diffWithLatest = abs(ChronoUnit.MINUTES.between(candidate.preferredTime, groupLatest))

                    if (diffWithEarliest <= maxDiffMinutes && diffWithLatest <= maxDiffMinutes) {
                        group.add(candidate)
                        added = true
                        break
                    }
                }
            }

            if (!added) {
                result.add(mutableListOf(candidate))
            }
        }

        return result
    }

    /**
     * Constructs a [PickupGroupRecommendation] with optimal departure window and metrics.
     */
    private fun buildRecommendation(
        groupId: String,
        members: List<CommuterRideCandidate>
    ): PickupGroupRecommendation {
        val avgPickupLat = members.map { it.pickupLat }.average()
        val avgPickupLng = members.map { it.pickupLng }.average()
        val avgDestLat = members.map { it.destLat }.average()
        val avgDestLng = members.map { it.destLng }.average()

        // Most frequent or first representative names
        val pickupName = members.groupBy { it.pickupName }.maxByOrNull { it.value.size }?.key
            ?: members.first().pickupName
        val destName = members.groupBy { it.destName }.maxByOrNull { it.value.size }?.key
            ?: members.first().destName

        // Median time window calculation (+/- 10 minutes from median)
        val sortedMinutes = members.map { it.preferredTime.hour * 60 + it.preferredTime.minute }.sorted()
        val medianMinutes = sortedMinutes[sortedMinutes.size / 2]
        val medianTime = LocalTime.of(medianMinutes / 60, medianMinutes % 60)

        val windowStart = medianTime.minusMinutes(10)
        val windowEnd = medianTime.plusMinutes(10)

        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val windowLabel = "${windowStart.format(timeFormatter)}–${windowEnd.format(timeFormatter)}"

        val totalRiders = members.size
        val totalSeats = members.sumOf { it.seatsRequested }

        // Distance & CO2 calculation
        val roadDistKm = co2Service.estimateRoadDistanceKm(avgPickupLat, avgPickupLng, avgDestLat, avgDestLng)
        val co2Result = co2Service.estimateCo2(roadDistKm, totalSeats)

        // Cost share: RM 5.00 cap divided equally among riders
        val effectiveRiders = totalSeats.coerceAtLeast(1)
        val costPerRiderSen = (MAX_OFFER_COST_SEN / (effectiveRiders + 1)).coerceAtLeast(100)

        // Match score: 85% base + bonus for fullness
        val fullnessBonus = (totalSeats * 3).coerceAtMost(10)
        val timeProximityPenalty = (sortedMinutes.last() - sortedMinutes.first()) / 2
        val matchScore = (85 + fullnessBonus - timeProximityPenalty).coerceIn(70, 98)

        return PickupGroupRecommendation(
            groupId = groupId,
            clusterPickupName = pickupName,
            clusterPickupLat = avgPickupLat,
            clusterPickupLng = avgPickupLng,
            destinationName = destName,
            destinationLat = avgDestLat,
            destinationLng = avgDestLng,
            recommendedWindowStart = windowStart,
            recommendedWindowEnd = windowEnd,
            recommendedWindowLabel = windowLabel,
            riderCount = totalRiders,
            totalSeatsRequested = totalSeats,
            estimatedCostPerRiderSen = costPerRiderSen,
            estimatedCo2SavedKg = ((co2Result.totalTripCo2Kg * 10).roundToInt()) / 10.0,
            matchScorePercentage = matchScore,
            members = members
        )
    }

    /**
     * Deterministic sample pickup groups for offline preview and testing.
     */
    fun getSamplePickupGroups(originArea: String = "Kolej Kediaman 11"): List<PickupGroupRecommendation> {
        val isTiara = originArea.contains("Tiara", ignoreCase = true)
        val isPacific = originArea.contains("Pacific", ignoreCase = true)

        val (origin, origLat, origLng) = when {
            isTiara -> Triple("Tiara Damansara Guardhouse", 3.1289, 101.6375)
            isPacific -> Triple("Pacific Star Main Lobby", 3.1189, 101.6378)
            else -> Triple("Kolej Kediaman 11 Foyer", 3.1215, 101.6521)
        }

        val candidates = listOf(
            CommuterRideCandidate("c1", "u1", "Sarah", origLat, origLng, origin, 3.1284, 101.6505, "FSKTM", LocalTime.of(8, 10), 1),
            CommuterRideCandidate("c2", "u2", "Ahmad", origLat + 0.001, origLng + 0.001, origin, 3.1284, 101.6505, "FSKTM", LocalTime.of(8, 15), 1),
            CommuterRideCandidate("c3", "u3", "Wei Ling", origLat - 0.001, origLng, origin, 3.1284, 101.6505, "FSKTM", LocalTime.of(8, 20), 1)
        )

        return groupCommuters(candidates)
    }
}
