package com.garmincodex.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.*
import kotlin.math.roundToInt

@Composable internal fun OverviewStat(label:String,value:String,caption:String,modifier:Modifier=Modifier,onClick:(()->Unit)?=null) {
    val p=LocalPulseStyle.current
    Column(modifier.background(Brush.verticalGradient(listOf(p.primary.copy(alpha=.10f).compositeOver(p.surface),Color.White.copy(alpha=.035f).compositeOver(p.surface))),RoundedCornerShape(22.dp)).border(1.dp,p.primary.copy(alpha=.18f),RoundedCornerShape(22.dp)).then(if(onClick!=null)Modifier.clickable(role=Role.Button,onClick=onClick) else Modifier).padding(14.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Text(label,color=p.muted,fontSize=12.sp)
        androidx.compose.foundation.text.BasicText(value,maxLines=1,autoSize=androidx.compose.foundation.text.TextAutoSize.StepBased(18.sp,28.sp),style=androidx.compose.ui.text.TextStyle(color=Color.White,fontWeight=FontWeight.Medium,letterSpacing=(-.7).sp))
        Text(caption,color=p.muted,fontSize=10.sp)
    }
}

@Composable internal fun VitalGrid(day:ConnectedDay?,onDetail:(String)->Unit) {
    Text("Health at a glance",fontSize=16.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(top=6.dp))
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        OverviewStat("Recovery",day?.recovery?.let{"$it / 100"} ?: "—",if(day?.recovery==null)"Baseline needed" else "Pulse estimate ›",Modifier.weight(1f)){onDetail("recovery")}
        OverviewStat("Heart rate",day?.hrAverage?.let{"$it bpm"} ?: "—","Daily average ›",Modifier.weight(1f)){onDetail("hr")}
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        OverviewStat("Resting HR",day?.restingHr?.roundToInt()?.let{"$it bpm"} ?: "—",if(day?.restingHr==null)"Not shared" else "Imported measurement ›",Modifier.weight(1f)){onDetail("hr")}
        OverviewStat("HRV",day?.hrv?.roundToInt()?.let{"$it ms"} ?: "—",if(day?.hrv==null)"Not shared" else "Daily RMSSD ›",Modifier.weight(1f)){onDetail("hr")}
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        OverviewStat("Cardio strain",day?.takeIf{it.coveredMinutes>0}?.let{String.format(java.util.Locale.US,"%.1f / 21",Scores.strain(it.load))} ?: "—","Pulse estimate ›",Modifier.weight(1f)){onDetail("strain")}
        OverviewStat("Total energy",day?.calories?.roundToInt()?.let{"$it kcal"} ?: "—","Active + resting ›",Modifier.weight(1f)){onDetail("calories")}
    }
}

@Composable internal fun HomeShortcuts(day:ConnectedDay?,days:List<ConnectedDay>,goals:PersonalGoals,workouts:List<Workout>,selected:String,onFitness:()->Unit,onWorkouts:()->Unit,onJournal:()->Unit) {
    val p=LocalPulseStyle.current
    val fitness=remember(days,goals.age){FitnessAgeModel.calculate(days,goals.age,LocalDate.now())}
    val sessions=workouts.count{Instant.ofEpochMilli(it.start).atZone(ZoneId.systemDefault()).toLocalDate().toString()==selected}
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        OverviewStat("Fitness age",fitness.age?.let{String.format(java.util.Locale.US,"%.1f",it)} ?: "—","View estimate & actions ›",Modifier.weight(1f),onFitness)
        OverviewStat("Workouts",sessions.toString(),"Recorded on this day ›",Modifier.weight(1f),onWorkouts)
    }
    OutlinedButton(onClick=onJournal,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp)) { Text("How are you feeling?  ·  Check in",color=p.primary) }
}

@Composable internal fun TrendOverview(days:List<ConnectedDay>) {
    val last=LocalDate.now().minusDays(1)
    val recent=days.filter{val d=LocalDate.parse(it.date); d<=last && d>last.minusDays(7)}
    val steps=recent.mapNotNull{it.steps};val sleep=recent.mapNotNull{it.asleep}
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        OverviewStat("Average steps",if(steps.isEmpty())"—" else String.format(java.util.Locale.getDefault(),"%,d",steps.average().roundToInt()),"${steps.size}/7 completed days",Modifier.weight(1f))
        val minutes=if(sleep.isEmpty())null else sleep.average().roundToInt()
        OverviewStat("Average sleep",minutes?.let{"${it/60}h ${it%60}m"} ?: "—","${sleep.size}/7 completed days",Modifier.weight(1f))
    }
}

@Composable internal fun TrainingSummary(workouts:List<Workout>) {
    val start=LocalDate.now().minusDays(6).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val recent=workouts.filter{it.start>=start && it.start<=System.currentTimeMillis()}
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        OverviewStat("Last 7 days",recent.size.toString(),"Recorded sessions",Modifier.weight(1f))
        OverviewStat("Time training","${recent.sumOf{(it.end-it.start).coerceAtLeast(0)/60000}} min","Elapsed session time",Modifier.weight(1f))
    }
}

@Composable internal fun SettingsSections(selected:String,onSelect:(String)->Unit) {
    PulseTabs(listOf("Overview","Appearance","Layout"),selected,onSelect,scrollable=false)
}
