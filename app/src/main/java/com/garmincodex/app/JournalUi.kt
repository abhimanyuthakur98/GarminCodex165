package com.garmincodex.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.*
import java.util.UUID

private val JournalMuted=Color(0xFFA4B2AB)
@Composable internal fun JournalPage(entries:List<JournalEntry>,tags:List<HabitTag>,days:List<ConnectedDay>,workouts:List<Workout>,onSave:(JournalEntry)->Unit,onTag:(HabitTag)->Unit,onDelete:(String)->Unit,onClear:()->Unit,reminder:JournalReminder,onReminder:(JournalReminder)->Unit) {
    var date by rememberSaveable {mutableStateOf(LocalDate.now().toString())}
    var tab by rememberSaveable {mutableIntStateOf(0)}
    var month by rememberSaveable {mutableStateOf(YearMonth.now().toString())}
    var clear by remember {mutableStateOf(false)}
    Text("Small habits.\nYour bigger picture.",fontSize=32.sp,lineHeight=35.sp,fontWeight=FontWeight.Bold)
    Text("A moment to notice what matters.",color=JournalMuted,fontSize=14.sp)
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {listOf("Check-in","Patterns","Calendar").forEachIndexed {i,title->FilterChip(tab==i,{tab=i},label={Text(title)})}}
    when(tab) {
        0 -> {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {TextButton(onClick={date=LocalDate.parse(date).minusDays(1).toString()}){Text("Previous day")};Text(date,fontSize=13.sp);TextButton(enabled=LocalDate.parse(date)<LocalDate.now(),onClick={date=LocalDate.parse(date).plusDays(1).toString()}){Text("Next day")}}
            key(date) { JournalForm(date,entries.firstOrNull{it.date==date},tags,onSave,onTag,onDelete) }
            ReminderCard(reminder,onReminder)
        }
        1 -> {
            JournalCard("PATTERNS, NOT PROOF",Color(0xFFA9B6FF)) {
                Text("Your habits & next morning",fontSize=24.sp,fontWeight=FontWeight.Bold)
                Text("Last 90 logged days, paired with the following morning. Differences appear after 5 yes and 5 no days with an outcome. Unanswered questions are excluded. These associations can reflect other factors; they do not establish cause.",fontSize=12.sp,color=JournalMuted)
            }
            val recent=entries.filter {LocalDate.parse(it.date)>=LocalDate.now().minusDays(89)}
            val habits:List<Pair<String,(JournalEntry)->Boolean?>> = listOf("Afternoon/evening caffeine" to {e:JournalEntry -> e.caffeine?.let{it in listOf("Afternoon","Evening")}},"Late meal" to {e:JournalEntry->e.lateMeal}) + tags.map { tag -> tag.name to { e:JournalEntry -> JournalAnalysis.answers(e.habits)[tag.id] } }
            habits.forEach { (name,answer)-> JournalCard(name.uppercase()) {
                listOf(Triple("Sleep","min",{d:ConnectedDay->d.asleep?.toDouble()}),Triple("Recovery","points",{d:ConnectedDay->d.recovery?.toDouble()})).forEach { (label,unit,value)->
                    val result=JournalAnalysis.pattern(recent,days,answer,value)
                    Text("$label · ${result.yesCount} yes / ${result.noCount} no",fontWeight=FontWeight.SemiBold)
                    Text(result.difference?.let {"${String.format(java.util.Locale.US,"%+.1f",it)} $unit after yes versus no"} ?: "More paired entries needed",color=if(result.difference!=null)Lime else JournalMuted,fontSize=13.sp)
                }
            } }
        }
        2 -> {
            val current=YearMonth.parse(month)
            JournalCard("YOUR MONTH IN COLOUR") {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {TextButton(onClick={month=current.minusMonths(1).toString()}){Text("‹")};Text(current.toString(),fontSize=22.sp);TextButton(enabled=current<YearMonth.now(),onClick={month=current.plusMonths(1).toString()}){Text("›")}}
                Row(Modifier.fillMaxWidth()) {listOf("M","T","W","T","F","S","S").forEach {Text(it,Modifier.weight(1f),color=JournalMuted,fontSize=11.sp)}}
                val offset=current.atDay(1).dayOfWeek.value-1
                (0 until ((offset+current.lengthOfMonth()+6)/7)).forEach {week->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                    (0..6).forEach {weekday-> val n=week*7+weekday-offset+1
                        if(n !in 1..current.lengthOfMonth()) Spacer(Modifier.weight(1f)) else {
                            val day=current.atDay(n);val key=day.toString();val recovery=days.firstOrNull{it.date==key}?.recovery
                            val color=when {recovery==null->Color(0xFF29312E);recovery<34->Color(0xFFAD4F60);recovery<67->Color(0xFF9A853B);else->Color(0xFF347A5F)}
                            Column(Modifier.weight(1f).background(color,RoundedCornerShape(10.dp)).clickable(enabled=day<=LocalDate.now()){date=key;tab=0}.padding(vertical=9.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                                Text(n.toString(),fontSize=12.sp,color=if(day>LocalDate.now())Color.Gray else Color.White)
                                Text((if(entries.any{it.date==key}) "•" else " ")+(if(workouts.any {Instant.ofEpochMilli(it.start).atZone(ZoneId.systemDefault()).toLocalDate()==day}) "◆" else " "),fontSize=9.sp,color=Lime)
                            }
                        }
                    }
                } }
                Text("Colour = recovery tier · grey = unavailable\n• journal   ◆ workout\nTap a past day to review or edit its check-in.",color=JournalMuted,fontSize=12.sp)
            }
        }
    }
    TextButton(onClick={clear=true}){Text("Clear journal & custom habits",color=Color(0xFFFF8995))}
    if(clear) AlertDialog(onDismissRequest={clear=false},title={Text("Clear your journal?")},text={Text("Deletes all daily check-ins and custom habits on this phone. Watch data and workout ratings stay intact.")},confirmButton={TextButton(onClick={onClear();clear=false}){Text("Delete journal")}},dismissButton={TextButton(onClick={clear=false}){Text("Cancel")}})
}
@Composable private fun JournalForm(date:String,entry:JournalEntry?,tags:List<HabitTag>,onSave:(JournalEntry)->Unit,onTag:(HabitTag)->Unit,onDelete:(String)->Unit) {
    var energy by remember(entry){mutableStateOf(entry?.energy)};var stress by remember(entry){mutableStateOf(entry?.stress)};var sore by remember(entry){mutableStateOf(entry?.soreness)}
    var caffeine by remember(entry){mutableStateOf(entry?.caffeine)};var late by remember(entry){mutableStateOf(entry?.lateMeal)}
    var answers by remember(entry){mutableStateOf(JournalAnalysis.answers(entry?.habits ?: ""))}
    var name by remember {mutableStateOf("")}
    var delete by remember {mutableStateOf(false)}
    JournalCard("DAILY CHECK-IN") {
        Text("How did today feel?",fontSize=24.sp,fontWeight=FontWeight.Bold)
        RatingRow("Energy",energy,"1 depleted · 5 full of energy"){energy=it}
        RatingRow("Stress",stress,"1 calm · 5 very stressed"){stress=it}
        RatingRow("Soreness",sore,"1 none · 5 very sore"){sore=it}
        Text("Last caffeine",fontWeight=FontWeight.SemiBold)
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {listOf("None","Morning","Afternoon","Evening").forEach {v->FilterChip(caffeine==v,{caffeine=if(caffeine==v)null else v},label={Text(v)})}}
        Text("A meal within 2 hours of bedtime?",fontSize=13.sp)
        AnswerRow("Late meal",late){late=it}
        tags.filterNot{it.archived}.forEach {tag->Text(tag.name,fontWeight=FontWeight.SemiBold);AnswerRow(tag.name,answers[tag.id]){v->answers=if(v==null)answers-tag.id else answers+(tag.id to v)};TextButton(onClick={onTag(tag.copy(archived=true))}){Text("Archive ${tag.name}",fontSize=11.sp)}}
        OutlinedTextField(name,{name=it.take(30)},modifier=Modifier.fillMaxWidth().testTag("habit-name"),singleLine=true,label={Text("Add a custom habit")})
        TextButton(enabled=name.isNotBlank() && tags.none{it.name.equals(name.trim(),true)} && tags.count{!it.archived}<12,onClick={onTag(HabitTag(UUID.randomUUID().toString(),name.trim()));name=""}){Text("Add habit")}
        tags.filter { it.archived }.forEach { tag -> TextButton(enabled=tags.count { !it.archived }<12,onClick={onTag(tag.copy(archived=false))}) { Text("Restore ${tag.name}",fontSize=11.sp) } }
        val draft=JournalEntry(date,energy,stress,sore,caffeine,late,JournalAnalysis.encode(answers))
        Button(enabled=listOf(energy,stress,sore,caffeine,late).any{it!=null} || answers.isNotEmpty(),onClick={onSave(draft)},modifier=Modifier.fillMaxWidth()){Text(if(entry==draft) "Check-in saved" else "Save daily check-in")}
        Text("Save before changing days or tabs. Tap a selected answer to clear it. Blank means unknown, never no. Saved habits describe this calendar day; patterns use the next morning's sleep/recovery.",color=JournalMuted,fontSize=11.sp)
        if(entry!=null) {if(delete) Row {TextButton(onClick={onDelete(date);delete=false}){Text("Confirm delete entry")};TextButton(onClick={delete=false}){Text("Cancel")}} else TextButton(onClick={delete=true}){Text("Delete this entry")}}
    }
}
@Composable private fun RatingRow(label:String,value:Int?,hint:String,onChange:(Int?)->Unit) {
    Text(label,fontWeight=FontWeight.SemiBold);Text(hint,color=JournalMuted,fontSize=11.sp)
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){(1..5).forEach {n->OutlinedButton(onClick={onChange(if(value==n)null else n)},modifier=Modifier.weight(1f).testTag("$label-$n"),contentPadding=PaddingValues(0.dp),colors=ButtonDefaults.outlinedButtonColors(containerColor=if(value==n)Lime else Color.Transparent,contentColor=if(value==n)Color.Black else Color.White)){Text(n.toString())}}}
}
@Composable private fun AnswerRow(label:String,value:Boolean?,onChange:(Boolean?)->Unit) {Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf(true,false).forEach{v->FilterChip(value==v,{onChange(if(value==v)null else v)},label={Text(if(v) "Yes" else "No")},modifier=Modifier.testTag("$label-$v"))}}}
@Composable private fun ReminderCard(reminder:JournalReminder,onChange:(JournalReminder)->Unit) {
    var time by remember(reminder.minute){mutableStateOf("%02d:%02d".format(reminder.minute/60,reminder.minute%60))}
    val parsed=DailyCoach.parseWake(time)
    JournalCard("A GENTLE NUDGE",Color(0xFFA9B6FF)) {
        Text("Your daily reminder",fontSize=22.sp,fontWeight=FontWeight.Bold)
        OutlinedTextField(time,{time=it.take(5)},label={Text("Local time · HH:mm")},singleLine=true,modifier=Modifier.fillMaxWidth().testTag("reminder-time"),isError=parsed==null)
        Button(enabled=parsed!=null,onClick={parsed?.let{onChange(JournalReminder(true,it.hour*60+it.minute))}}){Text(if(reminder.enabled) "Update reminder" else "Enable reminder")}
        if(reminder.enabled) TextButton(onClick={onChange(reminder.copy(enabled=false))}){Text("Turn off reminder")}
        Text(if(reminder.enabled) "Scheduled around ${"%02d:%02d".format(reminder.minute/60,reminder.minute%60)}. Android may delay delivery. Notification permission and channel settings must allow it." else "Off by default. Notifications only start after you enable them.",color=JournalMuted,fontSize=12.sp)
    }
}
