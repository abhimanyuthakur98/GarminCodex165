package com.garmincodex.app

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
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
@Config(sdk=[35], application=Application::class, qualifiers="w360dp-h800dp-320dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CoachUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun plannerSavesAndStatusDistinguishesMissingAccess() {
        System.setProperty("roborazzi.test.record", "true")
        var goals by mutableStateOf(PersonalGoals())
        var permissionsRequested = false
        val days = (0L..14L).map { previewDay(LocalDate.now().minusDays(it).toString()) }
        compose.setContent { PulseTheme { Surface { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            MorningBriefing(days.first(), days, LocalDate.now().toString(), 480)
            SleepPlanner(days, goals, 480) { goals = it }
            WeeklyReport(days, 480)
            DataStatusCard(days.first(), SyncState(lastSync = 1), { permissionsRequested = true }, {})
        } } } }
        compose.onRoot().captureRoboImage("build/screenshots/briefing.png")
        compose.onNodeWithText("Change wake time").performScrollTo().performClick()
        compose.onNodeWithTag("wake-input").performScrollTo().performTextReplacement("06:30")
        compose.onNodeWithText("Save wake time").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(390, goals.wakeMinute) }
        compose.onNodeWithText("SLEEP PLANNER").performScrollTo()
        compose.onRoot().captureRoboImage("build/screenshots/planner.png")
        compose.onNodeWithText("YOUR WEEKLY REPORT").performScrollTo()
        compose.onRoot().captureRoboImage("build/screenshots/weekly-report.png")
        compose.onNodeWithText("Check data categories").performScrollTo().performClick()
        compose.onNodeWithText("Active calories · Permission needed").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Review Pulse permissions").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(permissionsRequested) }
    }
}
