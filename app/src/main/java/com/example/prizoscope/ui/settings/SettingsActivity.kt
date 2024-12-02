package com.example.prizoscope.ui.settings

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.prizoscope.R
import com.example.prizoscope.ui.Startup
import com.example.prizoscope.utils.ThemeUtils

class SettingsActivity : AppCompatActivity() {

    private lateinit var themeSwitch: Switch
    private lateinit var logoutButton: Button
    private lateinit var notificationsSwitch: Switch
    private lateinit var resetButton: Button
    private lateinit var aboutButton: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        themeSwitch = findViewById(R.id.switch_theme)
        logoutButton = findViewById(R.id.logout_button)
        notificationsSwitch = findViewById(R.id.switch_notifications)
        resetButton = findViewById(R.id.reset_button)
        aboutButton = findViewById(R.id.about_button)

        val sharedPreferences: SharedPreferences =
            getSharedPreferences("app_prefs", MODE_PRIVATE)

        // Theme toggle
        val isDarkMode = sharedPreferences.getBoolean("dark_mode", false)
        themeSwitch.isChecked = isDarkMode
        themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            ThemeUtils.saveAndApplyTheme(this, isChecked)
            recreate()
        }

        // Notifications toggle
        val areNotificationsEnabled = sharedPreferences.getBoolean("notifications_enabled", true)
        notificationsSwitch.isChecked = areNotificationsEnabled
        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("notifications_enabled", isChecked).apply()
        }

        // Logout
        logoutButton.setOnClickListener {
            sharedPreferences.edit().clear().apply()
            startActivity(Intent(this, Startup::class.java))
            finish()
        }

        // Reset settings
        resetButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Reset Settings")
                .setMessage("Are you sure you want to reset all settings?")
                .setPositiveButton("Yes") { _, _ ->
                    sharedPreferences.edit().clear().apply()
                    recreate()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // About section
        aboutButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("About")
                .setMessage("PrizoScope\nVersion: 1.0\nDeveloped by Gomez Jeremiah.\nTeam members: Awal Emzelle & Echon Adrian")
                .setPositiveButton("OK", null)
                .show()
        }
    }
}
