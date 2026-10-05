package com.example.network

import com.example.model.Song
import com.example.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest

object LastFmScrobbler {
    private const val TAG = "LastFmScrobbler"
    private const val API_KEY = "f86be2e684074ee2820a40d5885c5b5c"
    private const val API_SECRET = "db9d7a2283e74be0a1690ccb87201b1b"

    private val client = OkHttpClient()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    var sessionKey: String? = null // Can be configured from settings screen

    fun updateNowPlaying(song: Song) {
        val sk = sessionKey ?: return
        scope.launch {
            try {
                val params = mutableMapOf(
                    "method" to "track.updateNowPlaying",
                    "artist" to song.artist,
                    "track" to song.title,
                    "api_key" to API_KEY,
                    "sk" to sk
                )
                params["api_sig"] = generateSignature(params)

                val bodyBuilder = FormBody.Builder()
                params.forEach { (k, v) -> bodyBuilder.add(k, v) }

                val request = Request.Builder()
                    .url("https://ws.audioscrobbler.com/2.0/")
                    .post(bodyBuilder.build())
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        AppLogger.i(TAG, "Successfully updated now playing on Last.fm: ${song.title}")
                    } else {
                        AppLogger.w(TAG, "Failed updating Last.fm now playing: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Last.fm NowPlaying failed: ${e.message}")
            }
        }
    }

    fun scrobble(song: Song) {
        val sk = sessionKey ?: return
        scope.launch {
            try {
                val timestamp = (System.currentTimeMillis() / 1000L).toString()
                val params = mutableMapOf(
                    "method" to "track.scrobble",
                    "artist" to song.artist,
                    "track" to song.title,
                    "timestamp" to timestamp,
                    "api_key" to API_KEY,
                    "sk" to sk
                )
                params["api_sig"] = generateSignature(params)

                val bodyBuilder = FormBody.Builder()
                params.forEach { (k, v) -> bodyBuilder.add(k, v) }

                val request = Request.Builder()
                    .url("https://ws.audioscrobbler.com/2.0/")
                    .post(bodyBuilder.build())
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        AppLogger.i(TAG, "Successfully scrobbled track to Last.fm: ${song.title}")
                    } else {
                        AppLogger.w(TAG, "Failed to scrobble to Last.fm: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Last.fm Scrobble failed: ${e.message}")
            }
        }
    }

    private fun generateSignature(params: Map<String, String>): String {
        val sorted = params.toSortedMap()
        val signatureBuilder = StringBuilder()
        sorted.forEach { (k, v) ->
            signatureBuilder.append(k).append(v)
        }
        signatureBuilder.append(API_SECRET)
        return md5(signatureBuilder.toString())
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val bytes = md.digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
