package com.example.prizoscope.api

data class VisionApiResponse(
    val responses: List<AnnotateImageResponse>
) {
    data class AnnotateImageResponse(
        val localizedObjectAnnotations: List<DetectedObject>
    )

    data class DetectedObject(
        val name: String, // Generic label (e.g., "Electronics", "Computer hardware")
        val score: Float // Confidence score
    )
}