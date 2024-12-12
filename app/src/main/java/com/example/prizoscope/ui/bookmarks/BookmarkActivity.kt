package com.example.prizoscope.ui.bookmarks

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prizoscope.R
import com.example.prizoscope.data.model.Item
import com.example.prizoscope.utils.DatabaseHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.prizoscope.ui.settings.SettingsActivity
import com.example.prizoscope.ui.chat.ChatActivity
import com.example.prizoscope.ui.camera.CameraActivity
import com.example.prizoscope.ui.shopping.ShoppingActivity

class BookmarkActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BookmarkAdapter
    private val bookmarks: MutableList<Item> = mutableListOf()
    private lateinit var databaseHelper: DatabaseHelper
    private var userId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bookmark)

        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize DatabaseHelper
        databaseHelper = DatabaseHelper(this)

        // Get the logged-in user's ID
        val sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE)
        val username = sharedPreferences.getString("username", null)
        userId = databaseHelper.getUserId(username ?: "")

        if (userId != null) {
            loadBookmarks()
        } else {
            // Handle the case where the user is not logged in
            bookmarks.clear()
        }

        adapter = BookmarkAdapter(bookmarks) { bookmark ->
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(bookmark.purchaseLink)))
        }
        recyclerView.adapter = adapter

        setupBottomNav()
    }

    private fun loadBookmarks() {
        val sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE)
        val username = sharedPreferences.getString("username", null)

        if (username != null) {
            // Use a key unique to the user
            val bookmarkKey = "bookmarks_$username"

            val bookmarkJson = getSharedPreferences("bookmarks", MODE_PRIVATE).getString(bookmarkKey, "[]")
            bookmarks.clear()
            bookmarks.addAll(Item.fromJsonArray(bookmarkJson ?: "[]"))
        } else {
            // Handle the case where the user is not logged in
            bookmarks.clear()
        }
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
                R.id.nav_chat -> {
                    startActivity(Intent(this, ChatActivity::class.java))
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
