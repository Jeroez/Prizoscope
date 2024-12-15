package com.example.prizoscope.data.repository

import android.util.Log
import com.example.prizoscope.data.model.Item
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
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
                    price = document.get("price").toString(), // Safely convert to string
                    rating = document.getString("rating") ?: "",
                    url = document.getString("url") ?: "",
                    img_url = document.getString("img_url") ?: "",
                    discount_price  = document.get("promotion.discount_price")?.toString() ?: "",
                    duration_hours = document.get("promotion.duration_hours")?.toString() ?: ""
                )
            } catch (e: Exception) {
                Log.e("ItemRepository", "Error parsing item: ${e.message}")
                null
            }
        }
    }
}

