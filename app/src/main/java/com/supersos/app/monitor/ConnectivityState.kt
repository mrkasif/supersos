package com.supersos.app.monitor

/**
 * Coarse picture of how reachable the phone is.
 *
 * ONLINE     – normal data connectivity.
 * LOW_SIGNAL – data up but cell signal is so weak the phone is effectively
 *              unreachable; SMS may or may not still get out.
 * OFFLINE    – no data connectivity at all.
 */
enum class ConnectivityState {
    ONLINE,
    LOW_SIGNAL,
    OFFLINE
}
