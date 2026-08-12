package com.supersos.app.notify

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.supersos.app.R
import com.supersos.app.monitor.SmsStatusReceiver
import com.supersos.app.monitor.SmsStatusReceiver.Companion.ACTION_SMS_SENT
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Actually gets the alert out to the 3 contacts.
 *
 * Primary channel: SMS. SMS only needs a cell signal — NOT data — so it is the
 * best shot at reaching someone from a low-coverage area. If there is no cell
 * signal at all the send fails and the alert stays queued for later.
 *
 * Secondary channel: [RemoteBackend] (server heartbeat) for the fully-offline
 * case — see RemoteBackend.kt.
 */
class LocationNotifier(private val context: Context) {

    /** Returns true if at least one SMS was handed to the radio. */
    fun sendSmsToAll(alert: PendingAlertStore.PendingAlert): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "SEND_SMS not granted — cannot SMS alert")
            return false
        }

        val body = buildSmsBody(alert)
        var anySent = false

        alert.contactPhones.forEachIndexed { index, phone ->
            try {
                val sentPi = PendingIntent.getBroadcast(
                    context,
                    index,
                    Intent(context, SmsStatusReceiver::class.java)
                        .setAction(ACTION_SMS_SENT)
                        .putExtra(EXTRA_PHONE, phone),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                SmsManager.getDefault().sendTextMessage(phone, null, body, sentPi, null)
                anySent = true
            } catch (e: Exception) {
                Log.w(TAG, "SMS to $phone failed", e)
            }
        }
        return anySent
    }

    private fun buildSmsBody(alert: PendingAlertStore.PendingAlert): String {
        val whenStr = SimpleDateFormat("EEE, dd MMM yyyy HH:mm", Locale.getDefault())
            .format(Date(alert.timestampMs))
        val mapsUrl = "https://maps.google.com/?q=${alert.lat},${alert.lng}"
        return context.getString(R.string.alert_sms_body, whenStr, alert.accuracyM.toInt(), mapsUrl)
    }

    companion object {
        private const val TAG = "LocationNotifier"
        const val EXTRA_PHONE = "extra_phone"
    }
}
