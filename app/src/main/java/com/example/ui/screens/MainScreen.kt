package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.MusicRepository
import com.example.model.Song
import com.example.ui.components.BottomPlayerBar

@Composable
fun MainScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    var currentSong by remember { mutableStateOf<Song?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var showPlayerFullScreen by remember { mutableStateOf(false) }
    var playbackPositionMs by remember { mutableStateOf(0L) }
    
    // Playback playlist queue tracking
    var currentQueue by remember { mutableStateOf<List<Song>>(emptyList()) }

    val repository = remember { MusicRepository() }

    // Synchronize liked state dynamically
    val isLiked = remember(currentSong, MusicRepository.likedSongs) {
        currentSong?.let { repository.isSongLiked(it.id) } ?: false
    }

    val localContext = androidx.compose.ui.platform.LocalContext.current
    var songSelectionCount by remember { mutableStateOf(0) }

    val playSongWithAd: (Song, List<Song>) -> Unit = { selectedSong, selectedQueue ->
        val activity = localContext as? android.app.Activity
        songSelectionCount++
        if (songSelectionCount % 3 == 0 && activity != null) {
            com.example.ui.components.AdMobInterstitialHelper.showAd(activity) {
                currentSong = selectedSong
                currentQueue = selectedQueue
                isPlaying = true
            }
        } else {
            currentSong = selectedSong
            currentQueue = selectedQueue
            isPlaying = true
        }
    }

    // Reset playback position when song changes
    LaunchedEffect(currentSong) {
        playbackPositionMs = 0L
    }

    // Progression timer loop
    LaunchedEffect(isPlaying, currentSong) {
        val song = currentSong
        if (isPlaying && song != null) {
            while (true) {
                kotlinx.coroutines.delay(1000L)
                val newPosition = playbackPositionMs + 1000L
                if (newPosition >= song.durationMs) {
                    playbackPositionMs = song.durationMs
                    isPlaying = false
                    // Auto-play next song in queue if available
                    if (currentQueue.isNotEmpty()) {
                        val index = currentQueue.indexOfFirst { it.id == song.id }
                        if (index != -1 && index + 1 < currentQueue.size) {
                            currentSong = currentQueue[index + 1]
                            isPlaying = true
                        }
                    }
                    break
                } else {
                    playbackPositionMs = newPosition
                }
            }
        }
    }

    // Previous and Next song in queue execution
    val onNextTrack: () -> Unit = {
        val queue = currentQueue
        val song = currentSong
        if (queue.isNotEmpty() && song != null) {
            val index = queue.indexOfFirst { it.id == song.id }
            if (index != -1) {
                val nextIndex = (index + 1) % queue.size
                currentSong = queue[nextIndex]
                isPlaying = true
            }
        }
    }

    val onPreviousTrack: () -> Unit = {
        val queue = currentQueue
        val song = currentSong
        if (queue.isNotEmpty() && song != null) {
            val index = queue.indexOfFirst { it.id == song.id }
            if (index != -1) {
                val prevIndex = if (index - 1 < 0) queue.size - 1 else index - 1
                currentSong = queue[prevIndex]
                isPlaying = true
            }
        }
    }

    val onLikeToggle: () -> Unit = {
        val songState = currentSong
        if (songState != null) {
            repository.toggleLikeSong(songState)
        }
    }

    val song = currentSong
    if (showPlayerFullScreen && song != null) {
        PlayerScreen(
            song = song,
            isPlaying = isPlaying,
            playbackPositionMs = playbackPositionMs,
            onPositionChange = { newPosition ->
                playbackPositionMs = newPosition.coerceIn(0L, song.durationMs)
            },
            onPlayPause = { isPlaying = !isPlaying },
            onClose = { showPlayerFullScreen = false },
            onNextTrack = onNextTrack,
            onPreviousTrack = onPreviousTrack,
            isLiked = isLiked,
            onLikeToggle = onLikeToggle
        )
    } else {
        Scaffold(
            bottomBar = {
                Column {
                    if (currentSong != null) {
                        BottomPlayerBar(
                            currentSong = currentSong!!,
                            isPlaying = isPlaying,
                            playbackPositionMs = playbackPositionMs,
                            onPlayPause = { isPlaying = !isPlaying },
                            onClick = { showPlayerFullScreen = true },
                            isLiked = isLiked,
                            onLikeToggle = onLikeToggle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("bottom_player_bar")
                        )
                    }
                    
                    // Display AdMob Banner Ad above bottom navigation
                    com.example.ui.components.AdMobBannerAd(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )

                    NavigationBar {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            label = { Text("Home") },
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            modifier = Modifier.testTag("nav_home_tab")
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                            label = { Text("Search") },
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            modifier = Modifier.testTag("nav_search_tab")
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Library") },
                            label = { Text("Library") },
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            modifier = Modifier.testTag("nav_library_tab")
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                when (selectedTab) {
                    0 -> HomeScreen(
                        onSongSelected = playSongWithAd,
                        onPlaylistSelected = { playlistId ->
                            // Dynamically swap tab to library and open playlist detail
                            selectedTab = 2
                        }
                    )
                    1 -> SearchScreen(
                        onSongSelected = playSongWithAd
                    )
                    2 -> LibraryScreen(
                        onSongSelected = playSongWithAd,
                        repository = repository
                    )
                }
            }
        }
    }
}
