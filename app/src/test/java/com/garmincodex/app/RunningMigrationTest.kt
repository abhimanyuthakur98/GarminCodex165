package com.garmincodex.app
import android.app.Application
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],application=Application::class)
class RunningMigrationTest {
    @Test fun versionFivePreservesWorkoutsAndJournalAndAddsNullableMetrics() {
        val context=ApplicationProvider.getApplicationContext<Application>()
        val file=context.getDatabasePath("v5-running.db");file.parentFile!!.mkdirs()
        val schema=org.json.JSONObject(java.io.File("schemas/com.garmincodex.app.PulseDatabase/5.json").readText()).getJSONObject("database").getJSONArray("entities")
        SQLiteDatabase.openOrCreateDatabase(file,null).use { db ->
            for(i in 0 until schema.length()) {
                val entity=schema.getJSONObject(i);val table=entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\u0024{TABLE_NAME}",table))
                if(table in setOf("Workout","WorkoutCheckIn","JournalEntry")) {
                    val values=android.content.ContentValues();val fields=entity.getJSONArray("fields")
                    for(j in 0 until fields.length()) {val field=fields.getJSONObject(j);if(field.optBoolean("notNull")){if(field.getString("affinity")=="TEXT")values.put(field.getString("columnName"),"kept") else values.put(field.getString("columnName"),1)}}
                    db.insertOrThrow(table,null,values)
                }
            }
            db.version=5
        }
        val room=Room.databaseBuilder(context,PulseDatabase::class.java,file.name).allowMainThreadQueries().build()
        room.openHelper.writableDatabase.query("SELECT id,distanceMeters,timerSeconds,movingSeconds,paceChart,laps FROM Workout").use{c->assertTrue(c.moveToFirst());assertEquals("kept",c.getString(0));assertTrue(c.isNull(1));assertTrue(c.isNull(2));assertTrue(c.isNull(3));assertEquals("",c.getString(4));assertEquals("",c.getString(5))}
        listOf("WorkoutCheckIn","JournalEntry").forEach{table->room.openHelper.writableDatabase.query("SELECT count(*) FROM $table").use{c->assertTrue(c.moveToFirst());assertEquals(1,c.getInt(0))}}
        room.close();context.deleteDatabase(file.name)
    }
}
