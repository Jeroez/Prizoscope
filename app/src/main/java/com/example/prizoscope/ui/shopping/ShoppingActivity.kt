package com.example.prizoscope.ui.shopping

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
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

    // Sorting/filtering state
    private var currentSortType = SortType.NONE
    private var currentSortAscending = true
    private var maxPrice: Double? = null
    private var currentSearchQuery = ""
    private var originalItems = emptyList<Item>()
    private var filteredItems = emptyList<Item>()

    enum class SortType { NONE, PRICE, RATING }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShoppingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val itemRepository = ItemRepository()
        val factory = ItemViewModelFactory(itemRepository)
        itemViewModel = ViewModelProvider(this, factory).get(ItemViewModel::class.java)

        // Initialize adapter
        adapter = ItemAdapter(emptyList()) { item -> showItemDetails(item) }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ShoppingActivity)
            adapter = this@ShoppingActivity.adapter
        }

        // Observe items from ViewModel
        itemViewModel.items.observe(this) { items ->
            originalItems = items
            applyAllFiltersAndSorting()
            binding.progressBar.visibility = View.GONE

            // Handle auto-search if needed
            val searchTerm = intent.getStringExtra("search_term")
            val autoSearch = intent.getBooleanExtra("auto_search", false)
            if (!searchTerm.isNullOrEmpty() && autoSearch) {
                binding.searchField.setText(searchTerm)
                currentSearchQuery = searchTerm
                applyAllFiltersAndSorting()
            }
        }

        setupSortSpinner()
        setupPriceFilter()
        setupSearch()

        binding.progressBar.visibility = View.VISIBLE
        itemViewModel.loadItems()

        setupBottomNav()
    }

    private fun setupSortSpinner() {
        val sortOptions = arrayOf("Sort by", "Price", "Rating")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sortOptions).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.sortSpinner.apply {
            adapter = spinnerAdapter
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    when (position) {
                        1 -> toggleSort(SortType.PRICE)
                        2 -> toggleSort(SortType.RATING)
                        else -> resetSort()
                    }
                    applyAllFiltersAndSorting()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }

        // Set up the sort order button
        binding.sortOrderButton.setOnClickListener {
            currentSortAscending = !currentSortAscending
            binding.sortOrderButton.setImageResource(
                if (currentSortAscending) R.drawable.up else R.drawable.down
            )
            applyAllFiltersAndSorting()
        }
    }


    private fun setupPriceFilter() {
        binding.priceFilter.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                maxPrice = s?.toString()?.toDoubleOrNull()
                applyAllFiltersAndSorting()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupSearch() {
        binding.searchField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                currentSearchQuery = s?.toString()?.trim() ?: ""
                applyAllFiltersAndSorting()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun toggleSort(newType: SortType) {
        if (currentSortType == newType) {
            currentSortAscending = !currentSortAscending
        } else {
            currentSortType = newType
            currentSortAscending = when (newType) {
                SortType.PRICE -> true
                SortType.RATING -> false
                else -> true
            }
        }
    }

    private fun resetSort() {
        currentSortType = SortType.NONE
        currentSortAscending = true
    }

    private fun applyAllFiltersAndSorting() {
        // Apply filters first
        filteredItems = originalItems.filter { item ->
            item.name.contains(currentSearchQuery, true) &&
                    (maxPrice?.let { item.price <= it } ?: true)
        }

        // Then apply sorting
        filteredItems = when (currentSortType) {
            SortType.PRICE -> if (currentSortAscending) {
                filteredItems.sortedBy { it.price }
            } else {
                filteredItems.sortedByDescending { it.price }
            }
            SortType.RATING -> if (currentSortAscending) {
                filteredItems.sortedByDescending { it.rating }
            } else {
                filteredItems.sortedBy { it.rating }
            }
            else -> filteredItems
        }

        adapter.updateData(filteredItems)
    }

    private fun showItemDetails(item: Item) {
        Intent(this, ItemDetailActivity::class.java).apply {
            putExtra("item", item)
            startActivity(this)
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
                R.id.nav_shopping -> true

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