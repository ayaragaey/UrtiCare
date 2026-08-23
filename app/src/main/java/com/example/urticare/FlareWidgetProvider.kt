package com.example.urticare

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.Toast
import com.example.urticare.data.TrackerDatabaseHelper
import com.example.urticare.model.EntryType
import com.example.urticare.model.LogEntry
import java.time.ZonedDateTime
import java.util.UUID

class FlareWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_LOG_FLARE = "com.example.urticare20.ACTION_LOG_FLARE"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateFlareWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_LOG_FLARE) {
            // Log the flare up in database
            val dbHelper = TrackerDatabaseHelper(context)
            val timestamp = ZonedDateTime.now().toString()
            val sharedPrefs = context.getSharedPreferences("urticare_prefs", Context.MODE_PRIVATE)
            var finalMetadata = "Severity: Mild; Angioedema: No"
            try {
                val today = java.time.LocalDate.now()
                val activeMeds = mutableListOf<String>()

                // Check active profile medications
                val otherMedsJson = sharedPrefs.getString("profile_other_medications", "") ?: ""
                if (otherMedsJson.isNotEmpty()) {
                    val array = org.json.JSONArray(otherMedsJson)
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val name = obj.optString("name", "")
                        val mgs = obj.optString("mgs", "")
                        val startDateStr = obj.optString("startDate", "")
                        if (name.isNotEmpty() && startDateStr.isNotEmpty()) {
                            try {
                                val medStart = java.time.LocalDate.parse(startDateStr)
                                if (!today.isBefore(medStart)) {
                                    activeMeds.add("$name ($mgs mg)")
                                }
                            } catch (e: Exception) {}
                        }
                    }
                }

                // Check completed courses
                val completedJson = sharedPrefs.getString("completed_medication_courses", "") ?: ""
                if (completedJson.isNotEmpty()) {
                    val array = org.json.JSONArray(completedJson)
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val name = obj.optString("name", "")
                        val mgs = obj.optString("mgs", "")
                        val startDateStr = obj.optString("startDate", "")
                        val endDateStr = obj.optString("endDate", "")
                        if (name.isNotEmpty() && startDateStr.isNotEmpty() && endDateStr.isNotEmpty()) {
                            try {
                                val start = java.time.LocalDate.parse(startDateStr)
                                val end = java.time.LocalDate.parse(endDateStr)
                                if (!today.isBefore(start) && !today.isAfter(end)) {
                                    activeMeds.add("$name ($mgs mg)")
                                }
                            } catch (e: Exception) {}
                        }
                    }
                }

                val distinctMeds = activeMeds.distinct()
                if (distinctMeds.isNotEmpty()) {
                    finalMetadata = "$finalMetadata; Ongoing Medication: ${distinctMeds.joinToString(", ")}"
                }

                val profileSex = sharedPrefs.getString("profile_sex", "") ?: ""
                val profilePregnant = sharedPrefs.getString("profile_pregnant", "") ?: ""
                if (profileSex == "F" && profilePregnant == "Yes") {
                    finalMetadata = "$finalMetadata; Possibly a pregnancy-associated flare-up"
                }
            } catch (e: Exception) {}

            val newEntry = LogEntry(
                id = "${EntryType.FLARE_UP.name}_${UUID.randomUUID()}",
                timestamp = timestamp,
                type = EntryType.FLARE_UP,
                metadata = finalMetadata
            )
            
            val success = dbHelper.insertEntry(newEntry)
            if (success) {
                Toast.makeText(context, "Symptom Flare Up logged", Toast.LENGTH_SHORT).show()
                
                // Update any active widget instances to ensure visual responsiveness
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, FlareWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                onUpdate(context, appWidgetManager, appWidgetIds)
            } else {
                Toast.makeText(context, "Failed to log flare up", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

internal fun updateFlareWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    val views = RemoteViews(context.packageName, R.layout.flare_widget_layout)

    // PendingIntent for widget tap action
    val intent = Intent(context, FlareWidgetProvider::class.java).apply {
        action = FlareWidgetProvider.ACTION_LOG_FLARE
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
