package com.example.prizoscope.data.csv

import android.content.Context
import com.example.prizoscope.data.model.Item
import java.io.BufferedReader
import java.io.InputStreamReader

class CsvParser(private val context: Context) {
    fun parseCsv(): List<Item> {
        val items = mutableListOf<Item>()
        val inputStream = context.assets.open("items.csv")
        BufferedReader(InputStreamReader(inputStream)).use { reader ->
            reader.readLine() // Skip header line
            reader.forEachLine { line ->
                val columns = line.split(",").map { it.trim() }
                if (columns.size >= 6) {
                    items.add(
                        Item(
                            id = columns[0],
                            name = columns[1],
                            price = columns[2].toDoubleOrNull() ?: 0.0,  // Convert to Double
                            img_url = columns[3],
                            rating = columns[4].toFloatOrNull(),  // Convert to Float
                            url = columns[5]
                        )
                    )
                }
            }
        }
        return items
    }
}
