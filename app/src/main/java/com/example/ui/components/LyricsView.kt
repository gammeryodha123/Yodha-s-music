package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LyricLine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LyricsView(
    lyrics: List<LyricLine>,
    playbackPositionMs: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (lyrics.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "🎵",
                    fontSize = 48.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No Synced Lyrics Available",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    // Find current active lyric line index based on playback time
    val activeIndex = remember(playbackPositionMs, lyrics) {
        lyrics.indexOfLast { it.timeMs <= playbackPositionMs }
    }

    var userHasScrolledManually by remember { mutableStateOf(false) }
    var lastScrollTimestamp by remember { mutableLongStateOf(0L) }

    // Track user scrolling interaction
    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (lazyListState.isScrollInProgress) {
            userHasScrolledManually = true
            lastScrollTimestamp = System.currentTimeMillis()
        }
    }

    // Auto-reset manual scroll state after 4 seconds of inactivity
    LaunchedEffect(userHasScrolledManually, activeIndex) {
        if (userHasScrolledManually) {
            delay(4000L)
            userHasScrolledManually = false
        }
    }

    // Smooth scroll animation to center active lyric line
    LaunchedEffect(activeIndex, userHasScrolledManually) {
        if (activeIndex >= 0 && activeIndex < lyrics.size && !userHasScrolledManually) {
            coroutineScope.launch {
                lazyListState.animateScrollToItem(
                    index = activeIndex,
                    scrollOffset = -220 // Offsets active line towards the vertical center
                )
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("synced_lyrics_column"),
            contentPadding = PaddingValues(
                top = 180.dp,
                bottom = 260.dp,
                start = 20.dp,
                end = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(
                items = lyrics,
                key = { index, item -> "${index}_${item.timeMs}" }
            ) { index, line ->
                val isActive = index == activeIndex

                // Smooth property animations for active line highlighting
                val animatedFontSize by animateFloatAsState(
                    targetValue = if (isActive) 22f else 16f,
                    animationSpec = tween(durationMillis = 350)
                )

                val animatedAlpha by animateFloatAsState(
                    targetValue = if (isActive) 1.0f else 0.40f,
                    animationSpec = tween(durationMillis = 350)
                )

                val animatedColor by animateColorAsState(
                    targetValue = if (isActive) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    animationSpec = tween(durationMillis = 350)
                )

                val animatedScale by animateFloatAsState(
                    targetValue = if (isActive) 1.04f else 1.0f,
                    animationSpec = spring(stiffness = 300f)
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = animatedScale
                            scaleY = animatedScale
                        }
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple()
                        ) {
                            userHasScrolledManually = false
                            onSeekTo(line.timeMs)
                        }
                        .testTag("lyric_line_$index"),
                    color = if (isActive) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    } else {
                        Color.Transparent
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Text(
                            text = line.text,
                            fontSize = animatedFontSize.sp,
                            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                            color = animatedColor,
                            lineHeight = (animatedFontSize + 8).sp,
                            modifier = Modifier
                                .weight(1f)
                                .alpha(animatedAlpha)
                        )

                        if (isActive) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(8.dp)
                            ) {}
                        }
                    }
                }
            }
        }

        // Floating "Sync Lyrics" Re-center Button when manually scrolled away
        if (userHasScrolledManually && activeIndex >= 0) {
            FloatingActionButton(
                onClick = {
                    userHasScrolledManually = false
                    coroutineScope.launch {
                        lazyListState.animateScrollToItem(
                            index = activeIndex,
                            scrollOffset = -220
                        )
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .testTag("sync_lyrics_button"),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = CircleShape
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sync Lyrics",
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Sync to Current Line",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
