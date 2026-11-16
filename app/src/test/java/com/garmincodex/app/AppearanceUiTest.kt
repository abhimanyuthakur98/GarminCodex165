package com.garmincodex.app

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],application=Application::class,qualifiers="w360dp-h800dp-320dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppearanceUiTest {
    @get:Rule val compose=createComposeRule()
    @Test fun liveThemeSelectionAndNestedConnectionNavigation() {
        System.setProperty("roborazzi.test.record","true")
        var style by mutableStateOf(PulseStyle.NEON_CYAN)
        var observed=Color.Unspecified
        compose.setContent { PulseTheme(style) {
            observed=MaterialTheme.colorScheme.primary
            PulseDashboard(emptyList(),SyncState(),DashboardState(),"",null,480,190,55,{},{},{},{},{},{_,_,_->},{},{},{},appearance=style,onAppearance={style=it})
        } }
        compose.onNodeWithText("Connect").assertDoesNotExist()
        compose.onNodeWithText("Settings").assertDoesNotExist()
        compose.onNodeWithText("Trends").performClick()
        compose.onNodeWithContentDescription("Open Settings").performClick()
        compose.onNodeWithText("Workouts").assertDoesNotExist()
        for(choice in PulseStyle.entries) {
            compose.onNodeWithText(choice.title).performScrollTo().performClick()
            compose.runOnIdle { assertEquals(choice.primary,observed) }
            compose.onRoot().captureRoboImage("build/screenshots/theme-${choice.name}.png")
        }
        compose.onNodeWithText("Connections & imports").performScrollTo().performClick()
        compose.onNodeWithText("Connect Health Connect").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("‹ Settings").performScrollTo().performClick()
        compose.onNodeWithText("Neon Cyan").assertExists()
        compose.runOnIdle { assertEquals(PulseStyle.MIDNIGHT,style) }
        compose.onNodeWithContentDescription("Back to previous screen").performClick()
        compose.onNodeWithText("History").assertExists()
        compose.onNodeWithContentDescription("Open Settings").assertIsDisplayed()
    }
    @Test fun unknownPreferenceFallsBackWithoutCrashing() {
        assertEquals(PulseStyle.NEON_CYAN,PulseStyle.fromId("obsolete"))
        assertEquals(PulseStyle.NEON_CYAN,PulseStyle.fromId(null))
    }
}
