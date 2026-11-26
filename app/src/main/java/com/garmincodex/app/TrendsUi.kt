package com.garmincodex.app

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

internal enum class TrendMetric(val title:String,val unit:String) {
    SLEEP("Sleep","min"),RECOVERY("Recovery","%"),STRAIN("Strain","/21"),STEPS("Steps","steps"),CALORIES("Active calories","kcal"),HR("Overnight HR","bpm"),DAILY_HR("Heart rate","bpm"),RESTING_HR("Resting HR","bpm"),HRV("HRV","ms"),ENERGY("Total energy","kcal");
    fun value(d:ConnectedDay):Double?=when(this){SLEEP->d.asleep?.toDouble();RECOVERY->d.recovery?.toDouble();STRAIN->if(d.coveredMinutes>0)Scores.strain(d.load) else null;STEPS->d.steps?.toDouble();CALORIES->d.activeCalories;HR->d.overnightHr;DAILY_HR->d.hrAverage?.toDouble();RESTING_HR->d.restingHr;HRV->d.hrv;ENERGY->d.calories}
    fun format(value:Double?):String=if(value==null) "No record" else when(this){SLEEP->"${value.toInt()/60}h ${value.toInt()%60}m";STRAIN->String.format(java.util.Locale.US,"%.1f /21",value);else->"${value.roundToInt()} $unit"}
}
@Composable internal fun TrendsExplorer(days:List<ConnectedDay>,onDay:(String)->Unit) {
    val palette = LocalPulseStyle.current
    val Lime = palette.primary

    var metricName by rememberSaveable{mutableStateOf(TrendMetric.SLEEP.name)}
    val metric=TrendMetric.valueOf(metricName)
    var mode by rememberSaveable{mutableStateOf("History")}
    var period by rememberSaveable{mutableIntStateOf(7)}
    var menu by remember{mutableStateOf(false)}
    var periodMenu by remember{mutableStateOf(false)}
    Text("Trends",fontSize=28.sp,fontWeight=FontWeight.Bold)
    Text("See what is changing over time",color=palette.muted,fontSize=13.sp)
    TrendOverview(days)
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
        Box {OutlinedButton(onClick={menu=true}){Text(metric.title+" ▾")};DropdownMenu(menu,{menu=false}){TrendMetric.entries.forEach{m->DropdownMenuItem(text={Text(m.title)},onClick={metricName=m.name;menu=false})}}}
        if(mode!="Weekly") Box {OutlinedButton(onClick={periodMenu=true}){Text("$period days ▾")};DropdownMenu(periodMenu,{periodMenu=false}){listOf(7,14,30).forEach{n->DropdownMenuItem(text={Text("$n days")},onClick={period=n;periodMenu=false})}}}
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {listOf("History","Usual range","Weekly").forEach{m->FilterChip(mode==m,{mode=m},label={Text(m,fontSize=12.sp)})}}
    key(metric,mode,period) {
        Column(pageMotion(Triple(metric,mode,period)),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            if(mode=="Weekly") {
                val report=DailyCoach.weekly(days,LocalDate.now(),metric::value)
                JournalCard("WEEKLY AVERAGES") {
                    Text(metric.format(report.current),fontSize=32.sp,fontWeight=FontWeight.Bold)
                    MetricBars(listOf(Triple("Last 7 days",report.current,metric.format(report.current)),Triple("Previous 7",report.previous,metric.format(report.previous))))
                    Text("${report.currentDays}/7 days recorded · previous ${report.previousDays}/7",color=Color.Gray,fontSize=12.sp)
                    Text(if(report.currentDays==7 && report.previousDays==7) "7 recorded days in each week" else "Partial weeks · compare with care",color=Lime,fontSize=12.sp)
                    Text("Today excluded",color=Color.Gray,fontSize=11.sp)
                }
            } else {
                TrendPlot(days,metric,period,mode=="Usual range",onDay)
            }
        }
    }
}
@Composable internal fun TrendPlot(days:List<ConnectedDay>,metric:TrendMetric,period:Int,baseline:Boolean,onDay:(String)->Unit,endDate:LocalDate=LocalDate.now()) {
    val palette = LocalPulseStyle.current
    val Lime = palette.primary

    val today=LocalDate.now()
    val dates=remember(endDate,period){(period-1 downTo 0).map{endDate.minusDays(it.toLong())}}
    val byDate=remember(days){days.distinctBy{it.date}.associateBy{it.date}}
    val values=remember(byDate,dates,metric){dates.map{byDate[it.toString()]?.let(metric::value)}}
    val bands=remember(days,dates,metric,baseline){dates.map{if(baseline)PersonalBaseline.calculate(days,it,metric::value) else null}}
    var selected by rememberSaveable(metric,period,baseline,endDate){mutableIntStateOf(period-1)}
    val date=dates[selected]
    val known=values.filterNotNull()
    val range=known+bands.filterNotNull().flatMap{listOf(it.lower,it.upper)}
    val max=(range.maxOrNull() ?: 1.0).coerceAtLeast(1.0)*1.1
    val reveal=chartEntrance()
    Column(Modifier.fillMaxWidth().background(palette.surface,androidx.compose.foundation.shape.RoundedCornerShape(24.dp)).padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(metric.format(values[selected]),fontSize=28.sp,fontWeight=FontWeight.Bold,color=Lime)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(date.format(DateTimeFormatter.ofPattern("d MMM"))+if(date==today) " · so far" else "",fontSize=12.sp,color=Color.Gray,modifier=Modifier.padding(top=12.dp));TextButton(onClick={onDay(date.toString())},enabled=byDate[date.toString()]!=null){Text("Open day ›")}}
        Text(metricSource(byDate[date.toString()],metricKind(metric)),fontSize=11.sp,color=palette.muted)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(metric.unit,fontSize=10.sp,color=Color.Gray);Text("Scale 0–${max.roundToInt()} ${metric.unit}",fontSize=10.sp,color=Color.Gray)}
        Canvas(Modifier.fillMaxWidth().height(140.dp).semantics{contentDescription="${metric.title} trend chart. Use date slider below."}.chartScrub(Triple(metric,period,endDate)){selected=chartIndex(it,period)}) {
            val width=size.width/period
            fun y(value:Double)=size.height*(1-value/max).toFloat()
            repeat(4){i->drawLine(Color.White.copy(alpha=.06f),Offset(0f,size.height*i/3),Offset(size.width,size.height*i/3))}
            drawLine(Color.White.copy(alpha=.2f),Offset((selected+.5f)*width,0f),Offset((selected+.5f)*width,size.height))
            clipRect(right=size.width*reveal){
                dates.indices.forEach{i->
                    bands[i]?.let{b->drawRect(palette.violet.copy(alpha=.25f),Offset(i*width+1,y(b.upper)),androidx.compose.ui.geometry.Size((width-2).coerceAtLeast(1f),(y(b.lower)-y(b.upper)).coerceAtLeast(2f)))}
                    values[i]?.let{v->drawCircle(Lime,3.dp.toPx(),Offset((i+.5f)*width,y(v)))}
                    if(i>0 && values[i]!=null && values[i-1]!=null)drawLine(Lime,Offset((i-.5f)*width,y(values[i-1]!!)),Offset((i+.5f)*width,y(values[i]!!)),2.dp.toPx())
                }
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(dates.first().format(DateTimeFormatter.ofPattern("d MMM")),fontSize=11.sp,color=Color.Gray);Text(if(endDate==today)"Today" else endDate.format(DateTimeFormatter.ofPattern("d MMM")),fontSize=11.sp,color=Color.Gray)}
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            TextButton(enabled=selected>0,onClick={selected--},modifier=Modifier.semantics{contentDescription="Previous chart day"}){Text("‹ Previous")}
            TextButton(enabled=selected<dates.lastIndex,onClick={selected++},modifier=Modifier.semantics{contentDescription="Next chart day"}){Text("Next ›")}
        }
        Slider(selected.toFloat(),{selected=it.roundToInt()},valueRange=0f..(period-1).toFloat(),steps=period-2,modifier=Modifier.semantics{contentDescription="Select trend date"})
        if(baseline) Text(bands[selected]?.let{"Usual: ${metric.format(it.lower)} – ${metric.format(it.upper)}"} ?: "Needs 7 prior recorded days",fontSize=12.sp,color=palette.violet)
        else Text("${known.size}/$period days · average ${metric.format(known.takeIf{it.isNotEmpty()}?.average())}",fontSize=12.sp,color=Color.Gray)
        ChartExplanation(if(baseline) "Shading is the middle 50% of the preceding 14 days, requiring 7 values. It describes your history, not a clinical healthy range. Missing days remain gaps." else "Tap, drag horizontally or use the date controls to inspect a date. Missing days remain gaps; zero values are real records. Only locally imported history appears. Today may be incomplete.")
    }
}
