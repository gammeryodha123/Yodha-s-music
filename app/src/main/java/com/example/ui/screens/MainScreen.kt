package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.MusicRepository
import com.example.model.Song
import com.example.network.AudioPlayerManager
import com.example.ui.components.BottomPlayerBar

@Composable
fun MainScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    var showPlayerFullScreen by remember { mutableStateOf(false) }
    var searchInitialSource by remember { mutableStateOf(com.example.network.SearchSource.ALL) }

    val repository = remember { MusicRepository() }
    val localContext = androidx.compose.ui.platform.LocalContext.current

    // Bind state flows directly to the real Android MediaPlayer manager
    val currentSong by AudioPlayerManager.currentSong.collectAsState()
    val isPlaying by AudioPlayerManager.isPlaying.collectAsState()
    val playbackPositionMs by AudioPlayerManager.playbackPositionMs.collectAsState()

    // Synchronize liked state dynamically
    val isLiked = remember(currentSong, MusicRepository.likedSongs) {
        currentSong?.let { repository.isSongLiked(it.id) } ?: false
    }

    var songSelectionCount by remember { mutableStateOf(0) }

    val playSongWithAd: (Song, List<Song>) -> Unit = { selectedSong, selectedQueue ->
        val activity = localContext as? android.app.Activity
        songSelectionCount++
        if (songSelectionCount % 3 == 0 && activity != null) {
            com.example.ui.components.AdMobInterstitialHelper.showAd(activity) {
                AudioPlayerManager.setQueue(selectedQueue)
                AudioPlayerManager.playSong(localContext, selectedSong)
            }
        } else {
            AudioPlayerManager.setQueue(selectedQueue)
            AudioPlayerManager.playSong(localContext, selectedSong)
        }
    }

    val onNextTrack: () -> Unit = {
        AudioPlayerManager.playNext(localContext)
    }

    val onPreviousTrack: () -> Unit = {
        AudioPlayerManager.playPrevious(localContext)
    }

    val onLikeToggle: () -> Unit = {
        val songState = currentSong
        if (songState != null) {
            repository.toggleLikeSong(songState)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                Column {
                    if (currentSong != null) {
                        BottomPlayerBar(
                            currentSong = currentSong!!,
                            isPlaying = isPlaying,
                            playbackPositionMs = playbackPositionMs,
                            onPlayPause = { AudioPlayerManager.togglePlayPause() },
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
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Lock, contentDescription = "Privacy Hub") },
                            label = { Text("Privacy") },
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            modifier = Modifier.testTag("nav_privacy_tab")
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
                        onSongSelected = playSongWithAd,
                        initialSource = searchInitialSource
                    )
                    2 -> LibraryScreen(
                        onSongSelected = playSongWithAd,
                        repository = repository
                    )
                    3 -> OpenSourceScreen(
                        onNavigateToSearchWithSource = { sourceName ->
                            searchInitialSource = if (sourceName == "PeerTube") com.example.network.SearchSource.PEERTUBE else com.example.network.SearchSource.ALL
                            selectedTab = 1
                        }
                    )
                }
            }
        }

        // Beautiful Slide-Up Full-Screen Player Overlay
        AnimatedVisibility(
            visible = showPlayerFullScreen && currentSong != null,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            )
        ) {
            currentSong?.let { activeSong ->
                PlayerScreen(
                    song = activeSong,
                    isPlaying = isPlaying,
                    playbackPositionMs = playbackPositionMs,
                    onPositionChange = { newPosition ->
                        AudioPlayerManager.seekTo(newPosition)
                    },
                    onPlayPause = { AudioPlayerManager.togglePlayPause() },
                    onClose = { showPlayerFullScreen = false },
                    onNextTrack = onNextTrack,
                    onPreviousTrack = onPreviousTrack,
                    isLiked = isLiked,
                    onLikeToggle = onLikeToggle
                )
            }
        }
    }
}
