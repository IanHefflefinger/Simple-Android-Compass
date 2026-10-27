package com.ian.compass

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import kotlin.math.abs

/**
 * Keeps the widget alive. Reads the sensor only while the screen is on,
 * so it costs almost nothing when the phone is in your pocket.
 */
class CompassService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var sensor: Sensor? = null
    private val tracker = HeadingTracker()
    private var listening = false
    private var lastPushed = -100f
    private var lastPushTime = 0L
    private var decl: Float? = null

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_ON) startListening() else stopListening()
        }
    }

    // Picks up a new declination when the app refreshes it
    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == Declination.KEY) {
            decl = Declination.get(this)
            CompassWidget.push(this, CompassWidget.lastHeading, decl)
        }
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        decl = Declination.get(this)
        getSharedPreferences("compass", MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(prefListener)

        val channelId = "compass_widget"
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(channelId, "Compass widget", NotificationManager.IMPORTANCE_MIN)
        )
        val notification = Notification.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("Compass widget running")
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }

        registerReceiver(screenReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        })
        if (getSystemService(PowerManager::class.java).isInteractive) startListening()
    }

    private fun startListening() {
        if (listening) return
        sensor?.let {
            decl = Declination.get(this)
            tracker.reset()
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            listening = true
        }
    }

    private fun stopListening() {
        if (!listening) return
        sensorManager.unregisterListener(this)
        listening = false
    }

    override fun onSensorChanged(e: SensorEvent) {
        val mag = tracker.update(e.values)
        val now = SystemClock.elapsedRealtime()
        // Only redraw the widget for a real change, and at most ~12 times a second
        if (abs(mag - lastPushed) >= 1f && now - lastPushTime >= 80) {
            lastPushed = mag
            lastPushTime = now
            CompassWidget.push(this, mag, decl)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopListening()
        unregisterReceiver(screenReceiver)
        getSharedPreferences("compass", MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(prefListener)
        super.onDestroy()
    }

    companion object {
        /** Starts the service if a widget is on the home screen. Safe to call anytime. */
        fun start(context: Context) {
            if (!CompassWidget.hasWidgets(context)) return
            try {
                context.startForegroundService(Intent(context, CompassService::class.java))
            } catch (e: Exception) {
                // Android may refuse background starts; tapping the widget (opens the app) fixes it
            }
        }
    }
}
