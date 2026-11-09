package com.garmincodex.app

import kotlin.math.*

data class SensorSample(val time: Long, val hr: Int, val rrMs: List<Double> = emptyList(), val steps: Int? = null, val acceleration: Double? = null)
data class Night(val hrv: Double, val baselineHrv: Double, val rhr: Int, val baselineRhr: Int, val deep: Int, val light: Int, val rem: Int, val awake: Int, val need: Int, val consistency: Int, val bodyBattery: Int) {
    val asleep get() = deep + light + rem
    val efficiency get() = asleep.toDouble() / (asleep + awake).coerceAtLeast(1)
    val performance get() = (100.0 * asleep / need.coerceAtLeast(1)).roundToInt().coerceIn(0, 100)
}
object Scores {
    fun load(hr: Int, seconds: Double, resting: Int = 55, maximum: Int = 190): Double {
        require(maximum > resting)
        val reserve = ((hr - resting).toDouble() / (maximum - resting)).coerceIn(0.0, 1.0)
        val weight = when { reserve < .3 -> 0.0; reserve < .5 -> 1.0; reserve < .6 -> 2.0; reserve < .7 -> 3.0; reserve < .8 -> 5.0; reserve < .9 -> 8.0; else -> 12.0 }
        return seconds.coerceIn(0.0, 10.0) / 60.0 * weight
    }
    fun strain(load: Double) = (21 * (1 - exp(-load.coerceAtLeast(0.0) / 180))).coerceIn(0.0, 21.0)
    fun recovery(n: Night): Int {
        require(n.baselineHrv > 0 && n.hrv > 0)
        val hrv = (70 + 100 * ln(n.hrv / n.baselineHrv)).coerceIn(0.0, 100.0)
        val rhr = (80 - 5.0 * (n.rhr - n.baselineRhr)).coerceIn(0.0, 100.0)
        return (.45 * hrv + .25 * rhr + .2 * n.efficiency * 100 + .1 * n.bodyBattery.coerceIn(0, 100)).roundToInt().coerceIn(0, 100)
    }
    fun tier(score: Int) = when { score < 34 -> "LOW"; score < 67 -> "MODERATE"; else -> "READY" }
}

/** Bluetooth SIG Heart Rate Measurement, including uint16 HR and optional RR intervals. */
object HeartRatePacket {
    fun parse(bytes: ByteArray, time: Long): SensorSample? {
        if (bytes.size < 2) return null
        fun u(i: Int) = bytes[i].toInt() and 255
        val flags = u(0)
        var p = 1
        val wide = flags and 1 != 0
        if (wide && bytes.size < 3) return null
        val hr = if (wide) u(p++) + (u(p++) shl 8) else u(p++)
        if (flags and 6 == 4) return null // contact supported but not detected
        if (flags and 8 != 0) p += 2
        if (p > bytes.size || (flags and 16 != 0 && (bytes.size - p) % 2 != 0)) return null
        val rr = mutableListOf<Double>()
        if (flags and 16 != 0) while (p + 1 < bytes.size) rr += (u(p++) + (u(p++) shl 8)) * 1000.0 / 1024
        return if (hr in 25..240) SensorSample(time, hr, rr) else null
    }
}
