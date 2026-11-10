package com.garmincodex.app
import org.junit.Assert.*
import org.junit.Test
class RunningMetricsTest {
    @Test fun paceAndDistanceUseSelectedUnitsAndHandleMissingOrStoppedSamples() {
        assertEquals("5:00 /km",RunningMetrics.pace(1000.0/300,false))
        assertEquals("8:03 /mi",RunningMetrics.pace(1000.0/300,true))
        assertEquals("1.00 mi",RunningMetrics.distance(1609.344,true))
        assertEquals("—",RunningMetrics.pace(0.0,false))
        assertEquals("—",RunningMetrics.pace(Double.NaN,false))
        assertNull(RunningMetrics.speed(5000.0,0.0))
        assertEquals(listOf(PaceSample(1,3.0)),PaceSample.decode("1,3;2,NaN;3,-1;4,200;bad"))
    }
    @Test fun comparisonsRequireSimilarDistanceSourceAndType() {
        val w=Workout("now","FIT","RUNNING","Run",100,200,"",distanceMeters=5000.0)
        val match=w.copy(id="match",start=10,distanceMeters=5500.0)
        assertEquals(match,RunningMetrics.comparable(w,listOf(match,w.copy(id="long",start=90,distanceMeters=10000.0),w.copy(id="hc",start=95,source="HEALTH_CONNECT"))))
        assertNull(RunningMetrics.comparable(w,listOf(w.copy(start=1,distanceMeters=null))))
    }
    @Test fun diagnosticsDoNotCountMissingValuesAsZeroRecords() {
        val report=importReport(listOf(ConnectedDay("2026-11-10",steps=0,activeCalories=0.0,hrCount=12)),"2026-10-28","2026-11-10",2)
        assertTrue(report.contains("Steps: 1 days"));assertTrue(report.contains("Active calories: 1 days"))
        assertTrue(report.contains("Total calories: 0 days"));assertTrue(report.contains("Heart rate: 12 samples"))
        assertTrue(report.contains("Latest HR measurement: none supplied"))
    }
}
