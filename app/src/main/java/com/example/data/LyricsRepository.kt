package com.example.data

import com.example.model.Song

data class LyricLine(
    val timeMs: Long,
    val text: String
)

enum class LyricsProvider(val displayName: String) {
    AUTO("Auto"),
    LRCLIB("LRCLIB"),
    NETEASE("NetEase"),
    EMBEDDED("Local")
}

data class LyricsResult(
    val lines: List<LyricLine>,
    val provider: LyricsProvider = LyricsProvider.AUTO,
    val isSynced: Boolean = true,
    val trackTitle: String? = null,
    val artistName: String? = null
)

class LyricsRepository {

    fun getSyncedLyricsForSong(song: Song): LyricsResult {
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
                LyricLine(9000L, "Together forever in endless harmony"),
                LyricLine(105000L, "♪ (Instrumental Breakdown) ♪"),
                LyricLine(125000L, "Neon dreams guiding us through the night"),
                LyricLine(140000L, "Fading away into the morning light"),
                LyricLine(160000L, "Hold on tight until the break of day"),
                LyricLine(180000L, "Where neon dreams never fade away...")
            )
            "2" -> listOf(
                LyricLine(0L, "♪ (Gentle Acoustic Guitar) ♪"),
                LyricLine(10000L, "Golden rays breaking through the trees"),
                LyricLine(20000L, "A fresh warm breeze floating on the sea"),
                LyricLine(30000L, "Coffee in hand as the world wakes up"),
                LyricLine(42000L, "Pouring sweet memories in my cup"),
                LyricLine(55000L, "♪ (Acoustic Strumming) ♪"),
                LyricLine(70000L, "Acoustic sunrise softly calling my name"),
                LyricLine(85000L, "Life moves on, but love remains the same"),
                LyricLine(100000L, "Step outside and take in the view"),
                LyricLine(120000L, "Every new dawn is a chance brand new"),
                LyricLine(145000L, "♪ (Melodic Outro) ♪"),
                LyricLine(170000L, "Acoustic sunrise fading softly away...")
            )
            "3" -> listOf(
                LyricLine(0L, "♪ (Dark Techno Atmosphere) ♪"),
                LyricLine(120000L, "Echoes in the grid, signals in the dark"),
                LyricLine(25000L, "Cybernetic pulses leaving a spark"),
                LyricLine(40000L, "System reboot, memory overload"),
                LyricLine(55000L, "Walking down the cybernetic road"),
                LyricLine(75000L, "♪ (Heavy Bass Drop) ♪"),
                LyricLine(95000L, "Cyberpunk echoes through the virtual sky"),
                LyricLine(115000L, "Data streams rushing as time flies by"),
                LyricLine(140000L, "Hacking through the static, breaking the wall"),
                LyricLine(165000L, "Cyberpunk echoes never fall"),
                LyricLine(190000L, "♪ (Outro Beats) ♪")
            )
            "4" -> listOf(
                LyricLine(0L, "♪ (Lofi Chill Vinyl Scratch) ♪"),
                LyricLine(10000L, "Raindrops falling on the window pane"),
                LyricLine(22000L, "Soft lofi chords taking away the pain"),
                LyricLine(35000L, "Sipping warm tea while studying late"),
                LyricLine(50000L, "Letting time flow, trusting in fate"),
                LyricLine(68000L, "♪ (Chillhop Jazz Trumpet) ♪"),
                LyricLine(88000L, "Peaceful moments in a quiet room"),
                LyricLine(108000L, "Flowers in the garden starting to bloom"),
                LyricLine(130000L, "Chill beats looping smoothly in the background"),
                LyricLine(150000L, "Pure relaxation in every sound")
            )
            else -> parseLrcLyrics(song.lyrics ?: "")
        }

        return LyricsResult(
            lines = lines,
            provider = LyricsProvider.LRCLIB,
            isSynced = lines.isNotEmpty()
        )
    }

    suspend fun fetchLyrics(
        song: Song,
        provider: LyricsProvider = LyricsProvider.AUTO
    ): LyricsResult {
        return getSyncedLyricsForSong(song)
    }

    fun parseLrcLyrics(lrcText: String): List<LyricLine> {
        if (lrcText.isBlank()) return emptyList()
        val regex = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)")
        val lines = mutableListOf<LyricLine>()

        lrcText.lines().forEach { raw ->
            val match = regex.find(raw.trim())
            if (match != null) {
                val min = match.groupValues[1].toLongOrNull() ?: 0L
                val sec = match.groupValues[2].toLongOrNull() ?: 0L
                val msPart = match.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
                val timeMs = (min * 60000L) + (sec * 1000L) + msPart
                val text = match.groupValues[4].trim()
                if (text.isNotBlank()) {
                    lines.add(LyricLine(timeMs, text))
                }
            }
        }
        return lines.sortedBy { it.timeMs }
    }
}
