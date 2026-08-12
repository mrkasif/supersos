package com.supersos.app.data

import android.content.Context

/**
 * App-wide preferences (guard on/off, etc.).
 */
object AppPrefs {

    private const val PREFS_NAME = "supersos"
    private const val KEY_GUARD = "guard_enabled"

    fun isGuardEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_GUARD, false)

    fun setGuardEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_GUARD, enabled)
            .apply()
    }
}
