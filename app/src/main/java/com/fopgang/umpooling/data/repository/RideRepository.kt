package com.fopgang.umpooling.data.repository

import com.fopgang.umpooling.SupabaseClientProvider
import com.fopgang.umpooling.data.RideRequest
import com.fopgang.umpooling.data.TripOffer
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns

class RideRepository {

    // 1. Post a new Trip Offer (Enforces fuel share cap <= RM 10.00)
    suspend fun createTripOffer(offer: TripOffer): Result<TripOffer> {
        return try {
            if (offer.costPerSeat > 10.00) {
                return Result.failure(Exception("Fuel share cost cannot exceed RM 10.00 limit."))
            }
            val createdOffer = SupabaseClientProvider.client.postgrest["trip_offers"]
                .insert(offer) {
                    select()
                }.decodeSingle<TripOffer>()

            Result.success(createdOffer)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 2. Fetch all open trip offers
    suspend fun getOpenTripOffers(): Result<List<TripOffer>> {
        return try {
            val offers = SupabaseClientProvider.client.postgrest["trip_offers"]
                .select(Columns.ALL) {
                    filter {
                        eq("status", "open")
                        gt("availableSeat", 0)
                    }
                }.decodeList<TripOffer>()

            Result.success(offers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 3. Request a ride on an open offer
    suspend fun requestRide(requestId: String, userId: String, tripOfferId: String): Result<RideRequest> {
        return try {
            val newRequest = RideRequest(
                userId = userId,
                tripOfferId = tripOfferId,
                status = "pending"
            )
            val insertedRequest = SupabaseClientProvider.client.postgrest["ride_requests"]
                .insert(newRequest) {
                    select()
                }.decodeSingle<RideRequest>()

            Result.success(insertedRequest)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 4. Update request status (e.g., Driver accepts/rejects)
    suspend fun updateRequestStatus(requestId: String, status: String): Result<Unit> {
        return try {
            SupabaseClientProvider.client.postgrest["ride_requests"]
                .update({ set("status", status) }) {
                    filter { eq("requestId", requestId) }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
