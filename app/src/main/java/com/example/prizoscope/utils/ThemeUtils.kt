package com.example.prizoscope.utils

import android.app.Activity
import android.content.Context
import android.view.View
import androidx.appcompat.app.AppCompatDelegate

object ThemeUtils {

    private const val PREF_NAME = "app_prefs"
    private const val DARK_MODE_KEY = "dark_mode"

    fun saveAndApplyTheme(context: Context, isDarkMode: Boolean) {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putBoolean(DARK_MODE_KEY, isDarkMode)
            apply()
        }
        applyTheme(context, isDarkMode)
    }

    fun applyTheme(context: Context, isDarkMode: Boolean) {
        val mode = if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.setDefaultNightMode(mode)
        setNavAndStatusBar(context, isDarkMode)
    }

    private fun setNavAndStatusBar(context: Context, isDarkMode: Boolean) {
        if (context is Activity) {
            val decorView = context.window.decorView
            val flags = decorView.systemUiVisibility
            decorView.systemUiVisibility = if (isDarkMode) {
                flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv() // Clear light status bar for dark theme
            } else {
                flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR // Set light status bar for light theme
            }
        }
    }

    fun isDarkModeEnabled(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(DARK_MODE_KEY, false)
    }
}
