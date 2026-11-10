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
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],application=Application::class,qualifiers="w360dp-h800dp-320dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WorkoutUiTest {
    @get:Rule val compose=createComposeRule()
    @Test fun sessionDetailsAndCheckInWork() {
        System.setProperty("roborazzi.test.record","true")
        val start=System.currentTimeMillis()-3600000
        val w=Workout("w","FIT","RUNNING","Morning run",start,start+1800000,(0..29).joinToString(";"){"${start+it*60000},${120+it}"},250.0,distanceMeters=5000.0,timerSeconds=1800.0,movingSeconds=1750.0,paceChart=(0..29).joinToString(";"){PaceSample(start+it*60000,2.5+it*.02).encode()},laps=RunLap(start,start+1800000,5000.0,1800.0).encode())
        var miles by mutableStateOf(false)
        var ratings by mutableStateOf(emptyList<WorkoutCheckIn>())
        compose.setContent { PulseTheme { Surface { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            WorkoutLibrary(listOf(w,w.copy(id="earlier",title="Earlier run",start=start-86400000,end=start-86400000+1500000)),ratings,55,190,{ratings=listOf(it)},{},{},{},null,false,miles,{miles=it})
        } } } }
        compose.onRoot().captureRoboImage("build/screenshots/workouts.png")
        compose.onNodeWithText("Miles").performClick()
        compose.runOnIdle { assertTrue(miles) }
        compose.onNodeWithText("Morning run").performClick()
        compose.onNode(hasText("3.11 mi") and hasAnyAncestor(isDialog())).performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Select pace sample").performScrollTo().performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress){it(10f)}
        compose.onNodeWithText("+10 min",substring=true).assertExists()
        compose.onNode(isDialog()).captureRoboImage("build/screenshots/pace-detail.png")
        compose.onNodeWithText("HEART RATE").performScrollTo()
        compose.onNode(isDialog()).captureRoboImage("build/screenshots/workout-detail.png")
        compose.onNodeWithContentDescription("Workout effort").performScrollTo().performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress){it(8f)}
        compose.onNodeWithContentDescription("Workout soreness").performScrollTo().performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress){it(3f)}
        compose.onNodeWithText("Save check-in").performScrollTo().performClick()
        compose.runOnIdle {assertEquals(WorkoutCheckIn("w",8,3),ratings.single())}
        compose.onNodeWithText("Check-in saved").assertIsDisplayed()
        compose.onNode(isDialog()).captureRoboImage("build/screenshots/workout-checkin.png")
    }
}
