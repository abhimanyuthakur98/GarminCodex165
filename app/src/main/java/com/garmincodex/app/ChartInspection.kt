package com.garmincodex.app

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import java.time.LocalDate
import kotlin.math.abs

internal fun chartIndex(fraction:Float,count:Int):Int = if(count<=0)-1 else (fraction.coerceIn(0f,1f)*count).toInt().coerceAtMost(count-1)
internal fun Modifier.chartScrub(identity:Any,onFraction:(Float)->Unit):Modifier = composed {
    val callback by rememberUpdatedState(onFraction)
    this.pointerInput(identity){detectTapGestures{callback((it.x/size.width.coerceAtLeast(1)).coerceIn(0f,1f))}}
        .pointerInput(identity){detectHorizontalDragGestures(onDragStart={callback((it.x/size.width.coerceAtLeast(1)).coerceIn(0f,1f))}){change,_->
            change.consume();callback((change.position.x/size.width.coerceAtLeast(1)).coerceIn(0f,1f))
        }}
}
internal fun metricKind(metric:TrendMetric):String=when(metric){
    TrendMetric.STEPS->"steps";TrendMetric.CALORIES->"calories";TrendMetric.ENERGY->"energy";TrendMetric.SLEEP->"sleep";TrendMetric.RECOVERY->"recovery";TrendMetric.STRAIN->"strain";TrendMetric.RESTING_HR->"resting";TrendMetric.HRV->"hrv";else->"hr"
}
internal data class RecentComparison(val average:Double?,val count:Int,val selected:Double?)
internal fun recentComparison(days:List<ConnectedDay>,date:LocalDate,metric:TrendMetric):RecentComparison {
    val unique=days.distinctBy{it.date}
    val day=unique.firstOrNull{it.date==date.toString()}
    val activity=metric in setOf(TrendMetric.STEPS,TrendMetric.CALORIES,TrendMetric.ENERGY)
    val previous=unique.filter{it.date>=date.minusDays(14).toString() && it.date<date.toString() && (!activity || day!=null && it.activitySource==day.activitySource)}
        .mapNotNull(metric::value).filter{it.isFinite()}
    return RecentComparison(previous.takeIf{it.size>=7}?.average(),previous.size,day?.let(metric::value))
}
internal fun comparisonText(result:RecentComparison,metric:TrendMetric,date:LocalDate,today:LocalDate=LocalDate.now()):String {
    val average=result.average ?: return "${result.count}/7 prior recorded days · building your comparison"
    val baseline="Prior average ${metric.format(average)} · ${result.count}/14 days"
    if(date>=today) return "$baseline · selected day may still change"
    val value=result.selected ?: return "No selected-day record · $baseline"
    val delta=value-average
    val change=if(abs(delta)<0.5)"About the same as your recent average" else metric.format(abs(delta))+if(delta>0)" above your recent average" else " below your recent average"
    return "$change\n$baseline"
}
internal fun stageAtFraction(spans:List<SleepSpan>,fraction:Float):SleepSpan? {
    val valid=spans.filter{it.end>it.start}
    if(valid.isEmpty())return null
    val start=valid.minOf{it.start};val end=valid.maxOf{it.end}
    val time=(start+((end-start)*fraction.coerceIn(0f,1f)).toLong()).coerceAtMost(end-1)
    return valid.firstOrNull{time>=it.start && time<it.end}
}
