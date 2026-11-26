package com.garmincodex.app

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Test

class DailyUseTest {
    @Test fun progressDoesNotReprocessRecordsButSourceAndDataChangesDo() = runBlocking {
        val records=MutableStateFlow(listOf(ConnectedDay("2026-11-26",steps=123,asleep=480,activitySource=ActivitySource.GARMIN)))
        val status=MutableStateFlow(SyncState())
        val outputs=Channel<CachedHealth>(Channel.UNLIMITED)
        val job=launch{cachedHealth(records,status).collect{outputs.send(it)}}
        try { withTimeout(3000) {
            val first=outputs.receive();assertTrue(first.loaded);assertEquals(123L,first.days.first().steps)
            repeat(20){status.value=status.value.copy(progress=it/20f,phase="Reading $it",busy=true);yield()}
            assertTrue(outputs.tryReceive().isFailure)
            status.value=status.value.copy(stepSource="other")
            val switched=outputs.receive();assertNull(switched.days.first().steps);assertEquals(480,switched.days.first().asleep)
            records.value=listOf(records.value.first().copy(steps=65,activitySource="other"))
            assertEquals(65L,outputs.receive().days.first().steps)
        } } finally {job.cancelAndJoin();outputs.close()}
    }
    @Test fun takeawaysDoNotCelebrateMissingOrFailedImports() {
        val goals=PersonalGoals()
        val day=ConnectedDay("2026-11-26",steps=8000,activeCalories=500.0)
        assertEquals("Both activity goals reached",dailyTakeaway(day,SyncState(),goals))
        assertTrue(dailyTakeaway(day.copy(activeCalories=null),SyncState(),goals).contains("missing"))
        assertTrue(dailyTakeaway(day,SyncState(lastError="Failed"),goals).contains("review"))
        assertEquals("Step goal reached · 1 active kcal to go",dailyTakeaway(day.copy(activeCalories=499.9),SyncState(),goals))
        assertFalse(dailyTakeaway(day.copy(steps=0,activeCalories=0.0),SyncState(),goals).contains("reached"))
    }
}
