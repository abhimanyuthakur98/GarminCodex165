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
@Config(sdk = [35], application = Application::class)
class DatabaseMigrationTest {
    @Test fun upgradePreservesExistingLiveHistory() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val file = context.getDatabasePath("migration-test.db")
        file.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL("CREATE TABLE IF NOT EXISTS DailyRecord (date TEXT NOT NULL, source TEXT NOT NULL, load REAL NOT NULL, recovery INTEGER, sleepMinutes INTEGER, updatedAt INTEGER NOT NULL, PRIMARY KEY(date,source))")
            db.execSQL("INSERT INTO DailyRecord VALUES ('2026-11-09','LIVE',12.5,NULL,NULL,123)")
            db.version = 1
        }
        val room = Room.databaseBuilder(context, PulseDatabase::class.java, file.name).allowMainThreadQueries().build()
        room.openHelper.writableDatabase.query("SELECT load FROM DailyRecord WHERE source='LIVE'").use { cursor -> assertTrue(cursor.moveToFirst()); assertEquals(12.5, cursor.getDouble(0), .001) }
        room.openHelper.writableDatabase.query("SELECT count(*) FROM ConnectedDay").use { cursor -> assertTrue(cursor.moveToFirst()); assertEquals(0, cursor.getInt(0)) }
        room.close(); context.deleteDatabase(file.name)
    }
    @Test fun versionTwoImportsSurviveUpgrade() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val file = context.getDatabasePath("v2-test.db"); file.parentFile!!.mkdirs()
        val schema = org.json.JSONObject(java.io.File("schemas/com.garmincodex.app.PulseDatabase/2.json").readText()).getJSONObject("database").getJSONArray("entities")
        SQLiteDatabase.openOrCreateDatabase(file,null).use { db ->
            for (i in 0 until schema.length()) {
                val entity=schema.getJSONObject(i)
                db.execSQL(entity.getString("createSql").replace("\u0024{TABLE_NAME}",entity.getString("tableName")))
                if(entity.getString("tableName")=="ConnectedDay") {
                    val values=android.content.ContentValues(); val fields=entity.getJSONArray("fields")
                    for(j in 0 until fields.length()) { val field=fields.getJSONObject(j); if(field.optBoolean("notNull")) { if(field.getString("affinity")=="TEXT") values.put(field.getString("columnName"),"") else values.put(field.getString("columnName"),0) } }
                    values.put("date","2026-11-09"); values.put("steps",8642L); values.put("calories",2185.0)
                    db.insertOrThrow("ConnectedDay",null,values)
                }
            }
            db.version=2
        }
        val room=Room.databaseBuilder(context,PulseDatabase::class.java,file.name).allowMainThreadQueries().build()
        room.openHelper.writableDatabase.query("SELECT steps,calories,activeCalories,hourly FROM ConnectedDay").use { c -> assertTrue(c.moveToFirst()); assertEquals(8642,c.getLong(0)); assertEquals(2185.0,c.getDouble(1),.001); assertTrue(c.isNull(2)); assertEquals("",c.getString(3)) }
        room.close(); context.deleteDatabase(file.name)
    }}
