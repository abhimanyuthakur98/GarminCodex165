package com.garmincodex.app

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity data class JournalEntry(@PrimaryKey val date:String,val energy:Int?,val stress:Int?,val soreness:Int?,val caffeine:String?,val lateMeal:Boolean?,val habits:String)
@Entity data class HabitTag(@PrimaryKey val id:String,val name:String,val archived:Boolean=false)
data class HabitPattern(val yesCount:Int,val noCount:Int,val difference:Double?)
object JournalAnalysis {
    fun answers(raw:String):Map<String,Boolean> = raw.split(';').mapNotNull { val p=it.split('=');if(p.size==2 && p[1] in listOf("Y","N")) p[0] to (p[1]=="Y") else null }.toMap()
    fun encode(values:Map<String,Boolean>)=values.toSortedMap().entries.joinToString(";"){"${it.key}=${if(it.value) "Y" else "N"}"}
    fun validate(entry:JournalEntry) {
        require(LocalDate.parse(entry.date)<=LocalDate.now())
        require(listOf(entry.energy,entry.stress,entry.soreness).all {it==null || it in 1..5})
        require(entry.caffeine==null || entry.caffeine in listOf("None","Morning","Afternoon","Evening"))
    }
    fun pattern(entries:List<JournalEntry>,days:List<ConnectedDay>,habit:(JournalEntry)->Boolean?,value:(ConnectedDay)->Double?):HabitPattern {
        val data=days.associateBy {it.date}
        val pairs=entries.distinctBy {it.date}.mapNotNull { e -> val answer=habit(e) ?: return@mapNotNull null; val next=data[LocalDate.parse(e.date).plusDays(1).toString()] ?: return@mapNotNull null; value(next)?.takeIf {it.isFinite()}?.let {answer to it} }
        val yes=pairs.filter {it.first}.map {it.second};val no=pairs.filterNot {it.first}.map {it.second}
        return HabitPattern(yes.size,no.size,if(yes.size>=5 && no.size>=5) yes.average()-no.average() else null)
    }
}
