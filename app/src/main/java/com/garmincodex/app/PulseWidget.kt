package com.garmincodex.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.work.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.*
import java.time.format.DateTimeFormatter

class PulseWidget : AppWidgetProvider() {
    override fun onUpdate(context:Context, manager:AppWidgetManager, ids:IntArray) { refresh(context) }
    companion object {
        fun refresh(context:Context) {
            if(AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context,PulseWidget::class.java)).isNotEmpty())
                WorkManager.getInstance(context).enqueueUniqueWork("pulse-widget",ExistingWorkPolicy.REPLACE,OneTimeWorkRequestBuilder<WidgetRefreshWorker>().build())
        }
        internal fun views(context:Context,day:ConnectedDay?,lastSync:Long,today:LocalDate=LocalDate.now()):RemoteViews {
            val current=day?.takeIf{it.date==today.toString()}
            return RemoteViews(context.packageName,R.layout.pulse_widget).apply {
                setTextViewText(R.id.widget_steps,current?.steps?.let{String.format(java.util.Locale.getDefault(),"%,d",it)} ?: "—")
                setTextViewText(R.id.widget_recovery,current?.recovery?.let{"$it%"} ?: "—")
                val time=if(lastSync==0L) "No import yet" else "Imported "+Instant.ofEpochMilli(lastSync).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM · HH:mm"))
                setTextViewText(R.id.widget_sync,"${today.format(DateTimeFormatter.ofPattern("EEE d MMM"))} · $time")
                val flags=PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                setOnClickPendingIntent(R.id.widget_root,PendingIntent.getActivity(context,80,Intent(context,MainActivity::class.java).putExtra("openToday",true),flags))
                setOnClickPendingIntent(R.id.widget_journal,PendingIntent.getActivity(context,81,Intent(context,MainActivity::class.java).putExtra("openJournal",true),flags))
            }
        }
    }
}
class WidgetRefreshWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params),KoinComponent {
    private val dao:DailyDao by inject()
    override suspend fun doWork():Result {
        val today=LocalDate.now()
        val day=dao.day(today.toString())
        val prefs=applicationContext.getSharedPreferences("health_sync",Context.MODE_PRIVATE)
        AppWidgetManager.getInstance(applicationContext).updateAppWidget(ComponentName(applicationContext,PulseWidget::class.java),PulseWidget.views(applicationContext,day,prefs.getLong("lastSync",0),today))
        return Result.success()
    }
}
