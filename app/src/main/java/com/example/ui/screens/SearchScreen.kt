package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Search
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
import com.example.data.MusicRepository
import com.example.model.Song
import com.example.network.AudioPlayerManager
import com.example.network.MusicSourcesManager
import com.example.network.SearchSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    onSongSelected: (Song, List<Song>) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf(SearchSource.ALL) }

    val repository = remember { MusicRepository() }
    val defaultSongs = remember { repository.getSampleSongs() }

    var searchResults by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    // Live search trigger whenever searchQuery or selectedSource changes
    LaunchedEffect(searchQuery, selectedSource) {
        if (searchQuery.isBlank()) {
            searchResults = defaultSongs
            isSearching = false
        } else {
            isSearching = true
            delay(300) // Debounce rapid typing
            val liveResults = MusicSourcesManager.searchAllSources(searchQuery, selectedSource)
            searchResults = if (liveResults.isNotEmpty()) liveResults else defaultSongs.filter {
                it.title.contains(searchQuery, ignoreCase = true) || it.artist.contains(searchQuery, ignoreCase = true)
            }
            isSearching = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Discover & Open Streaming",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Search Input Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search songs, artists, albums...") },
            leadingIcon = {
                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_text_field"),
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Open Source Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = selectedSource == SearchSource.ALL,
                    onClick = { selectedSource = SearchSource.ALL },
                    label = { Text("All Sources 🌐") }
                )
            }
            item {
                FilterChip(
                    selected = selectedSource == SearchSource.JIOSAAVN,
                    onClick = { selectedSource = SearchSource.JIOSAAVN },
                    label = { Text("JioSaavn 320kbps 🎧") }
                )
            }
            item {
                FilterChip(
                    selected = selectedSource == SearchSource.PIPED,
                    onClick = { selectedSource = SearchSource.PIPED },
                    label = { Text("Piped YouTube ▶️") }
                )
            }
            item {
                FilterChip(
                    selected = selectedSource == SearchSource.ITUNES,
                    onClick = { selectedSource = SearchSource.ITUNES },
                    label = { Text("iTunes & Deezer 🍎") }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (isSearching) "Searching open streams..." else if (searchQuery.isBlank()) "Featured & Trending (${searchResults.size})" else "Results (${searchResults.size} tracks)",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Results List
        if (searchResults.isEmpty() && !isSearching) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No songs found for \"$searchQuery\"",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(searchResults) { song ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSongSelected(song, searchResults) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = song.albumArtUrl,
                                contentDescription = song.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(song.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("${song.artist} • ${song.album}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                val sourceLabel = when {
                                    song.id.startsWith("deezer_") -> "Deezer Hi-Fi"
                                    song.id.startsWith("saavn_") -> "JioSaavn 320kbps"
                                    song.id.startsWith("piped_") -> "Piped YouTube"
                                    song.id.startsWith("itunes_") -> "iTunes Stream"
                                    else -> "Open Stream"
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text(
                                        text = sourceLabel,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Add to Queue button
                            IconButton(onClick = { AudioPlayerManager.addToQueue(song) }) {
                                Icon(
                                    imageVector = Icons.Default.PlaylistAdd,
                                    contentDescription = "Add to Queue",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}
