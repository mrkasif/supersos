package com.supersos.app.monitor

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Detects when the phone loses reachability:
 *  - NetworkCallback fires when data connectivity is lost.
 *  - TelephonyManager signal-strength listener fires when the cell signal
 *    degrades (so we can flag "quasi-unreachable" before data fully dies).
 */
class ConnectivityMonitor(context: Context) {

    private val appContext = context.applicationContext
    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val telephonyManager =
        appContext.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

    private val _state = MutableStateFlow(ConnectivityState.ONLINE)
    val state: StateFlow<ConnectivityState> = _state.asStateFlow()

    @Volatile private var networkAvailable = true
    @Volatile private var signalLevel = 4 // SignalStrength.level is 0..4

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            networkAvailable = true
            refresh()
        }

        override fun onLost(network: Network) {
            networkAvailable = false
            refresh()
        }
    }

    private val phoneListener = object : PhoneStateListener() {
        override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
            signalLevel = signalStrength.level
            refresh()
        }
    }

    fun start() {
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
        telephonyManager?.listen(phoneListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
        refresh()
    }

    fun stop() {
        runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
        telephonyManager?.listen(phoneListener, PhoneStateListener.LISTEN_NONE)
    }

    private fun refresh() {
        _state.value = when {
            !networkAvailable -> ConnectivityState.OFFLINE
            signalLevel <= LOW_SIGNAL_THRESHOLD -> ConnectivityState.LOW_SIGNAL
            else -> ConnectivityState.ONLINE
        }
    }

    companion object {
        /** Level 0-1 out of 4 = effectively unreachable. */
        const val LOW_SIGNAL_THRESHOLD = 1
    }
}
