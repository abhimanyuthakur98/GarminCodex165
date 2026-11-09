package com.garmincodex.app

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.util.UUID

interface SensorFeed { fun samples(): Flow<SensorSample> }
class MockFeed : SensorFeed {
    override fun samples() = flow {
        var tick = 0
        while (true) {
            val hr = (112 + 36 * kotlin.math.sin(tick / 35.0) + 5 * kotlin.math.sin(tick.toDouble())).toInt()
            emit(SensorSample(System.currentTimeMillis(), hr, listOf(60000.0 / hr), 4200 + tick * 2, 9.81 + kotlin.math.sin(tick.toDouble())))
            tick++; delay(1000)
        }
    }
}
data class NearbyWatch(val address: String, val name: String)
@SuppressLint("MissingPermission")
class BleFeed(private val context: Context) : SensorFeed {
    private val manager = context.getSystemService(BluetoothManager::class.java)
    val devices = MutableStateFlow<List<NearbyWatch>>(emptyList())
    val status = MutableStateFlow("Choose a watch")
    var address: String? = null
    private var scan: ScanCallback? = null
    fun stopScan() { runCatching { scan?.let { manager.adapter?.bluetoothLeScanner?.stopScan(it) } }; scan = null }
    fun scan() {
        stopScan(); devices.value = emptyList()
        val adapter = manager.adapter
        if (adapter == null || !adapter.isEnabled) { status.value = "Enable Bluetooth, then scan again"; return }
        status.value = "Scanning for heart-rate broadcasts…"
        scan = object : ScanCallback() {
            override fun onScanResult(type: Int, result: ScanResult) {
                val watch = NearbyWatch(result.device.address, result.device.name ?: "Heart-rate sensor")
                devices.value = (devices.value + watch).distinctBy { it.address }
            }
            override fun onScanFailed(errorCode: Int) { status.value = "Scan failed ($errorCode). Try again." }
        }
        adapter.bluetoothLeScanner.startScan(listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(HR_SERVICE)).build()), ScanSettings.Builder().build(), scan)
    }
    override fun samples(): Flow<SensorSample> = callbackFlow {
        val selected = address ?: error("Select a watch first")
        var gatt: BluetoothGatt? = null
        val callback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(g: BluetoothGatt, code: Int, state: Int) {
                if (code == BluetoothGatt.GATT_SUCCESS && state == BluetoothProfile.STATE_CONNECTED) {
                    status.value = "Discovering heart-rate service…"; g.discoverServices()
                } else if (state == BluetoothProfile.STATE_DISCONNECTED || code != BluetoothGatt.GATT_SUCCESS) close(IllegalStateException("Watch disconnected ($code)"))
            }
            override fun onServicesDiscovered(g: BluetoothGatt, code: Int) {
                val characteristic = g.getService(HR_SERVICE)?.getCharacteristic(HR_MEASUREMENT)
                val descriptor = characteristic?.getDescriptor(CCCD)
                if (code != 0 || characteristic == null || descriptor == null) { close(IllegalStateException("No heart-rate broadcast service")); return }
                if (!g.setCharacteristicNotification(characteristic, true)) { close(IllegalStateException("Notifications unavailable")); return }
                @Suppress("DEPRECATION")
                run { descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE; if (!g.writeDescriptor(descriptor)) close(IllegalStateException("Subscription failed")) }
            }
            override fun onDescriptorWrite(g: BluetoothGatt, d: BluetoothGattDescriptor, code: Int) {
                if (code == 0) status.value = "Connected • live heart rate" else close(IllegalStateException("Subscription rejected ($code)"))
            }
            @Deprecated("Legacy callback for Android 12 and earlier")
            override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic) { @Suppress("DEPRECATION") val value = c.value; accept(value) }
            override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic, value: ByteArray) { accept(value) }
            private fun accept(value: ByteArray) { HeartRatePacket.parse(value, System.currentTimeMillis())?.let { trySend(it) } }
        }
        status.value = "Connecting…"
        gatt = manager.adapter.getRemoteDevice(selected).connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
        val timeout = launch { delay(25000); if (!status.value.startsWith("Connected")) close(IllegalStateException("Connection timed out")) }
        awaitClose { timeout.cancel(); gatt?.disconnect(); gatt?.close() }
    }.retryWhen { cause, attempt ->
        if (cause is SecurityException) { status.value = "Bluetooth permission required"; false }
        else { status.value = "${cause.message} • reconnecting"; delay((2000L * (attempt + 1)).coerceAtMost(30000)); true }
    }
    companion object {
        val HR_SERVICE: UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        val HR_MEASUREMENT: UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        val CCCD: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}
