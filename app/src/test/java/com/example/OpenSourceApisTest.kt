package com.example

import com.example.data.LyricsRepository
import com.example.model.Song
import com.example.network.MusicSourcesManager
import com.example.network.SearchSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenSourceApisTest {

    @Test
    fun `test MusicSourcesManager search returns playable tracks for JioSaavn and Piped`() = runBlocking {
        val results = MusicSourcesManager.searchAllSources("Kesariya", SearchSource.ALL)
        assertTrue(results.isNotEmpty())
        val firstTrack = results.first()
        assertNotNull(firstTrack.title)
        assertNotNull(firstTrack.artist)
        assertTrue(firstTrack.streamUrl.startsWith("http"))
    }

    @Test
    fun `test LyricsRepository parses LRC synchronized text correctly`() {
        val repo = LyricsRepository()
        val lrcSample = """
            [00:12.34] Yeah, I feel it in my bones
            [00:18.50] Enough to make my systems grow
            [00:24.10] Welcome to the new age
        """.trimIndent()

        val lines = repo.parseLrcLyrics(lrcSample)
        assertEquals(3, lines.size)
        assertEquals(12340L, lines[0].timeMs)
        assertEquals("Yeah, I feel it in my bones", lines[0].text)
        assertEquals(18500L, lines[1].timeMs)
    }

    @Test
    fun `test LyricsRepository online fetch falls back smoothly when offline`() = runBlocking {
        val repo = LyricsRepository()
        val song = Song(
            id = "test_open_source",
            title = "Believer",
            artist = "Imagine Dragons",
            albumArtUrl = "https://picsum.photos/seed/believer/400/400",
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
        )

        val lyricsResult = repo.fetchLyricsOnline(song)
        assertNotNull(lyricsResult)
        assertTrue(lyricsResult.lines.isNotEmpty())
    }
}
