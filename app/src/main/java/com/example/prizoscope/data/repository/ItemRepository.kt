package com.example.prizoscope.data.repository

import android.util.Log
import com.example.prizoscope.data.model.Item
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ItemRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val itemsCollection = firestore.collection("items")

    suspend fun fetchAllItems(): List<Item> {
        val snapshot = itemsCollection.get().await()
        return snapshot.documents.mapNotNull { document ->
            try {
                Item(
                    id = document.getString("id") ?: "",
                    name = document.getString("name") ?: "",
                    price = document.getDouble("price") ?: 0.0,  // Convert to Double
                    rating = document.getFloatOrNull("rating"),  // Convert to Float
                    url = document.getString("url") ?: "",
                    img_url = document.getString("img_url") ?: "",
                    discount_price = document.getIntOrNull("promotion.discount_price"),  // Convert to Int
                    duration_hours = document.getIntOrNull("promotion.duration_hours")  // Convert to Int
                )
            } catch (e: Exception) {
                Log.e("ItemRepository", "Error parsing item: ${e.message}")
                null
            }
        }
    }

    // Firestore-safe number conversion functions
    private fun com.google.firebase.firestore.DocumentSnapshot.getIntOrNull(key: String): Int? {
        return when (val value = get(key)) {
            is Number -> value.toInt()
            else -> null
        }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.getFloatOrNull(key: String): Float? {
        return when (val value = get(key)) {
            is Number -> value.toFloat()
            else -> null
        }
    }
}
