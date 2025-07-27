package com.bwc.bluethai.data.local


import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.bwc.bluethai.data.model.ConversationSession
import com.bwc.bluethai.data.model.TranslationEntry

@Database(
    entities = [
        ConversationSession::class,
        TranslationEntry::class
    ],
    version = 2,  // Roll back version since we're removing WebSocket tables
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun entryDao(): EntryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "translator_database"
                )
                    .fallbackToDestructiveMigration() // Add this for now to handle version change
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}