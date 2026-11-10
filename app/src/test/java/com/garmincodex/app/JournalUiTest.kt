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
class JournalUiTest {
    @get:Rule val compose=createComposeRule()
    @Test fun savesHabitsShowsCalendarAndConfiguresReminder() {
        System.setProperty("roborazzi.test.record","true")
        var entries by mutableStateOf(emptyList<JournalEntry>())
        var tags by mutableStateOf(emptyList<HabitTag>())
        var reminder by mutableStateOf(JournalReminder())
        compose.setContent {PulseTheme {Surface {Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            JournalPage(entries,tags,emptyList(),emptyList(),{entries=listOf(it)},{tags=listOf(it)},{entries=emptyList()},{entries=emptyList();tags=emptyList()},reminder,{reminder=it})
        }}}}
        compose.onNodeWithTag("Energy-4").performScrollTo().performClick()
        compose.onRoot().captureRoboImage("build/screenshots/journal.png")
        compose.onNodeWithTag("habit-name").performScrollTo().performTextInput("Evening walk")
        compose.onNodeWithText("Add habit").performScrollTo().performClick()
        compose.onNodeWithTag("Evening walk-true").performScrollTo().performClick()
        compose.onNodeWithText("Save daily check-in").performScrollTo().performClick()
        compose.runOnIdle {assertEquals(4,entries.single().energy);assertTrue(JournalAnalysis.answers(entries.single().habits)[tags.single().id]!!)}
        compose.onNodeWithTag("reminder-time").performScrollTo().performTextReplacement("21:30")
        compose.onNodeWithText("Enable reminder").performScrollTo().performClick()
        compose.runOnIdle {assertEquals(JournalReminder(true,1290),reminder)}
        compose.onNodeWithText("Calendar").performScrollTo().performClick()
        compose.onRoot().captureRoboImage("build/screenshots/journal-calendar.png")
        compose.onNodeWithText("Patterns").performScrollTo().performClick()
        compose.onNodeWithText("PATTERNS, NOT PROOF").assertIsDisplayed()
    }
}
