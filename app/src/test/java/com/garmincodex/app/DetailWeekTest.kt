package com.garmincodex.app

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class DetailWeekTest {
    @Test fun movementKeepsHiddenCardsAndIgnoresUnknownTargets() {
        val prefs=DashboardPreferences(homeHidden=setOf("hrv"))
        val moved=prefs.placeHome("sleep","heart")
        assertEquals("sleep",moved.homeOrder[3])
        assertEquals(prefs.homeHidden,moved.homeHidden)
        assertEquals(prefs,prefs.placeHome("sleep","unknown"))
        assertEquals(prefs,prefs.placeHome("unknown","sleep"))
    }
    @Test fun sevenDayStripPreservesGapsAndActivitySources() {
        val date=LocalDate.of(2026,11,27)
        val today=ConnectedDay(date.toString(),steps=123,calories=2100.0,activitySource=ActivitySource.GARMIN)
        val other=today.copy(date=date.minusDays(1).toString(),activitySource="other",steps=9000)
        val zero=today.copy(date=date.minusDays(2).toString(),steps=0)
        val result=detailWeek(listOf(today,other,zero),date,TrendMetric.STEPS)
        assertEquals(7,result.size)
        assertEquals(123.0,result.last().second!!,0.0)
        assertNull(result[5].second)
        assertEquals(0.0,result[4].second!!,0.0)
        assertNull(result.first().second)
        assertEquals(2100.0,detailWeek(listOf(today),date,TrendMetric.ENERGY).last().second!!,0.0)
    }
}
