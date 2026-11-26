package com.garmincodex.app

import org.junit.Assert.*
import org.junit.Test

class CalorieTileValueTest {
    @Test fun prefersActiveIncludingZeroAndLabelsTotalFallback() {
        val day=ConnectedDay("2026-11-26",activeCalories=420.0,calories=2100.0)
        assertEquals(CalorieTileValue(420.0,true),calorieTileValue(day))
        assertEquals(CalorieTileValue(0.0,true),calorieTileValue(day.copy(activeCalories=0.0)))
        assertEquals(CalorieTileValue(2100.0,false),calorieTileValue(day.copy(activeCalories=null)))
        assertEquals(CalorieTileValue(null,false),calorieTileValue(null))
        assertEquals(CalorieTileValue(null,false),calorieTileValue(day.copy(activeCalories=Double.NaN,calories=-1.0)))
    }
}
