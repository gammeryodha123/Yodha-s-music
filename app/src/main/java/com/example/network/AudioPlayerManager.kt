package com.example.network

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.LyricLine
import com.example.data.LyricsRepository
import com.example.data.OfflineDownloadManager
import com.example.model.Song
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class RepeatMode(val displayName: String) {
    OFF("Off"),
    ALL("Repeat All"),
    ONE("Repeat One")
}

enum class AudioPreset(val displayName: String, val bassMultiplier: Float, val trebleMultiplier: Float) {
    BALANCED("Balanced / Flat", 1.0f, 1.0f),
    BASS_BOOST("Bass Boost 🎧", 1.4f, 0.9f),
    VOCAL_CLARITY("Vocal Clarity 🎙️", 0.8f, 1.3f),
    SPATIAL_3D("Spatial 3D Audio 🌌", 1.2f, 1.2f),
    ELECTRONIC("Electronic Club ⚡", 1.3f, 1.2f),
    ACOUSTIC("Acoustic Warmth ☕", 1.1f, 1.1f)
}

object AudioPlayerManager {
    private const val TAG = "AudioPlayerManager"
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val lyricsRepository = LyricsRepository()

    private var exoPlayer: ExoPlayer? = null

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

    private val _currentLyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val currentLyrics: StateFlow<List<LyricLine>> = _currentLyrics.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.ALL)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _audioPreset = MutableStateFlow(AudioPreset.BALANCED)
    val audioPreset: StateFlow<AudioPreset> = _audioPreset.asStateFlow()

    private val _sleepTimerMinutesRemaining = MutableStateFlow<Int?>(null)
    val sleepTimerMinutesRemaining: StateFlow<Int?> = _sleepTimerMinutesRemaining.asStateFlow()

    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null

    private fun getOrCreatePlayer(context: Context): ExoPlayer {
        return exoPlayer ?: ExoPlayer.Builder(context.applicationContext).build().also { player ->
            exoPlayer = player
            player.playbackParameters = PlaybackParameters(_playbackSpeed.value)
            player.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    _isPlaying.value = playing
                    if (playing) {
                        startProgressTracker()
                    } else {
                        stopProgressTracker()
                    }
                }

                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        handleSongCompletion(context)
                    }
                }
            })
        }
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

        scope.launch(Dispatchers.IO) {
            val localFile = OfflineDownloadManager.getLocalAudioFile(context, song.id)
            val isOffline = localFile != null && localFile.exists()

            val mediaUri: Uri = if (isOffline) {
                Uri.fromFile(localFile)
            } else {
                val directUrl = song.streamUrl.ifBlank {
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
            } else {
                player.play()
            }
        }
    }

    fun playNext(context: Context) {
        val current = _currentSong.value ?: return
        val list = _playlist.value
        if (list.isEmpty()) return

        if (_isShuffleEnabled.value && list.size > 1) {
            val randomNext = list.filter { it.id != current.id }.randomOrNull() ?: list.first()
            playSong(context, randomNext)
            return
        }

        val currentIndex = list.indexOfFirst { it.id == current.id }
        val nextIndex = (currentIndex + 1) % list.size
        playSong(context, list[nextIndex])
    }

    fun playPrevious(context: Context) {
        val current = _currentSong.value ?: return
        val list = _playlist.value
        if (list.isEmpty()) return

        // If played more than 3 seconds, seek back to beginning of current track
        if (_playbackPositionMs.value > 3000L) {
            seekTo(0L)
            return
        }

        val currentIndex = list.indexOfFirst { it.id == current.id }
        val prevIndex = if (currentIndex - 1 < 0) list.size - 1 else currentIndex - 1
        playSong(context, list[prevIndex])
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _playbackPositionMs.value = positionMs
    }

    fun toggleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
            RepeatMode.OFF -> RepeatMode.ALL
        }
    }

    fun toggleShuffle() {
        _isShuffleEnabled.value = !_isShuffleEnabled.value
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        exoPlayer?.playbackParameters = PlaybackParameters(speed)
    }

    fun setAudioPreset(preset: AudioPreset) {
        _audioPreset.value = preset
    }

    fun addToQueue(song: Song) {
        val currentList = _playlist.value.toMutableList()
        if (currentList.none { it.id == song.id }) {
            currentList.add(song)
            _playlist.value = currentList
        }
    }

    fun removeFromQueue(index: Int) {
        val currentList = _playlist.value.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            _playlist.value = currentList
        }
    }

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        _sleepTimerMinutesRemaining.value = minutes
        if (minutes != null && minutes > 0) {
            sleepTimerJob = scope.launch {
                var remaining = minutes
                while (remaining > 0) {
                    delay(60000L)
                    remaining--
                    _sleepTimerMinutesRemaining.value = remaining
                }
                // Time's up - pause playback smoothly
                exoPlayer?.pause()
                _sleepTimerMinutesRemaining.value = null
            }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            var loop = 0
            while (isActive) {
                exoPlayer?.let { player ->
                    if (player.isPlaying) {
                        val currentPos = player.currentPosition.coerceAtLeast(0L)
                        _playbackPositionMs.value = currentPos
                        if (player.duration > 0) {
                            _durationMs.value = player.duration
                        }
                        loop++
                        if (loop % 6 == 0) {
                            val active = _currentSong.value
                            if (active != null) {
                                OfflineDownloadManager.savePlaybackProgress(
                                    songId = active.id,
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
        progressJob?.cancel()
        progressJob = null
    }
}
