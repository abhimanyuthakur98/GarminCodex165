package com.garmincodex.app

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],application=Application::class)
class StepSourceUiTest {
    @get:Rule val compose=createComposeRule()
    @Test fun sourceComparisonShowsFullCountsAndSelection() {
        var chosen=""
        val pkg="com.sec.android.app.shealth"
        compose.setContent { PulseTheme { StepSourceCard(SyncState(stepComparisons=mapOf(pkg to 14000L)),{chosen=it}) } }
        compose.onNodeWithText("Samsung Health · 14,000 steps").performClick()
        compose.runOnIdle { assertEquals(pkg,chosen) }
        compose.onNodeWithText("Health Connect priority").performClick()
        compose.runOnIdle { assertEquals("",chosen) }
    }
}
