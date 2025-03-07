package com.example.prizoscope.api

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ObjectDetectionApiService {
    @Multipart
    @POST("images:annotate") // ✅ Ensure this endpoint is correct
    fun detectObjects(
        @Part image: MultipartBody.Part
    ): Call<ObjectDetectionResponse>
}
