package com.garmincodex.app

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],application=Application::class,qualifiers="w390dp-h844dp-320dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class VisualRefreshTest {
    @get:Rule val compose=createComposeRule()
    @Test fun primaryScreensAndSettingsRemainReachable() {
        System.setProperty("roborazzi.test.record","true")
        val today=LocalDate.now()
        val now=System.currentTimeMillis()
        val days=(0L..28L).map { previewDay(today.minusDays(it).toString()).copy(activitySource=ActivitySource.GARMIN,updatedAt=now) }
        val workout=Workout("visual","FIT","RUNNING","Morning run",now-3600000,now-1800000,"",250.0,distanceMeters=5000.0,timerSeconds=1800.0)
        compose.setContent { PulseTheme { PulseDashboard(days,SyncState(lastSync=now,granted=HealthConnectSync.READ_PERMISSIONS),DashboardState(),"",null,480,190,55,{},{},{},{},{},{_,_,_->},{},{},{},goals=PersonalGoals(age=30),workouts=listOf(workout)) } }
        compose.onNodeWithTag("goal-Steps").assertIsDisplayed()
        compose.onNodeWithTag("goal-Calories").assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/screenshots/overhaul-home.png")
        compose.onNodeWithText("Trends").performClick()
        compose.onNodeWithText("YOUR HISTORY").assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/screenshots/overhaul-trends.png")
        compose.onNodeWithText("Fitness Age").performClick()
        compose.onNodeWithText("YOUR HABITS").assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/screenshots/overhaul-fitness.png")
        compose.onNodeWithText("Workouts").performClick()
        compose.onNodeWithText("Morning run").assertExists()
        compose.onRoot().captureRoboImage("build/screenshots/overhaul-workouts.png")
        compose.onNodeWithContentDescription("Open Settings").performClick()
        compose.onNodeWithText("Connections & imports").assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/screenshots/overhaul-settings.png")
        compose.onNodeWithText("Connections & imports").performClick()
        compose.onNodeWithText("YOUR HEALTH DATA").assertIsDisplayed()
    }
}
