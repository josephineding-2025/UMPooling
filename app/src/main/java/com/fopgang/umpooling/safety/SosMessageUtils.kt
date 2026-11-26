
package com.fopgang.umpooling.safety

import java.util.Locale

internal object SosMessageUtils {

    fun normalizePhone(phone: String): String {
        val digits = phone.filter(Char::isDigit)
        return if (phone.trim().startsWith("+")) {
            "+$digits"
        } else {
            digits
        }
    }

    fun whatsappPhone(phone: String): String {
        return normalizePhone(phone).filter(Char::isDigit)
    }

    fun mapsLink(latitude: Double, longitude: Double): String {
        return String.format(
            Locale.US,
            "https://www.google.com/maps?q=%.6f,%.6f",
            latitude,
            longitude
        )
    }
}