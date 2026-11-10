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

private val Soft=Color(0xFF9BAEA5)
private val Mint=Color(0xFF72E8BE)
private val Lavender=Color(0xFFB6B9FF)
@Composable internal fun PulseHeader(busy:Boolean,onSync:()->Unit,lastSync:Long=0) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
        Image(painterResource(R.drawable.ic_pulse),contentDescription="Pulse 165",modifier=Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)))
        val imported=if(lastSync>0) Instant.ofEpochMilli(lastSync).atZone(ZoneId.systemDefault()) else null
        Text(if(busy) "Updating…" else imported?.let{"Imported "+it.format(DateTimeFormatter.ofPattern(if(it.toLocalDate()==LocalDate.now()) "HH:mm" else "d MMM · HH:mm"))} ?: "Not connected",fontSize=11.sp,color=Soft)
        IconButton(onClick=onSync,enabled=!busy,modifier=Modifier.background(Color.White.copy(alpha=.04f),RoundedCornerShape(16.dp)).semantics{contentDescription=if(busy) "Syncing health data" else "Sync health data"}) {
            if(busy) CircularProgressIndicator(Modifier.size(20.dp),strokeWidth=2.dp,color=Mint)
            else Canvas(Modifier.size(22.dp)) {
                drawArc(Mint,40f,285f,false,style=Stroke(1.8.dp.toPx(),cap=StrokeCap.Round))
                val p=Path().apply{moveTo(size.width*.91f,size.height*.08f);lineTo(size.width*.91f,size.height*.35f);lineTo(size.width*.65f,size.height*.29f)}
                drawPath(p,Mint,style=Stroke(1.8.dp.toPx(),cap=StrokeCap.Round,join=StrokeJoin.Round))
            }
        }
    }
}
@Composable internal fun TodayDate(date:String,onDate:(String)->Unit,onJournal:()->Unit) {
    val d=LocalDate.parse(date)
    var calendar by remember{mutableStateOf(false)}
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
        Column(Modifier.weight(1f).clickable(role=Role.Button,onClickLabel="Show date navigation"){calendar=!calendar}.padding(vertical=6.dp)) {
            Text(if(d==LocalDate.now()) "Today" else d.format(DateTimeFormatter.ofPattern("EEEE")),fontSize=27.sp,fontWeight=FontWeight.Bold)
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
    Column(modifier.fillMaxWidth().background(Color(0xFF15211C),RoundedCornerShape(22.dp)).clickable(role=Role.Button,onClick=onClick).padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp),content=content)
}
@Composable internal fun TodaySnapshot(day:ConnectedDay?,sync:SyncState,goals:PersonalGoals,sleepNeed:Int,onDetail:(String)->Unit,onConnection:()->Unit) {
    val needsAttention=listOf("Sync interrupted", "Health access changed", "Health Connect is unavailable", "Allow Pulse", "Could not revoke").any{sync.message.startsWith(it)}
    if(!sync.busy && needsAttention) TextButton(onClick=onConnection){Text("Sync needs attention · review connection ›",color=Orange,fontSize=12.sp)}
    if(day==null || day.hrCount==0 && day.asleep==null && day.steps==null && day.activeCalories==null && day.recovery==null) {
        SnapshotCard(onClick=onConnection) {
            Text(if(sync.lastSync==0L) "Your day starts here" else "No records for this day",fontSize=24.sp,fontWeight=FontWeight.Bold)
            Text(if(sync.lastSync==0L) "Connect Garmin to see your sleep, recovery and movement." else "Sync your watch in Garmin Connect, then refresh Pulse.",color=Soft,fontSize=14.sp)
            Text("${if(sync.lastSync==0L) "Connect Garmin" else "Check connection"} ›",color=Mint,fontSize=14.sp)
        }
        return
    }
    val reveal=chartEntrance()
    val recovery=day?.recovery
    val tint=when{recovery==null->Soft;recovery<34->Color(0xFFFF8791);recovery<67->Color(0xFFF4CA58);else->Mint}
    Row(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF203A2D),Color(0xFF13211B))),RoundedCornerShape(26.dp)).clickable(role=Role.Button,onClickLabel="Recovery details"){onDetail("recovery")}.padding(18.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(18.dp)) {
        Box(Modifier.size(88.dp),contentAlignment=Alignment.Center) {
            Canvas(Modifier.fillMaxSize().padding(4.dp)){drawArc(Color.White.copy(alpha=.09f),-90f,360f,false,style=Stroke(5.dp.toPx()));drawArc(tint,-90f,(recovery ?: 0)*3.6f*reveal,false,style=Stroke(5.dp.toPx(),cap=StrokeCap.Round))}
            Text(recovery?.let{"$it%"} ?: "—",fontSize=27.sp,fontWeight=FontWeight.Bold,color=tint)
        }
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
            Text("RECOVERY",fontSize=10.sp,color=tint,letterSpacing=1.sp)
            Text(when{recovery==null->"Building your baseline";recovery>=67->"Ready for your day";recovery>=34->"Find your balance";else->"Room to recover"},fontSize=20.sp,fontWeight=FontWeight.SemiBold)
            Text(if(recovery==null) "Tap to see what’s missing" else "Daily estimate · details ›",fontSize=11.sp,color=Soft)
        }
    }
    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
        SnapshotCard(Modifier.weight(1f),{onDetail("sleep")}) {
            Text("Sleep",color=Lavender,fontSize=12.sp)
            Text(day?.asleep?.let{"${it/60}h ${it%60}m"} ?: "—",fontSize=25.sp,fontWeight=FontWeight.Bold)
            LinearProgressIndicator(progress={((day?.asleep ?: 0).toFloat()/sleepNeed.coerceAtLeast(1)).coerceIn(0f,1f)*reveal},modifier=Modifier.fillMaxWidth(),color=Lavender,trackColor=Color.White.copy(alpha=.07f))
            Text("Goal ${sleepNeed/60}h ${sleepNeed%60}m ›",fontSize=11.sp,color=Soft)
        }
        SnapshotCard(Modifier.weight(1f),{onDetail("strain")}) {
            Text("Recorded strain",color=Orange,fontSize=12.sp)
            val strain=day?.takeIf{it.coveredMinutes>0}?.let{Scores.strain(it.load)}
            Text(strain?.let{String.format(Locale.US,"%.1f",it)} ?: "—",fontSize=25.sp,fontWeight=FontWeight.Bold)
            LinearProgressIndicator(progress={((strain ?: 0.0)/21).toFloat().coerceIn(0f,1f)*reveal},modifier=Modifier.fillMaxWidth(),color=Orange,trackColor=Color.White.copy(alpha=.07f))
            Text("Of 21 · details ›",fontSize=11.sp,color=Soft)
        }
    }
    Column(Modifier.fillMaxWidth().background(Color(0xFF15211C),RoundedCornerShape(22.dp)).padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text("Your movement",fontWeight=FontWeight.SemiBold,fontSize=16.sp)
        listOf(Triple("Steps",day?.steps?.toDouble(),goals.steps),Triple("Active calories",day?.activeCalories,goals.activeCalories)).forEachIndexed{index,(label,value,goal)->
            val color=if(index==0)Lime else Orange
            Column(Modifier.fillMaxWidth().testTag(if(index==0)"goal-Steps" else "goal-Calories").clickable(role=Role.Button,onClickLabel="$label details and goal"){onDetail(if(index==0)"steps" else "calories")},verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(label,fontSize=12.sp,color=Soft);Text(value?.let{String.format(Locale.getDefault(),"%,d",it.roundToInt())+" / $goal"} ?: "Not shared",fontSize=13.sp,color=color)}
                LinearProgressIndicator(progress={((value ?: 0.0)/goal.coerceAtLeast(1)).coerceIn(0.0,1.0).toFloat()*reveal},modifier=Modifier.fillMaxWidth(),color=color,trackColor=Color.White.copy(alpha=.07f))
            }
        }
    }
    val last=remember(day?.chart){ day?.chart?.split(';')?.mapNotNull{row->val p=row.split(',');if(p.size==2)p[0].toLongOrNull()?.let{t->p[1].toIntOrNull()?.let{t to it}} else null}?.maxByOrNull{it.first} }
    SnapshotCard(onClick={onDetail("hr")}) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Recent heart rate",fontSize=12.sp,color=Soft);Text(last?.let{"${it.second} bpm"} ?: "—",fontWeight=FontWeight.Bold)}
        Text(last?.let{Instant.ofEpochMilli(it.first).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM · HH:mm"))+" · chart average ›"} ?: "No heart-rate samples yet",fontSize=11.sp,color=Soft)
    }
}
