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
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class RideRepository {

    /** Server-configured cap; fail closed instead of inventing a UI policy. */
    suspend fun maxCostPerSeat(): Result<Double> = suspendRunCatching {
        val cap = SupabaseClientProvider.client.postgrest.rpc("max_cost_per_seat").decodeAs<Double>()
        require(cap.isFinite() && cap in 0.0..999.99)
        cap
    }

    suspend fun cancelBooking(bookingId: String): Result<Int> = suspendRunCatching {
        SupabaseClientProvider.client.postgrest.rpc("cancel_booking", buildJsonObject {
            put("p_booking_id", bookingId)
        }).decodeAs<Int>()
    }

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
                order("departure_time", order = Order.ASCENDING)
            }.decodeList<TripOffer>()
    }

    // 2b. Paginated fetch of open trip offers for the filterable feed (FR2.3)
    suspend fun getOpenTripOffersPaged(
        limit: Int = 20,
        offset: Int = 0
    ): Result<List<TripOffer>> = suspendRunCatching {
        val safeLimit = limit.coerceAtLeast(1)
        val safeOffset = offset.coerceAtLeast(0)
        SupabaseClientProvider.client.postgrest["trip_offers"]
            .select(Columns.ALL) {
                filter {
                    eq("status", OfferStatus.OPEN)
                    gt("available_seats", 0)
                }
                order("departure_time", order = Order.ASCENDING)
                range(from = safeOffset.toLong(), to = (safeOffset + safeLimit - 1).toLong())
            }.decodeList<TripOffer>()
    }

    // 3. Reserve seats through the database function, which locks the offer row so two
    // passengers cannot take the same seat (FR3.2). Returns the new booking id.
    suspend fun bookSeat(
        tripOfferId: String,
        seats: Int,
        pickupLng: Double,
        pickupLat: Double,
        quoteVersion: Long? = null
    ): Result<String> = suspendRunCatching {
        val client = SupabaseClientProvider.client
        val passengerId = client.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("Sign in before booking.")
        client.postgrest.rpc(
            if (quoteVersion == null) "book_seat_transaction" else "book_seat_with_quote",
            buildJsonObject {
                put("p_trip_offer_id", tripOfferId)
                if (quoteVersion == null) put("p_passenger_id", passengerId)
                else put("p_quote_version", quoteVersion)
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

    // 9. Cancel an open trip offer (FR2.2)
    suspend fun cancelTripOffer(tripOfferId: String): Result<Unit> = suspendRunCatching {
        SupabaseClientProvider.client.postgrest["trip_offers"]
            .update({ set("status", OfferStatus.CANCELLED) }) {
                filter { eq("trip_offer_id", tripOfferId) }
            }
        Unit
    }

    // 10. Cancel a pending ride request (FR2.2)
    suspend fun cancelRideRequest(requestId: String): Result<Unit> =
        updateRequestStatus(requestId, com.fopgang.umpooling.data.RequestStatus.ERROR)

    // 11. Fetch driver's own trip offers for cancellation & expiry management (FR2.2)
    suspend fun getMyTripOffers(driverId: String): Result<List<TripOffer>> = suspendRunCatching {
        SupabaseClientProvider.client.postgrest["trip_offers"]
            .select(Columns.ALL) {
                filter { eq("driver_id", driverId) }
            }.decodeList<TripOffer>()
    }

    // 12. Fetch user's own ride requests for cancellation & expiry management (FR2.2)
    suspend fun getMyRideRequests(userId: String): Result<List<RideRequest>> = suspendRunCatching {
        SupabaseClientProvider.client.postgrest["ride_requests"]
            .select(Columns.ALL) {
                filter { eq("user_id", userId) }
            }.decodeList<RideRequest>()
    }
}
