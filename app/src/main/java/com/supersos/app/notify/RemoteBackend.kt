package com.supersos.app.notify

import com.supersos.app.notify.PendingAlertStore.PendingAlert

/**
 * Optional cloud backend.
 *
 * The phone itself can only send while it has SOME signal. The most reliable
 * way to alert contacts when the phone goes fully dark is a SERVER that:
 *   1. receives a heartbeat + location from this phone while it has coverage;
 *   2. if the heartbeat stops for X minutes, the SERVER pushes the last known
 *      location to the contacts (SMS/call/push) — no phone signal required.
 *
 * Implement [report] to forward heartbeats to such a server.
 */
interface RemoteBackend {
    fun isConfigured(): Boolean
    suspend fun report(alert: PendingAlert)
}

/** Placeholder — fill in your endpoint + auth. */
class HttpRemoteBackend(private val endpoint: String) : RemoteBackend {

    override fun isConfigured(): Boolean = endpoint.isNotBlank()

    override suspend fun report(alert: PendingAlert) {
        // TODO: POST {lat, lng, accuracy, ts, phones[]} to $endpoint over HTTPS
    }
}
