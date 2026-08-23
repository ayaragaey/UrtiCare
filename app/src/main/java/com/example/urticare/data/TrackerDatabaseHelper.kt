package com.example.urticare.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.urticare.model.EntryType
import com.example.urticare.model.LogEntry
import com.example.urticare.model.ConsumableItem
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.ZonedDateTime
import java.util.UUID

class TrackerDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "urticare_tracker.db"
        private const val DATABASE_VERSION = 2

        private const val TABLE_LOGS = "log_entries"
        private const val COLUMN_ID = "id"
        private const val COLUMN_TIMESTAMP = "timestamp"
        private const val COLUMN_TYPE = "type"
        private const val COLUMN_METADATA = "metadata"
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
        createConsumablesTable(db)
        createFavoritesTable(db)
    }

    private fun createConsumablesTable(db: SQLiteDatabase) {
        val query = """
            CREATE TABLE IF NOT EXISTS consumables_catalog (
                consumable_id TEXT PRIMARY KEY,
                display_name TEXT NOT NULL,
                category TEXT NOT NULL,
                subcategory TEXT NOT NULL,
                item_group TEXT,
                specificity TEXT,
                parent_id TEXT,
                selectable INTEGER NOT NULL,
                aliases TEXT,
                default_unit TEXT,
                allowed_units TEXT,
                requires_dosage_confirmation INTEGER NOT NULL,
                search_keywords TEXT,
                sort_order INTEGER,
                active INTEGER NOT NULL,
                implementation_note TEXT
            )
        """.trimIndent()
        db.execSQL(query)
    }

    private fun createFavoritesTable(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS consumables_favorites (consumable_id TEXT PRIMARY KEY)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            createConsumablesTable(db)
            createFavoritesTable(db)
        }
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        createConsumablesTable(db)
        createFavoritesTable(db)
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
        val selectQuery = "SELECT * FROM $TABLE_LOGS ORDER BY $COLUMN_TIMESTAMP DESC, $COLUMN_ID DESC"
        
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
                        if (id.contains("_mock_")) {
                            val writeDb = this.writableDatabase
                            writeDb.delete(TABLE_LOGS, "$COLUMN_ID = ?", arrayOf(id))
                            continue
                        }
                        val type = EntryType.valueOf(typeStr)
                        // Verify date parser runs successfully on loaded parameters
                        ZonedDateTime.parse(timestamp)
                        list.add(LogEntry(id, timestamp, type, metadata))
                    } catch (e: Exception) {
                        Log.e("TrackerDatabaseHelper", "Corruption Guard triggered for entry ID: $id. Purging record.", e)
                        // Proactively delete anomalous record to avoid loops
                        val writeDb = this.writableDatabase
                        writeDb.delete(TABLE_LOGS, "$COLUMN_ID = ?", arrayOf(id))
                    }
                } while (cursor.moveToNext())
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e("TrackerDatabaseHelper", "Fatal error during entries retrieval query", e)
        }
        return list
    }

    /**
     * Seeds the consumables catalog table from the assets CSV file if it is empty.
     */
    fun seedConsumablesCatalogIfEmpty(context: Context) {
        val db = this.writableDatabase
        try {
            val countQuery = "SELECT COUNT(*) FROM consumables_catalog WHERE consumable_id NOT LIKE 'custom_%'"
            val count = db.compileStatement(countQuery).simpleQueryForLong()
            if (count >= 2000L) {
                return // Already seeded
            }
        } catch (e: Exception) {
            // Table might not exist yet or count query failed
        }

        try {
            db.beginTransaction()
            val inputStream = context.assets.open("consumables_master_catalog.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))
            
            var headerLine = reader.readLine() ?: return
            if (headerLine.startsWith("\uFEFF")) {
                headerLine = headerLine.substring(1)
            }
            val headers = parseCsvLine(headerLine)
            val idIdx = headers.indexOf("consumable_id")
            val nameIdx = headers.indexOf("display_name")
            val catIdx = headers.indexOf("category")
            val subcatIdx = headers.indexOf("subcategory")
            val groupIdx = headers.indexOf("item_group")
            val specIdx = headers.indexOf("specificity")
            val parentIdx = headers.indexOf("parent_id")
            val selectIdx = headers.indexOf("selectable")
            val aliasIdx = headers.indexOf("aliases")
            val defUnitIdx = headers.indexOf("default_unit")
            val allowUnitIdx = headers.indexOf("allowed_units")
            val reqConfirmIdx = headers.indexOf("requires_dosage_confirmation")
            val keywordsIdx = headers.indexOf("search_keywords")
            val sortIdx = headers.indexOf("sort_order")
            val activeIdx = headers.indexOf("active")
            val noteIdx = headers.indexOf("implementation_note")

            var line = reader.readLine()
            while (line != null) {
                if (line.trim().isEmpty()) {
                    line = reader.readLine()
                    continue
                }
                val tokens = parseCsvLine(line)
                if (idIdx >= 0 && tokens.size > idIdx) {
                    val id = tokens[idIdx]
                    val active = if (activeIdx >= 0 && tokens.size > activeIdx) tokens[activeIdx].uppercase() == "TRUE" else true
                    if (active) {
                        val name = if (nameIdx >= 0) tokens.getOrNull(nameIdx) ?: "" else ""
                        val category = if (catIdx >= 0) tokens.getOrNull(catIdx) ?: "" else ""
                        val subcategory = if (subcatIdx >= 0) tokens.getOrNull(subcatIdx) ?: "" else ""
                        val group = if (groupIdx >= 0) tokens.getOrNull(groupIdx) ?: "" else ""
                        val specificity = if (specIdx >= 0) tokens.getOrNull(specIdx) ?: "" else ""
                        val parentId = if (parentIdx >= 0) tokens.getOrNull(parentIdx) ?: "" else ""
                        val selectable = if (selectIdx >= 0) tokens.getOrNull(selectIdx)?.uppercase() == "TRUE" else false
                        val aliases = if (aliasIdx >= 0) tokens.getOrNull(aliasIdx) ?: "" else ""
                        val defaultUnit = if (defUnitIdx >= 0) tokens.getOrNull(defUnitIdx) ?: "" else ""
                        val allowedUnits = if (allowUnitIdx >= 0) tokens.getOrNull(allowUnitIdx) ?: "" else ""
                        val requiresConfirm = if (reqConfirmIdx >= 0) tokens.getOrNull(reqConfirmIdx)?.uppercase() == "TRUE" else false
                        val keywords = if (keywordsIdx >= 0) tokens.getOrNull(keywordsIdx) ?: "" else ""
                        val sortOrder = if (sortIdx >= 0) tokens.getOrNull(sortIdx)?.toIntOrNull() ?: 9999 else 9999
                        val note = if (noteIdx >= 0) tokens.getOrNull(noteIdx) ?: "" else ""

                        val values = ContentValues().apply {
                            put("consumable_id", id)
                            put("display_name", name)
                            put("category", category)
                            put("subcategory", subcategory)
                            put("item_group", group)
                            put("specificity", specificity)
                            put("parent_id", parentId)
                            put("selectable", if (selectable) 1 else 0)
                            put("aliases", aliases)
                            put("default_unit", defaultUnit)
                            put("allowed_units", allowedUnits)
                            put("requires_dosage_confirmation", if (requiresConfirm) 1 else 0)
                            put("search_keywords", keywords)
                            put("sort_order", sortOrder)
                            put("active", 1)
                            put("implementation_note", note)
                        }
                        db.insertWithOnConflict("consumables_catalog", null, values, SQLiteDatabase.CONFLICT_REPLACE)
                    }
                }
                line = reader.readLine()
            }
            db.setTransactionSuccessful()
            Log.d("TrackerDatabaseHelper", "Consumables catalog seeded successfully from CSV.")
        } catch (e: Exception) {
            Log.e("TrackerDatabaseHelper", "Error seeding consumables catalog", e)
        } finally {
            try {
                db.endTransaction()
            } catch (e: Exception) {}
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var curVal = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            if (inQuotes) {
                if (ch == '\"') {
                    if (i + 1 < line.length && line[i + 1] == '\"') {
                        curVal.append('\"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    curVal.append(ch)
                }
            } else {
                if (ch == '\"') {
                    inQuotes = true
                } else if (ch == ',') {
                    result.add(curVal.toString().trim())
                    curVal = StringBuilder()
                } else {
                    curVal.append(ch)
                }
            }
            i++
        }
        result.add(curVal.toString().trim())
        return result
    }

    /**
     * Fetches all active consumables from the database.
     */
    fun getAllConsumables(): List<ConsumableItem> {
        val list = mutableListOf<ConsumableItem>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM consumables_catalog WHERE active = 1 ORDER BY sort_order ASC, display_name ASC", null)
        try {
            if (cursor.moveToFirst()) {
                do {
                    list.add(parseConsumableItemCursor(cursor))
                } while (cursor.moveToNext())
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e("TrackerDatabaseHelper", "Error fetching all consumables", e)
        }
        return list
    }

    private fun parseConsumableItemCursor(cursor: android.database.Cursor): ConsumableItem {
        return ConsumableItem(
            id = cursor.getString(cursor.getColumnIndexOrThrow("consumable_id")),
            displayName = cursor.getString(cursor.getColumnIndexOrThrow("display_name")),
            category = cursor.getString(cursor.getColumnIndexOrThrow("category")),
            subcategory = cursor.getString(cursor.getColumnIndexOrThrow("subcategory")),
            itemGroup = cursor.getString(cursor.getColumnIndexOrThrow("item_group")),
            specificity = cursor.getString(cursor.getColumnIndexOrThrow("specificity")),
            parentId = cursor.getString(cursor.getColumnIndexOrThrow("parent_id")),
            selectable = cursor.getInt(cursor.getColumnIndexOrThrow("selectable")) == 1,
            aliases = cursor.getString(cursor.getColumnIndexOrThrow("aliases")),
            defaultUnit = cursor.getString(cursor.getColumnIndexOrThrow("default_unit")),
            allowedUnits = cursor.getString(cursor.getColumnIndexOrThrow("allowed_units")),
            requiresDosageConfirmation = cursor.getInt(cursor.getColumnIndexOrThrow("requires_dosage_confirmation")) == 1,
            searchKeywords = cursor.getString(cursor.getColumnIndexOrThrow("search_keywords")),
            sortOrder = cursor.getInt(cursor.getColumnIndexOrThrow("sort_order")),
            active = cursor.getInt(cursor.getColumnIndexOrThrow("active")) == 1,
            implementationNote = cursor.getString(cursor.getColumnIndexOrThrow("implementation_note"))
        )
    }

    /**
     * Performs a ranked search on the consumables catalog.
     */
    fun searchConsumables(query: String, favoriteIds: Set<String>, recents: List<String>): List<ConsumableItem> {
        val all = getAllConsumables()
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) {
            return all.sortedWith(
                compareByDescending<ConsumableItem> { favoriteIds.contains(it.id) }
                    .thenByDescending { recents.contains(it.displayName) }
                    .thenBy { it.sortOrder }
            )
        }

        val scored = all.mapNotNull { item ->
            val nameLower = item.displayName.lowercase()
            var score = 0
            var matched = false

            if (nameLower == trimmed) {
                score += 1000
                matched = true
            } else if (nameLower.startsWith(trimmed)) {
                score += 500
                matched = true
            }

            if (favoriteIds.contains(item.id)) {
                score += 300
            }

            if (recents.contains(item.displayName)) {
                score += 200
            }

            val aliasesList = item.aliases?.split("|") ?: emptyList()
            if (aliasesList.any { it.trim().lowercase() == trimmed || it.trim().lowercase().startsWith(trimmed) }) {
                score += 100
                matched = true
            }

            val keywords = item.searchKeywords?.lowercase() ?: ""
            if (keywords.contains(trimmed) || nameLower.contains(trimmed)) {
                score += 50
                matched = true
            }

            if (matched) Pair(item, score) else null
        }

        return scored.sortedByDescending { it.second }.map { it.first }
    }

    /**
     * Toggles a consumable's favorite status.
     */
    fun toggleConsumableFavorite(id: String, isFavorite: Boolean) {
        val db = this.writableDatabase
        try {
            if (isFavorite) {
                val values = ContentValues().apply { put("consumable_id", id) }
                db.insertWithOnConflict("consumables_favorites", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            } else {
                db.delete("consumables_favorites", "consumable_id = ?", arrayOf(id))
            }
        } catch (e: Exception) {
            Log.e("TrackerDatabaseHelper", "Error toggling favorite status", e)
        }
    }

    /**
     * Checks if a consumable is favorited.
     */
    fun isConsumableFavorite(id: String): Boolean {
        val db = this.readableDatabase
        var fav = false
        try {
            val cursor = db.rawQuery("SELECT 1 FROM consumables_favorites WHERE consumable_id = ?", arrayOf(id))
            fav = cursor.count > 0
            cursor.close()
        } catch (e: Exception) {
            Log.e("TrackerDatabaseHelper", "Error checking favorite status", e)
        }
        return fav
    }

    /**
     * Gets favorited consumable IDs.
     */
    fun getFavoriteConsumableIds(): Set<String> {
        val db = this.readableDatabase
        val set = mutableSetOf<String>()
        try {
            val cursor = db.rawQuery("SELECT consumable_id FROM consumables_favorites", null)
            if (cursor.moveToFirst()) {
                do {
                    set.add(cursor.getString(0))
                } while (cursor.moveToNext())
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e("TrackerDatabaseHelper", "Error getting favorite IDs", e)
        }
        return set
    }

    /**
     * Gets favorited consumable display names.
     */
    fun getFavoriteConsumableNames(): List<String> {
        val db = this.readableDatabase
        val list = mutableListOf<String>()
        val query = """
            SELECT c.display_name 
            FROM consumables_favorites f
            JOIN consumables_catalog c ON f.consumable_id = c.consumable_id
        """.trimIndent()
        try {
            val cursor = db.rawQuery(query, null)
            if (cursor.moveToFirst()) {
                do {
                    list.add(cursor.getString(0))
                } while (cursor.moveToNext())
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e("TrackerDatabaseHelper", "Error getting favorite names", e)
        }
        return list
    }

    /**
     * Inserts a user custom consumable.
     */
    fun insertCustomConsumable(name: String, category: String, defaultUnit: String): ConsumableItem {
        val db = this.writableDatabase
        val id = "custom_" + UUID.randomUUID().toString()
        val values = ContentValues().apply {
            put("consumable_id", id)
            put("display_name", name)
            put("category", category)
            put("subcategory", "Custom Items")
            put("item_group", "")
            put("specificity", "specific")
            put("parent_id", "")
            put("selectable", 1)
            put("aliases", "")
            put("default_unit", defaultUnit)
            put("allowed_units", "serving|piece|g|kg|oz|cup|tbsp|tsp|slice|bowl|plate|pack")
            put("requires_dosage_confirmation", 0)
            put("search_keywords", "$name Custom item")
            put("sort_order", 9999)
            put("active", 1)
            put("implementation_note", "User custom item")
        }
        try {
            db.insert("consumables_catalog", null, values)
        } catch (e: Exception) {
            Log.e("TrackerDatabaseHelper", "Error inserting custom consumable", e)
        }
        return ConsumableItem(
            id = id,
            displayName = name,
            category = category,
            subcategory = "Custom Items",
            itemGroup = "",
            specificity = "specific",
            parentId = "",
            selectable = true,
            aliases = "",
            defaultUnit = defaultUnit,
            allowedUnits = "serving|piece|g|kg|oz|cup|tbsp|tsp|slice|bowl|plate|pack",
            requiresDosageConfirmation = false,
            searchKeywords = "$name Custom item",
            sortOrder = 9999,
            active = true,
            implementationNote = "User custom item"
        )
    }

    /**
     * Gets recently used consumables dynamically from history logs.
     */
    fun getRecentlyUsedConsumables(limit: Int = 10): List<String> {
        val entries = getAllEntries().filter { it.type == EntryType.CONSUMPTION }
        val names = mutableListOf<String>()
        entries.forEach { entry ->
            val parts = entry.metadata?.split(":::")
            if (parts != null && parts.size >= 2 && parts[0] == "Consumption") {
                val name = parts[1]
                if (name.isNotEmpty() && !names.contains(name)) {
                    names.add(name)
                    if (names.size >= limit) return names
                }
            }
        }
        return names
    }

    /**
     * Gets last used quantity and unit for a consumable item.
     */
    fun getLastUsedQuantityAndUnit(itemName: String): Pair<String, String>? {
        val entries = getAllEntries().filter { it.type == EntryType.CONSUMPTION }
        entries.forEach { entry ->
            val parts = entry.metadata?.split(":::")
            if (parts != null && parts.size >= 4 && parts[0] == "Consumption" && parts[1].equals(itemName, ignoreCase = true)) {
                val amt = parts[3]
                if (amt.isNotEmpty()) {
                    val tokens = amt.trim().split(" ")
                    if (tokens.size >= 2) {
                        val qty = tokens[0]
                        val unit = tokens.subList(1, tokens.size).joinToString(" ")
                        return Pair(qty, unit)
                    } else if (tokens.size == 1) {
                        return Pair(tokens[0], "")
                    }
                }
            }
        }
        return null
    }
}
