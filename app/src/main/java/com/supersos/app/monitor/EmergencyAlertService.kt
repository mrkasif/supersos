package com.supersos.app.monitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.supersos.app.R
import com.supersos.app.location.LocationTracker
import com.supersos.app.notify.LocationNotifier
import com.supersos.app.notify.PendingAlertStore

/**
 * Foreground service that keeps the guard alive while the app runs
 * (and after boot via [BootReceiver]).
 *
 * Runs all three pieces:
 *  - LocationTracker   (GPS fix, works offline)
 *  - ConnectivityMonitor (data loss + weak signal detection)
 *  - EmergencyMonitor    (the alert state machine)
 */
class EmergencyAlertService : Service() {

    private lateinit var tracker: LocationTracker
    private lateinit var connectivity: ConnectivityMonitor
    private lateinit var monitor: EmergencyMonitor

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        tracker = LocationTracker(this)
        connectivity = ConnectivityMonitor(this)
        monitor = EmergencyMonitor(
            context = this,
            tracker = tracker,
            connectivity = connectivity,
            notifier = LocationNotifier(this),
            store = PendingAlertStore(this)
        )

        tracker.start()
        connectivity.start()
        monitor.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        START_STICKY // restarted by the system if it is killed

    override fun onDestroy() {
        monitor.stop()
        connectivity.stop()
        tracker.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
        )
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text))
            .setSmallIcon(R.drawable.ic_launcher)
            .setOngoing(true)
            .build()

    companion object {
        private const val NOTIFICATION_ID = 101
        private const val CHANNEL_ID = "supersos_guard"

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, EmergencyAlertService::class.java)
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, EmergencyAlertService::class.java))
        }
    }
}
