package com.fopgang.umpooling.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Column names follow the live schema as described in supabase/migrations/*.sql.
// public.users keeps its quoted legacy camelCase names; other tables use snake_case.

/** Role of a commuter. Staff access is assigned only by the backend, never by the client. */
object UserRole {
    const val PASSENGERS = "passengers"
    const val DRIVERS = "drivers"
    const val BOTH = "both"
    val selectable = listOf(PASSENGERS, DRIVERS, BOTH)
}

// Enum labels in the live database (case matters). Send these exact strings.
object RequestStatus {
    const val PENDING = "PENDING"
    const val SUCCESS = "SUCCESS"
    const val ERROR = "ERROR"
}

object OfferStatus {
    const val OPEN = "open"
    const val FULL = "full"
    const val CANCELLED = "cancelled"
    const val COMPLETED = "completed"
}

object TripStatus {
    const val SCHEDULED = "scheduled"
    const val IN_PROGRESS = "in_progress"
    const val COMPLETED = "completed"
    const val CANCELLED = "cancelled"
}

// Table: users (the signed-in user's own row)
@Serializable
data class UserProfile(
    @SerialName("usersId") val usersId: String,
    @SerialName("usersEmail") val usersEmail: String,
    @SerialName("usersRole") val usersRole: String = UserRole.PASSENGERS,
    @SerialName("vehiclePlate") val vehiclePlate: String? = null,
    @SerialName("vehicleModel") val vehicleModel: String? = null,
    @SerialName("faculty") val faculty: String? = null,
    @SerialName("residential_area") val residentialArea: String? = null,
    @SerialName("residential_college") val residentialCollege: String? = null,
    @SerialName("vehicle_type") val vehicleType: String? = null,
    @SerialName("fuel_category") val fuelCategory: String? = null,
    @SerialName("trust_rating") val trustRating: Double? = null,
    @SerialName("createdTime") val createdTime: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("emergency_contact") val emergencyContact: EmergencyContact? = null
)

// Stored in users.emergency_contact (jsonb). Used by the SOS flow (FR4.2).
@Serializable
data class EmergencyContact(
    @SerialName("name") val name: String = "",
    @SerialName("phone") val phone: String
)

// Table: saved_routes (read model for frequent routes / FR2.1)
@Serializable
data class SavedRoute(
    @SerialName("route_id") val routeId: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("origin_zone") val originZone: String? = null,
    @SerialName("destination_zone") val destinationZone: String? = null,
    @SerialName("distance_km") val distanceKm: Double? = null,
    @SerialName("preferred_time") val preferredTime: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

// Insert payload for saved_routes. origin_geo and destination_geo are NOT NULL in the database
// and sent as EWKT text, e.g. GeoPoint.ewkt(lng, lat).
@Serializable
data class NewSavedRoute(
    @SerialName("user_id") val userId: String,
    @SerialName("origin_geo") val originGeo: String,
    @SerialName("destination_geo") val destinationGeo: String,
    @SerialName("origin_zone") val originZone: String? = null,
    @SerialName("destination_zone") val destinationZone: String? = null,
    @SerialName("distance_km") val distanceKm: Double? = null,
    @SerialName("preferred_time") val preferredTime: String? = null
)

// Table: trip_offers
@Serializable
data class TripOffer(
    @SerialName("trip_offer_id") val tripOfferId: String? = null,
    @SerialName("driver_id") val driverId: String,
    @SerialName("cost_per_seat") val costPerSeat: Double,
    @SerialName("available_seats") val availableSeats: Int,
    @SerialName("status") val status: String = OfferStatus.OPEN,
    @SerialName("departure_time") val departureTime: String,
    @SerialName("origin_zone") val originZone: String? = null,
    @SerialName("destination_zone") val destinationZone: String? = null,
    @SerialName("distance_km") val distanceKm: Double? = null,
    @SerialName("created_at") val createdAt: String? = null
)

// Insert payload for trip_offers. origin_geo and destination_geo are NOT NULL in the database
// and are sent as EWKT text, e.g. GeoPoint.ewkt(lng, lat). status uses the database default.
@Serializable
data class NewTripOffer(
    @SerialName("driver_id") val driverId: String,
    @SerialName("origin_geo") val originGeo: String,
    @SerialName("destination_geo") val destinationGeo: String,
    @SerialName("cost_per_seat") val costPerSeat: Double,
    @SerialName("available_seats") val availableSeats: Int,
    @SerialName("departure_time") val departureTime: String,
    @SerialName("origin_zone") val originZone: String? = null,
    @SerialName("destination_zone") val destinationZone: String? = null,
    @SerialName("distance_km") val distanceKm: Double? = null
)

// Table: ride_requests (read model). A request exists before any offer does, so
// trip_offer_id is optional. Geography columns are not read into this model.
@Serializable
data class RideRequest(
    @SerialName("request_id") val requestId: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("trip_offer_id") val tripOfferId: String? = null,
    @SerialName("status") val status: String = RequestStatus.PENDING,
    @SerialName("requested_at") val requestedAt: String? = null,
    @SerialName("preferred_time") val preferredTime: String? = null
)

// Insert payload for ride_requests. pickup_geo is required by the database and is sent as
// EWKT text, e.g. GeoPoint.ewkt(lng, lat). status and requested_at use the database defaults.
@Serializable
data class NewRideRequest(
    @SerialName("user_id") val userId: String,
    @SerialName("pickup_geo") val pickupGeo: String,
    @SerialName("destination_geo") val destinationGeo: String? = null,
    @SerialName("preferred_time") val preferredTime: String? = null,
    @SerialName("trip_offer_id") val tripOfferId: String? = null
)

// Table: messages. A message targets a booking or a trip (the database requires at least one,
// and the chat policies in supabase/migrations/20261115000004_access_controls.sql allow exactly one).
@Serializable
data class Message(
    @SerialName("message_id") val messageId: String? = null,
    @SerialName("booking_id") val bookingId: String? = null,
    @SerialName("trip_id") val tripId: String? = null,
    @SerialName("sender_id") val senderId: String,
    @SerialName("content") val content: String,
    @SerialName("sent_at") val sentAt: String? = null
)

// Helper model for NLP parsing output (Internal UI helper, not a Supabase table)
@Serializable
data class ParsedRideNlp(
    val origin: String,
    val destination: String,
    val departureTime: String,
    val seats: Int = 1
)
