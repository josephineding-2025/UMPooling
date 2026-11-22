package com.fopgang.umpooling.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request payload sent to parse-ride-request Supabase Edge Function.
 */
@Serializable
data class ParseRideRequestPayload(
    @SerialName("query") val query: String,
    @SerialName("current_datetime") val currentDatetime: String
)

/**
 * Response payload returned from parse-ride-request Supabase Edge Function.
 */
@Serializable
data class ParsedRideResponse(
    @SerialName("origin") val origin: String? = null,
    @SerialName("destination") val destination: String? = null,
    @SerialName("departure_date") val departureDate: String? = null,
    @SerialName("departure_time") val departureTime: String? = null,
    @SerialName("seats") val seats: Int = 1,
    @SerialName("missing_fields") val missingFields: List<String> = emptyList(),
    @SerialName("ambiguous_fields") val ambiguousFields: List<String> = emptyList(),
    @SerialName("confidence") val confidence: Double = 0.0,
    @SerialName("raw_query") val rawQuery: String = ""
)

/**
 * UI-ready representation of the parsed ride request for Jetpack Compose forms.
 * Enables highlighting of missing/ambiguous fields and graceful fallback to manual entry.
 */
data class RideParserUiState(
    val origin: String? = null,
    val destination: String? = null,
    val departureDate: String? = null,
    val departureTime: String? = null,
    val seats: Int = 1,
    val missingFields: List<String> = emptyList(),
    val ambiguousFields: List<String> = emptyList(),
    val confidence: Double = 0.0,
    val isSuccess: Boolean = true,
    val requiresManualInput: Boolean = false,
    val errorMessage: String? = null
) {
    val isOriginMissing: Boolean get() = origin.isNullOrBlank() || missingFields.contains("origin")
    val isDestinationMissing: Boolean get() = destination.isNullOrBlank() || missingFields.contains("destination")
    val isTimeMissing: Boolean get() = departureTime.isNullOrBlank() || missingFields.contains("departure_time")
}
