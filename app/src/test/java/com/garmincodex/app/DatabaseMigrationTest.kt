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
}
