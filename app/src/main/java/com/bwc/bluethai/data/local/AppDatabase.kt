package com.bwc.bluethai.data.local

import com.bwc.bluethai.data.local.AppDatabase
import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.bwc.bluethai.data.model.ConversationSession
import com.bwc.bluethai.data.model.LogEntry
import com.bwc.bluethai.data.model.TranslationEntry

@Database(
    entities = [
        ConversationSession::class,
        TranslationEntry::class,
                LogEntry::class,

    ],
    version = 4,  // Roll back version since we're removing WebSocket tables
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionsDao
    abstract fun entryDao(): EntryDao
    abstract fun logDao(): LogDao

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
                    .fallbackToDestructiveMigration()
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
// Initialize with default data if needed
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }


        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE logs (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, timestamp INTEGER NOT NULL, level TEXT NOT NULL, tag TEXT NOT NULL, message TEXT NOT NULL)"
                )
            }
        }
    }
}