package com.example

import com.example.data.MusicRepository
import com.example.model.Song
import com.example.network.AudioPlayerManager
import com.example.network.AudioPreset
import com.example.network.RepeatMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicPlayerFeatureTest {

    @Test
    fun `test sample song repository contents`() {
        val repo = MusicRepository()
        val songs = repo.getSampleSongs()
        assertTrue(songs.size >= 8)

        songs.forEach { song ->
            assertTrue(song.genre.isNotBlank())
            assertTrue(song.album.isNotBlank())
            assertTrue(song.streamUrl.startsWith("http"))
        }
    }

    @Test
    fun `test repeat mode transitions correctly`() {
        // First transition from initial state (OFF)
        AudioPlayerManager.toggleRepeatMode()
        assertEquals(RepeatMode.ALL, AudioPlayerManager.repeatMode.value)

        AudioPlayerManager.toggleRepeatMode()
        assertEquals(RepeatMode.ONE, AudioPlayerManager.repeatMode.value)

        AudioPlayerManager.toggleRepeatMode()
        assertEquals(RepeatMode.OFF, AudioPlayerManager.repeatMode.value)
    }

    @Test
    fun `test shuffle mode toggle`() {
        val initial = AudioPlayerManager.isShuffleEnabled.value
        AudioPlayerManager.toggleShuffle()
        assertEquals(!initial, AudioPlayerManager.isShuffleEnabled.value)
    }

    @Test
    fun `test audio preset configuration`() {
        AudioPlayerManager.setAudioPreset(AudioPreset.BASS_BOOST)
        assertEquals(AudioPreset.BASS_BOOST, AudioPlayerManager.audioPreset.value)
        assertEquals(1.4f, AudioPreset.BASS_BOOST.bassMultiplier, 0.01f)
    }

    @Test
    fun `test queue addition and removal`() {
        val testSong = Song(
            id = "test_101",
            title = "Test Queue Track",
            artist = "Queue Master",
            albumArtUrl = "https://picsum.photos/seed/q101/300/300"
        )

        AudioPlayerManager.addToQueue(testSong)
        assertTrue(AudioPlayerManager.playlist.value.any { it.id == "test_101" })

        val index = AudioPlayerManager.playlist.value.indexOfFirst { it.id == "test_101" }
        if (index != -1) {
            AudioPlayerManager.removeFromQueue(index)
            assertTrue(AudioPlayerManager.playlist.value.none { it.id == "test_101" })
        }
    }
}
