package com.fopgang.umpooling.ai

import com.fopgang.umpooling.BuildConfig
import com.fopgang.umpooling.SupabaseClientProvider
import com.fopgang.umpooling.data.repository.suspendRunCatching
import io.github.jan.supabase.functions.functions
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Repository for invoking the Supabase parse-ride-request Edge Function (FR5.1).
 * Maps server-side LLM extractions to UI-ready states and guarantees safe manual fallback.
 */
class RideParserRepository {

    private val configured: Boolean
        get() = BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Parses a natural-language commuting query into structured ride fields.
     * Guaranteed never to throw; returns Result with [RideParserUiState] or graceful fallback.
     */
    suspend fun parseRideRequest(
        query: String,
        currentDatetime: String? = null
    ): Result<RideParserUiState> = suspendRunCatching {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) {
            return@suspendRunCatching createManualFallbackState(
                rawQuery = "",
                reason = "Please enter or speak your ride request."
            )
        }

        if (!configured) {
            // Local development / offline mode: return heuristic manual fallback without crashing
            return@suspendRunCatching fallbackHeuristicParse(trimmedQuery, currentDatetime)
        }

        val refTime = currentDatetime ?: DateTimeFormatter.ISO_INSTANT.format(Instant.now())
        val payload = ParseRideRequestPayload(query = trimmedQuery, currentDatetime = refTime)

        try {
            val httpResponse = SupabaseClientProvider.client.functions.invoke(
                function = "parse-ride-request",
                body = payload
            )
            val responseText = httpResponse.bodyAsText()
            val response = json.decodeFromString<ParsedRideResponse>(responseText)

            val requiresManual = response.missingFields.isNotEmpty() ||
                    response.ambiguousFields.isNotEmpty() ||
                    response.confidence < 0.70

            RideParserUiState(
                origin = response.origin,
                destination = response.destination,
                departureDate = response.departureDate,
                departureTime = response.departureTime,
                seats = response.seats,
                missingFields = response.missingFields,
                ambiguousFields = response.ambiguousFields,
                confidence = response.confidence,
                isSuccess = true,
                requiresManualInput = requiresManual,
                errorMessage = null
            )
        } catch (e: Exception) {
            // Edge Function call failed (network, quota, etc.) -> graceful fallback to manual entry
            fallbackHeuristicParse(trimmedQuery, refTime, errorMsg = e.message)
        }
    }

    /**
     * Creates a fallback UI state allowing manual completion in Compose form.
     */
    fun createManualFallbackState(
        rawQuery: String = "",
        reason: String? = null
    ): RideParserUiState {
        return RideParserUiState(
            origin = null,
            destination = null,
            departureDate = null,
            departureTime = null,
            seats = 1,
            missingFields = listOf("origin", "destination", "departure_time"),
            ambiguousFields = emptyList(),
            confidence = 0.0,
            isSuccess = false,
            requiresManualInput = true,
            errorMessage = reason ?: "Unable to automatically parse ride. Please fill in details manually."
        )
    }

    /**
     * Local client heuristic for zero-config offline environments.
     */
    fun fallbackHeuristicParse(
        query: String,
        refTime: String? = null,
        errorMsg: String? = null
    ): RideParserUiState {
        val lower = query.lowercase()
        val missing = mutableListOf<String>()

        var origin: String? = null
        var destination: String? = null
        var seats = 1

        val seatRegex = Regex("(\\d+)\\s*(?:seat|pax|orang|people|kerusi)")
        seatRegex.find(lower)?.let { match ->
            seats = match.groupValues[1].toIntOrNull()?.coerceIn(1, 6) ?: 1
        }

        if (lower.contains("tiara")) origin = "Tiara Damansara"
        else if (lower.contains("pacific star")) origin = "Pacific Star"
        else if (lower.contains("vista")) origin = "Vista Angkasa"
        else if (lower.contains("kk12")) origin = "KK12"
        else if (lower.contains("pantai hillpark")) origin = "Pantai Hillpark"

        if (lower.contains("fsktm") || lower.contains("computer science")) destination = "FSKTM"
        else if (lower.contains("engineering") || lower.contains("kejuruteraan")) destination = "Faculty of Engineering"
        else if (lower.contains("library") || lower.contains("perpustakaan")) destination = "Main Library"
        else if (lower.contains("dtc")) destination = "DTC"

        if (origin == null) missing.add("origin")
        if (destination == null) missing.add("destination")
        missing.add("departure_time")

        return RideParserUiState(
            origin = origin,
            destination = destination,
            departureDate = null,
            departureTime = null,
            seats = seats,
            missingFields = missing,
            ambiguousFields = emptyList(),
            confidence = if (origin != null && destination != null) 0.60 else 0.30,
            isSuccess = true,
            requiresManualInput = true,
            errorMessage = errorMsg
        )
    }
}
