package com.example.prizoscope.data.model

import org.json.JSONArray
import org.json.JSONObject

data class Item(
    val id: String = "",
    val name: String = "",
    val price: Any? = null, // Accept both String and Long
    val discount_price: Any? = null, // Accept both String and Long
    val duration_hours: String? = null,
    val rating: String = "",
    val url: String = "",
    val img_url: String = ""
) {
    fun getFormattedPrice(): String {
        return price?.toString() ?: "N/A"
    }

    fun getFormattedDiscountPrice(): String? {
        return discount_price?.toString()
    }

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
                        discount_price = jsonObject.optString("discount_price", null),
                        duration_hours = jsonObject.optString("expiration_time", null),
                        rating = jsonObject.optString("rating", ""),
                        url = jsonObject.optString("url", ""),
                        img_url = jsonObject.optString("img_url", "")
                    )
                )
            }
            return items
        }

        fun toJsonArray(items: List<Item>): String {
            val jsonArray = JSONArray()
            items.forEach { item ->
                val jsonObject = JSONObject()
                jsonObject.put("id", item.id)
                jsonObject.put("name", item.name)
                jsonObject.put("price", item.price)
                item.discount_price?.let { jsonObject.put("discount_price", it) }
                item.duration_hours?.let { jsonObject.put("duration_hours", it) }
                jsonObject.put("rating", item.rating)
                jsonObject.put("url", item.url)
                jsonObject.put("img_url", item.img_url)
                jsonArray.put(jsonObject)
            }
            return jsonArray.toString()
        }
    }


    fun getEffectivePrice(): String {
        return discount_price?.toString() ?: price?.toString() ?: "N/A"
    }

    fun hasPromotion(): Boolean {
        return discount_price != null && duration_hours != null
    }
 }

