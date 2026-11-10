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
class FitnessAgeUiTest {
 @get:Rule val compose=createComposeRule()
 @Test fun factorsOpenSuggestionsAndHistoryRemainsInteractive(){
  System.setProperty("roborazzi.test.record","true")
  val days=(0L..27L).map{previewDay(LocalDate.now().minusDays(it).toString()).copy(recovery=if(it==0L)20 else 80)}
  compose.setContent{PulseTheme{Surface{Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){FitnessAgePage(days,PersonalGoals(age=30)) {}}}}}
  compose.onNodeWithText("Pause motion").performScrollTo().performClick();compose.onNodeWithText("Play motion").assertExists()
  compose.onRoot().captureRoboImage("build/screenshots/fitness-overview.png")
  compose.onNodeWithText("Focus: Recovery ›").performScrollTo().performClick()
  compose.onNodeWithText("Leave room for recovery").assertIsDisplayed()
  compose.onNodeWithContentDescription("Cardio activity:",substring=true).performClick()
  compose.onNodeWithText("Build consistency after rest").assertIsDisplayed()
  compose.onRoot().captureRoboImage("build/screenshots/fitness-actions.png")
  compose.onNodeWithText("Progress").performScrollTo().performClick()
  compose.onNodeWithContentDescription("Select fitness age date").performScrollTo().performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress){it(0f)}
  compose.onNodeWithText("14/14 estimates",substring=true).assertExists()
  compose.onRoot().captureRoboImage("build/screenshots/fitness-progress.png")
 }
 @Test fun noHistoryDoesNotGeneratePersonalisedAdvice(){compose.setContent{PulseTheme{Column{FitnessAgePage(emptyList(),PersonalGoals()) {}}}};compose.onNodeWithText("Actions").performClick();compose.onNodeWithText("Add chronological age").assertExists();compose.onNodeWithText("YOUR NEXT SMALL STEP").assertDoesNotExist()}
}
