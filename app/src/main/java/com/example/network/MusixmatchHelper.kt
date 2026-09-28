package com.example.network

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import java.net.URLEncoder

object MusixmatchHelper {
    const val PACKAGE_NAME = "com.musixmatch.android.lyrify"
    const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.musixmatch.android.lyrify"
    const val WEB_SEARCH_URL = "https://www.musixmatch.com/search/"

    fun isAppInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(PACKAGE_NAME, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun launchMusixmatch(context: Context, trackName: String, artistName: String? = null) {
        val query = if (!artistName.isNullOrBlank()) "$trackName $artistName" else trackName
        val encodedQuery = try {
            URLEncoder.encode(query, "UTF-8")
        } catch (e: Exception) {
            query
        }

        if (isAppInstalled(context)) {
            try {
                val intent = context.packageManager.getLaunchIntentForPackage(PACKAGE_NAME)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return
                }
            } catch (e: Exception) {
                // Fallback to web search
            }
        }

        // Open Musixmatch on the web or Play Store
        try {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("$WEB_SEARCH_URL$encodedQuery"))
            webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(webIntent)
        } catch (e: Exception) {
            openPlayStore(context)
        }
    }

    fun openPlayStore(context: Context) {
        try {
            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$PACKAGE_NAME"))
            marketIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(marketIntent)
        } catch (e: Exception) {
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_URL))
                webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(webIntent)
            } catch (e2: Exception) {
                Toast.makeText(context, "Unable to open Google Play Store", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
