package com.garmincodex.app

internal fun importCategoryName(label:String):String = when(label) {
    "HeartRateRecord"->"heart rate"
    "SleepSessionRecord"->"sleep"
    "StepsRecord"->"step intervals"
    "RestingHeartRateRecord"->"resting heart rate"
    "HeartRateVariabilityRmssdRecord"->"HRV"
    "ExerciseSessionRecord"->"workouts"
    else->label
}
internal fun categoryImportStatus(allowed:Boolean,present:Boolean,rejected:Boolean):String = when {
    !allowed -> "Permission needed"
    rejected -> if(present) "Refresh failed · saved value" else "Refresh failed · no value"
    present -> "Saved value"
    else -> "No shared record for this day"
}
internal fun unavailableForMetric(sync:SyncState,kind:String):Boolean {
    val labels=when(kind) {
        "activity" -> setOf("daily steps","active calories","total calories","basal energy","hourly steps","hourly active calories","hourly total calories")
        "steps" -> setOf("daily steps","hourly steps")
        "calories" -> setOf("active calories","hourly active calories")
        "energy" -> setOf("total calories","hourly total calories")
        "sleep" -> setOf("SleepSessionRecord")
        "hr","strain" -> setOf("HeartRateRecord")
        "recovery" -> setOf("HeartRateRecord","SleepSessionRecord")
        "resting" -> setOf("RestingHeartRateRecord")
        "hrv" -> setOf("HeartRateVariabilityRmssdRecord")
        else -> emptySet()
    }
    return sync.unavailable.any{it in labels}
}
