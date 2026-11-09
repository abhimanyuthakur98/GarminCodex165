package com.garmincodex.app

import android.app.Application
import androidx.compose.ui.test.*
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
        compose.onNodeWithText("Ready for your day").assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/screenshots/dashboard.png")
        compose.onNodeWithText("Sleep").performClick()
        compose.onNodeWithText("WAKE CONSISTENCY").assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/screenshots/sleep.png")
        compose.onNodeWithText("Strain").performClick()
        compose.onNodeWithText("CARDIOVASCULAR LOAD").assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/screenshots/strain.png")
        compose.onNodeWithText("Trends").performClick()
        compose.onNodeWithText("Your rhythm").assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/screenshots/trends.png")
        compose.onNodeWithText("Connect").performClick()
        compose.onNodeWithText("Connect Health Connect").assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/screenshots/connect.png")
    }
}
