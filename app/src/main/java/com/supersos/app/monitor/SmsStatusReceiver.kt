package com.supersos.app.monitor

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log
import com.supersos.app.notify.LocationNotifier
import com.supersos.app.notify.LocationNotifier.Companion.EXTRA_PHONE

/**
 * Receives the result of each SMS send attempt so we know whether the alert
 * actually got out (useful for logging / future retry logic).
 */
class SmsStatusReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SMS_SENT) return
        val phone = intent.getStringExtra(EXTRA_PHONE) ?: return

        val outcome = when (resultCode) {
            Activity.RESULT_OK -> "delivered to network"
            SmsManager.RESULT_ERROR_NO_SERVICE -> "no service"
            SmsManager.RESULT_ERROR_GENERIC_FAILURE -> "generic failure"
            SmsManager.RESULT_ERROR_RADIO_OFF -> "radio off"
            SmsManager.RESULT_ERROR_NULL_PDU -> "null pdu"
            else -> "code $resultCode"
        }
        Log.i(TAG, "SMS to $phone → $outcome")
    }

    companion object {
        const val ACTION_SMS_SENT = "com.supersos.app.action.SMS_SENT"
        private const val TAG = "SmsStatusReceiver"
    }
}
