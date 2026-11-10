package com.garmincodex.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.*
import kotlin.random.Random

private val AgeMint=Color(0xFF72E8BE)
private val AgeMuted=Color(0xFF9BAEA5)
private val FactorColors=listOf(Color(0xFFC6F65B),Color(0xFFB6B9FF),AgeMint,Color(0xFFFF8750))
private fun ageNumber(value:Double)=String.format(Locale.US,"%.1f",value)
@Composable internal fun FitnessAgePage(days:List<ConnectedDay>,goals:PersonalGoals,onAge:()->Unit) {
    val today=LocalDate.now()
    val result=remember(days,goals.age,today){FitnessAgeModel.calculate(days,goals.age,today)}
    var section by rememberSaveable{mutableStateOf("Overview")}
    var factorIndex by rememberSaveable{mutableIntStateOf(-1)}
    var moving by rememberSaveable{mutableStateOf(true)}
    val currentRecovery=days.firstOrNull{it.date==today.toString()}?.recovery
    val index=if(factorIndex<0)FitnessCoaching.focus(result,currentRecovery) else factorIndex.coerceIn(0,3)
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Text("Fitness age",fontSize=28.sp,fontWeight=FontWeight.Bold);TextButton(onAge){Text(goals.age?.let{"Age $it"} ?: "Set age")}}
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("Overview","Progress","Actions").forEach{tab->FilterChip(section==tab,{section=tab},label={Text(tab)})}}
    key(section) { Column(pageMotion(section),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        when(section) {
            "Overview" -> {
                Box(Modifier.fillMaxWidth().height(224.dp),contentAlignment=Alignment.Center) {
                    AuroraOrb(Modifier.size(224.dp),moving)
                    Column(Modifier.width(210.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(5.dp)) {
                        Text(result.age?.let(::ageNumber) ?: "—",fontSize=54.sp,fontWeight=FontWeight.Bold)
                        Text("EXPERIMENTAL FITNESS AGE",fontSize=9.sp,color=AgeMuted)
                        Text(result.age?.let{"${ageNumber(abs(it-goals.age!!))} years ${if(it<=goals.age) "below" else "above"} your age"} ?: "${result.days}/7 qualifying days",fontSize=12.sp,color=AgeMint)
                    }
                }
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Text("Wellness estimate · not biological age",fontSize=10.sp,color=AgeMuted);TextButton(onClick={moving=!moving}){Text(if(moving) "Pause motion" else "Play motion",fontSize=11.sp)}}
                if(result.factors.isNotEmpty()) {
                    FactorChart(result.factors){factorIndex=it;section="Actions"}
                    TextButton(onClick={factorIndex=-1;section="Actions"}){Text("Focus: ${result.factors[FitnessCoaching.focus(result,currentRecovery)].name} ›")}
                } else CalibrationCard(result,onAge,goals.age==null)
            }
            "Progress" -> AgeHistory(days,goals.age,today)
            else -> {
                if(result.factors.isEmpty()) CalibrationCard(result,onAge,goals.age==null)
                else {
                    FactorChart(result.factors,index){factorIndex=it}
                    val factor=result.factors[index]
                    val advice=FitnessCoaching.suggestion(factor,result.factors.any{it.name=="Recovery" && it.score<34} || (currentRecovery!=null && currentRecovery<34))
                    JournalCard("YOUR NEXT SMALL STEP",FactorColors[index]) {
                        Text(advice.title,fontSize=24.sp,fontWeight=FontWeight.Bold)
                        Text("${factor.name} · ${factor.score.roundToInt()}/100",color=FactorColors[index],fontSize=13.sp)
                        Text(factor.detail,color=AgeMuted,fontSize=12.sp)
                        if(currentRecovery!=null && currentRecovery<34) Text("Recovery is low today; favour a comfortable effort.",fontSize=12.sp,color=AgeMint)
                        Text(advice.action,fontSize=16.sp)
                        FitnessExplanation("Why this suggestion",advice.why+" Suggestions support consistent habits; they do not guarantee a lower age estimate or reverse aging.")
                        val uri=LocalUriHandler.current
                        TextButton(onClick={uri.openUri(if(factor.name=="Sleep duration" || factor.name=="Recovery") "https://www.cdc.gov/sleep/about/" else "https://www.niddk.nih.gov/health-information/weight-management/tips-get-active/tips-starting-physical-activity")}){Text("Read habit guidance ↗",fontSize=12.sp)}
                    }
                }
            }
        }
        if(section!="Overview") FitnessExplanation("How the estimate works","Pulse's unvalidated wellness model uses seven qualifying days from the preceding 14; today is excluded. Movement, sleep, recovery and cardio contribute equally. Index 70 maps to your entered age; each six index points changes the illustrative age by one year, capped at ±5 years (minimum 18). It does not measure biological aging or predict lifespan. Changing personal goals does not change these references.")
    } }
}
@Composable private fun CalibrationCard(result:FitnessAgeResult,onAge:()->Unit,missingAge:Boolean) {
    JournalCard("BUILDING YOUR PICTURE") {
        LinearProgressIndicator(progress={result.days.coerceAtMost(7)/7f},modifier=Modifier.fillMaxWidth(),color=AgeMint)
        Text(result.reason,fontSize=13.sp,color=AgeMuted)
        if(missingAge) Button(onClick=onAge){Text("Add chronological age")}
        else Text("Keep syncing your watch. Each qualifying day needs steps, sleep, recovery and at least two hours of recorded HR.",fontSize=13.sp)
    }
}
@Composable private fun FactorChart(factors:List<FitnessFactor>,selected:Int=-1,onFactor:(Int)->Unit) {
    val reveal=chartEntrance()
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        factors.forEachIndexed{i,factor->Column(Modifier.weight(1f).background(Color(0xFF15211C),RoundedCornerShape(16.dp)).border(1.dp,if(i==selected) FactorColors[i] else Color.Transparent,RoundedCornerShape(16.dp)).clickable(role=Role.Button,onClickLabel="Explore ${factor.name}"){onFactor(i)}.padding(horizontal=6.dp,vertical=10.dp).semantics{contentDescription="${factor.name}: ${factor.score.roundToInt()} out of 100"},horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(5.dp)) {
            Text("${factor.score.roundToInt()}",fontSize=18.sp,fontWeight=FontWeight.Bold,color=FactorColors[i])
            Box(Modifier.height(30.dp).width(18.dp).background(Color.White.copy(alpha=.06f),RoundedCornerShape(8.dp)),contentAlignment=Alignment.BottomCenter){Box(Modifier.fillMaxWidth().fillMaxHeight((factor.score/100*reveal).toFloat().coerceIn(0f,1f)).background(FactorColors[i],RoundedCornerShape(8.dp)))}
            Text(listOf("Move","Sleep","Recover","Cardio")[i],fontSize=10.sp,color=AgeMuted,maxLines=1)
        }}
    }
}
@Composable private fun AgeHistory(days:List<ConnectedDay>,age:Int?,today:LocalDate) {
    val dates=remember(today){(13 downTo 0).map{today.minusDays(it.toLong())}}
    val values=remember(days,age,today){dates.map{FitnessAgeModel.calculate(days,age,it).age}}
    var selected by rememberSaveable{mutableIntStateOf(13)}
    val known=values.filterNotNull();val low=(known.minOrNull() ?: 18.0)-1;val high=(known.maxOrNull() ?: 19.0)+1
    val reveal=chartEntrance()
    JournalCard("14-DAY ESTIMATE HISTORY",AgeMint) {
        Text(values[selected]?.let{ageNumber(it)+" years"} ?: "Not enough data",fontSize=30.sp,fontWeight=FontWeight.Bold)
        Text(dates[selected].format(DateTimeFormatter.ofPattern("d MMM yyyy")),fontSize=12.sp,color=AgeMuted)
        Text("Scale ${ageNumber(low)}–${ageNumber(high)} years",fontSize=11.sp,color=AgeMuted)
        Canvas(Modifier.fillMaxWidth().height(160.dp).semantics{contentDescription="Fitness age history. Use date slider below."}.pointerInput(dates){detectTapGestures{selected=(it.x/size.width*14).toInt().coerceIn(0,13)}}){
            val width=size.width/14
            fun point(i:Int,v:Double)=Offset((i+.5f)*width,((high-v)/(high-low)*size.height).toFloat())
            repeat(4){i->drawLine(Color.White.copy(alpha=.07f),Offset(0f,size.height*i/3),Offset(size.width,size.height*i/3))}
            clipRect(right=size.width*reveal){values.forEachIndexed{i,v->if(v!=null){drawCircle(AgeMint,3.dp.toPx(),point(i,v));if(i>0 && values[i-1]!=null)drawLine(AgeMint,point(i-1,values[i-1]!!),point(i,v),2.dp.toPx())}}}
            drawLine(Color.White.copy(alpha=.3f),Offset((selected+.5f)*width,0f),Offset((selected+.5f)*width,size.height))
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(dates.first().format(DateTimeFormatter.ofPattern("d MMM")),fontSize=11.sp,color=AgeMuted);Text("Today",fontSize=11.sp,color=AgeMuted)}
        Slider(selected.toFloat(),{selected=it.roundToInt()},valueRange=0f..13f,steps=12,modifier=Modifier.semantics{contentDescription="Select fitness age date"})
        Text("${known.size}/14 estimates · gaps mean insufficient history",fontSize=12.sp,color=AgeMuted)
        Text("Recomputed from local records using your current entered age. Each point excludes that day's data.",fontSize=11.sp,color=AgeMuted)
    }
}

@Composable private fun AuroraOrb(modifier:Modifier,moving:Boolean) {
    val transition=rememberInfiniteTransition(label="fluid-orb")
    val phase=transition.animateFloat(0f,2*PI.toFloat(),infiniteRepeatable(tween(24000,easing=LinearEasing)),label="outward-flow")
    val particles=remember{val r=Random(165);List(180){floatArrayOf(r.nextFloat()*2*PI.toFloat(),r.nextFloat(),r.nextFloat(),r.nextFloat())}}
    var frozen by remember{mutableFloatStateOf(0f)}
    var offset by remember{mutableFloatStateOf(0f)}
    LaunchedEffect(moving){if(!moving)frozen=(phase.value+offset) else offset=frozen-phase.value}
    val entrance=chartEntrance()
    Canvas(modifier) {
        val raw=if(moving)phase.value+offset else frozen
        val t=((raw%(2*PI.toFloat()))+2*PI.toFloat())%(2*PI.toFloat())
        val radius=size.minDimension*.46f
        val path=Path()
        for(i in 0..120){val a=i*2*PI/120;val r=radius*(1+.022*sin(3*a+t)+.012*cos(5*a-2*t));val p=center+Offset((cos(a)*r).toFloat(),(sin(a)*r).toFloat());if(i==0)path.moveTo(p.x,p.y) else path.lineTo(p.x,p.y)}
        path.close()
        drawCircle(Brush.radialGradient(listOf(AgeMint.copy(alpha=.04f*entrance),Color.Transparent),center,radius*1.08f),radius*1.08f)
        drawPath(path,Brush.radialGradient(listOf(Color(0xFF0B1511),Color(0xFF102B24),AgeMint.copy(alpha=.23f*entrance)),center,radius))
        clipPath(path){
            particles.forEach{p->val travel=(p[1]+t/(2*PI.toFloat()))%1f;val a=p[0]+.16f*sin(t+p[0])+travel*.2f;val r=radius*(.1f+.9f*travel);val fade=(travel*4).coerceAtMost(1f)*((1-travel)*6).coerceAtMost(1f);drawCircle((if(p[2]>.75f)Color(0xFF9DDCFF) else AgeMint).copy(alpha=(.2f+p[2]*.55f)*fade*entrance),( .65f+p[3]*1.2f).dp.toPx(),center+Offset(cos(a)*r,sin(a)*r))}
        }
        drawPath(path,Brush.sweepGradient(listOf(AgeMint.copy(alpha=.1f),Color(0xFF9DDCFF).copy(alpha=.65f),AgeMint.copy(alpha=.15f),AgeMint.copy(alpha=.1f)),center),style=Stroke(1.2.dp.toPx()))
        drawCircle(Brush.radialGradient(listOf(Color(0xFF0A1410).copy(alpha=.97f),Color(0xFF0A1410).copy(alpha=.8f),Color.Transparent),center,radius*.72f),radius*.72f)
    }
}

@Composable private fun FitnessExplanation(label:String,text:String) {
    var expanded by remember{mutableStateOf(false)}
    TextButton(onClick={expanded=!expanded},contentPadding=PaddingValues(0.dp)){Text(if(expanded) "Hide explanation" else label,fontSize=12.sp)}
    if(expanded) Text(text,fontSize=12.sp,color=AgeMuted)
}