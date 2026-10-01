package com.example.util

object AppLogger {
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        try {
            android.util.Log.e(tag, message, throwable)
        } catch (e: Throwable) {
            println("[$tag ERROR] $message ${throwable?.message ?: ""}")
        }
    }

    fun w(tag: String, message: String) {
        try {
            android.util.Log.w(tag, message)
        } catch (e: Throwable) {
            println("[$tag WARN] $message")
        }
    }

    fun i(tag: String, message: String) {
        try {
            android.util.Log.i(tag, message)
        } catch (e: Throwable) {
            println("[$tag INFO] $message")
        }
    }
}
