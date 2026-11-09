package com.garmincodex.app

import java.time.LocalDate
import kotlin.math.*

data class PersonalGoals(val steps: Int = 8000, val activeCalories: Int = 500, val age: Int? = null, val wakeMinute: Int = 420) {
    fun validate() { require(wakeMinute in 0..1439 && steps in 1000..100000 && activeCalories in 100..5000 && (age == null || age in 18..100)) }
}
data class HourBucket(val start: Long, val end: Long, val steps: Long?, val active: Double?, val total: Double?) {
    fun encode() = "$start,$end,${steps ?: ""},${active ?: ""},${total ?: ""}"
    companion object {
        fun fill(start: Long, end: Long, records: List<HourBucket>): List<HourBucket> {
            require(end >= start)
            val indexed = records.associateBy { it.start }
            return generateSequence(start) { it + 3600000 }.takeWhile { it < end }.map { t ->
                indexed[t]?.copy(end = minOf(t + 3600000, end)) ?: HourBucket(t, minOf(t + 3600000, end), null, null, null)
            }.toList()
        }
        fun decode(text: String): List<HourBucket> = text.split(';').mapNotNull { row ->
            val p = row.split(',')
            if (p.size != 5) null else runCatching { HourBucket(p[0].toLong(), p[1].toLong(), p[2].toLongOrNull(), p[3].toDoubleOrNull(), p[4].toDoubleOrNull()) }.getOrNull()
        }.sortedBy { it.start }
    }
}
object EnergyBreakdown {
    // This is non-active expenditure, not a measured basal metabolic rate.
    fun nonActive(total: Double?, active: Double?): Double? = if (total != null && active != null && total.isFinite() && active.isFinite() && total >= active && active >= 0) total - active else null
}
data class FitnessFactor(val name: String, val score: Double, val detail: String)
data class FitnessAgeResult(val age: Double?, val index: Int?, val days: Int, val factors: List<FitnessFactor>, val reason: String)
object FitnessAgeModel {
    /** An explicit wellness heuristic, not an age-normed physiological model or longevity prediction. */
    fun calculate(days: List<ConnectedDay>, age: Int?, today: LocalDate = LocalDate.now()): FitnessAgeResult {
        require(age == null || age in 18..100)
        val valid = days.filter { runCatching { val date = LocalDate.parse(it.date); date < today && date >= today.minusDays(14) }.getOrDefault(false) && it.steps != null && it.asleep != null && it.recovery != null && it.coveredMinutes >= 120 }
            .distinctBy { it.date }.sortedByDescending { it.date }.take(7)
        if (age == null) return FitnessAgeResult(null, null, valid.size, emptyList(), "Add your chronological age to start.")
        if (valid.size < 7) return FitnessAgeResult(null, null, valid.size, emptyList(), "${valid.size}/7 complete days. Needs steps, sleep, recovery and at least 2h of HR coverage per day.")
        val steps = valid.map { it.steps!!.toDouble() }.average()
        val sleep = valid.map { it.asleep!!.toDouble() }.average()
        val recovery = valid.map { it.recovery!!.toDouble() }.average()
        val cardio = valid.map { d -> d.zones.split(',').mapNotNull { it.toDoubleOrNull() }.drop(2).sum() }.average()
        // Fixed reference targets: changing a personal goal must never manipulate Fitness Age.
        val factors = listOf(
            FitnessFactor("Daily movement", (steps / 8000 * 100).coerceIn(0.0, 100.0), "${steps.roundToInt()} steps/day · reference 8,000"),
            FitnessFactor("Sleep duration", (sleep / 480 * 100).coerceIn(0.0, 100.0), "${sleep.roundToInt()} min/night · reference 480"),
            FitnessFactor("Recovery", recovery.coerceIn(0.0, 100.0), "${recovery.roundToInt()}% average Pulse recovery"),
            FitnessFactor("Cardio activity", (cardio / 30 * 100).coerceIn(0.0, 100.0), "${cardio.roundToInt()} min/day above 50% HR reserve")
        )
        val index = factors.map { it.score }.average()
        val offset = ((70 - index) / 6).coerceIn(-5.0, 5.0)
        return FitnessAgeResult((age + offset).coerceIn(18.0, 105.0), index.roundToInt(), valid.size, factors, "7 complete days within the last 14; today excluded.")
    }
}
