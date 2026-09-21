package com.example

import com.example.data.LyricLine
import com.example.data.LyricsRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsRepositoryTest {

    private val repository = LyricsRepository()

    @Test
    fun `test fetch lyrics for mock song 1`() = runBlocking {
        val lyrics = repository.fetchLyrics("1")
        assertTrue(lyrics.isNotEmpty())
        assertEquals(0L, lyrics[0].timeMs)
        assertTrue(lyrics.any { it.text.contains("neon dreams", ignoreCase = true) })
    }

    @Test
    fun `test dynamic lyrics generation for unknown song`() = runBlocking {
        val lyrics = repository.fetchLyrics("unknown_id")
        assertTrue(lyrics.isNotEmpty())
        assertEquals(0L, lyrics[0].timeMs)
        // Ensure we got multiple generated lines
        assertTrue(lyrics.size > 2)
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
