// File: app/src/main/java/com/example/prizoscope/api/ObjectDetectionApiService.kt
package com.example.prizoscope.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ObjectDetectionApiService {
    @POST("v1/images:annotate")
    fun detectObjects(
        @Body request: VisionApiRequest
    ): Call<VisionApiResponse>
}