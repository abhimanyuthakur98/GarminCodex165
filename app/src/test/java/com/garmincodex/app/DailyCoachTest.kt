package com.garmincodex.app

import org.junit.Assert.*
import org.junit.Test
import java.time.*

class DailyCoachTest {
    @Test fun bedtimeWrapsMidnightAndRejectsInvalidWakeTime() {
        assertEquals(LocalTime.of(23, 0), DailyCoach.bedtime(420, 480))
        assertNull(DailyCoach.parseWake("24:00"))
        assertNull(DailyCoach.parseWake("07:60"))
        assertEquals(LocalTime.of(6, 30), DailyCoach.parseWake("06:30"))
    }
    @Test fun weeklyExcludesTodayAndPreservesMissingCoverage() {
        val today = LocalDate.of(2026, 11, 10)
        val days = listOf(previewDay(today.toString()).copy(steps = 99999), previewDay(today.minusDays(1).toString()).copy(steps = 1000), previewDay(today.minusDays(8).toString()).copy(steps = 2000))
        val report = DailyCoach.weekly(days + days.last(), today) { it.steps?.toDouble() }
        assertEquals(1000.0, report.current!!, .001)
        assertEquals(2000.0, report.previous!!, .001)
        assertEquals(1, report.currentDays)
        assertEquals(1, report.previousDays)
    }
    @Test fun sleepShortfallCountsOnlyRecordedNights() {
        val today = LocalDate.of(2026, 11, 10)
        val days = listOf(previewDay(today.toString()).copy(asleep = 400), previewDay(today.minusDays(1).toString()).copy(asleep = 500), previewDay(today.minusDays(2).toString()).copy(asleep = null))
        assertEquals(80 to 2, DailyCoach.shortfall(days, today, 480))
        assertTrue(DailyCoach.briefing(null, null, 480).contains("has not arrived"))
    }
}
