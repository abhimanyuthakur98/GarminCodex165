package com.garmincodex.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

@Composable internal fun RunningDetails(w:Workout,miles:Boolean) {
    JournalCard("DISTANCE & TIMING",Orange) {
        Text(RunningMetrics.distance(w.distanceMeters,miles),fontSize=30.sp,fontWeight=FontWeight.Bold)
        val elapsed=(w.end-w.start)/1000.0
        MetricBars(listOf(Triple("Elapsed",elapsed,"${(elapsed/60).roundToInt()} min"),Triple("Timer",w.timerSeconds,w.timerSeconds?.let{"${(it/60).roundToInt()} min"} ?: "Not supplied"),Triple("Moving",w.movingSeconds,w.movingSeconds?.let{"${(it/60).roundToInt()} min"} ?: "Not supplied")),Orange,elapsed)
        Text("Average timer pace ${RunningMetrics.pace(RunningMetrics.speed(w.distanceMeters,w.timerSeconds),miles)}",color=Lime)
        ChartExplanation("Timer time excludes stopped-timer periods; moving time is shown only when explicitly recorded. Pace uses timer time, not elapsed time.")
        if(w.source=="FIT" && w.distanceMeters==null) Text("Reimport the original FIT file to populate these fields. If the file lacks them, they remain unavailable.",fontSize=12.sp)
    }
    PaceChart(w,miles)
    JournalCard("RECORDED LAPS / SPLITS",Orange) {
        val laps=remember(w.laps){RunLap.decode(w.laps)}
        var lapPage by remember(w.id){mutableIntStateOf(0)}
        val pageCount=((laps.size+19)/20).coerceAtLeast(1)
        val visibleLaps=laps.drop(lapPage.coerceAtMost(pageCount-1)*20).take(20)
        if(pageCount>1) Row { TextButton(onClick={lapPage--},enabled=lapPage>0){Text("Previous")};Text("${lapPage+1}/$pageCount");TextButton(onClick={lapPage++},enabled=lapPage<pageCount-1){Text("Next")} }
        if(laps.isEmpty()) Text("No lap records supplied. Reimport an existing FIT file to read its lap summaries.",fontSize=13.sp)
        Text("Lap speed · longer bar = faster",fontSize=11.sp,color=Color.Gray)
        MetricBars(visibleLaps.mapIndexed { i,lap -> Triple("Lap ${lapPage*20+i+1}",RunningMetrics.speed(lap.distance,lap.timer),RunningMetrics.pace(RunningMetrics.speed(lap.distance,lap.timer),miles)) },Orange)
        var showLaps by remember(w.id){mutableStateOf(false)}
        if(laps.isNotEmpty()) TextButton(onClick={showLaps=!showLaps}){Text(if(showLaps) "Hide lap details" else "Distances & lap times")}
        if(showLaps) visibleLaps.forEachIndexed { i,lap ->
            Text("Lap ${lapPage*20+i+1} · ${RunningMetrics.distance(lap.distance,miles)}",fontWeight=FontWeight.Bold)
            Text("${lap.timer?.let{String.format(java.util.Locale.US,"%.1f min timer",it/60)} ?: "Timer unavailable"} · ${RunningMetrics.pace(RunningMetrics.speed(lap.distance,lap.timer),miles)}",fontSize=13.sp,color=Lime)
            HorizontalDivider(color=Color.White.copy(alpha=.06f))
        }
        ChartExplanation("These are the file's recorded laps, including partial/manual laps. Changing units converts distances and pace; it does not regroup laps into exact kilometre/mile splits.")
    }
}
@Composable private fun PaceChart(w:Workout,miles:Boolean) {
    val samples=remember(w.paceChart){PaceSample.decode(w.paceChart).filter{it.time>=w.start && it.time<w.end}}
    val hr=remember(w.chart){WorkoutAnalysis.points(w)}
    val plot=remember(samples){samples.groupBy{(it.time-w.start)/((w.end-w.start)/600).coerceAtLeast(1000)}.values.map {group->PaceSample(group.first().time,group.map{it.speed}.average())}}
    var selected by remember(w.id,w.paceChart){mutableIntStateOf(0)}
    val reveal=chartEntrance()
    JournalCard("PACE THROUGH YOUR SESSION",Orange) {
        val point=plot.getOrNull(selected)
        Text(point?.let {"${RunningMetrics.pace(it.speed,miles)} · +${(it.time-w.start)/60000} min"} ?: "No speed samples supplied",fontSize=22.sp,fontWeight=FontWeight.Bold)
        val near=point?.let{p->hr.minByOrNull{abs(it.time-p.time)}?.takeIf{abs(it.time-p.time)<=60000}}
        Text(near?.let{"${it.bpm} bpm near selected time"} ?: "No HR sample within 60 seconds",fontSize=12.sp,color=Lime)
        val positive=plot.filter{it.speed>0}
        val fast=positive.maxOfOrNull{it.speed};val slow=positive.minOfOrNull{it.speed}
        Text("Top ${RunningMetrics.pace(fast,miles)} · bottom ${RunningMetrics.pace(slow,miles)}",fontSize=11.sp,color=Color.Gray)
        Canvas(Modifier.fillMaxWidth().height(180.dp).semantics{contentDescription="Pace chart; faster pace is higher. Use the sample slider below."}.pointerInput(plot){detectTapGestures {tap->if(plot.isNotEmpty()){val t=w.start+(tap.x/size.width).toDouble()*(w.end-w.start);selected=plot.indices.minBy{abs(plot[it].time-t)}}}}) {
            val paceValues=plot.mapNotNull{if(it.speed>0)RunningMetrics.unitMeters(miles)/it.speed else null}
            val low=paceValues.minOrNull() ?: 0.0;val high=(paceValues.maxOrNull() ?: 1.0).coerceAtLeast(low+1)
            fun x(t:Long)=((t-w.start).toDouble()/(w.end-w.start).coerceAtLeast(1)*size.width).toFloat()
            fun y(s:Double)=((RunningMetrics.unitMeters(miles)/s-low)/(high-low)*size.height*.85+size.height*.05).toFloat()
            repeat(4){i->drawLine(Color.White.copy(alpha=.06f),Offset(0f,i*size.height/4),Offset(size.width,i*size.height/4))}
            clipRect(right=size.width*reveal) {
                plot.filter{it.speed>0}.forEach{drawCircle(Orange,1.5.dp.toPx(),Offset(x(it.time),y(it.speed)))}
                plot.zipWithNext().forEach{(a,b)->if(a.speed>0 && b.speed>0 && b.time-a.time<=60000)drawLine(Orange,Offset(x(a.time),y(a.speed)),Offset(x(b.time),y(b.speed)),2.dp.toPx())}
            }
            point?.let{drawLine(Color.White.copy(alpha=.4f),Offset(x(it.time),0f),Offset(x(it.time),size.height))}
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("0 min",fontSize=11.sp,color=Color.Gray);Text("${(w.end-w.start)/60000} min elapsed",fontSize=11.sp,color=Color.Gray)}
        if(plot.size>1) Slider(selected.toFloat(),{selected=it.roundToInt()},valueRange=0f..plot.lastIndex.toFloat(),modifier=Modifier.semantics{contentDescription="Select pace sample"})
        ChartExplanation("Tap or slide to inspect pace with nearby HR. Speed samples are averaged in time bins for display. Zero speed has no pace; gaps over one minute stay disconnected.")
    }
}
