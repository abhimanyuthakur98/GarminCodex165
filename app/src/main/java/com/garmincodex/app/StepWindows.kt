package com.garmincodex.app

import java.time.*

internal object StepWindows {
    // A cumulative source can write an interval ending at midnight even before the day ends.
    // Querying only up to 'now' prorates that record and understates the source's total.
    fun bounds(date: LocalDate, zone: ZoneId): Pair<Instant, Instant> =
        date.atStartOfDay(zone).toInstant() to date.plusDays(1).atStartOfDay(zone).toInstant()
    fun coarse(start: Instant, end: Instant): Boolean = Duration.between(start,end) > Duration.ofHours(1)
    fun label(pkg: String): String = when(pkg) {
        "com.garmin.android.apps.connectmobile" -> "Garmin Connect"
        "com.sec.android.app.shealth" -> "Samsung Health"
        "com.fitbit.FitbitMobile" -> "Fitbit"
        "com.google.android.apps.fitness" -> "Google Fit"
        "" -> "Health Connect priority"
        else -> if(pkg.startsWith("com.android.healthconnect.phone") || pkg=="android") "Phone steps" else pkg
    }
}
