package com.ian.compass

import android.hardware.SensorManager
import kotlin.math.abs

/** Turns rotation-vector sensor values into a smoothed compass heading (0-360, 0 = north). */
class HeadingTracker {
    private val rotation = FloatArray(9)
    private val remapped = FloatArray(9)
    private val orientation = FloatArray(3)
    private var current = 0f
    private var first = true

    fun reset() { first = true }

    fun update(values: FloatArray): Float {
        SensorManager.getRotationMatrixFromVector(rotation, values)
        SensorManager.getOrientation(rotation, orientation)

        // Held upright? Use the direction the back camera faces instead.
        var azimuth = orientation[0]
        if (abs(orientation[1]) > 1.0f) {
            SensorManager.remapCoordinateSystem(
                rotation, SensorManager.AXIS_X, SensorManager.AXIS_Z, remapped
            )
            SensorManager.getOrientation(remapped, orientation)
            azimuth = orientation[0]
        }

        val target = ((Math.toDegrees(azimuth.toDouble()).toFloat() % 360f) + 360f) % 360f
        if (first) { current = target; first = false }
        // Low-pass filter that handles the 359 -> 0 wraparound
        val delta = ((target - current + 540f) % 360f) - 180f
        current = (current + 0.15f * delta + 360f) % 360f
        return current
    }

    companion object {
        fun cardinal(h: Float): String {
            val dirs = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
            return dirs[((h + 22.5f) / 45f).toInt() % 8]
        }
    }
}
