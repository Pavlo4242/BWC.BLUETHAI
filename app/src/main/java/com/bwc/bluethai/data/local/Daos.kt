package com.bwc.bluethai.data.local

import androidx.room.*
import com.bwc.bluethai.data.model.ConversationSession
import com.bwc.bluethai.data.model.SessionWithPreview
import com.bwc.bluethai.data.model.TranslationEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ConversationSession): Long

    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<ConversationSession>>

    @Transaction
    @Query("""
        SELECT s.id, s.startTime, (
            SELECT COALESCE(
                CASE WHEN e.isFromEnglish THEN e.englishText ELSE e.thaiText END,
                'No messages'
            )
            FROM entries e 
            WHERE e.sessionId = s.id 
            ORDER BY e.timestamp ASC 
            LIMIT 1
        ) as previewText
        FROM sessions s
        ORDER BY s.startTime DESC
    """)
    fun getSessionsWithPreviews(): Flow<List<SessionWithPreview>>

    @Transaction
    suspend fun deleteSessionAndEntries(sessionId: Long) {
        deleteEntriesForSession(sessionId)
        deleteSessionById(sessionId)
    }

    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)

    @Query("DELETE FROM entries WHERE sessionId = :sessionId")
    suspend fun deleteEntriesForSession(sessionId: Long)
}

@Dao
interface EntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: TranslationEntry)

    @Query("SELECT * FROM entries WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getEntriesForSession(sessionId: Long): Flow<List<TranslationEntry>>

    @Query("DELETE FROM entries WHERE sessionId = :sessionId")
    suspend fun deleteEntriesForSession(sessionId: Long)
}