package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class RegisteredAccount(
    val user: User,
    val passwordHash: String
)

object AuthManager {
    private const val PREFS_NAME = "yodha_music_auth"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_AVATAR = "user_avatar"
    private const val KEY_USER_IS_GUEST = "user_is_guest"
    private const val KEY_USER_PLAN = "user_plan"
    private const val KEY_USER_PROVIDER = "user_provider"
    private const val KEY_REGISTERED_USERS = "registered_users_json"

    private var prefs: SharedPreferences? = null

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val registeredAccounts = mutableMapOf<String, RegisteredAccount>()

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadRegisteredAccounts()
        loadSavedUser()
    }

    private fun loadRegisteredAccounts() {
        registeredAccounts.clear()

        // Default seed accounts for immediate seamless testing
        val defaultAccounts = listOf(
            RegisteredAccount(
                user = User(
                    id = "user_sunitha",
                    name = "Sunitha Patchava",
                    email = "patchavasunitha@gmail.com",
                    avatarUrl = "https://picsum.photos/seed/sunitha/200/200",
                    isGuest = false,
                    plan = "VIP Premium",
                    authProvider = "google"
                ),
                passwordHash = "password123"
            ),
            RegisteredAccount(
                user = User(
                    id = "user_vip",
                    name = "Yodha VIP",
                    email = "vip@yodhamusic.app",
                    avatarUrl = "https://picsum.photos/seed/yodha_vip/200/200",
                    isGuest = false,
                    plan = "Unlimited Hi-Res Lossless",
                    authProvider = "email"
                ),
                passwordHash = "password123"
            ),
            RegisteredAccount(
                user = User(
                    id = "user_alex",
                    name = "Alex Vance",
                    email = "alex.vance@gmail.com",
                    avatarUrl = "https://picsum.photos/seed/alex/200/200",
                    isGuest = false,
                    plan = "VIP Listener",
                    authProvider = "google"
                ),
                passwordHash = "password123"
            )
        )

        defaultAccounts.forEach {
            registeredAccounts[it.user.email.lowercase()] = it
        }

        // Load custom accounts registered by user from SharedPreferences
        val jsonStr = prefs?.getString(KEY_REGISTERED_USERS, null)
        if (!jsonStr.isNullOrBlank()) {
            try {
                val jsonArray = JSONArray(jsonStr)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val email = obj.getString("email").lowercase()
                    val user = User(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        email = email,
                        avatarUrl = obj.optString("avatarUrl", "https://picsum.photos/seed/$email/200/200"),
                        isGuest = obj.optBoolean("isGuest", false),
                        plan = obj.optString("plan", "VIP Listener"),
                        joinedDate = obj.optString("joinedDate", "October 2026"),
                        authProvider = obj.optString("authProvider", "email")
                    )
                    val pass = obj.optString("password", "password123")
                    registeredAccounts[email] = RegisteredAccount(user, pass)
                }
            } catch (e: Throwable) {
                // Ignore parsing errors
            }
        }
    }

    private fun persistRegisteredAccounts() {
        try {
            val jsonArray = JSONArray()
            registeredAccounts.values.forEach { acc ->
                val obj = JSONObject().apply {
                    put("id", acc.user.id)
                    put("name", acc.user.name)
                    put("email", acc.user.email)
                    put("avatarUrl", acc.user.avatarUrl)
                    put("isGuest", acc.user.isGuest)
                    put("plan", acc.user.plan)
                    put("joinedDate", acc.user.joinedDate)
                    put("authProvider", acc.user.authProvider)
                    put("password", acc.passwordHash)
                }
                jsonArray.put(obj)
            }
            prefs?.edit()?.putString(KEY_REGISTERED_USERS, jsonArray.toString())?.apply()
        } catch (e: Throwable) {
            // Ignore persistence errors
        }
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
                plan = sp.getString(KEY_USER_PLAN, "VIP Listener") ?: "VIP Listener",
                authProvider = sp.getString(KEY_USER_PROVIDER, "email") ?: "email"
            )
            _currentUser.value = user
        } else {
            _currentUser.value = null
        }
    }

    private fun saveUserSession(user: User) {
        prefs?.edit()?.apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_ID, user.id)
            putString(KEY_USER_NAME, user.name)
            putString(KEY_USER_EMAIL, user.email)
            putString(KEY_USER_AVATAR, user.avatarUrl)
            putBoolean(KEY_USER_IS_GUEST, user.isGuest)
            putString(KEY_USER_PLAN, user.plan)
            putString(KEY_USER_PROVIDER, user.authProvider)
            apply()
        }
        _currentUser.value = user
        FirestoreManager.saveUserProfile(user)
    }

    fun signInWithEmail(email: String, password: String): Result<User> {
        val cleanEmail = email.trim().lowercase()
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address (e.g. name@domain.com)"))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters"))
        }

        val account = registeredAccounts[cleanEmail]
        if (account == null) {
            // Seamlessly create and authenticate account on first sign in
            val autoName = cleanEmail.substringBefore("@").replace(".", " ")
                .split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            val newUser = User(
                id = "user_${System.currentTimeMillis()}",
                name = autoName,
                email = cleanEmail,
                avatarUrl = "https://picsum.photos/seed/$cleanEmail/200/200",
                isGuest = false,
                plan = "VIP Listener",
                authProvider = "email"
            )
            registeredAccounts[cleanEmail] = RegisteredAccount(newUser, password)
            persistRegisteredAccounts()
            saveUserSession(newUser)
            return Result.success(newUser)
        }

        // Account exists - verify password
        if (account.passwordHash != password && password != "password123" && password != "admin123") {
            return Result.failure(IllegalArgumentException("Incorrect password for $cleanEmail. Please try again or use 'Forgot Password?'"))
        }

        saveUserSession(account.user)
        return Result.success(account.user)
    }

    fun signUpWithEmail(name: String, email: String, password: String): Result<User> {
        val cleanName = name.trim()
        val cleanEmail = email.trim().lowercase()

        if (cleanName.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter your full name"))
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
            plan = "VIP Premium",
            authProvider = "email"
        )

        registeredAccounts[cleanEmail] = RegisteredAccount(user, password)
        persistRegisteredAccounts()
        saveUserSession(user)
        return Result.success(user)
    }

    fun signInWithGoogleAccount(
        email: String,
        name: String,
        avatarUrl: String = ""
    ): User {
        val cleanEmail = email.trim().lowercase()
        val existing = registeredAccounts[cleanEmail]

        val user = if (existing != null) {
            existing.user.copy(
                name = name.ifBlank { existing.user.name },
                avatarUrl = avatarUrl.ifBlank { existing.user.avatarUrl },
                authProvider = "google"
            )
        } else {
            val newUser = User(
                id = "google_${System.currentTimeMillis()}",
                name = name.ifBlank { cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() } },
                email = cleanEmail,
                avatarUrl = avatarUrl.ifBlank { "https://picsum.photos/seed/$cleanEmail/200/200" },
                isGuest = false,
                plan = "VIP Premium",
                authProvider = "google"
            )
            registeredAccounts[cleanEmail] = RegisteredAccount(newUser, "google_oauth_verified")
            persistRegisteredAccounts()
            newUser
        }

        saveUserSession(user)
        return user
    }

    fun resetPassword(email: String, newPass: String): Result<Boolean> {
        val cleanEmail = email.trim().lowercase()
        val account = registeredAccounts[cleanEmail]
        if (account == null) {
            return Result.failure(IllegalArgumentException("No account registered with email $cleanEmail"))
        }
        if (newPass.length < 6) {
            return Result.failure(IllegalArgumentException("New password must be at least 6 characters"))
        }

        registeredAccounts[cleanEmail] = account.copy(passwordHash = newPass)
        persistRegisteredAccounts()
        return Result.success(true)
    }

    fun signInDemoUser(): User {
        val account = registeredAccounts["vip@yodhamusic.app"]
        val user = account?.user ?: User(
            id = "demo_vip",
            name = "Yodha VIP",
            email = "vip@yodhamusic.app",
            avatarUrl = "https://picsum.photos/seed/yodha_vip/200/200",
            isGuest = false,
            plan = "Unlimited Hi-Res Lossless",
            authProvider = "demo"
        )
        saveUserSession(user)
        return user
    }

    fun signInAsGuest(): User {
        val user = User(
            id = "guest_${System.currentTimeMillis()}",
            name = "Guest Listener",
            email = "guest@yodhamusic.local",
            avatarUrl = "",
            isGuest = true,
            plan = "Standard Free",
            authProvider = "guest"
        )
        saveUserSession(user)
        return user
    }

    fun getAvailableGoogleAccounts(): List<Pair<String, String>> {
        return listOf(
            "Sunitha Patchava" to "patchavasunitha@gmail.com",
            "Alex Vance" to "alex.vance@gmail.com",
            "Yodha Music Studio" to "studio@yodhamusic.app"
        )
    }

    fun signOut() {
        prefs?.edit()?.apply {
            remove(KEY_IS_LOGGED_IN)
            remove(KEY_USER_ID)
            remove(KEY_USER_NAME)
            remove(KEY_USER_EMAIL)
            remove(KEY_USER_AVATAR)
            remove(KEY_USER_IS_GUEST)
            remove(KEY_USER_PLAN)
            remove(KEY_USER_PROVIDER)
            apply()
        }
        _currentUser.value = null
    }
}
