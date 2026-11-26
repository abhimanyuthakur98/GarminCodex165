package com.garmincodex.app

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
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
class ChartInspectionUiTest {
    @get:Rule val compose=createComposeRule()
    @Test fun trendTapDragAndButtonsSelectActualDates() {
        System.setProperty("roborazzi.test.record","true")
        val today=LocalDate.now();var opened=""
        val days=(0L..6L).map{ConnectedDay(today.minusDays(it).toString(),steps=(it+1)*100,activitySource=ActivitySource.GARMIN)}
        compose.setContent{PulseTheme{Surface{Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)){TrendPlot(days,TrendMetric.STEPS,7,false,{opened=it})}}}}
        val chart=compose.onNodeWithContentDescription("Steps trend chart. Use date slider below.")
        chart.performScrollTo().performTouchInput{click(Offset(width*.05f,height*.5f))}
        compose.onNodeWithText("700 steps").assertExists()
        compose.onNodeWithText("Open day ›").performScrollTo().performClick()
        compose.runOnIdle{assertEquals(today.minusDays(6).toString(),opened)}
        chart.performScrollTo().performTouchInput{swipe(Offset(width*.05f,height*.5f),Offset(width*.95f,height*.5f),500)}
        compose.onNodeWithText("100 steps").assertExists()
        compose.onNodeWithContentDescription("Previous chart day").performScrollTo().performClick()
        compose.onNodeWithText("200 steps").assertExists()
        compose.onRoot().captureRoboImage("build/screenshots/chart-inspection.png")
    }
    @Test fun sleepTimelineGapIsNotAssignedAStage() {
        System.setProperty("roborazzi.test.record","true")
        val start=LocalDate.now().minusDays(1).atTime(23,0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        compose.setContent{PulseTheme{Surface{Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)){SleepStageExplorer(ConnectedDay(LocalDate.now().toString(),stages="$start,${start+3600000},5;${start+7200000},${start+10800000},4"))}}}}
        compose.onNodeWithContentDescription("Select sleep-stage time").performSemanticsAction(SemanticsActions.SetProgress){it(.5f)}
        compose.onNodeWithText("No stage record at this time").assertIsDisplayed()
        compose.onNodeWithContentDescription("Sleep-stage timeline").performTouchInput{swipe(Offset(width*.1f,height*.5f),Offset(width*.9f,height*.5f),500)}
        compose.onNodeWithText("Light").assertExists()
        compose.onNodeWithText("Show stage list").performClick()
        compose.onNodeWithText("Hide stage list").assertExists()
        compose.onRoot().captureRoboImage("build/screenshots/sleep-inspection.png")
    }
    @Test fun collapsedMetadataStillWarnsAboutPartialImports() {
        compose.setContent{PulseTheme{Surface{MetricRecordInfo(null,SyncState(unavailable=setOf("daily steps")),"steps")}}}
        compose.onNodeWithText("Import needs attention · saved values may be older").assertIsDisplayed()
        compose.onNodeWithText("Data source & import status").performClick()
        compose.onNodeWithText("Not imported yet").assertExists()
    }
}
