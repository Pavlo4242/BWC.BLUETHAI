package com.bwc.bluethai.data.local

import androidx.room.*
import androidx.sqlite.db.SimpleSQLiteQuery
import com.bwc.bluethai.data.model.ConversationSession
import com.bwc.bluethai.data.model.LogEntry
import com.bwc.bluethai.data.model.SessionWithPreview
import com.bwc.bluethai.data.model.TranslationEntry
import kotlinx.coroutines.flow.Flow

// Import the table name constants for consistency
import com.bwc.bluethai.data.model.TABLE_SESSIONS
import com.bwc.bluethai.data.model.TABLE_ENTRIES // Crucial for correct table name
import com.bwc.bluethai.data.model.TABLE_LOGS // Also good to import if needed for pragmas


@Dao
interface SessionsDao : BaseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ConversationSession): Long

    @Transaction
    @Query("""
        SELECT s.id, s.startTime, (
            SELECT COALESCE(
                CASE WHEN e.isFromEnglish THEN e.englishText ELSE e.thaiText END,
                'No messages'
            )
            FROM $TABLE_ENTRIES e 
            WHERE e.sessionId = s.id
            ORDER BY e.timestamp ASC
            LIMIT 1
        ) as previewText
        FROM $TABLE_SESSIONS s 
        ORDER BY s.startTime DESC
    """)
    fun getSessionsWithPreviews(): Flow<List<SessionWithPreview>>

    @Transaction
    suspend fun deleteSessionAndEntries(sessionId: Long) {
        deleteEntriesForSession(sessionId)
        deleteSessionById(sessionId)
    }

    @Query("DELETE FROM $TABLE_SESSIONS WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)

    @Query("DELETE FROM $TABLE_ENTRIES WHERE sessionId = :sessionId")
    suspend fun deleteEntriesForSession(sessionId: Long)

    // Fixed schema query approach
    @RawQuery
    override suspend fun getTableSchemaRaw(query: SimpleSQLiteQuery): List<TableInfo>

    suspend fun getSessionTableSchema(): List<TableInfo> {
        return getTableSchemaRaw(SimpleSQLiteQuery("PRAGMA table_info($TABLE_SESSIONS)"))
    }

    @Query("SELECT * FROM $TABLE_SESSIONS ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<ConversationSession>>

    @Query("SELECT COUNT(*) FROM $TABLE_SESSIONS")
    suspend fun getSessionCount(): Int
}

@Dao
interface EntryDao : BaseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: TranslationEntry)

    @Query("SELECT * FROM $TABLE_ENTRIES WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getEntriesForSession(sessionId: Long): Flow<List<TranslationEntry>>

    @Query("DELETE FROM $TABLE_ENTRIES WHERE sessionId = :sessionId")
    suspend fun deleteEntriesForSession(sessionId: Long)

    // Fixed schema query
    suspend fun getEntryTableSchema(): List<TableInfo> {
        return getTableSchemaRaw(SimpleSQLiteQuery("PRAGMA table_info($TABLE_ENTRIES)"))
    }

    @Query("SELECT COUNT(*) FROM $TABLE_ENTRIES")
    suspend fun getEntryCount(): Int
}

@Dao
interface LogDao {
    @Insert
    suspend fun insert(log: LogEntry)

    @Query("SELECT * FROM $TABLE_LOGS ORDER BY timestamp DESC")
    suspend fun getAll(): List<LogEntry>

    @Query("DELETE FROM $TABLE_LOGS")
    suspend fun clearAll()
}

// Shared RawQuery function (add this to your Database class)
@Dao
interface BaseDao {
    @RawQuery
    suspend fun getTableSchemaRaw(query: SimpleSQLiteQuery): List<TableInfo>
}

// Your TableInfo data class remains the same
data class TableInfo(
    val cid: Int,
    val name: String,
    val type: String,
    val notnull: Int,
    val dflt_value: String?,
    val pk: Int
)