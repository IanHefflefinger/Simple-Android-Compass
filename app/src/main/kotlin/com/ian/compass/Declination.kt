package com.ian.compass

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.GeomagneticField
import android.location.Location
import android.location.LocationManager
import android.os.Build

/**
 * Magnetic declination (degrees, east positive) for the phone's approximate location.
 * Computed offline from the cached location and stored, so the widget can use it too.
 */
object Declination {
    private const val PREFS = "compass"
    const val KEY = "decl"

    fun get(ctx: Context): Float? {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return if (p.contains(KEY)) p.getFloat(KEY, 0f) else null
    }

    fun hasPermission(ctx: Context): Boolean =
        ctx.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    private fun save(ctx: Context, loc: Location) {
        val field = GeomagneticField(
            loc.latitude.toFloat(), loc.longitude.toFloat(),
            loc.altitude.toFloat(), System.currentTimeMillis()
        )
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putFloat(KEY, field.declination).apply()
    }

    /** Call while the app is in the foreground. [onChanged] runs after a new value is saved. */
    @SuppressLint("MissingPermission")
    fun refresh(ctx: Context, onChanged: () -> Unit) {
        if (!hasPermission(ctx)) return
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val best = lm.getProviders(true).mapNotNull {
            try { lm.getLastKnownLocation(it) } catch (e: SecurityException) { null }
        }.maxByOrNull { it.time }
        if (best != null) {
            save(ctx, best); onChanged(); return
        }
        if (Build.VERSION.SDK_INT >= 30 && lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            lm.getCurrentLocation(LocationManager.NETWORK_PROVIDER, null, ctx.mainExecutor) { loc ->
                if (loc != null) { save(ctx, loc); onChanged() }
            }
        }
    }
}
