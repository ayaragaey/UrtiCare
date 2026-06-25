package com.example.urticare20.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.urticare20.model.EntryType
import com.example.urticare20.model.LogEntry
import java.time.ZonedDateTime

class TrackerDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "urticare_tracker.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_LOGS = "log_entries"
        private const val COLUMN_ID = "id"
        private const val COLUMN_TIMESTAMP = "timestamp"
        private const val COLUMN_TYPE = "type"
        private const val COLUMN_METADATA = "metadata" // New column for custom medication details
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_LOGS (
                $COLUMN_ID TEXT PRIMARY KEY,
                $COLUMN_TIMESTAMP TEXT NOT NULL,
                $COLUMN_TYPE TEXT NOT NULL,
                $COLUMN_METADATA TEXT
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LOGS")
        onCreate(db)
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        // Dynamically add metadata column if it doesn't exist yet (safely upgrades older databases in place)
        try {
            db.execSQL("ALTER TABLE $TABLE_LOGS ADD COLUMN $COLUMN_METADATA TEXT")
            Log.d("TrackerDatabaseHelper", "Database schema upgraded successfully: metadata column added.")
        } catch (e: Exception) {
            // Column already exists, safe to ignore
        }
    }

    /**
     * Inserts a new tracked log entry into the local SQLite database.
     */
    fun insertEntry(entry: LogEntry): Boolean {
        return try {
            val db = this.writableDatabase
            val values = ContentValues().apply {
                put(COLUMN_ID, entry.id)
                put(COLUMN_TIMESTAMP, entry.timestamp)
                put(COLUMN_TYPE, entry.type.name)
                put(COLUMN_METADATA, entry.metadata)
            }
            val result = db.insert(TABLE_LOGS, null, values)
            result != -1L
        } catch (e: Exception) {
            Log.e("TrackerDatabaseHelper", "Error inserting entry", e)
            false
        }
    }

    /**
     * Retrospectively modifies an existing log entry timestamp.
     */
    fun updateEntry(id: String, newTimestamp: String): Boolean {
        return try {
            val db = this.writableDatabase
            val values = ContentValues().apply {
                put(COLUMN_TIMESTAMP, newTimestamp)
            }
            val result = db.update(TABLE_LOGS, values, "$COLUMN_ID = ?", arrayOf(id))
            result > 0
        } catch (e: Exception) {
            Log.e("TrackerDatabaseHelper", "Error updating entry", e)
            false
        }
    }

    /**
     * Retrospectively modifies an existing log entry's timestamp and metadata.
     */
    fun updateEntryDetails(id: String, newTimestamp: String, newMetadata: String?): Boolean {
        return try {
            val db = this.writableDatabase
            val values = ContentValues().apply {
                put(COLUMN_TIMESTAMP, newTimestamp)
                put(COLUMN_METADATA, newMetadata)
            }
            val result = db.update(TABLE_LOGS, values, "$COLUMN_ID = ?", arrayOf(id))
            result > 0
        } catch (e: Exception) {
            Log.e("TrackerDatabaseHelper", "Error updating entry details", e)
            false
        }
    }

    /**
     * Clears/deletes a single log entry from the database.
     */
    fun deleteEntry(id: String): Boolean {
        return try {
            val db = this.writableDatabase
            val result = db.delete(TABLE_LOGS, "$COLUMN_ID = ?", arrayOf(id))
            result > 0
        } catch (e: Exception) {
            Log.e("TrackerDatabaseHelper", "Error deleting entry", e)
            false
        }
    }

    /**
     * Clears all tracking parameters from the local database.
     */
    fun clearAllEntries() {
        try {
            val db = this.writableDatabase
            db.delete(TABLE_LOGS, null, null)
        } catch (e: Exception) {
            Log.e("TrackerDatabaseHelper", "Error clearing all entries", e)
        }
    }

    /**
     * Clears all entries of specific types (useful for tab-specific resetting).
     */
    fun clearAllEntriesOfTypes(types: List<EntryType>): Boolean {
        return try {
            val db = this.writableDatabase
            val whereClause = "$COLUMN_TYPE IN (${types.joinToString { "'${it.name}'" }})"
            db.delete(TABLE_LOGS, whereClause, null)
            true
        } catch (e: Exception) {
            Log.e("TrackerDatabaseHelper", "Error clearing entries of types", e)
            false
        }
    }

    /**
     * Returns all entries stored inside SQLite, filtered chronologically (newest first).
     * Incorporates verification loops to catch and discard corrupted logs safely.
     */
    fun getAllEntries(): List<LogEntry> {
        val list = mutableListOf<LogEntry>()
        val db = this.readableDatabase
        val selectQuery = "SELECT * FROM $TABLE_LOGS ORDER BY $COLUMN_TIMESTAMP DESC"
        
        try {
            val cursor = db.rawQuery(selectQuery, null)
            if (cursor.moveToFirst()) {
                do {
                    val id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID))
                    val timestamp = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
                    val typeStr = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE))
                    
                    // Fetch metadata safely (handles null/missing columns gracefully)
                    val metadataIdx = cursor.getColumnIndex(COLUMN_METADATA)
                    val metadata = if (metadataIdx != -1) cursor.getString(metadataIdx) else null

                    // Audit check validation to gracefully discard anomalies
                    try {
                        val type = EntryType.valueOf(typeStr)
                        // Verify date parser runs successfully on loaded parameters
                        ZonedDateTime.parse(timestamp)
                        list.add(LogEntry(id, timestamp, type, metadata))
                    } catch (e: Exception) {
                        Log.e("TrackerDatabaseHelper", "Corruption Guard triggered for entry ID: $id. Purging record.", e)
                        // Proactively delete anomalous record to avoid loops
                        db.delete(TABLE_LOGS, "$COLUMN_ID = ?", arrayOf(id))
                    }
                } while (cursor.moveToNext())
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e("TrackerDatabaseHelper", "Fatal error during entries retrieval query", e)
        }
        return list
    }
}
