package com.ian.compass

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.view.View
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

class CompassView(context: Context) : View(context) {

    /** Raw magnetic heading from the sensor, 0 = magnetic north. */
    var magHeading = 0f
        set(value) { field = value; invalidate() }

    /** Declination in degrees (east positive), or null when location is unavailable. */
    var declination: Float? = null
        set(value) { field = value; invalidate() }

    var sensorMissing = false
        set(value) { field = value; invalidate() }

    private val dp = resources.displayMetrics.density
    private val white = 0xFFE8ECEF.toInt()
    private val red = 0xFFE53935.toInt()
    private val cyan = 0xFF4DD0E1.toInt()
    private val grey = 0xFF9AA5AE.toInt()

    private fun text(color: Int, align: Paint.Align) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color; textAlign = align; typeface = Typeface.DEFAULT_BOLD
    }

    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = white }
    private val tick = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = white }
    private val letter = text(white, Paint.Align.CENTER)
    private val big = text(white, Paint.Align.CENTER)
    private val small = text(white, Paint.Align.CENTER)
    private val diamond = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = cyan }
    private val pointer = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = white }
    private val legend = text(grey, Paint.Align.LEFT)
    private val path = Path()

    private fun diamondPath(cx: Float, cy: Float, hw: Float, hh: Float) {
        path.reset()
        path.moveTo(cx, cy - hh); path.lineTo(cx + hw, cy)
        path.lineTo(cx, cy + hh); path.lineTo(cx - hw, cy); path.close()
    }

    override fun onDraw(c: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = min(width, height) * 0.42f
        val u = r / 100f

        if (sensorMissing) {
            big.textSize = 20f * dp
            c.drawText("No compass sensor on this device", cx, cy, big)
            return
        }

        val decl = declination
        val trueHeading =
            if (decl != null) ((magHeading + decl) % 360f + 360f) % 360f else magHeading

        ring.strokeWidth = 3f * u
        c.drawCircle(cx, cy, r, ring)

        // ---- Rotating dial (north-up = TRUE north) ----
        c.save()
        c.rotate(-trueHeading, cx, cy)
        for (i in 0 until 72) {
            val major = i % 6 == 0
            tick.strokeWidth = if (major) 2.5f * u else 1.2f * u
            c.save()
            c.rotate(i * 5f, cx, cy)
            c.drawLine(cx, cy - r, cx, cy - r + (if (major) 10f else 5f) * u, tick)
            c.restore()
        }
        letter.textSize = 18f * u
        val names = arrayOf("N", "E", "S", "W")
        for (i in 0 until 4) {
            c.save()
            c.rotate(i * 90f, cx, cy)
            letter.color = if (i == 0) red else white
            c.drawText(names[i], cx, cy - r + 44f * u, letter)
            c.restore()
        }
        // Magnetic north: hollow cyan diamond with an M, offset by the declination
        if (decl != null) {
            c.save()
            c.rotate(decl, cx, cy)
            diamond.strokeWidth = 2f * u
            diamondPath(cx, cy - 82f * u, 8.5f * u, 8f * u)
            c.drawPath(path, diamond)
            small.color = cyan; small.textSize = 9f * u
            c.drawText("M", cx, cy - 82f * u + 3.2f * u, small)
            c.restore()
        }
        c.restore()

        // ---- Fixed white pointer: the way you are facing ----
        path.reset()
        path.moveTo(cx, cy - r - 4f * u)
        path.lineTo(cx - 9f * u, cy - r - 24f * u)
        path.lineTo(cx + 9f * u, cy - r - 24f * u)
        path.close()
        c.drawPath(path, pointer)

        // ---- Centre readout ----
        small.textSize = 11f * u
        small.color = if (decl != null) red else white
        c.drawText(if (decl != null) "TRUE" else "MAGNETIC", cx, cy - 18f * u, small)

        big.textSize = 34f * u
        val deg = trueHeading.roundToInt() % 360
        c.drawText("$deg° ${HeadingTracker.cardinal(trueHeading)}", cx, cy + 12f * u, big)

        if (decl != null) {
            small.color = cyan; small.textSize = 11f * u
            c.drawText("MAG ${magHeading.roundToInt() % 360}°", cx, cy + 30f * u, small)
            small.color = grey; small.textSize = 10f * u
            val d = decl.roundToInt()
            c.drawText("DECL ${abs(d)}° ${if (d >= 0) "E" else "W"}", cx, cy + 44f * u, small)
        }

        // ---- Legend ----
        legend.textSize = 14f * dp
        val iconX = cx - 100f * dp
        val textX = cx - 78f * dp
        val y1 = cy + r + 44f * dp
        val y2 = y1 + 34f * dp

        small.textSize = 16f * dp; small.color = red
        c.drawText("N", iconX, y1, small)
        legend.color = grey
        c.drawText(if (decl != null) "True north (solid red N)" else "North (magnetic only)", textX, y1, legend)

        if (decl != null) {
            diamond.strokeWidth = 1.6f * dp
            diamondPath(iconX, y2 - 5f * dp, 9f * dp, 9f * dp)
            c.drawPath(path, diamond)
            small.textSize = 10f * dp; small.color = cyan
            c.drawText("M", iconX, y2 - 1.5f * dp, small)
            c.drawText("Magnetic north (hollow diamond)", textX, y2, legend)
        } else {
            c.drawText("Allow location for true north", textX, y2, legend)
        }
    }
}
