package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.MusicRepository
import com.example.model.Song
import com.example.network.MusicSourcesManager
import com.example.network.SearchSource
import kotlinx.coroutines.launch

data class GenreCard(val name: String, val colors: List<Color>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onSongSelected: (Song, List<Song>) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf(SearchSource.ALL) }
    var searchResults by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    
    // Read search history and recently played from Room reactively
    val recentQueriesState = remember { MusicRepository.getRecentSearchQueries() }
    val recentQueries by recentQueriesState.collectAsState(initial = emptyList())

    val recentlyPlayedState = remember { MusicRepository.getRecentlyPlayedSongs() }
    val recentlyPlayed by recentlyPlayedState.collectAsState(initial = emptyList())

    val genres = remember {
        listOf(
            GenreCard("Synthwave", listOf(Color(0xFFE01A4F), Color(0xFFF15946))),
            GenreCard("Lofi Beats", listOf(Color(0xFF3F51B5), Color(0xFF00BCD4))),
            GenreCard("Bollywood", listOf(Color(0xFFFF9800), Color(0xFFE91E63))),
            GenreCard("Chill Study", listOf(Color(0xFF4CAF50), Color(0xFF8BC34A))),
            GenreCard("Workout", listOf(Color(0xFF9C27B0), Color(0xFFFF5722))),
            GenreCard("Pop Hits", listOf(Color(0xFF009688), Color(0xFF4CAF50))),
            GenreCard("Retro Spark", listOf(Color(0xFF3F51B5), Color(0xFF9C27B0))),
            GenreCard("Cyber Beats", listOf(Color(0xFF607D8B), Color(0xFF00BCD4)))
        )
    }

    LaunchedEffect(query, selectedSource) {
        if (query.isBlank()) {
            searchResults = emptyList()
        } else {
            isLoading = true
            try {
                searchResults = MusicSourcesManager.searchAllSources(query, selectedSource)
                // Auto-save search queries to Room if matches were found
                if (searchResults.isNotEmpty()) {
                    MusicRepository.saveSearchQuery(query)
                }
            } catch (e: Exception) {
                searchResults = emptyList()
            } finally {
                isLoading = false
            }
        }
    }

    // Handles playing a song and saving it to local Room "recents" database
    val handleSongClick: (Song, List<Song>) -> Unit = { selectedSong, list ->
        coroutineScope.launch {
            MusicRepository.saveRecentlyPlayedSong(selectedSong)
        }
        onSongSelected(selectedSong, list)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Search Services",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Modern Search Input
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search songs, artists or nodes...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .testTag("search_text_input"),
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent
            )
        )

        // Integrated Server & Service Selection Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val sources = listOf(
                SearchSource.ALL to "All Sources",
                SearchSource.YOUTUBE to "YouTube (YT)",
                SearchSource.SOUNDCLOUD to "SoundCloud",
                SearchSource.PIPED to "Piped Servers"
            )
            sources.forEach { (src, label) ->
                val selected = selectedSource == src
                FilterChip(
                    selected = selected,
                    onClick = { selectedSource = src },
                    label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        if (query.isEmpty()) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // 1. Room-Cached Recent Searches Section
                if (recentQueries.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recent searches",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            TextButton(
                                onClick = {
                                    coroutineScope.launch { MusicRepository.clearSearchHistory() }
                                }
                            ) {
                                Text("Clear all", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            recentQueries.forEach { recent ->
                                SuggestionChip(
                                    onClick = { query = recent.queryText },
                                    label = { Text(recent.queryText, fontSize = 12.sp) },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                )
                            }
                        }
                    }
                }

                // 2. Room-Cached Recently Played Section
                if (recentlyPlayed.isNotEmpty()) {
                    item {
                        Text(
                            text = "Recently played",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            recentlyPlayed.forEach { recentSong ->
                                val songObj = Song(
                                    id = recentSong.id,
                                    title = recentSong.title,
                                    artist = recentSong.artist,
                                    albumArtUrl = recentSong.albumArtUrl,
                                    streamUrl = recentSong.streamUrl,
                                    durationMs = recentSong.durationMs,
                                    lyrics = recentSong.lyrics
                                )
                                Column(
                                    modifier = Modifier
                                        .width(110.dp)
                                        .clickable {
                                            handleSongClick(songObj, recentlyPlayed.map {
                                                Song(it.id, it.title, it.artist, it.albumArtUrl, it.streamUrl, it.durationMs, it.lyrics)
                                            })
                                        }
                                ) {
                                    AsyncImage(
                                        model = recentSong.albumArtUrl,
                                        contentDescription = recentSong.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(110.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = recentSong.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(
                                        text = recentSong.artist,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        maxLines = 1,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Static Music Genres
                item {
                    Text(
                        text = "Browse genres",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                // Render grid using pairs of genres
                val rows = genres.chunked(2)
                items(rows) { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        pair.forEach { genre ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(100.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Brush.linearGradient(genre.colors))
                                    .clickable { query = genre.name }
                                    .padding(12.dp)
                                    .testTag("genre_card_${genre.name}")
                            ) {
                                Text(
                                    text = genre.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.align(Alignment.TopStart)
                                )
                            }
                        }
                        if (pair.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(100.dp)) // Extra scroll spacing
                }
            }
        } else {
            // Display Results or Loading State
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (searchResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tracks found on ${selectedSource.name.lowercase()} for \"$query\"",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(searchResults) { song ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { handleSongClick(song, searchResults) }
                                .padding(8.dp)
                                .testTag("search_result_${song.id}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = song.albumArtUrl,
                                contentDescription = song.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val sourceTag = when {
                                        song.id.startsWith("piped_") -> "Piped"
                                        song.id.startsWith("soundcloud_") -> "SoundCloud"
                                        song.id.startsWith("yt_") -> "YouTube"
                                        else -> "Local"
                                    }
                                    val tagColor = when (sourceTag) {
                                        "Piped" -> MaterialTheme.colorScheme.tertiary
                                        "SoundCloud" -> Color(0xFFFF5500)
                                        "YouTube" -> Color(0xFFFF0000)
                                        else -> MaterialTheme.colorScheme.secondary
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(tagColor.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = sourceTag,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = tagColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = song.artist,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        maxLines = 1
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp)) // Padding for bottom player
                    }
                }
            }
        }
    }
}
