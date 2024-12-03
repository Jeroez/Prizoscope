package com.example.prizoscope.ui.shopping

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prizoscope.R
import com.example.prizoscope.data.model.Item
import com.example.prizoscope.databinding.DialogItemDetailsBinding
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.example.prizoscope.ui.bookmarks.BookmarkActivity
import com.example.prizoscope.ui.maps.MapActivity
import com.example.prizoscope.ui.settings.SettingsActivity
import com.example.prizoscope.ui.camera.CameraActivity
import com.google.android.material.bottomnavigation.BottomNavigationView


class ShoppingActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ItemAdapter
    private lateinit var searchView: SearchView
    private lateinit var progressBar: ProgressBar
    private lateinit var executorService: ExecutorService

    private var items: List<Item> = emptyList()
    private var bookmarks: MutableList<Item> = mutableListOf()
    private var isItemsLoaded = false
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shopping)

        Log.d("ShoppingActivity", "onCreate() started")

        recyclerView = findViewById(R.id.recycler_view)
        searchView = findViewById(R.id.search_view)
        progressBar = findViewById(R.id.progress_bar)

        // Initialize ExecutorService
        executorService = Executors.newSingleThreadExecutor()

        // Initialize SharedPreferences for bookmarks
        sharedPreferences = getSharedPreferences("bookmarks", MODE_PRIVATE)
        loadBookmarks()

        // Initialize adapter with a click listener
        adapter = ItemAdapter(emptyList()) { item -> showItemDialog(item) }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Show progress bar while loading
        progressBar.visibility = View.VISIBLE
        Log.d("ShoppingActivity", "ProgressBar set to VISIBLE")

        // Load items in a background thread
        loadItemsInBackground()

        // Set up the search functionality
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { filterItems(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { filterItems(it) }
                return true
            }
        })
    }

    private fun loadItemsInBackground() {
        Log.d("ShoppingActivity", "Starting background task to load items")

        executorService.execute {
            try {
                val csvInputStream = assets.open("items.csv")
                val reader = BufferedReader(InputStreamReader(csvInputStream))
                val itemList = mutableListOf<Item>()

                reader.forEachLine { line ->
                    try {
                        val columns = line.split(",")
                        if (columns.size == 6) {
                            itemList.add(
                                Item(
                                    id = columns[0].trim(),
                                    name = columns[1].trim(),
                                    price = columns[2].trim().toDouble(),
                                    ratings = columns[3].trim().toFloat(),
                                    purchaseLink = columns[4].trim(),
                                    imageLink = columns[5].trim()
                                )
                            )
                        } else {
                            Log.w("ShoppingActivity", "Skipping malformed line: $line")
                        }
                    } catch (e: Exception) {
                        Log.e("ShoppingActivity", "Error parsing line: $line", e)
                    }
                }

                reader.close()
                Log.d("ShoppingActivity", "CSV file loaded with ${itemList.size} items.")

                // Update UI on the main thread
                Handler(Looper.getMainLooper()).post {
                    items = itemList
                    isItemsLoaded = true
                    Log.d("ShoppingActivity", "Items loaded, isItemsLoaded set to true")

                    adapter.updateData(items)
                    progressBar.visibility = View.GONE // Hide progress bar
                    Log.d("ShoppingActivity", "ProgressBar set to GONE")
                }
            } catch (e: Exception) {
                Log.e("ShoppingActivity", "Error loading items from CSV", e)
                Handler(Looper.getMainLooper()).post {
                    progressBar.visibility = View.GONE // Hide progress bar on error
                }
            }
        }
    }

    private fun filterItems(query: String) {
        Log.d("ShoppingActivity", "Filtering items with query: $query")

        if (!isItemsLoaded) {
            Log.e("ShoppingActivity", "Cannot filter items: Items not loaded.")
            return
        }

        val terms = query.split(" ").filter { it.isNotEmpty() }

        val filteredItems = items.filter { item ->
            terms.all { term -> item.name.contains(term, ignoreCase = true) }
        }.sortedWith(compareByDescending { item ->
            terms.count { term -> item.name.contains(term, ignoreCase = true) }
        })

        adapter.updateData(filteredItems)
    }

    private fun showItemDialog(item: Item) {
        val dialogBinding = DialogItemDetailsBinding.inflate(LayoutInflater.from(this))
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.itemName.text = item.name
        dialogBinding.itemPrice.text = String.format("$%.2f", item.price)
        dialogBinding.itemRatings.text = String.format("%.1f ★", item.ratings)

        dialogBinding.bookmarkButton.setOnClickListener {
            addBookmark(item)
            Toast.makeText(this, "${item.name} bookmarked!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialogBinding.purchaseButton.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.purchaseLink)))
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun addBookmark(item: Item) {
        if (bookmarks.any { it.id == item.id }) {
            Toast.makeText(this, "${item.name} is already bookmarked!", Toast.LENGTH_SHORT).show()
            return
        }

        bookmarks.add(item)
        saveBookmarks()
        Toast.makeText(this, "${item.name} bookmarked!", Toast.LENGTH_SHORT).show()
    }

    private fun loadBookmarks() {
        val bookmarkJson = sharedPreferences.getString("bookmarks", "[]")
        bookmarks = Item.fromJsonArray(bookmarkJson ?: "[]").toMutableList()
    }

    private fun saveBookmarks() {
        val editor = sharedPreferences.edit()
        editor.putString("bookmarks", Item.toJsonArray(bookmarks))
        editor.apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Shut down ExecutorService to avoid resource leaks
        executorService.shutdownNow()
        Log.d("ShoppingActivity", "ExecutorService shut down")
    }

    private fun setupBottomNav() {
        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_camera -> {
                    startActivity(Intent(this, CameraActivity::class.java))
                    true
                }
                R.id.nav_shopping -> true
                R.id.nav_bookmarks -> {
                    startActivity(Intent(this, BookmarkActivity::class.java))
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                R.id.nav_maps -> {
                    startActivity(Intent(this, MapActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
}
