package com.garmincodex.app

import android.app.Application
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
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
@Config(sdk=[35],application=Application::class,qualifiers="w390dp-h844dp-320dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeArrangeTest {
    @get:Rule val compose=createComposeRule()
    @Test fun holdOpensArrangementDragPersistsAndTapStillOpensDetails() {
        System.setProperty("roborazzi.test.record","true")
        var prefs by mutableStateOf(DashboardPreferences())
        val day=previewDay(LocalDate.now().toString()).copy(activitySource=ActivitySource.GARMIN)
        compose.setContent { PulseTheme { PulseDashboard(listOf(day),SyncState(lastSync=1),DashboardState(),"",null,480,190,55,{},{},{},{},{},{_,_,_->},{},{},{},preferences=prefs,onPreferences={prefs=it}) } }
        compose.onNodeWithContentDescription("Edit home").assertDoesNotExist()
        compose.onNodeWithTag("goal-Steps").performTouchInput { longClick() }
        compose.onNodeWithText("Arrange Home").assertIsDisplayed()
        compose.onNodeWithText("Edit goal").assertDoesNotExist()
        compose.onNode(isDialog()).captureRoboImage("build/screenshots/arrange-home.png")
        val grid=compose.onNodeWithTag("arrange-grid")
        val origin=grid.fetchSemanticsNode().boundsInRoot.topLeft
        val first=compose.onNodeWithTag("arrange-activity").fetchSemanticsNode().boundsInRoot.center-origin
        val second=compose.onNodeWithTag("arrange-sleep").fetchSemanticsNode().boundsInRoot.center-origin
        grid.performTouchInput { swipe(first,second,600) }
        compose.runOnIdle { assertEquals("sleep",prefs.homeOrder.first()) }
        compose.onNodeWithContentDescription("Hide Sleep").performClick()
        compose.runOnIdle { assertTrue("sleep" in prefs.homeHidden) }
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithTag("goal-Steps").performScrollTo().performClick()
        compose.onNodeWithText("Edit goal").assertIsDisplayed()
    }
    @Test fun cancelledDragDoesNotSaveAnOrder() {
        var prefs by mutableStateOf(DashboardPreferences())
        compose.setContent { PulseTheme { HomeArrange(prefs,null,{prefs=it},{},{}) } }
        val grid=compose.onNodeWithTag("arrange-grid")
        val origin=grid.fetchSemanticsNode().boundsInRoot.topLeft
        val first=compose.onNodeWithTag("arrange-activity").fetchSemanticsNode().boundsInRoot.center-origin
        grid.performTouchInput { down(first);moveBy(Offset(60f,0f));cancel() }
        compose.runOnIdle { assertEquals(DashboardPreferences.HOME_CARDS,prefs.homeOrder) }
    }

    @Test fun verticalDragReordersRatherThanScrollingTheGrid() {
        var prefs by mutableStateOf(DashboardPreferences())
        compose.setContent { PulseTheme { HomeArrange(prefs,null,{prefs=it},{},{}) } }
        val grid=compose.onNodeWithTag("arrange-grid")
        val origin=grid.fetchSemanticsNode().boundsInRoot.topLeft
        val from=compose.onNodeWithTag("arrange-activity").fetchSemanticsNode().boundsInRoot.center-origin
        val to=compose.onNodeWithTag("arrange-recovery").fetchSemanticsNode().boundsInRoot.center-origin
        grid.performTouchInput { swipe(from,to,600) }
        compose.runOnIdle { assertEquals("activity",prefs.homeOrder[2]) }
    }
}
