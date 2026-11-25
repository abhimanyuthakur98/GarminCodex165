package com.garmincodex.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable internal fun StepSourceCard(sync:SyncState,onSelect:(String)->Unit) {
    JournalCard("STEPS & CALORIES") {
        Text("Choose your activity source",fontSize=22.sp)
        Text("Garmin Connect is the default. This choice applies to steps and active, total and basal calories. Missing data stays unavailable; Pulse never substitutes a different app. Step comparisons are from the last import.",fontSize=12.sp)
        val choices=(listOf(ActivitySource.GARMIN,"com.sec.android.app.shealth","")+sync.stepComparisons.keys+listOf(sync.stepSource)).distinct()
        choices.forEach { pkg ->
            val count=sync.stepComparisons[pkg]?.let { String.format(Locale.getDefault(),"%,d steps",it) }
            FilterChip(selected=sync.stepSource==pkg,onClick={onSelect(pkg)},enabled=!sync.busy,
                label={Text(StepWindows.label(pkg)+(count?.let { " · $it" } ?: ""))},modifier=Modifier.fillMaxWidth())
        }
        Text("If both device apps show more than their imported counts, sync them to Health Connect first, then refresh Pulse. Full-day records may not contain a real hourly breakdown.",fontSize=12.sp)
    }
}
