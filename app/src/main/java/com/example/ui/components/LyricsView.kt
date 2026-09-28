package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LyricLine
import com.example.data.LyricsProvider
import com.example.data.LyricsRepository
import com.example.data.LyricsResult
import com.example.network.MusixmatchHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsView(
    songId: String,
    songTitle: String,
    playbackPositionMs: Long,
    onSeek: (Long) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    artistName: String? = "",
    durationMs: Long = 180000L,
    songLyrics: String? = "",
    lyricsRepository: LyricsRepository = remember { LyricsRepository() }
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedProvider by remember { mutableStateOf(LyricsProvider.AUTO) }
    var lyricsResult by remember { mutableStateOf<LyricsResult?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    // Offset in milliseconds (+500ms / -500ms sync calibration)
    var syncOffsetMs by remember { mutableStateOf(0L) }

    // Custom search dialog
    var showSearchDialog by remember { mutableStateOf(false) }
    var customSearchQuery by remember { mutableStateOf("$songTitle ${artistName ?: ""}".trim()) }

    // Fetch lyrics on song or provider change
    fun loadLyrics(provider: LyricsProvider = selectedProvider, queryOverride: String? = null) {
        coroutineScope.launch {
            isLoading = true
            hasError = false
            try {
                val titleToUse = if (queryOverride.isNullOrBlank()) songTitle else queryOverride
                lyricsResult = lyricsRepository.fetchLyrics(
                    songId = songId,
                    songTitle = titleToUse,
                    artistName = if (queryOverride.isNullOrBlank()) artistName else "",
                    durationMs = durationMs,
                    songLyrics = songLyrics ?: "",
                    provider = provider
                )
            } catch (e: Exception) {
                hasError = true
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(songId, songTitle, artistName, selectedProvider) {
        loadLyrics(selectedProvider)
    }

    val lyricLines = lyricsResult?.lines ?: emptyList()
    val adjustedPosition = playbackPositionMs - syncOffsetMs

    // Determine currently active lyric line
    val activeIndex = remember(lyricLines, adjustedPosition) {
        val index = lyricLines.indexOfLast { it.timeMs <= adjustedPosition }
        if (index == -1 && lyricLines.isNotEmpty()) 0 else index
    }

    val listState = rememberLazyListState()

    // Smoothly scroll to center the active lyric line
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0 && lyricLines.isNotEmpty()) {
            val targetScrollIndex = (activeIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(targetScrollIndex)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(horizontal = 16.dp)
            .testTag("lyrics_view_container")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 8.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "REAL-TIME SYNCED LYRICS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = songTitle,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1
                    )
                    if (!artistName.isNullOrBlank()) {
                        Text(
                            text = artistName,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            maxLines = 1
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showSearchDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Lyrics",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            if (lyricLines.isNotEmpty()) {
                                val fullText = lyricLines.joinToString("\n") { it.text }
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Song Lyrics", fullText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Lyrics copied to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Lyrics",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("close_lyrics_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Lyrics",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            // Lyrics Provider Selection Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Auto / LRCLIB provider
                FilterChip(
                    selected = selectedProvider == LyricsProvider.AUTO || selectedProvider == LyricsProvider.LRCLIB,
                    onClick = {
                        selectedProvider = LyricsProvider.LRCLIB
                        loadLyrics(LyricsProvider.LRCLIB)
                    },
                    label = { Text("LRCLIB", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )

                // Musixmatch Provider / Launcher
                AssistChip(
                    onClick = {
                        MusixmatchHelper.launchMusixmatch(context, songTitle, artistName)
                    },
                    label = { Text("Musixmatch", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open in Musixmatch",
                            modifier = Modifier.size(13.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )

                // NetEase Provider
                FilterChip(
                    selected = selectedProvider == LyricsProvider.NETEASE,
                    onClick = {
                        selectedProvider = LyricsProvider.NETEASE
                        loadLyrics(LyricsProvider.NETEASE)
                    },
                    label = { Text("NetEase", fontSize = 11.sp) }
                )

                // Reload
                IconButton(
                    onClick = { loadLyrics(selectedProvider) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reload Lyrics",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Offset Calibration Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Sync Calibration",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Sync: ${if (syncOffsetMs >= 0) "+${syncOffsetMs}ms" else "${syncOffsetMs}ms"}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { syncOffsetMs -= 500L },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("-0.5s", fontSize = 11.sp)
                    }
                    if (syncOffsetMs != 0L) {
                        TextButton(
                            onClick = { syncOffsetMs = 0L },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Reset", fontSize = 11.sp)
                        }
                    }
                    TextButton(
                        onClick = { syncOffsetMs += 500L },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("+0.5s", fontSize = 11.sp)
                    }
                }
            }

            // Lyrics List or State Containers
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Fetching synced lyrics from LRCLIB...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                } else if (hasError || lyricLines.isEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "No synchronized lyrics found for this track.",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { MusixmatchHelper.launchMusixmatch(context, songTitle, artistName) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Search on Musixmatch", fontSize = 13.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(lyricLines) { index, line ->
                            val isActive = index == activeIndex

                            val targetScale = if (isActive) 1.02f else 1.0f
                            val animatedScale by animateFloatAsState(
                                targetValue = targetScale,
                                animationSpec = spring(dampingRatio = 0.7f),
                                label = "scale"
                            )

                            val backgroundColor by animateColorAsState(
                                targetValue = if (isActive) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                                } else {
                                    Color.Transparent
                                },
                                label = "bgColor"
                            )

                            val textColor by animateColorAsState(
                                targetValue = if (isActive) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
                                },
                                label = "textColor"
                            )

                            val fontSize = if (isActive) 22.sp else 17.sp
                            val fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .scale(animatedScale)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(backgroundColor)
                                    .then(
                                        if (isActive) {
                                            Modifier.border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                        } else Modifier
                                    )
                                    .clickable { onSeek(line.timeMs + syncOffsetMs) }
                                    .padding(vertical = 10.dp, horizontal = 14.dp)
                                    .testTag("lyric_line_$index")
                            ) {
                                Text(
                                    text = line.text,
                                    fontSize = fontSize,
                                    fontWeight = fontWeight,
                                    color = textColor,
                                    lineHeight = 28.sp,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            // Sync/Tap Instruction Row & Musixmatch quick launcher footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Synced",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tap line to jump playback • Powered by LRCLIB",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                TextButton(
                    onClick = { MusixmatchHelper.launchMusixmatch(context, songTitle, artistName) },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("Musixmatch", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(11.dp))
                }
            }
        }
    }

    // Custom Search Dialog
    if (showSearchDialog) {
        AlertDialog(
            onDismissRequest = { showSearchDialog = false },
            title = { Text("Search Custom Lyrics", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Search LRCLIB for alternate lyrics versions or correct track title.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customSearchQuery,
                        onValueChange = { customSearchQuery = it },
                        label = { Text("Track Title / Artist Query") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSearchDialog = false
                        loadLyrics(selectedProvider, customSearchQuery)
                    }
                ) {
                    Text("Search")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSearchDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
