package com.example.prizoscope.ui.shopping

import android.os.Bundle
import androidx.appcompat.widget.SearchView

import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prizoscope.R
import com.example.prizoscope.data.model.Item
import java.io.BufferedReader
import java.io.InputStreamReader


class ShoppingActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ItemAdapter
    private lateinit var searchView: SearchView

    private var items: List<Item> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shopping)

        recyclerView = findViewById(R.id.recycler_view)
        searchView = findViewById(R.id.search_view)

        recyclerView.layoutManager = LinearLayoutManager(this)
        items = loadItems()
        adapter = ItemAdapter(items)
        recyclerView.adapter = adapter

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

    private fun loadItems(): List<Item> {
        return try {
            val csvInputStream = assets.open("items.csv")
            val reader = BufferedReader(InputStreamReader(csvInputStream))
            val items = mutableListOf<Item>()

            reader.forEachLine { line ->
                val columns = line.split(",")
                if (columns.size == 5) {
                    items.add(
                        Item(
                            id = columns[0].trim(),
                            name = columns[1].trim(),
                            price = columns[2].trim().toDouble(),
                            imageLink = columns[3].trim(),
                            ratings = columns[4].trim().toFloat(),
                            purchaseLink = ""
                        )
                    )
                }
            }

            reader.close()
            items
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }


    private fun filterItems(query: String) {
        val filteredItems = items.filter { it.name.contains(query, ignoreCase = true) }
        adapter.updateData(filteredItems)
    }
}
