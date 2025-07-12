package com.example.mymusic.repo.remote

import com.example.mymusic.repo.entity.ApiResponse
import io.reactivex.rxjava3.core.Observable
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {

    @GET("/songs")
    fun getSong(): Observable<ApiResponse>

    @GET("songs/style/{style}")
    fun getSongsByStyle(@Path("style") style: String): Observable<ApiResponse>
}