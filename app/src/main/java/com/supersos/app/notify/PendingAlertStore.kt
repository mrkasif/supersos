package com.supersos.app.notify

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists the pending "unreachable" alert across crashes / restarts.
 *
 * While the phone is in a dead zone we keep overwriting this with the freshest
 * GPS fix. When coverage returns the alert is flushed to the contacts.
 */
class PendingAlertStore(context: Context) {

    data class PendingAlert(
        val lat: Double,
        val lng: Double,
        val accuracyM: Float,
        val timestampMs: Long,
        val contactPhones: List<String>
    )

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Overwrite the queued alert (idempotent — used to refresh the location). */
    fun updatePending(alert: PendingAlert) {
        prefs.edit().putString(KEY_PENDING, toJson(alert)).apply()
    }

    /** Read without clearing. */
    fun peekPending(): PendingAlert? =
        prefs.getString(KEY_PENDING, null)?.let { runCatching { fromJson(it) }.getOrNull() }

    /** Read and clear — called when we are back online and hand off to the notifier. */
    fun takePending(): PendingAlert? {
        val alert = peekPending()
        if (alert != null) prefs.edit().remove(KEY_PENDING).apply()
        return alert
    }

    private fun toJson(alert: PendingAlert): String = JSONObject().apply {
        put("lat", alert.lat)
        put("lng", alert.lng)
        put("accuracy", alert.accuracyM)
        put("ts", alert.timestampMs)
        put("phones", JSONArray(alert.contactPhones))
    }.toString()

    private fun fromJson(json: String): PendingAlert {
        val o = JSONObject(json)
        val phones = mutableListOf<String>()
        val arr = o.getJSONArray("phones")
        for (i in 0 until arr.length()) phones.add(arr.getString(i))
        return PendingAlert(
            lat = o.getDouble("lat"),
            lng = o.getDouble("lng"),
            accuracyM = o.getDouble("accuracy").toFloat(),
            timestampMs = o.getLong("ts"),
            contactPhones = phones
        )
    }

    companion object {
        private const val PREFS_NAME = "supersos_alerts"
        private const val KEY_PENDING = "pending_alert"
    }
}
