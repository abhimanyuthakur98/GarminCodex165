package com.garmincodex.app

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(primaryKeys = ["date", "source"])
data class DailyRecord(val date: String, val source: String, val load: Double = 0.0, val recovery: Int? = null, val sleepMinutes: Int? = null, val updatedAt: Long = 0)
@Entity
data class ConnectedDay(
    @PrimaryKey val date: String,
    val load: Double = 0.0, val coveredMinutes: Int = 0, val hrCount: Int = 0,
    val hrMin: Int? = null, val hrMax: Int? = null, val hrAverage: Int? = null,
    val chart: String = "", val zones: String = "",
    val steps: Long? = null, val calories: Double? = null,
    val sleepStart: Long? = null, val sleepEnd: Long? = null, val asleep: Int? = null,
    val deep: Int = 0, val light: Int = 0, val rem: Int = 0, val awake: Int = 0, val unknownSleep: Int = 0,
    val stages: String = "", val overnightHr: Double? = null, val restingHr: Double? = null, val hrv: Double? = null,
    val recovery: Int? = null, val baselineNights: Int = 0, val consistency: Int? = null,
    val updatedAt: Long = 0, val lastSample: Long? = null,
    val activeCalories: Double? = null, val basalCalories: Double? = null,
    @ColumnInfo(defaultValue = "''") val hourly: String = ""
)
@Dao
interface DailyDao {
    @Query("SELECT * FROM JournalEntry ORDER BY date DESC") fun journal(): Flow<List<JournalEntry>>
    @Query("SELECT * FROM HabitTag ORDER BY name") fun habits(): Flow<List<HabitTag>>
    @Upsert suspend fun putJournal(entry: JournalEntry)
    @Upsert suspend fun putHabit(tag: HabitTag)
    @Query("DELETE FROM JournalEntry WHERE date=:date") suspend fun deleteJournal(date: String)
    @Query("DELETE FROM JournalEntry") suspend fun clearJournal()
    @Query("DELETE FROM HabitTag") suspend fun clearHabits()
    @Transaction suspend fun clearPersonalJournal() { clearJournal(); clearHabits() }
    @Query("SELECT * FROM Workout ORDER BY start DESC") fun workouts(): Flow<List<Workout>>
    @Query("SELECT * FROM WorkoutCheckIn") fun checkIns(): Flow<List<WorkoutCheckIn>>
    @Upsert suspend fun putWorkouts(workouts: List<Workout>)
    @Upsert suspend fun putCheckIn(checkIn: WorkoutCheckIn)
    @Query("DELETE FROM Workout WHERE source = 'HEALTH_CONNECT' AND start >= :since") suspend fun deleteRecentWorkouts(since: Long)
    @Query("DELETE FROM Workout WHERE id = :id") suspend fun deleteWorkout(id: String)
    @Query("DELETE FROM WorkoutCheckIn WHERE workoutId NOT IN (SELECT id FROM Workout)") suspend fun clearOrphanCheckIns()
    @Query("DELETE FROM Workout") suspend fun clearWorkouts()
    @Transaction suspend fun replaceWorkouts(since: Long, workouts: List<Workout>) { deleteRecentWorkouts(since); putWorkouts(workouts); clearOrphanCheckIns() }
    @Transaction suspend fun removeWorkout(id: String) { deleteWorkout(id); clearOrphanCheckIns() }
    @Transaction suspend fun replaceHealthImport(since: String, sinceTime: Long, days: List<ConnectedDay>, workouts: List<Workout>?) { replaceConnected(since, days); if(workouts != null) replaceWorkouts(sinceTime, workouts) }
    @Transaction suspend fun clearAllImports() { clearConnected(); clearWorkouts(); clearOrphanCheckIns() }
    @Query("SELECT * FROM DailyRecord ORDER BY date DESC, source LIMIT 28") fun observe(): Flow<List<DailyRecord>>
    @Query("SELECT * FROM DailyRecord WHERE date = :date AND source = :source") suspend fun get(date: String, source: String): DailyRecord?
    @Upsert suspend fun put(record: DailyRecord)
    @Query("SELECT * FROM ConnectedDay ORDER BY date DESC") fun connected(): Flow<List<ConnectedDay>>
    @Query("DELETE FROM ConnectedDay WHERE date >= :since") suspend fun deleteConnectedSince(since: String)
    @Query("DELETE FROM ConnectedDay") suspend fun clearConnected()
    @Upsert suspend fun putConnected(days: List<ConnectedDay>)
    @Transaction suspend fun replaceConnected(since: String, days: List<ConnectedDay>) { deleteConnectedSince(since); putConnected(days) }
}
@Database(entities = [DailyRecord::class, ConnectedDay::class, Workout::class, WorkoutCheckIn::class, JournalEntry::class, HabitTag::class], version = 5, exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3), AutoMigration(from = 3, to = 4), AutoMigration(from = 4, to = 5)])
abstract class PulseDatabase : RoomDatabase() { abstract fun days(): DailyDao }

interface GarminSyncGateway { suspend fun fetchDays(): List<DailyRecord> }
class SyncImporter(private val dao: DailyDao) {
    suspend fun sync(gateway: GarminSyncGateway) {
        gateway.fetchDays().forEach {
            require(it.source == "GARMIN_SYNC" && it.load.isFinite() && it.load >= 0)
            require(it.recovery == null || it.recovery in 0..100)
            require(it.sleepMinutes == null || it.sleepMinutes in 0..1440)
            java.time.LocalDate.parse(it.date); dao.put(it)
        }
    }
}
