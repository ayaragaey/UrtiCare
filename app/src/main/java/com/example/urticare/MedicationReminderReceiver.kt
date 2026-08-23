package com.example.urticare

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.urticare.data.TrackerDatabaseHelper
import com.example.urticare.model.EntryType
import com.example.urticare.model.LogEntry
import com.example.urticare.model.MedicationReminder
import org.json.JSONArray
import org.json.JSONObject
import java.time.ZonedDateTime
import java.util.UUID

class MedicationReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TRIGGER_REMINDER = "com.example.urticare20.ACTION_TRIGGER_REMINDER"
        const val ACTION_TAKEN = "com.example.urticare20.ACTION_TAKEN"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val CHANNEL_ID = "medication_reminders_channel"
        const val CHANNEL_NAME = "Medication Reminders"

        fun scheduleNextAlarm(context: Context, reminder: MedicationReminder) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val nextTrigger = calculateNextTriggerTime(
                reminder.scheduleType,
                reminder.timeOfDay,
                reminder.daysOfWeek,
                reminder.intervalHours
            )

            val intent = Intent(context, MedicationReminderReceiver::class.java).apply {
                action = ACTION_TRIGGER_REMINDER
                putExtra(EXTRA_REMINDER_ID, reminder.id)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reminder.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTrigger, pendingIntent)
                    } else {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTrigger, pendingIntent)
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTrigger, pendingIntent)
                }
            } catch (e: SecurityException) {
                // Fallback to inexact setAndAllowWhileIdle if exact alarm permission fails
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTrigger, pendingIntent)
            }
        }

        fun cancelAlarm(context: Context, reminderId: String) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, MedicationReminderReceiver::class.java).apply {
                action = ACTION_TRIGGER_REMINDER
                putExtra(EXTRA_REMINDER_ID, reminderId)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reminderId.hashCode(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }

        fun calculateNextTriggerTime(
            scheduleType: String,
            timeOfDay: String,
            daysOfWeekStr: String,
            intervalHours: Int
        ): Long {
            val now = ZonedDateTime.now()
            return when (scheduleType) {
                "DAILY" -> {
                    val parts = timeOfDay.split(":")
                    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 12
                    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                    var trigger = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
                    if (!trigger.isAfter(now)) {
                        trigger = trigger.plusDays(1)
                    }
                    trigger.toInstant().toEpochMilli()
                }
                "WEEKLY" -> {
                    val parts = timeOfDay.split(":")
                    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 12
                    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                    val days = daysOfWeekStr.split(",").filter { it.isNotEmpty() }.mapNotNull { it.toIntOrNull() }
                    if (days.isEmpty()) {
                        var trigger = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
                        if (!trigger.isAfter(now)) {
                            trigger = trigger.plusDays(1)
                        }
                        return trigger.toInstant().toEpochMilli()
                    }

                    var nextTrigger: ZonedDateTime? = null
                    for (dayOffset in 0..7) {
                        val candidate = now.plusDays(dayOffset.toLong()).withHour(hour).withMinute(minute).withSecond(0).withNano(0)
                        val candidateDayVal = candidate.dayOfWeek.value // 1 = Monday, ..., 7 = Sunday
                        if (days.contains(candidateDayVal)) {
                            if (candidate.isAfter(now)) {
                                if (nextTrigger == null || candidate.isBefore(nextTrigger)) {
                                    nextTrigger = candidate
                                }
                            }
                        }
                    }
                    nextTrigger?.toInstant()?.toEpochMilli() ?: (System.currentTimeMillis() + 24 * 3600 * 1000)
                }
                "INTERVAL" -> {
                    val hours = if (intervalHours <= 0) 8 else intervalHours
                    val trigger = now.plusHours(hours.toLong()).withSecond(0).withNano(0)
                    trigger.toInstant().toEpochMilli()
                }
                else -> System.currentTimeMillis() + 8 * 3600 * 1000
            }
        }

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Medication Reminder Alarms"
                    enableVibration(true)
                }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val action = intent.action

        if (action == Intent.ACTION_BOOT_COMPLETED) {
            // Reschedule all enabled alarms on device boot
            val sharedPrefs = context.getSharedPreferences("urticare_prefs", Context.MODE_PRIVATE)
            val json = sharedPrefs.getString("profile_reminders", "") ?: ""
            val reminders = deserializeReminders(json)
            reminders.filter { it.isEnabled }.forEach { rem ->
                scheduleNextAlarm(context, rem)
            }
            return
        }

        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
        val sharedPrefs = context.getSharedPreferences("urticare_prefs", Context.MODE_PRIVATE)
        val json = sharedPrefs.getString("profile_reminders", "") ?: ""
        val reminders = deserializeReminders(json)
        val reminder = reminders.find { it.id == reminderId }

        if (action == ACTION_TRIGGER_REMINDER) {
            if (reminder == null || !reminder.isEnabled) return

            // Create notification channel
            createNotificationChannel(context)

            // Setup "Taken?" PendingIntent
            val notificationId = reminder.id.hashCode()
            val takenIntent = Intent(context, MedicationReminderReceiver::class.java).apply {
                this.action = ACTION_TAKEN
                putExtra(EXTRA_REMINDER_ID, reminder.id)
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
            val takenPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                takenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Setup app launch intent
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                notificationId + 1,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Build Notification
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Medication Reminder")
                .setContentText("It is time to take ${reminder.medName}")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(openAppPendingIntent)
                .setAutoCancel(true)
                .addAction(
                    android.R.drawable.ic_menu_save,
                    "Done",
                    takenPendingIntent
                )

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(notificationId, builder.build())

            // Reschedule next recurrence for recurring schedules
            scheduleNextAlarm(context, reminder)

        } else if (action == ACTION_TAKEN) {
            // Dismiss the notification
            val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
            if (notificationId != -1) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(notificationId)
            }

            if (reminder != null) {
                // Log it to database
                val dbHelper = TrackerDatabaseHelper(context)
                val entryType = when (reminder.medType) {
                    "ANTIHISTAMINE" -> EntryType.ANTIHISTAMINE
                    "CORTICOSTEROID" -> EntryType.CORTISONE
                    else -> EntryType.ALTERNATIVE
                }
                val metadata = "${reminder.medName}:::${reminder.medMgs}"
                val newEntry = LogEntry(
                    id = "${entryType.name}_${UUID.randomUUID()}",
                    timestamp = ZonedDateTime.now().toString(),
                    type = entryType,
                    metadata = metadata
                )
                val success = dbHelper.insertEntry(newEntry)
                if (success) {
                    Toast.makeText(context, "${reminder.medName} logged", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to log medication", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

// Helpers for JSON Serialization

fun serializeReminders(list: List<MedicationReminder>): String {
    val array = JSONArray()
    list.forEach {
        val obj = JSONObject()
        obj.put("id", it.id)
        obj.put("medId", it.medId)
        obj.put("medName", it.medName)
        obj.put("medMgs", it.medMgs)
        obj.put("medType", it.medType)
        obj.put("scheduleType", it.scheduleType)
        obj.put("timeOfDay", it.timeOfDay)
        obj.put("daysOfWeek", it.daysOfWeek)
        obj.put("intervalHours", it.intervalHours)
        obj.put("isEnabled", it.isEnabled)
        array.put(obj)
    }
    return array.toString()
}

fun deserializeReminders(json: String): List<MedicationReminder> {
    if (json.isEmpty()) return emptyList()
    val list = mutableListOf<MedicationReminder>()
    try {
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                MedicationReminder(
                    id = obj.getString("id"),
                    medId = obj.getString("medId"),
                    medName = obj.getString("medName"),
                    medMgs = obj.getString("medMgs"),
                    medType = obj.getString("medType"),
                    scheduleType = obj.getString("scheduleType"),
                    timeOfDay = obj.getString("timeOfDay"),
                    daysOfWeek = obj.getString("daysOfWeek"),
                    intervalHours = obj.getInt("intervalHours"),
                    isEnabled = if (obj.has("isEnabled")) obj.getBoolean("isEnabled") else true
                )
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}
