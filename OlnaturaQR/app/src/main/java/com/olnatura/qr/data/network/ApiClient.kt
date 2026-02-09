package com.olnatura.qr.data.network

import com.olnatura.qr.core.session.SessionManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object ApiClient {

    fun create(
        baseUrl: String,
        cookieJar: CookieJar,
        sessionManager: SessionManager
    ): OlnaturaApi {

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val okHttp = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor(
                AuthInterceptor(
                    sessionManager = sessionManager,
                    cookieJar = cookieJar as PersistentCookieJar
                )
            )
            .addInterceptor(logging)
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        return retrofit.create(OlnaturaApi::class.java)
    }
}