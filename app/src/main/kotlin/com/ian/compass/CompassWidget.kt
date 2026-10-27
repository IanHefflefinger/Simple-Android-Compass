package com.ian.compass

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import kotlin.math.roundToInt

class CompassWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) {
            manager.updateAppWidget(id, fullViews(context, lastHeading, Declination.get(context)))
        }
        CompassService.start(context)
    }

    override fun onEnabled(context: Context) {
        CompassService.start(context)
    }

    override fun onDisabled(context: Context) {
        context.stopService(Intent(context, CompassService::class.java))
    }

    companion object {
        /** Last raw magnetic heading. */
        @Volatile var lastHeading = 0f

        private fun fullViews(context: Context, mag: Float, decl: Float?): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_compass)
            applyHeading(views, mag, decl)
            // Tapping the widget opens the full-screen compass
            val open = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.root, open)
            return views
        }

        private fun level(degrees: Float): Int =
            ((((degrees % 360f) + 360f) % 360f) / 360f * 10000f).toInt().coerceIn(0, 10000)

        private fun applyHeading(views: RemoteViews, mag: Float, decl: Float?) {
            val trueHeading = if (decl != null) ((mag + decl) % 360f + 360f) % 360f else mag

            // Dial turns opposite to the phone, so true north stays pointing north
            views.setInt(R.id.dial, "setImageLevel", level(-trueHeading))
            views.setTextViewText(
                R.id.readout,
                "${trueHeading.roundToInt() % 360}\u00B0 ${HeadingTracker.cardinal(trueHeading)}"
            )

            if (decl != null) {
                // Magnetic north sits "declination" degrees clockwise from true north on the dial
                views.setInt(R.id.mag_marker, "setImageLevel", level(decl - trueHeading))
                views.setViewVisibility(R.id.mag_marker, View.VISIBLE)
                views.setTextViewText(R.id.label_true, "TRUE")
                views.setTextViewText(R.id.readout_mag, "MAG ${mag.roundToInt() % 360}\u00B0")
                views.setViewVisibility(R.id.readout_mag, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.mag_marker, View.GONE)
                views.setTextViewText(R.id.label_true, "MAG ONLY")
                views.setViewVisibility(R.id.readout_mag, View.GONE)
            }
        }

        /** Cheap partial update, called by the service many times per second. */
        fun push(context: Context, mag: Float, decl: Float?) {
            lastHeading = mag
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, CompassWidget::class.java))
            if (ids.isEmpty()) return
            val views = RemoteViews(context.packageName, R.layout.widget_compass)
            applyHeading(views, mag, decl)
            manager.partiallyUpdateAppWidget(ids, views)
        }

        fun hasWidgets(context: Context): Boolean =
            AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, CompassWidget::class.java)).isNotEmpty()
    }
}
