package com.ian.compass

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import kotlin.math.roundToInt

class CompassWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) manager.updateAppWidget(id, fullViews(context, lastHeading))
        CompassService.start(context)
    }

    override fun onEnabled(context: Context) {
        CompassService.start(context)
    }

    override fun onDisabled(context: Context) {
        context.stopService(Intent(context, CompassService::class.java))
    }

    companion object {
        @Volatile var lastHeading = 0f

        private fun fullViews(context: Context, heading: Float): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_compass)
            applyHeading(views, heading)
            // Tapping the widget opens the full-screen compass
            val open = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.root, open)
            return views
        }

        private fun applyHeading(views: RemoteViews, heading: Float) {
            // The dial turns the opposite way to the phone: rotate by (360 - heading)
            val level = (((360f - heading) % 360f) / 360f * 10000f).toInt().coerceIn(0, 10000)
            views.setInt(R.id.dial, "setImageLevel", level)
            val deg = heading.roundToInt() % 360
            views.setTextViewText(R.id.readout, "$deg\u00B0 ${HeadingTracker.cardinal(heading)}")
        }

        /** Cheap partial update, called by the service many times per second. */
        fun push(context: Context, heading: Float) {
            lastHeading = heading
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, CompassWidget::class.java))
            if (ids.isEmpty()) return
            val views = RemoteViews(context.packageName, R.layout.widget_compass)
            applyHeading(views, heading)
            manager.partiallyUpdateAppWidget(ids, views)
        }

        fun hasWidgets(context: Context): Boolean =
            AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, CompassWidget::class.java)).isNotEmpty()
    }
}
