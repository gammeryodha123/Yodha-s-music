package com.example

import com.example.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class BottomPlayerBarTest {

    @Test
    fun `test Song domain model creation for mini player`() {
        val song = Song(
            id = "test_1",
            title = "Midnight Drive",
            artist = "Retro Wave",
            albumArtUrl = "https://example.com/art.jpg",
            streamUrl = "https://example.com/audio.mp3",
            durationMs = 200000L
        )

        assertEquals("test_1", song.id)
        assertEquals("Midnight Drive", song.title)
        assertEquals("Retro Wave", song.artist)
        assertEquals(200000L, song.durationMs)
    }

    @Test
    fun `test progress calculation fraction`() {
        val positionMs = 50000L
        val durationMs = 200000L
        val fraction = (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)

        assertEquals(0.25f, fraction, 0.001f)
    }
}
