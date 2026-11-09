package com.garmincodex.app

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(primaryKeys = ["date", "source"])
data class DailyRecord(val date: String, val source: String, val load: Double = 0.0, val recovery: Int? = null, val sleepMinutes: Int? = null, val updatedAt: Long = 0)
@Dao
interface DailyDao {
    @Query("SELECT * FROM DailyRecord ORDER BY date DESC, source LIMIT 28") fun observe(): Flow<List<DailyRecord>>
    @Query("SELECT * FROM DailyRecord WHERE date = :date AND source = :source") suspend fun get(date: String, source: String): DailyRecord?
    @Upsert suspend fun put(record: DailyRecord)
}
@Database(entities = [DailyRecord::class], version = 1, exportSchema = true)
abstract class PulseDatabase : RoomDatabase() { abstract fun days(): DailyDao }

/** Integration boundary: a licensed backend or FIT decoder normalizes data before import.
 * No Garmin credentials or proprietary protocol assumptions belong in the Android client. */
interface GarminSyncGateway { suspend fun fetchDays(): List<DailyRecord> }
class SyncImporter(private val dao: DailyDao) {
    suspend fun sync(gateway: GarminSyncGateway) {
        gateway.fetchDays().forEach {
            require(it.source == "GARMIN_SYNC" && it.load.isFinite() && it.load >= 0)
            require(it.recovery == null || it.recovery in 0..100)
            require(it.sleepMinutes == null || it.sleepMinutes in 0..1440)
            java.time.LocalDate.parse(it.date)
            dao.put(it)
        }
    }
}
