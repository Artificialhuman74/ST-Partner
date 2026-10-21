package com.sanathanatattva.partner.data.api

import com.sanathanatattva.partner.BuildConfig
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

object ApiClient {
    /**
     * [cacheDir]: a 20 MB HTTP cache. The server sends a tag with every answer, so asking again
     * gets "unchanged" with no body when nothing moved.
     */
    fun create(cacheDir: File, tokenProvider: () -> String?): PartnerApi {
        check(BuildConfig.BASE_URL.isNotBlank()) {
            "BASE_URL is empty. Set st.prodBaseUrl in gradle.properties for release builds."
        }
        val auth = Interceptor { chain ->
            val token = tokenProvider()
            val request = if (token.isNullOrBlank()) chain.request()
            else chain.request().newBuilder().header("Authorization", "Bearer $token").build()
            chain.proceed(request)
        }
        val client = OkHttpClient.Builder()
            .cache(Cache(File(cacheDir, "http"), 20L * 1024 * 1024))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            // Upper bound for a whole request, so no button waits forever on a stalled connection.
            .callTimeout(45, TimeUnit.SECONDS)
            .addInterceptor(auth)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                    })
                }
            }
            .build()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(apiJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(PartnerApi::class.java)
    }
}
