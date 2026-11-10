package com.garmincodex.app

import java.time.*
import java.time.format.DateTimeFormatter

internal fun importReport(days:List<ConnectedDay>,from:String,to:String,workouts:Int):String {
    val latest=days.mapNotNull{it.lastSample}.maxOrNull()?.let{Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm"))} ?: "none supplied"
    return "$from → $to\n" +
        "Heart rate: ${days.sumOf{it.hrCount}} samples\n" +
        "Sleep: ${days.count{it.sleepStart!=null}} nights\n" +
        "Steps: ${days.count{it.steps!=null}} days\n" +
        "Active calories: ${days.count{it.activeCalories!=null}} days\n" +
        "Total calories: ${days.count{it.calories!=null}} days\n" +
        "Basal energy: ${days.count{it.basalCalories!=null}} days\n" +
        "Resting HR: ${days.count{it.restingHr!=null}} days · HRV: ${days.count{it.hrv!=null}} days\n" +
        "Workouts: $workouts sessions (includes preceding day)\n" +
        "Latest HR measurement: $latest"
}
