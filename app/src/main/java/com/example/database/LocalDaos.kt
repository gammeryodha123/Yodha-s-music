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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<LocalSongEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: LocalSongEntity)

    @Query("UPDATE local_songs SET isLiked = :isLiked WHERE id = :songId")
    suspend fun updateLikedStatus(songId: String, isLiked: Boolean)

    @Query("SELECT * FROM local_songs WHERE id = :songId LIMIT 1")
    suspend fun getSongById(songId: String): LocalSongEntity?
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

    @Query("DELETE FROM recent_songs_played WHERE id = :songId")
    suspend fun deleteRecentSong(songId: String)

    @Query("DELETE FROM recent_songs_played")
    suspend fun clearRecentSongs()
}
