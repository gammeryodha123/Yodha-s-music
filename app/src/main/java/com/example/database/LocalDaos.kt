package com.example.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalSongDao {
    @Query("SELECT * FROM local_songs")
    fun getAllSongs(): Flow<List<LocalSongEntity>>

    @Query("SELECT * FROM local_songs WHERE isLiked = 1")
    fun getLikedSongs(): Flow<List<LocalSongEntity>>

    @Query("SELECT * FROM local_songs WHERE isDownloaded = 1 ORDER BY lastPlayedAt DESC")
    fun getDownloadedSongs(): Flow<List<LocalSongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<LocalSongEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: LocalSongEntity)

    @Query("UPDATE local_songs SET isLiked = :isLiked WHERE id = :songId")
    suspend fun updateLikedStatus(songId: String, isLiked: Boolean)

    @Query("UPDATE local_songs SET isDownloaded = :isDownloaded, localFilePath = :localFilePath, downloadProgress = :progress WHERE id = :songId")
    suspend fun updateDownloadStatus(songId: String, isDownloaded: Boolean, localFilePath: String?, progress: Int)

    @Query("UPDATE local_songs SET lastPlaybackPositionMs = :positionMs, lastPlayedAt = :playedAt WHERE id = :songId")
    suspend fun updatePlaybackPosition(songId: String, positionMs: Long, playedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM local_songs WHERE id = :songId LIMIT 1")
    suspend fun getSongById(songId: String): LocalSongEntity?

    @Query("DELETE FROM local_songs WHERE id = :songId")
    suspend fun deleteSong(songId: String)
}

@Dao
interface LocalPlaylistDao {
    @Query("SELECT * FROM local_playlists")
    fun getAllPlaylists(): Flow<List<LocalPlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: LocalPlaylistEntity)

    @Query("DELETE FROM local_playlists WHERE id = :playlistId")
    suspend fun deletePlaylistById(playlistId: String)

    @Query("SELECT * FROM local_playlists WHERE id = :playlistId LIMIT 1")
    suspend fun getPlaylistById(playlistId: String): LocalPlaylistEntity?
}

@Dao
interface RecentQueryDao {
    @Query("SELECT * FROM recent_queries ORDER BY timestamp DESC LIMIT 10")
    fun getRecentQueries(): Flow<List<RecentQueryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuery(query: RecentQueryEntity)

    @Query("DELETE FROM recent_queries WHERE queryText = :queryText")
    suspend fun deleteQuery(queryText: String)

    @Query("DELETE FROM recent_queries")
    suspend fun clearAllQueries()
}

@Dao
interface RecentSongDao {
    @Query("SELECT * FROM recent_songs_played ORDER BY playedAt DESC LIMIT 20")
    fun getRecentSongs(): Flow<List<RecentSongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentSong(song: RecentSongEntity)

    @Query("UPDATE recent_songs_played SET lastPlaybackPositionMs = :positionMs, playedAt = :playedAt WHERE id = :songId")
    suspend fun updateRecentSongProgress(songId: String, positionMs: Long, playedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM recent_songs_played WHERE id = :songId LIMIT 1")
    suspend fun getRecentSongById(songId: String): RecentSongEntity?

    @Query("DELETE FROM recent_songs_played WHERE id = :songId")
    suspend fun deleteRecentSong(songId: String)

    @Query("DELETE FROM recent_songs_played")
    suspend fun clearRecentSongs()
}

@Dao
interface PlaybackProgressDao {
    @Query("SELECT * FROM playback_progress WHERE songId = :songId LIMIT 1")
    fun getProgress(songId: String): Flow<PlaybackProgressEntity?>

    @Query("SELECT * FROM playback_progress WHERE songId = :songId LIMIT 1")
    suspend fun getProgressDirect(songId: String): PlaybackProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: PlaybackProgressEntity)

    @Query("SELECT * FROM playback_progress")
    fun getAllProgress(): Flow<List<PlaybackProgressEntity>>

    @Query("DELETE FROM playback_progress WHERE songId = :songId")
    suspend fun deleteProgress(songId: String)
}
