package com.garmincodex.app

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.*
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.work.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.*
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass
import kotlin.math.roundToInt

data class SyncState(val busy: Boolean = false, val message: String = "Connect Garmin through Health Connect", val lastSync: Long = 0, val granted: Set<String> = emptySet(), val background: Boolean = false)
class HealthConnectSync(private val context: Context, private val dao: DailyDao) {
    private val prefs = context.getSharedPreferences("health_sync", Context.MODE_PRIVATE)
    val status = MutableStateFlow(SyncState(lastSync = prefs.getLong("lastSync", 0)))
    val sleepNeed get() = prefs.getInt("sleepNeed", 480)
    val maximumHr get() = prefs.getInt("maximumHr", 190)
    val restingHr get() = prefs.getInt("restingHr", 55)
    private val lock = Mutex()
    private val origin = setOf(DataOrigin("com.garmin.android.apps.connectmobile"))
    fun availability() = HealthConnectClient.getSdkStatus(context)
    private fun client() = HealthConnectClient.getOrCreate(context)
    fun backgroundSupported() = availability() == HealthConnectClient.SDK_AVAILABLE && client().features.getFeatureStatus(HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_IN_BACKGROUND) == HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
    fun saveProfile(need: Int, maximum: Int, resting: Int) {
        require(need in 300..720 && maximum in 100..230 && resting in 30..100 && maximum > resting)
        prefs.edit().putInt("sleepNeed", need).putInt("maximumHr", maximum).putInt("restingHr", resting).apply()
    }
    suspend fun refreshPermissions() {
        if (availability() != HealthConnectClient.SDK_AVAILABLE) { status.value = status.value.copy(message = "Health Connect is unavailable. Install or update it first."); return }
        val granted = client().permissionController.getGrantedPermissions()
        status.value = status.value.copy(granted = granted, background = HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND in granted)
        if (status.value.background) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("garmin-health-sync", ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<HealthSyncWorker>(1, TimeUnit.HOURS).build())
        } else WorkManager.getInstance(context).cancelUniqueWork("garmin-health-sync")
    }
    suspend fun clear() = lock.withLock {
        try {
            if (availability() == HealthConnectClient.SDK_AVAILABLE) client().permissionController.revokeAllPermissions()
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { status.value = status.value.copy(message = "Could not revoke access. Open Health Connect settings to disconnect, then retry clearing."); return@withLock }
        dao.clearConnected(); prefs.edit().remove("lastSync").apply()
        WorkManager.getInstance(context).cancelUniqueWork("garmin-health-sync")
        // Revocation prevents the next app-open sync from restoring a cache the user just deleted.

        status.value = SyncState(message = "Imported data cleared and access disconnected. Garmin originals are unchanged.")
    }
    suspend fun sync(): Boolean = lock.withLock {
        try {
            refreshPermissions()
            val granted = status.value.granted
            if (granted.intersect(READ_PERMISSIONS).isEmpty()) { status.value = status.value.copy(message = "Allow Pulse 165 to read your health data to begin."); return@withLock false }
            status.value = status.value.copy(busy = true, message = "Reading Garmin history…")
            val client = client(); val now = Instant.now(); val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone); val first = today.minusDays(13)
            val start = first.minusDays(1).atStartOfDay(zone).toInstant()
            suspend fun <T : Record> read(type: KClass<T>): List<T> {
                if (HealthPermission.getReadPermission(type) !in granted) return emptyList()
                val records = mutableListOf<T>(); var token: String? = null
                do {
                    val response = client.readRecords(ReadRecordsRequest(type, TimeRangeFilter.between(start, now), dataOriginFilter = origin, pageSize = 1000, pageToken = token))
                    records += response.records; token = response.pageToken
                } while (!token.isNullOrEmpty())
                return records.distinctBy { it.metadata.id }
            }
            val hr = read(HeartRateRecord::class).flatMap { it.samples }.map { HrPoint(it.time.toEpochMilli(), it.beatsPerMinute.toInt()) }.filter { it.bpm in 25..240 }.groupBy { it.time }.map { (time, points) -> HrPoint(time, points.map { it.bpm }.average().roundToInt()) }
            val sleepRecords = read(SleepSessionRecord::class)
            val rests = read(RestingHeartRateRecord::class)
            val hrvs = read(HeartRateVariabilityRmssdRecord::class)
            val nights = sleepRecords.groupBy { it.endTime.atZone(zone).toLocalDate() }.mapValues { (_, sessions) ->
                // Main overnight sleep; naps are not added to recovery. Duplicate sessions cannot double count.
                val main = sessions.maxBy { Duration.between(it.startTime, it.endTime).toMinutes() }
                DailyAnalysis.sleep(SleepInput(main.startTime.toEpochMilli(), main.endTime.toEpochMilli(), main.stages.map { SleepSpan(it.startTime.toEpochMilli(), it.endTime.toEpochMilli(), it.stage) }))
            }
            val overnight = nights.mapValues { (_, night) -> DailyAnalysis.overnightHr(hr, night) }
            val result = (0L..13L).map { offset ->
                val date = first.plusDays(offset); val dayStart = date.atStartOfDay(zone).toInstant(); val dayEnd = minOf(date.plusDays(1).atStartOfDay(zone).toInstant(), now)
                val points = hr.filter { it.time >= dayStart.toEpochMilli() && it.time < dayEnd.toEpochMilli() }
                val load = DailyAnalysis.load(points, restingHr, maximumHr)
                val metrics = buildSet {
                    if (HealthPermission.getReadPermission(StepsRecord::class) in granted) add(StepsRecord.COUNT_TOTAL)
                    if (HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class) in granted) add(TotalCaloriesBurnedRecord.ENERGY_TOTAL)
                }
                val aggregate = if (metrics.isEmpty()) null else client.aggregate(AggregateRequest(metrics, TimeRangeFilter.between(dayStart, dayEnd), dataOriginFilter = origin))
                val night = nights[date]
                val baseline = overnight.filter { (d, value) -> d < date && d >= date.minusDays(7) && value != null }.values.filterNotNull()
                val wakeTimes = nights.filterKeys { it <= date && it >= date.minusDays(6) }.toSortedMap().values.map { it.end }
                fun inDay(time: Instant) = time >= dayStart && time < dayEnd
                val rest = rests.filter { inDay(it.time) }.map { it.beatsPerMinute.toDouble() }.average().takeIf { it.isFinite() }
                val hrv = hrvs.filter { inDay(it.time) }.map { it.heartRateVariabilityMillis }.average().takeIf { it.isFinite() }
                // Five-minute bins preserve actual time positions and gaps in the chart.
                val chart = load.points.groupBy { it.time / 300000L }.map { (_, p) -> HrPoint(p.first().time, p.map { it.bpm }.average().roundToInt()) }
                ConnectedDay(date.toString(), load.load, load.coveredMinutes, load.points.size,
                    load.points.minOfOrNull { it.bpm }, load.points.maxOfOrNull { it.bpm }, load.points.map { it.bpm }.average().takeIf { it.isFinite() }?.roundToInt(),
                    chart.joinToString(";") { "${it.time},${it.bpm}" }, load.zoneMinutes.joinToString(","),
                    aggregate?.get(StepsRecord.COUNT_TOTAL), aggregate?.get(TotalCaloriesBurnedRecord.ENERGY_TOTAL)?.inKilocalories,
                    night?.start, night?.end, night?.asleep, night?.deep ?: 0, night?.light ?: 0, night?.rem ?: 0, night?.awake ?: 0, night?.unknown ?: 0,
                    night?.stages?.joinToString(";") { "${it.start},${it.end},${it.stage}" } ?: "",
                    overnight[date], rest, hrv, DailyAnalysis.recovery(night, overnight[date], baseline, sleepNeed), baseline.size,
                    DailyAnalysis.consistency(wakeTimes, zone), now.toEpochMilli(), load.points.lastOrNull()?.time)
            }
            dao.replaceConnected(first.toString(), result)
            prefs.edit().putLong("lastSync", now.toEpochMilli()).apply()
            val hrCount = result.sumOf { it.hrCount }; val sleepCount = result.count { it.sleepStart != null }
            status.value = status.value.copy(busy = false, lastSync = now.toEpochMilli(), message = if (hrCount == 0 && sleepCount == 0 && result.none { it.steps != null })
                "No Garmin records found. Enable Garmin Connect’s write permissions in Health Connect, sync your watch in Garmin Connect, then retry. Earlier data may not be backfilled by Garmin."
                else "Imported $hrCount HR samples and $sleepCount nights from Garmin Connect. ${granted.intersect(READ_PERMISSIONS).size}/${READ_PERMISSIONS.size} read permissions granted.")
            true
        } catch (e: CancellationException) { status.value = status.value.copy(busy = false); throw e }
        catch (e: Exception) { status.value = status.value.copy(busy = false, message = if (e is SecurityException) "Health access changed. Grant permissions and sync again." else "Sync interrupted (${e.javaClass.simpleName}). Keep the app open and retry. Your previous import is retained."); false }
    }
    companion object {
        val READ_PERMISSIONS = setOf(HeartRateRecord::class, SleepSessionRecord::class, StepsRecord::class, TotalCaloriesBurnedRecord::class, RestingHeartRateRecord::class, HeartRateVariabilityRmssdRecord::class).map { HealthPermission.getReadPermission(it) }.toSet()
    }
}
class HealthSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params), KoinComponent {
    private val sync: HealthConnectSync by inject()
    override suspend fun doWork(): Result {
        return try {
            sync.refreshPermissions()
            if (!sync.status.value.background) Result.success()
            else if (sync.sync()) Result.success() else if (runAttemptCount < 3) Result.retry() else Result.failure()
        } catch (e: CancellationException) { throw e } catch (_: Exception) { if (runAttemptCount < 3) Result.retry() else Result.failure() }
    }
}
