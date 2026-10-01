package com.example.network

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// -----------------------------------------------------
// 1. LrcLib API Models & Retrofit Interface (lrclib.net)
// -----------------------------------------------------
@JsonClass(generateAdapter = true)
data class LrcLibResponse(
    val id: Long? = null,
    val trackName: String? = null,
    val artistName: String? = null,
    val albumName: String? = null,
    val duration: Double? = null,
    val instrumental: Boolean? = null,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null
)

interface LrcLibApiService {
    @GET("api/get")
    suspend fun getLyrics(
        @Query("track_name") trackName: String,
        @Query("artist_name") artistName: String,
        @Query("duration") durationInSeconds: Long? = null
    ): LrcLibResponse

    @GET("api/search")
    suspend fun searchLyrics(
        @Query("q") query: String
    ): List<LrcLibResponse>
}

// -----------------------------------------------------
// 2. Lyrics.ovh API Models & Retrofit Interface (api.lyrics.ovh)
// -----------------------------------------------------
@JsonClass(generateAdapter = true)
data class LyricsOvhResponse(
    val lyrics: String? = null
)

interface LyricsOvhApiService {
    @GET("v1/{artist}/{title}")
    suspend fun getLyrics(
        @Path("artist") artist: String,
        @Path("title") title: String
    ): LyricsOvhResponse
}

// -----------------------------------------------------
// 3. Open Source Lyrics Client Factory
// -----------------------------------------------------
object LrcLibClient {
    private const val LRCLIB_BASE_URL = "https://lrclib.net/"
    private const val LYRICS_OVH_BASE_URL = "https://api.lyrics.ovh/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    val api: LrcLibApiService by lazy {
        Retrofit.Builder()
            .baseUrl(LRCLIB_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(LrcLibApiService::class.java)
    }

    val lyricsOvhApi: LyricsOvhApiService by lazy {
        Retrofit.Builder()
            .baseUrl(LYRICS_OVH_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(LyricsOvhApiService::class.java)
    }
}
