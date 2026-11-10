package com.garmincodex.app
import android.app.Application
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
@Config(sdk=[35],application=Application::class,qualifiers="w360dp-h800dp-320dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TodayUiTest {
    @get:Rule val compose=createComposeRule()
    @Test fun snapshotKeepsDetailsOptionalAndJournalFollowsSelectedDate() {
        System.setProperty("roborazzi.test.record","true")
        val today=LocalDate.now();val yesterday=today.minusDays(1).toString()
        var refresh=0
        compose.setContent { PulseTheme { PulseDashboard(listOf(previewDay(today.toString()),previewDay(yesterday)),SyncState(lastSync=System.currentTimeMillis()),DashboardState(),"",null,480,190,55,{},{refresh++},{},{},{},{_,_,_->},{},{},{}) } }
        compose.onNodeWithContentDescription("Pulse 165").assertIsDisplayed()
        compose.onNodeWithText("PULSE / 165").assertDoesNotExist()
        compose.onNodeWithContentDescription("Sync health data").performClick()
        compose.runOnIdle{assertEquals(1,refresh)}
        compose.onNodeWithText("YOUR MORNING BRIEFING").assertDoesNotExist()
        compose.onNodeWithText("Your movement").assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/screenshots/today-snapshot.png")
        compose.onNode(hasClickAction() and hasText(today.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy")),substring=true)).performClick()
        compose.onNodeWithContentDescription("Next day").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Previous day").performClick()
        compose.onNodeWithContentDescription("Next day").assertIsEnabled()
        compose.onNodeWithText("Journal").performClick()
        compose.onNodeWithText(yesterday).performScrollTo().assertIsDisplayed()
    }
    @Test fun emptySnapshotOffersConnectionWithoutFakeMetrics() {
        compose.setContent {PulseTheme {PulseDashboard(emptyList(),SyncState(message="Health access changed. Grant permissions and sync again."),DashboardState(),"",null,480,190,55,{},{},{},{},{},{_,_,_->},{},{},{})}}
        compose.onNodeWithText("Sync needs attention · review connection ›").assertIsDisplayed()
        compose.onNodeWithText("Your day starts here").assertIsDisplayed()
        compose.onNodeWithText("Your movement").assertDoesNotExist()
        compose.onNodeWithText("Connect Garmin ›").performClick()
        compose.onNodeWithText("Connect Health Connect").performScrollTo().assertIsDisplayed()
    }
}
