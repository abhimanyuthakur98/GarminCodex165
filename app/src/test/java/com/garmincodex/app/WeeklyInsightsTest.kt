package com.garmincodex.app

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class WeeklyInsightsTest {
    private val end=LocalDate.of(2026,11,25)
    @Test fun goalSummaryExcludesTodayAndKeepsZeroDistinctFromMissing() {
        val data=listOf(ConnectedDay(end.toString(),steps=99999),ConnectedDay(end.minusDays(1).toString(),steps=9000),ConnectedDay(end.minusDays(2).toString(),steps=0))
        val steps=weeklyGoals(data,end,PersonalGoals(steps=8000),480).first()
        assertEquals(2,steps.recorded)
        assertEquals(1,steps.reached)
        assertNull(steps.values.first())
        assertEquals(0.0,steps.values[5]!!,0.0)
        assertEquals("1 of 2 recorded days",steps.summary)
        assertEquals("No recorded days",weeklyGoals(emptyList(),end,PersonalGoals(),480).first().summary)
    }
    @Test fun duplicateDatesDoNotInflateGoalsAndHistoricalWindowIsRespected() {
        val day=ConnectedDay(end.minusDays(7).toString(),steps=9000)
        val days=listOf(day,day,ConnectedDay(end.minusDays(8).toString(),steps=9000))
        assertEquals(1,weeklyGoals(days,end,PersonalGoals(steps=8000),480).first().recorded)
        assertEquals(1,weeklyGoals(days,end.minusDays(7),PersonalGoals(steps=8000),480).first().recorded)
    }
    @Test fun incompleteWeeksNeverClaimAnImprovement() {
        assertEquals("Full weeks needed to compare",weeklyChange(ReportValue(9000.0,0.0,6,7),"steps"))
        assertEquals("Full weeks needed to compare",weeklyChange(ReportValue(null,null,0,0),"steps"))
    }
    @Test fun zeroBaselineUsesAbsoluteChangeAndSleepUsesReadableDuration() {
        assertEquals("500 steps/day more than last week",weeklyChange(ReportValue(500.0,0.0,7,7),"steps"))
        assertEquals("30 min/night less than last week",weeklyChange(ReportValue(450.0,480.0,7,7),"sleep"))
        assertEquals("7h 55m",weeklyValue("sleep",475.0))
        assertEquals("—",weeklyValue("steps",null))
        assertEquals("0",weeklyValue("steps",0.0))
    }
}
