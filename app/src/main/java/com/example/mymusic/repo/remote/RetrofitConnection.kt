package com.example.mymusic.repo.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitConnection {

//    private const val BASE_URL = "http://192.168.0.105:8000/"
//    private const val BASE_URL = "http://192.168.231.60:8000/"
    private const val BASE_URL = "http://192.168.175.60:8000/"


    // 用于添加拦截器、日志等
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().also {
                it.level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }


    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            .build()
    }

    // 暴露 ApiService 接口
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}
