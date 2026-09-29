package com.example

import com.example.data.LyricsRepository
import com.example.model.Song
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsRepositoryTest {

    @Test
    fun `test fetch lyrics returns non-empty synced lines`() = runBlocking {
        val repo = LyricsRepository()
        val song = Song(
            id = "1",
            title = "Neon Dreams",
            artist = "Synthwave Collective",
            albumArtUrl = "https://picsum.photos/seed/music1/400/400"
        )

        val result = repo.fetchLyrics(song)
        assertTrue(result.lines.isNotEmpty())
        assertEquals(0L, result.lines.first().timeMs)
    }

    @Test
    fun `test LRC parser parses timestamped lines`() {
        val repo = LyricsRepository()
        val lrc = "[00:12.30]Verse 1 lyrics\n[00:24.50]Chorus lyrics"
        val lines = repo.parseLrcLyrics(lrc)

        assertEquals(2, lines.size)
        assertEquals(12300L, lines[0].timeMs)
        assertEquals("Verse 1 lyrics", lines[0].text)
        assertEquals(24500L, lines[1].timeMs)
        assertEquals("Chorus lyrics", lines[1].text)
    }
}
