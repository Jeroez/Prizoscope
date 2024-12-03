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
            reader.readLine()
            reader.forEachLine { line ->
                val columns = line.split(",")
                if (columns.size == 6) {
                    items.add(
                        Item(
                            id = columns[0].trim(),
                            name = columns[1].trim(),
                            price = columns[2].trim().toDouble(),
                            imageLink = columns[3].trim(),
                            ratings = columns[4].trim().toFloat(),
                            purchaseLink = columns[5].trim()
                        )
                    )
                }
            }
        }
        return items
    }
}
