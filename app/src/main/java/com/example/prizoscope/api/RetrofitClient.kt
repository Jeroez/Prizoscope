package com.example.prizoscope.api
import ObjectDetectionApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://vision.googleapis.com/v1/images:annotate" // Replace with your API base URL


    val instance: ObjectDetectionApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ObjectDetectionApiService::class.java)

    }
}