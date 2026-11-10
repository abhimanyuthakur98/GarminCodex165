package com.garmincodex.app
import android.app.Application
import android.widget.TextView
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],application=Application::class)
class PulseWidgetTest {
    @Test fun widgetShowsTodayOnlyAndPreservesZeroVersusMissing() {
        val context=ApplicationProvider.getApplicationContext<Application>()
        val today=LocalDate.of(2026,11,10)
        val day=ConnectedDay(today.toString(),steps=0,recovery=84)
        val view=PulseWidget.views(context,day,0,today).apply(context,FrameLayout(context))
        assertEquals("0",view.findViewById<TextView>(R.id.widget_steps).text.toString())
        assertEquals("84%",view.findViewById<TextView>(R.id.widget_recovery).text.toString())
        assertTrue(view.findViewById<TextView>(R.id.widget_sync).text.contains("No import yet"))
        val stale=PulseWidget.views(context,day.copy(date="2026-11-09"),0,today).apply(context,FrameLayout(context))
        assertEquals("—",stale.findViewById<TextView>(R.id.widget_steps).text.toString())
        assertEquals("—",stale.findViewById<TextView>(R.id.widget_recovery).text.toString())
    }
}
