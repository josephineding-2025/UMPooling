package com.fopgang.umpooling.data.repository

import com.fopgang.umpooling.SupabaseClientProvider
import com.fopgang.umpooling.data.NewRideRequest
import com.fopgang.umpooling.data.NewSavedRoute
import com.fopgang.umpooling.data.NewTripOffer
import com.fopgang.umpooling.data.OfferStatus
import com.fopgang.umpooling.data.RideRequest
import com.fopgang.umpooling.data.SavedRoute
import com.fopgang.umpooling.data.TripOffer
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class RideRepository {

    // 1. Post a new trip offer. The cost-share cap is enforced by the database (FR3.1),
    // so a modified client cannot bypass it; a violation comes back as a failure.
    suspend fun createTripOffer(offer: NewTripOffer): Result<TripOffer> = suspendRunCatching {
        SupabaseClientProvider.client.postgrest["trip_offers"]
            .insert(offer) { select() }
            .decodeSingle<TripOffer>()
    }

    // 2. Fetch all open trip offers that still have seats
    suspend fun getOpenTripOffers(): Result<List<TripOffer>> = suspendRunCatching {
        SupabaseClientProvider.client.postgrest["trip_offers"]
            .select(Columns.ALL) {
                filter {
                    eq("status", OfferStatus.OPEN)
                    gt("available_seats", 0)
                }
            }.decodeList<TripOffer>()
    }

    // 3. Reserve seats through the database function, which locks the offer row so two
    // passengers cannot take the same seat (FR3.2). Returns the new booking id.
    suspend fun bookSeat(
        tripOfferId: String,
        seats: Int,
        pickupLng: Double,
        pickupLat: Double
    ): Result<String> = suspendRunCatching {
        val client = SupabaseClientProvider.client
        val passengerId = client.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("Sign in before booking.")
        client.postgrest.rpc(
            "book_seat_transaction",
            buildJsonObject {
                put("p_trip_offer_id", tripOfferId)
                put("p_passenger_id", passengerId)
                put("p_seats", seats)
                put("p_pickup_lng", pickupLng)
                put("p_pickup_lat", pickupLat)
            }
        ).decodeAs<String>()
    }

    // 4. Post a ride request. It has no offer yet; drivers on overlapping routes are notified (FR2.2).
    suspend fun createRideRequest(request: NewRideRequest): Result<RideRequest> = suspendRunCatching {
        SupabaseClientProvider.client.postgrest["ride_requests"]
            .insert(request) { select() }
            .decodeSingle<RideRequest>()
    }

    // 5. Update request status (e.g., driver accepts/rejects)
    suspend fun updateRequestStatus(requestId: String, status: String): Result<Unit> = suspendRunCatching {
        SupabaseClientProvider.client.postgrest["ride_requests"]
            .update({ set("status", status) }) {
                filter { eq("request_id", requestId) }
            }
        Unit
    }

    // 6. Save a frequent commuter route (FR2.1)
    suspend fun createSavedRoute(route: NewSavedRoute): Result<SavedRoute> = suspendRunCatching {
        SupabaseClientProvider.client.postgrest["saved_routes"]
            .insert(route) { select() }
            .decodeSingle<SavedRoute>()
    }

    // 7. Get all frequent routes saved by a user (FR2.1)
    suspend fun getSavedRoutes(userId: String): Result<List<SavedRoute>> = suspendRunCatching {
        SupabaseClientProvider.client.postgrest["saved_routes"]
            .select(Columns.ALL) {
                filter {
                    eq("user_id", userId)
                }
            }.decodeList<SavedRoute>()
    }

    // 8. Delete a saved route (FR2.1)
    suspend fun deleteSavedRoute(routeId: String): Result<Unit> = suspendRunCatching {
        SupabaseClientProvider.client.postgrest["saved_routes"]
            .delete {
                filter {
                    eq("route_id", routeId)
                }
            }
        Unit
    }
}
