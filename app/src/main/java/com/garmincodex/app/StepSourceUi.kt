package com.garmincodex.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable internal fun StepSourceCard(sync:SyncState,onSelect:(String)->Unit) {
    JournalCard("STEP SOURCE") {
        Text("Match your daily total",fontSize=22.sp)
        Text("Choose Health Connect priority or one companion app. Counts are never added across apps. Comparisons below are for today at the last import.",fontSize=12.sp)
        val choices=(listOf("")+sync.stepComparisons.keys+listOf(sync.stepSource)).distinct()
        choices.forEach { pkg ->
            val count=sync.stepComparisons[pkg]?.let { String.format(Locale.getDefault(),"%,d steps",it) }
            FilterChip(selected=sync.stepSource==pkg,onClick={onSelect(pkg)},enabled=!sync.busy,
                label={Text(StepWindows.label(pkg)+(count?.let { " · $it" } ?: ""))},modifier=Modifier.fillMaxWidth())
        }
        Text("If both device apps show more than their imported counts, sync them to Health Connect first, then refresh Pulse. Full-day records may not contain a real hourly breakdown.",fontSize=12.sp)
    }
}
