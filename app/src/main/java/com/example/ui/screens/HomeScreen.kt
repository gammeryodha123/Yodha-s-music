package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.MusicRepository
import com.example.model.Playlist
import com.example.model.Song

@Composable
fun HomeScreen(
    onSongSelected: (Song, List<Song>) -> Unit,
    onPlaylistSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val repository = remember { MusicRepository() }
    var recentSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var playlists by remember { mutableStateOf<List<Playlist>>(emptyList()) }

    LaunchedEffect(Unit) {
        recentSongs = repository.getRecentSongs()
        playlists = repository.getFeaturedPlaylists()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text(
                text = "Welcome Back",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        item {
            Text(
                text = "Recently Played",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            if (recentSongs.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(recentSongs) { song ->
                        RecentSongItem(song = song, onClick = { onSongSelected(song, recentSongs) })
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Text(
                text = "Featured Playlists",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
        
        if (playlists.isEmpty()) {
            item {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        } else {
            items(playlists) { playlist ->
                PlaylistItem(playlist = playlist, onClick = { onPlaylistSelected(playlist.id) })
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(80.dp)) // space for bottom player
        }
    }
}

@Composable
fun RecentSongItem(song: Song, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable { onClick() }
            .testTag("recent_song_${song.id}")
    ) {
        AsyncImage(
            model = song.albumArtUrl,
            contentDescription = song.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = song.title,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = song.artist,
            fontSize = 12.sp,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun PlaylistItem(playlist: Playlist, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp)
            .testTag("featured_playlist_${playlist.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = playlist.coverUrl,
            contentDescription = playlist.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = playlist.description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1
            )
        }
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Play",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )
    }
}
