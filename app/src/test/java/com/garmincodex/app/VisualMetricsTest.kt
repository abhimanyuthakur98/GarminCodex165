package com.garmincodex.app
import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
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
class VisualMetricsTest {
    @get:Rule val compose=createComposeRule()
    @Test fun sleepSelectionDistinguishesMissingAndZeroAndCoverageHasLabels() {
        System.setProperty("roborazzi.test.record","true")
        val today=LocalDate.now()
        val days=listOf(ConnectedDay(today.minusDays(1).toString(),asleep=0),ConnectedDay(today.minusDays(3).toString(),asleep=420,steps=5000,hrCount=100),ConnectedDay(today.toString(),steps=0))
        compose.setContent { PulseTheme { Surface { Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            WeekSleepChart(days,today,480)
            MetricBars(listOf(Triple("Current",420.0,"7h"),Triple("Previous",360.0,"6h")))
            ImportCoverage(days)
        } } } }
        compose.onNodeWithContentDescription("${today.minusDays(2)}: No sleep record").performClick()
        compose.onNodeWithText("No sleep record",substring=true).assertIsDisplayed()
        compose.onNodeWithContentDescription("${today.minusDays(1)}: 0 minutes asleep").performClick()
        compose.onNodeWithText("0h 0m",substring=true).assertIsDisplayed()
        compose.onNodeWithContentDescription("Steps $today: Data present").assertExists()
        compose.onRoot().captureRoboImage("build/screenshots/visual-metrics.png")
    }
}
