package com.example.data

import androidx.compose.runtime.mutableStateListOf
import com.example.database.LocalSongEntity
import com.example.database.RecentSongEntity
import com.example.model.Playlist
import com.example.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MusicRepository {

    private val scope = CoroutineScope(Dispatchers.IO)

    fun getLikedSongsFlow(): Flow<List<Song>> {
        val db = AppDatabaseHelper.database ?: return flowOf(emptyList())
        return db.localSongDao().getLikedSongs().map { list ->
            list.map { entity ->
                Song(
                    id = entity.id,
                    title = entity.title,
                    artist = entity.artist,
                    albumArtUrl = entity.albumArtUrl,
                    streamUrl = entity.streamUrl,
                    durationMs = entity.durationMs,
                    lyrics = entity.lyrics ?: "",
                    isDownloaded = entity.isDownloaded,
                    localFilePath = entity.localFilePath,
                    isLiked = true,
                    genre = "Liked",
                    album = "Favorites"
                )
            }
        }
    }

    companion object {
        val likedSongs = mutableStateListOf<Song>()
        val customPlaylists = mutableStateListOf<Playlist>()

        fun getRecentlyPlayedSongs(): Flow<List<RecentSongEntity>> {
            val db = AppDatabaseHelper.database
            return db?.recentSongDao()?.getRecentSongsFlow() ?: flowOf(emptyList())
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
                durationMs = 210000L,
                genre = "Synthwave",
                album = "Neon Horizon"
            ),
            Song(
                id = "2",
                title = "Acoustic Sunrise",
                artist = "Morning Coffee",
                albumArtUrl = "https://picsum.photos/seed/music2/400/400",
                streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                durationMs = 195000L,
                genre = "Acoustic",
                album = "Daybreak Sessions"
            ),
            Song(
                id = "3",
                title = "Cyberpunk Echoes",
                artist = "Future Sound",
                albumArtUrl = "https://picsum.photos/seed/music3/400/400",
                streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                durationMs = 240000L,
                genre = "Cyberpunk",
                album = "Grid Runner"
            ),
            Song(
                id = "4",
                title = "Lofi Chill Beats",
                artist = "Chillhop Beats",
                albumArtUrl = "https://picsum.photos/seed/music4/400/400",
                streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                durationMs = 180000L,
                genre = "Lofi",
                album = "Late Night Study"
            ),
            Song(
                id = "5",
                title = "Midnight Highway",
                artist = "Retro Waves",
                albumArtUrl = "https://picsum.photos/seed/music5/400/400",
                streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
                durationMs = 225000L,
                genre = "Synthwave",
                album = "Outdrive"
            ),
            Song(
                id = "6",
                title = "Deep Forest Rain",
                artist = "Ambient Calm",
                albumArtUrl = "https://picsum.photos/seed/music6/400/400",
                streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
                durationMs = 260000L,
                genre = "Ambient",
                album = "Nature Escapes"
            ),
            Song(
                id = "7",
                title = "Solar Flares",
                artist = "Electro Horizon",
                albumArtUrl = "https://picsum.photos/seed/music7/400/400",
                streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3",
                durationMs = 215000L,
                genre = "Electronic",
                album = "Supernova"
            ),
            Song(
                id = "8",
                title = "Velvet Moon",
                artist = "Jazz Cafe",
                albumArtUrl = "https://picsum.photos/seed/music8/400/400",
                streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
                durationMs = 190000L,
                genre = "Jazz",
                album = "Midnight Blue"
            )
        )
    }

    fun getRecentSongs(): List<Song> = getSampleSongs().take(5)

    fun getFeaturedPlaylists(): List<Playlist> {
        val samples = getSampleSongs()
        return listOf(
            Playlist("p1", "Chill Vibes", "Relax, study and unwind with smooth tunes", "https://picsum.photos/seed/p1/400/400", listOf(samples[1], samples[3], samples[5])),
            Playlist("p2", "Cybernetic Drive", "High-energy synthwave and cyberpunk beats", "https://picsum.photos/seed/p2/400/400", listOf(samples[0], samples[2], samples[4], samples[6])),
            Playlist("p3", "Deep Focus", "Ambient sounds and soothing jazz for productivity", "https://picsum.photos/seed/p3/400/400", listOf(samples[5], samples[7], samples[3]))
        )
    }

    fun toggleLikeSong(song: Song) {
        val existing = likedSongs.find { it.id == song.id }
        if (existing != null) {
            likedSongs.remove(existing)
        } else {
            likedSongs.add(song.copy(isLiked = true))
        }

        scope.launch {
            try {
                val db = AppDatabaseHelper.database ?: return@launch
                val existingEntity = db.localSongDao().getSongById(song.id)
                if (existingEntity == null) {
                    db.localSongDao().insertSong(
                        LocalSongEntity(
                            id = song.id,
                            title = song.title,
                            artist = song.artist,
                            albumArtUrl = song.albumArtUrl,
                            streamUrl = song.streamUrl,
                            durationMs = song.durationMs,
                            lyrics = song.lyrics,
                            isLiked = (existing == null),
                            isDownloaded = false,
                            localFilePath = null,
                            downloadProgress = 0,
                            lastPlaybackPositionMs = 0L,
                            lastPlayedAt = System.currentTimeMillis()
                        )
                    )
                } else {
                    db.localSongDao().updateLikedStatus(song.id, existing == null)
                }
            } catch (e: Throwable) {
                com.example.util.AppLogger.w("MusicRepository", "Failed toggling like in database: ${e.message}")
            }
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
