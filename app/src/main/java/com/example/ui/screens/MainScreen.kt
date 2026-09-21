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
import androidx.compose.ui.unit.dp
import com.example.model.Song
import com.example.ui.components.BottomPlayerBar

@Composable
fun MainScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    var currentSong by remember { mutableStateOf<Song?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var showPlayerFullScreen by remember { mutableStateOf(false) }
    var playbackPositionMs by remember { mutableStateOf(0L) }

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
                    break
                } else {
                    playbackPositionMs = newPosition
                }
            }
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
            onClose = { showPlayerFullScreen = false }
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    NavigationBar {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            label = { Text("Home") },
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                            label = { Text("Search") },
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Library") },
                            label = { Text("Library") },
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 }
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                when (selectedTab) {
                    0 -> HomeScreen(onSongSelected = { 
                        currentSong = it
                        isPlaying = true 
                    })
                    1 -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Search (Coming Soon)") }
                    2 -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Your Library (Coming Soon)") }
                }
            }
        }
    }
}
