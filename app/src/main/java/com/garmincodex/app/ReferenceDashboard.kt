package com.garmincodex.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.*
import java.time.format.DateTimeFormatter

@Composable internal fun QuickWeek(date:String,onDate:(String)->Unit) {
    val p=LocalPulseStyle.current
    val selected=LocalDate.parse(date)
    val monday=selected.minusDays(selected.dayOfWeek.value-1L)
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
        repeat(7) { index ->
            val day=monday.plusDays(index.toLong());val active=day==selected
            val ink=if(active)tileInk(p.primary) else if(day>LocalDate.now())p.muted.copy(alpha=.35f) else p.muted
            Column(Modifier.weight(1f).background(if(active)p.primary else Color.Transparent,RoundedCornerShape(16.dp))
                .selectable(active,enabled=day<=LocalDate.now(),role=Role.Tab,onClick={onDate(day.toString())})
                .semantics{contentDescription="Choose day $day"}.padding(vertical=6.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(3.dp)) {
                Text(day.format(DateTimeFormatter.ofPattern("EEE")),fontSize=10.sp,color=ink)
                Text(day.dayOfMonth.toString(),fontSize=14.sp,color=ink,fontWeight=if(active)FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable internal fun WeekActivity(days:List<ConnectedDay>,selected:String,goals:PersonalGoals) {
    val p=LocalPulseStyle.current
    val end=LocalDate.parse(selected)
    val values=(6 downTo 0).map { back -> val date=end.minusDays(back.toLong());date to days.firstOrNull{it.date==date.toString()}?.steps }
    val maximum=maxOf(goals.steps.toLong(),values.mapNotNull{it.second}.maxOrNull() ?: 1L).coerceAtLeast(1)
    val reveal=chartEntrance(selected)
    Row(Modifier.fillMaxWidth().background(p.surface,RoundedCornerShape(22.dp)).padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
            Text("Your week",fontSize=16.sp,fontWeight=FontWeight.Medium)
            Text("Steps · last 7 days",fontSize=11.sp,color=p.muted)
            Text("${values.count{it.second!=null}} / 7 days recorded",fontSize=10.sp,color=p.muted)
        }
        Row(Modifier.weight(1.15f),horizontalArrangement=Arrangement.spacedBy(5.dp),verticalAlignment=Alignment.Bottom) {
            values.forEach { (date,value) -> Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Box(Modifier.fillMaxWidth().height(42.dp).semantics {contentDescription="$date: ${value?.let{"$it steps"} ?: "no record"}"},contentAlignment=Alignment.BottomCenter) {
                    if(value!=null) Box(Modifier.fillMaxWidth().height((maxOf(2f,value.toFloat()/maximum*42)*reveal).dp).background(p.primary.copy(alpha=if(date==end)1f else .55f),RoundedCornerShape(4.dp)))
                    else Box(Modifier.size(3.dp).background(p.muted.copy(alpha=.3f),RoundedCornerShape(3.dp)))
                }
                Text(date.dayOfWeek.toString().take(1),color=p.muted,fontSize=9.sp)
            } }
        }
    }
}

@Composable internal fun HeartSpark(day:ConnectedDay?) {
    val p=LocalPulseStyle.current
    val points=remember(day?.chart){day?.chart.orEmpty().split(';').mapNotNull{row->val a=row.split(',');if(a.size==2)runCatching{a[0].toLong() to a[1].toInt()}.getOrNull() else null}.sortedBy{it.first}}
    if(points.size<2)return
    val reveal=chartEntrance(day?.updatedAt ?: 0L)
    Canvas(Modifier.fillMaxWidth().height(58.dp).semantics {contentDescription="Heart-rate chart from imported samples. Gaps indicate missing readings."}) {
        val start=points.first().first;val span=(points.last().first-start).coerceAtLeast(1)
        val low=points.minOf{it.second}-5;val range=(points.maxOf{it.second}-low+5).coerceAtLeast(1)
        points.filterIndexed{i,_->i%maxOf(1,points.size/70)==0}.forEach{(time,bpm)->
            val x=(time-start).toFloat()/span*size.width
            val y=size.height-(bpm-low).toFloat()/range*size.height
            if(x<=size.width*reveal)drawLine(p.secondary,Offset(x,y-2.dp.toPx()),Offset(x,y+2.dp.toPx()),2.dp.toPx(),StrokeCap.Round)
        }
    }
}

internal data class RhythmStats(val current:Int,val longest:Int,val total:Int)
internal fun rhythmStats(dates:Set<LocalDate>,today:LocalDate):RhythmStats {
    val valid=dates.filter{it<=today}.sorted()
    var longest=0;var run=0;var previous:LocalDate?=null
    valid.forEach{run=if(previous?.plusDays(1)==it)run+1 else 1;longest=maxOf(longest,run);previous=it}
    var cursor=if(today in dates)today else today.minusDays(1);var current=0
    while(cursor in dates){current++;cursor=cursor.minusDays(1)}
    return RhythmStats(current,longest,valid.size)
}

@Composable internal fun JournalRhythm(entries:List<JournalEntry>) {
    val p=LocalPulseStyle.current
    val today=LocalDate.now()
    val dates=entries.map{LocalDate.parse(it.date)}.toSet()
    val stats=rhythmStats(dates,today)
    val start=today.minusDays(today.dayOfWeek.value-1L).minusWeeks(11)
    JournalCard("YOUR CHECK-IN RHYTHM") {
        Text("Small moments add up",fontSize=22.sp,fontWeight=FontWeight.Medium)
        Canvas(Modifier.fillMaxWidth().height(126.dp).semantics{contentDescription="Journal heatmap: ${dates.count{it>=start && it<=today}} check-ins in the last 12 calendar weeks. Bright cells are saved check-ins; dim cells have none."}) {
            val w=size.width/12;val h=size.height/7
            repeat(12){week->repeat(7){weekday->val d=start.plusDays(week*7L+weekday)
                drawRoundRect(if(d in dates && d<=today)p.primary else Color.White.copy(alpha=if(d>today).025f else .07f),Offset(week*w,weekday*h),Size(w-4.dp.toPx(),h-3.dp.toPx()),CornerRadius(3.dp.toPx()))
            }}
        }
        Text(start.format(DateTimeFormatter.ofPattern("d MMM"))+" — "+today.format(DateTimeFormatter.ofPattern("d MMM yyyy")),fontSize=12.sp,color=p.muted)
        Text("Each square is a day. Bright = saved check-in.",fontSize=11.sp,color=p.muted)
    }
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        listOf(Triple("Current streak",stats.current,"Longest: ${stats.longest} days"),Triple("Total check-ins",stats.total,"All saved days")).forEach{(label,value,caption)->
            Column(Modifier.weight(1f).fillMaxHeight().background(p.surface,RoundedCornerShape(24.dp)).padding(18.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
                Text(label,fontSize=12.sp)
                Text(value.toString(),color=p.primary,fontSize=40.sp,fontWeight=FontWeight.Light)
                Text(caption,color=p.muted,fontSize=11.sp)
            }
        }
    }
}
