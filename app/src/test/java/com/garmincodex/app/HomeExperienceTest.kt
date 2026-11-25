package com.garmincodex.app

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],application=Application::class,qualifiers="w360dp-h800dp-320dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeExperienceTest {
    @get:Rule val compose=createComposeRule()
    @Test fun hidingSleepOnlyRemovesItsFavouriteNotTheSleepHub() {
        var prefs by mutableStateOf(DashboardPreferences())
        val day=previewDay(LocalDate.now().toString())
        compose.setContent{PulseTheme{PulseDashboard(listOf(day),SyncState(lastSync=1),DashboardState(),"",null,480,190,55,{},{},{},{},{},{_,_,_->},{},{},{},preferences=prefs,onPreferences={prefs=it})}}
        compose.onNodeWithText("Edit home").performClick()
        compose.onNodeWithContentDescription("Pin Sleep").performClick()
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithText("Sleep").assertDoesNotExist()
        compose.onNodeWithText("Sleep hub").performClick()
        compose.onNodeWithText("Your sleep").assertIsDisplayed()
        compose.onNodeWithText("7h 55m").assertExists()
    }
    @Test fun favouritesCanBeHiddenReorderedAndPersisted() {
        var prefs by mutableStateOf(DashboardPreferences())
        compose.setContent{PulseTheme{HomeEditor(prefs,{prefs=it}){}}}
        compose.onNodeWithContentDescription("Pin Sleep").performClick()
        compose.onNodeWithContentDescription("Move favourite Sleep up").performClick()
        compose.runOnIdle{assertTrue("sleep" in prefs.homeHidden);assertEquals("sleep",prefs.homeOrder.first())}
        val context=ApplicationProvider.getApplicationContext<Application>()
        val db=Room.inMemoryDatabaseBuilder(context,PulseDatabase::class.java).build()
        try {HealthConnectSync(context,db.days()).saveDashboard(prefs);assertEquals(prefs,HealthConnectSync(context,db.days()).dashboardPreferences.value)}finally{db.close()}
        compose.onRoot().captureRoboImage("build/screenshots/home-editor.png")
    }
    @Test fun periodNavigationNeverReusesARecordForAnEmptyDay() {
        System.setProperty("roborazzi.test.record","true")
        val today=LocalDate.now()
        val data=listOf(previewDay(today.toString()).copy(steps=12345,activitySource=ActivitySource.GARMIN),previewDay(today.minusDays(2).toString()).copy(steps=2222,activitySource=ActivitySource.GARMIN))
        compose.setContent{PulseTheme{MetricDetails("steps",data.first(),PersonalGoals(),480,190,55,{},history=data){}}}
        compose.onNodeWithText("12,345").assertExists()
        compose.onNodeWithContentDescription("Previous metric period").performScrollTo().performClick()
        compose.onNodeWithText("12,345").assertDoesNotExist()
        compose.onNodeWithText("Not imported yet").assertExists()
        compose.onNodeWithText("Week").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Select trend date").performScrollTo().assertIsDisplayed()
        compose.onNode(isDialog()).captureRoboImage("build/screenshots/metric-week.png")
    }
    @Test fun sleepAndWeeklyHubsAreReachableFromHome() {
        System.setProperty("roborazzi.test.record","true")
        val today=LocalDate.now()
        val days=(0L..15L).map{previewDay(today.minusDays(it).toString()).copy(updatedAt=1,activitySource=ActivitySource.GARMIN)}
        compose.setContent{PulseTheme{PulseDashboard(days,SyncState(lastSync=1),DashboardState(),"",null,480,190,55,{},{},{},{},{},{_,_,_->},{},{},{})}}
        compose.onNodeWithText("Sleep hub").performClick()
        compose.onNodeWithText("Your sleep").assertIsDisplayed()
        compose.onNodeWithText("Change wake time").performScrollTo().assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/screenshots/sleep-hub.png")
        compose.onNodeWithText("Weekly").performScrollTo().performClick()
        compose.onNodeWithText("YOUR WEEKLY REPORT").assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/screenshots/weekly-hub.png")
    }
    @Test fun metadataAndNormalisationDoNotInventData() {
        assertEquals("Not imported yet",importStamp(null))
        assertEquals("Activity source not confirmed",metricSource(ConnectedDay("2026-11-25"),"steps"))
        assertEquals("Garmin Connect · Health Connect",metricSource(ConnectedDay("2026-11-25",activitySource=ActivitySource.GARMIN),"steps"))
        val prefs=DashboardPreferences(homeOrder=listOf("sleep","sleep","obsolete"),homeHidden=setOf("obsolete","sleep")).normalized()
        assertEquals(DashboardPreferences.HOME_CARDS.size,prefs.homeOrder.size);assertEquals("sleep",prefs.homeOrder.first());assertEquals(setOf("sleep"),prefs.homeHidden)
    }
}
