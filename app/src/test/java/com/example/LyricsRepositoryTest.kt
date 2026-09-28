package com.example

import com.example.data.LyricLine
import com.example.data.LyricsProvider
import com.example.data.LyricsRepository
import com.example.network.MusixmatchHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsRepositoryTest {

    private val repository = LyricsRepository()

    @Test
    fun `test fetch lyrics for mock song 1`() = runBlocking {
        val result = repository.fetchLyrics("1")
        val lyrics = result.lines
        assertTrue(lyrics.isNotEmpty())
        assertEquals(0L, lyrics[0].timeMs)
        assertTrue(lyrics.any { it.text.contains("neon dreams", ignoreCase = true) })
        assertEquals(LyricsProvider.LRCLIB, result.provider)
    }

    @Test
    fun `test dynamic lyrics generation for unknown song`() = runBlocking {
        val result = repository.fetchLyrics("unknown_id")
        val lyrics = result.lines
        assertTrue(lyrics.isNotEmpty())
        assertEquals(0L, lyrics[0].timeMs)
        // Ensure we got multiple generated lines
        assertTrue(lyrics.size > 2)
    }

    @Test
    fun `test parse LRC timestamp lyrics`() {
        val lrcSample = """
            [00:05.50]Hello world
            [00:15.00]Second line here
            [01:02.30]After one minute
        """.trimIndent()

        val parsed = repository.parseLrcLyrics(lrcSample)
        assertEquals(3, parsed.size)
        assertEquals(5500L, parsed[0].timeMs)
        assertEquals("Hello world", parsed[0].text)
        assertEquals(15000L, parsed[1].timeMs)
        assertEquals(62300L, parsed[2].timeMs)
    }

    @Test
    fun `test musixmatch package and store constants`() {
        assertEquals("com.musixmatch.android.lyrify", MusixmatchHelper.PACKAGE_NAME)
        assertTrue(MusixmatchHelper.PLAY_STORE_URL.contains("com.musixmatch.android.lyrify"))
    }

    @Test
    fun `test active lyric index matching logic`() {
        val lyricLines = listOf(
            LyricLine(0L, "Intro"),
            LyricLine(10000L, "First line"),
            LyricLine(20000L, "Second line"),
            LyricLine(30000L, "Third line")
        )

        // Helper function mimicking our activeIndex resolution logic in UI
        fun findActiveIndex(lines: List<LyricLine>, playbackPositionMs: Long): Int {
            val index = lines.indexOfLast { it.timeMs <= playbackPositionMs }
            return if (index == -1 && lines.isNotEmpty()) 0 else index
        }

        // Test start of song
        assertEquals(0, findActiveIndex(lyricLines, 0L))
        assertEquals(0, findActiveIndex(lyricLines, 5000L))

        // Test exact matches
        assertEquals(1, findActiveIndex(lyricLines, 10000L))
        assertEquals(2, findActiveIndex(lyricLines, 20000L))

        // Test mid-points
        assertEquals(1, findActiveIndex(lyricLines, 15000L))
        assertEquals(2, findActiveIndex(lyricLines, 25000L))

        // Test after last line
        assertEquals(3, findActiveIndex(lyricLines, 35000L))
        assertEquals(3, findActiveIndex(lyricLines, 100000L))
    }
}
