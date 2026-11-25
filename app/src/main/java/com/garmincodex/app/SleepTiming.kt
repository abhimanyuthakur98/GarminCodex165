package com.garmincodex.app

import java.time.*
import kotlin.math.*

internal data class SleepTiming(val date:LocalDate,val bedtime:Int?,val wake:Int?)
internal fun sleepTiming(days:List<ConnectedDay>,end:LocalDate,zone:ZoneId=ZoneId.systemDefault()):List<SleepTiming> {
    val byDate=days.distinctBy{it.date}.associateBy{it.date}
    fun minute(time:Long)=Instant.ofEpochMilli(time).atZone(zone).let{it.hour*60+it.minute}
    return (6 downTo 0).map { back ->
        val date=end.minusDays(back.toLong());val day=byDate[date.toString()]
        val valid=day?.sleepStart!=null && day.sleepEnd!=null && day.sleepEnd>day.sleepStart
        SleepTiming(date,if(valid)minute(day!!.sleepStart!!) else null,if(valid)minute(day!!.sleepEnd!!) else null)
    }
}
internal fun clockMean(minutes:List<Int>):Int? {
    if(minutes.size<3) return null
    val x=minutes.sumOf{cos(it*2*PI/1440)}/minutes.size
    val y=minutes.sumOf{sin(it*2*PI/1440)}/minutes.size
    if(hypot(x,y)<0.25) return null
    return ((atan2(y,x)*1440/(2*PI)).roundToInt()+1440)%1440
}
internal fun wakeChanges(nights:List<SleepTiming>):List<Int> = nights.zipWithNext().mapNotNull { (a,b) ->
    if(a.wake==null || b.wake==null || a.date.plusDays(1)!=b.date) null
    else abs(a.wake-b.wake).let{minOf(it,1440-it)}
}
internal fun sleepClock(minute:Int?):String = minute?.let{String.format(java.util.Locale.ROOT,"%02d:%02d",it/60,it%60)} ?: "—"
