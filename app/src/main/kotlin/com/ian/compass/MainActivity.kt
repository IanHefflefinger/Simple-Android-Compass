package com.ian.compass

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
        setContentView(compass)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        compass.sensorMissing = sensor == null

        // If a home-screen widget exists, make sure its sensor service is running
        CompassService.start(this)
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
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        tracker.reset()
    }

    override fun onSensorChanged(e: SensorEvent) {
        compass.heading = tracker.update(e.values)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
