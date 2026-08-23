package com.example.urticare.data

import android.content.Context
import com.example.urticare.model.ChatMessage
import com.example.urticare.model.EntryType
import java.time.Duration
import java.time.ZonedDateTime

class ChatContextAssembler(private val context: Context) {

    private val dbHelper = TrackerDatabaseHelper(context)

    fun assembleContextBlock(
        recentMessages: List<ChatMessage>
    ): String {
        val entries = dbHelper.getAllEntries()

        // 1. Layer A: Static Medical & Behavioral Knowledge Base
        val staticKnowledge = """
            [Layer A: Static Knowledge]
            - Persona: Urti is an empathetic, peer-like, grounded AI collaborator. Tone: validating, warm, clear, avoiding lecture-heavy or preachy prose.
            - Mast Cell-Stress Link: Psychological stress acts as a literal biological trigger for Mast Cell degranulation. Stress releases neuropeptides and hormones (like Corticotropin-Releasing Hormone, CRH) that command mast cells to dump histamine into surrounding tissues. This causes blood vessel dilation (leading to swollen, raised red welts or wheals) and severe nerve irritation (manifesting as intense burning, stinging, or itching) even in the complete absence of physical or environmental allergens.
            - The "Stress-Itch" Feedback Loop: Chronic urticaria forms a vicious cycle: emotional stress triggers physical hives, and the resulting physical discomfort, pain, visible welts, and sleep deprivation induce fresh anxiety and exhaustion, which in turn commands the nervous system to release more CRH and trigger further hives.
            - Safety Bound Intercept: If the user describes severe or life-threatening symptoms such as facial swelling, throat swelling, tongue swelling, difficulty breathing, chest tightness, or systemic distress, execute an immediate emergency safety intercept: validate the fear while firmly, clearly directing the user to seek immediate professional medical assistance or go to the nearest emergency room.
        """.trimIndent()

        // 2. Layer B: Dynamic Local Patient Metrics
        val dynamicMetrics = if (entries.isEmpty()) {
            """
                [Layer B: Dynamic Patient Metrics]
                - Fallback Active: No tracker metrics or logs exist in the local database. Keep responses generalized, supportive, and focus heavily on general stress-relief and mindfulness strategies.
            """.trimIndent()
        } else {
            // Last 4 raw entries
            val last4 = entries.take(4)
            val recentActivityList = last4.mapIndexed { index, entry ->
                val typeName = when (entry.type) {
                    EntryType.FLARE_UP -> "Flare-up"
                    EntryType.ANTIHISTAMINE -> "Antihistamine"
                    EntryType.CORTISONE -> "Cortisone"
                    EntryType.XOLAIR_150 -> "Xolair 150 mg"
                    EntryType.XOLAIR_300 -> "Xolair 300 mg"
                    EntryType.ALTERNATIVE -> "Alternative medication (${entry.metadata ?: "Unnamed"})"
                    EntryType.CONSUMPTION -> {
                        val parts = entry.metadata?.split(":::")
                        val name = if (parts != null && parts.size >= 2 && parts[0] == "Consumption") parts[1] else "Consumption"
                        "Consumption ($name)"
                    }
                }
                val agoText = getAgoText(entry.timestamp)
                "Entry ${index + 1}: $typeName logged $agoText"
            }.joinToString("\n")

            // Last antihistamine time
            val lastAntihistamine = entries.firstOrNull { it.type == EntryType.ANTIHISTAMINE }
            val lastAntihistamineText = if (lastAntihistamine != null) {
                try {
                    val lastDate = ZonedDateTime.parse(lastAntihistamine.timestamp)
                    val duration = Duration.between(lastDate, ZonedDateTime.now())
                    val secsTotal = duration.seconds
                    if (secsTotal < 0) {
                        "Last Antihistamine taken 0 hours, 0 minutes ago"
                    } else {
                        val hrs = secsTotal / 3600
                        val mins = (secsTotal % 3600) / 60
                        "Last Antihistamine was taken $hrs hours, $mins minutes ago"
                    }
                } catch (e: Exception) {
                    "Last Antihistamine: parsing error"
                }
            } else {
                "Last Antihistamine: No dosage recorded"
            }

            // Compliance & Safety Alerts: Two consecutive pill/medication entries < 8 hours apart
            val medicationEntries = entries.filter {
                it.type == EntryType.ANTIHISTAMINE ||
                it.type == EntryType.CORTISONE ||
                it.type == EntryType.XOLAIR_150 ||
                it.type == EntryType.XOLAIR_300 ||
                it.type == EntryType.ALTERNATIVE
            }
            var unsafeWindowFlag = false
            for (i in 0 until medicationEntries.size - 1) {
                try {
                    val d1 = ZonedDateTime.parse(medicationEntries[i].timestamp)
                    val d2 = ZonedDateTime.parse(medicationEntries[i + 1].timestamp)
                    val diffMins = Math.abs(Duration.between(d1, d2).toMinutes())
                    if (diffMins < 8 * 60) {
                        unsafeWindowFlag = true
                        break
                    }
                } catch (e: Exception) {
                    // Ignore parsing error
                }
            }

            val alertText = if (unsafeWindowFlag) {
                "CRITICAL MEDICATION ALERT: An unsafe medication compliance interval (<8 hours) has been detected in the user's logs. Gently promote compliance and dosing interval safety awareness in your responses."
            } else {
                "Medication compliance interval is safe (no doses recorded under 8 hours apart)."
            }

            """
                [Layer B: Dynamic Patient Metrics]
                - Recent Activity Stream:
                $recentActivityList
                - Live Tracker Delta: $lastAntihistamineText
                - Compliance Status: $alertText
            """.trimIndent()
        }

        // 3. Layer C: Conversational Memory (last 10 messages)
        val last10 = recentMessages.takeLast(10)
        val conversationMemory = if (last10.isEmpty()) {
            "[Layer C: Conversational Memory]\nNo previous messages in this session."
        } else {
            val formattedHistory = last10.joinToString("\n") { msg ->
                "[${msg.sender}]: ${msg.text}"
            }
            "[Layer C: Conversational Memory]\n$formattedHistory"
        }

        return """
            [User Context Block]
            ==================================================
            $staticKnowledge
            ==================================================
            $dynamicMetrics
            ==================================================
            $conversationMemory
            ==================================================
        """.trimIndent()
    }

    private fun getAgoText(timestamp: String): String {
        return try {
            val entryTime = ZonedDateTime.parse(timestamp)
            val now = ZonedDateTime.now()
            val diffMins = Duration.between(entryTime, now).toMinutes()
            when {
                diffMins < 0 -> "just now"
                diffMins < 1 -> "less than a minute ago"
                diffMins < 60 -> "$diffMins minutes ago"
                diffMins < 24 * 60 -> {
                    val hrs = diffMins / 60
                    val mins = diffMins % 60
                    "$hrs hours, $mins minutes ago"
                }
                else -> {
                    val days = diffMins / (24 * 60)
                    "$days days ago"
                }
            }
        } catch (e: Exception) {
            "some time ago"
        }
    }
}
