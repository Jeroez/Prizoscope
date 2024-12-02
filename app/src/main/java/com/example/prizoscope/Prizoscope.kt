package com.example.prizoscope

import android.app.Application
import com.example.prizoscope.utils.ThemeUtils

class PrizoscopeApp : Application() {

    override fun onCreate() {
        super.onCreate()

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isDarkModeEnabled = prefs.getBoolean("dark_mode", false)

        ThemeUtils.applyTheme(isDarkModeEnabled)
    }
}

