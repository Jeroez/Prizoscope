package com.example.prizoscope.data.repository

import android.content.Context
import com.example.prizoscope.data.model.Item

class ItemRepository(private val context: Context) {

    private val items = mutableListOf<Item>()
    private val bookmarkedItems = mutableListOf<Item>()

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
