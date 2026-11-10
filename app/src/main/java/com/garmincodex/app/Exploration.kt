package com.garmincodex.app

import java.time.*
import kotlin.math.*

data class DashboardPreferences(val order: List<String> = CARDS, val hidden: Set<String> = emptySet(), val startPage: Int = 0) {
    fun normalized() = copy(order = (order.filter { it in CARDS } + CARDS).distinct(), hidden = hidden.intersect(CARDS.toSet()), startPage = startPage.coerceIn(0, 5))
    fun move(id: String, delta: Int): DashboardPreferences {
        val list = normalized().order.toMutableList(); val index = list.indexOf(id)
        if (index < 0 || index + delta !in list.indices) return this
        java.util.Collections.swap(list, index, index + delta)
        return copy(order = list)
    }
    companion object {
        val CARDS = listOf("briefing", "recovery", "activity", "timeline", "sleep", "heart", "fitness")
        val NAMES = listOf("Morning briefing", "Recovery", "Steps & calories", "Activity timeline", "Sleep", "Heart rate", "Fitness Age")
        fun name(id: String) = NAMES[CARDS.indexOf(id)]
    }
}
data class Baseline(val lower: Double, val upper: Double, val count: Int)
object PersonalBaseline {
    fun calculate(days: List<ConnectedDay>, date: LocalDate, value: (ConnectedDay) -> Double?): Baseline? {
        val samples = days.distinctBy { it.date }.filter { val d = LocalDate.parse(it.date); d < date && d >= date.minusDays(14) }.mapNotNull(value).filter { it.isFinite() }.sorted()
        if (samples.size < 7) return null
        fun quantile(q: Double): Double { val position = (samples.size - 1) * q; val i = position.toInt(); return samples[i] + (samples[min(i+1,samples.lastIndex)] - samples[i]) * (position-i) }
        return Baseline(quantile(.25), quantile(.75), samples.size)
    }
}
data class TimelineHour(val start: Long, val end: Long, val steps: Long?, val hr: Double?, val sleepMinutes: Int)
object ActivityTimeline {
    fun hours(day: ConnectedDay, zone: ZoneId = ZoneId.systemDefault()): List<TimelineHour> {
        val date = LocalDate.parse(day.date)
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli(); val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val hr = day.chart.split(';').mapNotNull { val p=it.split(','); if (p.size != 2) null else p[0].toLongOrNull()?.let { t -> p[1].toDoubleOrNull()?.takeIf { it.isFinite() }?.let { t to it } } }
        return HourBucket.fill(start, end, HourBucket.decode(day.hourly)).map { bucket ->
            val samples=hr.filter { it.first >= bucket.start && it.first < bucket.end }.map { it.second }
            val overlap = if (day.sleepStart != null && day.sleepEnd != null) (min(bucket.end, day.sleepEnd)-max(bucket.start,day.sleepStart)).coerceAtLeast(0) / 60000 else 0
            TimelineHour(bucket.start,bucket.end,bucket.steps,samples.takeIf { it.isNotEmpty() }?.average(),overlap.toInt())
        }
    }
}
