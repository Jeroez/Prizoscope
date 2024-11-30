package com.example.prizoscope.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
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
    private lateinit var homeImageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ThemeUtils.applyTheme(this, isDarkModeEnabled())

        bottomNavigationView = findViewById(R.id.bottom_nav)
        homeImageView = findViewById(R.id.home_image)

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_camera -> {
                    updateUIForFragment(CameraFragment())
                    true
                }
                R.id.nav_shopping -> {
                    updateUIForFragment(ShoppingFragment())
                    true
                }
                R.id.nav_bookmarks -> {
                    updateUIForFragment(BookmarkFragment())
                    true
                }
                R.id.nav_settings -> {
                    updateUIForFragment(SettingsFragment())
                    true
                }
                R.id.nav_maps -> {
                    updateUIForFragment(MapFragment())
                    true
                }
                else -> false
            }
        }

        if (savedInstanceState == null) {
            updateUIForFragment(CameraFragment())
        }
    }


    private fun updateUIForFragment(fragment: Fragment) {
        homeImageView.visibility = View.INVISIBLE
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }


    private fun isDarkModeEnabled(): Boolean {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        return prefs.getBoolean("dark_mode", false)
    }
}
