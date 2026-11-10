package com.garmincodex.app

import android.app.*
import android.content.*
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.room.Room
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.android.ext.android.inject
import java.time.*

val demoNight = Night(68.0, 62.0, 52, 55, 92, 243, 105, 25, 480, 88, 86)
data class DashboardState(val mode: String = "DEMO", val running: Boolean = false, val sample: SensorSample? = null, val chart: List<Int> = emptyList(), val load: Double = 0.0, val message: String = "Simulation paused")
class HealthRepository(private val dao: DailyDao, val ble: BleFeed) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val state = MutableStateFlow(DashboardState())
    val history = dao.observe()
    private var job: Job? = null
    @Volatile private var requestedMode: String? = null
    fun start(mode: String) {
        requestedMode = mode
        val previous = job
        job = scope.launch {
            previous?.cancelAndJoin()
            var day = LocalDate.now().toString()
            var load = dao.get(day, mode)?.load ?: 0.0
            var last: Long? = null
            var count = 0
            state.value = DashboardState(mode, true, load = load, message = if (mode == "DEMO") "Simulated Forerunner 165 stream" else "Waiting for watch…")
            try {
                val feed: SensorFeed = if (mode == "DEMO") MockFeed() else ble
                feed.samples().collect { sample ->
                    val date = Instant.ofEpochMilli(sample.time).atZone(ZoneId.systemDefault()).toLocalDate().toString()
                    if (date != day) { persist(day, mode, load); day = date; load = dao.get(day, mode)?.load ?: 0.0; last = null }
                    val delta = last?.let { (sample.time - it) / 1000.0 } ?: 0.0
                    // Missing or duplicate samples never count as exercise time.
                    if (delta > 0 && delta <= 10) load += Scores.load(sample.hr, delta)
                    last = sample.time
                    state.value = state.value.copy(sample = sample, chart = (state.value.chart + sample.hr).takeLast(120), load = load)
                    if (++count % 10 == 0) persist(day, mode, load)
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { state.value = state.value.copy(message = "Tracking failed: ${e.message}") }
            finally { withContext(NonCancellable) { persist(day, mode, load) }; state.value = state.value.copy(running = false, sample = null) }
        }
    }
    private suspend fun persist(day: String, mode: String, load: Double) {
        dao.put(DailyRecord(day, mode, load, if (mode == "DEMO") Scores.recovery(demoNight) else null, if (mode == "DEMO") demoNight.asleep else null, System.currentTimeMillis()))
    }
    fun stop() { requestedMode = null; job?.cancel(); ble.stopScan() }
    fun stopLive() { if (requestedMode == "LIVE") stop() }
}
class DashboardViewModel(private val repo: HealthRepository, val sync: HealthConnectSync, private val dao: DailyDao) : ViewModel() {
    val journal = dao.journal().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val habits = dao.habits().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun saveJournal(entry: JournalEntry) { JournalAnalysis.validate(entry); viewModelScope.launch { dao.putJournal(entry) } }
    fun saveHabit(tag: HabitTag) { require(tag.name.isNotBlank() && tag.name.length<=30); viewModelScope.launch { dao.putHabit(tag) } }
    fun deleteJournal(date:String) { viewModelScope.launch { dao.deleteJournal(date) } }
    fun clearJournal() { viewModelScope.launch { dao.clearPersonalJournal() } }
    val workouts = dao.workouts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val checkIns = dao.checkIns().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val workoutMessage = MutableStateFlow<String?>(null)
    val importingFit = MutableStateFlow(false)
    fun saveCheckIn(value: WorkoutCheckIn) { require(value.effort in 1..10 && value.soreness in 0..10); viewModelScope.launch { dao.putCheckIn(value) } }
    fun removeWorkout(id: String) { viewModelScope.launch { dao.removeWorkout(id) } }
    fun importFit(readBytes: () -> ByteArray) {
        if(importingFit.value) return
        importingFit.value=true
        viewModelScope.launch {
            try {
                val imported=sync.importFit(readBytes)
                workoutMessage.value="Imported ${imported} FIT session(s). Exact file reimports update existing entries."
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) { workoutMessage.value="Could not import FIT: ${e.message?.take(160) ?: "invalid file"}" }
            finally { importingFit.value=false }
        }
    }
    val connected = dao.connected().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val syncStatus = sync.status.asStateFlow()
    private var syncJob: Job? = null
    fun refresh(onFinished: ((Boolean) -> Unit)? = null) { if (syncJob?.isActive == true) return; syncJob = viewModelScope.launch { val success=sync.sync();onFinished?.invoke(success) } }
    fun clearImported() { viewModelScope.launch { sync.clear() } }
    val state = repo.state.asStateFlow()
    val history = repo.history.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val devices = repo.ble.devices.asStateFlow()
    val connection = repo.ble.status.asStateFlow()
    private var scanJob: Job? = null
    fun scan() { repo.stop(); scanJob?.cancel(); scanJob = viewModelScope.launch { delay(100); try { repo.ble.scan(); delay(15000) } catch (_: SecurityException) { repo.ble.status.value = "Grant Bluetooth permission, then retry" } finally { repo.ble.stopScan() } } }
    fun select(address: String) { scanJob?.cancel(); repo.ble.stopScan(); repo.ble.address = address }
    fun demo() { scanJob?.cancel(); repo.ble.stopScan(); repo.start("DEMO") }
    fun stop() = repo.stop()
    override fun onCleared() { scanJob?.cancel(); repo.ble.stopScan(); if (repo.state.value.mode == "DEMO") repo.stop() }
}
class PulseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin { androidContext(this@PulseApp); modules(module {
            single { Room.databaseBuilder(get(), PulseDatabase::class.java, "pulse.db").build() }
            single { get<PulseDatabase>().days() }
            single { BleFeed(get()) }
            single { HealthRepository(get(), get()) }
            single { HealthConnectSync(get(), get()) }
            factory { DashboardViewModel(get(), get(), get()) }
            single { SyncImporter(get()) }
        }) }
    }
}
class TrackingService : Service() {
    private val repo: HealthRepository by inject()
    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("tracking", "Watch tracking", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1, Intent(this, TrackingService::class.java).setAction("STOP"), PendingIntent.FLAG_IMMUTABLE)
        startForeground(165, NotificationCompat.Builder(this, "tracking").setSmallIcon(android.R.drawable.ic_menu_compass).setContentTitle("Pulse 165 • watch tracking").setContentText("Heart-rate capture and offline sync are active").setContentIntent(open).addAction(0, "Stop", stop).setOngoing(true).build())
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") { repo.stop(); stopSelf() } else repo.start("LIVE")
        return START_NOT_STICKY
    }
    override fun onDestroy() { repo.stopLive(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
}
