package com.example.prizoscope.api

import android.util.Log
import com.example.prizoscope.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://vision.googleapis.com/v1/"  // ✅ Correct Base URL

    private val apiKey = BuildConfig.GOOGLE_CLOUD_VISION_API_KEY  // ✅ Ensure this is correctly loaded

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            val newUrl = original.url.newBuilder()
                .addQueryParameter("key", apiKey)  // ✅ Fix API key injection
                .build()
            val newRequest = original.newBuilder().url(newUrl).build()
            Log.d("RetrofitClient", "Final API URL: ${newUrl}")  // ✅ Log full API request
            chain.proceed(newRequest)
        }
        .build()

    val instance: ObjectDetectionApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient)
            .build()
            .create(ObjectDetectionApiService::class.java)
    }
}
