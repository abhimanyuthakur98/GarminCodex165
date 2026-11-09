package com.garmincodex.app

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class InsightsTest {
    @Test fun hourlyRoundTripKeepsMissingDifferentFromZero() {
        val buckets = listOf(HourBucket(0, 3600000, null, null, null), HourBucket(3600000,7200000,0,0.0,65.0))
        assertEquals(buckets, HourBucket.decode(buckets.joinToString(";") { it.encode() }))
        assertTrue(HourBucket.decode("bad;1,2,bad").isEmpty())
    }
    @Test fun calorieSplitNeverInventsBmrOrNegativeBurn() {
        assertEquals(1500.0, EnergyBreakdown.nonActive(2000.0,500.0)!!, .001)
        assertNull(EnergyBreakdown.nonActive(400.0,500.0)); assertNull(EnergyBreakdown.nonActive(2000.0,null)); assertNull(EnergyBreakdown.nonActive(Double.NaN,0.0))
    }
    @Test fun goalsValidateAndAgeIsOptional() {
        PersonalGoals().validate()
        assertThrows(IllegalArgumentException::class.java) { PersonalGoals(0,500).validate() }
        assertThrows(IllegalArgumentException::class.java) { PersonalGoals(age=10).validate() }
    }
    private val today = LocalDate.of(2026,11,9)
    private fun days() = (1L..7L).map { ConnectedDay(today.minusDays(it).toString(), coveredMinutes=600, steps=8000, asleep=480, recovery=80, zones="300,100,30,0,0,0,0") }
    @Test fun fitnessAgeRequiresSevenCompleteDaysAndAge() {
        assertNull(FitnessAgeModel.calculate(days(),null,today).age)
        assertNull(FitnessAgeModel.calculate(days().take(6),30,today).age)
        assertNull(FitnessAgeModel.calculate(days().map { it.copy(coveredMinutes=0) },30,today).age)
    }
    @Test fun fitnessAgeExcludesTodayAndDuplicateDates() {
        val d=days().take(6)
        assertNull(FitnessAgeModel.calculate(d + d.first() + d.first().copy(date=today.toString()),30,today).age)
    }
    @Test fun fitnessAgeIsBoundedAndExplainsItsInputs() {
        val result=FitnessAgeModel.calculate(days(),30,today)
        assertTrue(result.age!! in 25.0..35.0); assertEquals(4,result.factors.size)
        assertTrue(FitnessAgeModel.calculate(days(),18,today).age!! >= 18)
        val low=FitnessAgeModel.calculate(days().map { it.copy(steps=0,asleep=60,recovery=0,zones="0,0,0,0,0,0,0") },30,today)
        assertEquals(35.0,low.age!!,.001)
    }
    @Test fun hourlyGapsRemainEmptyAndDstHas25Buckets() {
        val zone = java.time.ZoneId.of("America/New_York")
        val day = LocalDate.of(2026,11,1)
        val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val hours = HourBucket.fill(start,end,listOf(HourBucket(start+3600000,start+7200000,500,20.0,80.0)))
        assertEquals(25,hours.size); assertNull(hours.first().steps); assertEquals(500L,hours[1].steps)
    }}
