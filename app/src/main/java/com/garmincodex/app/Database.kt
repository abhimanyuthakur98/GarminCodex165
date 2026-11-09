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
    val updatedAt: Long = 0, val lastSample: Long? = null
)
@Dao
interface DailyDao {
    @Query("SELECT * FROM DailyRecord ORDER BY date DESC, source LIMIT 28") fun observe(): Flow<List<DailyRecord>>
    @Query("SELECT * FROM DailyRecord WHERE date = :date AND source = :source") suspend fun get(date: String, source: String): DailyRecord?
    @Upsert suspend fun put(record: DailyRecord)
    @Query("SELECT * FROM ConnectedDay ORDER BY date DESC") fun connected(): Flow<List<ConnectedDay>>
    @Query("DELETE FROM ConnectedDay WHERE date >= :since") suspend fun deleteConnectedSince(since: String)
    @Query("DELETE FROM ConnectedDay") suspend fun clearConnected()
    @Upsert suspend fun putConnected(days: List<ConnectedDay>)
    @Transaction suspend fun replaceConnected(since: String, days: List<ConnectedDay>) { deleteConnectedSince(since); putConnected(days) }
}
@Database(entities = [DailyRecord::class, ConnectedDay::class], version = 2, exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)])
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
