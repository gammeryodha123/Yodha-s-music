package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.LyricLine
import com.example.data.LyricsRepository
import com.example.data.MusicRepository
import com.example.model.Song
import com.example.ui.components.LyricsView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    song: Song,
    isPlaying: Boolean,
    playbackPositionMs: Long,
    onPositionChange: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onClose: () -> Unit,
    onNextTrack: () -> Unit,
    onPreviousTrack: () -> Unit,
    isLiked: Boolean,
    onLikeToggle: () -> Unit
) {
    val sliderValue = if (song.durationMs > 0L) {
        (playbackPositionMs.toFloat() / song.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    var showLyricsFullScreen by remember { mutableStateOf(false) }
    var showPlaylistMenu by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }

    val lyricsRepository = remember { LyricsRepository() }
    var lyricLines by remember(song.id) { mutableStateOf<List<LyricLine>>(emptyList()) }
    var isLyricsLoading by remember(song.id) { mutableStateOf(true) }

    LaunchedEffect(song.id) {
        isLyricsLoading = true
        try {
            lyricLines = lyricsRepository.fetchLyrics(song.id)
        } catch (e: Exception) {
            lyricLines = emptyList()
        } finally {
            isLyricsLoading = false
        }
    }

    val activeIndex = remember(lyricLines, playbackPositionMs) {
        val index = lyricLines.indexOfLast { it.timeMs <= playbackPositionMs }
        if (index == -1 && lyricLines.isNotEmpty()) 0 else index
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Close", modifier = Modifier.size(32.dp))
                }
                Text(
                    text = "NOW PLAYING",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Box {
                    IconButton(onClick = { showPlaylistMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = showPlaylistMenu,
                        onDismissRequest = { showPlaylistMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Add to Playlist") },
                            onClick = {
                                showPlaylistMenu = false
                                showAddToPlaylistDialog = true
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            AsyncImage(
                model = song.albumArtUrl,
                contentDescription = "Album Art",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = song.artist,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }

                IconButton(
                    onClick = onLikeToggle,
                    modifier = Modifier.testTag("like_button")
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like Song",
                        tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Slider(
                value = sliderValue,
                onValueChange = { newValue ->
                    onPositionChange((newValue * song.durationMs).toLong())
                },
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatTime(playbackPositionMs), fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                Text(formatTime(song.durationMs), fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreviousTrack) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(48.dp))
                }
                
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(40.dp)
                    )
                }
                
                IconButton(onClick = onNextTrack) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", modifier = Modifier.size(48.dp))
                }
            }

            // Lyrics Preview Card
            if (lyricLines.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLyricsFullScreen = true }
                        .testTag("lyrics_preview_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Lyrics",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "TAP TO EXPAND",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val line1 = lyricLines.getOrNull(activeIndex)
                        val line2 = lyricLines.getOrNull(activeIndex + 1)
                        
                        if (line1 != null) {
                            Text(
                                text = line1.text,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1
                            )
                        }
                        if (line2 != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = line2.text,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }

        // Full Screen Overlay
        if (showLyricsFullScreen) {
            LyricsView(
                songId = song.id,
                songTitle = song.title,
                playbackPositionMs = playbackPositionMs,
                onSeek = onPositionChange,
                onClose = { showLyricsFullScreen = false },
                lyricsRepository = lyricsRepository,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    if (showAddToPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showAddToPlaylistDialog = false },
            title = { Text("Add to Playlist") },
            text = {
                val playlistsToAdd = MusicRepository.customPlaylists.filter { it.id != "liked" }
                if (playlistsToAdd.isEmpty()) {
                    Text("No custom playlists created yet. Create one in the Library tab!")
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(playlistsToAdd) { playlist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        MusicRepository().addSongToPlaylist(playlist.id, song)
                                        showAddToPlaylistDialog = false
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(playlist.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddToPlaylistDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000L
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
