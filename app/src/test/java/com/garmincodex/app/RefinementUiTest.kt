package com.garmincodex.app
import android.app.Application
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],application=Application::class,qualifiers="w360dp-h800dp-320dpi")
class RefinementUiTest {
 @get:Rule val compose=createComposeRule()
 private lateinit var back:OnBackPressedDispatcher
 private fun app(){val days=(0L..28L).map{previewDay(LocalDate.now().minusDays(it).toString())};compose.setContent{back=LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher;PulseTheme{PulseDashboard(days,SyncState(lastSync=1),DashboardState(),"",null,480,190,55,{},{},{},{},{},{_,_,_->},{},{},{},goals=PersonalGoals(age=30))}}}
 @Test fun tabsRememberSelectionsAndBackReturnsToSnapshot(){
  app()
  compose.onNodeWithText("Trends").performClick()
  compose.onNodeWithText("Sleep ▾").performClick();compose.onNodeWithText("Steps").performClick()
  compose.onNodeWithText("7 days ▾").performClick();compose.onNodeWithText("30 days").performClick()
  compose.onNodeWithText("Fitness Age").performClick();compose.onNodeWithText("Progress").performClick()
  compose.onNodeWithText("Trends").performClick();compose.onNodeWithText("Steps ▾").assertIsDisplayed();compose.onNodeWithText("30 days ▾").assertIsDisplayed()
  compose.onNodeWithText("Fitness Age").performClick();compose.onNodeWithContentDescription("Select fitness age date").assertExists()
  compose.runOnIdle{back.onBackPressed()}
  compose.onNodeWithText("Your movement").assertIsDisplayed()
  compose.onNodeWithText("All details").performScrollTo().performClick()
  compose.runOnIdle{back.onBackPressed()}
  compose.onNodeWithText("All details").assertExists();compose.onNodeWithText("YOUR MORNING BRIEFING").assertDoesNotExist()
 }
 @Test fun explicitJournalOpenUsesSelectedDayEvenAfterTabRestoration(){
  app();val today=LocalDate.now()
  compose.onNodeWithText("Journal").performClick();compose.onNodeWithText("Previous day").performScrollTo().performClick()
  compose.onNodeWithText(today.minusDays(1).toString()).assertIsDisplayed()
  compose.runOnIdle{back.onBackPressed()}
  compose.onNodeWithText("Journal").performClick();compose.onNodeWithText(today.toString()).assertIsDisplayed()
 }
 @Test fun reselectTodayReturnsFromHistoryToCurrentDay(){
  app();val today=LocalDate.now()
  compose.onNode(hasClickAction() and hasText(today.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy")),substring=true)).performClick()
  compose.onNodeWithContentDescription("Previous day").performClick()
  compose.onNode(hasText("Today") and androidx.compose.ui.test.SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.Role,androidx.compose.ui.semantics.Role.Tab)).performClick()
  compose.onNodeWithText(today.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy")),substring=true).assertExists()
 }
}
