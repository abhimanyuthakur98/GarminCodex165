package com.garmincodex.app

/** A full scan catches historical edits/deletions; routine refresh keeps the last three days current. */
internal object SyncWindow {
    fun full(requested:Boolean,lastFull:Long,now:Long,permissionsChanged:Boolean):Boolean =
        requested || permissionsChanged || lastFull<=0L || now<lastFull || now-lastFull>=24*60*60*1000L
}
