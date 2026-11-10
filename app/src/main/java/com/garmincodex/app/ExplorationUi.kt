package com.garmincodex.app

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.*
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val Soft = Color(0xFFA4A9A4)
private val Lavender = Color(0xFFA9B6FF)
@Composable private fun ExploreCard(title: String, content: @Composable ColumnScope.() -> Unit) { JournalCard(title, if(title.contains("TIMELINE")) Orange else Color(0xFF62E9B5), content) }
@Composable internal fun TimelineCard(day: ConnectedDay?, onSleep: () -> Unit) {
    val hours = remember(day) { day?.let { ActivityTimeline.hours(it) } ?: emptyList() }
    var selected by remember(day?.date) { mutableIntStateOf(0) }
    val reveal = chartEntrance()
    ExploreCard("YOUR DAY · TIMELINE") {
        Text("Sleep. Move. Recover.", fontSize=24.sp, fontWeight=FontWeight.Bold)
        if (hours.isEmpty()) Text("Import a day to explore its timeline.", color=Soft)
        else {
            Text("Sleep session / Steps / Heart rate", color=Soft, fontSize=12.sp)
            Canvas(Modifier.fillMaxWidth().height(150.dp).semantics { contentDescription="Three aligned hourly lanes: sleep session, steps and heart rate. Use the hour slider to inspect." }.pointerInput(hours) { detectTapGestures { selected=(it.x / size.width * hours.size).toInt().coerceIn(hours.indices) } }) {
                val width=size.width/hours.size; val lane=size.height/3
                val maxSteps=(hours.maxOfOrNull { it.steps ?: 0 } ?: 1).coerceAtLeast(1)
                hours.forEachIndexed { i,h ->
                    if (i == selected) drawRect(Color.White.copy(alpha=.08f), Offset(i*width,0f), Size(width,size.height))
                    listOf(0,1,2).forEach { row -> drawLine(Color.White.copy(alpha=.06f),Offset(i*width,(row+1)*lane),Offset((i+1)*width,(row+1)*lane)) }
                    if (i.toFloat()/hours.size <= reveal) {
                        if (h.sleepMinutes > 0) drawRect(Lavender,Offset(i*width+1,8f),Size((width-2).coerceAtLeast(1f),lane*.65f*h.sleepMinutes/60))
                        h.steps?.let { val height=(it.toFloat()/maxSteps*lane*.8f*reveal).coerceAtLeast(2f); drawRect(Lime,Offset(i*width+1,2*lane-height),Size((width-2).coerceAtLeast(1f),height)) }
                        h.hr?.let { drawCircle(Orange,3.dp.toPx(),Offset((i+.5f)*width,3*lane-((it-30)/190).toFloat().coerceIn(0f,1f)*lane*.8f-5)) }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Text("00:00",color=Soft,fontSize=10.sp);Text("12:00",color=Soft,fontSize=10.sp);Text("24:00",color=Soft,fontSize=10.sp) }
            val hour=hours[selected.coerceIn(hours.indices)]
            val stamp=Instant.ofEpochMilli(hour.start).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm XXX"))
            Text("$stamp · ${hour.steps?.let { "$it steps" } ?: "Steps unavailable"}",color=Lime)
            Text("${hour.hr?.let { "${it.roundToInt()} bpm · hourly mean of chart averages" } ?: "No HR samples"}\n${hour.sleepMinutes} min within the main sleep session",color=Soft,fontSize=12.sp)
            Slider(selected.toFloat(), { selected=it.roundToInt() },valueRange=0f..hours.lastIndex.toFloat(),steps=(hours.size-2).coerceAtLeast(0),modifier=Modifier.semantics { contentDescription="Select timeline hour" })
            Text("Midnight to midnight for ${day?.date}. Blank activity intervals mean no record. Sleep includes awake time within the session; its previous-night portion is outside this view.",color=Soft,fontSize=11.sp)
            TextButton(onClick=onSleep) { Text("Explore full sleep session") }
        }
    }
}
@Composable internal fun BaselineCard(days: List<ConnectedDay>) {
    val today=LocalDate.now()
    val metrics: List<Triple<String,String,(ConnectedDay)->Double?>> = listOf(Triple("Sleep duration","min",{it.asleep?.toDouble()}),Triple("Overnight heart rate","bpm",{it.overnightHr}))
    val reveal=chartEntrance()
    ExploreCard("YOUR PERSONAL BASELINES") {
        Text("Compared with your usual.",fontSize=24.sp,fontWeight=FontWeight.Bold)
        Text("Each day's band is the middle 50% of its previous 14 days. At least 7 measurements are required. This is a personal reference, not a healthy-range assessment.",color=Soft,fontSize=12.sp)
        metrics.forEach { (label,unit,value) ->
            var selected by remember(label) { mutableIntStateOf(6) }
            Text(label,fontWeight=FontWeight.Bold)
            val dates=(6L downTo 0L).map { today.minusDays(it) }
            val values=dates.map { date -> days.firstOrNull { it.date==date.toString() }?.let(value) }
            val bands=dates.map { PersonalBaseline.calculate(days,it,value) }
            val range=values.filterNotNull()+bands.filterNotNull().flatMap { listOf(it.lower,it.upper) }
            val low=(range.minOrNull() ?: 0.0)-5; val high=(range.maxOrNull() ?: 1.0)+5
            Canvas(Modifier.fillMaxWidth().height(100.dp).pointerInput(dates) { detectTapGestures { selected=(it.x / size.width * 7).toInt().coerceIn(0,6) } }.semantics { contentDescription="$label: seven daily measurements and their historical reference bands" }) {
                val width=size.width/7
                fun y(v:Double)=(1-(v-low)/(high-low)).toFloat()*size.height
                dates.indices.forEach { i ->
                    if (i/7f <= reveal) {
                        bands[i]?.let { drawRect(Lavender.copy(alpha=.22f),Offset(i*width+2,y(it.upper)),Size(width-4,(y(it.lower)-y(it.upper)).coerceAtLeast(2f))) }
                        values[i]?.let { drawCircle(Lime,4.dp.toPx(),Offset((i+.5f)*width,y(it))) }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { dates.forEach { Text(it.dayOfMonth.toString(),color=Soft,fontSize=10.sp) } }
            val band=bands[selected]
            Text("${dates[selected]}: " + (values[selected]?.let { "${it.roundToInt()} $unit" } ?: "No measurement") + " · " + (band?.let { "usual ${it.lower.roundToInt()}–${it.upper.roundToInt()} $unit (${it.count} days)" } ?: "baseline needs 7 prior measurements"),color=Soft,fontSize=12.sp)
            Slider(selected.toFloat(), { selected=it.roundToInt() }, valueRange=0f..6f, steps=5,modifier=Modifier.semantics { contentDescription="Select $label date" })
        }
    }
}
@Composable internal fun DashboardEditor(preferences: DashboardPreferences, onChange: (DashboardPreferences)->Unit) {
    var expanded by remember { mutableStateOf(false) }
    val prefs=preferences.normalized()
    ExploreCard("MAKE PULSE YOURS") {
        TextButton(onClick={expanded=!expanded}) { Text(if(expanded) "Close dashboard editor" else "Customise dashboard") }
        if(expanded) {
            Text("Overview cards",fontWeight=FontWeight.Bold)
            prefs.order.forEachIndexed { index,id ->
                Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(id !in prefs.hidden,{visible->onChange(prefs.copy(hidden=if(visible)prefs.hidden-id else prefs.hidden+id))},modifier=Modifier.semantics { contentDescription="Show ${DashboardPreferences.name(id)}" })
                    Text(DashboardPreferences.name(id),Modifier.weight(1f),fontSize=12.sp)
                    TextButton(enabled=index>0,onClick={onChange(prefs.move(id,-1))},contentPadding=PaddingValues(0.dp),modifier=Modifier.width(40.dp).semantics { contentDescription="Move ${DashboardPreferences.name(id)} up" }) { Text("↑") }
                    TextButton(enabled=index<prefs.order.lastIndex,onClick={onChange(prefs.move(id,1))},contentPadding=PaddingValues(0.dp),modifier=Modifier.width(40.dp).semantics { contentDescription="Move ${DashboardPreferences.name(id)} down" }) { Text("↓") }
                }
            }
            Text("Open Pulse on",fontWeight=FontWeight.Bold)
            listOf("Today","Trends","Fitness Age","Connect","Workouts","Journal").forEachIndexed { index,name->
                Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) { RadioButton(prefs.startPage==index,{onChange(prefs.copy(startPage=index))}); Text(name) }
            }
            Text("Changes save immediately. Start screen applies on a fresh launch; your current tab stays open.",color=Soft,fontSize=11.sp)
            TextButton(onClick={onChange(DashboardPreferences())}) { Text("Reset dashboard") }
        }
    }
}
