package com.garmincodex.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
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

@Composable internal fun GoalCelebration(complete:Boolean,replay:Int,modifier:Modifier=Modifier) {
    val progress=remember(complete,replay) { Animatable(if(complete)0f else 1f) }
    LaunchedEffect(progress) { if(complete) progress.animateTo(1f,tween(2600,easing=LinearEasing)) }
    if(complete && progress.value<1f) Canvas(modifier.testTag("goal-confetti")) {
        val t=progress.value
        val colours=listOf(Color.White,Color(0xFFFFD75E),Color(0xFFFF89C8),Color(0xFF72DFFF))
        repeat(30) { i ->
            val x=((i*37%101)/100f*size.width + kotlin.math.sin(t*7+i)*12.dp.toPx()).coerceIn(0f,size.width)
            val y=(t*1.4f-(i%7)*.055f)*size.height
            if(y in 0f..size.height) drawRect(colours[i%4].copy(alpha=1f-t),Offset(x,y),Size(3.dp.toPx(),6.dp.toPx()))
        }
    }
}

@Composable private fun GoalTile(label:String,value:Double?,goal:Int,unit:String,colour:Color,icon:Int,date:String,tag:String,reveal:Float,modifier:Modifier,onClick:()->Unit,footer:@Composable ColumnScope.()->Unit={}) {
    val ink=colour
    val complete=value!=null && value>=goal && goal>0
    var replay by remember(date){mutableIntStateOf(0)}
    Box(modifier.clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(Color(0xFF202020),Color(0xFF121212)))).border(1.dp,Color.White.copy(alpha=.035f),RoundedCornerShape(24.dp))) {
        Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Column(Modifier.fillMaxWidth().testTag(tag).clickable(role=Role.Button,onClick=onClick),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                Icon(painterResource(icon),null,tint=ink,modifier=Modifier.size(18.dp));Text(label,color=Color.White,fontSize=13.sp,fontWeight=FontWeight.SemiBold)
            }
            androidx.compose.foundation.text.BasicText(value?.let{String.format(Locale.getDefault(),"%,d",it.roundToInt())} ?: "—",maxLines=1,autoSize=androidx.compose.foundation.text.TextAutoSize.StepBased(26.sp,42.sp),style=androidx.compose.ui.text.TextStyle(color=ink,fontWeight=FontWeight.Light,fontFamily=androidx.compose.ui.text.font.FontFamily.Monospace,letterSpacing=(-2).sp))
            Text(unit,color=Color(0xFFA3A3A3),fontSize=11.sp)
            LinearProgressIndicator(progress={((value ?: 0.0)/goal.coerceAtLeast(1)).toFloat().coerceIn(0f,1f)*reveal},modifier=Modifier.fillMaxWidth().height(3.dp),color=ink,trackColor=Color.White.copy(alpha=.09f))
            Text("Goal ${String.format(Locale.getDefault(),"%,d",goal)}",color=Color(0xFFA3A3A3),fontSize=11.sp)
            }
            Box(Modifier.fillMaxWidth().height(40.dp),contentAlignment=Alignment.CenterStart) {
                if(complete) TextButton(onClick={replay++},contentPadding=PaddingValues(0.dp),modifier=Modifier.fillMaxHeight().testTag("replay-$label-$replay").semantics{contentDescription="Celebrate $label goal"}){Text("Goal reached ✦",color=ink,fontSize=11.sp)}
                else Text(if(value==null) "No record" else "${(goal-value).coerceAtLeast(0.0).roundToInt()} to go",color=ink,fontSize=11.sp)
            }
            Column(Modifier.fillMaxWidth().clickable(role=Role.Button,onClick=onClick)) { CompositionLocalProvider(LocalContentColor provides ink){footer()} }
        }
        GoalCelebration(complete,replay,Modifier.matchParentSize())
    }
}

@Composable internal fun ActivityTiles(day:ConnectedDay?,goals:PersonalGoals,reveal:Float,onDetail:(String)->Unit,sync:SyncState) {
    val p=LocalPulseStyle.current
    Text("Your movement",fontSize=17.sp,fontWeight=FontWeight.Medium)
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        GoalTile("Steps",day?.steps?.toDouble(),goals.steps,"steps",p.primary,R.drawable.symbol_directions_walk,day?.date ?: "","goal-Steps",reveal,Modifier.weight(1f).fillMaxHeight(),{onDetail("steps")})
        GoalTile("Calories",day?.activeCalories,goals.activeCalories,"active kcal",p.warm,R.drawable.symbol_local_fire_department,day?.date ?: "","goal-Calories",reveal,Modifier.weight(1f).fillMaxHeight(),{onDetail("calories")}) {
            if(day?.activeCalories==null) {
                day?.calories?.let { Text("Total calories",fontSize=10.sp);Text("${it.roundToInt()} kcal",fontSize=12.sp) }
                Text(if(activeCaloriesAllowed(sync)) "Active goal unavailable · Fix ›" else "Allow active calories · Fix ›",fontSize=10.sp)
            }
        }
    }
}

@Composable internal fun SleepHero(day:ConnectedDay?,need:Int,onClick:()->Unit) {
    val colour=LocalPulseStyle.current.violet
    val ink=tileInk(colour)
    var replay by remember(day?.date){mutableIntStateOf(0)}
    val complete=day?.asleep?.let{it>=need && need>0} ?: false
    val spans=remember(day?.stages){day?.stages.orEmpty().split(';').mapNotNull { row -> val a=row.split(',');if(a.size==3)runCatching{SleepSpan(a[0].toLong(),a[1].toLong(),a[2].toInt())}.getOrNull() else null }}
    val colours=listOf(Color(0xFF8F75E8),colour,LocalPulseStyle.current.secondary,LocalPulseStyle.current.warm)
    val reveal=chartEntrance(day?.updatedAt ?: 0L)
    fun stamp(t:Long?)=t?.let{Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))} ?: "—"
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(Color(0xFF202020),Color(0xFF121212))))) {
        Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Column(Modifier.fillMaxWidth().clickable(role=Role.Button,onClickLabel="Sleep details",onClick=onClick),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Icon(painterResource(R.drawable.symbol_bedtime),null,tint=ink,modifier=Modifier.size(22.dp));Text("Sleep",color=Color.White,fontWeight=FontWeight.SemiBold)}
                Text("Goal ${need/60}h ${need%60}m",fontSize=11.sp,color=ink)
            }
            Text(day?.asleep?.let{"${it/60}h ${it%60}m"} ?: "No sleep total",fontSize=36.sp,fontWeight=FontWeight.Light,color=ink,fontFamily=androidx.compose.ui.text.font.FontFamily.Monospace)
            if(spans.isNotEmpty() && day?.sleepStart!=null && day.sleepEnd!=null && day.sleepEnd>day.sleepStart) {
                Canvas(Modifier.fillMaxWidth().height(54.dp).semantics{contentDescription="Sleep timeline ${stamp(day.sleepStart)} to ${stamp(day.sleepEnd)}. Tap for stage details."}) {
                    val start=day.sleepStart;val duration=(day.sleepEnd-start).toFloat()
                    repeat(4){i->drawLine(ink.copy(alpha=.1f),Offset(0f,(i+1)*size.height/4),Offset(size.width,(i+1)*size.height/4))}
                    spans.forEach { s ->
                        val level=when(s.stage){5->3;4,2->2;6->1;else->0}
                        val c=when(s.stage){5->colours[0];4->colours[1];6->colours[2];1,3,7->colours[3];else->ink.copy(alpha=.3f)}
                        val x=((s.start-start)/duration).coerceIn(0f,1f)*size.width
                        val right=((s.end-start)/duration).coerceIn(0f,1f)*size.width*reveal
                        if(right>x)drawRoundRect(c,Offset(x,level*size.height/4),Size(right-x,size.height/4-3.dp.toPx()),CornerRadius(3.dp.toPx()))
                    }
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(stamp(day.sleepStart),color=ink,fontSize=11.sp);Text(stamp(day.sleepEnd),color=ink,fontSize=11.sp)}
                Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){listOf("Deep","Light","REM","Awake").forEachIndexed{i,label->Text("● $label",color=colours[i],fontSize=10.sp)}}
            } else Text(if(day?.sleepStart!=null) "Sleep stages not shared · view session ›" else "Sync after wearing your watch to bed",color=ink,fontSize=12.sp)
            }
            if(complete) TextButton(onClick={replay++},modifier=Modifier.testTag("replay-Sleep-$replay").semantics{contentDescription="Celebrate Sleep goal"}){Text("Sleep goal reached ✦",color=ink)}
        }
        GoalCelebration(complete,replay,Modifier.matchParentSize())
    }
}
