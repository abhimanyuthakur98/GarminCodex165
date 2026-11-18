package com.garmincodex.app
import android.app.Application
import androidx.compose.foundation.layout.*
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
@Config(sdk=[35],application=Application::class,qualifiers="w360dp-h800dp-320dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReferenceDashboardTest {
    @get:Rule val compose=createComposeRule()
    @Test fun reopeningResetsHistoricalSelectionToCurrentDayEvenWithoutNewRecords() {
        val today=LocalDate.now();val yesterday=today.minusDays(1)
        var request by mutableIntStateOf(0)
        var currentDay by mutableStateOf(yesterday.toString())
        compose.setContent { PulseTheme { PulseDashboard(listOf(previewDay(yesterday.toString())),SyncState(lastSync=1),DashboardState(),"",null,480,190,55,{},{},{},{},{},{_,_,_->},{},{},{},foregroundRequest=request,foregroundDate=currentDay) } }
        compose.onNodeWithContentDescription("Choose day $yesterday").assertIsSelected()
        compose.runOnIdle {currentDay=today.toString();request++}
        compose.onNodeWithContentDescription("Choose day $today").assertIsSelected()
        compose.onNodeWithText("No records for this day").assertExists()
        if(today.dayOfWeek.value>1) {
            compose.onNodeWithContentDescription("Choose day $yesterday").performClick()
            compose.onNodeWithContentDescription("Choose day $yesterday").assertIsSelected()
            compose.runOnIdle {request++}
            compose.onNodeWithContentDescription("Choose day $today").assertIsSelected()
        }
    }
    @Test fun weekSelectorChangesDateAndRejectsFutureDays() {
        val today=LocalDate.now();var chosen=today.toString()
        compose.setContent{PulseTheme{QuickWeek(today.toString()){chosen=it}}}
        val monday=today.minusDays(today.dayOfWeek.value-1L)
        compose.onNodeWithContentDescription("Choose day $monday").performClick()
        compose.runOnIdle{assertEquals(monday.toString(),chosen)}
        if(today.dayOfWeek.value<7)compose.onNodeWithContentDescription("Choose day ${today.plusDays(1)}").assertIsNotEnabled()
    }
    @Test fun journalRhythmUsesOnlySavedDates() {
        System.setProperty("roborazzi.test.record","true")
        val today=LocalDate.now()
        val dates=setOf(today,today.minusDays(1),today.minusDays(3),today.plusDays(1))
        assertEquals(RhythmStats(2,2,3),rhythmStats(dates,today))
        assertEquals(RhythmStats(0,0,0),rhythmStats(emptySet(),today))
        val entries=(0L..65L).filter{it%3!=0L}.map{JournalEntry(today.minusDays(it).toString(),3,null,null,null,null,"")}
        compose.setContent{PulseTheme{Surface{Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){JournalRhythm(entries)}}}}
        compose.onNodeWithText("Current streak").assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/screenshots/journal-rhythm.png")
    }
}
