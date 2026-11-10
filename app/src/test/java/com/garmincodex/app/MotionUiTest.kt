package com.garmincodex.app

import android.app.Application
import androidx.compose.foundation.layout.*
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "w320dp-h740dp-320dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MotionUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun narrowRingsAndMissingActiveEnergy() {
        System.setProperty("roborazzi.test.record", "true")
        var requested = false
        compose.setContent { PulseTheme {
            Row(Modifier.fillMaxWidth().padding(12.dp).height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GoalRingCard("Steps", 10911.0, 10000, Lime, Modifier.weight(1f).fillMaxHeight(), {}, {})
                GoalRingCard("Calories", null, 350, Orange, Modifier.weight(1f).fillMaxHeight(), {}, {}, 2185.0, { requested = true })
            }
        } }
        compose.onNodeWithText("10,911").assertIsDisplayed()
        compose.onNodeWithText("2185 kcal total").assertIsDisplayed()
        compose.onNodeWithText("Check access").performClick()
        compose.runOnIdle { assertTrue(requested) }
        compose.onRoot().captureRoboImage("build/screenshots/narrow-rings.png")
    }
    @Test fun dockLabelsFitAndAllDestinationsRespond() {
        System.setProperty("roborazzi.test.record", "true")
        var selected by mutableIntStateOf(0)
        compose.setContent { PulseTheme { PulseDock(selected) { selected=it } } }
        listOf("Today","Trends","Fitness Age","Connect","Workouts").forEachIndexed { i,label ->
            compose.onNodeWithText(label).assertIsDisplayed().performClick()
            compose.runOnIdle { assertEquals(i,selected) }
        }
        compose.onRoot().captureRoboImage("build/screenshots/dock-320dp.png")
    }
    @Test fun entranceAdvancesThenSettles() {
        compose.mainClock.autoAdvance = false
        var progress = -1f
        compose.setContent { progress = chartEntrance() }
        compose.mainClock.advanceTimeBy(32)
        assertTrue(progress < .1f)
        compose.mainClock.advanceTimeBy(500)
        assertTrue(progress > .1f && progress < 1f)
        compose.mainClock.advanceTimeBy(1000)
        assertEquals(1f, progress, .001f)
    }
}
