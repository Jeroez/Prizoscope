package com.example.prizoscope.data.model

data class Item(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val ratings: Float = 0f,
    val purchaseLink: String = "",
    val imageLink: String = ""
)
