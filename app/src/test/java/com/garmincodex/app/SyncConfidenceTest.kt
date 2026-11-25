package com.garmincodex.app

import org.junit.Assert.*
import org.junit.Test

class SyncConfidenceTest {
    @Test fun permissionAndRejectedQueriesTakePriorityOverSavedValues() {
        assertEquals("Permission needed",categoryImportStatus(false,true,true))
        assertEquals("Refresh failed · saved value",categoryImportStatus(true,true,true))
        assertEquals("Refresh failed · no value",categoryImportStatus(true,false,true))
        assertEquals("Saved value",categoryImportStatus(true,true,false))
        assertEquals("No shared record for this day",categoryImportStatus(true,false,false))
    }
    @Test fun failedCategoriesDoNotMarkUnrelatedMetricsAsRejected() {
        val state=SyncState(unavailable=setOf("active calories","SleepSessionRecord"))
        assertTrue(unavailableForMetric(state,"calories"))
        assertTrue(unavailableForMetric(state,"sleep"))
        assertTrue(unavailableForMetric(state,"recovery"))
        assertFalse(unavailableForMetric(state,"steps"))
        assertFalse(unavailableForMetric(state,"hr"))
        assertFalse(unavailableForMetric(SyncState(),"sleep"))
    }
}
