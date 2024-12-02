package com.example.prizoscope.utils

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import com.example.prizoscope.R

object ThemeUtils {
    private const val PREF_NAME = "app_prefs"
    private const val DARK_MODE_KEY = "dark_mode"

    fun saveAndApplyTheme(context: Context, isDarkMode: Boolean) {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putBoolean(DARK_MODE_KEY, isDarkMode)
            apply()
        }
        applyTheme(isDarkMode)
    }

    fun applyTheme(isDarkMode: Boolean) {
        val mode = if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun isDarkModeEnabled(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(DARK_MODE_KEY, false)
    }

    fun getCurrentTheme(context: Context): Int {
        val isDarkMode = isDarkModeEnabled(context)
        return if (isDarkMode) R.style.DarkTheme else R.style.LightTheme
    }
}
