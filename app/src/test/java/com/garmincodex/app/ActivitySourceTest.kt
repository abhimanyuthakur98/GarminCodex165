package com.garmincodex.app

import org.junit.Assert.*
import org.junit.Test

class ActivitySourceTest {
    @Test fun switchingSourceCannotReuseOtherAppsTotalsOrHourlyData() {
        val old=ConnectedDay("2026-11-25",steps=14000,activeCalories=600.0,calories=2300.0,basalCalories=1700.0,asleep=480,hrAverage=70,activitySource="com.sec.android.app.shealth",hourly=HourBucket(0,3600000,500,20.0,80.0).encode())
        val filtered=ActivitySource.matching(old,ActivitySource.GARMIN)
        assertNull(filtered.steps);assertNull(filtered.activeCalories);assertNull(filtered.calories);assertNull(filtered.basalCalories)
        assertNull(HourBucket.decode(filtered.hourly).single().steps)
        assertNull(HourBucket.decode(filtered.hourly).single().active)
        assertEquals(old.asleep,filtered.asleep);assertEquals(old.hrAverage,filtered.hrAverage)
        assertEquals(old,ActivitySource.matching(old,old.activitySource))
    }
    @Test fun legacyCacheDoesNotBecomeGarminDataAndZeroRemainsZero() {
        assertNull(ActivitySource.matching(ConnectedDay("2026-11-25",steps=9000),ActivitySource.GARMIN).steps)
        val zero=ConnectedDay("2026-11-25",steps=0,activeCalories=0.0,activitySource=ActivitySource.GARMIN)
        assertEquals(zero,ActivitySource.matching(zero,ActivitySource.GARMIN))
    }
}
