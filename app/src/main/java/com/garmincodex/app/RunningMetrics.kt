package com.garmincodex.app

import java.util.Locale
import kotlin.math.roundToInt

data class PaceSample(val time:Long,val speed:Double) {
    fun encode()="$time,$speed"
    companion object { fun decode(raw:String)=raw.split(';').mapNotNull { row -> val p=row.split(','); if(p.size!=2) null else p[0].toLongOrNull()?.let {time->p[1].toDoubleOrNull()?.takeIf{it.isFinite() && it in 0.0..100.0}?.let{PaceSample(time,it)} } }.distinctBy{it.time}.sortedBy{it.time} }
}
data class RunLap(val start:Long,val end:Long,val distance:Double?,val timer:Double?) {
    fun encode()="$start,$end,${distance ?: ""},${timer ?: ""}"
    companion object {fun decode(raw:String)=raw.split(';').mapNotNull {row->val p=row.split(',');if(p.size!=4)null else runCatching {RunLap(p[0].toLong(),p[1].toLong(),p[2].toDoubleOrNull(),p[3].toDoubleOrNull())}.getOrNull()}.filter{it.end>it.start}.sortedBy{it.start}}
}
object RunningMetrics {
    fun valid(value:Double?)=value?.takeIf{it.isFinite() && it>=0}
    fun unitMeters(miles:Boolean)=if(miles)1609.344 else 1000.0
    fun unit(miles:Boolean)=if(miles)"mi" else "km"
    fun distance(meters:Double?,miles:Boolean)=meters?.let{String.format(Locale.US,"%.2f %s",it/unitMeters(miles),unit(miles))} ?: "Not supplied"
    fun pace(speed:Double?,miles:Boolean):String {
        if(speed==null || !speed.isFinite() || speed<=0) return "—"
        val seconds=(unitMeters(miles)/speed).roundToInt()
        return "%d:%02d /%s".format(Locale.US,seconds/60,seconds%60,unit(miles))
    }
    fun speed(distance:Double?,seconds:Double?)=if(distance!=null && seconds!=null && distance>0 && seconds>0) distance/seconds else null
    fun comparable(w:Workout,all:List<Workout>):Workout? {
        val distance=w.distanceMeters?.takeIf{it>0} ?: return WorkoutAnalysis.previous(w,all)
        return all.filter{it.start<w.start && it.source==w.source && it.type==w.type && it.distanceMeters?.let {d->d>0 && kotlin.math.abs(d-distance)/distance<=.10}==true}.maxByOrNull{it.start}
    }
}
