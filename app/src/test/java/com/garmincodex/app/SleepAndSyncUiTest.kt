package com.garmincodex.app

import android.app.Application
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
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
class SleepAndSyncUiTest {
    @get:Rule val compose=createComposeRule()
    @Test fun failedImportOffersRetryAndPermissionFixAndDisablesDuplicateRetry() {
        System.setProperty("roborazzi.test.record","true")
        var calls=0;var permissions=0;var busy by mutableStateOf(false)
        val day=previewDay(LocalDate.now().toString()).copy(activitySource=ActivitySource.GARMIN)
        compose.setContent { PulseTheme { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            DataStatusCard(day,SyncState(busy=busy,lastSync=1,lastError="Interrupted",unavailable=setOf("active calories")),{permissions++},{},onRetry={calls++;busy=true})
        } } }
        compose.onNodeWithText("Last refresh failed. Saved records are still shown.").assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/screenshots/sync-confidence.png")
        compose.onNodeWithText("Retry import").performClick()
        compose.onNodeWithText("Refreshing…").assertIsNotEnabled()
        compose.onNodeWithText("Check data categories").performScrollTo().performClick()
        compose.onNodeWithText("Active calories · Permission needed").performScrollTo().performClick()
        compose.runOnIdle{assertEquals(1,calls);assertEquals(1,permissions)}
    }
    @Test fun sleepRoutineExpandsAndMissingStagesAreNotZeroes() {
        System.setProperty("roborazzi.test.record","true")
        val end=LocalDate.now();val day=previewDay(end.toString()).copy(stages="")
        compose.setContent { PulseTheme { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            SleepTimingCard(listOf(day),end);SleepStagesCard(day)
        } } }
        compose.onNodeWithText("Show all 7 nights").performScrollTo().performClick()
        compose.onNodeWithText("Show recent nights").assertExists()
        compose.onNodeWithText("YOUR SLEEP ROUTINE").performScrollTo()
        compose.onRoot().captureRoboImage("build/screenshots/sleep-routine.png")
        compose.onNodeWithText("Sleep stages weren’t shared for this night. Session times and a sleep total may still be available.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("0h 0m").assertDoesNotExist()
    }
    @Test fun importWarningsSurviveReopeningSync() {
        val context=ApplicationProvider.getApplicationContext<Application>()
        context.getSharedPreferences("health_sync",Context.MODE_PRIVATE).edit().putStringSet("unavailable",setOf("SleepSessionRecord")).putString("lastImportError","Interrupted").commit()
        val db=Room.inMemoryDatabaseBuilder(context,PulseDatabase::class.java).build()
        try {
            val state=HealthConnectSync(context,db.days()).status.value
            assertEquals(setOf("SleepSessionRecord"),state.unavailable)
            assertEquals("Interrupted",state.lastError)
        } finally {db.close()}
    }
}
