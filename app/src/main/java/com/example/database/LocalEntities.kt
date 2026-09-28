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
    val isLiked: Boolean = false,
    val isDownloaded: Boolean = false,
    val localFilePath: String? = null,
    val downloadProgress: Int = 0,
    val lastPlaybackPositionMs: Long = 0L,
    val lastPlayedAt: Long = 0L,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "local_playlists")
data class LocalPlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val coverUrl: String,
    val description: String,
    val songs: List<Song> = emptyList()
)

@Entity(tableName = "recent_queries")
data class RecentQueryEntity(
    @PrimaryKey val queryText: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "recent_songs_played")
data class RecentSongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val albumArtUrl: String,
    val streamUrl: String,
    val durationMs: Long,
    val lyrics: String? = null,
    val playedAt: Long = System.currentTimeMillis(),
    val lastPlaybackPositionMs: Long = 0L,
    val isDownloaded: Boolean = false,
    val localFilePath: String? = null
)

@Entity(tableName = "playback_progress")
data class PlaybackProgressEntity(
    @PrimaryKey val songId: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long = System.currentTimeMillis()
)
