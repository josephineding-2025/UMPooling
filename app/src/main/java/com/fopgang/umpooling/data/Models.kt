package com.fopgang.umpooling.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Table 1: users
@Serializable
data class UserProfile(
    @SerialName("usersId") val usersId: String,
    @SerialName("usersEmail") val usersEmail: String,
    @SerialName("usersRole") val usersRole: String = "passengers",
    @SerialName("vehiclePlate") val vehiclePlate: String? = null,
    @SerialName("vehicleModel") val vehicleModel: String? = null,
    @SerialName("createdTime") val createdTime: String? = null
)

// Table 3: trip_offers
@Serializable
data class TripOffer(
    @SerialName("tripOfferId") val tripOfferId: String? = null,
    @SerialName("driverId") val driverId: String,
    @SerialName("costPerSeat") val costPerSeat: Double,
    @SerialName("availableSeat") val availableSeat: Int,
    @SerialName("status") val status: String = "open",
    @SerialName("departureTime") val departureTime: String,
    @SerialName("createdTime") val createdTime: String? = null
)

// Table 4: ride_requests
@Serializable
data class RideRequest(
    @SerialName("requestId") val requestId: String? = null,
    @SerialName("userId") val userId: String,
    @SerialName("tripOfferId") val tripOfferId: String,
    @SerialName("status") val status: String = "pending",
    @SerialName("requestedTime") val requestedTime: String? = null
)

// Table 6: messages
@Serializable
data class Message(
    @SerialName("messageId") val messageId: String? = null,
    @SerialName("tripOfferId") val tripOfferId: String,
    @SerialName("userId") val userId: String,
    @SerialName("content") val content: String,
    @SerialName("messageTime") val messageTime: String? = null
)

// Helper model for NLP parsing output (Internal UI helper, not a Supabase table)
@Serializable
data class ParsedRideNlp(
    val origin: String,
    val destination: String,
    val departureTime: String,
    val seats: Int = 1
)
