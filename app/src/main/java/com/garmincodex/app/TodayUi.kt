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
@Composable internal fun PulseHeader(busy:Boolean,onSync:()->Unit,lastSync:Long=0,phase:String="",onSettings:()->Unit={},onBack:(()->Unit)?=null) {
    val palette=LocalPulseStyle.current
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
        if(onBack!=null) IconButton(onClick=onBack,modifier=Modifier.semantics{contentDescription="Back to previous screen"}) { Icon(painterResource(R.drawable.symbol_arrow_back),null,tint=palette.primary) }
        Column(Modifier.weight(1f)) {
            Text("PULSE",fontSize=16.sp,fontWeight=FontWeight.SemiBold,letterSpacing=3.sp,modifier=Modifier.semantics{contentDescription="Pulse 165"})
            val imported=if(lastSync>0) Instant.ofEpochMilli(lastSync).atZone(ZoneId.systemDefault()) else null
            Text(if(busy) (if(lastSync>0) "Refreshing · saved stats available" else "Importing health data…") else imported?.let{"Updated "+it.format(DateTimeFormatter.ofPattern(if(it.toLocalDate()==LocalDate.now()) "HH:mm" else "d MMM · HH:mm"))} ?: "Your health, together",fontSize=10.sp,color=palette.muted)
        }
        IconButton(onClick=onSync,enabled=!busy,modifier=Modifier.semantics{contentDescription=if(busy) "Syncing health data" else "Sync health data"}) {
            Icon(painterResource(R.drawable.symbol_refresh),null,tint=if(busy)palette.muted.copy(alpha=.4f) else palette.muted,modifier=Modifier.size(22.dp))
        }
        if(onBack==null) IconButton(onClick=onSettings,modifier=Modifier.semantics{contentDescription="Open Settings"}) { DockIcon(3,palette.primary) }
    }
}
@Composable internal fun TodayDate(date:String,onDate:(String)->Unit,onJournal:()->Unit,onEdit:(()->Unit)?=null) {
    val palette = LocalPulseStyle.current
    val Soft = palette.muted

    val d=LocalDate.parse(date)
    var calendar by remember{mutableStateOf(false)}
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
        Column(Modifier.weight(1f).clickable(role=Role.Button,onClickLabel="Show date navigation"){calendar=!calendar}.padding(vertical=6.dp)) {
            Text(if(d==LocalDate.now()) "Today" else d.format(DateTimeFormatter.ofPattern("EEEE")),fontSize=30.sp,fontWeight=FontWeight.SemiBold,letterSpacing=(-1).sp)
            Text(d.format(DateTimeFormatter.ofPattern("d MMM yyyy"))+"  ▾",fontSize=11.sp,color=Soft)
        }
        if(onEdit!=null) TextButton(onClick=onEdit){Text("Edit home",fontSize=11.sp)}
        JournalAction(false,onJournal)
    }
    if(calendar) QuickWeek(date,onDate)
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
@Composable internal fun TodaySnapshot(day:ConnectedDay?,sync:SyncState,goals:PersonalGoals,sleepNeed:Int,onDetail:(String)->Unit,onConnection:()->Unit,history:List<ConnectedDay> = emptyList(),preferences:DashboardPreferences=DashboardPreferences(),cacheLoading:Boolean=false) {
    val palette = LocalPulseStyle.current
    val Lime = palette.primary
    val Orange = palette.warm
    val Soft = palette.muted
    val Mint = palette.primary
    val Lavender = palette.violet

    if(cacheLoading && day==null) {
        Column(Modifier.fillMaxWidth().background(palette.surface,RoundedCornerShape(20.dp)).padding(20.dp)) {
            Text("Loading saved health data…",fontSize=16.sp,fontWeight=FontWeight.Medium)
            Text("Your dashboard will appear here",fontSize=12.sp,color=Soft)
        };return
    }
    val needsAttention=listOf("Sync interrupted", "Health access changed", "Health Connect is unavailable", "Allow Pulse", "Could not revoke").any{sync.message.startsWith(it)}
    if(!sync.busy && (needsAttention || sync.lastError.isNotBlank() || sync.unavailable.isNotEmpty())) TextButton(onClick=onConnection){Text("Sync needs attention · review connection ›",color=Orange,fontSize=12.sp)}
    if(day==null || day.hrCount==0 && day.asleep==null && day.steps==null && day.activeCalories==null && day.recovery==null) {
        SnapshotCard(onClick=onConnection) {
            Text(if(sync.lastSync==0L) "Your day starts here" else "No records for this day",fontSize=24.sp,fontWeight=FontWeight.Bold)
            Text(if(sync.lastSync==0L) "Connect your device to see your sleep, recovery and movement." else "Sync your watch in its companion app, then refresh Pulse.",color=Soft,fontSize=14.sp)
            Text("${if(sync.lastSync==0L) "Connect device" else "Check connection"} ›",color=Mint,fontSize=14.sp)
        }
        return
    }
    FavouriteCards(day,sync,goals,sleepNeed,onDetail,onConnection,history,preferences)
}
