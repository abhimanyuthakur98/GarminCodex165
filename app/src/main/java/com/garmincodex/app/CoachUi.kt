package com.garmincodex.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import java.time.*
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable private fun CoachCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    val palette = LocalPulseStyle.current
    val Lime = palette.primary
 JournalCard(title, if(title.contains("SLEEP")) palette.violet else if(title.contains("WEEK")) palette.secondary else Lime, content) }
@Composable internal fun MorningBriefing(day: ConnectedDay?, days: List<ConnectedDay>, date: String, need: Int) {
    val palette = LocalPulseStyle.current
    val Lime = palette.primary

    CoachCard(if (date == LocalDate.now().toString()) "YOUR MORNING BRIEFING" else "DAY BRIEFING") {
        Text("Your day, explained.", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        var expanded by remember(date) { mutableStateOf(false) }
        val briefing = DailyCoach.briefing(day, days.firstOrNull { it.date == LocalDate.parse(date).minusDays(1).toString() }, need)
        Text(if(expanded) briefing else briefing.substringBefore("\n\n"), fontSize = 15.sp, lineHeight = 22.sp)
        TextButton(onClick = { expanded = !expanded }) { Text(if(expanded) "Less detail" else "Read your briefing", color = Lime) }
        Text("Based on imported records. Missing coverage can affect comparisons.", color = Color.Gray, fontSize = 11.sp)
    }
}
@Composable internal fun SleepPlanner(days: List<ConnectedDay>, goals: PersonalGoals, need: Int, onGoals: (PersonalGoals) -> Unit) = SleepPlanner(days,goals,need,onGoals,LocalDate.now())
@Composable internal fun SleepPlanner(days: List<ConnectedDay>, goals: PersonalGoals, need: Int, onGoals: (PersonalGoals) -> Unit, date:LocalDate) {
    val palette = LocalPulseStyle.current

    var edit by remember { mutableStateOf(false) }
    var input by remember(goals.wakeMinute) { mutableStateOf("%02d:%02d".format(goals.wakeMinute / 60, goals.wakeMinute % 60)) }
    val parsed = DailyCoach.parseWake(input)
    val shortage = DailyCoach.shortfall(days, date, need)
    CoachCard("SLEEP PLANNER") {
        Text("Make room for rest.", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Bedtime ${DailyCoach.bedtime(goals.wakeMinute, need)}  →  Wake ${LocalTime.of(goals.wakeMinute / 60, goals.wakeMinute % 60)}", fontSize = 20.sp, color = palette.violet)
        Text("Allows ${need / 60}h ${need % 60}m for your sleep goal. Plan extra time to wind down and fall asleep.", fontSize = 13.sp)
        WeekSleepChart(days,date,need)
        Text("${shortage.first / 60}h ${shortage.first % 60}m below goal · ${shortage.second}/7 recorded nights", color = Color.Gray, fontSize = 12.sp)
        val consistency = days.firstOrNull { it.date == date.toString() }?.consistency
        Text("Wake consistency: ${consistency?.let { "$it%" } ?: "more nights needed"}", fontSize = 13.sp)
        if (edit) {
            OutlinedTextField(input, { input = it.take(5) }, label = { Text("Wake time (24-hour HH:mm)") }, modifier = Modifier.fillMaxWidth().testTag("wake-input"), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text), singleLine = true, isError = parsed == null)
            Row { Button(enabled = parsed != null, onClick = { parsed?.let { onGoals(goals.copy(wakeMinute = it.hour * 60 + it.minute)) }; edit = false }) { Text("Save wake time") }; TextButton(onClick = { edit = false }) { Text("Cancel") } }
        } else TextButton(onClick = { edit = true }) { Text("Change wake time") }
        ChartExplanation("Missing nights are excluded. This is a goal shortfall, not a clinical sleep-debt estimate. Planning only; no alarm is scheduled. Change your sleep goal in Settings → Sleep & heart-rate targets.")
    }
}
@Composable internal fun WeeklyReport(days: List<ConnectedDay>, need: Int, end:LocalDate=LocalDate.now(), goals:PersonalGoals=PersonalGoals()) {
    val p=LocalPulseStyle.current
    val goalsWeek=weeklyGoals(days,end,goals,need)
    CoachCard("YOUR WEEKLY REPORT") {
        Text("Your week at a glance",fontSize=22.sp,fontWeight=FontWeight.Bold)
        val fmt=DateTimeFormatter.ofPattern("d MMM")
        Text("${end.minusDays(7).format(fmt)} – ${end.minusDays(1).format(fmt)} · completed days",fontSize=12.sp,color=p.muted)
        goalsWeek.forEach { goal ->
            Column(verticalArrangement=Arrangement.spacedBy(5.dp)) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    Text(goal.label,fontSize=13.sp,fontWeight=FontWeight.SemiBold)
                    Text(goal.summary,fontSize=11.sp,color=p.muted)
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                    goal.values.forEachIndexed { index,value ->
                        val hit=value!=null && value>=goal.goal
                        val date=end.minusDays(7-index.toLong())
                        Box(Modifier.weight(1f).height(8.dp).background(if(hit)p.primary else if(value!=null)p.muted.copy(alpha=.4f) else p.muted.copy(alpha=.1f),RoundedCornerShape(4.dp)).semantics{contentDescription="${goal.label} $date: ${if(value==null) "no record" else if(hit) "goal met" else "below goal"}"})
                    }
                }
            }
        }
        Text("Bright: goal met · grey: below goal · faint: missing",fontSize=10.sp,color=p.muted)
        HorizontalDivider(color=p.muted.copy(alpha=.15f))
        Text("Daily averages",fontSize=16.sp,fontWeight=FontWeight.SemiBold)
        val metrics:List<Triple<String,String,(ConnectedDay)->Double?>> = listOf(
            Triple("Steps","steps",{it.steps?.toDouble()}), Triple("Active calories","calories",{it.activeCalories}), Triple("Sleep","sleep",{it.asleep?.toDouble()}))
        metrics.forEach { (label,kind,value) -> WeeklyComparison(label,kind,DailyCoach.weekly(days,end,value)) }
        var more by remember { mutableStateOf(false) }
        TextButton(onClick={more=!more}){Text(if(more)"Fewer metrics" else "Recovery & strain")}
        if(more) {
            WeeklyComparison("Recovery · Pulse estimate","recovery",DailyCoach.weekly(days,end){it.recovery?.toDouble()})
            WeeklyComparison("Strain · Pulse estimate","strain",DailyCoach.weekly(days,end){it.takeIf{d->d.coveredMinutes>0}?.let{d->Scores.strain(d.load)}})
        }
        Text("Missing days are excluded from averages. Goal counts use your current targets. Comparisons require 7 recorded days in each week.",fontSize=11.sp,color=p.muted)
    }
}
@Composable private fun WeeklyComparison(label:String,kind:String,result:ReportValue) {
    val p=LocalPulseStyle.current
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(3.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Text(label,fontSize=13.sp)
            Text(weeklyValue(kind,result.current)+(if(kind=="calories" && result.current!=null)" kcal" else if(kind=="recovery" && result.current!=null)"%" else ""),fontSize=18.sp,fontWeight=FontWeight.Bold,color=p.primary)
        }
        Text(weeklyChange(result,kind),fontSize=12.sp,color=p.muted)
        Text("${result.currentDays}/7 days · previous ${weeklyValue(kind,result.previous)} (${result.previousDays}/7 days)",fontSize=10.sp,color=p.muted)
    }
}
@Composable internal fun DataStatusCard(day: ConnectedDay?, sync: SyncState, onPermissions: () -> Unit, onSettings: () -> Unit, days: List<ConnectedDay> = emptyList(),onRetry:(()->Unit)?=null,onDevice:(()->Unit)?=null) {
    val palette = LocalPulseStyle.current
    val Lime = palette.primary
    val Orange = palette.warm

    var expanded by remember { mutableStateOf(false) }
    CoachCard("DATA STATUS") {
        Text(if (sync.busy) "Importing shared records…" else if (sync.lastSync == 0L) "No successful import yet" else "Last import: " + Instant.ofEpochMilli(sync.lastSync).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM, HH:mm")), fontSize = 13.sp)
        Text("Showing "+(day?.date ?: "a day without saved records"),fontSize=12.sp,color=palette.muted)
        Text(metricSource(day,"activity"),fontSize=12.sp,color=palette.muted)
        if(sync.lastError.isNotBlank()) Text("Last refresh failed. Saved records are still shown.",fontSize=13.sp,color=Orange)
        if(sync.unavailable.isNotEmpty()) Text("Partial import: "+sync.unavailable.map(::importCategoryName).distinct().joinToString()+". Saved values may be older than the import time.",fontSize=13.sp,color=Orange)
        Row {
            if(onRetry!=null) TextButton(enabled=!sync.busy,onClick=onRetry){Text(if(sync.busy)"Refreshing…" else "Retry import")}
            if(onDevice!=null) TextButton(onClick=onDevice){Text("Open device app")}
        }
        ImportCoverage(days)
        TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Hide data categories" else "Check data categories") }
        if (expanded) {
            if(sync.diagnostics.isNotBlank()) {
                Text("LAST SUCCESSFUL IMPORT",color=Lime,fontSize=11.sp)
                Text(sync.diagnostics,fontSize=12.sp,lineHeight=20.sp)
                Text("Counts describe imported Health Connect data, not permission grants. A zero means no data was read; review the permission rows below. FIT imports are separate.",fontSize=11.sp,color=Color.Gray)
            }
            val fields = listOf(Triple("Heart rate", HeartRateRecord::class, (day?.hrCount ?: 0) > 0), Triple("Sleep", SleepSessionRecord::class, day?.sleepStart != null), Triple("Steps", StepsRecord::class, day?.steps != null), Triple("Active calories", ActiveCaloriesBurnedRecord::class, day?.activeCalories != null), Triple("Total calories", TotalCaloriesBurnedRecord::class, day?.calories != null), Triple("Basal energy", BasalMetabolicRateRecord::class, day?.basalCalories != null), Triple("Resting heart rate",RestingHeartRateRecord::class,day?.restingHr!=null), Triple("HRV",HeartRateVariabilityRmssdRecord::class,day?.hrv!=null))
            fields.forEach { (label, type, present) ->
                val allowed = HealthPermission.getReadPermission(type) in sync.granted
                val kind=when(label){"Heart rate"->"hr";"Sleep"->"sleep";"Steps"->"steps";"Active calories"->"calories";"Total calories"->"energy";"Resting heart rate"->"resting";"HRV"->"hrv";else->"basal"}
                val rejected=if(kind=="basal")"basal energy" in sync.unavailable else unavailableForMetric(sync,kind)
                TextButton(onClick=if(!allowed)onPermissions else onSettings,contentPadding=PaddingValues(0.dp)) {
                    Text("$label · "+categoryImportStatus(allowed,present,rejected),fontSize=12.sp,color=if(!allowed || !present || rejected)Orange else Lime)
                }
            }
            Text("Tap a category to review its permissions or device sharing. Sync in your device app first, then retry the import here.",fontSize=12.sp)
            Text("A successful import does not guarantee fresh watch measurements. Missing records can mean your device app has not shared that category yet.", fontSize = 11.sp, color = Color.Gray)
            TextButton(onClick = onPermissions) { Text("Review Pulse permissions") }
            TextButton(onClick = onSettings) { Text("Review device sharing") }
        }
    }
}
