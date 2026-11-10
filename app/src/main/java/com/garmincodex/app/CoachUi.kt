package com.garmincodex.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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

@Composable private fun CoachCard(title: String, content: @Composable ColumnScope.() -> Unit) { JournalCard(title, if(title.contains("SLEEP")) Color(0xFFA9B6FF) else if(title.contains("WEEK")) Color(0xFF62E9B5) else Lime, content) }
@Composable internal fun MorningBriefing(day: ConnectedDay?, days: List<ConnectedDay>, date: String, need: Int) {
    CoachCard(if (date == LocalDate.now().toString()) "YOUR MORNING BRIEFING" else "DAY BRIEFING") {
        Text("Your day, explained.", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        var expanded by remember(date) { mutableStateOf(false) }
        val briefing = DailyCoach.briefing(day, days.firstOrNull { it.date == LocalDate.parse(date).minusDays(1).toString() }, need)
        Text(if(expanded) briefing else briefing.substringBefore("\n\n"), fontSize = 15.sp, lineHeight = 22.sp)
        TextButton(onClick = { expanded = !expanded }) { Text(if(expanded) "Less detail" else "Read your briefing", color = Lime) }
        Text("Based on imported records. Missing coverage can affect comparisons.", color = Color.Gray, fontSize = 11.sp)
    }
}
@Composable internal fun SleepPlanner(days: List<ConnectedDay>, goals: PersonalGoals, need: Int, onGoals: (PersonalGoals) -> Unit) {
    var edit by remember { mutableStateOf(false) }
    var input by remember(goals.wakeMinute) { mutableStateOf("%02d:%02d".format(goals.wakeMinute / 60, goals.wakeMinute % 60)) }
    val parsed = DailyCoach.parseWake(input)
    val shortage = DailyCoach.shortfall(days, LocalDate.now(), need)
    CoachCard("SLEEP PLANNER") {
        Text("Make room for rest.", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Bedtime ${DailyCoach.bedtime(goals.wakeMinute, need)}  →  Wake ${LocalTime.of(goals.wakeMinute / 60, goals.wakeMinute % 60)}", fontSize = 20.sp, color = Color(0xFFA9B6FF))
        Text("Allows ${need / 60}h ${need % 60}m for your sleep goal. Plan extra time to wind down and fall asleep.", fontSize = 13.sp)
        WeekSleepChart(days,LocalDate.now(),need)
        Text("${shortage.first / 60}h ${shortage.first % 60}m below goal · ${shortage.second}/7 recorded nights", color = Color.Gray, fontSize = 12.sp)
        val consistency = days.firstOrNull { it.date == LocalDate.now().toString() }?.consistency
        Text("Wake consistency: ${consistency?.let { "$it%" } ?: "more nights needed"}", fontSize = 13.sp)
        if (edit) {
            OutlinedTextField(input, { input = it.take(5) }, label = { Text("Wake time (24-hour HH:mm)") }, modifier = Modifier.fillMaxWidth().testTag("wake-input"), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text), singleLine = true, isError = parsed == null)
            Row { Button(enabled = parsed != null, onClick = { parsed?.let { onGoals(goals.copy(wakeMinute = it.hour * 60 + it.minute)) }; edit = false }) { Text("Save wake time") }; TextButton(onClick = { edit = false }) { Text("Cancel") } }
        } else TextButton(onClick = { edit = true }) { Text("Change wake time") }
        ChartExplanation("Missing nights are excluded. This is a goal shortfall, not a clinical sleep-debt estimate. Planning only; no alarm is scheduled. Change your sleep goal in Connect → Your targets.")
    }
}
@Composable internal fun WeeklyReport(days: List<ConnectedDay>, need: Int) {
    val end = LocalDate.now()
    CoachCard("YOUR WEEKLY REPORT") {
        Text("Seven days in perspective.", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("${end.minusDays(7)} – ${end.minusDays(1)} vs the previous 7 days. Today is excluded.", fontSize = 12.sp, color = Color.Gray)
        val metrics: List<Pair<String, (ConnectedDay) -> Double?>> = listOf("Sleep · min/night" to { it.asleep?.toDouble() }, "Steps · per day" to { it.steps?.toDouble() }, "Recovery · %" to { it.recovery?.toDouble() }, "Strain · /21" to { it.takeIf { d -> d.coveredMinutes > 0 }?.let { d -> Scores.strain(d.load) } })
        metrics.forEach { (label, value) ->
            val result = DailyCoach.weekly(days, end, value)
            Text(label, fontWeight = FontWeight.Bold)
            MetricBars(listOf(Triple("This week",result.current,result.current?.let{String.format(java.util.Locale.US,"%.1f",it)} ?: "No records"),Triple("Previous",result.previous,result.previous?.let{String.format(java.util.Locale.US,"%.1f",it)} ?: "No records")))
            Text("${result.currentDays}/7 days · previous ${result.previousDays}/7 days" + (if (result.currentDays == 7 && result.previousDays == 7) " · ${String.format(java.util.Locale.US, "%+.1f", result.current!! - result.previous!!)} change" else " · comparison waits for complete weeks"), color = Color.Gray, fontSize = 11.sp)
            HorizontalDivider(color = Color.White.copy(alpha = .08f))
        }
        val nights = days.distinctBy { it.date }.filter { it.date >= end.minusDays(7).toString() && it.date < end.toString() }.mapNotNull { it.asleep }
        Text("Sleep goal met on ${nights.count { it >= need }}/${nights.size} recorded nights.", color = Lime, fontSize = 13.sp)
    }
}
@Composable internal fun DataStatusCard(day: ConnectedDay?, sync: SyncState, onPermissions: () -> Unit, onSettings: () -> Unit, days: List<ConnectedDay> = emptyList()) {
    var expanded by remember { mutableStateOf(false) }
    CoachCard("DATA STATUS") {
        Text(if (sync.busy) "Importing Garmin records…" else if (sync.lastSync == 0L) "No successful import yet" else "Last import: " + Instant.ofEpochMilli(sync.lastSync).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM, HH:mm")), fontSize = 13.sp)
        ImportCoverage(days)
        TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Hide data categories" else "Check data categories") }
        if (expanded) {
            if(sync.diagnostics.isNotBlank()) {
                Text("LAST SUCCESSFUL IMPORT",color=Lime,fontSize=11.sp)
                Text(sync.diagnostics,fontSize=12.sp,lineHeight=20.sp)
                Text("Counts describe imported Garmin data, not permission grants. A zero means no data was read; review the permission rows below. FIT imports are separate.",fontSize=11.sp,color=Color.Gray)
            }
            val fields = listOf(Triple("Heart rate", HeartRateRecord::class, (day?.hrCount ?: 0) > 0), Triple("Sleep", SleepSessionRecord::class, day?.sleepStart != null), Triple("Steps", StepsRecord::class, day?.steps != null), Triple("Active calories", ActiveCaloriesBurnedRecord::class, day?.activeCalories != null), Triple("Total calories", TotalCaloriesBurnedRecord::class, day?.calories != null), Triple("Basal energy", BasalMetabolicRateRecord::class, day?.basalCalories != null))
            fields.forEach { (label, type, present) ->
                val allowed = HealthPermission.getReadPermission(type) in sync.granted
                Text("$label · ${if (!allowed) "Permission needed" else if (present) "Imported" else "No Garmin record for this day"}", fontSize = 12.sp, color = if (!allowed || !present) Orange else Lime)
            }
            Text("A successful import does not guarantee fresh watch measurements. Missing records can mean Garmin has not shared that category yet.", fontSize = 11.sp, color = Color.Gray)
            TextButton(onClick = onPermissions) { Text("Review Pulse permissions") }
            TextButton(onClick = onSettings) { Text("Review Garmin sharing") }
        }
    }
}
