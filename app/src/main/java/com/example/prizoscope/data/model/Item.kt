package com.example.prizoscope.data.model

import org.json.JSONArray
import org.json.JSONObject

data class Item(
    val id: String,
    val name: String,
    val price: Double,
    val ratings: Float,
    val purchaseLink: String,
    val imageLink: String
) {
    companion object {
        // Convert JSON string to a list of Items
        fun fromJsonArray(json: String): List<Item> {
            val jsonArray = JSONArray(json)
            val items = mutableListOf<Item>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                items.add(
                    Item(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        price = obj.getDouble("price"),
                        ratings = obj.getDouble("ratings").toFloat(), // Cast Double to Float
                        purchaseLink = obj.getString("purchaseLink"),
                        imageLink = obj.getString("imageLink")
                    )
                )
            }
            return items
        }

        // Convert a list of Items to a JSON string
        fun toJsonArray(items: List<Item>): String {
            val jsonArray = JSONArray()
            items.forEach { item ->
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("name", item.name)
                obj.put("price", item.price)
                obj.put("ratings", item.ratings)
                obj.put("purchaseLink", item.purchaseLink)
                obj.put("imageLink", item.imageLink)
                jsonArray.put(obj)
            }
            return jsonArray.toString()
        }
    }
}
