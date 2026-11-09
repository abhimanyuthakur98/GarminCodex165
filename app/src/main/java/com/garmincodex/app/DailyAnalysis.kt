package com.garmincodex.app

import java.time.*
import kotlin.math.*

/** Normalized records keep the analysis independently testable from Android APIs. */
data class HrPoint(val time: Long, val bpm: Int)
data class SleepSpan(val start: Long, val end: Long, val stage: Int)
data class SleepInput(val start: Long, val end: Long, val stages: List<SleepSpan>)
data class SleepSummary(val start: Long, val end: Long, val asleep: Int?, val deep: Int, val light: Int, val rem: Int, val awake: Int, val unknown: Int, val stages: List<SleepSpan>)
data class LoadSummary(val load: Double, val coveredMinutes: Int, val points: List<HrPoint>, val zoneMinutes: List<Double>)
object DailyAnalysis {
    // Stage values follow Health Connect: unknown=0, awake=1, sleeping=2, out of bed=3, light=4, deep=5, REM=6, awake in bed=7.
    fun sleep(input: SleepInput): SleepSummary {
        val spans = input.stages.sortedBy { it.start }.mapNotNull {
            val start = max(it.start, input.start); val end = min(it.end, input.end)
            if (end > start) SleepSpan(start, end, it.stage) else null
        }
        val boundaries = (listOf(input.start, input.end) + spans.flatMap { listOf(it.start, it.end) }).distinct().sorted()
        val totals = LongArray(8)
        val normalized = boundaries.zipWithNext().map { (start, end) ->
            val stage = spans.lastOrNull { it.start <= start && it.end >= end }?.stage?.takeIf { it in 0..7 } ?: 0
            totals[stage] += end - start
            SleepSpan(start, end, stage)
        }
        val knownAsleep = listOf(2, 4, 5, 6).sumOf { totals[it] }
        return SleepSummary(input.start, input.end, if (knownAsleep > 0) (knownAsleep / 60000).toInt() else null,
            (totals[5] / 60000).toInt(), (totals[4] / 60000).toInt(), (totals[6] / 60000).toInt(),
            ((totals[1] + totals[3] + totals[7]) / 60000).toInt(), (totals[0] / 60000).toInt(), normalized)
    }
    fun load(points: List<HrPoint>, resting: Int, maximum: Int): LoadSummary {
        require(maximum > resting)
        val clean = points.filter { it.bpm in 25..240 }.groupBy { it.time }.map { (t, samples) -> HrPoint(t, samples.map { it.bpm }.average().roundToInt()) }.sortedBy { it.time }
        val zones = DoubleArray(7)
        var covered = 0.0
        clean.zipWithNext().forEach { (a, b) ->
            val seconds = (b.time - a.time) / 1000.0
            // Garmin may export sparse all-day HR. Interpolate at most five minutes; never bridge larger gaps.
            if (seconds in 0.001..300.0) {
                val reserve = (((a.bpm + b.bpm) / 2.0 - resting) / (maximum - resting)).coerceIn(0.0, 1.0)
                val zone = when { reserve < .3 -> 0; reserve < .5 -> 1; reserve < .6 -> 2; reserve < .7 -> 3; reserve < .8 -> 4; reserve < .9 -> 5; else -> 6 }
                zones[zone] += seconds / 60; covered += seconds
            }
        }
        val weighted = zones.zip(listOf(0, 1, 2, 3, 5, 8, 12)).sumOf { (minutes, weight) -> minutes * weight }
        return LoadSummary(weighted, (covered / 60).toInt(), clean, zones.toList())
    }
    fun overnightHr(points: List<HrPoint>, sleep: SleepSummary?): Double? {
        if (sleep == null) return null
        val values = points.filter { it.time in sleep.start until sleep.end && it.bpm in 25..240 }.map { it.bpm }
        return if (values.size >= 5) values.average() else null
    }
    fun recovery(sleep: SleepSummary?, overnight: Double?, baseline: List<Double>, need: Int): Int? {
        if (sleep?.asleep == null || overnight == null || baseline.size < 3 || sleep.unknown > 30) return null
        val efficiency = sleep.asleep.toDouble() / ((sleep.end - sleep.start) / 60000.0).coerceAtLeast(1.0)
        val duration = (sleep.asleep.toDouble() / need).coerceIn(0.0, 1.0) * 100
        val cardio = (75 - 5 * (overnight - baseline.average())).coerceIn(0.0, 100.0)
        return (.4 * duration + .2 * efficiency * 100 + .4 * cardio).roundToInt().coerceIn(0, 100)
    }
    fun consistency(wakeTimes: List<Long>, zone: ZoneId): Int? {
        if (wakeTimes.size < 3) return null
        val minutes = wakeTimes.map { Instant.ofEpochMilli(it).atZone(zone).toLocalTime().toSecondOfDay() / 60.0 }
        // Circular distance prevents midnight-adjacent wake times from looking 24h apart.
        val anchor = minutes.last()
        val deviations = minutes.map { min(abs(it - anchor), 1440 - abs(it - anchor)) }
        return (100 - deviations.average() / 1.2).roundToInt().coerceIn(0, 100)
    }
}
