package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.AppDatabaseHelper
import com.example.data.MusicRepository
import com.example.network.AudioPlayerManager
import com.example.ui.components.BottomPlayerBar
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MusicAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppDatabaseHelper.init(applicationContext)

        setContent {
            MusicAppTheme {
                MainAppContent()
            }
        }
    }
}

@Composable
fun MainAppContent() {
    val context = LocalContext.current
    val repository = remember { MusicRepository() }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showFullPlayerScreen by remember { mutableStateOf(false) }

    val currentSong by AudioPlayerManager.currentSong.collectAsState()
    val isPlaying by AudioPlayerManager.isPlaying.collectAsState()
    val playbackPositionMs by AudioPlayerManager.playbackPositionMs.collectAsState()
    val durationMs by AudioPlayerManager.durationMs.collectAsState()

    val likedSongs = MusicRepository.likedSongs
    val isCurrentSongLiked = remember(currentSong?.id, likedSongs.size) {
        val activeSong = currentSong
        if (activeSong != null) {
            likedSongs.any { liked -> liked.id == activeSong.id }
        } else false
    }

    BackHandler(enabled = showFullPlayerScreen) {
        showFullPlayerScreen = false
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Persistent Mini-Player Component
                BottomPlayerBar(
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    playbackPositionMs = playbackPositionMs,
                    durationMs = durationMs,
                    onPlayPauseToggle = {
                        AudioPlayerManager.togglePlayPause()
                    },
                    onSkipNext = {
                        AudioPlayerManager.playNext(context)
                    },
                    onOpenFullPlayer = {
                        showFullPlayerScreen = true
                    },
                    isLiked = isCurrentSongLiked,
                    onLikeToggle = {
                        currentSong?.let { repository.toggleLikeSong(it) }
                    }
                )

                // Navigation Bar
                NavigationBar(
                    modifier = Modifier.testTag("bottom_navigation_bar"),
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0 && !showFullPlayerScreen,
                        onClick = {
                            selectedTab = 0
                            showFullPlayerScreen = false
                        },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        modifier = Modifier.testTag("nav_home")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1 && !showFullPlayerScreen,
                        onClick = {
                            selectedTab = 1
                            showFullPlayerScreen = false
                        },
                        icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        label = { Text("Search") },
                        modifier = Modifier.testTag("nav_search")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2 && !showFullPlayerScreen,
                        onClick = {
                            selectedTab = 2
                            showFullPlayerScreen = false
                        },
                        icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Library") },
                        label = { Text("Library") },
                        modifier = Modifier.testTag("nav_library")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3 && !showFullPlayerScreen,
                        onClick = {
                            selectedTab = 3
                            showFullPlayerScreen = false
                        },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        modifier = Modifier.testTag("nav_settings")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> HomeScreen(
                    onSongSelected = { song, queue ->
                        AudioPlayerManager.playSong(context, song, queue)
                    }
                )
                1 -> SearchScreen(
                    onSongSelected = { song, queue ->
                        AudioPlayerManager.playSong(context, song, queue)
                    }
                )
                2 -> LibraryScreen(
                    onSongSelected = { song, queue ->
                        AudioPlayerManager.playSong(context, song, queue)
                    }
                )
                3 -> SettingsScreen()
            }

            if (showFullPlayerScreen && currentSong != null) {
                PlayerScreen(
                    song = currentSong!!,
                    isPlaying = isPlaying,
                    playbackPositionMs = playbackPositionMs,
                    onPlayPauseToggle = { AudioPlayerManager.togglePlayPause() },
                    onSkipNext = { AudioPlayerManager.playNext(context) },
                    onSkipPrevious = { AudioPlayerManager.playPrevious(context) },
                    onPositionChange = { newPosition -> AudioPlayerManager.seekTo(newPosition) },
                    onClose = { showFullPlayerScreen = false },
                    isLiked = isCurrentSongLiked,
                    onLikeToggle = { repository.toggleLikeSong(currentSong!!) }
                )
            }
        }
    }
}
