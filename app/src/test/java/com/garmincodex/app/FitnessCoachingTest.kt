package com.garmincodex.app
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
class FitnessCoachingTest {
 private val result=FitnessAgeResult(28.0,80,7,listOf(FitnessFactor("Daily movement",80.0,""),FitnessFactor("Sleep duration",90.0,""),FitnessFactor("Recovery",80.0,""),FitnessFactor("Cardio activity",20.0,"")),"")
 @Test fun weakestFactorIsSuggestedButLowRecoveryTakesPriority(){assertEquals(3,FitnessCoaching.focus(result));assertEquals(2,FitnessCoaching.focus(result,20));assertEquals(2,FitnessCoaching.focus(result.copy(factors=result.factors.map{if(it.name=="Recovery")it.copy(score=10.0) else it})));assertTrue(FitnessCoaching.suggestion(result.factors[3],true).action.contains("recovery is low"))}
 @Test fun historicalEstimatesNeverUseFutureOrSameDayData(){val date=LocalDate.of(2026,11,10);val past=(1L..7L).map{ConnectedDay(date.minusDays(it).toString(),steps=8000,asleep=480,recovery=80,coveredMinutes=600,zones="0,0,30,0,0,0,0")};val original=FitnessAgeModel.calculate(past,30,date);val extra=ConnectedDay(date.toString(),steps=0,asleep=0,recovery=0,coveredMinutes=600);assertEquals(original,FitnessAgeModel.calculate(past+extra+extra.copy(date=date.plusDays(1).toString()),30,date))}
}
