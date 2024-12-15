package com.example.prizoscope.ui.shopping

import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.TextView
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
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

        // Initialize ViewModel
        val itemRepository = ItemRepository()
        val factory = ItemViewModelFactory(itemRepository)
        itemViewModel = ViewModelProvider(this, factory).get(ItemViewModel::class.java)

        // Initialize the RecyclerView adapter
        adapter = ItemAdapter(emptyList()) { item ->
            showItemDetailsDialog(item)
        }

        // Setup RecyclerView
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        // Observe the LiveData from ViewModel
        itemViewModel.items.observe(this) { items ->
            adapter.updateData(items)
            binding.progressBar.visibility = View.GONE
        }

        // Load items from the repository
        binding.progressBar.visibility = View.VISIBLE
        itemViewModel.loadItems()

        // Handle search queries passed from CameraActivity
        val searchTerm = intent.getStringExtra("search_term")
        if (!searchTerm.isNullOrEmpty()) {
            binding.searchField.setText(searchTerm)
            filterItems(searchTerm)
        }

        // Setup search functionality with EditText
        setupSearchField()

        // Setup bottom navigation
        setupBottomNav()
    }

    private fun setupSearchField() {
        // Listen for "Search" action when the user presses Enter
        binding.searchField.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.searchField.text.toString().trim()
                if (query.isNotEmpty()) {
                    filterItems(query)
                } else {
                    Toast.makeText(this, "Please enter a search term", Toast.LENGTH_SHORT).show()
                }
                true
            } else {
                false
            }
        }

        // Add a TextWatcher to handle real-time filtering as the user types
        binding.searchField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                filterItems(query)
            }
        })
    }

    private fun filterItems(query: String?) {
        val filteredItems = itemViewModel.items.value?.filter {
            it.name.contains(query ?: "", ignoreCase = true)
        } ?: emptyList()
        adapter.updateData(filteredItems)
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

        // Populate dialog with item details
        itemName.text = item.name
        itemPrice.text = "Price: ₱${item.getEffectivePrice()}"
        itemRatings.text = "Ratings: ${item.rating} ★"

        // Bookmark functionality
        bookmarkButton.setOnClickListener {
            saveToBookmarks(item)
            dialog.dismiss()
        }

        // Purchase functionality
        purchaseButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url))
            startActivity(intent)
        }

        dialog.show()
    }

    private fun saveToBookmarks(item: Item) {
        val sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE)
        val username = sharedPreferences.getString("username", null)

        if (username != null) {
            val bookmarkKey = "bookmarks_$username"
            val bookmarkJson = getSharedPreferences("bookmarks", MODE_PRIVATE)
                .getString(bookmarkKey, "[]")
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
                    navigateToActivity(CameraActivity::class.java)
                    true
                }
                R.id.nav_shopping -> true // Already on ShoppingActivity
                R.id.nav_bookmarks -> {
                    navigateToActivity(BookmarkActivity::class.java)
                    true
                }
                R.id.nav_settings -> {
                    navigateToActivity(SettingsActivity::class.java)
                    true
                }
                R.id.nav_chat -> {
                    navigateToActivity(ChatActivity::class.java)
                    true
                }
                else -> false
            }
        }
    }

    private fun navigateToActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
        finish()
    }
}
