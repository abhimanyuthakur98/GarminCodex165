package com.garmincodex.app

/** Only provider argument/feature rejections are recoverable. Permission, I/O and cancellation
 * failures must still abort the transaction, retaining the previous complete import. */
internal class ImportGuard {
    val unavailable = linkedSetOf<String>()
    var stage = "permissions"
    suspend fun <T> query(label:String, fallback:T, block:suspend ()->T):T {
        stage=label
        if(label in unavailable) return fallback
        return try { block() }
        catch (_:IllegalArgumentException) { unavailable += label; fallback }
        catch (_:UnsupportedOperationException) { unavailable += label; fallback }
    }
}

internal fun preserveUnavailableRecords(fresh:ConnectedDay,previous:ConnectedDay?,unavailable:Set<String>):ConnectedDay {
    if(previous==null) return fresh
    var result=fresh
    if("HeartRateRecord" in unavailable) result=result.copy(load=previous.load,coveredMinutes=previous.coveredMinutes,hrCount=previous.hrCount,hrMin=previous.hrMin,hrMax=previous.hrMax,hrAverage=previous.hrAverage,chart=previous.chart,zones=previous.zones,lastSample=previous.lastSample,overnightHr=previous.overnightHr)
    if("SleepSessionRecord" in unavailable) result=result.copy(sleepStart=previous.sleepStart,sleepEnd=previous.sleepEnd,asleep=previous.asleep,deep=previous.deep,light=previous.light,rem=previous.rem,awake=previous.awake,unknownSleep=previous.unknownSleep,stages=previous.stages,consistency=previous.consistency,overnightHr=previous.overnightHr)
    if("HeartRateRecord" in unavailable || "SleepSessionRecord" in unavailable) result=result.copy(recovery=previous.recovery,baselineNights=previous.baselineNights)
    if("RestingHeartRateRecord" in unavailable) result=result.copy(restingHr=previous.restingHr)
    if("HeartRateVariabilityRmssdRecord" in unavailable) result=result.copy(hrv=previous.hrv)
    val oldHours=HourBucket.decode(previous.hourly).associateBy { it.start }
    result=result.copy(hourly=HourBucket.decode(result.hourly).map { h ->
        val old=oldHours[h.start]
        h.copy(steps=if("hourly steps" in unavailable)old?.steps else h.steps,active=if("hourly active calories" in unavailable)old?.active else h.active,total=if("hourly total calories" in unavailable)old?.total else h.total)
    }.joinToString(";") { it.encode() })
    return result
}
