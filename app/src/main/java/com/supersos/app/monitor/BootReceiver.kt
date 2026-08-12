package com.supersos.app.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.supersos.app.data.AppPrefs

/**
 * Re-arms the guard after the phone restarts, if the user left it enabled.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                if (AppPrefs.isGuardEnabled(context)) {
                    EmergencyAlertService.start(context)
                }
            }
        }
    }
}
