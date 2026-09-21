package com.example.data.remote

import com.example.data.model.GutendexBook
import com.example.data.model.GutendexResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GutendexApiService {
    @GET("books")
    suspend fun getBooks(
        @Query("search") search: String? = null,
        @Query("topic") topic: String? = null,
        @Query("languages") languages: String? = null,
        @Query("page") page: Int? = null,
        @Query("sort") sort: String? = null
    ): GutendexResponse

    @GET("books/{id}")
    suspend fun getBookById(
        @Path("id") id: Int
    ): GutendexBook
}
