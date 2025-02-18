package com.example.prizoscope.data.model

import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable

data class Item(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val discount_price: Int? = null,
    val duration_hours: Int? = null,
    val rating: Float? = 5f,
    val url: String = "",
    val img_url: String = "",
    val store: String = ""
) : Serializable {

    fun getFormattedPrice(): String = "₱%.2f".format(price)

    fun getEffectivePrice(): String {
        return discount_price?.let { "₱$it" } ?: getFormattedPrice()
    }

    companion object {
        fun fromJsonArray(jsonArrayString: String): List<Item> {
            val jsonArray = JSONArray(jsonArrayString)
            return (0 until jsonArray.length()).map { i ->
                val jsonObject = jsonArray.getJSONObject(i)
                Item(
                    id = jsonObject.optString("id", ""),
                    name = jsonObject.optString("name", ""),
                    price = jsonObject.optDouble("price"),
                    discount_price = jsonObject.optIntOrNull("discount_price"),
                    duration_hours = jsonObject.optIntOrNull("duration_hours"),
                    rating = jsonObject.optNumberAsFloat("rating"),
                    url = jsonObject.optString("url", ""),
                    img_url = jsonObject.optString("img_url", ""),
                    store = jsonObject.optString("Store", "")
                )
            }
        }

        private fun JSONObject.optIntOrNull(key: String): Int? {
            return if (has(key) && !isNull(key)) optInt(key) else null
        }

        private fun JSONObject.optNumberAsFloat(key: String): Float? {
            return when {
                has(key) && !isNull(key) -> {
                    try {
                        optDouble(key).toFloat()
                    } catch (e: Exception) {
                        null
                    }
                }
                else -> null
            }
        }

        fun toJsonArray(items: List<Item>): String {
            val jsonArray = JSONArray()
            items.forEach { item ->
                JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("price", item.price)
                    item.discount_price?.let { put("discount_price", it) }
                    item.duration_hours?.let { put("duration_hours", it) }
                    item.rating?.let { put("rating", it) }
                    put("url", item.url)
                    put("img_url", item.img_url)
                    put("Store", item.store)
                    jsonArray.put(this)
                }
            }
            return jsonArray.toString()
        }
    }
}