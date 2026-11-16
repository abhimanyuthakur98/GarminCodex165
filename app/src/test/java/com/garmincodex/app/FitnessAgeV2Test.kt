package com.garmincodex.app

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class FitnessAgeV2Test {
    private val now = LocalDate.of(2026, 11, 16)
    private fun days() = (1L..28L).map { ConnectedDay(now.minusDays(it).toString(), steps=8000, asleep=420, coveredMinutes=600, zones="450,120,30,0,0,0,0") }
    @Test fun allHistoryCountsAndDailyExcessDoesNotEraseInactiveDays() {
        val good = FitnessAgeModel.calculate(days(),30,now)
        assertEquals(28,good.days); assertEquals(27.0,good.age!!,.001)
        val mixed=days().mapIndexed { i,d -> d.copy(steps=if(i%2==0)16000 else 0) }
        assertEquals(50.0,FitnessAgeModel.calculate(mixed,30,now).factors[0].score,.001)
        assertTrue(FitnessAgeModel.calculate(days().mapIndexed { i,d -> if(i>13)d.copy(steps=0) else d },30,now).age!! > good.age!!)
    }
    @Test fun staleMissingMalformedAndLowCoverageDoNotProduceAnAge() {
        assertNull(FitnessAgeModel.calculate(days().drop(7),30,now).age)
        for(bad in listOf("", "0,0,NaN,0,0,0,0", "0,0,-2,0,0,0,0", "0,0,9999,0,0,0,0"))
            assertNull(FitnessAgeModel.calculate(days().map { it.copy(zones=bad) },30,now).age)
        assertNull(FitnessAgeModel.calculate(days().map { it.copy(coveredMinutes=479) },30,now).age)
        assertNull(FitnessAgeModel.calculate(days().map { it.copy(unknownSleep=31) },30,now).age)
        assertNull(FitnessAgeModel.calculate(days().map { it.copy(steps=null) },30,now).age)
    }
    @Test fun recoveryDoesNotDoubleCountSleepAndVigorousMinutesAreWeighted() {
        val base=days().map { it.copy(zones="590,0,10,0,0,0,0",recovery=null) }
        assertEquals(FitnessAgeModel.calculate(base,30,now),FitnessAgeModel.calculate(base.map { it.copy(recovery=100) },30,now))
        val vigorous=FitnessAgeModel.calculate(base.map { it.copy(zones="590,0,0,0,10,0,0") },30,now)
        assertTrue(vigorous.factors[3].score > FitnessAgeModel.calculate(base,30,now).factors[3].score)
    }
    @Test fun latestInvalidSnapshotCannotBeReplacedByStaleValidDuplicate() {
        val old=days().take(14)
        val invalid=old.first().copy(updatedAt=10,steps=null)
        assertNull(FitnessAgeModel.calculate(old+invalid,30,now).age)
        assertNull(FitnessAgeModel.calculate(listOf(invalid)+old,30,now).age)
    }
}
