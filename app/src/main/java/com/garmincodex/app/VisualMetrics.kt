package com.garmincodex.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Comparable quantities share a zero-based scale. Missing readings never become zero. */
@Composable internal fun MetricBars(rows:List<Triple<String,Double?,String>>,tint:Color=Lime,scale:Double?=null) {
    val ceiling=(scale ?: rows.mapNotNull{it.second?.takeIf{v->v.isFinite() && v>=0}}.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
    val reveal=chartEntrance()
    rows.forEachIndexed { index,(label,value,display) ->
        Column(Modifier.fillMaxWidth().semantics(mergeDescendants=true){contentDescription="$label: $display"},verticalArrangement=Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Text(label,fontSize=12.sp,modifier=Modifier.weight(1f));Text(display,fontSize=12.sp,color=if(value==null) Color.Gray else tint) }
            Box(Modifier.fillMaxWidth().height(9.dp).background(Color.White.copy(alpha=.07f),RoundedCornerShape(5.dp))) {
                if(value!=null && value.isFinite() && value>0) Box(Modifier.fillMaxWidth(((value/ceiling).coerceIn(0.0,1.0)*reveal).toFloat()).fillMaxHeight().background(tint.copy(alpha=if(index==0)1f else .6f),RoundedCornerShape(5.dp)))
            }
        }
    }
}

@Composable internal fun WeekSleepChart(days:List<ConnectedDay>,end:LocalDate,need:Int) {
    val dates=remember(end){(7 downTo 1).map{end.minusDays(it.toLong())}}
    val values=dates.map{date->days.firstOrNull{it.date==date.toString()}?.asleep}
    var selected by remember(end){mutableIntStateOf(6)}
    val ceiling=maxOf(need,values.filterNotNull().maxOrNull() ?: need,1).toFloat()
    val reveal=chartEntrance()
    val tint=Color(0xFFA9B6FF)
    val value=values[selected]
    Text("${dates[selected].format(DateTimeFormatter.ofPattern("EEE d MMM"))} · ${value?.let{"${it/60}h ${it%60}m"} ?: "No sleep record"}",color=tint,fontSize=14.sp)
    Box(Modifier.fillMaxWidth().height(132.dp)) {
        Box(Modifier.fillMaxWidth().offset(y=(112*(1-need/ceiling)).dp).height(1.dp).background(tint.copy(alpha=.4f)))
        Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            dates.forEachIndexed { i,date ->
                Column(Modifier.weight(1f).fillMaxHeight().clickable(role=Role.Button,onClickLabel="Inspect sleep on $date"){selected=i}.semantics{contentDescription="$date: ${values[i]?.let{"$it minutes asleep"} ?: "No sleep record"}"},horizontalAlignment=Alignment.CenterHorizontally) {
                    Box(Modifier.height(112.dp).fillMaxWidth(),contentAlignment=Alignment.BottomCenter) {
                        if(values[i]!=null) Box(Modifier.fillMaxWidth(.65f).fillMaxHeight((values[i]!!/ceiling*reveal).coerceIn(.001f,1f)).background(tint.copy(alpha=if(i==selected)1f else .45f),RoundedCornerShape(topStart=6.dp,topEnd=6.dp)))
                        else Text("—",color=Color.Gray,fontSize=12.sp)
                    }
                    Text(date.dayOfWeek.name.take(1),color=if(i==selected)tint else Color.Gray,fontSize=11.sp)
                }
            }
        }
    }
    Text("Tap a day · line = ${need/60}h ${need%60}m goal · — = missing",color=Color.Gray,fontSize=11.sp)
}

@Composable internal fun ImportCoverage(days:List<ConnectedDay>) {
    val today=LocalDate.now()
    val dates=(13 downTo 0).map{today.minusDays(it.toLong()).toString()}
    val categories:List<Pair<String,(ConnectedDay)->Boolean>> = listOf("HR" to {it.hrCount>0},"Sleep" to {it.sleepStart!=null},"Steps" to {it.steps!=null},"Active kcal" to {it.activeCalories!=null})
    Text("14-DAY DATA COVERAGE",fontSize=11.sp,color=Lime)
    categories.forEach{(label,present)->
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(3.dp)) {
            Text(label,fontSize=10.sp,modifier=Modifier.width(60.dp))
            dates.forEach{date->
                val found=days.firstOrNull{it.date==date}?.let(present)==true
                Box(Modifier.weight(1f).height(12.dp).background(if(found)Lime.copy(alpha=.75f) else Color.White.copy(alpha=.08f),RoundedCornerShape(3.dp)).semantics{contentDescription="$label $date: ${if(found) "Data present" else "No imported data"}"})
            }
        }
    }
    Text("Oldest → today · filled = data present, dim = missing. Presence does not mean full-day coverage.",fontSize=11.sp,color=Color.Gray)
}

@Composable internal fun ChartExplanation(text:String) {
    var expanded by remember{mutableStateOf(false)}
    TextButton(onClick={expanded=!expanded},contentPadding=PaddingValues(0.dp)) {Text(if(expanded) "Hide explanation" else "How to read this",fontSize=12.sp)}
    if(expanded) Text(text,fontSize=11.sp,color=Color.Gray)
}
