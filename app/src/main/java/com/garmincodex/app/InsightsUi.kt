package com.garmincodex.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.*
import kotlin.random.Random

internal val Lime = Color(0xFFC6F65B)
internal val Orange = Color(0xFFFF834D)
private val Fog = Color(0xFFA4A9A4)
private val Slate = Color(0xFF171C1B)
private val Mint = Color(0xFF62E9B5)
private fun fmt(n: Double) = String.format(Locale.US, "%.1f", n)
private fun time(t: Long) = Instant.ofEpochMilli(t).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
@Composable private fun InsightPanel(content: @Composable ColumnScope.() -> Unit) { Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF202724), Slate)), RoundedCornerShape(24.dp)).border(1.dp, Color.White.copy(alpha = .06f), RoundedCornerShape(24.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content) }
@Composable private fun Caption(text: String) { Text(text.uppercase(), color = Fog, fontSize = 10.sp, letterSpacing = 1.6.sp) }

@Composable internal fun GoalRingCard(title: String, value: Double?, goal: Int, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit, onEdit: () -> Unit) {
    val progress by animateFloatAsState(((value ?: 0.0) / goal).toFloat().coerceIn(0f, 1f), label = "goal")
    Column(modifier.testTag("goal-$title").background(Brush.verticalGradient(listOf(color.copy(alpha = .12f), Slate)), RoundedCornerShape(25.dp)).border(1.dp, color.copy(alpha = .13f), RoundedCornerShape(25.dp)).clickable(onClickLabel = "Open $title details", onClick = onClick).padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(title.uppercase(), fontSize = 10.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold); TextButton(onClick = onEdit, contentPadding = PaddingValues(0.dp), modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) { Text("Edit", fontSize = 11.sp, color = color) } }
        Box(Modifier.fillMaxWidth().aspectRatio(1.15f).semantics { contentDescription = "$title ${value?.roundToInt() ?: "unavailable"}, goal $goal" }, contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize().padding(9.dp)) {
                val side = min(size.width, size.height); val origin = Offset((size.width - side) / 2, (size.height - side) / 2)
                drawArc(color.copy(alpha = .12f), -90f, 360f, false, topLeft = origin, size = Size(side, side), style = Stroke(8.dp.toPx()))
                drawArc(color, -90f, 360f * progress, false, topLeft = origin, size = Size(side, side), style = Stroke(8.dp.toPx(), cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(value?.roundToInt()?.let { String.format(Locale.US, "%,d", it) } ?: "—", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = color); Text(if (value == null) "NO DATA" else "${(value / goal * 100).roundToInt()}%", fontSize = 10.sp, color = Fog) }
        }
        Text("of ${String.format(Locale.US, "%,d", goal)} ${if (title == "Steps") "steps" else "active kcal"}", color = Fog, fontSize = 10.sp)
        Text("Hourly details  ↗", color = color, fontSize = 11.sp)
    }
}
@Composable internal fun GoalEditor(kind: String, goals: PersonalGoals, onClose: () -> Unit, onSave: (PersonalGoals) -> Unit) {
    var text by remember { mutableStateOf(when (kind) { "steps" -> goals.steps.toString(); "calories" -> goals.activeCalories.toString(); else -> goals.age?.toString() ?: "" }) }
    val value = text.toIntOrNull(); val range = when (kind) { "steps" -> 1000..100000; "calories" -> 100..5000; else -> 18..100 }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth().padding(24.dp), color = Slate, shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(22.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(when (kind) { "steps" -> "Your daily step goal"; "calories" -> "Your active-calorie goal"; else -> "Your chronological age" }, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(if (kind == "age") "Used for the experimental Fitness Age comparison. Stored on your phone." else "Your ring updates immediately. A personal target, not a medical recommendation.", color = Fog, fontSize = 13.sp)
                OutlinedTextField(text, { text = it.filter(Char::isDigit).take(6) }, label = { Text(if (kind == "age") "Age in years" else if (kind == "steps") "Steps / day" else "Active kcal / day") }, modifier = Modifier.fillMaxWidth().testTag("goal-input"), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, isError = value !in range)
                Text("Allowed: ${range.first}–${range.last}", color = Fog, fontSize = 11.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onClose) { Text("Cancel") }
                    Button(enabled = value in range, onClick = { onSave(when (kind) { "steps" -> goals.copy(steps = value!!); "calories" -> goals.copy(activeCalories = value!!); else -> goals.copy(age = value!!) }); onClose() }) { Text(if (kind == "age") "Save age" else "Save goal") }
                }
            }
        }
    }
}
@Composable internal fun MetricDetails(kind: String, d: ConnectedDay?, goals: PersonalGoals, need: Int, maximum: Int, resting: Int, onGoals: (PersonalGoals) -> Unit, onClose: () -> Unit) {
    var edit by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Color(0xFF090D0C)) {
            Column(Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { TextButton(onClick = onClose) { Text("‹ Back") }; Caption(d?.date ?: "No imported day"); if (kind == "steps" || kind == "calories") TextButton(onClick = { edit = true }) { Text("Edit goal") } }
                if (edit) InlineGoalForm(kind, goals, onGoals) { edit = false }
                Text(when (kind) { "hr" -> "Every heartbeat."; "steps" -> "Keep moving."; "calories" -> "Energy, unpacked."; "sleep" -> "Inside your night."; "recovery" -> "Your recovery."; else -> "Your effort." }, fontSize = 34.sp, fontWeight = FontWeight.Bold, lineHeight = 38.sp)
                when (kind) {
                    "steps", "calories" -> {
                        val steps = kind == "steps"; val color = if (steps) Lime else Orange
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) { Text((if (steps) d?.steps?.toDouble() else d?.activeCalories)?.roundToInt()?.let { String.format(Locale.US, "%,d", it) } ?: "—", fontSize = 58.sp, fontWeight = FontWeight.Bold, color = color); Text(if (steps) "steps" else "active kcal", color = Fog, modifier = Modifier.padding(bottom = 12.dp)) }
                        Text("Daily goal ${if (steps) goals.steps else goals.activeCalories} · tap Edit goal to change", color = Fog, fontSize = 12.sp)
                        if (!steps) CalorieBreakdown(d)
                        InsightPanel {
                            Caption("Hourly ${if (steps) "steps" else "active energy"}")
                            HourlyChart(HourBucket.decode(d?.hourly ?: ""), steps, color)
                            Text("Tap a bar to inspect its hour. Empty intervals mean no record, not zero. Health Connect may distribute a source interval across hourly buckets.", color = Fog, fontSize = 11.sp)
                        }
                    }
                    "hr" -> { InsightPanel { Caption("Heart rate • tap or scrub"); InteractiveHeart(d) }; HeartPanel(d); Text("Chart values are five-minute averages; daily low/high use original imported samples. Long gaps are not interpolated.", color = Fog, fontSize = 12.sp) }
                    "sleep" -> { SleepPanel(d, need, false); InsightPanel { Caption("Stage-by-stage"); val spans = (d?.stages ?: "").split(';').mapNotNull { val p = it.split(','); if (p.size == 3) runCatching { SleepSpan(p[0].toLong(), p[1].toLong(), p[2].toInt()) }.getOrNull() else null }; if (spans.isEmpty()) Text("No sleep-stage records yet.", color = Fog); spans.forEach { s -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("${time(s.start)}–${time(s.end)}", color = Fog, fontSize = 12.sp); Text(when (s.stage) { 5 -> "Deep"; 4 -> "Light"; 6 -> "REM"; 1, 3, 7 -> "Awake"; 2 -> "Asleep"; else -> "Unknown" }, fontSize = 12.sp) } } } }
                    "recovery" -> { RecoveryPanel(d); InsightPanel { Caption("How this score is built"); Text("Resting HR: ${d?.restingHr?.roundToInt()?.toString() ?: "Not supplied"} · HRV: ${d?.hrv?.roundToInt()?.toString() ?: "Not supplied"}", color = Fog, fontSize = 12.sp); Text("40% sleep goal completion\n20% sleep efficiency\n40% overnight HR versus your baseline", lineHeight = 27.sp); Text("Requires 3 previous baseline nights. Missing stages or insufficient HR keep the score unavailable. HRV and Body Battery are not fabricated.", color = Fog, fontSize = 12.sp) } }
                    else -> { StrainPanel(d, maximum, resting); InsightPanel { Caption("HR reserve contribution"); Text("Each minute has a zone weight: 0, 1, 2, 3, 5, 8 or 12. Accumulated weighted minutes map onto a bounded 0–21 score.", color = Fog); Text("${d?.coveredMinutes ?: 0} minutes of measured coverage", color = Lime) } }
                }
                Text("Garmin Connect / Health Connect · ${d?.date ?: "No data"}", color = Fog, fontSize = 10.sp)
            }
        }

    }
}
@Composable private fun CalorieBreakdown(d: ConnectedDay?) {
    InsightPanel {
        Caption("Calorie breakdown")
        val nonActive = EnergyBreakdown.nonActive(d?.calories, d?.activeCalories)
        listOf("Active" to d?.activeCalories, "Resting / other · total minus active" to nonActive, "Total burned" to d?.calories, "Basal energy · reported BMR" to d?.basalCalories).forEach { (label, value) -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = Fog, fontSize = 11.sp, modifier = Modifier.weight(1f)); Text(value?.roundToInt()?.let { "$it kcal" } ?: "—", fontWeight = FontWeight.SemiBold) } }
        Text("Resting/other is derived only when total and active are both present and consistent. It is not a BMR measurement. Basal energy is shown separately only if Garmin supplies BMR records, and is for the imported portion of the day.", color = Fog, fontSize = 11.sp)
        if (d?.activeCalories == null) Text("Active energy unavailable. Review the new active-calorie read permission in Connect, then sync. Garmin must also share this category.", color = Orange, fontSize = 12.sp)
    }
}
@Composable private fun HourlyChart(hours: List<HourBucket>, steps: Boolean, color: Color) {
    var selected by remember(hours, steps) { mutableIntStateOf(-1) }
    fun value(h: HourBucket) = if (steps) h.steps?.toDouble() else h.active
    val data = hours.filter { value(it) != null }
    val peak = data.maxByOrNull { value(it) ?: 0.0 }
    val chosen = hours.getOrNull(selected)
    Text(if (chosen == null) peak?.let { "Most active: ${time(it.start)} · ${value(it)!!.roundToInt()} ${if (steps) "steps" else "kcal"}" } ?: "No hourly records" else "${Instant.ofEpochMilli(chosen.start).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm XXX"))} · ${value(chosen)?.roundToInt()?.toString() ?: "No record"} ${if (steps) "steps" else "kcal"}", color = color, fontSize = 14.sp)
    Canvas(Modifier.fillMaxWidth().height(180.dp).semantics { contentDescription = "Hourly activity chart. Use the hour selector below for accessible values." }.pointerInput(hours, steps) { detectTapGestures { p -> if (hours.isNotEmpty()) selected = (p.x / size.width * hours.size).toInt().coerceIn(hours.indices) } }) {
        if (hours.isEmpty()) return@Canvas
        val max = (data.maxOfOrNull { value(it) ?: 0.0 } ?: 1.0).coerceAtLeast(1.0); val width = size.width / hours.size
        repeat(4) { drawLine(Color.White.copy(alpha = .05f), Offset(0f, it * size.height / 4), Offset(size.width, it * size.height / 4), 1f) }
        hours.forEachIndexed { i, h -> val v = value(h); val height = if (v == null) 3.dp.toPx() else (v / max * (size.height - 10)).toFloat().coerceAtLeast(3.dp.toPx()); drawRoundRect(if (v == null) Color.White.copy(alpha = .08f) else if (i == selected) Color.White else color, Offset(i * width + 2, size.height - height), Size((width - 4).coerceAtLeast(1f), height), androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())) }
    }
    if (hours.isNotEmpty()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(time(hours.first().start), color = Fog, fontSize = 10.sp); Text(time(hours.last().end), color = Fog, fontSize = 10.sp) }
        if (hours.size > 1) Slider(value = selected.coerceAtLeast(0).toFloat(), onValueChange = { selected = it.roundToInt() }, valueRange = 0f..hours.lastIndex.toFloat(), steps = (hours.size - 2).coerceAtLeast(0), modifier = Modifier.semantics { contentDescription = "Select activity hour" })
    }
}
@Composable private fun InteractiveHeart(d: ConnectedDay?) {
    val points = remember(d?.chart) { d?.chart?.split(';')?.mapNotNull { val p = it.split(','); if (p.size == 2) runCatching { HrPoint(p[0].toLong(), p[1].toInt()) }.getOrNull() else null }?.sortedBy { it.time } ?: emptyList() }
    var selected by remember(points) { mutableIntStateOf(0) }
    val point = points.getOrNull(selected)
    Text(point?.let { "${it.bpm} bpm  ·  ${time(it.time)}" } ?: "No heart-rate records", color = Mint, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    Canvas(Modifier.fillMaxWidth().height(220.dp).pointerInput(points) { detectTapGestures { p -> if (points.isNotEmpty()) { val target = points.first().time + (p.x / size.width) * (points.last().time - points.first().time); selected = points.indices.minBy { abs(points[it].time - target) } } } }) {
        if (points.isEmpty()) return@Canvas
        fun x(p: HrPoint) = (p.time - points.first().time).toFloat() / (points.last().time - points.first().time).coerceAtLeast(1) * size.width
        fun y(p: HrPoint) = size.height * (1 - (p.bpm - 30) / 190f).coerceIn(0f, 1f)
        repeat(4) { drawLine(Color.White.copy(alpha = .07f), Offset(0f, it * size.height / 4), Offset(size.width, it * size.height / 4), 1f) }
        points.zipWithNext().forEach { (a,b) -> if (b.time - a.time <= 600000) drawLine(Mint, Offset(x(a), y(a)), Offset(x(b), y(b)), 2.dp.toPx()) }
        point?.let { drawLine(Fog, Offset(x(it), 0f), Offset(x(it), size.height), 1.dp.toPx()); drawCircle(Color.White, 5.dp.toPx(), Offset(x(it), y(it))) }
    }
    if (points.size > 1) Slider(value = selected.toFloat(), onValueChange = { selected = it.roundToInt() }, valueRange = 0f..points.lastIndex.toFloat(), modifier = Modifier.semantics { contentDescription = "Select heart-rate sample" })
}

@Composable internal fun FitnessAgePage(days: List<ConnectedDay>, goals: PersonalGoals, onAge: () -> Unit) {
    val result = remember(days, goals.age) { FitnessAgeModel.calculate(days, goals.age) }
    Caption("Fitness Age / Pulse Lab")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Your long-term\npicture.", fontSize = 34.sp, fontWeight = FontWeight.Bold, lineHeight = 37.sp); TextButton(onClick = onAge) { Text(if (goals.age == null) "Set age" else "Age ${goals.age}", color = Mint) } }
    Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(12.dp)) {
            val radius = size.minDimension / 2; val center = center
            drawCircle(Brush.radialGradient(listOf(Color.Transparent, Mint.copy(alpha = .03f), Mint.copy(alpha = .2f)), radius = radius), radius)
            drawCircle(Mint.copy(alpha = .35f), radius, style = Stroke(1.dp.toPx()))
            val random = Random(165)
            repeat(350) { val angle = random.nextDouble() * 2 * PI; val r = radius * (.58 + random.nextDouble() * .4); val p = center + Offset((cos(angle) * r).toFloat(), (sin(angle) * r).toFloat()); drawCircle(Mint.copy(alpha = (.12 + random.nextDouble() * .65).toFloat()), (1 + random.nextDouble() * 2.5).dp.toPx(), p) }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(result.age?.let(::fmt) ?: "—", fontSize = 62.sp, fontWeight = FontWeight.Bold); Caption("Experimental fitness age"); Text(result.age?.let { val delta = it - goals.age!!; "${fmt(abs(delta))} years ${if (delta <= 0) "below" else "above"} your age" } ?: "${result.days}/7 complete days", color = Mint, fontSize = 13.sp) }
    }
    InsightPanel { Caption("A wellness illustration, not biological age"); Text(result.reason, fontSize = 14.sp); Text("This is an unvalidated Pulse heuristic based on habits and recovery. It does not measure aging, predict lifespan or reproduce WHOOP Age. Chronological age is your input; the offset is illustrative.", color = Fog, fontSize = 12.sp) }
    result.factors.forEach { factor -> InsightPanel { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(factor.name, fontWeight = FontWeight.Bold); Text("${factor.score.roundToInt()}/100", color = Mint) }; Text(factor.detail, color = Fog, fontSize = 12.sp); LinearProgressIndicator(progress = { (factor.score / 100).toFloat() }, modifier = Modifier.fillMaxWidth(), color = Mint, trackColor = Color.White.copy(alpha = .07f)) } }
    InsightPanel { Caption("Transparent model"); Text("Movement, sleep, recovery and cardio contribute equally. Index 70 maps to your entered age. Each 6 index points adjusts the displayed age by 1 year, capped at ±5 years (minimum age 18). Fixed reference targets prevent goal changes from gaming the result.", color = Fog, fontSize = 12.sp) }
}

@Composable private fun InlineGoalForm(kind: String, goals: PersonalGoals, onSave: (PersonalGoals) -> Unit, onClose: () -> Unit) {
    var value by remember { mutableStateOf(if (kind == "steps") goals.steps.toString() else goals.activeCalories.toString()) }
    val parsed = value.toIntOrNull(); val valid = parsed != null && parsed in if (kind == "steps") 1000..100000 else 100..5000
    InsightPanel {
        Caption("Edit daily goal")
        OutlinedTextField(value, { value = it.filter(Char::isDigit).take(6) }, modifier = Modifier.fillMaxWidth().testTag("goal-input"), label = { Text(if (kind == "steps") "Steps / day" else "Active kcal / day") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, isError = !valid)
        Row { Button(enabled = valid, onClick = { onSave(if (kind == "steps") goals.copy(steps = parsed!!) else goals.copy(activeCalories = parsed!!)); onClose() }) { Text("Save goal") }; TextButton(onClick = onClose) { Text("Cancel") } }
    }
}
