package com.example.data

import androidx.compose.runtime.mutableStateListOf
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.example.model.Playlist
import com.example.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MusicRepository {
    companion object {
        var isDemoLoggedIn = false
        val likedSongs = mutableStateListOf<Song>()
        val customPlaylists = mutableStateListOf<Playlist>(
            Playlist(
                id = "liked",
                name = "Liked Songs",
                coverUrl = "https://picsum.photos/seed/liked/300/300",
                description = "Your favorite tracks",
                songs = emptyList()
            )
        )

        val masterSongList = listOf(
            Song("1", "Neon Dreams", "Synthwave Yodha", "https://picsum.photos/seed/s1/300/300", "", 210000),
            Song("2", "Acoustic Sunrise", "Chill Vibes", "https://picsum.photos/seed/s2/300/300", "", 180000),
            Song("3", "Cyberpunk Echoes", "Neo-Tokyo", "https://picsum.photos/seed/s3/300/300", "", 240000),
            Song("4", "Lofi Beats", "Study Girl", "https://picsum.photos/seed/s4/300/300", "", 150000),
            Song("5", "Midnight Drive", "Electro Spark", "https://picsum.photos/seed/s5/300/300", "", 195000),
            Song("6", "Summer Breeze", "Sunkissed", "https://picsum.photos/seed/s6/300/300", "", 165000),
            Song("7", "Electric Hearts", "Synth City", "https://picsum.photos/seed/s7/300/300", "", 220000),
            Song("8", "Coffee Shop Jams", "Lofi Master", "https://picsum.photos/seed/s8/300/300", "", 140000),
            Song("9", "Rainy Nights", "Cozy Waves", "https://picsum.photos/seed/s9/300/300", "", 185000),
            Song("10", "Techno Pulse", "Digital God", "https://picsum.photos/seed/s10/300/300", "", 250000)
        )
    }

    private val auth: FirebaseAuth? by lazy {
        try { FirebaseAuth.getInstance() } catch (e: Throwable) { null }
    }
    private val db: FirebaseFirestore? by lazy {
        try { FirebaseFirestore.getInstance() } catch (e: Throwable) { null }
    }

    suspend fun getFeaturedPlaylists(): List<Playlist> {
        return try {
            val snapshot = db?.collection("playlists")?.get()?.await()
            val list = snapshot?.toObjects(Playlist::class.java) ?: emptyList()
            if (list.isEmpty()) getFallbackPlaylists() else list
        } catch (e: Exception) {
            getFallbackPlaylists()
        }
    }

    private fun getFallbackPlaylists(): List<Playlist> {
        return listOf(
            Playlist("1", "Top Hits India", "https://picsum.photos/seed/p1/300/300", "The biggest hits from India.", masterSongList.take(4)),
            Playlist("2", "Global Top 50", "https://picsum.photos/seed/p2/300/300", "What the world is listening to.", masterSongList.drop(4).take(3)),
            Playlist("3", "Workout Warrior", "https://picsum.photos/seed/p3/300/300", "Get pumped with these beats.", masterSongList.takeLast(3))
        )
    }

    suspend fun getRecentSongs(): List<Song> {
        return try {
            val snapshot = db?.collection("recent_songs")?.get()?.await()
            val list = snapshot?.toObjects(Song::class.java) ?: emptyList()
            if (list.isEmpty()) masterSongList.take(4) else list
        } catch (e: Exception) {
            masterSongList.take(4)
        }
    }

    fun isUserLoggedIn(): Boolean {
        if (isDemoLoggedIn) return true
        return try {
            auth?.currentUser != null
        } catch (e: Exception) {
            false
        }
    }

    suspend fun signInWithEmail(email: String, pass: String) {
        try {
            val a = auth
            if (a != null) {
                a.signInWithEmailAndPassword(email, pass).await()
                syncPlaylists()
            } else {
                throw Exception("Firebase Auth is uninitialized.")
            }
        } catch (e: Exception) {
            isDemoLoggedIn = true // Fallback to local demo session
        }
    }

    suspend fun signUpWithEmail(email: String, pass: String) {
        try {
            val a = auth
            if (a != null) {
                a.createUserWithEmailAndPassword(email, pass).await()
                syncPlaylists()
            } else {
                throw Exception("Firebase Auth is uninitialized.")
            }
        } catch (e: Exception) {
            isDemoLoggedIn = true // Fallback to local demo session
        }
    }

    suspend fun signInWithGoogle(idToken: String) {
        try {
            val a = auth
            if (a != null) {
                a.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
                syncPlaylists()
            } else {
                throw Exception("Firebase Auth is uninitialized.")
            }
        } catch (e: Exception) {
            isDemoLoggedIn = true // Fallback to local demo session
        }
    }

    fun logout() {
        isDemoLoggedIn = false
        try {
            auth?.signOut()
        } catch (e: Exception) {
            // Ignored
        }
        // Clear playlists back to just Liked Songs
        customPlaylists.clear()
        customPlaylists.add(
            Playlist(
                id = "liked",
                name = "Liked Songs",
                coverUrl = "https://picsum.photos/seed/liked/300/300",
                description = "Your favorite tracks",
                songs = likedSongs.toList()
            )
        )
    }

    // Liked Songs / Favorites
    fun toggleLikeSong(song: Song) {
        if (likedSongs.any { it.id == song.id }) {
            likedSongs.removeIf { it.id == song.id }
        } else {
            likedSongs.add(song)
        }
        // Keep Liked Songs playlist in sync
        customPlaylists.replaceAll { playlist ->
            if (playlist.id == "liked") {
                playlist.copy(songs = likedSongs.toList())
            } else {
                playlist
            }
        }
    }

    fun isSongLiked(songId: String): Boolean {
        return likedSongs.any { it.id == songId }
    }

    // Custom Playlists (with Firestore sync support)
    suspend fun syncPlaylists() {
        val dbRef = db ?: return
        val userId = auth?.currentUser?.uid ?: if (isDemoLoggedIn) "demo_user" else return
        try {
            val snapshot = dbRef.collection("user_playlists")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val loadedList = snapshot.documents.mapNotNull { doc ->
                val id = doc.getString("id") ?: ""
                val name = doc.getString("name") ?: ""
                val coverUrl = doc.getString("coverUrl") ?: ""
                val description = doc.getString("description") ?: ""

                val songsListRaw = doc.get("songs") as? List<Map<String, Any>> ?: emptyList()
                val songs = songsListRaw.map { sMap ->
                    Song(
                        id = sMap["id"] as? String ?: "",
                        title = sMap["title"] as? String ?: "",
                        artist = sMap["artist"] as? String ?: "",
                        albumArtUrl = sMap["albumArtUrl"] as? String ?: "",
                        streamUrl = sMap["streamUrl"] as? String ?: "",
                        durationMs = (sMap["durationMs"] as? Number)?.toLong() ?: 0L
                    )
                }
                Playlist(
                    id = id,
                    name = name,
                    coverUrl = coverUrl,
                    description = description,
                    songs = songs
                )
            }

            val likedPlaylist = customPlaylists.find { it.id == "liked" } ?: Playlist(
                id = "liked",
                name = "Liked Songs",
                coverUrl = "https://picsum.photos/seed/liked/300/300",
                description = "Your favorite tracks",
                songs = likedSongs.toList()
            )

            customPlaylists.clear()
            customPlaylists.add(likedPlaylist)
            customPlaylists.addAll(loadedList.filter { it.id != "liked" })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun createPlaylist(name: String, description: String) {
        val id = "custom_${System.currentTimeMillis()}"
        val newPlaylist = Playlist(
            id = id,
            name = name,
            coverUrl = "https://picsum.photos/seed/$id/300/300",
            description = description,
            songs = emptyList()
        )
        customPlaylists.add(newPlaylist)

        // Sync asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            savePlaylistToFirestore(newPlaylist)
        }
    }

    fun deletePlaylist(playlistId: String) {
        customPlaylists.removeIf { it.id == playlistId }
        CoroutineScope(Dispatchers.IO).launch {
            deletePlaylistFromFirestore(playlistId)
        }
    }

    fun addSongToPlaylist(playlistId: String, song: Song) {
        var updatedPlaylist: Playlist? = null
        customPlaylists.replaceAll { playlist ->
            if (playlist.id == playlistId) {
                if (playlist.songs.any { it.id == song.id }) {
                    playlist
                } else {
                    val p = playlist.copy(songs = playlist.songs + song)
                    updatedPlaylist = p
                    p
                }
            } else {
                playlist
            }
        }
        updatedPlaylist?.let { p ->
            CoroutineScope(Dispatchers.IO).launch {
                savePlaylistToFirestore(p)
            }
        }
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        var updatedPlaylist: Playlist? = null
        customPlaylists.replaceAll { playlist ->
            if (playlist.id == playlistId) {
                val p = playlist.copy(songs = playlist.songs.filter { it.id != songId })
                updatedPlaylist = p
                p
            } else {
                playlist
            }
        }
        updatedPlaylist?.let { p ->
            CoroutineScope(Dispatchers.IO).launch {
                savePlaylistToFirestore(p)
            }
        }
    }

    private suspend fun savePlaylistToFirestore(playlist: Playlist) {
        val dbRef = db ?: return
        val userId = auth?.currentUser?.uid ?: if (isDemoLoggedIn) "demo_user" else "anonymous_user"
        val data = hashMapOf(
            "id" to playlist.id,
            "name" to playlist.name,
            "coverUrl" to playlist.coverUrl,
            "description" to playlist.description,
            "userId" to userId,
            "songs" to playlist.songs.map { song ->
                hashMapOf(
                    "id" to song.id,
                    "title" to song.title,
                    "artist" to song.artist,
                    "albumArtUrl" to song.albumArtUrl,
                    "streamUrl" to song.streamUrl,
                    "durationMs" to song.durationMs
                )
            },
            "updatedAt" to System.currentTimeMillis()
        )
        try {
            dbRef.collection("user_playlists").document(playlist.id).set(data).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun deletePlaylistFromFirestore(playlistId: String) {
        val dbRef = db ?: return
        try {
            dbRef.collection("user_playlists").document(playlistId).delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
