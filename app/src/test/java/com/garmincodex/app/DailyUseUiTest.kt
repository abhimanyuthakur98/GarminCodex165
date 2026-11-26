package com.garmincodex.app

import android.app.Application
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
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
@Config(sdk=[35],application=Application::class,qualifiers="w320dp-h800dp-320dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DailyUseUiTest {
    @get:Rule val compose=createComposeRule()
    @Test fun cacheLoadingNeverFlashesConnectionSetupAndKeepsSavedValuesDuringSync() {
        var loading by mutableStateOf(true)
        var day by mutableStateOf<ConnectedDay?>(null)
        compose.setContent{PulseTheme{Surface{Column{TodaySnapshot(day,SyncState(busy=true,lastSync=1),PersonalGoals(),480,{},{},cacheLoading=loading)}}}}
        compose.onNodeWithText("Loading saved health data…").assertIsDisplayed()
        compose.onNodeWithText("Your day starts here").assertDoesNotExist()
        compose.runOnIdle{day=previewDay(LocalDate.now().toString());loading=false}
        compose.onNodeWithText("8,642").assertExists()
        compose.onNodeWithText("Loading saved health data…").assertDoesNotExist()
    }
    @Test fun longPressReordersFavouritesWithLargerText() {
        System.setProperty("roborazzi.test.record","true")
        var prefs by mutableStateOf(DashboardPreferences())
        compose.setContent{CompositionLocalProvider(LocalDensity provides Density(2f,1.3f)){PulseTheme{HomeEditor(prefs,{prefs=it}){}}}}
        compose.onNodeWithContentDescription("Drag Sleep").performTouchInput {
            down(center);advanceEventTime(700);moveBy(Offset(0f,-170f));up()
        }
        compose.runOnIdle{assertEquals("sleep",prefs.homeOrder.first())}
        compose.onNodeWithContentDescription("Pin Sleep").performClick()
        compose.runOnIdle{assertTrue("sleep" in prefs.homeHidden)}
        compose.onNodeWithText("Done").assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/screenshots/home-editor-large-text.png")
    }
    @Test fun backFromSleepCategoryReturnsToFavourites() {
        lateinit var back:OnBackPressedDispatcher
        val day=previewDay(LocalDate.now().toString())
        compose.setContent{back=LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher;PulseTheme{PulseDashboard(listOf(day),SyncState(lastSync=1),DashboardState(),"",null,480,190,55,{},{},{},{},{},{_,_,_->},{},{},{})}}
        compose.onNodeWithText("Sleep hub").performScrollTo().performClick()
        compose.onNodeWithText("Your sleep").assertExists()
        compose.runOnIdle{back.onBackPressed()}
        compose.onNodeWithText("Daily activity").assertIsDisplayed()
        compose.onNodeWithText("Your sleep").assertDoesNotExist()
    }
}
