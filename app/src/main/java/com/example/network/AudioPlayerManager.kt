package com.example.network

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.AppDatabaseHelper
import com.example.data.AuthManager
import com.example.data.FirestoreManager
import com.example.data.OfflineDownloadManager
import com.example.database.RecentSongEntity
import com.example.model.Song
import com.example.util.AppLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

enum class RepeatMode(val displayName: String) {
    OFF("Off"),
    ALL("Repeat All"),
    ONE("Repeat One")
}

enum class AudioPreset(val displayName: String, val bassMultiplier: Float = 1.0f) {
    BALANCED("Standard Balanced", 1.0f),
    BASS_BOOST("Bass Boost (+6dB)", 1.4f),
    VOCAL_CLEAR("Vocal Clarity", 0.9f),
    TREBLE_BOOST("Treble Sparkle", 0.8f),
    HI_RES_LOSSLESS("Hi-Res Lossless Master", 1.2f)
}

object AudioPlayerManager {
    private const val TAG = "AudioPlayerManager"

    private var exoPlayer: ExoPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackPositionMs = MutableStateFlow(0L)
    val playbackPositionMs: StateFlow<Long> = _playbackPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _playlist = MutableStateFlow<List<Song>>(emptyList())
    val playlist: StateFlow<List<Song>> = _playlist.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _playbackPitch = MutableStateFlow(1.0f)
    val playbackPitch: StateFlow<Float> = _playbackPitch.asStateFlow()

    private val _isSkipSilenceEnabled = MutableStateFlow(false)
    val isSkipSilenceEnabled: StateFlow<Boolean> = _isSkipSilenceEnabled.asStateFlow()

    private val _audioPreset = MutableStateFlow(AudioPreset.BALANCED)
    val audioPreset: StateFlow<AudioPreset> = _audioPreset.asStateFlow()

    private val _sleepTimerMinutesRemaining = MutableStateFlow<Int?>(null)
    val sleepTimerMinutesRemaining: StateFlow<Int?> = _sleepTimerMinutesRemaining.asStateFlow()

    private var sleepTimerJob: Job? = null

    fun getOrCreatePlayer(context: Context): ExoPlayer {
        if (exoPlayer == null) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build()

            exoPlayer = ExoPlayer.Builder(context.applicationContext)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .build().apply {
                    addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            _isPlaying.value = isPlaying
                            if (isPlaying) {
                                startProgressTracking()
                            } else {
                                stopProgressTracking()
                            }
                        }

                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (playbackState == Player.STATE_READY) {
                                _durationMs.value = duration.coerceAtLeast(0L)
                            } else if (playbackState == Player.STATE_ENDED) {
                                handleSongCompletion(context)
                            }
                        }
                    })
                }
        }
        return exoPlayer!!
    }

    private fun handleSongCompletion(context: Context) {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                seekTo(0L)
                exoPlayer?.play()
            }
            RepeatMode.ALL -> {
                playNext(context)
            }
            RepeatMode.OFF -> {
                val list = _playlist.value
                val current = _currentSong.value
                val currentIndex = list.indexOfFirst { it.id == current?.id }
                if (currentIndex in 0 until list.size - 1) {
                    playNext(context)
                } else {
                    _isPlaying.value = false
                }
            }
        }
    }

    fun playSong(context: Context, song: Song, queue: List<Song> = emptyList()) {
        val player = getOrCreatePlayer(context)
        _currentSong.value = song
        if (queue.isNotEmpty()) {
            _playlist.value = queue
        } else if (_playlist.value.none { it.id == song.id }) {
            _playlist.value = listOf(song)
        }

        // Firebase & Room Backend Logging
        scope.launch(Dispatchers.IO) {
            val currentUser = AuthManager.currentUser.value
            if (currentUser != null) {
                FirestoreManager.recordRecentlyPlayed(currentUser.id, song)
            }

            try {
                val db = AppDatabaseHelper.database
                db?.recentSongDao()?.insertRecentSong(
                    RecentSongEntity(
                        id = song.id,
                        title = song.title,
                        artist = song.artist,
                        albumArtUrl = song.albumArtUrl,
                        streamUrl = song.streamUrl,
                        durationMs = song.durationMs,
                        lyrics = song.lyrics,
                        playedAt = System.currentTimeMillis(),
                        lastPlaybackPositionMs = 0L,
                        isDownloaded = song.isDownloaded,
                        localFilePath = song.localFilePath
                    )
                )
            } catch (e: Throwable) {
                AppLogger.w(TAG, "Room recent insert error: ${e.message}")
            }
        }

        scope.launch(Dispatchers.IO) {
            val localFile: File? = OfflineDownloadManager.getLocalAudioFile(context, song.id)
            val isOffline = localFile != null && localFile.exists()

            val mediaUri: Uri = if (isOffline && localFile != null) {
                Uri.fromFile(localFile)
            } else {
                var rawStreamUrl = song.streamUrl
                if (song.id.startsWith("piped_") || rawStreamUrl.contains("watch?v=") || rawStreamUrl.contains("piped")) {
                    val resolvedStream = MusicSourcesManager.getPipedStreamUrl(song.id)
                    if (!resolvedStream.isNullOrBlank()) {
                        rawStreamUrl = resolvedStream
                    }
                }
                val directUrl = rawStreamUrl.ifBlank {
                    "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
                }
                Uri.parse(directUrl)
            }

            val metadata = MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artist)
                .build()

            val mediaItem = MediaItem.Builder()
                .setMediaId(song.id)
                .setUri(mediaUri)
                .setMediaMetadata(metadata)
                .build()

            val savedProgress = OfflineDownloadManager.getSavedPlaybackProgress(song.id)

            withContext(Dispatchers.Main) {
                player.setMediaItem(mediaItem)
                if (savedProgress > 2000L && (song.durationMs <= 0 || savedProgress < (song.durationMs - 5000L))) {
                    player.seekTo(savedProgress)
                    _playbackPositionMs.value = savedProgress
                }
                player.prepare()
                player.playWhenReady = true
                _isPlaying.value = true

                val updatedSong = song.copy(
                    isDownloaded = isOffline,
                    localFilePath = localFile?.absolutePath
                )
                _currentSong.value = updatedSong
                OfflineDownloadManager.cacheRecentlyPlayedSong(updatedSong, savedProgress)
            }
        }
    }

    fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                _isPlaying.value = false
            } else {
                player.play()
                _isPlaying.value = true
            }
        }
    }

    fun playNext(context: Context) {
        val list = _playlist.value
        if (list.isEmpty()) return
        val current = _currentSong.value
        val currentIndex = list.indexOfFirst { it.id == current?.id }

        val nextIndex = if (_isShuffleEnabled.value) {
            (list.indices).random()
        } else {
            (currentIndex + 1) % list.size
        }

        playSong(context, list[nextIndex], list)
    }

    fun playPrevious(context: Context) {
        val list = _playlist.value
        if (list.isEmpty()) return
        val current = _currentSong.value
        val currentIndex = list.indexOfFirst { it.id == current?.id }

        val prevIndex = if (currentIndex <= 0) list.size - 1 else currentIndex - 1
        playSong(context, list[prevIndex], list)
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _playbackPositionMs.value = positionMs

        val activeSong = _currentSong.value
        if (activeSong != null) {
            OfflineDownloadManager.savePlaybackProgress(activeSong.id, positionMs)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        exoPlayer?.let { player ->
            player.playbackParameters = androidx.media3.common.PlaybackParameters(speed, _playbackPitch.value)
        }
    }

    fun setPlaybackPitch(pitch: Float) {
        _playbackPitch.value = pitch
        exoPlayer?.let { player ->
            player.playbackParameters = androidx.media3.common.PlaybackParameters(_playbackSpeed.value, pitch)
        }
    }

    fun setPlaybackSpeedAndPitch(speed: Float, pitch: Float) {
        _playbackSpeed.value = speed
        _playbackPitch.value = pitch
        exoPlayer?.let { player ->
            player.playbackParameters = androidx.media3.common.PlaybackParameters(speed, pitch)
        }
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun toggleSkipSilence() {
        val nextValue = !_isSkipSilenceEnabled.value
        _isSkipSilenceEnabled.value = nextValue
        exoPlayer?.skipSilenceEnabled = nextValue
    }

    fun setAudioPreset(preset: AudioPreset) {
        _audioPreset.value = preset
        AppLogger.i(TAG, "Applied audio preset: ${preset.displayName}")
    }

    fun toggleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    fun toggleShuffle() {
        _isShuffleEnabled.value = !_isShuffleEnabled.value
    }

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        if (minutes == null || minutes <= 0) {
            _sleepTimerMinutesRemaining.value = null
            return
        }

        _sleepTimerMinutesRemaining.value = minutes
        sleepTimerJob = scope.launch(Dispatchers.IO) {
            var remaining = minutes
            while (remaining > 0) {
                delay(60000L)
                remaining--
                _sleepTimerMinutesRemaining.value = remaining
            }
            withContext(Dispatchers.Main) {
                exoPlayer?.pause()
                _isPlaying.value = false
                _sleepTimerMinutesRemaining.value = null
            }
        }
    }

    fun addToQueue(song: Song) {
        if (_playlist.value.none { it.id == song.id }) {
            _playlist.value = _playlist.value + song
        }
    }

    fun removeFromQueue(index: Int) {
        val currentList = _playlist.value.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            _playlist.value = currentList
        }
    }

    private fun startProgressTracking() {
        stopProgressTracking()
        progressJob = scope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    if (player.isPlaying) {
                        val pos = player.currentPosition
                        _playbackPositionMs.value = pos

                        val activeSong = _currentSong.value
                        if (activeSong != null && pos > 0) {
                            OfflineDownloadManager.savePlaybackProgress(activeSong.id, pos)
                        }
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stopProgressTracking()
        sleepTimerJob?.cancel()
        exoPlayer?.release()
        exoPlayer = null
    }
}
