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
import com.example.prizoscope.ui.bookmarks.BookmarkActivity
import com.example.prizoscope.ui.chat.ChatActivity
import com.example.prizoscope.ui.shopping.ShoppingActivity
import com.example.prizoscope.ui.camera.CameraActivity
import com.example.prizoscope.utils.ThemeUtils
import com.google.android.material.bottomnavigation.BottomNavigationView

class SettingsActivity : AppCompatActivity() {

    private lateinit var themeSwitch: Switch
    private lateinit var logoutButton: Button
    private lateinit var notificationsSwitch: Switch
    private lateinit var resetButton: Button
    private lateinit var aboutButton: TextView
    private lateinit var bottomNavigationView: BottomNavigationView

    // SharedPreferences for app settings and user session
    private lateinit var appPreferences: SharedPreferences
    private lateinit var userSessionPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Initialize Views
        logoutButton = findViewById(R.id.logout_button)
        notificationsSwitch = findViewById(R.id.switch_notifications)
        resetButton = findViewById(R.id.reset_button)
        aboutButton = findViewById(R.id.about_button)
        bottomNavigationView = findViewById(R.id.bottom_nav)

        // Initialize SharedPreferences
        appPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        userSessionPreferences = getSharedPreferences("user_session", MODE_PRIVATE)

        // Setup theme toggle
        val isDarkModeEnabled = appPreferences.getBoolean("dark_mode", false) // Default: Light mode
        themeSwitch.isChecked = isDarkModeEnabled
        themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            ThemeUtils.saveAndApplyTheme(this, isChecked) // Save and apply theme
            recreate() // Restart the activity to apply the theme
        }

        // Notifications toggle
        val areNotificationsEnabled = appPreferences.getBoolean("notifications_enabled", true) // Default: Enabled
        notificationsSwitch.isChecked = areNotificationsEnabled
        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            appPreferences.edit().putBoolean("notifications_enabled", isChecked).apply()
        }

        // Logout button
        logoutButton.setOnClickListener {
            logoutUser()
        }

        // Reset settings button
        resetButton.setOnClickListener {
            showResetDialog()
        }

        // About button
        aboutButton.setOnClickListener {
            showAboutDialog()
        }

        // Setup bottom navigation
        setupBottomNav()
    }

    /**
     * Clears user session and navigates to the Startup screen.
     */
    private fun logoutUser() {
        userSessionPreferences.edit().clear().apply() // Clear user session
        appPreferences.edit().remove("notifications_enabled").remove("dark_mode").apply() // Clear app-specific settings (optional)
        val intent = Intent(this, Startup::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear activity stack
        startActivity(intent)
    }

    /**
     * Displays a reset settings confirmation dialog.
     */
    private fun showResetDialog() {
        AlertDialog.Builder(this)
            .setTitle("Reset Settings")
            .setMessage("Are you sure you want to reset all settings?")
            .setPositiveButton("Yes") { _, _ ->
                resetSettings()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Clears app preferences and resets UI state.
     */
    private fun resetSettings() {
        appPreferences.edit().clear().apply() // Clear app preferences
        recreate() // Restart activity to apply changes
    }

    /**
     * Displays the About dialog with app information.
     */
    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("About")
            .setMessage("PrizoScope\nVersion: 1.0\nDeveloped by Gomez Jeremiah.\nTeam members: Awal Emzelle & Echon Adrian")
            .setPositiveButton("OK", null)
            .show()
    }

    /**
     * Sets up the bottom navigation bar.
     */
    private fun setupBottomNav() {
        bottomNavigationView.selectedItemId = R.id.nav_settings // Highlight current tab

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_camera -> {
                    startActivity(Intent(this, CameraActivity::class.java))
                    true
                }
                R.id.nav_shopping -> {
                    startActivity(Intent(this, ShoppingActivity::class.java))
                    true
                }
                R.id.nav_bookmarks -> {
                    startActivity(Intent(this, BookmarkActivity::class.java))
                    true
                }
                R.id.nav_settings -> true
                R.id.nav_chat -> {
                    startActivity(Intent(this, ChatActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
}
