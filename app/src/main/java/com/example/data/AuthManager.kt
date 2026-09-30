package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AuthManager {
    private const val PREFS_NAME = "yodha_music_auth"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_AVATAR = "user_avatar"
    private const val KEY_USER_IS_GUEST = "user_is_guest"
    private const val KEY_USER_PLAN = "user_plan"

    private var prefs: SharedPreferences? = null

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadSavedUser()
    }

    private fun loadSavedUser() {
        val sp = prefs ?: return
        val isLoggedIn = sp.getBoolean(KEY_IS_LOGGED_IN, false)
        if (isLoggedIn) {
            val user = User(
                id = sp.getString(KEY_USER_ID, "u1") ?: "u1",
                name = sp.getString(KEY_USER_NAME, "Music Fan") ?: "Music Fan",
                email = sp.getString(KEY_USER_EMAIL, "user@yodhamusic.com") ?: "user@yodhamusic.com",
                avatarUrl = sp.getString(KEY_USER_AVATAR, "https://picsum.photos/seed/user_avatar/200/200") ?: "",
                isGuest = sp.getBoolean(KEY_USER_IS_GUEST, false),
                plan = sp.getString(KEY_USER_PLAN, "VIP Listener") ?: "VIP Listener"
            )
            _currentUser.value = user
        } else {
            _currentUser.value = null
        }
    }

    private fun saveUserToPrefs(user: User) {
        prefs?.edit()?.apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_ID, user.id)
            putString(KEY_USER_NAME, user.name)
            putString(KEY_USER_EMAIL, user.email)
            putString(KEY_USER_AVATAR, user.avatarUrl)
            putBoolean(KEY_USER_IS_GUEST, user.isGuest)
            putString(KEY_USER_PLAN, user.plan)
            apply()
        }
        _currentUser.value = user
    }

    fun signInWithEmail(email: String, password: String):Result<User> {
        val cleanEmail = email.trim()
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address"))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters"))
        }

        val name = cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
        val user = User(
            id = "user_${System.currentTimeMillis()}",
            name = name,
            email = cleanEmail,
            avatarUrl = "https://picsum.photos/seed/$cleanEmail/200/200",
            isGuest = false,
            plan = "VIP Listener"
        )
        saveUserToPrefs(user)
        return Result.success(user)
    }

    fun signUpWithEmail(name: String, email: String, password: String): Result<User> {
        val cleanName = name.trim()
        val cleanEmail = email.trim()
        if (cleanName.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter your name"))
        }
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address"))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters"))
        }

        val user = User(
            id = "user_${System.currentTimeMillis()}",
            name = cleanName,
            email = cleanEmail,
            avatarUrl = "https://picsum.photos/seed/$cleanEmail/200/200",
            isGuest = false,
            plan = "VIP Listener"
        )
        saveUserToPrefs(user)
        return Result.success(user)
    }

    fun signInWithGoogle(): User {
        val user = User(
            id = "google_${System.currentTimeMillis()}",
            name = "Alex Vance",
            email = "alex.vance@gmail.com",
            avatarUrl = "https://picsum.photos/seed/google_user/200/200",
            isGuest = false,
            plan = "VIP Listener"
        )
        saveUserToPrefs(user)
        return user
    }

    fun signInDemoUser(): User {
        val user = User(
            id = "demo_vip",
            name = "Yodha VIP",
            email = "vip@yodhamusic.app",
            avatarUrl = "https://picsum.photos/seed/yodha_vip/200/200",
            isGuest = false,
            plan = "Unlimited Hi-Res Lossless"
        )
        saveUserToPrefs(user)
        return user
    }

    fun signInAsGuest(): User {
        val user = User(
            id = "guest_${System.currentTimeMillis()}",
            name = "Guest Listener",
            email = "guest@yodhamusic.local",
            avatarUrl = "",
            isGuest = true,
            plan = "Standard Free"
        )
        saveUserToPrefs(user)
        return user
    }

    fun signOut() {
        prefs?.edit()?.clear()?.apply()
        _currentUser.value = null
    }
}
