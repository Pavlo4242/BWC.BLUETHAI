package com.bwc.bluethai.data.websocket

import android.content.Context
import androidx.room.*
import com.bwc.bluethai.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import okio.Bytestring
import java.io.File
import java.util.*

@Entity(tableName = "websocket_logs")
data class WebSocketLogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "direction") val direction: Direction,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "message") val message: String,
    @ColumnInfo(name = "is_error") val isError: Boolean = false,
    @ColumnInfo(name = "error_message") val errorMessage: String? = null
) {
    enum class Direction { SENT, RECEIVED }
}

@Dao
interface WebSocketLogDao {
    @Insert
    suspend fun insert(logEntry: WebSocketLogEntry)

    @Query("SELECT * FROM websocket_logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentLogs(limit: Int = 200): List<WebSocketLogEntry>

    @Query("SELECT * FROM websocket_logs ORDER BY timestamp DESC")
    suspend fun getAllLogs(): List<WebSocketLogEntry>

    @Query("DELETE FROM websocket_logs WHERE timestamp < :cutoffTime")
    suspend fun deleteOldLogs(cutoffTime: Long)
}

class WebSocketLogger(private val context: Context) {
    private val dao = AppDatabase.getDatabase(context).webSocketLogDao()

    suspend fun logSentMessage(url: String, message: String) = withContext(Dispatchers.IO) {
        dao.insert(WebSocketLogEntry(
            direction = WebSocketLogEntry.Direction.SENT,
            url = url,
            message = message
        ))
    }

    suspend fun logReceivedMessage(url: String, message: String) = withContext(Dispatchers.IO) {
        if (!isAudioMessage(message)) {
            dao.insert(WebSocketLogEntry(
                direction = WebSocketLogEntry.Direction.RECEIVED,
                url = url,
                message = message
            ))
        }
    }

    suspend fun logError(url: String, error: String, message: String? = null) = withContext(Dispatchers.IO) {
        dao.insert(WebSocketLogEntry(
            direction = WebSocketLogEntry.Direction.RECEIVED,
            url = url,
            message = message ?: "",
            isError = true,
            errorMessage = error
        ))
    }

    suspend fun getLogs(limit: Int = 200): List<WebSocketLogEntry> = withContext(Dispatchers.IO) {
        dao.getRecentLogs(limit)
    }

    suspend fun exportLogs(): File = withContext(Dispatchers.IO) {
        val logs = dao.getAllLogs()
        val file = File(context.cacheDir, "websocket_logs_${System.currentTimeMillis()}.txt")

        file.writeText(buildString {
            logs.forEach { log ->
                appendLine("[${Date(log.timestamp)}] ${log.direction} ${log.url}")
                if (log.isError) {
                    appendLine("ERROR: ${log.errorMessage}")
                }
                appendLine(log.message)
                appendLine("=".repeat(80))
            }
        })
        file
    }

    private fun isAudioMessage(message: String): Boolean {
        return try {
            when {
                message.startsWith("{") -> {
                    val json = JSONObject(message)
                    json.has("audio") || json.optString("contentType").contains("audio", true)
                }
                else -> message.contains("content-type: audio", ignoreCase = true)
            }
        } catch (e: Exception) {
            false
        }
    }
}
