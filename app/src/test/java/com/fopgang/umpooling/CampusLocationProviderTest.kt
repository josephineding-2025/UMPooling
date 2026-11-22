package com.fopgang.umpooling

import com.fopgang.umpooling.data.CampusLocationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CampusLocationProviderTest {

    @Test
    fun containsAll75Locations() {
        assertEquals(75, CampusLocationProvider.allLocations.size)
    }

    @Test
    fun findsFsktmAuditedCentroid() {
        val fsktm = CampusLocationProvider.findById("fsktm")
        assertNotNull(fsktm)
        assertEquals("Faculty of Computer Science and Information Technology", fsktm!!.name)
        assertEquals(3.12841, fsktm.lat!!, 0.0001)
        assertEquals(101.65049, fsktm.lng!!, 0.0001)
        assertEquals("SRID=4326;POINT(101.65049 3.12841)", fsktm.ewkt)
    }

    @Test
    fun searchFiltersCorrectly() {
        val results = CampusLocationProvider.search("Damansara")
        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.id == "tiara_damansara" })
    }

    @Test
    fun residentialCollegesAreAllPresent() {
        for (i in 1..13) {
            val kk = CampusLocationProvider.findById("kk$i")
            assertNotNull("KK$i should be present in locations", kk)
        }
    }
}
