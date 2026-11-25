package com.garmincodex.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable internal fun SleepTimingCard(days:List<ConnectedDay>,end:LocalDate) {
    val p=LocalPulseStyle.current
    val nights=sleepTiming(days,end)
    val recorded=nights.count{it.bedtime!=null}
    val changes=wakeChanges(nights)
    var expanded by remember(end){mutableStateOf(false)}
    JournalCard("YOUR SLEEP ROUTINE",p.violet) {
        Text("Bedtime & wake time",fontSize=21.sp,fontWeight=FontWeight.Bold)
        Text("${end.minusDays(6).format(DateTimeFormatter.ofPattern("d MMM"))} – ${end.format(DateTimeFormatter.ofPattern("d MMM"))} · $recorded/7 nights",fontSize=12.sp,color=p.muted)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Column { Text("Average bedtime",fontSize=11.sp,color=p.muted);Text(sleepClock(clockMean(nights.mapNotNull{it.bedtime})),fontSize=23.sp,color=p.violet) }
            Column { Text("Average wake time",fontSize=11.sp,color=p.muted);Text(sleepClock(clockMean(nights.mapNotNull{it.wake})),fontSize=23.sp,color=p.violet) }
        }
        Text(if(changes.size<2) "More consecutive nights needed to describe your wake-time changes." else "Wake time shifted by ${changes.average().roundToInt()} min on average across ${changes.size} consecutive-night pairs.",fontSize=13.sp)
        (if(expanded)nights else nights.takeLast(3)).forEach { night ->
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                Text(night.date.format(DateTimeFormatter.ofPattern("EEE d")),fontSize=12.sp,color=p.muted)
                Text(if(night.bedtime==null) "No session times" else "${sleepClock(night.bedtime)} → ${sleepClock(night.wake)}",fontSize=14.sp)
            }
        }
        TextButton(onClick={expanded=!expanded}){Text(if(expanded)"Show recent nights" else "Show all 7 nights")}
        Text("Dates refer to waking. Times use your phone’s current time zone. Averages need at least 3 sessions with a consistent timing pattern; missing sessions are excluded.",fontSize=11.sp,color=p.muted)
    }
}

@Composable internal fun SleepStagesCard(day:ConnectedDay?) {
    val p=LocalPulseStyle.current
    val hasStages=!day?.stages.isNullOrBlank()
    JournalCard("SLEEP STAGES",p.violet) {
        if(!hasStages) Text("Sleep stages weren’t shared for this night. Session times and a sleep total may still be available.",fontSize=13.sp)
        else {
            val night=requireNotNull(day)
            listOf("Deep" to night.deep,"Light" to night.light,"REM" to night.rem,"Awake" to night.awake,"Unclassified" to night.unknownSleep).forEach { (label,minutes) ->
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    Text(label,fontSize=13.sp,color=p.muted)
                    Text("${minutes/60}h ${minutes%60}m",fontSize=14.sp,fontWeight=FontWeight.SemiBold)
                }
            }
            Text("Awake time is separate from time asleep. Unclassified time is not assigned to a sleep stage.",fontSize=11.sp,color=p.muted)
        }
    }
}
