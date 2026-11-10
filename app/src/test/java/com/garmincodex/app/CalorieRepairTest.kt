package com.garmincodex.app
import android.app.Application
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],application=Application::class)
class CalorieRepairTest {
 @get:Rule val compose=createComposeRule()
 @Test fun totalEnergyIsNotActiveAndRepairActionsAreReachable(){
  var allowed by mutableStateOf(false);var settings=0;var garmin=0;var imported=0
  val d=previewDay(LocalDate.now().toString()).copy(activeCalories=null,calories=2100.0)
  compose.setContent{PulseTheme{PulseDashboard(listOf(d),SyncState(lastSync=1,granted=if(allowed)setOf(HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)) else emptySet()),DashboardState(),"",null,480,190,55,{allowed=true},{imported++},{settings++},{garmin++},{},{_,_,_->},{},{},{})}}
  compose.onNodeWithText("Not shared").assertDoesNotExist()
  compose.onNodeWithText("Total calories").performScrollTo().assertIsDisplayed()
  compose.onNodeWithText("2100 kcal").assertExists()
  compose.onNodeWithText("Allow active calories · Fix ›").performClick()
  compose.onNodeWithText("Pulse needs read access").performScrollTo().assertIsDisplayed()
  compose.onNodeWithText("1 · Review Pulse access").performScrollTo().performClick()
  compose.onNodeWithText("No active-calorie record for this day").performScrollTo().assertIsDisplayed()
  compose.onNodeWithText("2 · Garmin sharing settings").performScrollTo().performClick()
  compose.onNodeWithText("3 · Sync in Garmin").performScrollTo().performClick()
  compose.onNodeWithText("4 · Import again").performScrollTo().performClick()
  compose.runOnIdle{assertEquals(1,settings);assertEquals(1,garmin);assertEquals(1,imported)}
 }
}
