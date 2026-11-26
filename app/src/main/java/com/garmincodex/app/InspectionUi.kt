package com.garmincodex.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.*
import java.time.format.DateTimeFormatter

@Composable internal fun MetricRecordInfo(day:ConnectedDay?,sync:SyncState,kind:String) {
    var expanded by remember(kind){mutableStateOf(false)}
    val p=LocalPulseStyle.current
    Column {
        Text(metricSource(day,kind),fontSize=11.sp,color=p.primary)
        Text("Pulse import · "+importStamp(day)+(if(sync.busy)" · refreshing" else ""),fontSize=10.sp,color=p.muted)
        if(!expanded && (sync.lastError.isNotBlank() || unavailableForMetric(sync,kind))) Text("Import needs attention · saved values may be older",fontSize=12.sp,color=p.warm)
        TextButton(onClick={expanded=!expanded},contentPadding=PaddingValues(0.dp)){Text(if(expanded)"Hide data source & import status" else "Data source & import status",fontSize=12.sp)}
        if(expanded) DataFreshness(day,sync,kind)
    }
}

@Composable internal fun SleepStageExplorer(day:ConnectedDay?) {
    val p=LocalPulseStyle.current
    val spans=remember(day?.stages){day?.stages.orEmpty().split(';').mapNotNull{row->val a=row.split(',');if(a.size==3)runCatching{SleepSpan(a[0].toLong(),a[1].toLong(),a[2].toInt())}.getOrNull()?.takeIf{it.end>it.start} else null}.sortedBy{it.start}}
    var fraction by remember(spans){mutableFloatStateOf(0f)}
    var list by remember(day?.date){mutableStateOf(false)}
    fun stamp(time:Long)=Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm XXX"))
    fun label(stage:Int)=when(stage){5->"Deep";4->"Light";6->"REM";1,3,7->"Awake";2->"Asleep · stage unspecified";else->"Unknown"}
    fun ink(stage:Int)=when(stage){5->p.violet;4->p.primary;6->p.secondary;1,3,7->p.warm;else->p.muted}
    JournalCard("EXPLORE SLEEP STAGES",p.violet) {
        if(spans.isEmpty()) Text("No sleep-stage records yet.",color=p.muted)
        else {
            val chosen=stageAtFraction(spans,fraction)
            val start=spans.minOf{it.start};val end=spans.maxOf{it.end};val duration=(end-start).toFloat()
            Text(chosen?.let{label(it.stage)} ?: "No stage record at this time",fontSize=20.sp,fontWeight=FontWeight.SemiBold)
            Text(chosen?.let{"${stamp(it.start)}–${stamp(it.end)} · ${(it.end-it.start)/60000} min"} ?: stamp(start+((end-start)*fraction).toLong()),fontSize=12.sp,color=p.muted)
            Text("Health Connect · imported stages",fontSize=11.sp,color=p.muted)
            Canvas(Modifier.fillMaxWidth().height(64.dp).semantics{contentDescription="Sleep-stage timeline"}.chartScrub(spans){fraction=it}) {
                spans.forEach { span ->
                    val left=(span.start-start)/duration*size.width
                    val right=(span.end-start)/duration*size.width
                    drawRect(ink(span.stage).copy(alpha=if(span==chosen)1f else .5f),Offset(left,12.dp.toPx()),Size((right-left).coerceAtLeast(1f),40.dp.toPx()))
                }
                drawLine(Color.White,Offset(fraction*size.width,0f),Offset(fraction*size.width,size.height),2.dp.toPx())
            }
            Slider(fraction,{fraction=it},modifier=Modifier.semantics{contentDescription="Select sleep-stage time"})
            Text("Tap or drag to inspect. Empty gaps have no stage record.",fontSize=11.sp,color=p.muted)
            TextButton(onClick={list=!list}){Text(if(list)"Hide stage list" else "Show stage list")}
            if(list) spans.forEach { span ->
                Column(Modifier.fillMaxWidth()) {
                    Text(label(span.stage),fontSize=13.sp,color=ink(span.stage))
                    Text("${stamp(span.start)}–${stamp(span.end)}",fontSize=11.sp,color=p.muted)
                }
            }
        }
    }
}
