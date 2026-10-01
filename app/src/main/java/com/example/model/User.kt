package com.example.model

data class User(
    val id: String,
    val name: String,
    val email: String,
    val avatarUrl: String = "",
    val isGuest: Boolean = false,
    val plan: String = "VIP Listener",
    val joinedDate: String = "September 2026",
    val authProvider: String = "email"
)
