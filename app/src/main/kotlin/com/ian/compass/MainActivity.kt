package com.ian.compass

import android.Manifest
import android.app.Activity
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.view.WindowManager

class MainActivity : Activity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var compass: CompassView
    private var sensor: Sensor? = null
    private val tracker = HeadingTracker()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        compass = CompassView(this)
        compass.declination = Declination.get(this)
        setContentView(compass)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        compass.sensorMissing = sensor == null

        askForLocationOnce()

        // If a home-screen widget exists, make sure its sensor service is running
        CompassService.start(this)
    }

    private fun askForLocationOnce() {
        val prefs = getSharedPreferences("compass", MODE_PRIVATE)
        if (!Declination.hasPermission(this) && !prefs.getBoolean("asked", false)) {
            prefs.edit().putBoolean("asked", true).apply()
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 1)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        refreshDeclination()
    }

    private fun refreshDeclination() {
        Declination.refresh(this) {
            runOnUiThread { compass.declination = Declination.get(this) }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
        }
    }

    override fun onResume() {
        super.onResume()
        sensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        refreshDeclination()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        tracker.reset()
    }

    override fun onSensorChanged(e: SensorEvent) {
        compass.magHeading = tracker.update(e.values)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
