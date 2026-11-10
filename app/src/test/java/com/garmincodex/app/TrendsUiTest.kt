package com.garmincodex.app
import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
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
@Config(sdk=[35],application=Application::class,qualifiers="w360dp-h800dp-320dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TrendsUiTest {
 @get:Rule val compose=createComposeRule()
 @Test fun focusedViewsSwitchMetricPeriodAndOpenSelectedDay() {
  System.setProperty("roborazzi.test.record","true")
  val today=LocalDate.now();val days=(0L..29L).map{previewDay(today.minusDays(it).toString()).copy(asleep=400+it.toInt(),steps=if(it==0L)0 else 8000)}
  var opened=""
  compose.setContent{PulseTheme{Surface{Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){TrendsExplorer(days){opened=it}}}}}
  compose.onRoot().captureRoboImage("build/screenshots/trends-focused.png")
  compose.onNodeWithText("7 days ▾").performClick();compose.onNodeWithText("30 days").performClick()
  compose.onNodeWithContentDescription("Select trend date").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress){it(0f)}
  compose.onNodeWithText("Open day ›").performClick();compose.runOnIdle{assertEquals(today.minusDays(29).toString(),opened)}
  compose.onNodeWithText("Sleep ▾").performClick();compose.onNodeWithText("Steps").performClick()
  compose.onNodeWithText("0 steps").assertExists()
  compose.onNodeWithText("Usual range").performClick();compose.onNodeWithText("Usual:",substring=true).assertExists()
  compose.onNodeWithText("Weekly").performClick();compose.onNodeWithText("WEEKLY AVERAGES").assertIsDisplayed()
  compose.onNodeWithContentDescription("Select trend date").assertDoesNotExist()
  compose.onRoot().captureRoboImage("build/screenshots/trends-weekly.png")
 }
 @Test fun zeroIsNotMissingAndUnavailableStrainStaysUnknown(){assertEquals("0 kcal",TrendMetric.CALORIES.format(0.0));assertEquals("No record",TrendMetric.CALORIES.format(null));assertNull(TrendMetric.STRAIN.value(ConnectedDay("2026-11-10",load=30.0)));assertEquals(0.0,TrendMetric.STEPS.value(ConnectedDay("2026-11-10",steps=0))!!,0.0)}
}
