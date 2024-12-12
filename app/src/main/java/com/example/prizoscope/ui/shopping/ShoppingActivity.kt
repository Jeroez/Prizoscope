package com.example.prizoscope.ui.shopping

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prizoscope.R
import com.example.prizoscope.data.model.Item
import com.example.prizoscope.data.repository.ItemRepository
import com.example.prizoscope.databinding.ActivityShoppingBinding
import com.example.prizoscope.ui.bookmarks.BookmarkActivity
import com.example.prizoscope.ui.settings.SettingsActivity
import com.example.prizoscope.ui.camera.CameraActivity
import com.example.prizoscope.ui.chat.ChatActivity
import com.example.prizoscope.viewmodel.ItemViewModel
import com.example.prizoscope.viewmodel.ItemViewModelFactory
import com.google.android.material.bottomnavigation.BottomNavigationView

class ShoppingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShoppingBinding
    private lateinit var itemViewModel: ItemViewModel
    private lateinit var adapter: ItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShoppingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize ViewModel
        val itemRepository = ItemRepository()
        val factory = ItemViewModelFactory(itemRepository)
        itemViewModel = ViewModelProvider(this, factory).get(ItemViewModel::class.java)

        // Initialize the adapter with a click listener for items
        adapter = ItemAdapter(emptyList()) { item ->
            showItemDetailsDialog(item) // Show dialog when an item is clicked
        }

        // Setup RecyclerView
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        // Observe items LiveData and update the adapter
        itemViewModel.items.observe(this) { items ->
            adapter.updateData(items)
            binding.progressBar.visibility = View.GONE
        }

        // Load items from repository
        binding.progressBar.visibility = View.VISIBLE
        itemViewModel.loadItems()

        // Setup SearchView
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterItems(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterItems(newText)
                return true
            }

            private fun filterItems(query: String?) {
                val filteredItems = itemViewModel.items.value?.filter {
                    it.name.contains(query ?: "", ignoreCase = true)
                } ?: emptyList()
                adapter.updateData(filteredItems)
            }
        })

        // Setup bottom navigation
        setupBottomNav()
    }

    private fun showItemDetailsDialog(item: Item) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_item_details)

        // Initialize dialog views
        val itemName = dialog.findViewById<TextView>(R.id.item_name)
        val itemPrice = dialog.findViewById<TextView>(R.id.item_price)
        val itemRatings = dialog.findViewById<TextView>(R.id.item_ratings)
        val bookmarkButton = dialog.findViewById<Button>(R.id.bookmark_button)
        val purchaseButton = dialog.findViewById<Button>(R.id.purchase_button)

        // Set item details
        itemName.text = item.name
        itemPrice.text = "Price: ₱${item.getEffectivePrice()}"
        itemRatings.text = "Ratings: ${item.rating} ★"

        // Bookmark button functionality
        bookmarkButton.setOnClickListener {
            saveToBookmarks(item)
            dialog.dismiss() // Close the dialog
        }

        // Purchase button functionality
        purchaseButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.purchaseLink))
            startActivity(intent) // Open the purchase link
        }

        dialog.show()
    }

    private fun saveToBookmarks(item: Item) {
        val sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE)
        val username = sharedPreferences.getString("username", null)

        if (username != null) {
            // Use a key unique to the user
            val bookmarkKey = "bookmarks_$username"

            val bookmarkJson = getSharedPreferences("bookmarks", MODE_PRIVATE).getString(bookmarkKey, "[]")
            val bookmarks = Item.fromJsonArray(bookmarkJson ?: "[]").toMutableList()

            // Add the new item to the user's bookmarks
            bookmarks.add(item)

            val updatedJson = Item.toJsonArray(bookmarks)
            getSharedPreferences("bookmarks", MODE_PRIVATE).edit()
                .putString(bookmarkKey, updatedJson)
                .apply()
        } else {
            // Handle the case where the user is not logged in
            Toast.makeText(this, "User not logged in. Cannot save bookmarks.", Toast.LENGTH_SHORT).show()
        }
    }




    private fun setupBottomNav() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNavigationView.selectedItemId = R.id.nav_shopping

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_camera -> {
                    startActivity(Intent(this, CameraActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_shopping -> true
                R.id.nav_bookmarks -> {
                    startActivity(Intent(this, BookmarkActivity::class.java))
                    finish()
                    true
                }
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
                else -> false
            }
        }
    }
}
