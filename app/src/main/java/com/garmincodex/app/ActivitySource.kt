package com.garmincodex.app

internal object ActivitySource {
    const val GARMIN = "com.garmin.android.apps.connectmobile"
    // Never label a previous source's cached totals as the newly selected source.
    fun matching(day:ConnectedDay,source:String):ConnectedDay = if(day.activitySource==source) day else day.copy(
        steps=null,activeCalories=null,calories=null,basalCalories=null,
        hourly=HourBucket.decode(day.hourly).map{it.copy(steps=null,active=null,total=null)}.joinToString(";"){it.encode()})
}
