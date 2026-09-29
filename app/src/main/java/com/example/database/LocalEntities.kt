package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_songs")
data class LocalSongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val albumArtUrl: String,
    val streamUrl: String,
    val durationMs: Long,
    val lyrics: String?,
    val isLiked: Boolean,
    val isDownloaded: Boolean,
    val localFilePath: String?,
    val downloadProgress: Int,
    val lastPlaybackPositionMs: Long,
    val lastPlayedAt: Long
)

@Entity(tableName = "recent_songs")
data class RecentSongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val albumArtUrl: String,
    val streamUrl: String,
    val durationMs: Long,
    val lyrics: String?,
    val playedAt: Long,
    val lastPlaybackPositionMs: Long,
    val isDownloaded: Boolean,
    val localFilePath: String?
)

@Entity(tableName = "playback_progress")
data class PlaybackProgressEntity(
    @PrimaryKey val songId: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long
)
