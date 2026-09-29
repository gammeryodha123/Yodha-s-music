package com.example.data

import android.content.Context
import com.example.database.MusicDatabase

object AppDatabaseHelper {
    private var applicationContext: Context? = null

    fun init(context: Context) {
        applicationContext = context.applicationContext
    }

    val context: Context?
        get() = applicationContext

    val database: MusicDatabase by lazy {
        val ctx = applicationContext ?: throw IllegalStateException("AppDatabaseHelper is not initialized.")
        MusicDatabase.getDatabase(ctx)
    }
}
