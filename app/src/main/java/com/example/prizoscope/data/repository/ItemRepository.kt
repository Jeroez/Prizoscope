package com.example.prizoscope.data.repository

import android.content.Context
import com.example.prizoscope.data.csv.CsvParser
import com.example.prizoscope.data.model.Item

class ItemRepository(private val context: Context) {

    private val csvParser = CsvParser(context)

    fun getItems(): List<Item> {
        return csvParser.parseCsv()
    }

    fun searchItems(query: String): List<Item> {
        return getItems().filter { it.name.contains(query, ignoreCase = true) }
    }

    fun getItemById(id: String): Item? {
        return getItems().find { it.id == id }
    }
}
