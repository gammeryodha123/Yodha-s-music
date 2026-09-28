package com.example.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Song(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val albumArtUrl: String = "",
    val streamUrl: String = "",
    val durationMs: Long = 0L,
    val lyrics: String? = null,
    val localFilePath: String? = null,
    val isDownloaded: Boolean = false,
    val lastPlaybackPositionMs: Long = 0L
)

@JsonClass(generateAdapter = true)
data class Playlist(
    val id: String = "",
    val name: String = "",
    val coverUrl: String = "",
    val description: String = "",
    val songs: List<Song> = emptyList()
)
