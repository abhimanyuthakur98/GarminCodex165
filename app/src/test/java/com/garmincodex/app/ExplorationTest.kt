package com.garmincodex.app

import org.junit.Assert.*
import org.junit.Test
import java.time.*

class ExplorationTest {
    @Test fun baselinesExcludeCurrentAndFutureAndRequireSevenPriorDays() {
        val today=LocalDate.of(2026,11,10)
        val history=(1L..7L).map { previewDay(today.minusDays(it).toString()).copy(overnightHr=50.0+it) }
        assertNull(PersonalBaseline.calculate(history.take(6),today){it.overnightHr})
        val result=PersonalBaseline.calculate(history+previewDay(today.toString()).copy(overnightHr=200.0),today){it.overnightHr}!!
        assertEquals(7,result.count)
        assertEquals(52.5,result.lower,.001)
        assertEquals(55.5,result.upper,.001)
    }
    @Test fun timelinePreservesMissingHoursAndHandlesDst() {
        val zone=ZoneId.of("America/New_York")
        val date=LocalDate.of(2026,11,1)
        val start=date.atStartOfDay(zone).toInstant().toEpochMilli()
        val day=ConnectedDay(date.toString(),hourly=HourBucket(start,start+3600000,0,null,null).encode(),chart="$start,60",sleepStart=start-3600000,sleepEnd=start+1800000)
        val hours=ActivityTimeline.hours(day,zone)
        assertEquals(25,hours.size)
        assertEquals(0L,hours.first().steps)
        assertNull(hours[1].steps)
        assertEquals(60.0,hours.first().hr!!,.001)
        assertNull(hours[1].hr)
        assertEquals(30,hours.first().sleepMinutes)
    }
    @Test fun preferencesNormalizeUnknownIdsAndMoveWithinBounds() {
        val prefs=DashboardPreferences(listOf("heart","heart","unknown"),setOf("sleep","unknown"),9).normalized()
        assertEquals(DashboardPreferences.CARDS.size,prefs.order.size)
        assertEquals("heart",prefs.order.first())
        assertEquals(setOf("sleep"),prefs.hidden)
        assertEquals(5,prefs.startPage)
        assertEquals(prefs,prefs.move("heart",-1))
        assertEquals("heart",prefs.move("heart",1).order[1])
    }
}
