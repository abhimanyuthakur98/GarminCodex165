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
class WorkoutStorageTest {
    @Test fun refreshRetainsRatingsAndDoesNotDeleteFitImports()=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(),PulseDatabase::class.java).build()
        try {
            val dao=db.days(); val hc=Workout("hc","HEALTH_CONNECT","RUNNING","Run",100,200,"");val fit=hc.copy(id="fit",source="FIT")
            dao.putWorkouts(listOf(hc,fit));dao.putCheckIn(WorkoutCheckIn(hc.id,7,2))
            dao.replaceWorkouts(0,listOf(hc.copy(title="Updated")))
            assertEquals(2,dao.workouts().first().size);assertEquals(7,dao.checkIns().first().single().effort)
            dao.replaceWorkouts(0,emptyList())
            assertEquals(listOf(fit),dao.workouts().first());assertTrue(dao.checkIns().first().isEmpty())
            dao.clearAllImports();assertTrue(dao.workouts().first().isEmpty())
        } finally {db.close()}
    }
}
