package com.example.data

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.model.User
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

object GoogleAuthHelper {
    private const val TAG = "GoogleAuthHelper"
    // Web client ID from firebase-applet-config.json
    private const val WEB_CLIENT_ID = "684033752161-rq07s0hfajbjfp7f53nq1a3ad8jdfk65.apps.googleusercontent.com"

    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .apply {
                try {
                    requestIdToken(WEB_CLIENT_ID)
                } catch (e: Throwable) {
                    Log.w(TAG, "Could not set requestIdToken: ${e.message}")
                }
            }
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun getSignInIntent(context: Context): Intent {
        return getGoogleSignInClient(context).signInIntent
    }

    fun parseSignInResult(data: Intent?): Result<User> {
        if (data == null) {
            return Result.failure(IllegalStateException("No Google Sign-In intent data received."))
        }
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        return try {
            val account = task.getResult(ApiException::class.java)
            if (account != null && !account.email.isNullOrBlank()) {
                val email = account.email!!
                val name = account.displayName ?: email.substringBefore("@")
                val photoUrl = account.photoUrl?.toString() ?: "https://picsum.photos/seed/$email/200/200"

                val user = AuthManager.signInWithGoogleAccount(
                    email = email,
                    name = name,
                    avatarUrl = photoUrl
                )
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("Could not retrieve Google account information."))
            }
        } catch (e: ApiException) {
            Log.e(TAG, "Google Sign-In failed with status code ${e.statusCode}: ${e.message}")
            Result.failure(e)
        } catch (e: Throwable) {
            Log.e(TAG, "Unexpected error during Google Sign-In", e)
            Result.failure(e)
        }
    }
}
