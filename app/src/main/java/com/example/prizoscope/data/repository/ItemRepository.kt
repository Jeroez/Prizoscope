package com.example.prizoscope.data.repository

import com.example.prizoscope.data.model.Item
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.tasks.await

class ItemRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val itemsCollection = firestore.collection("items")

    suspend fun fetchAllItems(): List<Item> {
        val snapshot = itemsCollection.get().await()
        return snapshot.documents.mapNotNull { it.toObject<Item>() }
    }

    fun addItem(item: Item) {
        itemsCollection.document(item.id).set(item)
    }
}
