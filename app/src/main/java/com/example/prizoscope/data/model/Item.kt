package com.example.prizoscope.data.model

data class Item(
    val id: String,
    val name: String,
    val price: Double,
    val imageLink: String,
    val ratings: Float,
    val purchaseLink: String
)
