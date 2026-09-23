package com.example.database

import androidx.room.TypeConverter
import com.example.model.Song
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class RoomTypeConverters {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val songListType = Types.newParameterizedType(List::class.java, Song::class.java)
    private val songListAdapter = moshi.adapter<List<Song>>(songListType)

    @TypeConverter
    fun fromSongList(songs: List<Song>?): String? {
        return songs?.let { songListAdapter.toJson(it) }
    }

    @TypeConverter
    fun toSongList(json: String?): List<Song>? {
        return json?.let { songListAdapter.fromJson(it) }
    }
}
