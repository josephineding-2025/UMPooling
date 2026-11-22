package com.fopgang.umpooling

import com.fopgang.umpooling.booking.*
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class DepartureBranchTest {
    private val now = Instant.parse("2026-11-18T00:00:00Z")
    private val clock = Clock.fixed(now,CAMPUS_ZONE)
    private fun criteria(seconds: Long): FindRideCriteria {
        val time = now.plusSeconds(seconds).atZone(CAMPUS_ZONE)
        return FindRideCriteria(departureDate=time.toLocalDate(),departureTime=time.toLocalTime())
    }
    @Test fun thirtyMinuteBoundaryIsInclusive() {
        assertTrue(criteria(0).isImmediate(clock))
        assertTrue(criteria(1800).isImmediate(clock))
        assertFalse(criteria(1801).isImmediate(clock))
        assertFalse(criteria(-1).isImmediate(clock))
    }
}
