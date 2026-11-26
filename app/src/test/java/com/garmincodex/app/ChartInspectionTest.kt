package com.garmincodex.app

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class ChartInspectionTest {
    private val date=LocalDate.of(2026,11,25)
    @Test fun comparisonsExcludeSelectedDayDuplicatesMissingValuesAndOtherSources() {
        val selected=ConnectedDay(date.toString(),steps=99999,activitySource=ActivitySource.GARMIN)
        val history=(1L..7L).map{selected.copy(date=date.minusDays(it).toString(),steps=if(it==1L)0 else 700)}
        val result=recentComparison(history+history+selected+selected.copy(date=date.minusDays(8).toString(),steps=90000,activitySource="other"),date,TrendMetric.STEPS)
        assertEquals(7,result.count);assertEquals(600.0,result.average!!,0.0);assertEquals(99999.0,result.selected!!,0.0)
        assertNull(recentComparison(history.take(6)+selected,date,TrendMetric.STEPS).average)
        assertNull(recentComparison(history,date,TrendMetric.STEPS).average)
    }
    @Test fun currentDayDoesNotClaimAFullDayChange() {
        val result=RecentComparison(8000.0,10,100.0)
        assertFalse(comparisonText(result,TrendMetric.STEPS,date,date).contains("below"))
        assertTrue(comparisonText(result,TrendMetric.STEPS,date,date.plusDays(1)).contains("below"))
        assertTrue(comparisonText(result.copy(selected=null),TrendMetric.STEPS,date,date.plusDays(1)).startsWith("No selected-day record"))
    }
    @Test fun chartEdgesClampAndSleepGapsStayMissing() {
        assertEquals(-1,chartIndex(.5f,0));assertEquals(0,chartIndex(-1f,7));assertEquals(6,chartIndex(1f,7))
        val spans=listOf(SleepSpan(1000,2000,5),SleepSpan(3000,4000,4))
        assertEquals(5,stageAtFraction(spans,0f)!!.stage)
        assertNull(stageAtFraction(spans,.5f))
        assertEquals(4,stageAtFraction(spans,1f)!!.stage)
        assertNull(stageAtFraction(emptyList(),.5f))
    }
}
