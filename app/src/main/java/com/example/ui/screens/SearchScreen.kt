package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

data class GenreCard(val name: String, val colors: List<Color>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onSongSelected: (Song, List<Song>) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    
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

    val filteredSongs = remember(query) {
        if (query.isBlank()) {
            emptyList()
        } else {
            MusicRepository.masterSongList.filter { song ->
                song.title.contains(query, ignoreCase = true) ||
                song.artist.contains(query, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Search",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Modern Search Input
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("What do you want to listen to?", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) },
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
                .padding(bottom = 16.dp)
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

        if (query.isEmpty()) {
            // Browse Categories
            Text(
                text = "Browse all",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
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
                    Spacer(modifier = Modifier.height(80.dp)) // Extra scroll spacing
                }
            }
        } else {
            // Display Filtered Songs
            if (filteredSongs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No results found for \"$query\"",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredSongs) { song ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSongSelected(song, filteredSongs) }
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
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = song.artist,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
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
