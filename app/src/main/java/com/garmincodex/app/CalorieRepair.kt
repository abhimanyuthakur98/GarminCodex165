package com.garmincodex.app
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord

internal fun activeCaloriesAllowed(sync:SyncState)=HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class) in sync.granted
@Composable internal fun CalorieRepair(day:ConnectedDay?,sync:SyncState,onPermissions:()->Unit,onSettings:()->Unit,onGarmin:()->Unit,onSync:()->Unit) {
    JournalCard("ACTIVE CALORIES · CONNECTION",Orange) {
        val allowed=activeCaloriesAllowed(sync)
        Text(if(!allowed) "Pulse needs read access" else "No active-calorie record for this day",fontSize=20.sp)
        Text(if(!allowed) "Allow Active calories burned for Pulse in Health Connect." else "Pulse has read permission. Garmin may not have written this category yet, or there may be no record for this date.",fontSize=13.sp,color=Color.Gray)
        if(day?.calories!=null) Text("Total calories arrived. They include resting energy and cannot fill your active goal.",fontSize=12.sp,color=Orange)
        Button(onClick=onPermissions,modifier=Modifier.fillMaxWidth()){Text("1 · Review Pulse access")}
        OutlinedButton(onClick=onSettings,modifier=Modifier.fillMaxWidth()){Text("2 · Garmin sharing settings")}
        Text("In Health Connect → App permissions → Garmin Connect, enable writing Active calories burned. Update Garmin Connect if the category is absent.",fontSize=12.sp,color=Color.Gray)
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){TextButton(onClick=onGarmin){Text("3 · Sync in Garmin")};TextButton(onClick=onSync,enabled=!sync.busy){Text(if(sync.busy) "Importing…" else "4 · Import again")}}
        ChartExplanation("Garmin lists active and total calories as shareable. Sharing does not guarantee historical backfill. After enabling it, sync your watch in Garmin Connect and import again in Pulse. Health Connect's Active calories burned data entries can confirm whether Garmin wrote records. Pulse cannot reconstruct active energy from total energy or steps alone.")
    }
}
