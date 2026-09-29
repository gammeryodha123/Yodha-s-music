package com.example

import com.example.data.LyricLine
import com.example.data.LyricsRepository
import com.example.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncedLyricsTest {

    @Test
    fun `test active lyric line matching at timestamp`() {
        val lyrics = listOf(
            LyricLine(0L, "Intro"),
            LyricLine(10000L, "First line"),
            LyricLine(25000L, "Second line"),
            LyricLine(40000L, "Third line")
        )

        val activeIndexAt5s = lyrics.indexOfLast { it.timeMs <= 5000L }
        val activeIndexAt15s = lyrics.indexOfLast { it.timeMs <= 15000L }
        val activeIndexAt30s = lyrics.indexOfLast { it.timeMs <= 30000L }

        assertEquals(0, activeIndexAt5s)
        assertEquals(1, activeIndexAt15s)
        assertEquals(2, activeIndexAt30s)
    }

    @Test
    fun `test LRC parser accurately parses timestamped lyrics`() {
        val lrcSample = """
            [00:10.50]Hello world
            [00:25.10]Synced lyrics in Kotlin Compose
            [01:05.00]End of track
        """.trimIndent()

        val repo = LyricsRepository()
        val lines = repo.parseLrcLyrics(lrcSample)

        assertEquals(3, lines.size)
        assertEquals(10500L, lines[0].timeMs)
        assertEquals("Hello world", lines[0].text)
        assertEquals(25100L, lines[1].timeMs)
        assertEquals(65000L, lines[2].timeMs)
    }
}
