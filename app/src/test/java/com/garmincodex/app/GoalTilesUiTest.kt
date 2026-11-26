package com.garmincodex.app
import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
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
@Config(sdk=[35],application=Application::class,qualifiers="w320dp-h840dp-320dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GoalTilesUiTest {
    @get:Rule val compose=createComposeRule()
    @Test fun sideBySideGoalsAndFiniteReplayableCelebrations() {
        System.setProperty("roborazzi.test.record","true")
        compose.mainClock.autoAdvance=false
        val day=previewDay(LocalDate.now().toString()).copy(steps=14000,activeCalories=650.0,asleep=490)
        compose.setContent{PulseTheme{Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
            ActivityTiles(day,PersonalGoals(),1f,{},SyncState())
            SleepHero(day,480){}
        }}}
        compose.mainClock.advanceTimeBy(350)
        compose.onNodeWithText("14,000").assertIsDisplayed()
        compose.onNodeWithText("650").assertIsDisplayed()
        val steps=compose.onNodeWithTag("goal-Steps").getUnclippedBoundsInRoot()
        val calories=compose.onNodeWithTag("goal-Calories").getUnclippedBoundsInRoot()
        assertEquals(steps.top,calories.top)
        assertTrue(steps.right<calories.left)
        compose.onAllNodesWithTag("goal-confetti",useUnmergedTree=true).assertCountEquals(3)
        compose.onRoot().captureRoboImage("build/screenshots/goal-tiles-celebration.png")
        compose.mainClock.advanceTimeBy(3000)
        compose.onAllNodesWithTag("goal-confetti",useUnmergedTree=true).assertCountEquals(0)
        compose.mainClock.autoAdvance=true
        compose.onNodeWithContentDescription("Celebrate Steps goal").performClick()
        compose.onNodeWithTag("replay-Steps-1").assertExists()
        compose.onAllNodesWithTag("goal-confetti",useUnmergedTree=true).assertCountEquals(0)
        compose.onNodeWithContentDescription("Celebrate Sleep goal").performClick()
        compose.onNodeWithTag("replay-Sleep-1").assertExists()
        compose.onAllNodesWithTag("goal-confetti",useUnmergedTree=true).assertCountEquals(0)
    }
    @Test fun missingMetricsCannotCelebrate() {
        compose.setContent{PulseTheme{Column{ActivityTiles(null,PersonalGoals(),1f,{},SyncState());SleepHero(null,480){}}}}
        compose.onAllNodesWithTag("goal-confetti",useUnmergedTree=true).assertCountEquals(0)
        compose.onNodeWithText("No sleep total").assertExists()
        compose.onNodeWithContentDescription("Celebrate Steps goal").assertDoesNotExist()
    }

    @Test fun totalEnergyFallbackKeepsCardsEqualAtLargerText() {
        System.setProperty("roborazzi.test.record","true")
        val day=previewDay(LocalDate.now().toString()).copy(activeCalories=null,calories=2100.0)
        compose.setContent { PulseTheme {
            val density=androidx.compose.ui.platform.LocalDensity.current
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density,1.3f)) {
                Column(Modifier.padding(12.dp)) { ActivityTiles(day,PersonalGoals(),1f,{},SyncState(granted=HealthConnectSync.READ_PERMISSIONS)) }
            }
        } }
        compose.onNodeWithText("2,100").assertIsDisplayed()
        compose.onNodeWithText("total kcal · active + resting").assertIsDisplayed()
        compose.onNodeWithContentDescription("Celebrate Calories goal").assertDoesNotExist()
        val steps=compose.onNodeWithTag("card-Steps").getUnclippedBoundsInRoot()
        val calories=compose.onNodeWithTag("card-Calories").getUnclippedBoundsInRoot()
        assertEquals(steps.height,calories.height)
        assertEquals(steps.width,calories.width)
        val caption=compose.onNodeWithText("Active not shared · details ›").getUnclippedBoundsInRoot()
        assertTrue(caption.bottom<=calories.bottom)
        compose.onRoot().captureRoboImage("build/screenshots/calorie-total-large-text.png")
    }
    @Test fun iconCategoriesExposeNamesAndSelection() {
        var selected by androidx.compose.runtime.mutableStateOf("Favourites")
        compose.setContent { PulseTheme { HomeSections(selected,{selected=it}) } }
        listOf("Favourites","Activity","Sleep hub","Vitals","Weekly").forEach { label ->
            compose.onNodeWithText(label).assertDoesNotExist()
            compose.onNodeWithContentDescription(label).performClick().assertIsSelected()
        }
    }
}
