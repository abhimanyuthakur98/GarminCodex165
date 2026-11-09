package com.garmincodex.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.*

private val Ink = Color(0xFF090D0C)
private val Card = Color(0xFF171C1B)
private val Muted = Color(0xFF9BA4AB)
private val Blue = Color(0xFFC6F65B)
private val Green = Color(0xFF62E9B5)
private val Purple = Color(0xFFA9B6FF)
private val Line = Color(0xFF30383F)
private fun recoveryColor(score: Int?) = when { score == null -> Muted; score < 34 -> Color(0xFFFF6675); score < 67 -> Color(0xFFF4CA58); else -> Green }
private fun number(v: Double) = String.format(Locale.US, "%.1f", v)
private fun duration(m: Int?) = m?.let { "${it / 60}h ${it % 60}m" } ?: "—"
private fun clock(t: Long?) = t?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm")) } ?: "—"
@Composable fun PulseTheme(content: @Composable () -> Unit) { MaterialTheme(colorScheme = darkColorScheme(primary = Blue, onPrimary = Ink, secondary = Blue, secondaryContainer = Color(0xFF2E3B20), onSecondaryContainer = Blue, background = Ink, surface = Card, onSurface = Color.White), content = content) }
@Composable private fun Label(text: String, color: Color = Muted) { Text(text, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp, color = color) }
@Composable private fun Panel(content: @Composable ColumnScope.() -> Unit) { Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF212825), Card)), RoundedCornerShape(24.dp)).border(1.dp, Color.White.copy(alpha = .06f), RoundedCornerShape(24.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content) }

@Composable fun PulseDashboard(days: List<ConnectedDay>, sync: SyncState, live: DashboardState, connection: String, message: String?, sleepNeed: Int, maximumHr: Int, restingHr: Int,
    onConnect: () -> Unit, onSync: () -> Unit, onHealthSettings: () -> Unit, onGarmin: () -> Unit, onBackground: () -> Unit,
    onProfile: (Int, Int, Int) -> Unit, onClear: () -> Unit, onLive: () -> Unit, onStop: () -> Unit, goals: PersonalGoals = PersonalGoals(), onGoals: (PersonalGoals) -> Unit = {}, preferences: DashboardPreferences = DashboardPreferences(), onPreferences: (DashboardPreferences) -> Unit = {}) {
    var detail by rememberSaveable { mutableStateOf<String?>(null) }
    var goalEditor by rememberSaveable { mutableStateOf<String?>(null) }
    var page by rememberSaveable { mutableIntStateOf(preferences.startPage.coerceIn(0, 3)) }; var metric by rememberSaveable { mutableIntStateOf(0) }
    var selected by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var preview by rememberSaveable { mutableStateOf(false) }
    val day = if (preview) previewDay(selected) else days.firstOrNull { it.date == selected }
    var profile by remember { mutableStateOf(false) }; var clear by remember { mutableStateOf(false) }
    var needState by remember(sleepNeed) { mutableIntStateOf(sleepNeed) }; var maxState by remember(maximumHr) { mutableIntStateOf(maximumHr) }; var restState by remember(restingHr) { mutableIntStateOf(restingHr) }
    Scaffold(containerColor = Ink, bottomBar = {
        NavigationBar(containerColor = Card, tonalElevation = 0.dp) {
            listOf("Today" to "◉", "Trends" to "▥", "Fitness Age" to "✧", "Connect" to "⇄").forEachIndexed { i, (title, icon) -> NavigationBarItem(selected = page == i, onClick = { page = i }, icon = { Text(icon, fontSize = 22.sp) }, label = { Text(title) }, colors = NavigationBarItemDefaults.colors(indicatorColor = Blue.copy(alpha = .12f), selectedIconColor = Blue, selectedTextColor = Blue)) }
        }
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(remember(page) { ScrollState(0) }).padding(horizontal = 18.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("PULSE / 165", fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                TextButton(onClick = onSync, enabled = !sync.busy) { Text(if (sync.busy) "SYNCING…" else "↻ SYNC", color = Blue, fontSize = 11.sp) }
            }
            if (preview) Row(Modifier.fillMaxWidth().background(Color(0xFF483F21), RoundedCornerShape(8.dp)).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("PREVIEW • synthetic data", fontSize = 11.sp); Text("Exit", Modifier.clickable { preview = false }, color = Blue) }
            if (message != null) Text(message, color = Color(0xFFF4CA58), fontSize = 12.sp)
            when (page) {
                0 -> {
                    DateSelector(selected) { selected = it }
                    WeekStrip(selected) { selected = it }
                    if (!preview && (sync.busy || sync.lastSync == 0L || day?.hrCount == 0)) Text(sync.message, color = Muted, fontSize = 12.sp)
                    if (!preview && sync.lastSync == 0L) SetupCard(onConnect, onGarmin, onHealthSettings)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        RingMetric("SLEEP", day?.asleep?.let { "${(100.0 * it / needState).roundToInt().coerceAtMost(100)}%" } ?: "—", day?.asleep?.div(needState.toFloat()) ?: 0f, Purple, Modifier.weight(1f)) { detail = "sleep" }
                        RingMetric("RECOVERY", day?.recovery?.let { "$it%" } ?: "—", (day?.recovery ?: 0) / 100f, recoveryColor(day?.recovery), Modifier.weight(1f)) { detail = "recovery" }
                        RingMetric("STRAIN", day?.takeIf { it.coveredMinutes > 0 }?.let { number(Scores.strain(it.load)) } ?: "—", (Scores.strain(day?.load ?: 0.0) / 21).toFloat(), Orange, Modifier.weight(1f)) { detail = "strain" }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Label(if (preview) "DEMO DATA" else "GARMIN CONNECT"); Text(if (sync.busy) "Importing…" else if (sync.lastSync > 0) "Imported ${clock(sync.lastSync)}" else "Not connected", color = Muted, fontSize = 11.sp) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Overview", "Sleep", "Strain").forEachIndexed { i, title -> FilterChip(selected = metric == i, onClick = { metric = i }, label = { Text(title) }) } }
                    when (metric) {
                        0 -> {
                            preferences.normalized().order.filter { it !in preferences.hidden }.forEach { card -> key(card) {
                                when(card) {
                                    "briefing" -> MorningBriefing(day, if (preview) listOf(day!!) else days, selected, needState)
                                    "recovery" -> Box(Modifier.clickable(onClickLabel = "Recovery details") { detail = "recovery" }) { RecoveryPanel(day) }
                                    "activity" -> Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        GoalRingCard("Steps", day?.steps?.toDouble(), goals.steps, Lime, Modifier.weight(1f).fillMaxHeight(), { detail = "steps" }, { goalEditor = "steps" })
                                        GoalRingCard("Calories", day?.activeCalories, goals.activeCalories, Orange, Modifier.weight(1f).fillMaxHeight(), { detail = "calories" }, { goalEditor = "calories" }, totalCalories = day?.calories, onPermissions = onConnect)
                                    }
                                    "timeline" -> TimelineCard(day) { detail = "sleep" }
                                    "sleep" -> Box(Modifier.clickable(onClickLabel = "Sleep details") { detail = "sleep" }) { SleepPanel(day, needState, true) }
                                    "heart" -> Box(Modifier.clickable(onClickLabel = "Heart rate details") { detail = "hr" }) { HeartPanel(day) }
                                    "fitness" -> Panel { Label("FITNESS AGE • PULSE LAB", Green); Text("Your habits, in perspective.", fontSize = 23.sp, fontWeight = FontWeight.Bold); TextButton(onClick = { page = 2 }) { Text("Explore Fitness Age ↗") } }
                                }
                            } }
                            TextButton(onClick = { page = 3 }) { Text("Customise this dashboard") }
                        }
                        1 -> { Box(Modifier.clickable(onClickLabel = "Sleep stage details") { detail = "sleep" }) { SleepPanel(day, needState, false) }; SleepPlanner(days, goals, needState, onGoals) }
                        2 -> { Box(Modifier.clickable(onClickLabel = "Strain details") { detail = "strain" }) { StrainPanel(day, maxState, restState) }; Box(Modifier.clickable(onClickLabel = "Heart rate details") { detail = "hr" }) { HeartPanel(day) }; LivePanel(live, connection, onLive, onStop) }
                    }
                    if (!preview && (day == null || day.hrCount == 0 && day.sleepStart == null)) Panel { Text("No Garmin measurements for this day", fontWeight = FontWeight.Bold); Text("Sync your watch in Garmin Connect first. If sharing was just enabled, Garmin may only send new data; check after your next night of sleep.", color = Muted, fontSize = 13.sp); TextButton(onClick = { page = 3 }) { Text("Check connection & permissions") } }
                    Text("${if (preview) "Synthetic preview" else "Garmin → Health Connect → Pulse"} • Independent estimates, not Garmin or WHOOP scores.", color = Muted, fontSize = 10.sp)
                }
                1 -> { BaselineCard(days); WeeklyReport(days, needState); TrendsPage(days, needState) { selected = it; page = 0 } }
                2 -> FitnessAgePage(if (preview) (1L..10L).map { previewDay(LocalDate.now().minusDays(it).toString()) } else days, goals) { goalEditor = "age" }
                3 -> {
                    Text("Your watch.\nYour daily picture.", fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
                    DashboardEditor(preferences, onPreferences)
                    DataStatusCard(day, sync, onConnect, onHealthSettings)
                    SetupCard(onConnect, onGarmin, onHealthSettings)
                    Panel {
                        Label("CONNECTION STATUS", Blue); Text(sync.message, fontSize = 14.sp)
                        Text("${sync.granted.intersect(HealthConnectSync.READ_PERMISSIONS).size}/${HealthConnectSync.READ_PERMISSIONS.size} health permissions • last import ${if (sync.lastSync > 0) clock(sync.lastSync) else "never"}", color = Muted, fontSize = 12.sp)
                        Button(onClick = onSync, enabled = !sync.busy, modifier = Modifier.fillMaxWidth()) { Text(if (sync.busy) "Importing…" else "Import last 14 days") }
                        OutlinedButton(onClick = onConnect, modifier = Modifier.fillMaxWidth()) { Text("Review read permissions") }
                        Text("Background refresh: ${if (sync.background) "enabled, about hourly" else "off; refreshes when Pulse opens"}", color = Muted, fontSize = 12.sp)
                        if (!sync.background) TextButton(onClick = onBackground) { Text("Enable background refresh") }
                        TextButton(onClick = onHealthSettings) { Text("Health Connect settings") }
                    }
                    Panel {
                        Label("WHAT YOUR GARMIN SHARES"); Text("Heart rate · Sleep stages · Steps · Active & total calories", fontWeight = FontWeight.SemiBold)
                        Text("Garmin does not list HRV, resting heart rate or Body Battery in its Health Connect export. HRV/RHR appear only if those record types arrive. Recovery uses sleep and overnight HR trends after 3 baseline nights.", color = Muted, fontSize = 13.sp)
                    }
                    Panel {
                        Label("YOUR TARGETS"); Text("Sleep ${duration(needState)} • max HR $maxState • resting HR $restState", fontSize = 13.sp)
                        TextButton(onClick = { profile = true }) { Text("Adjust sleep & heart-rate profile") }; HorizontalDivider(color = Line)
                        Text("Live heart rate is optional", fontWeight = FontWeight.Bold)
                        Text("Broadcast HR for a workout stream. Daily data comes from Garmin Connect even when broadcasting is off.", color = Muted, fontSize = 13.sp)
                        OutlinedButton(onClick = onLive) { Text("Connect live HR sensor") }
                        if (live.running) TextButton(onClick = onStop) { Text("Stop sensor stream") }
                    }
                    Panel {
                        Label("PRIVACY & TESTING")
                        Text("Read-only, on-device storage. No Garmin password, uploads or ads. Clearing imports removes local Health Connect history and disconnects access; Garmin originals stay intact.", color = Muted, fontSize = 13.sp)
                        TextButton(onClick = { clear = true }) { Text("Clear imported data & disconnect", color = Color(0xFFFF8791)) }
                        TextButton(onClick = { preview = true; page = 0 }) { Text("Open synthetic design preview") }
                    }
                }
            }
        }
    }
    detail?.let { MetricDetails(it, day, goals, needState, maxState, restState, onGoals) { detail = null } }
    goalEditor?.let { GoalEditor(it, goals, { goalEditor = null }, onGoals) }
    if (profile) ProfileDialog(needState, maxState, restState, { profile = false }) { n, m, r -> needState = n; maxState = m; restState = r; onProfile(n, m, r); profile = false }
    if (clear) AlertDialog(onDismissRequest = { clear = false }, title = { Text("Clear imported data?") }, text = { Text("Removes imported history and revokes Pulse’s Health Connect access. Garmin originals and separate BLE recordings are unchanged.") }, confirmButton = { TextButton(onClick = { onClear(); clear = false }) { Text("Clear & disconnect") } }, dismissButton = { TextButton(onClick = { clear = false }) { Text("Cancel") } })
}
@Composable private fun SetupCard(onConnect: () -> Unit, onGarmin: () -> Unit, onSettings: () -> Unit) {
    Panel {
        Label("GARMIN → HEALTH CONNECT → PULSE", Blue); Text("Bring your whole day in.", fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Text("1  In Health Connect, allow Garmin Connect to write heart rate, sleep, steps and calories.\n\n2  Open Garmin Connect and sync your Forerunner.\n\n3  Allow Pulse to read those records, then import.", color = Muted, fontSize = 13.sp, lineHeight = 20.sp)
        Button(onClick = onConnect, modifier = Modifier.fillMaxWidth()) { Text("Connect Health Connect") }
        Row { TextButton(onClick = onSettings) { Text("Sharing settings", fontSize = 12.sp) }; TextButton(onClick = onGarmin) { Text("Open Garmin", fontSize = 12.sp) } }
    }
}
@Composable private fun DateSelector(selected: String, onSelect: (String) -> Unit) {
    val date = LocalDate.parse(selected)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(onClick = { onSelect(date.minusDays(1).toString()) }) { Text("‹", fontSize = 28.sp) }
        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(if (date == LocalDate.now()) "TODAY" else date.dayOfWeek.toString(), fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 14.sp); Text(date.format(DateTimeFormatter.ofPattern("d MMMM yyyy")), color = Muted, fontSize = 11.sp) }
        TextButton(onClick = { onSelect(date.plusDays(1).toString()) }, enabled = date < LocalDate.now()) { Text("›", fontSize = 28.sp) }
    }
}
@Composable private fun RingMetric(title: String, value: String, progress: Float, color: Color, modifier: Modifier, onClick: () -> Unit) {
    val entrance = chartEntrance()
    Column(modifier.clickable(onClick = onClick).semantics { contentDescription = "$title $value" }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.padding(horizontal = 5.dp).fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize().padding(7.dp)) { drawArc(Line, -90f, 360f, false, style = Stroke(6.dp.toPx())); drawArc(color, -90f, 360f * progress.coerceIn(0f, 1f) * entrance, false, style = Stroke(6.dp.toPx(), cap = StrokeCap.Round)) }
            Text(value, color = color, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        }; Label(title, Color.White)
    }
}
@Composable internal fun RecoveryPanel(d: ConnectedDay?) {
    val color = recoveryColor(d?.recovery)
    Column(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(color.copy(alpha = .13f), Card)), RoundedCornerShape(18.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Label("RECOVERY • ESTIMATE", color)
        Text(when { d?.recovery == null -> "Learning your baseline"; d.recovery >= 67 -> "Ready for your day"; d.recovery >= 34 -> "Find your balance"; else -> "A little room to recover" }, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text(if (d?.recovery == null) "${(d?.baselineNights ?: 0).coerceAtMost(3)}/3 baseline nights. Recovery needs sleep stages and overnight HR. Sleep and activity are available independently." else "Sleep + overnight heart rate. Tap to explore your recovery estimate.", color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
        HorizontalDivider(color = Line)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { MiniStat("NIGHT HR", d?.overnightHr?.roundToInt()?.let { "$it bpm" } ?: "—"); MiniStat("SLEEP", duration(d?.asleep)); MiniStat("BASELINE", "${d?.baselineNights ?: 0} nights") }
    }
}
@Composable private fun MiniStat(label: String, value: String) { Column(verticalArrangement = Arrangement.spacedBy(5.dp)) { Text(label, color = Muted, fontSize = 9.sp); Text(value, fontWeight = FontWeight.SemiBold, fontSize = 17.sp) } }
@Composable private fun MetricTile(label: String, value: String, detail: String, color: Color, modifier: Modifier) { Column(modifier.background(Card, RoundedCornerShape(16.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Label(label); Text(value, fontSize = 27.sp, fontWeight = FontWeight.Bold, color = color); Text(detail, color = Muted, fontSize = 10.sp) } }
@Composable internal fun SleepPanel(d: ConnectedDay?, need: Int, compact: Boolean) {
    Panel {
        Label("SLEEP PERFORMANCE", Purple)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) { Text(duration(d?.asleep), fontSize = 34.sp, fontWeight = FontWeight.Bold); Text("/ ${duration(need)} goal", fontSize = 12.sp, color = Muted) }
        if (d?.sleepStart == null) Text("No sleep record for this wake-up date. Sync Garmin after waking, then refresh Pulse.", color = Muted, fontSize = 13.sp)
        else {
            Text("${clock(d.sleepStart)} — ${clock(d.sleepEnd)} • wake-up date ${d.date}", color = Muted, fontSize = 12.sp)
            if (d.asleep == null) Text("This session has no classified sleep stages. Its duration is not presented as time asleep.", color = Muted, fontSize = 12.sp)
            SleepTimeline(d)
            listOf(Triple("Deep", d.deep, Color(0xFF7566CC)), Triple("Light", d.light, Purple), Triple("REM", d.rem, Blue), Triple("Awake", d.awake, Color(0xFFE8C172))).chunked(2).forEach { pair -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { pair.forEach { (label, minutes, color) -> Text("● $label  ${duration(minutes)}", color = color, fontSize = 12.sp) } } }
            if (!compact) { HorizontalDivider(color = Line); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { MiniStat("WAKE CONSISTENCY", d.consistency?.let { "$it%" } ?: "3 nights needed"); MiniStat("UNCLASSIFIED", duration(d.unknownSleep)) }; Text("Main sleep only; naps excluded. Unknown intervals stay unknown. Consistency compares wake times across up to 7 nights.", color = Muted, fontSize = 11.sp) }
        }
    }
}
@Composable private fun SleepTimeline(d: ConnectedDay) {
    val entrance = chartEntrance()
    val spans = remember(d.stages) { d.stages.split(';').mapNotNull { val p = it.split(','); if (p.size == 3) runCatching { SleepSpan(p[0].toLong(), p[1].toLong(), p[2].toInt()) }.getOrNull() else null } }
    Canvas(Modifier.fillMaxWidth().height(78.dp).semantics { contentDescription = "Sleep timeline ${clock(d.sleepStart)} to ${clock(d.sleepEnd)}" }) {
        val start = d.sleepStart ?: return@Canvas; val end = d.sleepEnd ?: return@Canvas; val duration = (end - start).coerceAtLeast(1)
        repeat(4) { i -> drawLine(Line, Offset(0f, i * size.height / 4), Offset(size.width, i * size.height / 4), 1f) }
        clipRect(right = size.width * entrance) { spans.forEach { s ->
            val color = when (s.stage) { 5 -> Color(0xFF7566CC); 4 -> Purple; 6 -> Blue; 1, 3, 7 -> Color(0xFFE8C172); 2 -> Purple.copy(alpha = .5f); else -> Line }
            val level = when (s.stage) { 5 -> 3; 4, 2 -> 2; 6 -> 1; else -> 0 }
            drawRoundRect(color, Offset((s.start - start).toFloat() / duration * size.width, level * size.height / 4), Size(((s.end - s.start).toFloat() / duration * size.width).coerceAtLeast(1f), size.height / 4 - 3), androidx.compose.ui.geometry.CornerRadius(3f))
        } }
    }
}
@Composable internal fun HeartPanel(d: ConnectedDay?) {
    Panel {
        Label("HEART RATE • DAILY", Blue)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { MiniStat("AVERAGE", d?.hrAverage?.let { "$it bpm" } ?: "—"); MiniStat("LOW", d?.hrMin?.toString() ?: "—"); MiniStat("HIGH", d?.hrMax?.toString() ?: "—") }
        val points = remember(d?.chart) { d?.chart?.split(';')?.mapNotNull { val p = it.split(','); if (p.size == 2) runCatching { HrPoint(p[0].toLong(), p[1].toInt()) }.getOrNull() else null } ?: emptyList() }
        DailyHeartChart(points, d?.date ?: LocalDate.now().toString())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { listOf("00:00", "06:00", "12:00", "18:00", "24:00").forEach { Text(it, color = Muted, fontSize = 9.sp) } }
        Text("${d?.hrCount ?: 0} samples • ${duration(d?.coveredMinutes)} covered • gaps stay blank", color = Muted, fontSize = 11.sp)
    }
}
@Composable private fun DailyHeartChart(points: List<HrPoint>, date: String) {
    val entrance = chartEntrance()
    Canvas(Modifier.fillMaxWidth().height(120.dp)) {
        val start = LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(); val end = LocalDate.parse(date).plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        fun x(t: Long) = ((t - start).toDouble() / (end - start) * size.width).toFloat()
        fun y(hr: Int) = size.height * (1 - (hr - 30) / 190f).coerceIn(0f, 1f)
        repeat(4) { i -> drawLine(Line, Offset(0f, i * size.height / 4), Offset(size.width, i * size.height / 4), 1f) }
        clipRect(right = size.width * entrance) { points.forEach { drawCircle(Blue, 1.5.dp.toPx(), Offset(x(it.time), y(it.bpm))) }
        points.zipWithNext().forEach { (a, b) -> if (b.time - a.time <= 600000) drawLine(Blue, Offset(x(a.time), y(a.bpm)), Offset(x(b.time), y(b.bpm)), 1.5.dp.toPx()) } }
    }
}
@Composable internal fun StrainPanel(d: ConnectedDay?, maximum: Int, resting: Int) {
    val entrance = chartEntrance()
    Panel {
        Label("CARDIOVASCULAR LOAD", Blue); Text(d?.takeIf { it.coveredMinutes > 0 }?.let { number(Scores.strain(it.load)) } ?: "—", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Blue); Text("Daily strain / 21 • estimated from imported HR", color = Muted, fontSize = 12.sp)
        val zones = d?.zones?.split(',')?.mapNotNull { it.toDoubleOrNull() } ?: emptyList()
        listOf("Rest / below 30%", "Easy / 30–50%", "Light / 50–60%", "Moderate / 60–70%", "Hard / 70–80%", "Very hard / 80–90%", "Peak / 90%+").forEachIndexed { i, name -> val minutes = zones.getOrElse(i) { 0.0 }; Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(name, color = Muted, fontSize = 11.sp); Text(duration(minutes.roundToInt()), fontSize = 11.sp) }; LinearProgressIndicator(progress = { (minutes / (zones.maxOrNull() ?: 1.0).coerceAtLeast(1.0)).toFloat() * entrance }, modifier = Modifier.fillMaxWidth().height(4.dp), color = Blue, trackColor = Line) }
        Text("HR reserve: resting $resting / max $maximum bpm. Estimates between readings up to 5 minutes apart; longer gaps add no load. BLE sessions stay separate to avoid double-counting workouts.", color = Muted, fontSize = 11.sp)
    }
}
@Composable private fun LivePanel(live: DashboardState, connection: String, onStart: () -> Unit, onStop: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }; LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(1000); now = System.currentTimeMillis() } }
    val sample = live.sample?.takeIf { live.running && now - it.time < 10000 }
    Panel { Label("OPTIONAL LIVE SESSION", Blue); Text(sample?.hr?.let { "$it bpm" } ?: "No fresh signal", fontSize = 26.sp, fontWeight = FontWeight.Bold); Text(if (live.running) connection else "Broadcast HR to stream here. Daily imports do not require broadcasting.", color = Muted, fontSize = 12.sp); Row { TextButton(onClick = onStart) { Text("Connect sensor") }; if (live.running) TextButton(onClick = onStop) { Text("Stop") } } }
}
@Composable private fun TrendsPage(days: List<ConnectedDay>, need: Int, onDay: (String) -> Unit) {
    Text("Your rhythm", fontSize = 32.sp, fontWeight = FontWeight.Bold); Label("DAILY HISTORY")
    val actual = days.filter { it.hrCount > 0 || it.sleepStart != null || it.steps != null }
    if (actual.isEmpty()) Panel { Text("Your history starts with your first import."); Text("Connect Health Connect and sync Garmin. Imported days remain available offline.", color = Muted) }
    val entrance = chartEntrance()
    actual.forEach { d -> Column(Modifier.fillMaxWidth().background(Card, RoundedCornerShape(16.dp)).clickable { onDay(d.date) }.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(LocalDate.parse(d.date).format(DateTimeFormatter.ofPattern("EEE, d MMM")), fontWeight = FontWeight.Bold); Text("›", color = Muted) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { MiniStat("SLEEP", duration(d.asleep)); MiniStat("RECOVERY", d.recovery?.let { "$it%" } ?: "—"); MiniStat("STRAIN", if (d.coveredMinutes > 0) number(Scores.strain(d.load)) else "—") }
        LinearProgressIndicator(progress = { ((d.asleep ?: 0) / need.toFloat()).coerceIn(0f, 1f) * entrance }, modifier = Modifier.fillMaxWidth().height(5.dp), color = Purple, trackColor = Line)
    } }
}
@Composable fun WatchDialog(watches: List<NearbyWatch>, status: String, onClose: () -> Unit, onScan: () -> Unit, onSelect: (String) -> Unit) { AlertDialog(onDismissRequest = onClose, title = { Text("Live HR sensor") }, text = { Column { Text(status); Text("Enable Broadcast Heart Rate on the watch. Optional for daily sync.", color = Muted, fontSize = 12.sp); watches.forEach { TextButton(onClick = { onSelect(it.address) }) { Text("${it.name}\n${it.address}") } } } }, confirmButton = { TextButton(onClick = onScan) { Text("Scan again") } }, dismissButton = { TextButton(onClick = onClose) { Text("Close") } }) }
@Composable private fun ProfileDialog(need: Int, maximum: Int, resting: Int, onClose: () -> Unit, onSave: (Int, Int, Int) -> Unit) {
    var n by remember { mutableStateOf(need.toString()) }; var m by remember { mutableStateOf(maximum.toString()) }; var r by remember { mutableStateOf(resting.toString()) }
    val nn = n.toIntOrNull(); val mm = m.toIntOrNull(); val rr = r.toIntOrNull(); val valid = nn != null && mm != null && rr != null && nn in 300..720 && mm in 100..230 && rr in 30..100 && mm > rr
    AlertDialog(onDismissRequest = onClose, title = { Text("Your scoring profile") }, text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { OutlinedTextField(n, { n = it }, label = { Text("Sleep goal (300–720 min)") }, singleLine = true); OutlinedTextField(m, { m = it }, label = { Text("Maximum HR (100–230)") }, singleLine = true); OutlinedTextField(r, { r = it }, label = { Text("Resting HR (30–100)") }, singleLine = true); Text("Use your established Garmin profile. Recalculates the last 14 imported days.", color = Muted, fontSize = 12.sp) } }, confirmButton = { TextButton(enabled = valid, onClick = { onSave(nn!!, mm!!, rr!!) }) { Text("Save & recalculate") } }, dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } })
}
@Composable fun PrivacyScreen(onClose: () -> Unit) { Surface(Modifier.fillMaxSize(), color = Ink) { Column(Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) { Text("Your data stays yours.", fontSize = 30.sp, fontWeight = FontWeight.Bold); Text("Pulse reads Garmin-origin heart rate, sleep, steps, active/total/basal calories and optional resting HR/HRV from Health Connect. It never writes to Health Connect. Background access is separately granted and refreshes about hourly.\n\nSummaries stay on your phone, excluded from backup. No server uploads, Garmin passwords, ads, analytics or sale of health data.\n\nRevoke permissions in Health Connect anytime. Connect → Clear imported data & disconnect removes cached imports and revokes access. Uninstall removes all app data. Source deletions are reflected in the next successful 14-day refresh; older cached days can be removed with Clear.\n\nScores are experimental fitness estimates, not medical advice or Garmin/WHOOP scores.", color = Muted, lineHeight = 23.sp); Button(onClick = onClose) { Text("Done") } } } }
internal fun previewDay(date: String): ConnectedDay {
    val start = LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(); val ss = start - 35 * 60000; val se = start + 440 * 60000
    val stages = listOf(SleepSpan(ss, start + 80 * 60000, 4), SleepSpan(start + 80 * 60000, start + 172 * 60000, 5), SleepSpan(start + 172 * 60000, start + 270 * 60000, 4), SleepSpan(start + 270 * 60000, start + 375 * 60000, 6), SleepSpan(start + 375 * 60000, se, 4))
    return ConnectedDay(date = date, load = 140.0, coveredMinutes = 860, hrCount = 1440, hrMin = 48, hrMax = 172, hrAverage = 72,
        chart = (0..180).joinToString(";") { "${start + it * 300000},${(65 + 14 * sin(it / 9.0) + if (it in 105..125) 65 else 0).toInt()}" }, zones = "600,120,45,40,30,20,5", steps = 8642, calories = 2185.0,
        sleepStart = ss, sleepEnd = se, asleep = 475, deep = 92, light = 278, rem = 105, stages = stages.joinToString(";") { "${it.start},${it.end},${it.stage}" }, overnightHr = 54.0, recovery = 84, baselineNights = 7, consistency = 91, activeCalories = 485.0, basalCalories = 1500.0,
        hourly = (0..23).joinToString(";") { h -> val steps = if (h in 7..21) ((sin(h * 2.3) + 1.2) * 370).toLong() else 0L; HourBucket(start + h * 3600000, start + (h + 1) * 3600000, steps, steps / 18.0, 65 + steps / 18.0).encode() })
}

@Composable private fun WeekStrip(selected: String, onSelect: (String) -> Unit) {
    val date = LocalDate.parse(selected)
    val first = date.minusDays((date.dayOfWeek.value - 1).toLong())
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        (0L..6L).forEach { offset -> val d = first.plusDays(offset); val active = d == date
            Column(Modifier.weight(1f).background(if (active) Lime.copy(alpha = .1f) else Color(0xFF111614), RoundedCornerShape(14.dp)).border(1.dp, if (active) Lime else Color.Transparent, RoundedCornerShape(14.dp)).clickable(enabled = d <= LocalDate.now()) { onSelect(d.toString()) }.padding(vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(d.dayOfWeek.toString().take(1), color = if (active) Lime else Muted, fontSize = 10.sp)
                Text(d.dayOfMonth.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (d > LocalDate.now()) Muted.copy(alpha = .3f) else Color.White)

            }
        }
    }
}
