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
