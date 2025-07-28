package com.bwc.bluethai.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

class DatabaseInspector(private val database: RoomDatabase) {

    fun getTableNames(): List<String> {
        return database.query("SELECT name FROM sqlite_master WHERE type='table'", null).use { cursor ->
            mutableListOf<String>().apply {
                while (cursor.moveToNext()) {
                    add(cursor.getString(0))
                }
            }
        }
    }

    fun getTableSchema(tableName: String): List<ColumnInfo> {
        return database.query("PRAGMA table_info($tableName)", null).use { cursor ->
            mutableListOf<ColumnInfo>().apply {
                while (cursor.moveToNext()) {
                    add(ColumnInfo(
                        cid = cursor.getInt(0),
                        name = cursor.getString(1),
                        type = cursor.getString(2),
                        notNull = cursor.getInt(3) == 1,
                        defaultValue = cursor.getString(4),
                        primaryKey = cursor.getInt(5) == 1
                    ))
                }
            }
        }
    }

    data class ColumnInfo(
        val cid: Int,
        val name: String,
        val type: String,
        val notNull: Boolean,
        val defaultValue: String?,
        val primaryKey: Boolean
    )
}