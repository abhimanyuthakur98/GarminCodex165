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
    const val REQUIRED_DAYS = 14
    const val WINDOW_DAYS = 28L
    const val EXPLANATION = "Model v2 uses all qualifying days in the preceding 28 days, excluding today. It needs 14 days, including 4 in the last week. Each day needs steps, known sleep and at least 8 hours of heart-rate coverage. Movement contributes 30%, sleep 20%, activity consistency 10%, and cardio 40%. Daily movement is capped at 8,000 steps; sleep at 7 hours. Consistency counts days with 6,000 steps or 20 equivalent cardio minutes. Cardio uses recorded minutes at 50–70% HR reserve plus twice the minutes at 70% or above, against a 150-minute weekly reference. These are model settings, not personal prescriptions. Index 70 maps to your entered age; 10 points changes the illustration by one year, capped at ±5 years and minimum age 18. It is not a validated physiological age, VO₂ max estimate or lifespan prediction. Personal goals do not change the references; your HR profile affects zones."
    private fun zones(day: ConnectedDay): List<Double>? {
        val parts = day.zones.split(',')
        if (parts.size != 7) return null
        val values = parts.map { it.toDoubleOrNull() ?: return null }
        return values.takeIf { it.all { n -> n.isFinite() && n >= 0 } && it.sum() <= day.coveredMinutes + 1.0 }
    }
    private fun cardio(day: ConnectedDay): Double {
        val z = zones(day)!!
        return z[2] + z[3] + 2 * z.drop(4).sum()
    }
    fun calculate(days: List<ConnectedDay>, age: Int?, today: LocalDate = LocalDate.now()): FitnessAgeResult {
        require(age == null || age in 18..100)
        // Choose the newest snapshot before validating; stale duplicates cannot rescue invalid data.
        val valid = days.groupBy { it.date }.values.map { rows -> rows.maxBy { it.updatedAt } }.filter {
            runCatching { val date = LocalDate.parse(it.date); date < today && date >= today.minusDays(WINDOW_DAYS) }.getOrDefault(false) &&
                it.steps != null && it.steps >= 0 && it.asleep != null && it.asleep in 1..960 &&
                it.unknownSleep in 0..30 && it.coveredMinutes in 480..1500 && zones(it) != null
        }.sortedBy { it.date }
        val recent = valid.count { LocalDate.parse(it.date) >= today.minusDays(7) }
        if (age == null) return FitnessAgeResult(null, null, valid.size, emptyList(), "Add your chronological age to start.")
        if (valid.size < REQUIRED_DAYS || recent < 4) return FitnessAgeResult(null, null, valid.size, emptyList(), "${valid.size}/14 qualifying days; $recent/4 needed in the last week. Needs steps, known sleep and 8h of HR coverage per day.")
        val steps = valid.map { it.steps!!.toDouble() }.average()
        val sleep = valid.map { it.asleep!!.toDouble() }.average()
        val cardioMinutes = valid.map(::cardio).average()
        val consistent = valid.count { it.steps!! >= 6000 || cardio(it) >= 20 }
        val factors = listOf(
            FitnessFactor("Daily movement", valid.map { (it.steps!! / 8000.0 * 100).coerceAtMost(100.0) }.average(), "${steps.roundToInt()} steps/day · daily reference 8,000 · weight 30%"),
            FitnessFactor("Sleep duration", valid.map { (it.asleep!! / 420.0 * 100).coerceAtMost(100.0) }.average(), "${sleep.roundToInt()} min/night · reference 7h · weight 20%"),
            FitnessFactor("Activity consistency", consistent * 100.0 / valid.size, "$consistent/${valid.size} days with 6,000 steps or 20 equivalent cardio minutes · weight 10%"),
            FitnessFactor("Cardio activity", (cardioMinutes * 7 / 150 * 100).coerceIn(0.0, 100.0), "${(cardioMinutes * 7).roundToInt()} equivalent min/week on observed days · reference 150 · weight 40%")
        )
        val index = factors.zip(listOf(.3, .2, .1, .4)).sumOf { (factor, weight) -> factor.score * weight }
        val offset = ((70 - index) / 10).coerceIn(-5.0, 5.0)
        return FitnessAgeResult((age + offset).coerceIn(18.0, 105.0), index.roundToInt(), valid.size, factors,
            "${valid.size}/28 qualifying days · $recent in the last week · model v2")
    }
}
