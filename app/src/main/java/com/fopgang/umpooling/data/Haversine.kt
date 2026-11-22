package com.fopgang.umpooling.data

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Spatial distance calculations and road distance estimation for Module 2 (FR2.1, FR2.2, FR2.3).
 */
object Haversine {
    private const val EARTH_RADIUS_KM = 6371.0
    private const val ROAD_CIRCUITY_FACTOR = 1.3 // Standard detour/circuity factor for urban road networks

    /**
     * Calculates great-circle distance between two GPS coordinates using the Haversine formula.
     */
    fun haversineKm(
        lat1: Double, lng1: Double,
        lat2: Double, lng2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_KM * c
    }

    /**
     * Estimates actual driving/road distance by applying the standard 1.3x road circuity factor.
     * Rounded to 2 decimal places.
     */
    fun estimateRoadDistanceKm(
        lat1: Double, lng1: Double,
        lat2: Double, lng2: Double
    ): Double {
        val direct = haversineKm(lat1, lng1, lat2, lng2)
        return round(direct * ROAD_CIRCUITY_FACTOR * 100.0) / 100.0
    }

    /**
     * Checks if two coordinates are within a specified radius (in kilometers).
     */
    fun isWithinRadius(
        lat1: Double, lng1: Double,
        lat2: Double, lng2: Double,
        radiusKm: Double
    ): Boolean {
        return haversineKm(lat1, lng1, lat2, lng2) <= radiusKm
    }
}
