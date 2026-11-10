package com.garmincodex.app

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.runtime.*
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
@Config(sdk = [35], application = Application::class, qualifiers = "w412dp-h915dp-420dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DashboardUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun realDashboardRendersAndTabsNavigate() {
        System.setProperty("roborazzi.test.record", "true")
        val day = previewDay(LocalDate.now().toString())
        compose.setContent { PulseTheme { PulseDashboard(listOf(day), SyncState(lastSync = System.currentTimeMillis()), DashboardState(), "", null, 480, 190, 55, {}, {}, {}, {}, {}, { _, _, _ -> }, {}, {}, {}) } }
        compose.onRoot().captureRoboImage("build/screenshots/home-refresh.png")
        compose.onNodeWithText("Ready for your day").performScrollTo().assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/screenshots/dashboard.png")
        compose.onNodeWithText("Sleep").performScrollTo().performClick()
        compose.onNodeWithText("WAKE CONSISTENCY").performScrollTo().assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/screenshots/sleep.png")
        compose.onNodeWithText("Strain").performScrollTo().performClick()
        compose.onNodeWithText("CARDIOVASCULAR LOAD").assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/screenshots/strain.png")
        compose.onNodeWithText("Trends").performClick()
        compose.onNodeWithText("Your rhythm").performScrollTo().assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/screenshots/trends.png")
        compose.onNodeWithText("Connect").performClick()
        compose.onNodeWithText("Connect Health Connect").performScrollTo().assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/screenshots/connect.png")
    }
    @Test fun goalsAndDetailViewsAreInteractive() {
        System.setProperty("roborazzi.test.record", "true")
        val today = LocalDate.now()
        var goals by mutableStateOf(PersonalGoals(age = 30))
        val days = (0L..10L).map { previewDay(today.minusDays(it).toString()) }
        compose.setContent { PulseTheme { PulseDashboard(days, SyncState(lastSync = System.currentTimeMillis()), DashboardState(), "", null, 480, 190, 55, {}, {}, {}, {}, {}, { _, _, _ -> }, {}, {}, {}, goals = goals, onGoals = { goals = it }) } }
        compose.onNodeWithTag("goal-Steps").performScrollTo().performClick()
        compose.onNodeWithText("Keep moving.").assertIsDisplayed()

        compose.onNodeWithText("Edit goal").performClick()

        compose.onNodeWithTag("goal-input").performTextClearance()
        compose.onNodeWithTag("goal-input").performTextInput("10000")
        compose.onNodeWithText("Save goal").performClick()


        compose.runOnIdle { org.junit.Assert.assertEquals(10000, goals.steps) }
        compose.onNodeWithText("HOURLY STEPS").performScrollTo()
        compose.onNode(isDialog()).captureRoboImage("build/screenshots/steps-detail.png")
        compose.onNodeWithContentDescription("Select activity hour").performScrollTo().performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) { it(12f) }
        compose.onNodeWithText("‹ Back").performScrollTo().performClick()
        compose.onNodeWithTag("goal-Calories").performScrollTo().performClick()
        compose.onNodeWithText("Energy, unpacked.").assertIsDisplayed()
        compose.onNodeWithText("CALORIE BREAKDOWN").performScrollTo()
        compose.onNode(isDialog()).captureRoboImage("build/screenshots/calories-detail.png")
        compose.onNodeWithText("‹ Back").performScrollTo().performClick()
        compose.onNodeWithText("Fitness Age").performClick()
        compose.onNodeWithText("EXPERIMENTAL FITNESS AGE").assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/screenshots/fitness-age.png")
        compose.onNodeWithText("Today").performClick()
        compose.onNodeWithText("HEART RATE • DAILY").performScrollTo().performClick()
        compose.onNodeWithText("Every heartbeat.").assertIsDisplayed()
        compose.onNodeWithContentDescription("Select heart-rate sample").performScrollTo().performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) { it(12f) }
        compose.onNode(isDialog()).captureRoboImage("build/screenshots/heart-detail.png")
    }    @Test fun cardGoalAndAgeEditorsSave() {
        val today=LocalDate.now()
        var goals by mutableStateOf(PersonalGoals())
        compose.setContent { PulseTheme { PulseDashboard(listOf(previewDay(today.toString())), SyncState(lastSync=1), DashboardState(), "", null, 480,190,55, {},{},{},{},{},{_,_,_->},{},{},{}, goals=goals, onGoals={ goals=it }) } }
        compose.onNodeWithTag("goal-Calories").performScrollTo()
        compose.onAllNodesWithText("Edit")[1].performClick()
        compose.onNodeWithTag("goal-input").performTextReplacement("600")
        compose.onNodeWithText("Save goal").performClick()
        compose.runOnIdle { org.junit.Assert.assertEquals(600,goals.activeCalories) }
        compose.onNodeWithText("Fitness Age").performClick()
        compose.onNodeWithText("Set age").performClick()
        compose.onNodeWithTag("goal-input").performTextInput("32")
        compose.onNodeWithText("Save age").performClick()
        compose.runOnIdle { org.junit.Assert.assertEquals(32,goals.age) }
    }}
