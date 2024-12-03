package com.example.prizoscope.ui.bookmarks

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prizoscope.R
import com.example.prizoscope.data.model.Item
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.prizoscope.ui.settings.SettingsActivity
import com.example.prizoscope.ui.maps.MapActivity
import com.example.prizoscope.ui.camera.CameraActivity
import com.example.prizoscope.ui.bookmarks.BookmarkActivity
import com.example.prizoscope.ui.shopping.ShoppingActivity
import java.io.BufferedReader
import java.io.InputStreamReader



class BookmarkActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BookmarkAdapter
    private val bookmarks: MutableList<Item> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bookmark)

        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadBookmarks()
        adapter = BookmarkAdapter(bookmarks) { bookmark ->
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(bookmark.purchaseLink)))
        }
        recyclerView.adapter = adapter

        setupBottomNav()
    }

    private fun loadBookmarks() {
        val bookmarkJson = getSharedPreferences("bookmarks", MODE_PRIVATE).getString("bookmarks", "[]")
        bookmarks.clear()
        bookmarks.addAll(Item.fromJsonArray(bookmarkJson ?: "[]"))
    }


    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)

        bottomNav.selectedItemId = R.id.nav_bookmarks

        bottomNav.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_maps -> {
                    startActivity(Intent(this, MapActivity::class.java))
                    finish()
                    true
                }
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

                R.id.nav_bookmarks -> true
                else -> false
            }
        }
    }
}
