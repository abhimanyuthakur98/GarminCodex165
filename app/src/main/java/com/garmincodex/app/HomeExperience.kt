package com.garmincodex.app

import androidx.compose.foundation.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.*
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable internal fun HomeSections(selected:String,onSelect:(String)->Unit) {
    val p=LocalPulseStyle.current
    val tabs=listOf("Favourites" to R.drawable.symbol_star,"Activity" to R.drawable.symbol_directions_walk,"Sleep hub" to R.drawable.symbol_bedtime,"Vitals" to R.drawable.symbol_favorite,"Weekly" to R.drawable.symbol_calendar)
    Row(Modifier.fillMaxWidth().testTag("home-sections"),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        tabs.forEach { (label,icon) ->
            val active=selected==label
            val tint by androidx.compose.animation.animateColorAsState(if(active)p.primary else p.muted,label="home-category")
            Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(16.dp)).background(if(active)p.primary.copy(alpha=.18f) else p.surface).selectable(active,role=Role.Tab,onClick={onSelect(label)}).semantics{contentDescription=label},contentAlignment=Alignment.Center) {
                Icon(androidx.compose.ui.res.painterResource(icon),null,tint=tint,modifier=Modifier.size(24.dp))
            }
        }
    }
}

internal fun importStamp(day:ConnectedDay?):String = day?.updatedAt?.takeIf{it>0}?.let {
    "Imported "+Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM · HH:mm"))
} ?: "Not imported yet"

internal fun metricSource(day:ConnectedDay?,kind:String):String = when(kind) {
    "activity","steps","calories","energy" -> if(day==null || day.activitySource=="legacy") "Activity source not confirmed" else StepWindows.label(day.activitySource)+" · Health Connect"
    "recovery","strain" -> "Pulse estimate · imported measurements"
    else -> "Health Connect"
}

@Composable internal fun DataFreshness(day:ConnectedDay?,sync:SyncState,kind:String) {
    val p=LocalPulseStyle.current
    Column(verticalArrangement=Arrangement.spacedBy(3.dp)) {
        Text(metricSource(day,kind),fontSize=11.sp,color=p.muted)
        if(sync.lastError.isNotBlank()) Text("Last refresh failed · showing saved records",fontSize=11.sp,color=p.warm)
        if(unavailableForMetric(sync,kind)) Text("Last import incomplete · saved values may be older",fontSize=11.sp,color=p.warm)
        Text(importStamp(day)+(if(sync.busy)" · refreshing" else ""),fontSize=10.sp,color=p.muted)
        if(kind=="hr") Text(day?.lastSample?.let { "Last recorded sample "+Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM · HH:mm")) } ?: "No recorded sample time",fontSize=10.sp,color=p.muted)
    }
}

@Composable internal fun FavouriteCards(day:ConnectedDay?,sync:SyncState,goals:PersonalGoals,need:Int,onDetail:(String)->Unit,onConnection:()->Unit,history:List<ConnectedDay>,preferences:DashboardPreferences) {
    val prefs=preferences.normalized()
    val cards=prefs.homeOrder.filter{it !in prefs.homeHidden}
    if(cards.isEmpty()) Text("Your favourites are empty. Hold here to add cards, or open Settings → Arrange Home.",color=LocalPulseStyle.current.muted)
    var position=0
    while(position<cards.size) {
        val id=cards[position]
        if(id in setOf("activity","sleep","week")) {
            when(id) {
                "activity" -> {
                    Column(verticalArrangement=Arrangement.spacedBy(3.dp)) {
                        Text("Daily activity",fontWeight=FontWeight.SemiBold)
                        Text(dailyTakeaway(day,sync,goals),fontSize=12.sp,color=LocalPulseStyle.current.muted)
                    }
                    ActivityTiles(day,goals,chartEntrance(day?.date ?: ""),onDetail,sync)
                    ActivityImportStatus(day,sync,onConnection)
                }
                "sleep" -> SleepHero(day,need,compact=true){onDetail("sleep")}
                "week" -> WeekActivity(history,day?.date ?: LocalDate.now().toString(),goals)
            }
            position++
        } else {
            val next=cards.getOrNull(position+1)?.takeIf{it !in setOf("activity","sleep","week")}
            if(id=="recovery") Text("Health at a glance",fontSize=16.sp,fontWeight=FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                FavouriteStat(id,day,Modifier.weight(1f).fillMaxHeight(),onDetail)
                if(next!=null) FavouriteStat(next,day,Modifier.weight(1f).fillMaxHeight(),onDetail) else Spacer(Modifier.weight(1f))
            }
            position+=if(next==null)1 else 2
        }
    }
}

@Composable private fun FavouriteStat(id:String,day:ConnectedDay?,modifier:Modifier,onDetail:(String)->Unit) {
    val kind=if(id=="heart")"hr" else id
    val metric=detailMetric(kind)
    val value=day?.let(metric::value)
    OverviewStat(DashboardPreferences.homeName(id),if(value==null)"—" else metric.format(value),when(id){"recovery","strain"->"Pulse estimate ›";"heart"->"Daily average ›";"energy"->"Total burned ›";else->if(value==null)"Not shared" else "Daily measurement ›"},modifier){onDetail(kind)}
}

@Composable internal fun HomeEditor(preferences:DashboardPreferences,onSave:(DashboardPreferences)->Unit,onClose:()->Unit) {
    val p=LocalPulseStyle.current
    val prefs=preferences.normalized()
    val latest by rememberUpdatedState(prefs)
    val save by rememberUpdatedState(onSave)
    val rowHeight=with(LocalDensity.current){64.dp.toPx()}
    var draggingId by remember{mutableStateOf<String?>(null)}
    val listState=rememberLazyListState()
    Dialog(onDismissRequest=onClose,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxSize().safeDrawingPadding(),color=p.background) {
            Column(Modifier.padding(20.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("Edit home",fontSize=26.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));TextButton(onClick=onClose){Text("Done")}}
                Text("Choose your favourites. Hold and drag a handle to reorder, or use the arrows. Changes save immediately.",fontSize=12.sp,color=p.muted)
                LazyColumn(Modifier.weight(1f).testTag("home-favourites-list"),state=listState) {
                    itemsIndexed(prefs.homeOrder,key={_,id->id}) { index,id ->
                        var measuredHeight by remember(id){mutableFloatStateOf(rowHeight)}
                        Row(Modifier.animateItem().fillMaxWidth().heightIn(min=64.dp).onSizeChanged{measuredHeight=it.height.toFloat()}.background(if(draggingId==id)p.primary.copy(alpha=.12f) else androidx.compose.ui.graphics.Color.Transparent,RoundedCornerShape(12.dp)).testTag("home-row-$id"),verticalAlignment=Alignment.CenterVertically) {
                            Text("≡",modifier=Modifier.size(48.dp).semantics{contentDescription="Drag ${DashboardPreferences.homeName(id)}"}.pointerInput(id){
                                var travel=0f
                                detectDragGesturesAfterLongPress(onDragStart={travel=0f;draggingId=id},onDragEnd={travel=0f;draggingId=null},onDragCancel={travel=0f;draggingId=null}) { change,amount ->
                                    change.consume();travel+=amount.y
                                    if(kotlin.math.abs(travel)>=measuredHeight){val direction=if(travel>0)1 else -1;save(latest.moveHome(id,direction));travel-=direction*measuredHeight}
                                }
                            }.wrapContentSize(Alignment.Center),color=p.muted)
                            Checkbox(id !in prefs.homeHidden,{visible->onSave(prefs.copy(homeHidden=if(visible)prefs.homeHidden-id else prefs.homeHidden+id))},modifier=Modifier.semantics{contentDescription="Pin ${DashboardPreferences.homeName(id)}"})
                            Text(DashboardPreferences.homeName(id),modifier=Modifier.weight(1f),fontSize=12.sp)
                            IconButton(enabled=index>0,onClick={onSave(prefs.moveHome(id,-1))},modifier=Modifier.semantics{contentDescription="Move favourite ${DashboardPreferences.homeName(id)} up"}){Text("↑")}
                            IconButton(enabled=index<prefs.homeOrder.lastIndex,onClick={onSave(prefs.moveHome(id,1))},modifier=Modifier.semantics{contentDescription="Move favourite ${DashboardPreferences.homeName(id)} down"}){Text("↓")}
                        }
                    }
                }
                TextButton(onClick={onSave(prefs.copy(homeOrder=DashboardPreferences.HOME_CARDS,homeHidden=emptySet()))}){Text("Reset favourites")}
            }
        }
    }
}

@Composable internal fun SleepHub(day:ConnectedDay?,days:List<ConnectedDay>,selected:String,goals:PersonalGoals,need:Int,onGoals:(PersonalGoals)->Unit,sync:SyncState,onDetail:()->Unit,onTarget:()->Unit) {
    Text("Your sleep",fontSize=24.sp,fontWeight=FontWeight.Bold)
    Text("Night ending on $selected",fontSize=12.sp,color=LocalPulseStyle.current.muted)
    DataFreshness(day,sync,"sleep")
    SleepHero(day,need,onClick=onDetail)
    SleepStagesCard(day)
    SleepTimingCard(days,LocalDate.parse(selected))
    SleepPlanner(days,goals,need,onGoals,LocalDate.parse(selected))
    OutlinedButton(onClick=onTarget){Text("Adjust sleep goal")}
    val nights=days.filter{it.date<=selected && it.date>=LocalDate.parse(selected).minusDays(6).toString()}.mapNotNull{it.asleep}
    JournalCard("THIS WEEK'S SLEEP") {
        Text(if(nights.isEmpty())"No recorded nights" else "Average ${nights.average().roundToInt()/60}h ${nights.average().roundToInt()%60}m",fontSize=24.sp)
        Text("${nights.count{it>=need}} of ${nights.size} recorded nights met your goal. Missing nights are excluded.",fontSize=13.sp)
        Text(if(nights.size<3)"Keep syncing after waking to build your sleep history." else "Use the bedtime plan and wake-time consistency to review your routine.",fontSize=13.sp)
    }
}

@Composable internal fun WeeklyHub(days:List<ConnectedDay>,workouts:List<Workout>,goals:PersonalGoals,need:Int) {
    var offset by rememberSaveable { mutableIntStateOf(0) }
    val end=LocalDate.now().minusWeeks(offset.toLong())
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){TextButton(onClick={offset++}){Text("Previous week")};TextButton(enabled=offset>0,onClick={offset--}){Text("Next week")}}
    WeeklyReport(days,need,end,goals)
    val sessions=workouts.filter{val date=Instant.ofEpochMilli(it.start).atZone(ZoneId.systemDefault()).toLocalDate();date>=end.minusDays(7) && date<end}
    JournalCard("GOALS & TRAINING") {
        Text("${sessions.size} workout records · ${sessions.sumOf{(it.end-it.start).coerceAtLeast(0)/60000}} minutes elapsed",fontSize=15.sp)
        Text("FIT and Health Connect records are listed separately; duplicate sessions can affect this workout summary.",fontSize=11.sp,color=LocalPulseStyle.current.muted)
    }
}

internal fun detailMetric(kind:String):TrendMetric=when(kind){"steps"->TrendMetric.STEPS;"calories"->TrendMetric.CALORIES;"energy"->TrendMetric.ENERGY;"sleep"->TrendMetric.SLEEP;"recovery"->TrendMetric.RECOVERY;"hr"->TrendMetric.DAILY_HR;"resting"->TrendMetric.RESTING_HR;"hrv"->TrendMetric.HRV;else->TrendMetric.STRAIN}

@Composable internal fun MetricNavigation(date:String,onDate:(String)->Unit,range:Int,onRange:(Int)->Unit) {
    val selected=LocalDate.parse(date)
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
        TextButton(onClick={onDate(selected.minusDays(range.toLong()).toString())},modifier=Modifier.semantics{contentDescription="Previous metric period"}){Text("‹")}
        Text(selected.format(DateTimeFormatter.ofPattern("d MMM yyyy")),fontSize=14.sp)
        TextButton(enabled=selected<LocalDate.now(),onClick={onDate(minOf(selected.plusDays(range.toLong()),LocalDate.now()).toString())},modifier=Modifier.semantics{contentDescription="Next metric period"}){Text("›")}
    }
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {listOf("Day" to 1,"Week" to 7,"Month" to 30).forEach{(title,n)->FilterChip(range==n,{onRange(n)},label={Text(title)})}}
}

@Composable internal fun ActivityImportStatus(day:ConnectedDay?,sync:SyncState,onConnection:()->Unit) {
    val p=LocalPulseStyle.current
    val missing=day?.steps==null || day.activeCalories==null
    TextButton(onClick=onConnection,contentPadding=PaddingValues(horizontal=8.dp,vertical=4.dp)) {
        Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(2.dp)) {
            Text(if(day==null || day.activitySource=="legacy") "Activity source not confirmed ›" else StepWindows.label(day.activitySource)+" · via Health Connect ›",fontSize=11.sp,color=p.muted)
            Text(if(sync.busy) "Refreshing · showing saved records" else if(sync.lastError.isNotBlank()) "Last refresh failed · review connection" else if(unavailableForMetric(sync,"activity")) "Partial import · saved values may be older" else if(missing) "Missing activity · review sharing" else importStamp(day),fontSize=10.sp,color=if(missing || sync.lastError.isNotBlank() || unavailableForMetric(sync,"activity"))p.warm else p.muted)
        }
    }
}
