package com.example.prizoscope.ui.maps

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.prizoscope.R
import com.example.prizoscope.ui.camera.CameraActivity
import com.example.prizoscope.ui.bookmarks.BookmarkActivity
import com.example.prizoscope.ui.settings.SettingsActivity
import com.example.prizoscope.ui.shopping.ShoppingActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MapActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        setupBottomNav()
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)

        bottomNav.selectedItemId = R.id.nav_maps

        bottomNav.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_maps -> true
                R.id.nav_camera -> {
                    startActivity(Intent(this, CameraActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_shopping -> {
                    startActivity(Intent(this, ShoppingActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_bookmarks -> {
                    startActivity(Intent(this, BookmarkActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }
}
