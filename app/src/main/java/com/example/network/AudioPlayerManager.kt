package com.example.network

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import com.example.model.Song
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AudioPlayerManager {
    private const val TAG = "AudioPlayerManager"

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null

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

    // Playlist Queue
    private var currentQueue: List<Song> = emptyList()

    init {
        initializePlayer()
    }

    private fun initializePlayer() {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setOnPreparedListener { mp ->
                    _isLoading.value = false
                    mp.start()
                    _isPlaying.value = true
                    _durationMs.value = mp.duration.toLong()
                    startProgressTracker()
                }
                setOnCompletionListener {
                    handleCompletion()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer Error: what=$what, extra=$extra")
                    _isLoading.value = false
                    _isPlaying.value = false
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaPlayer: ${e.message}")
        }
    }

    fun setQueue(queue: List<Song>) {
        currentQueue = queue
    }

    fun playSong(context: Context, song: Song) {
        scope.launch {
            try {
                _currentSong.value = song
                _isLoading.value = true
                _isPlaying.value = false
                _playbackPositionMs.value = 0L
                stopProgressTracker()

                // Reset and prep player
                initializePlayer()
                val player = mediaPlayer ?: return@launch

                // 1. Resolve direct stream URL dynamically based on source
                val directUrl = resolveStreamUrl(context, song)
                if (directUrl.isNullOrBlank()) {
                    Log.e(TAG, "Unable to resolve playable stream URL for song: ${song.title}")
                    _isLoading.value = false
                    return@launch
                }

                Log.d(TAG, "Playing resolved stream URL: $directUrl")
                player.setDataSource(directUrl)
                player.prepareAsync()
            } catch (e: Exception) {
                Log.e(TAG, "Error playing song: ${e.message}")
                _isLoading.value = false
            }
        }
    }

    private suspend fun resolveStreamUrl(context: Context, song: Song): String? = withContext(Dispatchers.IO) {
        return@withContext when {
            // Resolve YouTube / Piped direct audio stream URL
            song.id.startsWith("piped_") -> {
                val directUrl = MusicSourcesManager.getPipedStreamUrl(song.id)
                // Fallback to high quality direct stream URL or test URL if node fails
                directUrl ?: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
            }
            // Standard static / direct streaming URL
            song.streamUrl.isNotEmpty() -> {
                song.streamUrl
            }
            // Safety local fallback URL
            else -> {
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
            }
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer ?: return
        try {
            if (player.isPlaying) {
                player.pause()
                _isPlaying.value = false
                stopProgressTracker()
            } else {
                player.start()
                _isPlaying.value = true
                startProgressTracker()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling play/pause: ${e.message}")
        }
    }

    fun seekTo(positionMs: Long) {
        val player = mediaPlayer ?: return
        try {
            player.seekTo(positionMs.toInt())
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

        // Auto play next song if queue is available
        val song = _currentSong.value
        if (song != null && currentQueue.isNotEmpty()) {
            val index = currentQueue.indexOfFirst { it.id == song.id }
            if (index != -1 && index + 1 < currentQueue.size) {
                val ctx = com.example.database.AppDatabaseHelper.context
                if (ctx != null) {
                    playNext(ctx)
                }
            }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        _playbackPositionMs.value = mp.currentPosition.toLong()
                    }
                }
                delay(500L)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stopProgressTracker()
        mediaPlayer?.release()
        mediaPlayer = null
        _isPlaying.value = false
    }
}
