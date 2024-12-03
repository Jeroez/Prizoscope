package com.example.prizoscope.ui.shopping

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prizoscope.R
import com.example.prizoscope.data.model.Item
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ShoppingActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ItemAdapter
    private lateinit var searchView: SearchView
    private lateinit var progressBar: ProgressBar
    private lateinit var executorService: ExecutorService

    private var items: List<Item> = emptyList()
    private var isItemsLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shopping)

        Log.d("ShoppingActivity", "onCreate() started")

        recyclerView = findViewById(R.id.recycler_view)
        searchView = findViewById(R.id.search_view)
        progressBar = findViewById(R.id.progress_bar)

        // Initialize ExecutorService
        executorService = Executors.newSingleThreadExecutor()

        // Initialize adapter with an empty list
        adapter = ItemAdapter(emptyList())
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

    override fun onDestroy() {
        super.onDestroy()
        // Shut down ExecutorService to avoid resource leaks
        executorService.shutdownNow()
        Log.d("ShoppingActivity", "ExecutorService shut down")
    }
}
