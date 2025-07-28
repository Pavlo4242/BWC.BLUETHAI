package com.bwc.bluethai.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import androidx.room.ForeignKey
import androidx.room.Index

const val TABLE_SESSIONS = "sessions"
const val TABLE_ENTRIES = "entries"
const val TABLE_LOGS = "logs" // Added for consistency

@Entity(
    tableName = TABLE_ENTRIES,
    indices = [
        Index(value = ["sessionId"], name = "idx_entries_session_id"),
        Index(value = ["timestamp"], name = "idx_entries_timestamp"),
        Index(value = ["id"], unique = true, name = "idx_entries_id")
    ],
    foreignKeys = [ForeignKey(
        entity = ConversationSession::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class TranslationEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Long,
    val englishText: String,
    val thaiText: String,
    val timestamp: Date = Date(),
    val isFromEnglish: Boolean
)

@Entity(
    tableName = TABLE_SESSIONS, // Changed to use constant
    indices = [Index(value = ["startTime"])]
)
data class ConversationSession(
    @PrimaryKey val id: Long = System.currentTimeMillis(),
    val startTime: Date = Date()
)
data class SessionWithPreview(
    val id: Long,
    val startTime: Date,
    val previewText: String?
)

data class SessionPreview(
    val session: ConversationSession,
    val previewText: String
)


// This @Entity block has been moved to correctly annotate the LogEntry class
// The previous, incorrect annotation has been removed.
@Entity(
    tableName = TABLE_LOGS,
    indices = [
        Index(value = ["timestamp"]),
    ]
)
data class LogEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val level: String, // e.g., "INFO", "ERROR", "NETWORK"
    val tag: String,
    val message: String
)