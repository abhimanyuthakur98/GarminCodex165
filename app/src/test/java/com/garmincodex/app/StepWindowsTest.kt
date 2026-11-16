package com.garmincodex.app

import org.junit.Assert.*
import org.junit.Test
import java.time.*

class StepWindowsTest {
    @Test fun fullDayCumulativeStepsAreNotProratedAtTwoPm() {
        val date=LocalDate.of(2026,11,16);val zone=ZoneId.of("Asia/Kolkata")
        val (start,end)=StepWindows.bounds(date,zone)
        val now=date.atTime(14,0).atZone(zone).toInstant()
        val total=14000L
        fun prorated(until:Instant)=total*Duration.between(start,until).seconds/Duration.between(start,end).seconds
        assertEquals(8166L,prorated(now));assertEquals(14000L,prorated(end))
        assertTrue(end>now);assertTrue(StepWindows.coarse(start,end))
    }
    @Test fun localDaysRespectDstAndShortIntervalsStayHourly() {
        val (start,end)=StepWindows.bounds(LocalDate.of(2026,11,1),ZoneId.of("America/New_York"))
        assertEquals(25,Duration.between(start,end).toHours().toInt())
        assertFalse(StepWindows.coarse(start,start.plusSeconds(3600)))
        assertTrue(StepWindows.coarse(start,start.plusSeconds(3601)))
    }
}
