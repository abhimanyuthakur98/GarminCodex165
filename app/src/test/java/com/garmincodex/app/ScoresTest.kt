package com.garmincodex.app
import org.junit.Assert.*
import org.junit.Test
class ScoresTest {
    @Test fun strainIsBoundedAndMonotonic() {
        assertEquals(0.0, Scores.strain(0.0), .001)
        assertTrue(Scores.strain(200.0) > Scores.strain(100.0))
        assertTrue(Scores.strain(1e9) <= 21)
        assertEquals(0.0, Scores.load(55, 10.0), .001)
        assertEquals(Scores.load(180, 10.0), Scores.load(180, 900.0), .001)
    }
    @Test fun recoveryFallsWithPhysiologicalStress() {
        val n = Night(65.0, 60.0, 53, 55, 90, 240, 100, 20, 480, 90, 85)
        assertTrue(Scores.recovery(n) > Scores.recovery(n.copy(hrv = 35.0, rhr = 65, awake = 180, bodyBattery = 20)))
        assertEquals("LOW", Scores.tier(33)); assertEquals("MODERATE", Scores.tier(34)); assertEquals("READY", Scores.tier(67))
    }
    @Test fun parsesWideHeartRateAndRr() {
        val s = HeartRatePacket.parse(byteArrayOf(0x19, 180.toByte(), 0, 0, 0, 0, 4), 123)!!
        assertEquals(180, s.hr); assertEquals(1000.0, s.rrMs.single(), .001)
    }
    @Test fun rejectsTruncationAndLostContact() {
        assertNull(HeartRatePacket.parse(byteArrayOf(1, 60), 0))
        assertNull(HeartRatePacket.parse(byteArrayOf(16, 60, 1), 0))
        assertNull(HeartRatePacket.parse(byteArrayOf(4, 60), 0))
        assertEquals(60, HeartRatePacket.parse(byteArrayOf(0, 60), 0)!!.hr)
    }
}
