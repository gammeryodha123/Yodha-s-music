package com.example.network

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// -----------------------------------------------------
// 1. LrcLib API Models & Retrofit Interface
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
// 2. NetEase Cloud Music Lyrics API Models & Interface
// -----------------------------------------------------
@JsonClass(generateAdapter = true)
data class NetEaseLyricItem(
    val lyric: String? = null
)

@JsonClass(generateAdapter = true)
data class NetEaseLyricResponse(
    val lrc: NetEaseLyricItem? = null,
    val tlyric: NetEaseLyricItem? = null
)

@JsonClass(generateAdapter = true)
data class NetEaseSongItem(
    val id: Long,
    val name: String? = null
)

@JsonClass(generateAdapter = true)
data class NetEaseSearchResult(
    val songs: List<NetEaseSongItem>? = null
)

@JsonClass(generateAdapter = true)
data class NetEaseSearchResponse(
    val result: NetEaseSearchResult? = null
)

interface NetEaseApiService {
    @GET("api/search/get/web")
    suspend fun searchSong(
        @Query("s") query: String,
        @Query("type") type: Int = 1,
        @Query("limit") limit: Int = 5
    ): NetEaseSearchResponse

    @GET("api/song/lyric")
    suspend fun getLyric(
        @Query("id") songId: Long,
        @Query("lv") lv: Int = -1,
        @Query("kv") kv: Int = -1,
        @Query("tv") tv: Int = -1
    ): NetEaseLyricResponse
}

// -----------------------------------------------------
// 3. Multi-Source Lyrics Client Factory
// -----------------------------------------------------
object LrcLibClient {
    private const val BASE_URL = "https://lrclib.net/"
    private const val NETEASE_BASE_URL = "https://music.163.com/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    val api: LrcLibApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(LrcLibApiService::class.java)
    }

    val netEaseApi: NetEaseApiService by lazy {
        Retrofit.Builder()
            .baseUrl(NETEASE_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(NetEaseApiService::class.java)
    }
}
