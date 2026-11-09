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
class ExplorationUiTest {
    @get:Rule val compose=createComposeRule()
    @Test fun timelineBaselineAndCustomizationAreInteractive() {
        System.setProperty("roborazzi.test.record","true")
        val days=(0L..20L).map { previewDay(LocalDate.now().minusDays(it).toString()).copy(overnightHr=52.0+it%5,asleep=420+(it%4).toInt()*20) }
        var prefs by mutableStateOf(DashboardPreferences())
        compose.setContent { PulseTheme { Surface { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            TimelineCard(days.first()) {}
            BaselineCard(days)
            DashboardEditor(prefs) {prefs=it}
        } } } }
        compose.onNodeWithContentDescription("Select timeline hour").performScrollTo().performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress){it(12f)}
        compose.onRoot().captureRoboImage("build/screenshots/timeline.png")
        compose.onNodeWithContentDescription("Select Sleep duration date").performScrollTo().performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress){it(3f)}
        compose.onRoot().captureRoboImage("build/screenshots/baselines.png")
        compose.onNodeWithText("Customise dashboard").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Show Sleep").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Move Heart rate up").performScrollTo().performClick()
        compose.runOnIdle {assertTrue("sleep" in prefs.hidden);assertTrue(prefs.order.indexOf("heart")<prefs.order.indexOf("sleep"))}
        compose.onRoot().captureRoboImage("build/screenshots/customise.png")
        compose.onNodeWithText("Reset dashboard").performScrollTo().performClick()
        compose.runOnIdle {assertEquals(DashboardPreferences(),prefs)}
    }
}
