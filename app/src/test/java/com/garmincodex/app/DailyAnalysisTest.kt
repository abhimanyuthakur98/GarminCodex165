package com.garmincodex.app

import org.junit.Assert.*
import org.junit.Test
import java.time.*

class DailyAnalysisTest {
    @Test fun sparseImportedHrCountsRealIntervalsButNeverLongGaps() {
        val result = DailyAnalysis.load(listOf(HrPoint(0, 150), HrPoint(120000, 150), HrPoint(240000, 150), HrPoint(3600000, 150)), 55, 190)
        assertEquals(4, result.coveredMinutes)
        assertEquals(20.0, result.load, .001)
    }
    @Test fun repeatedSamplesCannotDoubleCountLoad() {
        val a = HrPoint(0, 150); val b = HrPoint(60000, 150)
        assertEquals(DailyAnalysis.load(listOf(a, b), 55, 190), DailyAnalysis.load(listOf(b, a, a, b), 55, 190))
    }
    @Test fun ignoresOutOfRangeHrAndDoesNotInferMissingDay() {
        val load = DailyAnalysis.load(listOf(HrPoint(0, 0), HrPoint(60000, 250)), 55, 190)
        assertEquals(0, load.coveredMinutes); assertTrue(load.points.isEmpty())
    }
    @Test fun sleepCrossesMidnightAndRetainsUnknownIntervals() {
        val start = Instant.parse("2026-11-08T18:30:00Z").toEpochMilli()
        val s = DailyAnalysis.sleep(SleepInput(start, start + 480 * 60000L, listOf(SleepSpan(start, start + 90 * 60000L, 5), SleepSpan(start + 90 * 60000L, start + 450 * 60000L, 4))))
        assertEquals(450, s.asleep); assertEquals(90, s.deep); assertEquals(30, s.unknown)
    }
    @Test fun missingStagesDoNotFabricateSleepDuration() {
        val s = DailyAnalysis.sleep(SleepInput(0, 480 * 60000L, emptyList()))
        assertNull(s.asleep); assertEquals(480, s.unknown)
    }
    @Test fun overlappingSleepStagesCannotExceedSession() {
        val s = DailyAnalysis.sleep(SleepInput(0, 600000, listOf(SleepSpan(-60000, 600000, 4), SleepSpan(300000, 900000, 5))))
        assertEquals(10, s.asleep); assertEquals(5, s.deep); assertEquals(5, s.light)
    }
    @Test fun recoveryRequiresBaselineAndRealInputs() {
        val s = DailyAnalysis.sleep(SleepInput(0, 480 * 60000L, listOf(SleepSpan(0, 480 * 60000L, 4))))
        assertNull(DailyAnalysis.recovery(s, 55.0, listOf(55.0, 56.0), 480))
        assertNull(DailyAnalysis.recovery(s, null, listOf(55.0, 56.0, 54.0), 480))
        val rested = DailyAnalysis.recovery(s, 55.0, listOf(55.0, 56.0, 54.0), 480)!!
        val stressed = DailyAnalysis.recovery(s, 70.0, listOf(55.0, 56.0, 54.0), 480)!!
        assertTrue(rested > stressed); assertTrue(rested in 0..100)
    }
    @Test fun awakeAndUnknownAreNotAsleep() {
        val s = DailyAnalysis.sleep(SleepInput(0, 600000, listOf(SleepSpan(0, 300000, 7), SleepSpan(300000, 600000, 0))))
        assertNull(s.asleep); assertEquals(5, s.awake); assertEquals(5, s.unknown)
    }
    @Test fun consistencyHandlesMidnightWrap() {
        val times = listOf("2026-11-07T23:55:00Z", "2026-11-09T00:05:00Z", "2026-11-10T00:00:00Z").map { Instant.parse(it).toEpochMilli() }
        assertTrue(DailyAnalysis.consistency(times, ZoneOffset.UTC)!! > 90)
    }
    @Test fun daylightSavingDayIsNotAssumedToBe24Hours() {
        val zone = ZoneId.of("America/New_York"); val day = LocalDate.of(2026, 11, 1)
        assertEquals(25, Duration.between(day.atStartOfDay(zone), day.plusDays(1).atStartOfDay(zone)).toHours().toInt())
    }
}
