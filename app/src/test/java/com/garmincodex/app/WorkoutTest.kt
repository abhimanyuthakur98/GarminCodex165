package com.garmincodex.app

import com.garmin.fit.*
import org.junit.Assert.*
import org.junit.Test

class WorkoutTest {
    @Test fun fitSessionsDecodeAndReimportHasStableIdentity() {
        val file=java.io.File.createTempFile("pulse-fixture", ".fit")
        try {
            val encoder=FileEncoder(file,Fit.ProtocolVersion.V2_0)
            val start=DateTime(1000000000L)
            val fileId=FileIdMesg(); fileId.type=com.garmin.fit.File.ACTIVITY; fileId.manufacturer=Manufacturer.GARMIN; fileId.timeCreated=start
            encoder.write(fileId)
            repeat(10) { i -> val point=RecordMesg();point.timestamp=DateTime(1000000000L+i*60);point.heartRate=(100+i).toShort();encoder.write(point) }
            val session=SessionMesg(); session.startTime=start;session.timestamp=DateTime(1000000600L);session.sport=Sport.RUNNING;session.totalCalories=120;encoder.write(session);encoder.close()
            val bytes=file.readBytes()
            val first=FitWorkoutReader.read(bytes).single()
            assertEquals(first,FitWorkoutReader.read(bytes).single())
            assertEquals("RUNNING",first.type)
            assertEquals(120.0,first.calories!!,.01)
            assertEquals(10,WorkoutAnalysis.points(first).size)
            assertEquals(600000,first.end-first.start)
            bytes[bytes.lastIndex]=(bytes.last().toInt() xor 1).toByte()
            assertThrows(IllegalArgumentException::class.java){FitWorkoutReader.read(bytes)}
        } finally {file.delete()}
    }
    @Test fun malformedFitIsRejectedAndComparisonRespectsSourceAndType() {
        assertThrows(IllegalArgumentException::class.java){FitWorkoutReader.read(byteArrayOf(1,2,3))}
        val current=Workout("c","FIT","RUNNING","Run",10000,20000,"")
        val previous=current.copy(id="p",start=1000,end=2000)
        assertEquals(previous,WorkoutAnalysis.previous(current,listOf(previous,current.copy(id="other",source="HEALTH_CONNECT",start=9000),current.copy(id="cycle",type="CYCLING",start=9500))))
    }
}
