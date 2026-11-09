package com.garmincodex.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.android.ext.android.get

class MainActivity : ComponentActivity() {
    private lateinit var vm: DashboardViewModel
    private val uiMessage = mutableStateOf<String?>(null)
    private val choosing = mutableStateOf(false)
    private val healthPermissions = registerForActivityResult(PermissionController.createRequestPermissionResultContract()) { vm.refresh() }
    private val blePermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.all { it }) { choosing.value = true; vm.scan() }
        else uiMessage.value = "Allow Nearby devices for optional live HR. Daily sync does not need Bluetooth permissions."
    }
    private fun open(intent: Intent) { try { startActivity(intent) } catch (_: Exception) { uiMessage.value = "This app or settings screen is unavailable on your phone." } }
    private fun connectHealth() {
        if (vm.sync.availability() == HealthConnectClient.SDK_AVAILABLE) healthPermissions.launch(HealthConnectSync.READ_PERMISSIONS)
        else open(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")))
    }
    override fun onResume() { super.onResume(); if (::vm.isInitialized) vm.refresh() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); enableEdgeToEdge()
        vm = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST") override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = get<DashboardViewModel>() as T
        })[DashboardViewModel::class.java]
        setContent {
            val goals by vm.sync.goals.collectAsStateWithLifecycle()
            val days by vm.connected.collectAsStateWithLifecycle()
            val sync by vm.syncStatus.collectAsStateWithLifecycle()
            val live by vm.state.collectAsStateWithLifecycle()
            val connection by vm.connection.collectAsStateWithLifecycle()
            val watches by vm.devices.collectAsStateWithLifecycle()
            PulseTheme {
                PulseDashboard(days, sync, live, connection, uiMessage.value, vm.sync.sleepNeed, vm.sync.maximumHr, vm.sync.restingHr,
                    onConnect = ::connectHealth, onSync = { vm.refresh() },
                    onHealthSettings = { open(Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)) },
                    onGarmin = { open(packageManager.getLaunchIntentForPackage("com.garmin.android.apps.connectmobile") ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.garmin.android.apps.connectmobile"))) },
                    onBackground = { if (vm.sync.backgroundSupported()) healthPermissions.launch(setOf(HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND)) else uiMessage.value = "Background access unavailable. Sync works when Pulse is open." },
                    onProfile = { n, m, r -> vm.sync.saveProfile(n, m, r); vm.refresh() }, onClear = { vm.clearImported() },
                    onLive = { stopService(Intent(this, TrackingService::class.java)); blePermissions.launch(if (Build.VERSION.SDK_INT >= 31) arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT) else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)) },
                    onStop = { vm.stop(); stopService(Intent(this, TrackingService::class.java)) },
                    goals = goals, onGoals = { vm.sync.saveGoals(it) })
                if (choosing.value) WatchDialog(watches, connection, { choosing.value = false }, { vm.scan() }) {
                    vm.select(it); choosing.value = false
                    try { ContextCompat.startForegroundService(this, Intent(this, TrackingService::class.java)) }
                    catch (e: Exception) { uiMessage.value = "Live session could not start: ${e.javaClass.simpleName}" }
                }
            }
        }
    }
}
class PrivacyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { PulseTheme { PrivacyScreen { finish() } } } }
}
