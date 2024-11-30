package com.example.prizoscope.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.prizoscope.R
import com.example.prizoscope.ui.bookmarks.BookmarkFragment
import com.example.prizoscope.ui.camera.CameraFragment
import com.example.prizoscope.ui.maps.MapFragment
import com.example.prizoscope.ui.settings.SettingsFragment
import com.example.prizoscope.ui.shopping.ShoppingFragment
import com.example.prizoscope.utils.ThemeUtils
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Apply theme based on user preference
        ThemeUtils.applyTheme(this, isDarkModeEnabled())

        bottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_camera -> loadFragment(CameraFragment())
                R.id.nav_shopping -> loadFragment(ShoppingFragment())
                R.id.nav_bookmarks -> loadFragment(BookmarkFragment())
                R.id.nav_settings -> loadFragment(SettingsFragment())
                R.id.nav_maps -> loadFragment(MapFragment())
                else -> false
            }
        }

        if (savedInstanceState == null) {
            loadFragment(CameraFragment())
        }
    }

    private fun loadFragment(fragment: Fragment): Boolean {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
        return true
    }

    private fun isDarkModeEnabled(): Boolean {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        return prefs.getBoolean("dark_mode", false)
    }
}
