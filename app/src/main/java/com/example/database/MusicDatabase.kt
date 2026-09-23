package com.example.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [LocalSongEntity::class, LocalPlaylistEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(RoomTypeConverters::class)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun localSongDao(): LocalSongDao
    abstract fun localPlaylistDao(): LocalPlaylistDao

    companion object {
        @Volatile
        private var INSTANCE: MusicDatabase? = null

        fun getDatabase(context: Context): MusicDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MusicDatabase::class.java,
                    "music_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

object AppDatabaseHelper {
    private var applicationContext: Context? = null

    fun init(context: Context) {
        applicationContext = context.applicationContext
    }

    val database: MusicDatabase by lazy {
        val context = applicationContext ?: throw IllegalStateException("AppDatabaseHelper is not initialized.")
        MusicDatabase.getDatabase(context)
    }
}
