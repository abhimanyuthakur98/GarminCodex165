package com.garmincodex.app

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class ImportGuardTest {
    @Test fun rejectedOptionalRequestDoesNotBlockOtherCategories() = runBlocking {
        val guard=ImportGuard();var calls=0
        assertNull(guard.query("hourly steps",null as Long?) { calls++;throw IllegalArgumentException("provider rejects request") })
        assertEquals(14000L,guard.query("daily steps",null as Long?) { 14000L })
        assertNull(guard.query("hourly steps",null as Long?) { calls++;1L })
        assertEquals(1,calls);assertEquals(setOf("hourly steps"),guard.unavailable)
    }
    @Test fun securityAndCancellationAreNeverSwallowed() = runBlocking {
        val guard=ImportGuard()
        try { guard.query("steps",0) { throw SecurityException() };fail() } catch (_:SecurityException) {}
        try { guard.query("steps",0) { throw CancellationException() };fail() } catch (_:CancellationException) {}
        assertTrue(guard.unavailable.isEmpty())
    }
    @Test fun unavailableCategoryKeepsPreviousValuesButOtherCategoriesRefresh() {
        val previous=ConnectedDay("2026-11-16",hrCount=30,chart="known",steps=8000,recovery=75,asleep=420)
        val fresh=ConnectedDay("2026-11-16",steps=14000,asleep=480)
        val merged=preserveUnavailableRecords(fresh,previous,setOf("HeartRateRecord"))
        assertEquals(14000L,merged.steps);assertEquals(480,merged.asleep);assertEquals("known",merged.chart);assertEquals(75,merged.recovery)
        assertEquals(fresh,preserveUnavailableRecords(fresh,previous,emptySet()))
    }
}
