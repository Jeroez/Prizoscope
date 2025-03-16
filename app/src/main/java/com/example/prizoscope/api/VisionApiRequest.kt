package com.example.prizoscope.api

data class VisionApiRequest(
    val requests: List<AnnotateImageRequest>
) {
    data class AnnotateImageRequest(
        val image: Image,
        val features: List<Feature>
    )

    data class Image(
        val content: String // Base64-encoded image
    )

    data class Feature(
        val type: String = "OBJECT_LOCALIZATION", // Detects generic objects
        val maxResults: Int = 5 // Reduce for tech items only
    )
}