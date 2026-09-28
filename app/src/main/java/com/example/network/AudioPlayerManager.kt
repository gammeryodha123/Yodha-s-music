package com.example.network

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.LyricLine
import com.example.data.LyricsRepository
import com.example.model.Song
import com.example.service.MusicPlaybackService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AudioPlayerManager {
    private const val TAG = "AudioPlayerManager"

    private var exoPlayer: ExoPlayer? = null
    private var serviceContext: Context? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null
    private var lyricsJob: Job? = null
    private val lyricsRepository = LyricsRepository()

    // Player State Flows
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackPositionMs = MutableStateFlow(0L)
    val playbackPositionMs: StateFlow<Long> = _playbackPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Backend Initialized Lyrics Flows
    private val _currentLyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val currentLyrics: StateFlow<List<LyricLine>> = _currentLyrics.asStateFlow()

    private val _isLyricsLoading = MutableStateFlow(false)
    val isLyricsLoading: StateFlow<Boolean> = _isLyricsLoading.asStateFlow()

    // Playlist Queue
    private var currentQueue: List<Song> = emptyList()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            if (isPlaying) {
                startProgressTracker()
            } else {
                stopProgressTracker()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    _isLoading.value = true
                }
                Player.STATE_READY -> {
                    _isLoading.value = false
                    exoPlayer?.let { player ->
                        _durationMs.value = player.duration.coerceAtLeast(0L)
                    }
                }
                Player.STATE_ENDED -> {
                    _isLoading.value = false
                    handleCompletion()
                }
                Player.STATE_IDLE -> {
                    _isLoading.value = false
                }
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            Log.e(TAG, "ExoPlayer Error: ${error.message}", error)
            _isLoading.value = false
            _isPlaying.value = false
        }
    }

    fun attachPlayer(player: ExoPlayer, context: Context) {
        exoPlayer?.removeListener(playerListener)
        exoPlayer = player
        serviceContext = context.applicationContext
        player.addListener(playerListener)
        _isPlaying.value = player.isPlaying
        if (player.duration > 0) {
            _durationMs.value = player.duration
        }
    }

    fun detachPlayer() {
        exoPlayer?.removeListener(playerListener)
        exoPlayer = null
        stopProgressTracker()
        _isPlaying.value = false
    }

    fun initialize(context: Context) {
        if (exoPlayer != null) return
        try {
            // Start MusicPlaybackService to maintain persistent background playback session
            val serviceIntent = Intent(context.applicationContext, MusicPlaybackService::class.java)
            try {
                ContextCompat.startForegroundService(context.applicationContext, serviceIntent)
            } catch (e: Exception) {
                Log.w(TAG, "Starting foreground service via intent failed, fallback to direct binding: ${e.message}")
            }

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build()

            val player = ExoPlayer.Builder(context.applicationContext)
                .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .build()

            player.addListener(playerListener)
            exoPlayer = player
            serviceContext = context.applicationContext
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ExoPlayer: ${e.message}")
        }
    }

    fun setQueue(queue: List<Song>) {
        currentQueue = queue
    }

    fun playSong(context: Context, song: Song) {
        initialize(context)
        
        // Asynchronously initialize lyrics in backend
        initializeBackendLyrics(song)

        scope.launch {
            try {
                _currentSong.value = song
                _isLoading.value = true
                _isPlaying.value = false
                _playbackPositionMs.value = 0L
                stopProgressTracker()

                // Start MusicPlaybackService if not running
                val serviceIntent = Intent(context.applicationContext, MusicPlaybackService::class.java)
                try {
                    ContextCompat.startForegroundService(context.applicationContext, serviceIntent)
                } catch (e: Exception) {
                    Log.d(TAG, "Service start attempt: ${e.message}")
                }

                val player = exoPlayer ?: return@launch

                // 1. Check if song has been downloaded offline to local storage
                val localFile = com.example.data.OfflineDownloadManager.getLocalAudioFile(context, song.id)
                val isOfflineAvailable = localFile != null && localFile.exists() && localFile.length() > 0

                val mediaUri: Uri = if (isOfflineAvailable) {
                    Log.d(TAG, "Playing offline cached audio file: ${localFile!!.absolutePath}")
                    Uri.fromFile(localFile)
                } else {
                    val directUrl = resolveStreamUrl(context, song)
                    if (directUrl.isNullOrBlank()) {
                        Log.e(TAG, "Unable to resolve stream URL for song: ${song.title}")
                        _isLoading.value = false
                        return@launch
                    }
                    Uri.parse(directUrl)
                }

                val metadata = MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setArtworkUri(if (song.albumArtUrl.isNotBlank()) Uri.parse(song.albumArtUrl) else null)
                    .build()

                val mediaItem = MediaItem.Builder()
                    .setMediaId(song.id)
                    .setUri(mediaUri)
                    .setMediaMetadata(metadata)
                    .build()

                player.setMediaItem(mediaItem)

                // 2. Check and restore saved playback progress from Room database
                val savedProgress = com.example.data.OfflineDownloadManager.getSavedPlaybackProgress(song.id)
                if (savedProgress > 2000L && (song.durationMs <= 0 || savedProgress < (song.durationMs - 5000L))) {
                    Log.d(TAG, "Resuming ${song.title} from saved progress: ${savedProgress}ms")
                    player.seekTo(savedProgress)
                    _playbackPositionMs.value = savedProgress
                }

                player.prepare()
                player.playWhenReady = true

                // 3. Cache song metadata into Room
                val updatedSong = song.copy(
                    isDownloaded = isOfflineAvailable,
                    localFilePath = localFile?.absolutePath
                )
                _currentSong.value = updatedSong
                com.example.data.OfflineDownloadManager.cacheRecentlyPlayedSong(updatedSong, savedProgress)
            } catch (e: Exception) {
                Log.e(TAG, "Error playing song: ${e.message}")
                _isLoading.value = false
            }
        }
    }

    private fun initializeBackendLyrics(song: Song) {
        lyricsJob?.cancel()
        _currentLyrics.value = emptyList()
        _isLyricsLoading.value = true

        lyricsJob = scope.launch(Dispatchers.IO) {
            try {
                val fetched = lyricsRepository.fetchLyrics(song)
                _currentLyrics.value = fetched.lines
                Log.d(TAG, "Backend initialized ${fetched.lines.size} lyric lines for ${song.title}")
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing lyrics in backend: ${e.message}")
            } finally {
                _isLyricsLoading.value = false
            }
        }
    }

    private suspend fun resolveStreamUrl(context: Context, song: Song): String? = withContext(Dispatchers.IO) {
        return@withContext when {
            song.streamUrl.isNotBlank() -> song.streamUrl
            song.id.startsWith("piped_") -> {
                val directUrl = MusicSourcesManager.getPipedStreamUrl(song.id)
                directUrl ?: getFallbackStreamUrl(song.id)
            }
            else -> getFallbackStreamUrl(song.id.ifEmpty { song.title })
        }
    }

    private fun getFallbackStreamUrl(key: String): String {
        val index = (Math.abs(key.hashCode()) % 16) + 1
        return "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-$index.mp3"
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        try {
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling play/pause: ${e.message}")
        }
    }

    fun seekTo(positionMs: Long) {
        val player = exoPlayer ?: return
        try {
            player.seekTo(positionMs)
            _playbackPositionMs.value = positionMs
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking: ${e.message}")
        }
    }

    fun playNext(context: Context) {
        val song = _currentSong.value ?: return
        if (currentQueue.isNotEmpty()) {
            val index = currentQueue.indexOfFirst { it.id == song.id }
            if (index != -1) {
                val nextIndex = (index + 1) % currentQueue.size
                playSong(context, currentQueue[nextIndex])
            }
        }
    }

    fun playPrevious(context: Context) {
        val song = _currentSong.value ?: return
        if (currentQueue.isNotEmpty()) {
            val index = currentQueue.indexOfFirst { it.id == song.id }
            if (index != -1) {
                val prevIndex = if (index - 1 < 0) currentQueue.size - 1 else index - 1
                playSong(context, currentQueue[prevIndex])
            }
        }
    }

    private fun handleCompletion() {
        _isPlaying.value = false
        stopProgressTracker()
        _playbackPositionMs.value = _durationMs.value

        val song = _currentSong.value
        if (song != null && currentQueue.isNotEmpty()) {
            val index = currentQueue.indexOfFirst { it.id == song.id }
            if (index != -1 && index + 1 < currentQueue.size) {
                val ctx = serviceContext ?: com.example.database.AppDatabaseHelper.context
                if (ctx != null) {
                    playNext(ctx)
                }
            }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            var loopCount = 0
            while (isActive) {
                exoPlayer?.let { player ->
                    if (player.isPlaying) {
                        val currentPos = player.currentPosition.coerceAtLeast(0L)
                        _playbackPositionMs.value = currentPos
                        if (player.duration > 0) {
                            _durationMs.value = player.duration
                        }

                        // Save progress every ~3 seconds (6 loops of 500ms)
                        loopCount++
                        if (loopCount % 6 == 0) {
                            val activeSong = _currentSong.value
                            if (activeSong != null) {
                                com.example.data.OfflineDownloadManager.savePlaybackProgress(
                                    songId = activeSong.id,
                                    positionMs = currentPos,
                                    durationMs = player.duration.coerceAtLeast(0L)
                                )
                            }
                        }
                    }
                }
                delay(500L)
            }
        }
    }

    private fun stopProgressTracker() {
        // Save current progress on stop
        val activeSong = _currentSong.value
        val player = exoPlayer
        if (activeSong != null && player != null) {
            com.example.data.OfflineDownloadManager.savePlaybackProgress(
                songId = activeSong.id,
                positionMs = player.currentPosition.coerceAtLeast(0L),
                durationMs = player.duration.coerceAtLeast(0L)
            )
        }
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stopProgressTracker()
        lyricsJob?.cancel()
        exoPlayer?.release()
        exoPlayer = null
        _isPlaying.value = false
    }
}
