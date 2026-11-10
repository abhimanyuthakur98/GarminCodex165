package com.garmincodex.app
import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35], application=Application::class)
class GoalsPersistenceTest {
    @Test fun savedGoalsSurviveRepositoryRecreation() {
        val context=ApplicationProvider.getApplicationContext<Application>()
        context.getSharedPreferences("health_sync",0).edit().clear().commit()
        val db=Room.inMemoryDatabaseBuilder(context,PulseDatabase::class.java).build()
        val goals=PersonalGoals(12000,650,32,390)
        HealthConnectSync(context,db.days()).saveGoals(goals)
        assertEquals(goals,HealthConnectSync(context,db.days()).goals.value)
        val dashboard = DashboardPreferences(hidden=setOf("sleep"), startPage=1).move("heart", -1)
        HealthConnectSync(context,db.days()).saveDashboard(dashboard)
        assertEquals(dashboard,HealthConnectSync(context,db.days()).dashboardPreferences.value)
        HealthConnectSync(context,db.days()).saveDistanceMiles(true)
        assertTrue(HealthConnectSync(context,db.days()).distanceMiles.value)
        db.close()
    }
}
