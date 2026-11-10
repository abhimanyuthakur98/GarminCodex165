package com.garmincodex.app

import androidx.room.*
import com.garmin.fit.*
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.util.Locale

@Entity data class Workout(
    @PrimaryKey val id: String, val source: String, val type: String, val title: String,
    val start: Long, val end: Long, val chart: String, val calories: Double? = null
)
@Entity data class WorkoutCheckIn(@PrimaryKey val workoutId: String, val effort: Int, val soreness: Int)
object WorkoutAnalysis {
    fun points(workout: Workout): List<HrPoint> = workout.chart.split(';').mapNotNull { row -> val p=row.split(','); if(p.size!=2) null else p[0].toLongOrNull()?.let { t -> p[1].toIntOrNull()?.takeIf { it in 25..240 }?.let { HrPoint(t,it) } } }.filter { it.time >= workout.start && it.time < workout.end }.distinctBy { it.time }.sortedBy { it.time }
    fun previous(workout: Workout, all: List<Workout>): Workout? = all.filter { it.start < workout.start && it.type == workout.type && it.source == workout.source }.maxByOrNull { it.start }
    fun title(type: String) = type.lowercase(Locale.US).replace('_',' ').replaceFirstChar { it.titlecase(Locale.US) }
}
object FitWorkoutReader {
    const val MAX_BYTES = 10 * 1024 * 1024
    fun read(bytes: ByteArray): List<Workout> {
        require(bytes.size in 14..MAX_BYTES) { "Choose a FIT activity file under 10 MB." }
        val decoder=Decode()
        require(decoder.checkFileIntegrity(ByteArrayInputStream(bytes))) { "FIT file is incomplete or corrupt." }
        val sessions=mutableListOf<SessionMesg>(); val points=mutableListOf<HrPoint>()
        val broadcaster=MesgBroadcaster(decoder)
        broadcaster.addListener(SessionMesgListener { session -> require(sessions.size < 100); sessions.add(session) })
        broadcaster.addListener(RecordMesgListener { record ->
            val time=record.timestamp?.date?.time; val hr=record.heartRate?.toInt()
            if(time != null && hr != null && hr in 25..240) { require(points.size < 200000) { "Too many HR samples." }; points.add(HrPoint(time,hr)) }
        })
        decoder.read(ByteArrayInputStream(bytes),broadcaster,broadcaster)
        val hash=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        return sessions.mapIndexedNotNull { index,s ->
            val start=s.startTime?.date?.time ?: return@mapIndexedNotNull null
            val end=s.timestamp?.date?.time ?: return@mapIndexedNotNull null
            if(end <= start) return@mapIndexedNotNull null
            val type=s.sport?.toString() ?: "OTHER"
            Workout("FIT:$hash:$index","FIT",type,WorkoutAnalysis.title(type),start,end,
                points.filter { it.time >= start && it.time < end }.distinctBy { it.time }.sortedBy { it.time }.joinToString(";") { "${it.time},${it.bpm}" }, s.totalCalories?.toDouble())
        }.also { require(it.isNotEmpty()) { "No complete activity sessions in this FIT file." } }
    }
}
