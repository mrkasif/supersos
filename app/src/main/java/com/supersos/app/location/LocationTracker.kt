package com.supersos.app.location

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks the phone's position using plain GPS/network providers.
 *
 * Key point: GPS works with ZERO network coverage, so this keeps producing
 * fresh fixes even deep in a dead zone. The latest fix is what gets sent to
 * contacts (either over SMS if any cell signal exists, or queued for the
 * moment coverage returns).
 */
class LocationTracker(context: Context) {

    private val locationManager =
        context.applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val _lastLocation = MutableStateFlow<Location?>(null)
    val lastLocation: StateFlow<Location?> = _lastLocation.asStateFlow()

    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            _lastLocation.value = location
        }

        override fun onProviderEnabled(provider: String) = Unit
        override fun onProviderDisabled(provider: String) = Unit
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
    }

    fun start() {
        seedLastKnown()
        for (provider in PROVIDERS) {
            if (!locationManager.isProviderEnabled(provider)) continue
            try {
                // 30 s / 10 m — tune for battery vs. freshness.
                locationManager.requestLocationUpdates(provider, 30_000L, 10f, listener)
            } catch (_: SecurityException) {
                // No permission yet — the UI prompts for it.
            }
        }
    }

    fun stop() {
        runCatching { locationManager.removeUpdates(listener) }
    }

    private fun seedLastKnown() {
        for (provider in PROVIDERS) {
            if (_lastLocation.value != null) return
            try {
                locationManager.getLastKnownLocation(provider)?.let { _lastLocation.value = it }
            } catch (_: SecurityException) {
            }
        }
    }

    companion object {
        private val PROVIDERS = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
    }
}
