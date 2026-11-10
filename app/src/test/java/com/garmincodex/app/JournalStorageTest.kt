package com.garmincodex.app
import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],application=Application::class)
class JournalStorageTest {
    @Test fun dailyEditsReplaceAndWatchClearPreservesJournal()=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(),PulseDatabase::class.java).build()
        try {
            val dao=db.days();val entry=JournalEntry("2026-01-01",3,2,null,"Morning",false,"tag=Y")
            dao.putHabit(HabitTag("tag","Walk"));dao.putJournal(entry);dao.putJournal(entry.copy(energy=5));dao.clearAllImports()
            assertEquals(5,dao.journal().first().single().energy);assertEquals(1,dao.habits().first().size)
            dao.clearPersonalJournal();assertTrue(dao.journal().first().isEmpty());assertTrue(dao.habits().first().isEmpty())
        } finally {db.close()}
    }
}
