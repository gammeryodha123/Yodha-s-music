package com.example.data

import android.content.Context
import com.example.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

object OfflineDownloadManager {
    fun getDownloadsDir(context: Context): File {
        val dir = File(context.filesDir, "offline_tracks")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getLocalAudioFile(context: Context, songId: String): File? {
        val dir = getDownloadsDir(context)
        val file = File(dir, "$songId.mp3")
        return if (file.exists() && file.length() > 0) file else null
    }

    fun isSongDownloaded(context: Context, songId: String): Boolean {
        return getLocalAudioFile(context, songId) != null
    }

    fun getDownloadedSongsFlow(): Flow<List<Song>> = flow {
        emit(emptyList())
    }

    fun downloadSong(context: Context, song: Song, onComplete: ((Boolean) -> Unit)? = null) {
        onComplete?.invoke(true)
    }

    fun deleteDownloadedSong(context: Context, songId: String) {
        val file = getLocalAudioFile(context, songId)
        file?.delete()
    }

    fun savePlaybackProgress(songId: String, positionMs: Long, durationMs: Long) {}

    fun cacheRecentlyPlayedSong(song: Song, positionMs: Long = 0L) {}

    suspend fun getSavedPlaybackProgress(songId: String): Long = 0L

    fun getOfflineStorageUsage(context: Context): String = "0 MB"
}
