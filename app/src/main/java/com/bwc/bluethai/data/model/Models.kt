package com.bwc.bluethai.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import androidx.room.ForeignKey
import androidx.room.Index


@Entity(
    tableName = "sessions",
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

@Entity(
    tableName = "entries",
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["timestamp"])
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