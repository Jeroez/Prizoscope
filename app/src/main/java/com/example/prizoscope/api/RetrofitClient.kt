package com.example.prizoscope.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.prizoscope.BuildConfig
object RetrofitClient {
    private const val BASE_URL = "https://vision.googleapis.com/" // Base URL without "/v1/"

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(Interceptor { chain ->
            val original = chain.request()
            // Build the full URL path for Vision API
            val newUrl = original.url.newBuilder()
                .addPathSegment("v1") // Add "v1" path segment
                .addPathSegment("images:annotate") // Endpoint
                .addQueryParameter("key", BuildConfig.GOOGLE_CLOUD_VISION_API_KEY)
                .build()
            chain.proceed(original.newBuilder().url(newUrl).build())
        })
        .build()

    val instance: ObjectDetectionApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ObjectDetectionApiService::class.java)
    }
}