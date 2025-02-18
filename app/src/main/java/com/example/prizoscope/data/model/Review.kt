package com.example.prizoscope.data.model
import java.util.Date

data class Review(
    val itemName: String = "",
    val store: String = "",
    val user: String = "",
    val rating: Float = 5f,
    val comment: String = "",
    val timestamp: Date = Date()
)
