package com.garmincodex.app
import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],application=Application::class)
class RecentImportTest {
    @Test fun recentReplacementPreservesHistoryAndReconcilesDeletedRecentData()=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(),PulseDatabase::class.java).build()
        try {
            val dao=db.days()
            dao.putConnected(listOf(ConnectedDay("2026-11-01",steps=9000),ConnectedDay("2026-11-15",steps=5000),ConnectedDay("2026-11-16",steps=8000)))
            dao.replaceHealthImport("2026-11-15",0,listOf(ConnectedDay("2026-11-15"),ConnectedDay("2026-11-16",steps=14000)),null)
            assertEquals(9000L,dao.day("2026-11-01")?.steps)
            assertNull(dao.day("2026-11-15")?.steps)
            assertEquals(14000L,dao.day("2026-11-16")?.steps)
        } finally {db.close()}
    }
}
