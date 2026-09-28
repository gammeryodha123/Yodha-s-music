package com.example.data

import android.content.Context
import android.util.Log
import com.example.database.AppDatabaseHelper
import com.example.database.LocalSongEntity
import com.example.database.PlaybackProgressEntity
import com.example.database.RecentSongEntity
import com.example.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.DecimalFormat

sealed class DownloadState {
    object Idle : DownloadState()
    data class InProgress(val progressPercent: Int) : DownloadState()
    object Completed : DownloadState()
    data class Error(val message: String) : DownloadState()
}

object OfflineDownloadManager {
    private const val TAG = "OfflineDownloadManager"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    // Map of songId -> live download state
    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadState>> = _downloadStates.asStateFlow()

    // Get downloads directory
    fun getDownloadsDir(context: Context): File {
        val dir = File(context.filesDir, "offline_tracks")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    // Check if song has a local downloaded file
    fun getLocalAudioFile(context: Context, songId: String): File? {
        val dir = getDownloadsDir(context)
        val file = File(dir, "$songId.mp3")
        return if (file.exists() && file.length() > 0) file else null
    }

    fun isSongDownloaded(context: Context, songId: String): Boolean {
        return getLocalAudioFile(context, songId) != null
    }

    // Flow of all downloaded songs from Room database
    fun getDownloadedSongsFlow(): Flow<List<Song>> {
        val db = AppDatabaseHelper.database
        return db.localSongDao().getDownloadedSongs().map { entities ->
            entities.map { entity ->
                Song(
                    id = entity.id,
                    title = entity.title,
                    artist = entity.artist,
                    albumArtUrl = entity.albumArtUrl,
                    streamUrl = entity.streamUrl,
                    durationMs = entity.durationMs,
                    lyrics = entity.lyrics,
                    localFilePath = entity.localFilePath,
                    isDownloaded = true,
                    lastPlaybackPositionMs = entity.lastPlaybackPositionMs
                )
            }
        }
    }

    // Download a song and cache its metadata in Room Database
    fun downloadSong(context: Context, song: Song, onComplete: ((Boolean) -> Unit)? = null) {
        val directUrl = song.streamUrl.ifBlank {
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-${(Math.abs(song.id.hashCode()) % 16) + 1}.mp3"
        }

        updateDownloadState(song.id, DownloadState.InProgress(0))

        scope.launch {
            try {
                val dir = getDownloadsDir(context)
                val targetFile = File(dir, "${song.id}.mp3")
                val tempFile = File(dir, "${song.id}.tmp")

                val request = Request.Builder().url(directUrl).build()
                val response = httpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    throw Exception("HTTP download error: ${response.code}")
                }

                val body = response.body ?: throw Exception("Empty response body")
                val totalBytes = body.contentLength()
                val inputStream: InputStream = body.byteStream()
                val outputStream = FileOutputStream(tempFile)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytesRead = 0L
                var lastReportedPercent = 0

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead

                    if (totalBytes > 0) {
                        val percent = ((totalBytesRead * 100) / totalBytes).toInt().coerceIn(0, 100)
                        if (percent != lastReportedPercent) {
                            lastReportedPercent = percent
                            updateDownloadState(song.id, DownloadState.InProgress(percent))
                        }
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                if (tempFile.exists()) {
                    if (targetFile.exists()) targetFile.delete()
                    tempFile.renameTo(targetFile)
                }

                // Save song metadata with offline download path in Room
                val db = AppDatabaseHelper.database
                val existing = db.localSongDao().getSongById(song.id)
                val entity = LocalSongEntity(
                    id = song.id,
                    title = song.title,
                    artist = song.artist,
                    albumArtUrl = song.albumArtUrl,
                    streamUrl = song.streamUrl,
                    durationMs = song.durationMs,
                    lyrics = song.lyrics,
                    isLiked = existing?.isLiked ?: false,
                    isDownloaded = true,
                    localFilePath = targetFile.absolutePath,
                    downloadProgress = 100,
                    lastPlaybackPositionMs = existing?.lastPlaybackPositionMs ?: 0L,
                    lastPlayedAt = System.currentTimeMillis()
                )
                db.localSongDao().insertSong(entity)

                updateDownloadState(song.id, DownloadState.Completed)
                Log.d(TAG, "Song ${song.title} downloaded successfully to ${targetFile.absolutePath}")
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Download failed for ${song.title}: ${e.message}", e)
                updateDownloadState(song.id, DownloadState.Error(e.localizedMessage ?: "Download failed"))
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(false)
                }
            }
        }
    }

    // Delete a downloaded song
    fun deleteDownloadedSong(context: Context, songId: String) {
        scope.launch {
            try {
                val file = getLocalAudioFile(context, songId)
                file?.delete()

                val db = AppDatabaseHelper.database
                val existing = db.localSongDao().getSongById(songId)
                if (existing != null) {
                    db.localSongDao().updateDownloadStatus(
                        songId = songId,
                        isDownloaded = false,
                        localFilePath = null,
                        progress = 0
                    )
                }

                val currentMap = _downloadStates.value.toMutableMap()
                currentMap.remove(songId)
                _downloadStates.value = currentMap
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete downloaded song: ${e.message}")
            }
        }
    }

    // Save playback progress to Room database
    fun savePlaybackProgress(songId: String, positionMs: Long, durationMs: Long) {
        if (songId.isBlank() || positionMs < 0L) return
        scope.launch {
            try {
                val db = AppDatabaseHelper.database
                db.playbackProgressDao().saveProgress(
                    PlaybackProgressEntity(
                        songId = songId,
                        positionMs = positionMs,
                        durationMs = durationMs,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                db.localSongDao().updatePlaybackPosition(songId, positionMs)
                db.recentSongDao().updateRecentSongProgress(songId, positionMs)
            } catch (e: Exception) {
                Log.e(TAG, "Failed saving playback progress: ${e.message}")
            }
        }
    }

    // Cache recently played song into Room
    fun cacheRecentlyPlayedSong(song: Song, positionMs: Long = 0L) {
        scope.launch {
            try {
                val db = AppDatabaseHelper.database
                // Cache into recent songs table
                db.recentSongDao().insertRecentSong(
                    RecentSongEntity(
                        id = song.id,
                        title = song.title,
                        artist = song.artist,
                        albumArtUrl = song.albumArtUrl,
                        streamUrl = song.streamUrl,
                        durationMs = song.durationMs,
                        lyrics = song.lyrics,
                        playedAt = System.currentTimeMillis(),
                        lastPlaybackPositionMs = positionMs,
                        isDownloaded = song.isDownloaded,
                        localFilePath = song.localFilePath
                    )
                )
                // Cache into local songs table
                val existing = db.localSongDao().getSongById(song.id)
                db.localSongDao().insertSong(
                    LocalSongEntity(
                        id = song.id,
                        title = song.title,
                        artist = song.artist,
                        albumArtUrl = song.albumArtUrl,
                        streamUrl = song.streamUrl,
                        durationMs = song.durationMs,
                        lyrics = song.lyrics,
                        isLiked = existing?.isLiked ?: false,
                        isDownloaded = existing?.isDownloaded ?: song.isDownloaded,
                        localFilePath = existing?.localFilePath ?: song.localFilePath,
                        downloadProgress = existing?.downloadProgress ?: 0,
                        lastPlaybackPositionMs = positionMs,
                        lastPlayedAt = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed caching recently played song: ${e.message}")
            }
        }
    }

    suspend fun getSavedPlaybackProgress(songId: String): Long = withContext(Dispatchers.IO) {
        return@withContext try {
            val db = AppDatabaseHelper.database
            val progress = db.playbackProgressDao().getProgressDirect(songId)
            progress?.positionMs ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    fun getOfflineStorageUsage(context: Context): String {
        val dir = getDownloadsDir(context)
        val files = dir.listFiles() ?: return "0 MB"
        val totalBytes = files.sumOf { it.length() }
        val mb = totalBytes.toDouble() / (1024 * 1024)
        return DecimalFormat("#,##0.0").format(mb) + " MB"
    }

    private fun updateDownloadState(songId: String, state: DownloadState) {
        val current = _downloadStates.value.toMutableMap()
        current[songId] = state
        _downloadStates.value = current
    }
}
