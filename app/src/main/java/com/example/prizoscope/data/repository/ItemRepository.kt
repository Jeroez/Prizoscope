package com.example.prizoscope.data.repository

import android.content.Context
import com.example.prizoscope.data.model.Item
import com.opencsv.CSVReader
import java.io.InputStreamReader

class ItemRepository(private val context: Context) {

    private val items = mutableListOf<Item>()
    private val bookmarkedItems = mutableListOf<Item>()

    init {
        loadItemsFromCsv()
    }

    private fun loadItemsFromCsv() {
        try {
            val inputStream = context.assets.open("items.csv")
            val reader = CSVReader(InputStreamReader(inputStream))
            val rows = reader.readAll()

            for (row in rows.drop(1)) {
                try {
                    val priceString = row[2].replace("₱", "").replace(",", "") // Remove ₱ and commas
                    val item = Item(
                        id = row[0],
                        name = row[1],
                        price = priceString.toDouble(),
                        imageLink = row[5],
                        ratings = row[3].toFloat(),
                        purchaseLink = row[4]
                    )
                    items.add(item)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getAllItems(): List<Item> {
        return items
    }

    fun bookmarkItem(item: Item) {
        if (!bookmarkedItems.contains(item)) {
            bookmarkedItems.add(item)
        }
    }

    fun getBookmarkedItems(): List<Item> {
        return bookmarkedItems
    }
}
