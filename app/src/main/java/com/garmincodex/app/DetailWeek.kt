package com.garmincodex.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

internal fun detailWeek(days:List<ConnectedDay>,date:LocalDate,metric:TrendMetric):List<Pair<LocalDate,Double?>> {
    val byDate=days.distinctBy{it.date}.associateBy{it.date}
    val source=byDate[date.toString()]?.activitySource
    val activity=metric in setOf(TrendMetric.STEPS,TrendMetric.CALORIES,TrendMetric.ENERGY)
    return (6L downTo 0L).map { offset ->
        val day=date.minusDays(offset)
        val record=byDate[day.toString()]?.takeIf{!activity || source!=null && it.activitySource==source}
        day to record?.let(metric::value)?.takeIf{it.isFinite() && it>=0}
    }
}

@Composable internal fun DetailWeek(days:List<ConnectedDay>,date:LocalDate,metric:TrendMetric,onDate:(String)->Unit) {
    val p=LocalPulseStyle.current
    val week=remember(days,date,metric){detailWeek(days,date,metric)}
    val maximum=(week.mapNotNull{it.second}.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
    JournalCard("${metric.title.uppercase()} · LAST 7 DAYS") {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
            week.forEach { (day,value) ->
                Column(Modifier.weight(1f).clickable(role=Role.Button,onClick={onDate(day.toString())}).semantics{contentDescription="Open metric day $day: ${value?.let(metric::format) ?: "No record"}"}.padding(vertical=8.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.height(48.dp).fillMaxWidth(),contentAlignment=Alignment.BottomCenter) {
                        if(value==null) Text("—",color=p.muted)
                        else Box(Modifier.width(14.dp).height((3+45*(value/maximum)).dp).background(if(day==date)p.primary else p.primary.copy(alpha=.4f),RoundedCornerShape(5.dp)))
                    }
                    Text(day.dayOfMonth.toString(),fontSize=10.sp,color=p.muted)
                }
            }
        }
        Text("Tap a day to explore · missing records stay empty",fontSize=11.sp,color=p.muted)
    }
}
