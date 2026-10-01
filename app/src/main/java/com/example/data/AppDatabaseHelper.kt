package com.example.data

import android.content.Context
import com.example.database.MusicDatabase

object AppDatabaseHelper {
    @Volatile
    private var applicationContext: Context? = null

    fun init(context: Context) {
        if (applicationContext == null) {
            applicationContext = context.applicationContext
        }
    }

    val context: Context?
        get() = applicationContext

    val database: MusicDatabase?
        get() {
            val ctx = applicationContext ?: return null
            return MusicDatabase.getDatabase(ctx)
        }
}
