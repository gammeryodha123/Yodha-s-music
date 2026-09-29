package com.example.model

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val albumArtUrl: String,
    val streamUrl: String = "",
    val durationMs: Long = 180000L,
    val lyrics: String? = null,
    val isLiked: Boolean = false,
    val isDownloaded: Boolean = false,
    val localFilePath: String? = null,
    val lastPlaybackPositionMs: Long = 0L
)

data class Playlist(
    val id: String,
    val name: String,
    val description: String = "",
    val coverUrl: String = "",
    val songs: List<Song> = emptyList()
)
