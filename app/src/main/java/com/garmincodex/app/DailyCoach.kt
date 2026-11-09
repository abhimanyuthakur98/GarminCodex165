package com.garmincodex.app

import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToInt

data class ReportValue(val current: Double?, val previous: Double?, val currentDays: Int, val previousDays: Int)
object DailyCoach {
    fun weekly(days: List<ConnectedDay>, end: LocalDate, value: (ConnectedDay) -> Double?): ReportValue {
        val unique = days.distinctBy { it.date }
        fun samples(from: LocalDate, until: LocalDate) = unique.filter { val d = LocalDate.parse(it.date); d >= from && d < until }.mapNotNull(value).filter { it.isFinite() }
        val current = samples(end.minusDays(7), end)
        val previous = samples(end.minusDays(14), end.minusDays(7))
        return ReportValue(current.takeIf { it.isNotEmpty() }?.average(), previous.takeIf { it.isNotEmpty() }?.average(), current.size, previous.size)
    }
    fun parseWake(text: String): LocalTime? = if (!Regex("[0-9]{2}:[0-9]{2}").matches(text)) null else runCatching { LocalTime.of(text.take(2).toInt(), text.takeLast(2).toInt()) }.getOrNull()
    fun bedtime(wakeMinute: Int, need: Int): LocalTime = LocalTime.of(wakeMinute / 60, wakeMinute % 60).minusMinutes(need.toLong())
    fun shortfall(days: List<ConnectedDay>, end: LocalDate, need: Int): Pair<Int, Int> {
        val nights = days.distinctBy { it.date }.filter { val date = LocalDate.parse(it.date); date <= end && date > end.minusDays(7) }.mapNotNull { it.asleep }
        return nights.sumOf { (need - it).coerceAtLeast(0) } to nights.size
    }
    fun briefing(day: ConnectedDay?, previous: ConnectedDay?, need: Int): String {
        val sleep = day?.asleep?.let { "You slept ${it / 60}h ${it % 60}m, ${kotlin.math.abs(it - need)} min ${if (it >= need) "above" else "below"} your goal." } ?: "Sleep has not arrived for this wake-up date yet."
        val recovery = day?.recovery?.let { score ->
            val change = previous?.recovery?.let { " (${if (score >= it) "+" else ""}${score-it} points versus yesterday)" } ?: ""
            "Recovery is $score%$change, estimated from sleep and overnight heart rate."
        } ?: "Recovery is still waiting for sufficient sleep and baseline data."
        val activity = previous?.steps?.let { "Yesterday you recorded ${it} steps${previous.takeIf { it.coveredMinutes > 0 }?.let { d -> " and ${String.format(java.util.Locale.US, "%.1f", Scores.strain(d.load))} strain" } ?: ""}." } ?: "Yesterday's activity is not available yet."
        return "$sleep\n\n$recovery\n\n$activity"
    }
}
