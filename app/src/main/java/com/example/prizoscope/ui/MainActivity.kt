package com.example.prizoscope.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.prizoscope.R
import com.example.prizoscope.ui.bookmarks.BookmarkActivity
import com.example.prizoscope.ui.camera.CameraActivity
import com.example.prizoscope.ui.maps.MapActivity
import com.example.prizoscope.ui.settings.SettingsActivity
import com.example.prizoscope.ui.shopping.ShoppingActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigationView = findViewById(R.id.bottom_nav)
        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_camera -> navigateToActivity(CameraActivity::class.java)
                R.id.nav_shopping -> navigateToActivity(ShoppingActivity::class.java)
                R.id.nav_bookmarks -> navigateToActivity(BookmarkActivity::class.java)
                R.id.nav_settings -> navigateToActivity(SettingsActivity::class.java)
                R.id.nav_maps -> navigateToActivity(MapActivity::class.java)
            }
            true
        }
    }

    private fun navigateToActivity(activityClass: Class<*>) {
        if (activityClass != this::class.java) {
            startActivity(Intent(this, activityClass))
            finish()
        }
    }
}
