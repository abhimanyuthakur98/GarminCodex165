package com.garmincodex.app

import org.junit.Assert.*
import org.junit.Test

class SourceSelectionTest {
    private data class Session(val source:String,val start:Long,val end:Long,val type:Int=1)
    @Test fun supportsAnyPackageAndDeterministicCoverageSelection() {
        val samsung=Session("com.sec.android.app.shealth",0,100)
        val other=Session("any.vendor.app",0,200)
        assertEquals(listOf(other),SourceSelection.primary(listOf(samsung,other),{it.source},{it.end-it.start}))
        assertEquals(listOf(samsung),SourceSelection.primary(listOf(samsung),{it.source},{it.end-it.start}))
    }
    @Test fun mirroredWorkoutsAreRemovedButSeparateSessionsAndTypesRemain() {
        val a=Session("a",0,100)
        val mirror=Session("b",5,105)
        val separate=Session("b",110,200)
        val otherType=Session("b",0,100,2)
        val input=listOf(a,mirror,separate,otherType)
        val result=SourceSelection.workouts(input,{it.source},{it.start},{it.end},{it.type})
        assertEquals(3,result.size);assertFalse(result.contains(mirror))
        assertEquals(result,SourceSelection.workouts(input.reversed(),{it.source},{it.start},{it.end},{it.type}))
        assertEquals(2,SourceSelection.workouts(listOf(a,a.copy(start=5)),{it.source},{it.start},{it.end},{it.type}).size)
    }
}
