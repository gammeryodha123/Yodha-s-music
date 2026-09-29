package com.example.data

import androidx.compose.runtime.mutableStateListOf
import com.example.database.RecentSongEntity
import com.example.model.Playlist
import com.example.model.Song
import kotlinx.coroutines.flow.Flow

class MusicRepository {

    companion object {
        val likedSongs = mutableStateListOf<Song>()
        val customPlaylists = mutableStateListOf<Playlist>()

        fun getRecentlyPlayedSongs(): Flow<List<RecentSongEntity>> {
            return AppDatabaseHelper.database.recentSongDao().getRecentSongsFlow()
        }
    }

    fun getSampleSongs(): List<Song> {
        return listOf(
            Song(
                id = "1",
                title = "Neon Dreams",
                artist = "Synthwave Collective",
                albumArtUrl = "https://picsum.photos/seed/music1/400/400",
                streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                durationMs = 210000L
            ),
            Song(
                id = "2",
                title = "Acoustic Sunrise",
                artist = "Morning Coffee",
                albumArtUrl = "https://picsum.photos/seed/music2/400/400",
                streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                durationMs = 195000L
            ),
            Song(
                id = "3",
                title = "Cyberpunk Echoes",
                artist = "Future Sound",
                albumArtUrl = "https://picsum.photos/seed/music3/400/400",
                streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                durationMs = 240000L
            ),
            Song(
                id = "4",
                title = "Lofi Chill Beats",
                artist = "Chillhop Beats",
                albumArtUrl = "https://picsum.photos/seed/music4/400/400",
                streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                durationMs = 180000L
            )
        )
    }

    fun getRecentSongs(): List<Song> = getSampleSongs()

    fun getFeaturedPlaylists(): List<Playlist> {
        val samples = getSampleSongs()
        return listOf(
            Playlist("p1", "Chill Vibes", "Relax and unwind", "https://picsum.photos/seed/p1/400/400", samples.take(2)),
            Playlist("p2", "Electronic Beats", "Upbeat synthwave", "https://picsum.photos/seed/p2/400/400", samples.drop(2))
        )
    }

    fun toggleLikeSong(song: Song) {
        val existing = likedSongs.find { it.id == song.id }
        if (existing != null) {
            likedSongs.remove(existing)
        } else {
            likedSongs.add(song.copy(isLiked = true))
        }
    }

    fun addSongToPlaylist(playlistId: String, song: Song) {
        val index = customPlaylists.indexOfFirst { it.id == playlistId }
        if (index != -1) {
            val pl = customPlaylists[index]
            if (pl.songs.none { it.id == song.id }) {
                customPlaylists[index] = pl.copy(songs = pl.songs + song)
            }
        }
    }

    fun createPlaylist(name: String, description: String = "") {
        val newPl = Playlist(
            id = "custom_${System.currentTimeMillis()}",
            name = name,
            description = description,
            coverUrl = "https://picsum.photos/seed/$name/400/400",
            songs = emptyList()
        )
        customPlaylists.add(newPl)
    }
}
