package com.example.data

import android.content.Context
import com.example.database.LocalSongEntity
import com.example.database.PlaybackProgressEntity
import com.example.model.Song
import com.example.network.MusicSourcesManager
import com.example.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

object OfflineDownloadManager {
    private const val TAG = "OfflineDownloadManager"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val client = OkHttpClient()

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

    fun getDownloadedSongsFlow(): Flow<List<Song>> {
        val db = AppDatabaseHelper.database ?: return flow { emit(emptyList()) }
        return db.localSongDao().getDownloadedSongs().map { list ->
            list.map { entity ->
                Song(
                    id = entity.id,
                    title = entity.title,
                    artist = entity.artist,
                    albumArtUrl = entity.albumArtUrl,
                    streamUrl = entity.streamUrl,
                    durationMs = entity.durationMs,
                    lyrics = entity.lyrics ?: "",
                    isDownloaded = true,
                    localFilePath = entity.localFilePath,
                    genre = "Offline",
                    album = "Downloads"
                )
            }
        }
    }

    fun downloadSong(context: Context, song: Song, onComplete: ((Boolean) -> Unit)? = null) {
        scope.launch {
            try {
                val db = AppDatabaseHelper.database ?: throw IllegalStateException("Database is not ready")

                // Pre-insert local song entity
                val existing = db.localSongDao().getSongById(song.id)
                if (existing == null) {
                    db.localSongDao().insertSong(
                        LocalSongEntity(
                            id = song.id,
                            title = song.title,
                            artist = song.artist,
                            albumArtUrl = song.albumArtUrl,
                            streamUrl = song.streamUrl,
                            durationMs = song.durationMs,
                            lyrics = song.lyrics,
                            isLiked = false,
                            isDownloaded = false,
                            localFilePath = null,
                            downloadProgress = 0,
                            lastPlaybackPositionMs = 0L,
                            lastPlayedAt = System.currentTimeMillis()
                        )
                    )
                }

                var urlToDownload = song.streamUrl
                if (song.id.startsWith("piped_") || urlToDownload.contains("watch?v=") || urlToDownload.contains("piped")) {
                    val resolved = MusicSourcesManager.getPipedStreamUrl(song.id)
                    if (!resolved.isNullOrBlank()) {
                        urlToDownload = resolved
                    }
                }

                if (urlToDownload.isBlank()) {
                    throw IllegalArgumentException("No stream URL resolved for download")
                }

                val request = Request.Builder().url(urlToDownload).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw java.io.IOException("Http error code: ${response.code}")
                    val body = response.body ?: throw java.io.IOException("Empty response body")

                    val dir = getDownloadsDir(context)
                    val targetFile = File(dir, "${song.id}.mp3")

                    val totalBytes = body.contentLength()
                    var bytesDownloaded = 0L

                    body.byteStream().use { input ->
                        FileOutputStream(targetFile).use { output ->
                            val buffer = ByteArray(8 * 1024)
                            var bytesRead: Int
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                bytesDownloaded += bytesRead
                                if (totalBytes > 0) {
                                    val progress = ((bytesDownloaded * 100) / totalBytes).toInt()
                                    db.localSongDao().updateDownloadStatus(song.id, false, null, progress)
                                }
                            }
                        }
                    }

                    db.localSongDao().updateDownloadStatus(song.id, true, targetFile.absolutePath, 100)
                    AppLogger.i(TAG, "Completed downloading song ${song.id} to ${targetFile.absolutePath}")
                    onComplete?.invoke(true)
                }
            } catch (e: Throwable) {
                AppLogger.e(TAG, "Download failed for song ${song.id}: ${e.message}")
                onComplete?.invoke(false)
            }
        }
    }

    fun deleteDownloadedSong(context: Context, songId: String) {
        scope.launch {
            try {
                val file = getLocalAudioFile(context, songId)
                file?.delete()
                val db = AppDatabaseHelper.database
                db?.localSongDao()?.updateDownloadStatus(songId, false, null, 0)
            } catch (e: Throwable) {
                AppLogger.e(TAG, "Failed deleting offline track: ${e.message}")
            }
        }
    }

    fun savePlaybackProgress(songId: String, positionMs: Long, durationMs: Long = 0L) {
        scope.launch {
            try {
                val db = AppDatabaseHelper.database ?: return@launch
                db.playbackProgressDao().saveProgress(
                    PlaybackProgressEntity(
                        songId = songId,
                        positionMs = positionMs,
                        durationMs = durationMs,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                db.localSongDao().updatePlaybackPosition(songId, positionMs)
            } catch (e: Throwable) {
                AppLogger.w(TAG, "Error saving progress in DB: ${e.message}")
            }
        }
    }

    fun cacheRecentlyPlayedSong(song: Song, positionMs: Long = 0L) {
        scope.launch {
            try {
                val db = AppDatabaseHelper.database ?: return@launch
                db.recentSongDao().insertRecentSong(
                    com.example.database.RecentSongEntity(
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
            } catch (e: Throwable) {
                AppLogger.w(TAG, "Error caching recent play: ${e.message}")
            }
        }
    }

    suspend fun getSavedPlaybackProgress(songId: String): Long {
        val db = AppDatabaseHelper.database ?: return 0L
        return try {
            db.playbackProgressDao().getProgressDirect(songId)?.positionMs ?: 0L
        } catch (e: Throwable) {
            0L
        }
    }

    fun getOfflineStorageUsage(context: Context): String {
        return try {
            val dir = getDownloadsDir(context)
            val files = dir.listFiles() ?: return "0.00 MB"
            var totalBytes = 0L
            for (file in files) {
                if (file.isFile) {
                    totalBytes += file.length()
                }
            }
            val mb = totalBytes.toDouble() / (1024.0 * 1024.0)
            String.format("%.2f MB", mb)
        } catch (e: Throwable) {
            "0.00 MB"
        }
    }
}
