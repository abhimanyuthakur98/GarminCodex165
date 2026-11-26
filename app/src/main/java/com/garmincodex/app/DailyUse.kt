package com.garmincodex.app

import kotlinx.coroutines.flow.*
import java.util.Locale
import kotlin.math.ceil

data class CachedHealth(val days:List<ConnectedDay> = emptyList(),val loaded:Boolean=false)
internal fun cachedHealth(records:Flow<List<ConnectedDay>>,status:Flow<SyncState>):Flow<CachedHealth> =
    combine(records,status.map{it.stepSource}.distinctUntilChanged()) { days,source ->
        CachedHealth(days.map{ActivitySource.matching(it,source)},true)
    }

internal fun dailyTakeaway(day:ConnectedDay?,sync:SyncState,goals:PersonalGoals):String {
    if(sync.lastError.isNotBlank() || unavailableForMetric(sync,"activity")) return "Saved activity shown · review your import status"
    if(day?.steps==null || day.activeCalories==null) return "Some activity is missing · check device sharing"
    val steps=(goals.steps.toLong()-day.steps).coerceAtLeast(0)
    val calories=ceil((goals.activeCalories-day.activeCalories).coerceAtLeast(0.0)).toInt()
    return when {
        steps==0L && calories==0 -> "Both activity goals reached"
        steps==0L -> "Step goal reached · $calories active kcal to go"
        calories==0 -> "Calorie goal reached · ${String.format(Locale.getDefault(),"%,d",steps)} steps to go"
        else -> "${String.format(Locale.getDefault(),"%,d",steps)} steps to your daily goal"
    }
}
