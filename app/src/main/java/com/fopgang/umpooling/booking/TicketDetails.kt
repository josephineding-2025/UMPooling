package com.fopgang.umpooling.booking

/** Display-only contract: absence of a public profile must never produce a demo identity. */
data class TicketDetails(
    val status: String,
    val departure: String,
    val pickup: String,
    val destination: String,
    val driverName: String,
    val vehicle: String,
    val passengerName: String,
    val costPerSeat: Double?,
    val canSos: Boolean,
    val reference: String? = null,
)
