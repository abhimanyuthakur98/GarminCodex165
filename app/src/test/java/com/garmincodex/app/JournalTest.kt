package com.garmincodex.app
import org.junit.Assert.*
import org.junit.Test
import java.time.*
class JournalTest {
    @Test fun patternsPairFollowingMorningAndIgnoreUnknownAnswers() {
        val start=LocalDate.of(2026,1,1)
        val entries=(0..10).map {i->JournalEntry(start.plusDays(i.toLong()).toString(),null,null,null,null,if(i==10)null else i<5,"")}
        val days=(0..10).map {i->ConnectedDay(date=start.plusDays(i+1L).toString(),asleep=if(i<5)400 else 480,recovery=if(i<5)70 else 80)}
        val pattern=JournalAnalysis.pattern(entries,days,{it.lateMeal},{it.asleep?.toDouble()})
        assertEquals(5,pattern.yesCount);assertEquals(5,pattern.noCount);assertEquals(-80.0,pattern.difference!!,.001)
        assertNull(JournalAnalysis.pattern(entries.take(9),days,{it.lateMeal},{it.asleep?.toDouble()}).difference)
    }
    @Test fun customAnswersRoundTripWithoutTreatingUnknownAsNo() {
        val values=mapOf("a" to true,"b" to false)
        assertEquals(values,JournalAnalysis.answers(JournalAnalysis.encode(values)))
        assertNull(JournalAnalysis.answers("a=Y")["b"])
    }
    @Test fun reminderUsesNextLocalTimeAcrossDst() {
        val now=ZonedDateTime.of(2026,3,7,21,0,0,0,ZoneId.of("America/New_York"))
        val next=JournalNotifications.nextTime(now,20*60)
        assertEquals(20,next.hour);assertEquals(8,next.dayOfMonth)
        assertEquals(22,Duration.between(now,next).toHours())
    }
}
