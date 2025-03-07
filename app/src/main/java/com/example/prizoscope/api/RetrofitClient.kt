package com.example.prizoscope.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.prizoscope.BuildConfig
object RetrofitClient {
    private const val BASE_URL = "https://vision.googleapis.com/v1/" // ✅ Fixed Base URL

    // ✅ Automatically attach API Key to every request
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(Interceptor { chain ->
            val original = chain.request()
            val urlWithKey = original.url.newBuilder()
                .addQueryParameter("key", BuildConfig.GOOGLE_CLOUD_VISION_API_KEY) // ✅ Fix here
                .build()
            val requestWithKey = original.newBuilder().url(urlWithKey).build()
            chain.proceed(requestWithKey)
        })
        .build()

    val instance: ObjectDetectionApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient) // ✅ Automatically adds API Key
            .build()
            .create(ObjectDetectionApiService::class.java)
    }
}
