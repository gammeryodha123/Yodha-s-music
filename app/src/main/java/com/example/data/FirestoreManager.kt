package com.example.data

import com.example.model.Playlist
import com.example.model.Song
import com.example.model.User
import com.example.util.AppLogger
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

object FirestoreManager {
    private const val TAG = "FirestoreManager"

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Throwable) {
            AppLogger.e(TAG, "FirebaseFirestore initialization error: ${e.message}")
            null
        }
    }

    fun saveUserProfile(user: User) {
        val db = firestore ?: return
        try {
            val userMap = hashMapOf(
                "id" to user.id,
                "name" to user.name,
                "email" to user.email,
                "avatarUrl" to user.avatarUrl,
                "plan" to user.plan,
                "authProvider" to user.authProvider,
                "updatedAt" to System.currentTimeMillis()
            )
            db.collection("users")
                .document(user.id)
                .set(userMap, SetOptions.merge())
                .addOnSuccessListener {
                    AppLogger.i(TAG, "User profile saved to Firestore for ${user.email}")
                }
                .addOnFailureListener { e ->
                    AppLogger.w(TAG, "Failed saving user profile to Firestore: ${e.message}")
                }
        } catch (e: Throwable) {
            AppLogger.e(TAG, "Firestore error during saveUserProfile: ${e.message}")
        }
    }

    fun syncLikedSong(userId: String, song: Song, isLiked: Boolean) {
        val db = firestore ?: return
        try {
            val songRef = db.collection("users")
                .document(userId)
                .collection("liked_songs")
                .document(song.id)

            if (isLiked) {
                val songData = hashMapOf(
                    "id" to song.id,
                    "title" to song.title,
                    "artist" to song.artist,
                    "albumArtUrl" to song.albumArtUrl,
                    "streamUrl" to song.streamUrl,
                    "genre" to song.genre,
                    "album" to song.album,
                    "likedAt" to System.currentTimeMillis()
                )
                songRef.set(songData, SetOptions.merge())
            } else {
                songRef.delete()
            }
        } catch (e: Throwable) {
            AppLogger.e(TAG, "Firestore syncLikedSong error: ${e.message}")
        }
    }

    fun syncPlaylist(userId: String, playlist: Playlist) {
        val db = firestore ?: return
        try {
            val plData = hashMapOf(
                "id" to playlist.id,
                "name" to playlist.name,
                "description" to playlist.description,
                "coverUrl" to playlist.coverUrl,
                "songCount" to playlist.songs.size,
                "updatedAt" to System.currentTimeMillis()
            )
            db.collection("users")
                .document(userId)
                .collection("playlists")
                .document(playlist.id)
                .set(plData, SetOptions.merge())
        } catch (e: Throwable) {
            AppLogger.e(TAG, "Firestore syncPlaylist error: ${e.message}")
        }
    }

    fun recordRecentlyPlayed(userId: String, song: Song) {
        val db = firestore ?: return
        try {
            val recentData = hashMapOf(
                "id" to song.id,
                "title" to song.title,
                "artist" to song.artist,
                "albumArtUrl" to song.albumArtUrl,
                "streamUrl" to song.streamUrl,
                "playedAt" to System.currentTimeMillis()
            )
            db.collection("users")
                .document(userId)
                .collection("recent_plays")
                .document(song.id)
                .set(recentData, SetOptions.merge())
        } catch (e: Throwable) {
            AppLogger.e(TAG, "Firestore recordRecentlyPlayed error: ${e.message}")
        }
    }
}
