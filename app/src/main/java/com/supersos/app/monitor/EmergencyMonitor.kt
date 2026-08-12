package com.supersos.app.monitor

import android.content.Context
import android.location.Location
import android.util.Log
import com.supersos.app.data.ContactsRepository
import com.supersos.app.location.LocationTracker
import com.supersos.app.notify.LocationNotifier
import com.supersos.app.notify.PendingAlertStore
import com.supersos.app.notify.PendingAlertStore.PendingAlert
import com.supersos.app.notify.RemoteBackend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.atomic.AtomicLong

/**
 * THE core state machine.
 *
 * State              → Action
 * ONLINE             → nothing (normal life)
 * coverage drops     → start "unreachable" clock, immediately try an SMS with
 *                      the current fix (SMS needs only cell signal, not data)
 * still unreachable  → keep refreshing the queued location from GPS, retry SMS
 *                      periodically
 * coverage returns   → flush the queued alert to all contacts
 *
 * Tuning knobs (feel free to change):
 *  - unreachableThresholdMs: how long offline before we consider it an emergency
 *  - smsRetryIntervalMs:     how often to re-attempt SMS while offline
 */
class EmergencyMonitor(
    private val context: Context,
    private val tracker: LocationTracker,
    private val connectivity: ConnectivityMonitor,
    private val notifier: LocationNotifier,
    private val store: PendingAlertStore,
    private val backend: RemoteBackend = HttpRemoteBackend("")
) {

    private val contactsRepo = ContactsRepository(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    var unreachableThresholdMs = 2 * 60 * 1000L
    var smsRetryIntervalMs = 2 * 60 * 1000L

    private val unreachableSinceMs = AtomicLong(0L) // 0 = reachable
    private val lastSmsAttemptMs = AtomicLong(0L)

    fun start() {
        combine(connectivity.state, tracker.lastLocation) { state, location -> state to location }
            .onEach { (state, location) -> evaluate(state, location) }
            .launchIn(scope)
    }

    fun stop() {
        scope.cancel()
    }

    private fun evaluate(state: ConnectivityState, location: Location?) {
        val phones = contactsRepo.list().map { it.phone }
        if (phones.isEmpty()) return

        when (state) {
            ConnectivityState.ONLINE -> onReachable()

            ConnectivityState.OFFLINE, ConnectivityState.LOW_SIGNAL ->
                onUnreachable(state, location, phones)
        }
    }

    private fun onReachable() {
        if (unreachableSinceMs.get() == 0L) return
        Log.i(TAG, "Coverage restored after ${System.currentTimeMillis() - unreachableSinceMs.get()} ms")
        unreachableSinceMs.set(0L)
        flushPending()
    }

    private fun onUnreachable(state: ConnectivityState, location: Location?, phones: List<String>) {
        if (unreachableSinceMs.get() == 0L) {
            unreachableSinceMs.set(System.currentTimeMillis())
            Log.i(TAG, "Coverage lost ($state) — entering unreachable mode")
            // Best first shot: SMS the current fix while ANY cell signal remains.
            if (location != null) store.updatePending(location.toAlert(phones))
            store.peekPending()?.let { notifier.sendSmsToAll(it) }
            lastSmsAttemptMs.set(System.currentTimeMillis())
        }

        // GPS keeps fixing while offline — keep the queued alert as fresh as possible.
        location?.let { store.updatePending(it.toAlert(phones)) }

        maybeRetrySms()
    }

    private fun maybeRetrySms() {
        val since = unreachableSinceMs.get()
        val now = System.currentTimeMillis()
        val thresholdReached = since != 0L && (now - since) >= unreachableThresholdMs
        val due = (now - lastSmsAttemptMs.get()) >= smsRetryIntervalMs
        if (thresholdReached && due && store.peekPending() != null) {
            lastSmsAttemptMs.set(now)
            notifier.sendSmsToAll(store.peekPending()!!)
        }
    }

    private fun flushPending() {
        val pending = store.takePending() ?: return
        val sent = notifier.sendSmsToAll(pending)

        if (backend.isConfigured()) {
            scope.launch { backend.report(pending) }
        }

        if (!sent) {
            // Nothing got out — re-queue so the next connectivity cycle retries.
            store.updatePending(pending)
        }
    }

    private fun Location.toAlert(phones: List<String>) = PendingAlert(
        lat = latitude,
        lng = longitude,
        accuracyM = accuracy,
        timestampMs = time,
        contactPhones = phones
    )

    companion object {
        private const val TAG = "EmergencyMonitor"
    }
}
