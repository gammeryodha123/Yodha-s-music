package com.example.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalSongDao {
    @Query("SELECT * FROM local_songs ORDER BY lastPlayedAt DESC")
    suspend fun getAllSongs(): List<LocalSongEntity>

    @Query("SELECT * FROM local_songs WHERE isDownloaded = 1")
    fun getDownloadedSongs(): Flow<List<LocalSongEntity>>

    @Query("SELECT * FROM local_songs WHERE id = :songId LIMIT 1")
    suspend fun getSongById(songId: String): LocalSongEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: LocalSongEntity)

    @Query("UPDATE local_songs SET isDownloaded = :isDownloaded, localFilePath = :localFilePath, downloadProgress = :progress WHERE id = :songId")
    suspend fun updateDownloadStatus(songId: String, isDownloaded: Boolean, localFilePath: String?, progress: Int)

    @Query("SELECT * FROM local_songs WHERE isLiked = 1")
    fun getLikedSongs(): Flow<List<LocalSongEntity>>

    @Query("UPDATE local_songs SET isLiked = :isLiked WHERE id = :songId")
    suspend fun updateLikedStatus(songId: String, isLiked: Boolean)

    @Query("UPDATE local_songs SET lastPlaybackPositionMs = :positionMs WHERE id = :songId")
    suspend fun updatePlaybackPosition(songId: String, positionMs: Long)
}

@Dao
interface RecentSongDao {
    @Query("SELECT * FROM recent_songs ORDER BY playedAt DESC LIMIT 20")
    fun getRecentSongsFlow(): Flow<List<RecentSongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentSong(song: RecentSongEntity)

    @Query("UPDATE recent_songs SET lastPlaybackPositionMs = :positionMs WHERE id = :songId")
    suspend fun updateRecentSongProgress(songId: String, positionMs: Long)

    @Query("DELETE FROM recent_songs WHERE id = :songId")
    suspend fun deleteRecentSong(songId: String)

    @Query("DELETE FROM recent_songs")
    suspend fun clearAllRecent()
}

@Dao
interface PlaybackProgressDao {
    @Query("SELECT * FROM playback_progress WHERE songId = :songId LIMIT 1")
    suspend fun getProgressDirect(songId: String): PlaybackProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: PlaybackProgressEntity)
}
