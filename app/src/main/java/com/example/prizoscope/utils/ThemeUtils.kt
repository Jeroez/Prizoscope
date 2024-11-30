package com.example.prizoscope.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemeUtils {

    fun applyTheme(context: Context, darkModeEnabled: Boolean) {
        val mode = if (darkModeEnabled) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}
