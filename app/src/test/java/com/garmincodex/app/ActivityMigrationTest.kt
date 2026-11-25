package com.garmincodex.app

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],application=Application::class)
class ActivityMigrationTest {
    @Test fun garminDefaultIsAppliedOnceAndLaterChoicesPersist() {
        val context:Context=RuntimeEnvironment.getApplication()
        val prefs=context.getSharedPreferences("health_sync",Context.MODE_PRIVATE)
        prefs.edit().clear().putString("stepSource", "com.sec.android.app.shealth").commit()
        val db=Room.inMemoryDatabaseBuilder(context,PulseDatabase::class.java).allowMainThreadQueries().build()
        try {
            val sync=HealthConnectSync(context,db.days())
            assertEquals(ActivitySource.GARMIN,sync.status.value.stepSource)
            sync.saveStepSource("com.sec.android.app.shealth")
            assertEquals("com.sec.android.app.shealth",HealthConnectSync(context,db.days()).status.value.stepSource)
            sync.saveStepSource("")
            assertEquals("",HealthConnectSync(context,db.days()).status.value.stepSource)
        } finally {db.close();prefs.edit().clear().commit()}
    }
    @Test fun upgradePreservesDailyHistoryAndJournal()=runBlocking {
        val context:Context=RuntimeEnvironment.getApplication()
        val name="source-migration-test"
        context.deleteDatabase(name)
        val path=context.getDatabasePath(name);path.parentFile!!.mkdirs()
        val schema=JSONObject(File("schemas/com.garmincodex.app.PulseDatabase/6.json").readText()).getJSONObject("database")
        SQLiteDatabase.openOrCreateDatabase(path,null).use { db ->
            val entities=schema.getJSONArray("entities")
            for(i in 0 until entities.length()) {
                val entity=entities.getJSONObject(i)
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}",entity.getString("tableName")))
            }
            val setup=schema.getJSONArray("setupQueries")
            for(i in 0 until setup.length())db.execSQL(setup.getString(i))
            db.execSQL("INSERT INTO ConnectedDay (date,load,coveredMinutes,hrCount,chart,zones,deep,light,rem,awake,unknownSleep,stages,baselineNights,updatedAt,hourly,steps) VALUES ('2026-11-25',0,0,0,'','',0,0,0,0,0,'',0,0,'',14000)")
            db.execSQL("INSERT INTO JournalEntry (date,energy,habits) VALUES ('2026-11-25',4,'')")
            db.version=6
        }
        val db=Room.databaseBuilder(context,PulseDatabase::class.java,name).allowMainThreadQueries().build()
        try {
            val day=db.days().day("2026-11-25")!!
            assertEquals(14000L,day.steps);assertEquals("legacy",day.activitySource)
            db.openHelper.readableDatabase.query("SELECT energy FROM JournalEntry WHERE date='2026-11-25'").use{assertTrue(it.moveToFirst());assertEquals(4,it.getInt(0))}
        } finally { db.close();context.deleteDatabase(name) }
    }
}
