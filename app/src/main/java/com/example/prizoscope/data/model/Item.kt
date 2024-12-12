package com.example.prizoscope.data.model

import org.json.JSONArray
import org.json.JSONObject

data class Item(
    val id: String = "",
    val name: String = "",
    val price: String = "", // Regular price
    val discount_price: String? = null, // Discounted price (null if no promotion)
    val duration_hours: String? = null, // Duration of promotion (null if no promotion)
    val rating: String = "",
    val purchaseLink: String = "",
    val imageLink: String = ""
) {
    companion object {
        // Convert a JSON array string to a list of Item objects
        fun fromJsonArray(jsonArrayString: String): List<Item> {
            val jsonArray = JSONArray(jsonArrayString)
            val items = mutableListOf<Item>()
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                items.add(
                    Item(
                        id = jsonObject.optString("id", ""),
                        name = jsonObject.optString("name", ""),
                        price = jsonObject.optString("price", ""),
                        discount_price = jsonObject.optString("discount_price", null), // Null if not present
                        duration_hours = jsonObject.optString("duration_hours", null), // Null if not present
                        rating = jsonObject.optString("rating", ""),
                        purchaseLink = jsonObject.optString("url", ""), // Adjusted to match provided structure
                        imageLink = jsonObject.optString("img_url", "") // Adjusted to match provided structure
                    )
                )
            }
            return items
        }

        // Convert a list of Item objects to a JSON array string
        fun toJsonArray(items: List<Item>): String {
            val jsonArray = JSONArray()
            items.forEach { item ->
                val jsonObject = JSONObject()
                jsonObject.put("id", item.id)
                jsonObject.put("name", item.name)
                jsonObject.put("price", item.price)
                item.discount_price?.let { jsonObject.put("discount_price", it) } // Add only if not null
                item.duration_hours?.let { jsonObject.put("duration_hours", it) } // Add only if not null
                jsonObject.put("rating", item.rating)
                jsonObject.put("url", item.purchaseLink) // Adjusted to match provided structure
                jsonObject.put("img_url", item.imageLink) // Adjusted to match provided structure
                jsonArray.put(jsonObject)
            }
            return jsonArray.toString()
        }
    }

    // Utility function to get the effective price
    fun getEffectivePrice(): String {
        return discount_price ?: price // Return discounted price if available, otherwise regular price
    }

    // Utility function to check if the item has a promotion
    fun hasPromotion(): Boolean {
        return discount_price != null && duration_hours != null
    }
}
