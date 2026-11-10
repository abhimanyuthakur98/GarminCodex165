package com.garmincodex.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.*
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private fun workoutTime(time:Long)=Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm"))
private fun minutes(w:Workout)=(w.end-w.start)/60000.0
private fun decimal(value:Double)=String.format(java.util.Locale.US,"%.1f",value)
@Composable private fun WorkoutCard(content:@Composable ColumnScope.()->Unit) { Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF29221D),Color(0xFF171D1A))),RoundedCornerShape(26.dp)).border(1.dp,Orange.copy(alpha=.09f),RoundedCornerShape(26.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content) }
@Composable internal fun WorkoutLibrary(workouts:List<Workout>, checkIns:List<WorkoutCheckIn>, resting:Int, maximum:Int, onCheckIn:(WorkoutCheckIn)->Unit,onImport:()->Unit,onPermissions:()->Unit,onRemove:(String)->Unit,message:String?,busy:Boolean,miles:Boolean=false,onMiles:(Boolean)->Unit={}) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    var filter by rememberSaveable { mutableStateOf("All") }
    var window by rememberSaveable { mutableIntStateOf(0) }
    Text("Your training story.",fontSize=32.sp,fontWeight=FontWeight.Bold)
    Text("Recorded sessions, not inferred workouts.",color=Color.Gray,fontSize=13.sp)
    Row { Button(onClick=onImport,enabled=!busy) { Text(if(busy) "Importing…" else "Import FIT") }; TextButton(onClick=onPermissions) { Text("Allow exercise") } }
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { FilterChip(!miles,{onMiles(false)},label={Text("Kilometres")});FilterChip(miles,{onMiles(true)},label={Text("Miles")}) }
    message?.let { Text(it,color=Lime,fontSize=12.sp) }
    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf("All" to 0,"7 days" to 7,"30 days" to 30).forEach { (label,n) -> FilterChip(window==n,{window=n},label={Text(label)}) } }
    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) { (listOf("All")+workouts.map {it.type}.distinct().sorted()).forEach { type -> FilterChip(filter==type,{filter=type},label={Text(if(type=="All") "All activities" else WorkoutAnalysis.title(type))}) } }
    val cutoff=if(window==0) Long.MIN_VALUE else LocalDate.now().minusDays((window-1).toLong()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val filtered=workouts.filter { (filter=="All" || it.type==filter) && it.start>=cutoff }.sortedByDescending {it.start}
    if(filtered.isEmpty()) WorkoutCard { Text("No workouts in this view",fontWeight=FontWeight.Bold); Text("Allow Pulse to read Exercise in Health Connect, then sync Garmin. If Garmin does not share sessions, export an original .fit activity file from Garmin Connect and use Import FIT. Unzip downloaded archives first.",fontSize=13.sp,color=Color.Gray) }
    if(filtered.isEmpty() && (filter!="All" || window!=0)) TextButton(onClick={filter="All";window=0}){Text("Reset workout filters")}
    filtered.forEach { w -> Box(Modifier.clickable(onClickLabel="Open ${w.title}"){selectedId=w.id}) { WorkoutCard {
        Text(w.title,fontSize=22.sp,fontWeight=FontWeight.Bold)
        Text(workoutTime(w.start),color=Lime,fontSize=12.sp)
        w.distanceMeters?.let { Text(RunningMetrics.distance(it,miles),color=Orange,fontWeight=FontWeight.Bold) }
        Text("${decimal(minutes(w))} min elapsed · ${if(w.source=="FIT") "FIT file" else "Garmin / Health Connect"}",fontSize=12.sp)
        checkIns.firstOrNull {it.workoutId==w.id}?.let { Text("Effort ${it.effort}/10 · soreness ${it.soreness}/10",color=Color.Gray,fontSize=12.sp) }
    } } }
    Text("FIT and Health Connect entries remain separate. The same session may appear twice; comparisons stay within one source. Workouts never add a second copy of load to your daily score.",color=Color.Gray,fontSize=11.sp)
    selectedId?.let { id -> workouts.firstOrNull {it.id==id}?.let { w -> WorkoutDetails(w,workouts,checkIns.firstOrNull {it.workoutId==id},resting,maximum,onCheckIn,{onRemove(id);selectedId=null},miles) {selectedId=null} } }
}
@Composable internal fun WorkoutDetails(w:Workout,all:List<Workout>,checkIn:WorkoutCheckIn?,resting:Int,maximum:Int,onSave:(WorkoutCheckIn)->Unit,onRemove:()->Unit,miles:Boolean=false,onClose:()->Unit) {
    val points=remember(w.chart){WorkoutAnalysis.points(w)}
    val load=remember(points,resting,maximum){DailyAnalysis.load(points,resting,maximum)}
    val chart=remember(points){points.groupBy { (it.time-w.start)/((w.end-w.start)/500).coerceAtLeast(1000) }.values.map { chunk -> HrPoint(chunk.first().time,chunk.map {it.bpm}.average().roundToInt()) }.joinToString(";"){"${it.time},${it.bpm}"}}
    var effort by remember(w.id,checkIn){mutableIntStateOf(checkIn?.effort ?: 5)}
    var soreness by remember(w.id,checkIn){mutableIntStateOf(checkIn?.soreness ?: 0)}
    val saved = checkIn?.effort == effort && checkIn.soreness == soreness
    var deleting by remember {mutableStateOf(false)}
    Dialog(onDismissRequest=onClose,properties=DialogProperties(usePlatformDefaultWidth=false)) { Surface(Modifier.fillMaxSize(),color=Color(0xFF090D0C)) { Column(Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        TextButton(onClick=onClose){Text("‹ Workouts")}
        Text(w.title,fontSize=32.sp,fontWeight=FontWeight.Bold)
        Text(workoutTime(w.start),color=Lime)
        WorkoutCard {
            Text("${decimal(minutes(w))} min elapsed",fontSize=28.sp,fontWeight=FontWeight.Bold)
            Text(w.calories?.let {"${it.roundToInt()} kcal · ${if(w.source=="FIT") "reported session energy" else "active energy in session window"}"} ?: "Calories not supplied",color=Orange)
            Text("${points.size} HR samples · ${load.coveredMinutes} min HR coverage",fontSize=12.sp,color=Color.Gray)
            Text("Elapsed duration includes pauses. Health Connect energy may be apportioned from longer source intervals. Missing HR is not filled in.",fontSize=11.sp,color=Color.Gray)
        }
        RunningDetails(w,miles)
        WorkoutCard {
            Text("HEART RATE",fontWeight=FontWeight.Bold)
            InteractiveHeart(ConnectedDay(date=Instant.ofEpochMilli(w.start).atZone(ZoneId.systemDefault()).toLocalDate().toString(),chart=chart))
            Text(if(points.isEmpty()) "No HR samples supplied for this session." else "Original samples: ${points.minOf {it.bpm}}–${points.maxOf {it.bpm}} bpm. Chart uses grouped averages for long sessions.",fontSize=11.sp,color=Color.Gray)
        }
        WorkoutCard {
            Text("TIME IN HR RESERVE ZONES",fontWeight=FontWeight.Bold)
            val entrance=chartEntrance()
            listOf("Below 30%","30–50%","50–60%","60–70%","70–80%","80–90%","90%+").forEachIndexed { i,label ->
                val value=load.zoneMinutes.getOrElse(i){0.0}
                Text("$label · ${decimal(value)} min",fontSize=12.sp)
                LinearProgressIndicator(progress={ (value/(load.zoneMinutes.maxOrNull() ?: 1.0).coerceAtLeast(1.0)).toFloat()*entrance },modifier=Modifier.fillMaxWidth(),color=Lime)
            }
            Text("Profile: resting $resting / maximum $maximum bpm. Gaps over 5 minutes contribute no time. Paused-session samples are not excluded.",fontSize=11.sp,color=Color.Gray)
        }
        WorkoutCard {
            Text("PREVIOUS SAME-TYPE SESSION",fontWeight=FontWeight.Bold)
            val previous=RunningMetrics.comparable(w,all)
            if(previous==null) Text("Another earlier ${WorkoutAnalysis.title(w.type)} session from this source${if((w.distanceMeters ?: 0.0)>0) " within 10% of this distance" else ""} is needed.",color=Color.Gray)
            else {
                Text(workoutTime(previous.start),color=Lime)
                MetricBars(listOf(Triple("This session",minutes(w),"${decimal(minutes(w))} min"),Triple("Previous",minutes(previous),"${decimal(minutes(previous))} min")),Orange)
                if((w.distanceMeters ?: 0.0)>0) { Text("Distance match within 10%: ${RunningMetrics.distance(w.distanceMeters,miles)} vs ${RunningMetrics.distance(previous.distanceMeters,miles)}");Text("Timer pace: ${RunningMetrics.pace(RunningMetrics.speed(w.distanceMeters,w.timerSeconds),miles)} vs ${RunningMetrics.pace(RunningMetrics.speed(previous.distanceMeters,previous.timerSeconds),miles)}") }
                Text("Moving time: ${w.movingSeconds?.let{decimal(it/60)} ?: "—"} vs ${previous.movingSeconds?.let{decimal(it/60)} ?: "—"} min")
                val prevHr=WorkoutAnalysis.points(previous)
                if(points.isNotEmpty() && prevHr.isNotEmpty()) Text("Mean recorded HR: ${points.map {it.bpm}.average().roundToInt()} vs ${prevHr.map {it.bpm}.average().roundToInt()} bpm")
                Text("Descriptive comparison only. Route, distance, conditions and sampling may differ; a lower HR alone does not prove improved fitness.",fontSize=11.sp,color=Color.Gray)
            }
        }
        WorkoutCard {
            Text("HOW DID IT FEEL?",fontWeight=FontWeight.Bold)
            Text("Effort $effort/10 · 1 very easy / 10 maximal")
            Slider(effort.toFloat(),{effort=it.roundToInt()},valueRange=1f..10f,steps=8,modifier=Modifier.semantics {contentDescription="Workout effort"})
            Text("Soreness $soreness/10 · 0 none / 10 severe")
            Slider(soreness.toFloat(),{soreness=it.roundToInt()},valueRange=0f..10f,steps=9,modifier=Modifier.semantics {contentDescription="Workout soreness"})
            Button(onClick={onSave(WorkoutCheckIn(w.id,effort,soreness))}){Text(if(saved) "Check-in saved" else "Save check-in")}
            Text("Self-reported and stored locally. Ratings are not added to recovery or Fitness Age.",fontSize=11.sp,color=Color.Gray)
        }
        if(w.source=="FIT") { if(deleting) Row { Button(onClick=onRemove){Text("Delete FIT session")};TextButton(onClick={deleting=false}){Text("Cancel")} } else TextButton(onClick={deleting=true}){Text("Remove imported FIT session")} }
    } } }
}
