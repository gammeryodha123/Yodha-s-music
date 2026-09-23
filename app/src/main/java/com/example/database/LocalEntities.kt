package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.Song

@Entity(tableName = "local_songs")
data class LocalSongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val albumArtUrl: String,
    val streamUrl: String,
    val durationMs: Long,
    val lyrics: String? = null,
    val isLiked: Boolean = false
)

@Entity(tableName = "local_playlists")
data class LocalPlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val coverUrl: String,
    val description: String,
    val songs: List<Song> = emptyList()
)
