package com.example.prizoscope.ui.shopping

import android.content.Intent
import android.os.Bundle
import android.view.View
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

        // Initialize adapter – it receives an empty list initially and a click lambda
        adapter = ItemAdapter(emptyList()) { item ->
            showItemDetails(item)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        // Observe items from the ViewModel
        itemViewModel.items.observe(this) { items ->
            adapter.updateData(items)
            binding.progressBar.visibility = View.GONE

            // If launched with a search term, apply filtering
            val searchTerm = intent.getStringExtra("search_term")
            val autoSearch = intent.getBooleanExtra("auto_search", false)
            if (!searchTerm.isNullOrEmpty() && autoSearch) {
                binding.searchField.setText(searchTerm)
                filterItems(searchTerm)
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
        adapter.notifyDataSetChanged()
    }

    private fun showItemDetails(item: Item) {
        // When an item is clicked, open the ItemDetailActivity.
        // (Make sure that your Item class is Serializable or Parcelable as required.)
        val intent = Intent(this, ItemDetailActivity::class.java).apply {
            putExtra("item", item)
        }
        startActivity(intent)
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
