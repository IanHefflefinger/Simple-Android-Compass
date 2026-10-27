package com.ian.compass

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.view.View
import kotlin.math.min
import kotlin.math.roundToInt

class CompassView(context: Context) : View(context) {

    /** Heading in degrees, 0 = north, clockwise. */
    var heading = 0f
        set(value) { field = value; invalidate() }

    var sensorMissing = false
        set(value) { field = value; invalidate() }

    private val white = 0xFFE8ECEF.toInt()
    private val red = 0xFFE53935.toInt()

    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = white
    }
    private val tick = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = white }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = white; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
    }
    private val readout = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = white; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
    }
    private val pointer = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = red }
    private val path = Path()

    override fun onDraw(c: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = min(width, height) * 0.42f
        val unit = r / 100f

        if (sensorMissing) {
            readout.textSize = 20f * resources.displayMetrics.density
            c.drawText("No compass sensor on this device", cx, cy, readout)
            return
        }

        ring.strokeWidth = 3f * unit
        c.drawCircle(cx, cy, r, ring)

        // Rotating dial
        c.save()
        c.rotate(-heading, cx, cy)
        for (i in 0 until 72) {
            val major = i % 6 == 0   // every 30 degrees
            val len = if (major) 10f * unit else 5f * unit
            tick.strokeWidth = if (major) 2.5f * unit else 1.2f * unit
            c.save()
            c.rotate(i * 5f, cx, cy)
            c.drawLine(cx, cy - r, cx, cy - r + len, tick)
            c.restore()
        }
        label.textSize = 18f * unit
        val names = arrayOf("N", "E", "S", "W")
        for (i in 0 until 4) {
            c.save()
            c.rotate(i * 90f, cx, cy)
            label.color = if (i == 0) red else white
            c.drawText(names[i], cx, cy - r + 36f * unit, label)
            c.restore()
        }
        c.restore()

        // Fixed pointer at top (the direction you are facing)
        path.reset()
        path.moveTo(cx, cy - r - 4f * unit)
        path.lineTo(cx - 9f * unit, cy - r - 24f * unit)
        path.lineTo(cx + 9f * unit, cy - r - 24f * unit)
        path.close()
        c.drawPath(path, pointer)

        // Digital readout
        val deg = heading.roundToInt() % 360
        readout.textSize = 34f * unit
        c.drawText("$deg° ${cardinal(heading)}", cx, cy + 12f * unit, readout)
    }

    private fun cardinal(h: Float): String {
        val dirs = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        return dirs[((h + 22.5f) / 45f).toInt() % 8]
    }
}
