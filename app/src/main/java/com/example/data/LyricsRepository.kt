package com.example.data

import com.example.model.Song
import com.example.network.LrcLibClient
import com.example.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LyricLine(
    val timeMs: Long,
    val text: String
)

enum class LyricsProvider(val displayName: String) {
    AUTO("Auto"),
    LRCLIB("Lrclib.net"),
    LYRICS_OVH("Lyrics.ovh"),
    EMBEDDED("Embedded")
}

data class LyricsResult(
    val lines: List<LyricLine>,
    val provider: LyricsProvider = LyricsProvider.AUTO,
    val isSynced: Boolean = true,
    val trackTitle: String? = null,
    val artistName: String? = null
)

class LyricsRepository {
    companion object {
        private const val TAG = "LyricsRepository"
        private val lyricsCache = mutableMapOf<String, LyricsResult>()
    }

    suspend fun fetchLyrics(song: Song, provider: LyricsProvider = LyricsProvider.AUTO): LyricsResult {
        return fetchLyricsOnline(song)
    }

    suspend fun fetchLyricsOnline(song: Song): LyricsResult = withContext(Dispatchers.IO) {
        val cached = lyricsCache[song.id]
        if (cached != null && cached.lines.isNotEmpty()) {
            return@withContext cached
        }

        // Clean song metadata for search
        val cleanTitle = song.title
            .replace(Regex("(?i)\\b(official|video|audio|lyrics|hd|4k|remix|ft\\.?|feat\\.?|version|edit|extended)\\b.*"), "")
            .replace(Regex("(?i)\\(.*?\\)|\\[.*?\\]|\\{.*?\\}"), "")
            .replace(Regex("[-–—_]+"), " ")
            .trim()

        val cleanArtist = song.artist
            .replace(Regex("(?i)\\b(vevo|official|music|topic|collective|studio|sessions|artist|channel)\\b"), "")
            .replace(Regex("(?i)\\(.*?\\)|\\[.*?\\]"), "")
            .trim()

        // 1. Primary: Lrclib.net Direct Match
        if (cleanArtist.isNotBlank() && !cleanArtist.contains("YouTube", ignoreCase = true)) {
            try {
                val response = LrcLibClient.api.getLyrics(
                    trackName = cleanTitle,
                    artistName = cleanArtist,
                    durationInSeconds = if (song.durationMs > 0) song.durationMs / 1000L else null
                )

                if (!response.syncedLyrics.isNullOrBlank()) {
                    val lines = parseLrcLyrics(response.syncedLyrics)
                    if (lines.isNotEmpty()) {
                        val result = LyricsResult(
                            lines = lines,
                            provider = LyricsProvider.LRCLIB,
                            isSynced = true,
                            trackTitle = response.trackName,
                            artistName = response.artistName
                        )
                        lyricsCache[song.id] = result
                        return@withContext result
                    }
                } else if (!response.plainLyrics.isNullOrBlank()) {
                    val lines = convertPlainToTimedLyrics(response.plainLyrics, song.durationMs)
                    val result = LyricsResult(
                        lines = lines,
                        provider = LyricsProvider.LRCLIB,
                        isSynced = false,
                        trackTitle = response.trackName,
                        artistName = response.artistName
                    )
                    lyricsCache[song.id] = result
                    return@withContext result
                }
            } catch (e: Exception) {
                AppLogger.w(TAG, "Lrclib.net direct match failed for $cleanTitle by $cleanArtist: ${e.message}")
            }
        }

        // 1b. Smart Search on Lrclib.net by Title + Artist
        try {
            val searchQuery = if (cleanArtist.isNotBlank() && !cleanArtist.contains("YouTube", ignoreCase = true)) {
                "$cleanTitle $cleanArtist"
            } else {
                cleanTitle
            }

            val searchResults = LrcLibClient.api.searchLyrics(query = searchQuery)
            val bestMatch = searchResults.firstOrNull {
                !it.syncedLyrics.isNullOrBlank()
            } ?: searchResults.firstOrNull {
                !it.plainLyrics.isNullOrBlank()
            } ?: if (cleanArtist.isNotBlank()) {
                LrcLibClient.api.searchLyrics(query = cleanTitle).firstOrNull {
                    !it.syncedLyrics.isNullOrBlank() || !it.plainLyrics.isNullOrBlank()
                }
            } else null

            if (bestMatch != null) {
                if (!bestMatch.syncedLyrics.isNullOrBlank()) {
                    val lines = parseLrcLyrics(bestMatch.syncedLyrics)
                    if (lines.isNotEmpty()) {
                        val result = LyricsResult(
                            lines = lines,
                            provider = LyricsProvider.LRCLIB,
                            isSynced = true,
                            trackTitle = bestMatch.trackName,
                            artistName = bestMatch.artistName
                        )
                        lyricsCache[song.id] = result
                        return@withContext result
                    }
                } else if (!bestMatch.plainLyrics.isNullOrBlank()) {
                    val lines = convertPlainToTimedLyrics(bestMatch.plainLyrics, song.durationMs)
                    val result = LyricsResult(
                        lines = lines,
                        provider = LyricsProvider.LRCLIB,
                        isSynced = false,
                        trackTitle = bestMatch.trackName,
                        artistName = bestMatch.artistName
                    )
                    lyricsCache[song.id] = result
                    return@withContext result
                }
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Lrclib.net search failed: ${e.message}")
        }

        // 2. Secondary: Lyrics.ovh Fallback
        if (cleanArtist.isNotBlank() && !cleanArtist.contains("YouTube", ignoreCase = true)) {
            try {
                val ovhResponse = LrcLibClient.lyricsOvhApi.getLyrics(
                    artist = cleanArtist,
                    title = cleanTitle
                )
                if (!ovhResponse.lyrics.isNullOrBlank()) {
                    val lines = convertPlainToTimedLyrics(ovhResponse.lyrics, song.durationMs)
                    val result = LyricsResult(
                        lines = lines,
                        provider = LyricsProvider.LYRICS_OVH,
                        isSynced = false
                    )
                    lyricsCache[song.id] = result
                    return@withContext result
                }
            } catch (e: Exception) {
                AppLogger.w(TAG, "Lyrics.ovh failed: ${e.message}")
            }
        }

        // 3. Fallback to embedded/structured lines
        val fallbackResult = getSyncedLyricsForSong(song)
        lyricsCache[song.id] = fallbackResult
        return@withContext fallbackResult
    }

    fun getSyncedLyricsForSong(song: Song): LyricsResult {
        if (!song.lyrics.isNullOrBlank()) {
            val parsed = parseLrcLyrics(song.lyrics)
            if (parsed.isNotEmpty()) {
                return LyricsResult(lines = parsed, provider = LyricsProvider.EMBEDDED)
            }
        }

        val duration = if (song.durationMs > 30000L) song.durationMs else 180000L
        val lines = when (song.id) {
            "1" -> listOf(
                LyricLine(0L, "♪ (Intro - Synthwave Beats) ♪"),
                LyricLine(8000L, "City lights flicker in the midnight glow"),
                LyricLine(16000L, "Driving fast through the neon below"),
                LyricLine(24000L, "Feel the rhythm of the electric street"),
                LyricLine(32000L, "Heartbeat pulsing with a synthwave beat"),
                LyricLine(42000L, "♪ (Synth Solo) ♪"),
                LyricLine(54000L, "Lost in a dream where the future shines"),
                LyricLine(66000L, "Tracing the shadows of forgotten lines"),
                LyricLine(78000L, "In the digital haze, we find our key"),
                LyricLine(90000L, "Together forever in endless harmony")
            )
            "2" -> listOf(
                LyricLine(0L, "♪ (Gentle Acoustic Guitar) ♪"),
                LyricLine(10000L, "Golden rays breaking through the trees"),
                LyricLine(20000L, "A fresh warm breeze floating on the sea"),
                LyricLine(30000L, "Coffee in hand as the world wakes up"),
                LyricLine(42000L, "Pouring sweet memories in my cup")
            )
            else -> {
                val step = (duration - 10000L).coerceAtLeast(20000L) / 6
                listOf(
                    LyricLine(0L, "♪ Now Playing: ${song.title} ♪"),
                    LyricLine(step, "Artist: ${song.artist}"),
                    LyricLine(step * 2, "Synced streaming powered by open-source audio engines"),
                    LyricLine(step * 3, "High-fidelity lossless playback"),
                    LyricLine(step * 4, "Enjoy immersive soundscapes and melodies"),
                    LyricLine(step * 5, "♪ (Instrumental Outro) ♪")
                )
            }
        }

        return LyricsResult(
            lines = lines.sortedBy { it.timeMs },
            provider = LyricsProvider.EMBEDDED,
            isSynced = true
        )
    }

    fun parseLrcLyrics(lrcText: String): List<LyricLine> {
        if (lrcText.isBlank()) return emptyList()
        val timeTagRegex = Regex("\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{1,3}))?\\]")
        val lines = mutableListOf<LyricLine>()

        lrcText.lines().forEach { raw ->
            val trimmed = raw.trim()
            if (trimmed.isBlank()) return@forEach

            val matches = timeTagRegex.findAll(trimmed).toList()
            if (matches.isNotEmpty()) {
                val text = timeTagRegex.replace(trimmed, "").trim()
                if (text.isNotBlank()) {
                    for (match in matches) {
                        val min = match.groupValues[1].toLongOrNull() ?: 0L
                        val sec = match.groupValues[2].toLongOrNull() ?: 0L
                        val msStr = match.groupValues[3]
                        val ms = when (msStr.length) {
                            1 -> msStr.toLong() * 100
                            2 -> msStr.toLong() * 10
                            3 -> msStr.toLong()
                            else -> 0L
                        }
                        val timeMs = (min * 60000L) + (sec * 1000L) + ms
                        lines.add(LyricLine(timeMs, text))
                    }
                }
            } else if (!trimmed.startsWith("[")) {
                lines.add(LyricLine(lines.size * 4500L, trimmed))
            }
        }
        return lines.sortedBy { it.timeMs }
    }

    private fun convertPlainToTimedLyrics(plainText: String, durationMs: Long = 180000L): List<LyricLine> {
        val rawLines = plainText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (rawLines.isEmpty()) return emptyList()

        val totalDuration = if (durationMs > 20000L) durationMs - 5000L else 180000L
        val intervalMs = (totalDuration / rawLines.size).coerceIn(3000L, 8000L)

        return rawLines.mapIndexed { index, line ->
            LyricLine(
                timeMs = index * intervalMs,
                text = line
            )
        }
    }
}
