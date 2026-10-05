package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.LyricsRepository
import com.example.model.Song
import com.example.network.AudioPlayerManager
import com.example.network.AudioPreset
import com.example.network.RepeatMode
import com.example.ui.components.LyricsView

@Composable
fun PlayerScreen(
    song: Song,
    isPlaying: Boolean,
    playbackPositionMs: Long,
    onPlayPauseToggle: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onPositionChange: (Long) -> Unit,
    onClose: () -> Unit,
    isLiked: Boolean = false,
    onLikeToggle: () -> Unit,
    isBuffering: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val durationMs = if (song.durationMs > 0) song.durationMs else 180000L
    var activePlayerTab by remember { mutableIntStateOf(0) } // 0: Song, 1: Lyrics, 2: Queue

    val lyricsRepository = remember { LyricsRepository() }
    var syncedLyricsResult by remember(song.id) {
        mutableStateOf(lyricsRepository.getSyncedLyricsForSong(song))
    }
    LaunchedEffect(song.id) {
        syncedLyricsResult = lyricsRepository.fetchLyricsOnline(song)
    }
    val syncedLyrics = syncedLyricsResult.lines

    val repeatMode by AudioPlayerManager.repeatMode.collectAsState()
    val isShuffle by AudioPlayerManager.isShuffleEnabled.collectAsState()
    val playbackSpeed by AudioPlayerManager.playbackSpeed.collectAsState()
    val currentPreset by AudioPlayerManager.audioPreset.collectAsState()
    val sleepTimerMinutes by AudioPlayerManager.sleepTimerMinutesRemaining.collectAsState()
    val currentQueue by AudioPlayerManager.playlist.collectAsState()

    var showPresetDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showFullScreenLyricsModal by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("full_player_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse Player",
                        modifier = Modifier.size(32.dp)
                    )
                }

                // 3-Way Segmented View Switcher (Song / Lyrics / Queue)
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = activePlayerTab == 0,
                        onClick = { activePlayerTab = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                    ) {
                        Text("Song", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    SegmentedButton(
                        selected = showFullScreenLyricsModal,
                        onClick = { showFullScreenLyricsModal = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                    ) {
                        Text("Lyrics", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    SegmentedButton(
                        selected = activePlayerTab == 2,
                        onClick = { activePlayerTab = 2 },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                    ) {
                        Text("Queue", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                IconButton(onClick = onLikeToggle) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like Song",
                        tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Main View Area: Artwork vs Synced Lyrics vs Queue
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                when (activePlayerTab) {
                    0 -> {
                        // Artwork Hero Mode
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                modifier = Modifier.size(260.dp)
                            ) {
                                AsyncImage(
                                    model = song.albumArtUrl,
                                    contentDescription = song.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = song.title,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = song.artist,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Tags & Quick Preset Chips
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = song.genre,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                    modifier = Modifier.clickable { showPresetDialog = true }
                                ) {
                                    Text(
                                        text = "EQ: ${currentPreset.displayName.take(12)}...",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        // Full Screen Synced Lyrics Mode with Close 'X' Mark
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = song.title,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Source: ${syncedLyricsResult.provider.displayName}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                // Prominent Close 'X' Button to Exit Lyrics View
                                IconButton(
                                    onClick = { activePlayerTab = 0 },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = CircleShape
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close Lyrics View",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            LyricsView(
                                lyrics = syncedLyrics,
                                playbackPositionMs = playbackPositionMs,
                                onSeekTo = onPositionChange,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    2 -> {
                        // Queue & Up Next Mode
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Up Next (${currentQueue.size} tracks)",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                IconButton(
                                    onClick = { activePlayerTab = 0 },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = CircleShape
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close Queue View",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                itemsIndexed(currentQueue) { index, queueSong ->
                                    val isCurrent = queueSong.id == song.id
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isCurrent) {
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                            }
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                AudioPlayerManager.playSong(context, queueSong, currentQueue)
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AsyncImage(
                                                model = queueSong.albumArtUrl,
                                                contentDescription = queueSong.title,
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = queueSong.title,
                                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 14.sp,
                                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = queueSong.artist,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (isCurrent) {
                                                Icon(
                                                    imageVector = Icons.Default.Equalizer,
                                                    contentDescription = "Playing",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            } else {
                                                IconButton(onClick = { AudioPlayerManager.removeFromQueue(index) }) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Remove",
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick FX & Tools Row (Speed, EQ, Sleep Timer)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Playback Speed
                AssistChip(
                    onClick = { showSpeedDialog = true },
                    label = { Text("${playbackSpeed}x") },
                    leadingIcon = {
                        Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )

                // Sound Preset / Equalizer
                AssistChip(
                    onClick = { showPresetDialog = true },
                    label = { Text("EQ") },
                    leadingIcon = {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )

                // Sleep Timer
                AssistChip(
                    onClick = { showSleepTimerDialog = true },
                    label = {
                        Text(if (sleepTimerMinutes != null) "${sleepTimerMinutes}m left" else "Sleep")
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Bedtime, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    colors = if (sleepTimerMinutes != null) {
                        AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    } else {
                        AssistChipDefaults.assistChipColors()
                    }
                )
            }

            // Bottom Player Slider & Playback Controls
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = playbackPositionMs.toFloat().coerceIn(0f, durationMs.toFloat()),
                    onValueChange = { onPositionChange(it.toLong()) },
                    valueRange = 0f..durationMs.toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(playbackPositionMs), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatTime(durationMs), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Playback Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle Toggle
                    IconButton(onClick = { AudioPlayerManager.toggleShuffle() }) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (isShuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Previous Track
                    IconButton(onClick = onSkipPrevious, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(36.dp))
                    }

                    // Play/Pause Main FAB Button
                    IconButton(
                        onClick = onPlayPauseToggle,
                        modifier = Modifier
                            .size(68.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        if (isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }

                    // Next Track
                    IconButton(onClick = onSkipNext, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next", modifier = Modifier.size(36.dp))
                    }

                    // Repeat Mode Toggle (Off -> All -> One)
                    IconButton(onClick = { AudioPlayerManager.toggleRepeatMode() }) {
                        Icon(
                            imageVector = when (repeatMode) {
                                RepeatMode.ONE -> Icons.Default.RepeatOne
                                RepeatMode.ALL -> Icons.Default.Repeat
                                RepeatMode.OFF -> Icons.Default.Repeat
                            },
                            contentDescription = "Repeat Mode: ${repeatMode.displayName}",
                            tint = if (repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Full Screen Lyrics Modal Overlay
        AnimatedVisibility(
            visible = showFullScreenLyricsModal,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            FullScreenLyricsModal(
                song = song,
                playbackPositionMs = playbackPositionMs,
                syncedLyrics = syncedLyrics,
                syncedLyricsResult = syncedLyricsResult,
                onPositionChange = onPositionChange,
                onClose = { showFullScreenLyricsModal = false }
            )
        }
    }

    // Sound FX / Preset Dialog
    if (showPresetDialog) {
        AlertDialog(
            onDismissRequest = { showPresetDialog = false },
            title = { Text("Sound Equalizer Preset") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AudioPreset.entries.forEach { preset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    AudioPlayerManager.setAudioPreset(preset)
                                    showPresetDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentPreset == preset,
                                onClick = {
                                    AudioPlayerManager.setAudioPreset(preset)
                                    showPresetDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(preset.displayName, fontWeight = if (currentPreset == preset) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPresetDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Sleep Timer Dialog
    if (showSleepTimerDialog) {
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = { Text("Set Sleep Timer") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(15, 30, 45, 60).forEach { mins ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    AudioPlayerManager.setSleepTimer(mins)
                                    showSleepTimerDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("$mins Minutes", fontSize = 15.sp)
                        }
                    }

                    if (sleepTimerMinutes != null) {
                        TextButton(
                            onClick = {
                                AudioPlayerManager.setSleepTimer(null)
                                showSleepTimerDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Turn Off Sleep Timer", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepTimerDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    val playbackPitch by AudioPlayerManager.playbackPitch.collectAsState()
    val isSkipSilence by AudioPlayerManager.isSkipSilenceEnabled.collectAsState()

    // InnerTune Advanced Audio Tuner Dialog
    if (showSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("InnerTune Settings")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Speed Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Tempo / Speed", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(String.format("%.2fx", playbackSpeed), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = playbackSpeed,
                            onValueChange = { AudioPlayerManager.setPlaybackSpeed(it) },
                            valueRange = 0.5f..2.0f,
                            steps = 5
                        )
                    }

                    // Pitch Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Pitch Tune", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(String.format("%.2fx", playbackPitch), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = playbackPitch,
                            onValueChange = { AudioPlayerManager.setPlaybackPitch(it) },
                            valueRange = 0.5f..2.0f,
                            steps = 5
                        )
                    }

                    HorizontalDivider()

                    // Skip Silence Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { AudioPlayerManager.toggleSkipSilence() }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Skip Silence", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Automatically skip quiet segments", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isSkipSilence,
                            onCheckedChange = { AudioPlayerManager.toggleSkipSilence() }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun FullScreenLyricsModal(
    song: Song,
    playbackPositionMs: Long,
    syncedLyrics: List<com.example.data.LyricLine>,
    syncedLyricsResult: com.example.data.LyricsResult,
    onPositionChange: (Long) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp).copy(alpha = 0.98f),
                        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.98f),
                        MaterialTheme.colorScheme.background.copy(alpha = 0.98f)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .testTag("full_screen_lyrics_modal")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with artwork, song details, and prominent close icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(56.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        AsyncImage(
                            model = song.albumArtUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(14.dp))
                    
                    Column {
                        Text(
                            text = song.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = song.artist,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Prominent close icon matching Material Design 3 guidelines (circular background, target > 48dp)
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        )
                        .testTag("close_lyrics_modal_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Lyrics Modal",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Sync source label
            Text(
                text = "Synced via ${syncedLyricsResult.provider.displayName}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Auto-scrolling, highlighting, interactive lyrics view
            LyricsView(
                lyrics = syncedLyrics,
                playbackPositionMs = playbackPositionMs,
                onSeekTo = onPositionChange,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000L
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
