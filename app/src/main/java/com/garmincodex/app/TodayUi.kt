package com.garmincodex.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val Soft=Color(0xFFA9B5CA)
private val Mint=Color(0xFF83B4FF)
private val Lavender=Color(0xFFC0AFF5)
@Composable internal fun PulseHeader(busy:Boolean,onSync:()->Unit,lastSync:Long=0,onSettings:()->Unit={},onBack:(()->Unit)?=null) {
    val palette=LocalPulseStyle.current
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
        if(onBack!=null) IconButton(onClick=onBack,modifier=Modifier.semantics{contentDescription="Back to previous screen"}) { Text("‹",fontSize=32.sp,color=palette.primary) }
        Column(Modifier.weight(1f)) {
            Text("pulse",fontSize=25.sp,fontWeight=FontWeight.Bold,letterSpacing=(-1).sp,modifier=Modifier.semantics{contentDescription="Pulse 165"})
            val imported=if(lastSync>0) Instant.ofEpochMilli(lastSync).atZone(ZoneId.systemDefault()) else null
            Text(if(busy) "Updating your day…" else imported?.let{"Updated "+it.format(DateTimeFormatter.ofPattern(if(it.toLocalDate()==LocalDate.now()) "HH:mm" else "d MMM · HH:mm"))} ?: "Your health, together",fontSize=10.sp,color=palette.muted)
        }
        IconButton(onClick=onSync,enabled=!busy,modifier=Modifier.semantics{contentDescription=if(busy) "Syncing health data" else "Sync health data"}) {
            if(busy) CircularProgressIndicator(Modifier.size(20.dp),strokeWidth=2.dp,color=palette.primary)
            else Canvas(Modifier.size(20.dp)) {
                drawArc(palette.muted,40f,285f,false,style=Stroke(1.8.dp.toPx(),cap=StrokeCap.Round))
                val p=Path().apply{moveTo(size.width*.91f,size.height*.08f);lineTo(size.width*.91f,size.height*.35f);lineTo(size.width*.65f,size.height*.29f)}
                drawPath(p,palette.muted,style=Stroke(1.8.dp.toPx(),cap=StrokeCap.Round,join=StrokeJoin.Round))
            }
        }
        if(onBack==null) IconButton(onClick=onSettings,modifier=Modifier.background(palette.surface,RoundedCornerShape(16.dp)).semantics{contentDescription="Open Settings"}) { DockIcon(3,palette.primary) }
    }
}
@Composable internal fun TodayDate(date:String,onDate:(String)->Unit,onJournal:()->Unit) {
    val palette = LocalPulseStyle.current
    val Soft = palette.muted

    val d=LocalDate.parse(date)
    var calendar by remember{mutableStateOf(false)}
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
        Column(Modifier.weight(1f).clickable(role=Role.Button,onClickLabel="Show date navigation"){calendar=!calendar}.padding(vertical=6.dp)) {
            Text(if(d==LocalDate.now()) "Today" else d.format(DateTimeFormatter.ofPattern("EEEE")),fontSize=36.sp,fontWeight=FontWeight.Bold)
            Text(d.format(DateTimeFormatter.ofPattern("d MMM yyyy"))+"  ▾",fontSize=12.sp,color=Soft)
        }
        JournalAction(false,onJournal)
    }
    if(calendar) Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
        TextButton(onClick={onDate(d.minusDays(1).toString())},modifier=Modifier.semantics{contentDescription="Previous day"}){Text("‹ Previous")}
        TextButton(onClick={onDate(LocalDate.now().toString());calendar=false}){Text("Today")}
        TextButton(onClick={onDate(d.plusDays(1).toString())},enabled=d<LocalDate.now(),modifier=Modifier.semantics{contentDescription="Next day"}){Text("Next ›")}
    }
}
@Composable private fun SnapshotCard(modifier:Modifier=Modifier,onClick:()->Unit,content:@Composable ColumnScope.()->Unit) {
    val palette = LocalPulseStyle.current

    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(palette.surface).clickable(role=Role.Button,onClick=onClick).padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp),content=content)
}
@Composable internal fun TodaySnapshot(day:ConnectedDay?,sync:SyncState,goals:PersonalGoals,sleepNeed:Int,onDetail:(String)->Unit,onConnection:()->Unit) {
    val palette = LocalPulseStyle.current
    val Lime = palette.primary
    val Orange = palette.warm
    val Soft = palette.muted
    val Mint = palette.primary
    val Lavender = palette.violet

    val needsAttention=listOf("Sync interrupted", "Health access changed", "Health Connect is unavailable", "Allow Pulse", "Could not revoke").any{sync.message.startsWith(it)}
    if(!sync.busy && needsAttention) TextButton(onClick=onConnection){Text("Sync needs attention · review connection ›",color=Orange,fontSize=12.sp)}
    if(day==null || day.hrCount==0 && day.asleep==null && day.steps==null && day.activeCalories==null && day.recovery==null) {
        SnapshotCard(onClick=onConnection) {
            Text(if(sync.lastSync==0L) "Your day starts here" else "No records for this day",fontSize=24.sp,fontWeight=FontWeight.Bold)
            Text(if(sync.lastSync==0L) "Connect your device to see your sleep, recovery and movement." else "Sync your watch in its companion app, then refresh Pulse.",color=Soft,fontSize=14.sp)
            Text("${if(sync.lastSync==0L) "Connect device" else "Check connection"} ›",color=Mint,fontSize=14.sp)
        }
        return
    }
    val reveal=chartEntrance(day?.updatedAt ?: 0L)
    ActivityHero(day,goals,reveal,onDetail)
    val recovery=day?.recovery
    val tint=when{recovery==null->Soft;recovery<34->Color(0xFFFF8791);recovery<67->Color(0xFFF4CA58);else->palette.secondary}
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(palette.surface).clickable(role=Role.Button,onClickLabel="Recovery details"){onDetail("recovery")}.padding(18.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(58.dp),contentAlignment=Alignment.Center) {
            Canvas(Modifier.fillMaxSize().padding(3.dp)){drawArc(tint.copy(alpha=.12f),-90f,360f,false,style=Stroke(4.dp.toPx()));drawArc(tint,-90f,(recovery ?: 0)*3.6f*reveal,false,style=Stroke(4.dp.toPx(),cap=StrokeCap.Round))}
            Text(recovery?.toString() ?: "—",fontSize=20.sp,fontWeight=FontWeight.Bold,color=tint)
        }
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Text("RECOVERY",fontSize=10.sp,color=tint,letterSpacing=1.sp)
            Text(when{recovery==null->"Building your baseline";recovery>=67->"Ready for your day";recovery>=34->"Find your balance";else->"Room to recover"},fontSize=17.sp,fontWeight=FontWeight.SemiBold)
            Text("Daily estimate · explore ›",fontSize=11.sp,color=Soft)
        }
    }
    Text("Your health at a glance",fontSize=19.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(top=8.dp))
    Row(Modifier.height(IntrinsicSize.Min),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        SnapshotCard(Modifier.weight(1f).fillMaxHeight(),{onDetail("sleep")}) {
            Text("Sleep",color=Lavender,fontSize=13.sp)
            Text(day?.asleep?.let{"${it/60}h ${it%60}m"} ?: "—",fontSize=26.sp,fontWeight=FontWeight.Bold)
            LinearProgressIndicator(progress={((day?.asleep ?: 0).toFloat()/sleepNeed.coerceAtLeast(1)).coerceIn(0f,1f)*reveal},modifier=Modifier.fillMaxWidth().height(5.dp),color=Lavender,trackColor=Lavender.copy(alpha=.12f))
            Text("Goal ${sleepNeed/60}h ${sleepNeed%60}m ›",fontSize=11.sp,color=Soft)
        }
        SnapshotCard(Modifier.weight(1f).fillMaxHeight(),{onDetail("strain")}) {
            Text("Recorded strain",color=Orange,fontSize=13.sp)
            val strain=day?.takeIf{it.coveredMinutes>0}?.let{Scores.strain(it.load)}
            Text(strain?.let{String.format(Locale.US,"%.1f",it)} ?: "—",fontSize=26.sp,fontWeight=FontWeight.Bold)
            LinearProgressIndicator(progress={((strain ?: 0.0)/21).toFloat().coerceIn(0f,1f)*reveal},modifier=Modifier.fillMaxWidth().height(5.dp),color=Orange,trackColor=Orange.copy(alpha=.12f))
            Text("Of 21 · details ›",fontSize=11.sp,color=Soft)
        }
    }
    SnapshotCard(Modifier.testTag("goal-Calories"),{onDetail("calories")}) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
            Column { Text("Active calories",fontSize=13.sp,color=Orange);Text(day?.activeCalories?.let{"${it.roundToInt()} kcal"} ?: "No record",fontSize=26.sp,fontWeight=FontWeight.Bold) }
            Text("Goal ${goals.activeCalories} ›",color=Soft,fontSize=12.sp)
        }
        if(day?.activeCalories!=null) LinearProgressIndicator(progress={(day.activeCalories/goals.activeCalories.coerceAtLeast(1)).toFloat().coerceIn(0f,1f)*reveal},modifier=Modifier.fillMaxWidth().height(5.dp),color=Orange,trackColor=Orange.copy(alpha=.12f))
        else Text(if(activeCaloriesAllowed(sync)) "Active goal unavailable · Fix ›" else "Allow active calories · Fix ›",fontSize=11.sp,color=Orange)
    }
    val last=remember(day?.chart){ day?.chart?.split(';')?.mapNotNull{row->val p=row.split(',');if(p.size==2)p[0].toLongOrNull()?.let{t->p[1].toIntOrNull()?.let{t to it}} else null}?.maxByOrNull{it.first} }
    SnapshotCard(onClick={onDetail("hr")}) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Recent heart rate",fontSize=12.sp,color=Soft);Text(last?.let{"${it.second} bpm"} ?: "—",fontWeight=FontWeight.Bold)}
        Text(last?.let{Instant.ofEpochMilli(it.first).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM · HH:mm"))+" · chart average ›"} ?: "No heart-rate samples yet",fontSize=11.sp,color=Soft)
    }
}
