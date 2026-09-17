package com.example.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.example.model.Playlist
import com.example.model.Song
import kotlinx.coroutines.tasks.await

class MusicRepository {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    suspend fun getFeaturedPlaylists(): List<Playlist> {
        return try {
            val snapshot = db.collection("playlists").get().await()
            snapshot.toObjects(Playlist::class.java)
        } catch (e: Exception) {
            // Fallback for development if Firestore is not populated
            emptyList()
        }
    }

    suspend fun getRecentSongs(): List<Song> {
        return try {
            val snapshot = db.collection("recent_songs").get().await()
            snapshot.toObjects(Song::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    suspend fun signInWithEmail(email: String, pass: String): Boolean {
        return try {
            auth.signInWithEmailAndPassword(email, pass).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun signUpWithEmail(email: String, pass: String): Boolean {
        return try {
            auth.createUserWithEmailAndPassword(email, pass).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun signInWithGoogle(idToken: String): Boolean {
        return try {
            auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
