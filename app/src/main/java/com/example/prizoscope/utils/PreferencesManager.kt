package com.example.prizoscope.utils

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val DARK_MODE_KEY = "dark_mode"
    }

    fun isDarkModeEnabled(): Boolean {
        return prefs.getBoolean(DARK_MODE_KEY, false)
    }

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean(DARK_MODE_KEY, enabled).apply()
    }
}
