package com.garmincodex.app
import org.junit.Assert.*
import org.junit.Test
class SyncWindowTest {
    @Test fun fullImportForFirstRunDailyReconciliationAndPermissionChanges() {
        val now=200000000L
        assertTrue(SyncWindow.full(false,0,now,false))
        assertFalse(SyncWindow.full(false,now-60000,now,false))
        assertTrue(SyncWindow.full(false,now-86400000,now,false))
        assertTrue(SyncWindow.full(false,now-60000,now,true))
        assertTrue(SyncWindow.full(true,now-60000,now,false))
        assertTrue(SyncWindow.full(false,now+100,now,false))
    }
}
