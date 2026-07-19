package com.example.urticare20

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.Toast
import com.example.urticare20.data.TrackerDatabaseHelper
import com.example.urticare20.model.EntryType
import com.example.urticare20.model.LogEntry
import org.json.JSONArray
import java.time.ZonedDateTime
import java.util.UUID

class PillWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_LOG_PILL = "com.example.urticare20.ACTION_LOG_PILL"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updatePillWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_LOG_PILL) {
            // Log the pill in database
            val dbHelper = TrackerDatabaseHelper(context)
            
            // Check urticare_prefs for main antihistamine
            val sharedPrefs = context.getSharedPreferences("urticare_prefs", Context.MODE_PRIVATE)
            val json = sharedPrefs.getString("profile_antihistamines", "") ?: ""
            var metadata: String? = null
            var medName = "Antihistamine"
            
            if (json.isNotEmpty()) {
                try {
                    val array = JSONArray(json)
                    var firstMedName: String? = null
                    var firstMedMgs: String? = null
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val name = obj.getString("name")
                        val mgs = obj.getString("mgs")
                        if (i == 0) {
                            firstMedName = name
                            firstMedMgs = mgs
                        }
                        val isMain = if (obj.has("isMain")) obj.getBoolean("isMain") else false
                        if (isMain) {
                            medName = name
                            metadata = "$name:::$mgs"
                            break
                        }
                    }
                    if (metadata == null && firstMedName != null) {
                        medName = firstMedName
                        metadata = "$firstMedName:::$firstMedMgs"
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val timestamp = ZonedDateTime.now().toString()
            val newEntry = LogEntry(
                id = "${EntryType.ANTIHISTAMINE.name}_${UUID.randomUUID()}",
                timestamp = timestamp,
                type = EntryType.ANTIHISTAMINE,
                metadata = metadata
            )
            
            val success = dbHelper.insertEntry(newEntry)
            if (success) {
                Toast.makeText(context, "$medName logged", Toast.LENGTH_SHORT).show()
                
                // Update any active widget instances to ensure visual responsiveness
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, PillWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                onUpdate(context, appWidgetManager, appWidgetIds)
            } else {
                Toast.makeText(context, "Failed to log pill", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

internal fun updatePillWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    val views = RemoteViews(context.packageName, R.layout.pill_widget_layout)

    // PendingIntent for widget tap action
    val intent = Intent(context, PillWidgetProvider::class.java).apply {
        action = PillWidgetProvider.ACTION_LOG_PILL
    }
    
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

    appWidgetManager.updateAppWidget(appWidgetId, views)
}
