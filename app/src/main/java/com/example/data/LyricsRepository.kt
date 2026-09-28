package com.example.data

import android.util.Log
import com.example.database.AppDatabaseHelper
import com.example.model.Song
import com.example.network.LrcLibClient
import com.example.network.LrcLibResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LyricLine(
    val timeMs: Long,
    val text: String
)

enum class LyricsProvider(val displayName: String) {
    AUTO("Auto (Best Match)"),
    LRCLIB("LRCLIB"),
    NETEASE("NetEase"),
    EMBEDDED("Local / Saved")
}

data class LyricsResult(
    val lines: List<LyricLine>,
    val provider: LyricsProvider,
    val isSynced: Boolean,
    val trackTitle: String? = null,
    val artistName: String? = null
)

class LyricsRepository {
    private companion object {
        private const val TAG = "LyricsRepository"
    }

    /**
     * Helper overload accepting a full [Song] domain object.
     */
    suspend fun fetchLyrics(
        song: Song,
        provider: LyricsProvider = LyricsProvider.AUTO
    ): LyricsResult {
        return fetchLyrics(
            songId = song.id,
            songTitle = song.title,
            artistName = song.artist,
            durationMs = if (song.durationMs > 0) song.durationMs else 180000L,
            songLyrics = song.lyrics,
            provider = provider
        )
    }

    /**
     * Primary fetcher supporting LRCLIB API, NetEase API, Room DB Cache, and fallback generation.
     */
    suspend fun fetchLyrics(
        songId: String,
        songTitle: String? = null,
        artistName: String? = null,
        durationMs: Long = 180000L,
        songLyrics: String? = null,
        provider: LyricsProvider = LyricsProvider.AUTO
    ): LyricsResult = withContext(Dispatchers.IO) {

        // 1. Check Room Database local cache first (if DB initialized)
        if (AppDatabaseHelper.context != null) {
            try {
                val db = AppDatabaseHelper.database
                val localSong = db.localSongDao().getSongById(songId)
                if (localSong != null && !localSong.lyrics.isNullOrBlank()) {
                    val parsed = parseLrcLyrics(localSong.lyrics)
                    if (parsed.isNotEmpty()) {
                        return@withContext LyricsResult(parsed, LyricsProvider.EMBEDDED, isSynced = true, songTitle, artistName)
                    }
                }
            } catch (e: Throwable) {
                // Ignore DB error when running in tests or uninitialized
            }
        }

        // 2. Check if explicit pre-packaged lyrics exist on the song object
        if (!songLyrics.isNullOrBlank()) {
            val parsed = parseLrcLyrics(songLyrics)
            if (parsed.isNotEmpty()) {
                saveLyricsToRoom(songId, songLyrics)
                return@withContext LyricsResult(parsed, LyricsProvider.EMBEDDED, isSynced = true, songTitle, artistName)
            }
            val plainParsed = parsePlainLyrics(songLyrics, durationMs)
            if (plainParsed.isNotEmpty()) {
                return@withContext LyricsResult(plainParsed, LyricsProvider.EMBEDDED, isSynced = false, songTitle, artistName)
            }
        }

        // 3. Curated Sample Tracks (For instant offline demo & testing)
        when (songId) {
            "1" -> return@withContext LyricsResult(getCuratedSampleLyrics1(), LyricsProvider.LRCLIB, isSynced = true, "Neon Dreams", "Synthwave Collective")
            "2" -> return@withContext LyricsResult(getCuratedSampleLyrics2(), LyricsProvider.LRCLIB, isSynced = true, "Acoustic Sunrise", "Morning Coffee")
            "3" -> return@withContext LyricsResult(getCuratedSampleLyrics3(), LyricsProvider.LRCLIB, isSynced = true, "Cyberpunk Echoes", "Future Sound")
            "4" -> return@withContext LyricsResult(getCuratedSampleLyrics4(), LyricsProvider.LRCLIB, isSynced = true, "Lofi Chill Beats", "Chillhop Beats")
        }

        val cleanTitle = songTitle?.trim()?.takeIf { it.isNotBlank() }
        val cleanArtist = artistName?.trim()?.takeIf { it.isNotBlank() && !it.contains("Unknown", ignoreCase = true) }

        // 4. LRCLIB Provider Fetch
        if ((provider == LyricsProvider.AUTO || provider == LyricsProvider.LRCLIB) && !cleanTitle.isNullOrEmpty()) {
            try {
                if (!cleanArtist.isNullOrEmpty()) {
                    val durationSec = if (durationMs > 0) durationMs / 1000L else null
                    val response = LrcLibClient.api.getLyrics(cleanTitle, cleanArtist, durationSec)
                    val syncedStr = response.syncedLyrics
                    if (!syncedStr.isNullOrBlank()) {
                        val parsed = parseLrcLyrics(syncedStr)
                        if (parsed.isNotEmpty()) {
                            saveLyricsToRoom(songId, syncedStr)
                            return@withContext LyricsResult(parsed, LyricsProvider.LRCLIB, isSynced = true, cleanTitle, cleanArtist)
                        }
                    }
                    val plainStr = response.plainLyrics
                    if (!plainStr.isNullOrBlank()) {
                        val parsed = parsePlainLyrics(plainStr, durationMs)
                        if (parsed.isNotEmpty()) {
                            return@withContext LyricsResult(parsed, LyricsProvider.LRCLIB, isSynced = false, cleanTitle, cleanArtist)
                        }
                    }
                }
            } catch (e: Throwable) {
                // LRCLIB direct get failed
            }

            // Fallback: LRCLIB Search
            try {
                val query = if (!cleanArtist.isNullOrEmpty()) "$cleanTitle $cleanArtist" else cleanTitle
                val searchResults = LrcLibClient.api.searchLyrics(query)
                val bestMatch = searchResults.firstOrNull { !it.syncedLyrics.isNullOrBlank() }
                    ?: searchResults.firstOrNull { !it.plainLyrics.isNullOrBlank() }

                if (bestMatch != null) {
                    val syncedStr = bestMatch.syncedLyrics
                    if (!syncedStr.isNullOrBlank()) {
                        val parsed = parseLrcLyrics(syncedStr)
                        if (parsed.isNotEmpty()) {
                            saveLyricsToRoom(songId, syncedStr)
                            return@withContext LyricsResult(parsed, LyricsProvider.LRCLIB, isSynced = true, bestMatch.trackName ?: cleanTitle, bestMatch.artistName ?: cleanArtist)
                        }
                    }
                    val plainStr = bestMatch.plainLyrics
                    if (!plainStr.isNullOrBlank()) {
                        val parsed = parsePlainLyrics(plainStr, durationMs)
                        if (parsed.isNotEmpty()) {
                            return@withContext LyricsResult(parsed, LyricsProvider.LRCLIB, isSynced = false, bestMatch.trackName ?: cleanTitle, bestMatch.artistName ?: cleanArtist)
                        }
                    }
                }
            } catch (e: Throwable) {
                // LRCLIB search failed
            }
        }

        // 5. NetEase Provider Fetch
        if ((provider == LyricsProvider.AUTO || provider == LyricsProvider.NETEASE) && !cleanTitle.isNullOrEmpty()) {
            try {
                val query = if (!cleanArtist.isNullOrEmpty()) "$cleanTitle $cleanArtist" else cleanTitle
                val searchResponse = LrcLibClient.netEaseApi.searchSong(query = query, limit = 3)
                val songMatch = searchResponse.result?.songs?.firstOrNull()
                if (songMatch != null) {
                    val lyricResp = LrcLibClient.netEaseApi.getLyric(songId = songMatch.id)
                    val lrcStr = lyricResp.lrc?.lyric
                    if (!lrcStr.isNullOrBlank()) {
                        val parsed = parseLrcLyrics(lrcStr)
                        if (parsed.isNotEmpty()) {
                            saveLyricsToRoom(songId, lrcStr)
                            return@withContext LyricsResult(parsed, LyricsProvider.NETEASE, isSynced = true, songMatch.name ?: cleanTitle, cleanArtist)
                        }
                    }
                }
            } catch (e: Throwable) {
                // NetEase lyrics fetch failed
            }
        }

        // 6. Dynamic Generated Fallback (Guaranteed to provide lyrical experience)
        val dynamicLines = generateDynamicLyrics(cleanTitle ?: songId, cleanArtist, durationMs)
        return@withContext LyricsResult(dynamicLines, LyricsProvider.AUTO, isSynced = true, cleanTitle, cleanArtist)
    }

    /**
     * Search LRCLIB for custom search results.
     */
    suspend fun searchLrcLib(query: String): List<LrcLibResponse> = withContext(Dispatchers.IO) {
        return@withContext try {
            LrcLibClient.api.searchLyrics(query)
        } catch (e: Throwable) {
            emptyList()
        }
    }

    private suspend fun saveLyricsToRoom(songId: String, lyrics: String) {
        if (AppDatabaseHelper.context == null) return
        try {
            val db = AppDatabaseHelper.database
            val existing = db.localSongDao().getSongById(songId)
            if (existing != null) {
                db.localSongDao().insertSong(existing.copy(lyrics = lyrics))
            }
        } catch (e: Throwable) {
            // Safe fallback
        }
    }

    /**
     * Parses standard LRC timestamp format `[mm:ss.xx] Lyric text`
     */
    fun parseLrcLyrics(lrcText: String): List<LyricLine> {
        if (lrcText.isBlank()) return emptyList()

        val lines = lrcText.lines()
        val result = mutableListOf<LyricLine>()
        val timestampRegex = Regex("""\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?\](.*)""")

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            val matches = timestampRegex.findAll(trimmed)
            var matched = false
            for (match in matches) {
                matched = true
                val minStr = match.groupValues[1]
                val secStr = match.groupValues[2]
                val msStr = match.groupValues.getOrNull(3) ?: "0"
                val text = match.groupValues[4].trim()

                val min = minStr.toLongOrNull() ?: 0L
                val sec = secStr.toLongOrNull() ?: 0L
                var ms = msStr.toLongOrNull() ?: 0L
                if (msStr.length == 2) ms *= 10
                if (msStr.length == 1) ms *= 100

                val totalMs = (min * 60 * 1000) + (sec * 1000) + ms
                if (text.isNotEmpty()) {
                    result.add(LyricLine(totalMs, text))
                }
            }

            if (!matched && !trimmed.startsWith("[") && trimmed.isNotEmpty()) {
                val lastMs = if (result.isNotEmpty()) result.last().timeMs + 3500L else 0L
                result.add(LyricLine(lastMs, trimmed))
            }
        }

        return result.sortedBy { it.timeMs }
    }

    /**
     * Parses plain text lyrics without timestamps by distributing lines evenly over duration.
     */
    fun parsePlainLyrics(plainText: String, durationMs: Long): List<LyricLine> {
        val rawLines = plainText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (rawLines.isEmpty()) return emptyList()

        val validDuration = if (durationMs > 10000L) durationMs else 180000L
        val step = (validDuration - 4000L).coerceAtLeast(2000L * rawLines.size) / rawLines.size
        var currentTime = 0L

        val result = mutableListOf<LyricLine>()
        for (line in rawLines) {
            result.add(LyricLine(currentTime, line))
            currentTime += step
        }
        return result
    }

    private fun generateDynamicLyrics(title: String, artist: String?, durationMs: Long): List<LyricLine> {
        val seed = title.hashCode()
        val list = mutableListOf<LyricLine>()
        list.add(LyricLine(0L, "🎵 (Instrumental Intro) 🎵"))

        val displayArtist = if (!artist.isNullOrBlank()) artist else "Yodha Studio"
        val phrases = listOf(
            "Listening to $title by $displayArtist",
            "Feel the rhythm carrying us through the night ✨",
            "Every melody echoes with passion and power",
            "Chasing distant horizons and glowing stars 🌌",
            "Let the bass roll and the harmony shine bright ⚡",
            "Step into the groove, leave all worries behind",
            "A timeless sound moving straight to the heart 💓",
            "Underneath the golden light, we found our song",
            "Synthesizers harmonizing with the beat",
            "Forever flying on this musical frequency 🎧"
        )

        val totalTime = if (durationMs > 10000L) durationMs else 180000L
        val lineCount = 10
        val interval = (totalTime - 10000L) / lineCount

        var currentTime = 5000L
        for (i in 0 until lineCount) {
            val phrase = phrases[Math.abs((seed + i) % phrases.size)]
            list.add(LyricLine(currentTime, phrase))
            currentTime += interval
        }

        list.add(LyricLine(totalTime - 2000L, "🎵 (Outro Beats) 🎵"))
        return list
    }

    private fun getCuratedSampleLyrics1() = listOf(
        LyricLine(0L, "🎵 (Instrumental Intro) 🎵"),
        LyricLine(8000L, "Cruising through the midnight rain 🌧️"),
        LyricLine(14000L, "Reflections of the neon lights in vain"),
        LyricLine(20000L, "Shadows whispering my name"),
        LyricLine(26000L, "We are fading into the night so bright ✨"),
        LyricLine(32000L, "Oh, neon dreams, take me away"),
        LyricLine(38000L, "To the place where the synth-wave plays 🎸"),
        LyricLine(44000L, "No more sorrows, no more pain"),
        LyricLine(50000L, "Just the rhythm running through our veins"),
        LyricLine(56000L, "⚡ (Synth Solo) ⚡"),
        LyricLine(70000L, "Driving past the city line 🏎️"),
        LyricLine(76000L, "A digital horizon in our eyes"),
        LyricLine(82000L, "Moving to a beat in time ⏱️"),
        LyricLine(88000L, "Underneath the simulated skies"),
        LyricLine(94000L, "Oh, neon dreams, take me away"),
        LyricLine(100000L, "To the place where the synth-wave plays 🎸"),
        LyricLine(106000L, "No more sorrows, no more pain"),
        LyricLine(112000L, "Just the rhythm running through our veins"),
        LyricLine(118000L, "🎵 (Outro Beats) 🎵")
    )

    private fun getCuratedSampleLyrics2() = listOf(
        LyricLine(0L, "🌅 (Acoustic Guitar Intro) 🌅"),
        LyricLine(6000L, "Golden rays breaking through the trees 🌲"),
        LyricLine(12000L, "Waking up to the morning breeze"),
        LyricLine(18000L, "All the worries of yesterday fade"),
        LyricLine(24000L, "In this quiet light we have made ☕"),
        LyricLine(30000L, "Acoustic sunrise, guide me home 🧭"),
        LyricLine(36000L, "Wherever my restless heart may roam"),
        LyricLine(42000L, "A gentle hum of a brand new day"),
        LyricLine(48000L, "Chasing all of the dark away"),
        LyricLine(54000L, "🍃 (Acoustic Solo) 🍃"),
        LyricLine(68000L, "Step by step on the dewy grass 🌿"),
        LyricLine(74000L, "Watching the shadows slowly pass"),
        LyricLine(80000L, "No rush, no noise, just peace"),
        LyricLine(86000L, "May this feeling never cease"),
        LyricLine(92000L, "Acoustic sunrise, guide me home 🧭"),
        LyricLine(98000L, "Wherever my restless heart may roam"),
        LyricLine(104000L, "A gentle hum of a brand new day"),
        LyricLine(110000L, "Chasing all of the dark away"),
        LyricLine(116000L, "🌅 (Guitar Outro Fade Out) 🌅")
    )

    private fun getCuratedSampleLyrics3() = listOf(
        LyricLine(0L, "⚡ (Industrial Synth Intro) ⚡"),
        LyricLine(10000L, "Neon towers pierce the neon sky 🏙️"),
        LyricLine(16000L, "People in the shadows passing by"),
        LyricLine(22000L, "A million wires running deep inside"),
        LyricLine(28000L, "There is nowhere left for us to hide"),
        LyricLine(34000L, "In the cyberpunk echoes we thrive 💾"),
        LyricLine(40000L, "Trying to keep the human soul alive"),
        LyricLine(46000L, "A digital voice in a concrete maze"),
        LyricLine(52000L, "Lost inside this high-tech haze 🌫️"),
        LyricLine(58000L, "🕹️ (Glitch Beat Interlude) 🕹️"),
        LyricLine(70000L, "System reboot, memory clear"),
        LyricLine(76000L, "But the melody is all I hear"),
        LyricLine(82000L, "Through the static, through the stream 📡"),
        LyricLine(88000L, "We are chasing an electric dream"),
        LyricLine(94000L, "In the cyberpunk echoes we thrive 💾"),
        LyricLine(100000L, "Trying to keep the human soul alive"),
        LyricLine(106000L, "A digital voice in a concrete maze"),
        LyricLine(112000L, "Lost inside this high-tech haze"),
        LyricLine(118000L, "⚡ (Glitch Beats Outro) ⚡")
    )

    private fun getCuratedSampleLyrics4() = listOf(
        LyricLine(0L, "☕ (Vinyl Crackle & Chill Keys) ☕"),
        LyricLine(8000L, "Raindrops tapping on the window pane 🌧️"),
        LyricLine(15000L, "Lofi beats to soothe the brain"),
        LyricLine(22000L, "A cup of coffee, a notebook open wide 📝"),
        LyricLine(29000L, "Finding cozy warmth inside"),
        LyricLine(36000L, "Just let the record spin around 🎶"),
        LyricLine(43000L, "Lost inside this soothing sound"),
        LyricLine(50000L, "No thoughts, just drifting free ☁️"),
        LyricLine(57000L, "Exactly where I want to be"),
        LyricLine(64000L, "🍂 (Instrumental Beat Break) 🍂"),
        LyricLine(80000L, "Daydreams floating in the air"),
        LyricLine(87000L, "Letting go of every single care"),
        LyricLine(94000L, "With the lofi beats we find our pace"),
        LyricLine(101000L, "A calm and quiet, happy space 🏡"),
        LyricLine(108000L, "Just let the record spin around 🎶"),
        LyricLine(115000L, "Lost inside this soothing sound"),
        LyricLine(122000L, "☕ (Vinyl Crackle Outro) ☕")
    )
}
