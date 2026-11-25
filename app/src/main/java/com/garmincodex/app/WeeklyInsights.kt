package com.garmincodex.app

import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

internal data class GoalWeek(val label:String,val values:List<Double?>,val goal:Int) {
    val recorded get()=values.count{it!=null}
    val reached get()=values.count{it!=null && it>=goal}
    val summary get()=if(recorded==0) "No recorded days" else "$reached of $recorded recorded days"
}
internal fun weeklyGoals(days:List<ConnectedDay>,end:LocalDate,goals:PersonalGoals,need:Int):List<GoalWeek> {
    val byDate=days.distinctBy{it.date}.associateBy{it.date}
    val week=(7 downTo 1).map{byDate[end.minusDays(it.toLong()).toString()]}
    return listOf(GoalWeek("Steps",week.map{it?.steps?.toDouble()},goals.steps),GoalWeek("Active calories",week.map{it?.activeCalories?.takeIf{v->v.isFinite() && v>=0}},goals.activeCalories),GoalWeek("Sleep",week.map{it?.asleep?.toDouble()},need))
}
internal fun weeklyValue(kind:String,value:Double?):String = when {
    value==null -> "—"
    kind=="sleep" -> value.roundToInt().let{ "${it/60}h ${it%60}m" }
    kind=="strain" -> String.format(Locale.getDefault(),"%.1f",value)
    else -> String.format(Locale.getDefault(),"%,d",value.roundToInt())
}
internal fun weeklyChange(result:ReportValue,kind:String):String {
    if(result.currentDays!=7 || result.previousDays!=7 || result.current==null || result.previous==null) return "Full weeks needed to compare"
    val delta=result.current-result.previous
    if(abs(delta)<if(kind=="strain")0.05 else 0.5) return "About the same as last week"
    val amount=weeklyValue(if(kind=="sleep")"minutes" else kind,abs(delta))
    val unit=when(kind){"sleep"->" min/night";"steps"->" steps/day";"calories"->" kcal/day";"recovery"->" points";else->""}
    return amount+unit+if(delta>0) " more than last week" else " less than last week"
}
