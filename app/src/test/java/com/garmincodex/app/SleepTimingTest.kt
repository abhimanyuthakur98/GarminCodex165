package com.garmincodex.app

import org.junit.Assert.*
import org.junit.Test
import java.time.*

class SleepTimingTest {
    @Test fun averageHandlesBedtimesAcrossMidnight() {
        assertEquals(0,clockMean(listOf(23*60+50,0,10)))
        assertNull(clockMean(listOf(0,720)))
        assertNull(clockMean(listOf(0,360,720,1080)))
    }
    @Test fun wakeChangesIgnoreMissingNightsAndWrapMidnight() {
        val d=LocalDate.of(2026,11,25)
        val nights=listOf(SleepTiming(d,100,1430),SleepTiming(d.plusDays(1),100,10),SleepTiming(d.plusDays(2),null,null),SleepTiming(d.plusDays(3),100,300))
        assertEquals(listOf(20),wakeChanges(nights))
    }
    @Test fun invalidSessionsAndDatesOutsideWindowAreExcluded() {
        val d=LocalDate.of(2026,11,25)
        val start=d.minusDays(1).atTime(23,0).toInstant(ZoneOffset.UTC).toEpochMilli()
        val finish=d.atTime(7,0).toInstant(ZoneOffset.UTC).toEpochMilli()
        val valid=ConnectedDay(d.toString(),sleepStart=start,sleepEnd=finish)
        val data=listOf(valid,valid,valid.copy(date=d.minusDays(1).toString(),sleepEnd=start-1),valid.copy(date=d.plusDays(1).toString()))
        val result=sleepTiming(data,d,ZoneOffset.UTC)
        assertEquals(7,result.size)
        assertEquals(1,result.count{it.wake!=null})
        assertEquals(1380,result.last().bedtime)
        assertEquals(420,result.last().wake)
        assertEquals("07:00",sleepClock(result.last().wake))
    }
}
