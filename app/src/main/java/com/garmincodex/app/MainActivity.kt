package com.garmincodex.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModelProvider
import org.koin.android.ext.android.get
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Blue = Color(0xFF4CD6FF)
private val Green = Color(0xFFABED61)
private val Muted = Color(0xFF97A4B6)
private val Panel = Color(0xFF171E29)
private fun recoveryColor(score: Int) = if (score < 34) Color(0xFFFF667B) else if (score < 67) Color(0xFFFFCE66) else Green
class MainActivity : ComponentActivity() {
    private lateinit var vm: DashboardViewModel
    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (requiredPermissions().all { result[it] == true || ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) vm.scan()
        else permissionMessage.value = "Bluetooth permission was denied. Allow Nearby devices in Android Settings, then scan again."
    }
    private val permissionMessage = mutableStateOf<String?>(null)
    private fun requiredPermissions() = if (Build.VERSION.SDK_INT >= 31) arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT) else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); enableEdgeToEdge()
        vm = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST") override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = get<DashboardViewModel>() as T
        })[DashboardViewModel::class.java]
        setContent {
            val state by vm.state.collectAsStateWithLifecycle()
            val history by vm.history.collectAsStateWithLifecycle()
            val devices by vm.devices.collectAsStateWithLifecycle()
            val connection by vm.connection.collectAsStateWithLifecycle()
            var choosing by remember { mutableStateOf(false) }
            MaterialTheme(colorScheme = darkColorScheme(primary = Blue, background = Color(0xFF0B1018), surface = Panel, onSurface = Color.White)) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Dashboard(state, history, connection, permissionMessage.value,
                        onDemo = { choosing = false; stopService(Intent(this, TrackingService::class.java)); vm.demo() },
                        onLive = { choosing = true; permissionMessage.value = null; stopService(Intent(this, TrackingService::class.java)); permissions.launch(requiredPermissions()) },
                        onStop = { vm.stop(); stopService(Intent(this, TrackingService::class.java)) })
                    if (choosing) AlertDialog(onDismissRequest = { choosing = false }, title = { Text("Connect your watch") }, text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(permissionMessage.value ?: connection)
                            Text("Enable Broadcast Heart Rate on your Forerunner 165, then select its sensor. Scan runs for 15 seconds.", color = Muted)
                            devices.forEach { watch -> TextButton(onClick = {
                                vm.select(watch.address); choosing = false
                                try { ContextCompat.startForegroundService(this@MainActivity, Intent(this@MainActivity, TrackingService::class.java)) }
                                catch (e: Exception) { permissionMessage.value = "Cannot start tracking: ${e.message}" }
                            }) { Text("${watch.name}\n${watch.address}") } }
                        }
                    }, confirmButton = { TextButton(onClick = { permissions.launch(requiredPermissions()) }) { Text("Scan again") } }, dismissButton = { TextButton(onClick = { choosing = false }) { Text("Close") } })
                }
            }
        }
    }
}
@Composable
fun Dashboard(s: DashboardState, history: List<DailyRecord>, connection: String, error: String?, onDemo: () -> Unit, onLive: () -> Unit, onStop: () -> Unit) {
    val demo = s.mode == "DEMO"
    val recovery = if (demo) Scores.recovery(demoNight) else null
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(1000); now = System.currentTimeMillis() } }
    val fresh = s.sample?.takeIf { s.running && now - it.time < 10000 }
    Column(Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column { Text("PULSE / 165", fontWeight = FontWeight.Black, letterSpacing = 3.sp, fontSize = 20.sp); Text(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d")).uppercase(), color = Muted, fontSize = 11.sp, letterSpacing = 2.sp) }
            Text(if (demo) "DEMO" else "LIVE", color = if (demo) Color(0xFFFFCE66) else Green, fontWeight = FontWeight.Bold)
        }
        Text("Make today\ncount.", fontSize = 38.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = demo, onClick = onDemo, label = { Text("Simulation") })
            FilterChip(selected = !demo, onClick = onLive, label = { Text("Connect watch") })
            if (s.running) TextButton(onClick = onStop) { Text("Stop") }
        }
        Text(error ?: if (s.running && !demo) connection else s.message, color = Muted, fontSize = 12.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ScoreCard("RECOVERY", recovery?.let { "$it%" } ?: "—", recovery?.div(100f) ?: 0f, recovery?.let(::recoveryColor) ?: Muted, recovery?.let(Scores::tier) ?: "NEEDS SYNC", Modifier.weight(1f))
            ScoreCard("DAY STRAIN", String.format(Locale.US, "%.1f", Scores.strain(s.load)), (Scores.strain(s.load) / 21).toFloat(), Blue, "OF 21.0", Modifier.weight(1f))
        }
        CardBlock {
            Text(if (demo) "YOUR DAILY OUTLOOK" else "CAPTURED STRAIN", color = Green, fontSize = 11.sp, letterSpacing = 2.sp)
            Text(if (demo) "Rested. Recharged. Ready." else "Every heartbeat adds up.", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(if (demo) "Demo recovery is ${recovery}%. Start the simulation to see your cardiovascular load build." else "Strain covers recorded time only. Recovery and sleep require a Garmin sync integration.", color = Muted, fontSize = 13.sp)
        }
        CardBlock {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("HEART RATE", letterSpacing = 2.sp, fontSize = 12.sp); Text(if (fresh != null) "● STREAMING" else "NO FRESH DATA", color = if (fresh != null) Green else Muted, fontSize = 10.sp) }
            Row(verticalAlignment = Alignment.Bottom) { Text(fresh?.hr?.toString() ?: "—", fontSize = 42.sp, fontWeight = FontWeight.Bold); Text(" bpm", color = Muted, modifier = Modifier.padding(bottom = 8.dp)) }
            HeartChart(s.chart)
            Text("Last 120 samples  •  shaded zone: 60–80% HR reserve", color = Muted, fontSize = 10.sp)
            Text("Reference profile: resting 55 / maximum 190 bpm", color = Muted, fontSize = 10.sp)
            if (demo && fresh != null) Text("${fresh.steps} steps   •   RR ${fresh.rrMs.firstOrNull()?.toInt()} ms   •   ${String.format(Locale.US, "%.2f", fresh.acceleration)} m/s²", color = Muted, fontSize = 11.sp)
        }
        SleepCard(if (demo) demoNight else null)
        CardBlock {
            Text("RECENT DAYS", letterSpacing = 2.sp, fontSize = 12.sp)
            if (history.isEmpty()) Text("Recorded days will appear here. Data saves every 10 samples and when tracking stops.", color = Muted)
            history.take(14).forEach { day ->
                HorizontalDivider(color = Color(0xFF293341))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column { Text(day.date, fontSize = 13.sp); Text(day.source, color = if (day.source == "DEMO") Color(0xFFFFCE66) else Blue, fontSize = 10.sp) }
                    Text("${String.format(Locale.US, "%.1f", Scores.strain(day.load))} strain  /  ${day.recovery?.let { "$it%" } ?: "—"}", fontSize = 12.sp)
                }
            }
        }
        Text("Independent experimental scores • not WHOOP or Garmin scores. Simulation data is synthetic. Not for medical decisions.", color = Muted, fontSize = 11.sp)
    }
}
@Composable
fun CardBlock(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(22.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}
@Composable
fun ScoreCard(title: String, value: String, progress: Float, color: Color, caption: String, modifier: Modifier = Modifier) {
    Column(modifier.background(Panel, RoundedCornerShape(22.dp)).padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, fontSize = 10.sp, letterSpacing = 1.sp, color = Muted)
        Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize().padding(7.dp)) {
                drawArc(Color(0xFF303947), 140f, 260f, false, style = Stroke(9.dp.toPx(), cap = StrokeCap.Round))
                drawArc(color, 140f, 260f * progress.coerceIn(0f, 1f), false, style = Stroke(9.dp.toPx(), cap = StrokeCap.Round))
            }
            Text(value, color = color, fontSize = 29.sp, fontWeight = FontWeight.Bold)
        }
        Text(caption, color = color, fontSize = 10.sp, letterSpacing = 1.sp)
    }
}
@Composable
fun HeartChart(values: List<Int>) {
    Canvas(Modifier.fillMaxWidth().height(110.dp)) {
        fun y(hr: Int) = size.height * (1 - (hr - 40) / 160f).coerceIn(0f, 1f)
        drawRect(Blue.copy(alpha = .07f), Offset(0f, y(163)), androidx.compose.ui.geometry.Size(size.width, y(136) - y(163)))
        listOf(60, 100, 140, 180).forEach { drawLine(Color(0xFF303947), Offset(0f, y(it)), Offset(size.width, y(it)), 1f) }
        if (values.size > 1) {
            val path = Path()
            values.forEachIndexed { i, hr -> val x = i * size.width / (values.size - 1); if (i == 0) path.moveTo(x, y(hr)) else path.lineTo(x, y(hr)) }
            drawPath(path, Blue, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}
@Composable
fun SleepCard(n: Night?) {
    CardBlock {
        Text("SLEEP PERFORMANCE", color = Muted, fontSize = 12.sp, letterSpacing = 2.sp)
        if (n == null) { Text("Your night, decoded.", fontSize = 24.sp, fontWeight = FontWeight.Bold); Text("Sleep stages are unavailable over standard heart-rate BLE. Connect a licensed Garmin sync adapter to import them.", color = Muted) }
        else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("${n.asleep / 60}h ${n.asleep % 60}m", fontSize = 30.sp, fontWeight = FontWeight.Bold); Text("${n.performance}%", color = Color(0xFFBBABFF), fontSize = 30.sp) }
            Text("of ${n.need / 60}h sleep need  •  ${n.consistency}% consistency", color = Muted, fontSize = 12.sp)
            val stages = listOf(Triple("Deep", n.deep, Color(0xFF7869E8)), Triple("Light", n.light, Color(0xFFB3A4FF)), Triple("REM", n.rem, Blue), Triple("Awake", n.awake, Color(0xFFFFCE66)))
            Row(Modifier.fillMaxWidth().height(14.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) { stages.forEach { (_, minutes, color) -> Box(Modifier.weight(minutes.toFloat()).fillMaxHeight().background(color, RoundedCornerShape(4.dp))) } }
            stages.forEach { (label, minutes, color) -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("● $label", color = color, fontSize = 12.sp); Text("${minutes / 60}h ${minutes % 60}m", fontSize = 12.sp) } }
            Text("HRV ${n.hrv.toInt()} ms  •  RHR ${n.rhr} bpm  •  Body Battery ${n.bodyBattery}", color = Muted, fontSize = 11.sp)
        }
    }
}
