package com.example.prizoscope.ui.shopping

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prizoscope.R
import com.example.prizoscope.data.model.Item
import com.example.prizoscope.data.repository.ItemRepository
import com.example.prizoscope.databinding.ActivityShoppingBinding
import com.example.prizoscope.ui.bookmarks.BookmarkActivity
import com.example.prizoscope.ui.camera.CameraActivity
import com.example.prizoscope.ui.chat.ChatActivity
import com.example.prizoscope.ui.settings.SettingsActivity
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

        val itemRepository = ItemRepository()
        val factory = ItemViewModelFactory(itemRepository)
        itemViewModel = ViewModelProvider(this, factory).get(ItemViewModel::class.java)

        adapter = ItemAdapter(emptyList()) { item ->
            showItemDetailsDialog(item)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        // Observe items and load the RecyclerView
        itemViewModel.items.observe(this) { items ->
            adapter.updateData(items)
            binding.progressBar.visibility = View.GONE

            // Auto-search after items are loaded
            val searchTerm = intent.getStringExtra("search_term")
            val autoSearch = intent.getBooleanExtra("auto_search", false)
            if (!searchTerm.isNullOrEmpty() && autoSearch) {
                binding.searchField.setText(searchTerm) // Update the search field
                filterItems(searchTerm)                // Trigger the search
            }
        }

        binding.progressBar.visibility = View.VISIBLE
        itemViewModel.loadItems()

        setupBottomNav()
    }

    private fun filterItems(query: String?) {
        val filteredItems = itemViewModel.items.value?.filter {
            it.name.contains(query ?: "", ignoreCase = true)
        } ?: emptyList()
        adapter.updateData(filteredItems)
        adapter.notifyDataSetChanged() // Ensure the RecyclerView refreshes
    }


    private fun showItemDetailsDialog(item: Item) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_item_details)

        val itemName = dialog.findViewById<TextView>(R.id.item_name)
        val itemPrice = dialog.findViewById<TextView>(R.id.item_price)
        val itemRatings = dialog.findViewById<TextView>(R.id.item_ratings)
        val bookmarkButton = dialog.findViewById<Button>(R.id.bookmark_button)
        val purchaseButton = dialog.findViewById<Button>(R.id.purchase_button)

        itemName.text = item.name
        itemPrice.text = "Price: ₱${item.getEffectivePrice()}"
        itemRatings.text = "Ratings: ${item.rating} ★"

        bookmarkButton.setOnClickListener {
            saveToBookmarks(item)
            dialog.dismiss()
        }

        purchaseButton.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("imageUrl", item.img_url) // Pass the image URL
            intent.putExtra("price", item.getEffectivePrice()) // Pass the price
            startActivity(intent)
        }

        dialog.show()
    }

    private fun saveToBookmarks(item: Item) {
        val sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE)
        val username = sharedPreferences.getString("username", null)

        if (username != null) {
            val bookmarkKey = "bookmarks_$username"
            val bookmarkJson = getSharedPreferences("bookmarks", MODE_PRIVATE).getString(bookmarkKey, "[]")
            val bookmarks = Item.fromJsonArray(bookmarkJson ?: "[]").toMutableList()

            bookmarks.add(item)
            val updatedJson = Item.toJsonArray(bookmarks)

            getSharedPreferences("bookmarks", MODE_PRIVATE).edit()
                .putString(bookmarkKey, updatedJson)
                .apply()

            Toast.makeText(this, "Item bookmarked successfully.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "User not logged in. Cannot save bookmarks.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.selectedItemId = R.id.nav_shopping

        bottomNav.setOnItemSelectedListener { menuItem ->
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
