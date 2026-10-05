package com.example.network

import android.content.Context
import com.example.model.Song
import com.example.util.AppLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ListenTogetherManager {
    private const val TAG = "ListenTogetherManager"
    private const val DEFAULT_SERVER_URL = "wss://metroserver.metrolist.org/ws" // configurable metroserver endpoint

    private val client = OkHttpClient.Builder()
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var heartbeatJob: Job? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _currentRoomId = MutableStateFlow<String?>(null)
    val currentRoomId: StateFlow<String?> = _currentRoomId.asStateFlow()

    private val _activeListenersCount = MutableStateFlow(1)
    val activeListenersCount: StateFlow<Int> = _activeListenersCount.asStateFlow()

    private val _isHost = MutableStateFlow(false)
    val isHost: StateFlow<Boolean> = _isHost.asStateFlow()

    private var lastSentState: String = ""

    fun createRoom(username: String, serverUrl: String = DEFAULT_SERVER_URL) {
        val randomRoomId = (100000..999999).random().toString()
        _isHost.value = true
        connectToWebSocket(randomRoomId, username, serverUrl)
    }

    fun joinRoom(roomId: String, username: String, serverUrl: String = DEFAULT_SERVER_URL) {
        _isHost.value = false
        connectToWebSocket(roomId, username, serverUrl)
    }

    fun leaveRoom() {
        disconnect()
    }

    private fun connectToWebSocket(roomId: String, username: String, serverUrl: String) {
        disconnect()

        val request = Request.Builder()
            .url(serverUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                AppLogger.i(TAG, "WebSocket connected successfully to $serverUrl")
                _isConnected.value = true
                _currentRoomId.value = roomId

                // Send Join/Create command
                val joinMessage = JSONObject().apply {
                    put("action", "join")
                    put("roomId", roomId)
                    put("username", username)
                    put("isHost", _isHost.value)
                }
                webSocket.send(joinMessage.toString())

                startHeartbeat()
                startPlaybackMonitoring()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    val action = json.optString("action")
                    AppLogger.i(TAG, "Received message: $text")

                    when (action) {
                        "sync" -> {
                            // If we are a member (not host), apply sync commands from the host
                            if (!_isHost.value) {
                                val songId = json.optString("songId")
                                val isPlaying = json.optBoolean("isPlaying")
                                val positionMs = json.optLong("positionMs")
                                val title = json.optString("title")
                                val artist = json.optString("artist")
                                val artwork = json.optString("artwork")
                                val streamUrl = json.optString("streamUrl")

                                scope.launch(Dispatchers.Main) {
                                    val current = AudioPlayerManager.currentSong.value
                                    if (current?.id != songId && songId.isNotEmpty()) {
                                        val songToPlay = Song(
                                            id = songId,
                                            title = title,
                                            artist = artist,
                                            albumArtUrl = artwork,
                                            streamUrl = streamUrl,
                                            durationMs = json.optLong("durationMs", 180000L),
                                            genre = "Shared Session",
                                            album = "Shared Playlist"
                                        )
                                        AudioPlayerManager.playSong(com.example.data.AppDatabaseHelper.context ?: return@launch, songToPlay)
                                    }

                                    val player = AudioPlayerManager.getOrCreatePlayer(com.example.data.AppDatabaseHelper.context ?: return@launch)
                                    if (Math.abs(player.currentPosition - positionMs) > 3000L) {
                                        AudioPlayerManager.seekTo(positionMs)
                                    }

                                    if (isPlaying && !player.isPlaying) {
                                        player.play()
                                    } else if (!isPlaying && player.isPlaying) {
                                        player.pause()
                                    }
                                }
                            }
                        }
                        "roomUpdate" -> {
                            val count = json.optInt("listeners", 1)
                            _activeListenersCount.value = count
                        }
                        "ping" -> {
                            webSocket.send(JSONObject().apply { put("action", "pong") }.toString())
                        }
                    }
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Error parsing WebSocket packet: ${e.message}")
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                AppLogger.i(TAG, "WebSocket closing: $reason")
                resetStates()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                AppLogger.e(TAG, "WebSocket connection failed: ${t.message}")
                resetStates()
            }
        })
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(15000L) // Ping every 15s
                webSocket?.let { ws ->
                    try {
                        ws.send(JSONObject().apply { put("action", "ping") }.toString())
                    } catch (e: Exception) {
                        AppLogger.w(TAG, "Failed sending ping heartbeat: ${e.message}")
                    }
                }
            }
        }
    }

    private var monitorJob: Job? = null
    private fun startPlaybackMonitoring() {
        monitorJob?.cancel()
        monitorJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                delay(2000L) // Broadcast playback sync state to room every 2 seconds
                if (_isConnected.value && _isHost.value) {
                    val song = AudioPlayerManager.currentSong.value
                    if (song != null) {
                        val player = AudioPlayerManager.getOrCreatePlayer(com.example.data.AppDatabaseHelper.context ?: continue)
                        val syncState = JSONObject().apply {
                            put("action", "sync")
                            put("roomId", _currentRoomId.value)
                            put("songId", song.id)
                            put("title", song.title)
                            put("artist", song.artist)
                            put("artwork", song.albumArtUrl)
                            put("streamUrl", song.streamUrl)
                            put("durationMs", song.durationMs)
                            put("isPlaying", player.isPlaying)
                            put("positionMs", player.currentPosition)
                        }.toString()

                        if (syncState != lastSentState) {
                            lastSentState = syncState
                            withContext(Dispatchers.IO) {
                                webSocket?.send(syncState)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun resetStates() {
        _isConnected.value = false
        _currentRoomId.value = null
        _activeListenersCount.value = 1
        _isHost.value = false
        heartbeatJob?.cancel()
        monitorJob?.cancel()
    }

    private fun disconnect() {
        webSocket?.close(1000, "Leaving room")
        webSocket = null
        resetStates()
    }
}
