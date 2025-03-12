package com.example.prizoscope.api

data class ObjectDetectionResponse(
    val objects: List<DetectedObject>
)

data class DetectedObject(
    val label: String,
    val confidence: Float
)
