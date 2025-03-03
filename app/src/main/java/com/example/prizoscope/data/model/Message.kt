package com.example.prizoscope.data.model

data class Message(
    val sender: String,
    val text: String? = null,
    val imageUrl: String? = null,
    var price: String? = null,
    val timestamp: Long = 0L,
    val adminUsername: String? = null,
    val adminStore: String? = null,
    val isSuperAdmin: Boolean? = false
)
