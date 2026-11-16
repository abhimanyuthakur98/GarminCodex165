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

data class SyncState(val busy: Boolean = false, val message: String = "Connect your device through Health Connect", val lastSync: Long = 0, val granted: Set<String> = emptySet(), val background: Boolean = false, val diagnostics: String = "", val sources: List<String> = emptyList(), val stepSource:String="", val stepComparisons:Map<String,Long> = emptyMap())
class HealthConnectSync(private val context: Context, private val dao: DailyDao) {
    private val prefs = context.getSharedPreferences("health_sync", Context.MODE_PRIVATE)
    val status = MutableStateFlow(SyncState(lastSync = prefs.getLong("lastSync", 0), diagnostics = prefs.getString("diagnostics", "") ?: "", sources = prefs.getStringSet("sources", emptySet())!!.sorted(), stepSource=prefs.getString("stepSource", "") ?: ""))
    fun saveStepSource(pkg:String) { prefs.edit().putString("stepSource",pkg).apply();status.value=status.value.copy(stepSource=pkg) }
    val appearance=MutableStateFlow(PulseStyle.fromId(prefs.getString("appearance",null)))
    fun saveAppearance(value:PulseStyle) { prefs.edit().putString("appearance",value.name).apply();appearance.value=value }
    val goals = MutableStateFlow(PersonalGoals(prefs.getInt("stepGoal", 8000), prefs.getInt("activeGoal", 500), prefs.getInt("age", 0).takeIf { it in 18..100 }, prefs.getInt("wakeMinute", 420)))
    fun saveGoals(value: PersonalGoals) {
        value.validate()
        prefs.edit().putInt("stepGoal", value.steps).putInt("activeGoal", value.activeCalories).putInt("age", value.age ?: 0).putInt("wakeMinute", value.wakeMinute).apply()
        goals.value = value
    }
    val dashboardPreferences = MutableStateFlow(DashboardPreferences(
        prefs.getString("cardOrder", null)?.split(',') ?: DashboardPreferences.CARDS,
        prefs.getString("hiddenCards", "")!!.split(',').filter { it.isNotBlank() }.toSet(),
        prefs.getInt("startPage", 0)).normalized())
    fun saveDashboard(value: DashboardPreferences) {
        val normalized = value.normalized()
        prefs.edit().putString("cardOrder", normalized.order.joinToString(",")).putString("hiddenCards", normalized.hidden.joinToString(",")).putInt("startPage", normalized.startPage).apply()
        dashboardPreferences.value = normalized
    }
    val distanceMiles = MutableStateFlow(prefs.getBoolean("distanceMiles", false))
    fun saveDistanceMiles(value:Boolean) { prefs.edit().putBoolean("distanceMiles",value).apply();distanceMiles.value=value }
    val sleepNeed get() = prefs.getInt("sleepNeed", 480)
    val maximumHr get() = prefs.getInt("maximumHr", 190)
    val restingHr get() = prefs.getInt("restingHr", 55)
    private val lock = Mutex()
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
    suspend fun importFit(readBytes: () -> ByteArray): Int = lock.withLock {
        val records = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { FitWorkoutReader.read(readBytes()) }
        dao.putWorkouts(records); records.size
    }
    suspend fun clear() = lock.withLock {
        try {
            if (availability() == HealthConnectClient.SDK_AVAILABLE) client().permissionController.revokeAllPermissions()
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { status.value = status.value.copy(message = "Could not revoke access. Open Health Connect settings to disconnect, then retry clearing."); return@withLock }
        dao.clearAllImports(); prefs.edit().remove("lastSync").remove("diagnostics").remove("sources").apply(); PulseWidget.refresh(context)
        WorkManager.getInstance(context).cancelUniqueWork("garmin-health-sync")
        // Revocation prevents the next app-open sync from restoring a cache the user just deleted.

        status.value = SyncState(message = "Imported data cleared and access disconnected. Source originals are unchanged.")
    }
    suspend fun sync(): Boolean = lock.withLock {
        val guard=ImportGuard()
        try {
            refreshPermissions()
            val granted = status.value.granted
            if (granted.intersect(READ_PERMISSIONS).isEmpty()) { status.value = status.value.copy(message = "Allow Pulse 165 to read your health data to begin."); return@withLock false }
            status.value = status.value.copy(busy = true, message = "Reading Health Connect history…")
            val client = client(); val now = Instant.now(); val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone); val first = today.minusDays(28)
            val selectedStepSource = status.value.stepSource
            val stepOrigins = selectedStepSource.takeIf { it.isNotEmpty() }?.let { setOf(DataOrigin(it)) } ?: emptySet()
            val start = first.minusDays(1).atStartOfDay(zone).toInstant()
            suspend fun <T : Record> read(type: KClass<T>): List<T> {
                if (HealthPermission.getReadPermission(type) !in granted) return emptyList()
                return guard.query(type.simpleName ?: "records", emptyList<T>()) {
                val records = mutableListOf<T>(); var token: String? = null
                do {
                    val response = client.readRecords(ReadRecordsRequest(type, TimeRangeFilter.between(start, now), pageSize = 1000, pageToken = token))
                    records += response.records; token = response.pageToken
                } while (!token.isNullOrEmpty())
                records.distinctBy { it.metadata.id }
                }
            }
            val stepRecords = read(StepsRecord::class)
            val stepPackages = stepRecords.map { it.metadata.dataOrigin.packageName }.distinct().sorted()
            val stepComparisons = mutableMapOf<String,Long>()
            if(HealthPermission.getReadPermission(StepsRecord::class) in granted) {
                val bounds = StepWindows.bounds(today,zone)
                for(pkg in stepPackages) {
                    val value = guard.query("step source comparison", null as Long?) { client.aggregate(AggregateRequest(setOf(StepsRecord.COUNT_TOTAL),TimeRangeFilter.between(bounds.first,bounds.second),dataOriginFilter=setOf(DataOrigin(pkg))))[StepsRecord.COUNT_TOTAL] }
                    if(value!=null) stepComparisons[pkg]=value
                }
            }
            val sourcePackages = sortedSetOf<String>().apply { addAll(stepPackages) }
            val hrRecords = read(HeartRateRecord::class)
            sourcePackages += hrRecords.map { it.metadata.dataOrigin.packageName }
            val hr = hrRecords.flatMap { record -> record.samples.map { record.metadata.dataOrigin.packageName to it } }
                .groupBy { it.second.time.atZone(zone).toLocalDate() }.values.flatMap { samples ->
                    val sources = samples.groupBy { it.first }
                    val chosen = sources.keys.sorted().maxByOrNull { name ->
                        DailyAnalysis.load(sources.getValue(name).map { HrPoint(it.second.time.toEpochMilli(), it.second.beatsPerMinute.toInt()) }, restingHr, maximumHr).coveredMinutes
                    }
                    chosen?.let { sources.getValue(it) }.orEmpty().map { it.second }
                }.map { HrPoint(it.time.toEpochMilli(), it.beatsPerMinute.toInt()) }.filter { it.bpm in 25..240 }.groupBy { it.time }.map { (time, points) -> HrPoint(time, points.map { it.bpm }.average().roundToInt()) }
            val allExercises = read(ExerciseSessionRecord::class)
            sourcePackages += allExercises.map { it.metadata.dataOrigin.packageName }
            val exerciseRecords = SourceSelection.workouts(allExercises, { it.metadata.dataOrigin.packageName }, { it.startTime.toEpochMilli() }, { it.endTime.toEpochMilli() }, { it.exerciseType })
            val sleepRecords = read(SleepSessionRecord::class)
            sourcePackages += sleepRecords.map { it.metadata.dataOrigin.packageName }
            val rests = read(RestingHeartRateRecord::class)
            val hrvs = read(HeartRateVariabilityRmssdRecord::class)
            val nights = sleepRecords.groupBy { it.endTime.atZone(zone).toLocalDate() }.mapValues { (_, sessions) ->
                // Main overnight sleep; naps are not added to recovery. Duplicate sessions cannot double count.
                val main = sessions.sortedWith(compareByDescending<SleepSessionRecord> { Duration.between(it.startTime, it.endTime).toMinutes() }.thenBy { it.metadata.dataOrigin.packageName }).first()
                DailyAnalysis.sleep(SleepInput(main.startTime.toEpochMilli(), main.endTime.toEpochMilli(), main.stages.map { SleepSpan(it.startTime.toEpochMilli(), it.endTime.toEpochMilli(), it.stage) }))
            }
            val overnight = nights.mapValues { (_, night) -> DailyAnalysis.overnightHr(hr, night) }
            val result = (0L..28L).map { offset ->
                val date = first.plusDays(offset); val dayStart = date.atStartOfDay(zone).toInstant(); val dayEnd = minOf(date.plusDays(1).atStartOfDay(zone).toInstant(), now)
                val previous=dao.day(date.toString())
                val stepBounds = StepWindows.bounds(date,zone)
                val dailySteps = if(HealthPermission.getReadPermission(StepsRecord::class) !in granted) null else guard.query("daily steps",previous?.steps) { client.aggregate(AggregateRequest(setOf(StepsRecord.COUNT_TOTAL),TimeRangeFilter.between(stepBounds.first,stepBounds.second),dataOriginFilter=stepOrigins))[StepsRecord.COUNT_TOTAL] }
                val coarseSteps = stepRecords.any { (selectedStepSource.isEmpty() || it.metadata.dataOrigin.packageName==selectedStepSource) && it.startTime < stepBounds.second && it.endTime > stepBounds.first && StepWindows.coarse(it.startTime,it.endTime) }
                val points = hr.filter { it.time >= dayStart.toEpochMilli() && it.time < dayEnd.toEpochMilli() }
                val load = DailyAnalysis.load(points, restingHr, maximumHr)
                suspend fun <T:Any> metric(name:String, type:KClass<out Record>, value:androidx.health.connect.client.aggregate.AggregateMetric<T>, fallback:T?):T? {
                    if(HealthPermission.getReadPermission(type) !in granted) return null
                    return guard.query(name,fallback) {
                        client.aggregate(AggregateRequest(setOf(value),TimeRangeFilter.between(dayStart,dayEnd))).also { sourcePackages += it.dataOrigins.map { origin -> origin.packageName } }[value]
                    }
                }
                val active=metric("active calories",ActiveCaloriesBurnedRecord::class,ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL,previous?.activeCalories?.let { androidx.health.connect.client.units.Energy.kilocalories(it) })?.inKilocalories
                val total=metric("total calories",TotalCaloriesBurnedRecord::class,TotalCaloriesBurnedRecord.ENERGY_TOTAL,previous?.calories?.let { androidx.health.connect.client.units.Energy.kilocalories(it) })?.inKilocalories
                val basal=metric("basal energy",BasalMetabolicRateRecord::class,BasalMetabolicRateRecord.BASAL_CALORIES_TOTAL,previous?.basalCalories?.let { androidx.health.connect.client.units.Energy.kilocalories(it) })?.inKilocalories
                suspend fun <T:Any> hours(name:String,type:KClass<out Record>,value:androidx.health.connect.client.aggregate.AggregateMetric<T>,origins:Set<DataOrigin> = emptySet()):Map<Long,T?> {
                    if(HealthPermission.getReadPermission(type) !in granted || dayEnd<=dayStart) return emptyMap()
                    return guard.query(name,emptyMap()) {
                        client.aggregateGroupByDuration(AggregateGroupByDurationRequest(setOf(value),TimeRangeFilter.between(dayStart,dayEnd),Duration.ofHours(1),dataOriginFilter=origins))
                            .associate { it.startTime.toEpochMilli() to it.result[value] }
                    }
                }
                val stepHours=if(coarseSteps) emptyMap() else hours("hourly steps",StepsRecord::class,StepsRecord.COUNT_TOTAL,stepOrigins)
                val activeHours=hours("hourly active calories",ActiveCaloriesBurnedRecord::class,ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)
                val totalHours=hours("hourly total calories",TotalCaloriesBurnedRecord::class,TotalCaloriesBurnedRecord.ENERGY_TOTAL)
                val hourly=HourBucket.fill(dayStart.toEpochMilli(),dayEnd.toEpochMilli(),emptyList()).map { it.copy(steps=stepHours[it.start],active=activeHours[it.start]?.inKilocalories,total=totalHours[it.start]?.inKilocalories) }
                val night = nights[date]
                val baseline = overnight.filter { (d, value) -> d < date && d >= date.minusDays(7) && value != null }.values.filterNotNull()
                val wakeTimes = nights.filterKeys { it <= date && it >= date.minusDays(6) }.toSortedMap().values.map { it.end }
                fun inDay(time: Instant) = time >= dayStart && time < dayEnd
                val rest = SourceSelection.primary(rests.filter { inDay(it.time) }, { it.metadata.dataOrigin.packageName }, { 1L }).map { it.beatsPerMinute.toDouble() }.average().takeIf { it.isFinite() }
                val hrv = SourceSelection.primary(hrvs.filter { inDay(it.time) }, { it.metadata.dataOrigin.packageName }, { 1L }).map { it.heartRateVariabilityMillis }.average().takeIf { it.isFinite() }
                // Five-minute bins preserve actual time positions and gaps in the chart.
                val chart = load.points.groupBy { it.time / 300000L }.map { (_, p) -> HrPoint(p.first().time, p.map { it.bpm }.average().roundToInt()) }
                ConnectedDay(date.toString(), load.load, load.coveredMinutes, load.points.size,
                    load.points.minOfOrNull { it.bpm }, load.points.maxOfOrNull { it.bpm }, load.points.map { it.bpm }.average().takeIf { it.isFinite() }?.roundToInt(),
                    chart.joinToString(";") { "${it.time},${it.bpm}" }, load.zoneMinutes.joinToString(","),
                    dailySteps, total,
                    night?.start, night?.end, night?.asleep, night?.deep ?: 0, night?.light ?: 0, night?.rem ?: 0, night?.awake ?: 0, night?.unknown ?: 0,
                    night?.stages?.joinToString(";") { "${it.start},${it.end},${it.stage}" } ?: "",
                    overnight[date], rest, hrv, DailyAnalysis.recovery(night, overnight[date], baseline, sleepNeed), baseline.size,
                    DailyAnalysis.consistency(wakeTimes, zone), now.toEpochMilli(), load.points.lastOrNull()?.time,
                    active,
                    basal,
                    hourly.joinToString(";") { it.encode() }).let { fresh -> preserveUnavailableRecords(fresh,previous,guard.unavailable) }
            }
            val workouts = exerciseRecords.map { session ->
                val begin=session.startTime.toEpochMilli(); val finish=session.endTime.toEpochMilli()
                val type=when(session.exerciseType) {
                    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> "RUNNING"
                    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> "TREADMILL"
                    ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> "WALKING"
                    ExerciseSessionRecord.EXERCISE_TYPE_BIKING -> "CYCLING"
                    ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL -> "SWIMMING"
                    ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING -> "STRENGTH"
                    else -> "EXERCISE_${session.exerciseType}"
                }
                val calories = if(HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class) in granted)
                    guard.query("workout calories",null as Double?) { client.aggregate(AggregateRequest(setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),TimeRangeFilter.between(session.startTime, session.endTime)))[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories } else null
                Workout("HC:${session.metadata.id}","HEALTH_CONNECT",type,session.title ?: WorkoutAnalysis.title(type),begin,finish,
                    hr.filter { it.time >= begin && it.time < finish }.sortedBy { it.time }.joinToString(";") { "${it.time},${it.bpm}" },calories)
            }

            if(guard.unavailable.isNotEmpty() && result.none { it.steps!=null || it.asleep!=null || it.hrCount>0 || it.calories!=null || it.activeCalories!=null || it.restingHr!=null || it.hrv!=null } && workouts.isEmpty()) {
                status.value=status.value.copy(busy=false,message="Health Connect rejected: "+guard.unavailable.joinToString()+". No import was saved. Update Health Connect and your device app, then retry."); return@withLock false
            }
            guard.stage="saving local history"
            dao.replaceHealthImport(first.toString(), start.toEpochMilli(), result, workouts.takeIf { HealthPermission.getReadPermission(ExerciseSessionRecord::class) in granted && "ExerciseSessionRecord" !in guard.unavailable })
            val report = importReport(result, first.toString(), today.toString(), workouts.size) + if(guard.unavailable.isEmpty()) "" else "\nUnavailable in this import (previous values retained where available): " + guard.unavailable.joinToString()
            prefs.edit().putLong("lastSync", now.toEpochMilli()).putString("diagnostics",report).putStringSet("sources",sourcePackages).apply()
            PulseWidget.refresh(context)
            val hrCount = result.sumOf { it.hrCount }; val sleepCount = result.count { it.sleepStart != null }
            status.value = status.value.copy(busy = false, stepComparisons=stepComparisons, sources = sourcePackages.toList(), diagnostics = report, lastSync = now.toEpochMilli(), message = if(guard.unavailable.isNotEmpty()) "Imported available health data. Some requests were rejected: " + guard.unavailable.joinToString() + ". See Data status for details." else if (hrCount == 0 && sleepCount == 0 && workouts.isEmpty() && result.none { it.steps != null })
                "No shared records found. Enable your device app’s write permissions in Health Connect, sync your device in that app, then retry. Some apps only share new data."
                else "Imported $hrCount HR samples, $sleepCount nights and ${workouts.size} workouts from Health Connect. ${granted.intersect(READ_PERMISSIONS).size}/${READ_PERMISSIONS.size} read permissions granted.")
            true
        } catch (e: CancellationException) { status.value = status.value.copy(busy = false); throw e }
        catch (e: Exception) { status.value = status.value.copy(busy = false, message = if (e is SecurityException) "Health access changed. Grant permissions and sync again." else "Sync interrupted during ${guard.stage} (${e.javaClass.simpleName}). Your previous import is retained. Update Health Connect and retry."); false }
    }
    companion object {
        val READ_PERMISSIONS = setOf(ExerciseSessionRecord::class, HeartRateRecord::class, SleepSessionRecord::class, StepsRecord::class, TotalCaloriesBurnedRecord::class, ActiveCaloriesBurnedRecord::class, BasalMetabolicRateRecord::class, RestingHeartRateRecord::class, HeartRateVariabilityRmssdRecord::class).map { HealthPermission.getReadPermission(it) }.toSet()
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
