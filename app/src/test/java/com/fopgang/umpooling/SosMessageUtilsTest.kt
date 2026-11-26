
package com.fopgang.umpooling.safety

import org.junit.Assert.assertEquals
import org.junit.Test

class SosMessageUtilsTest {

    @Test
    fun normalizePhone_preservesInternationalPrefix() {
        assertEquals(
            "+60123456789",
            SosMessageUtils.normalizePhone("+60 12-345 6789")
        )
    }

    @Test
    fun normalizePhone_removesFormatting() {
        assertEquals(
            "0123456789",
            SosMessageUtils.normalizePhone("012-345 6789")
        )
    }

    @Test
    fun whatsappPhone_containsDigitsOnly() {
        assertEquals(
            "60123456789",
            SosMessageUtils.whatsappPhone("+60 12-345 6789")
        )
    }

    @Test
    fun mapsLink_usesSixDecimalPlaces() {
        assertEquals(
            "https://www.google.com/maps?q=3.120000,101.650000",
            SosMessageUtils.mapsLink(3.12, 101.65)
        )
    }

    @Test
    fun mapsLink_usesDecimalPointRegardlessOfLocale() {
        val original = java.util.Locale.getDefault()

        try {
            java.util.Locale.setDefault(java.util.Locale.GERMANY)

            assertEquals(
                "https://www.google.com/maps?q=3.120000,101.650000",
                SosMessageUtils.mapsLink(3.12, 101.65)
            )
        } finally {
            java.util.Locale.setDefault(original)
        }
    }
}