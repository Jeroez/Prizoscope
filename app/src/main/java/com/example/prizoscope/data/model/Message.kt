package com.example.prizoscope.data.model

data class Message(
    val sender: String,
    val text: String? = null,      // For text messages
    val imageUrl: String? = null, // For image messages
    var price: String? = null     // For associated prices
)
