package com.garmincodex.app

import android.app.*
import android.content.*
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import java.time.*
import java.util.concurrent.TimeUnit

data class JournalReminder(val enabled:Boolean=false,val minute:Int=1200)
object JournalNotifications {
    const val TAG="pulse-journal-reminder"
    fun nextTime(now:ZonedDateTime,minute:Int):ZonedDateTime {
        require(minute in 0..1439)
        val today=now.toLocalDate().atTime(minute/60,minute%60).atZone(now.zone)
        return if(today>now) today else now.toLocalDate().plusDays(1).atTime(minute/60,minute%60).atZone(now.zone)
    }
    fun save(context:Context,value:JournalReminder) {
        require(value.minute in 0..1439)
        context.getSharedPreferences("journal",0).edit().putBoolean("enabled",value.enabled).putInt("minute",value.minute).apply()
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
        if(value.enabled) schedule(context,value.minute)
        else NotificationManagerCompat.from(context).cancel(170)
    }
    fun read(context:Context):JournalReminder { val p=context.getSharedPreferences("journal",0);return JournalReminder(p.getBoolean("enabled",false),p.getInt("minute",1200)) }
    fun schedule(context:Context,minute:Int) {
        val now=ZonedDateTime.now();val next=nextTime(now,minute)
        val work=OneTimeWorkRequestBuilder<JournalReminderWorker>().setInitialDelay(Duration.between(now,next).toMillis(),TimeUnit.MILLISECONDS).addTag(TAG).build()
        WorkManager.getInstance(context).enqueueUniqueWork("$TAG-${next.toInstant().toEpochMilli()}",ExistingWorkPolicy.KEEP,work)
    }
}
class JournalReminderWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params) {
    override suspend fun doWork():Result {
        val config=JournalNotifications.read(applicationContext)
        if(!config.enabled) return Result.success()
        val manager=applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("journal","Journal reminders",NotificationManager.IMPORTANCE_DEFAULT))
        val intent=Intent(applicationContext,MainActivity::class.java).putExtra("openJournal",true)
        val open=PendingIntent.getActivity(applicationContext,170,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if(NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) {
            try { manager.notify(170,NotificationCompat.Builder(applicationContext,"journal").setSmallIcon(android.R.drawable.ic_menu_edit).setContentTitle("A moment for you").setContentText("How did today feel? Add your Pulse check-in.").setContentIntent(open).setAutoCancel(true).build()) } catch(_:SecurityException) { }
        }
        JournalNotifications.schedule(applicationContext,config.minute)
        return Result.success()
    }
}
