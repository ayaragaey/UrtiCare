package com.example.urticare.util

import com.example.urticare.model.EntryType
import com.example.urticare.model.LogEntry
import java.time.Duration
import java.time.ZonedDateTime

object MilestoneEvaluator {

    data class PastMilestone(
        val type: com.example.urticare.viewmodel.MilestoneType,
        val value: Long, // hours for stability, days for remission
        val dateCompletedStr: String,
        val reasonStopped: String,
        val completionTime: ZonedDateTime
    )

    // Calculates adherence streak in hours (medication-free window)
    fun calculateAdherenceHours(entries: List<LogEntry>, now: ZonedDateTime = ZonedDateTime.now()): Long? {
        val lastMed = entries.firstOrNull { it.type == EntryType.ANTIHISTAMINE || it.type == EntryType.CORTISONE || it.type == EntryType.ALTERNATIVE }
            ?: return null
        val lastMedTime = try {
            ZonedDateTime.parse(lastMed.timestamp)
        } catch (e: Exception) {
            return null
        }
        val duration = Duration.between(lastMedTime, now)
        val hours = duration.toHours()
        return if (hours < 0) 0 else hours
    }

    // Calculates remission streak in days (flare-up free window)
    fun calculateRemissionDays(entries: List<LogEntry>, now: ZonedDateTime = ZonedDateTime.now()): Long? {
        val lastFlare = entries.firstOrNull { it.type == EntryType.FLARE_UP }
            ?: return null
        val lastFlareTime = try {
            ZonedDateTime.parse(lastFlare.timestamp)
        } catch (e: Exception) {
            return null
        }
        val duration = Duration.between(lastFlareTime, now)
        val days = duration.toDays()
        return if (days < 0) 0 else days
    }

    // Calculates remission streak in hours (flare-up free window)
    fun calculateRemissionHours(entries: List<LogEntry>, now: ZonedDateTime = ZonedDateTime.now()): Long? {
        val lastFlare = entries.firstOrNull { it.type == EntryType.FLARE_UP }
            ?: return null
        val lastFlareTime = try {
            ZonedDateTime.parse(lastFlare.timestamp)
        } catch (e: Exception) {
            return null
        }
        val duration = Duration.between(lastFlareTime, now)
        val hours = duration.toHours()
        return if (hours < 0) 0 else hours
    }

    // Helper to get adherence last reset log ID (identifies unique streak)
    fun getAdherenceLogId(entries: List<LogEntry>): String {
        return entries.firstOrNull { it.type == EntryType.ANTIHISTAMINE || it.type == EntryType.CORTISONE || it.type == EntryType.ALTERNATIVE }?.id ?: "none"
    }

    // Helper to get remission last reset log ID (identifies unique streak)
    fun getRemissionLogId(entries: List<LogEntry>): String {
        return entries.firstOrNull { it.type == EntryType.FLARE_UP }?.id ?: "none"
    }

    fun getPastMilestones(entries: List<LogEntry>): List<PastMilestone> {
        val list = mutableListOf<PastMilestone>()
        val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy")

        // 1. Adherence (Stability) Milestones: Medication-free streaks >= 48 hours
        val meds = entries.filter { it.type == EntryType.ANTIHISTAMINE || it.type == EntryType.CORTISONE || it.type == EntryType.ALTERNATIVE }
            .sortedBy { try { ZonedDateTime.parse(it.timestamp) } catch (e: Exception) { ZonedDateTime.now() } }

        if (meds.size >= 2) {
            for (i in 0 until meds.size - 1) {
                val med0 = meds[i]
                val med1 = meds[i+1]
                try {
                    val t0 = ZonedDateTime.parse(med0.timestamp)
                    val t1 = ZonedDateTime.parse(med1.timestamp)
                    val duration = Duration.between(t0, t1)
                    val hours = duration.toHours()
                    if (hours >= 48L) {
                        // Extract reason from med1
                        val parts = (med1.metadata ?: "").split(":::").filter { !it.startsWith("wearing_off:") }
                        val rawReason = parts.find { it.startsWith("Streak Break Reason:") }
                            ?.replace("Streak Break Reason:", "")
                            ?.trim() ?: ""
                        val reason = if (rawReason.isNotEmpty()) rawReason else "Unspecified"
                        val dateCompletedStr = t1.format(dateFormatter)
                        list.add(
                            PastMilestone(
                                type = com.example.urticare.viewmodel.MilestoneType.ADHERENCE,
                                value = hours,
                                dateCompletedStr = dateCompletedStr,
                                reasonStopped = reason,
                                completionTime = t1
                            )
                        )
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }
        }

        // 2. Remission Milestones: Flare-up free streaks >= 7 days
        val flares = entries.filter { it.type == EntryType.FLARE_UP }
            .sortedBy { try { ZonedDateTime.parse(it.timestamp) } catch (e: Exception) { ZonedDateTime.now() } }

        if (flares.size >= 2) {
            for (i in 0 until flares.size - 1) {
                val flare0 = flares[i]
                val flare1 = flares[i+1]
                try {
                    val t0 = ZonedDateTime.parse(flare0.timestamp)
                    val t1 = ZonedDateTime.parse(flare1.timestamp)
                    val duration = Duration.between(t0, t1)
                    val days = duration.toDays()
                    if (days >= 7L) {
                        // Extract trigger/reasons from flare1
                        val cleanMetadata = (flare1.metadata ?: "")
                            .replace("Severity: Severe", "").replace("Angioedema: Yes", "").replace("Angioedema: No", "")
                            .replace("Severity: Mild", "")
                            .trim()
                            .removePrefix(";")
                            .removeSuffix(";")
                            .trim()
                        val reason = if (cleanMetadata.isNotEmpty()) cleanMetadata else "Unspecified"
                        val dateCompletedStr = t1.format(dateFormatter)
                        list.add(
                            PastMilestone(
                                type = com.example.urticare.viewmodel.MilestoneType.REMISSION,
                                value = days,
                                dateCompletedStr = dateCompletedStr,
                                reasonStopped = reason,
                                completionTime = t1
                            )
                        )
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }
        }

        return list.sortedByDescending { it.completionTime }
    }

    fun getLastBrokenMilestoneHours(entries: List<LogEntry>): Long? {
        val meds = entries.filter { it.type == EntryType.ANTIHISTAMINE || it.type == EntryType.CORTISONE || it.type == EntryType.ALTERNATIVE }
            .sortedBy { try { ZonedDateTime.parse(it.timestamp) } catch (e: Exception) { ZonedDateTime.now() } }
        if (meds.size < 2) return null
        
        for (i in meds.size - 2 downTo 0) {
            try {
                val t0 = ZonedDateTime.parse(meds[i].timestamp)
                val t1 = ZonedDateTime.parse(meds[i+1].timestamp)
                val duration = Duration.between(t0, t1)
                val hours = duration.toHours()
                if (hours >= 48L) {
                    return hours
                }
            } catch (e: Exception) {}
        }
        return null
    }

    fun getLastBrokenRemissionDays(entries: List<LogEntry>): Long? {
        val flares = entries.filter { it.type == EntryType.FLARE_UP }
            .sortedBy { try { ZonedDateTime.parse(it.timestamp) } catch (e: Exception) { ZonedDateTime.now() } }
        if (flares.size < 2) return null
        
        for (i in flares.size - 2 downTo 0) {
            try {
                val t0 = ZonedDateTime.parse(flares[i].timestamp)
                val t1 = ZonedDateTime.parse(flares[i+1].timestamp)
                val duration = Duration.between(t0, t1)
                val days = duration.toDays()
                if (days >= 7L) {
                    return days
                }
            } catch (e: Exception) {}
        }
        return null
    }

    // Adherence milestone sentences (+48 Hours Medication-Free, then in days after that)
    val adherenceSentences = listOf(
        "Milestone Unlocked: 48 Hours of System Stability achieved. Give your body some grace.",
        "4 Days Stable! Your system is finding its natural rhythm. Keep up the gentle pacing.",
        "6 Days of continuous balance. Your resilience is showing its true power.",
        "8 Days Stable! Another stability boundary crossed. You are successfully building a safe baseline.",
        "10 Days Stable! System Stability Tier Up! Your consistency is helping your immune system recalibrate.",
        "12 Days clear of active pills. Your mind and body are working beautifully together.",
        "14 Days Stable! Stability Milestone reached! You don't have to figure out the whole mountain—just celebrate this milestone step.",
        "16 Days Stable! A brand new milestone achieved. Trust your inner strength—you’ve got this."
    )

    // Remission milestone sentences (+1 Week Flare-Up Free)
    val remissionSentences = listOf(
        "Milestone Unlocked: 1 Week Free of Flares. Your resilience is stronger than the loop.",
        "2 Weeks of Remission! You are breaking the vicious stress-itch feedback loop day by day.",
        "3 Weeks Flare-Free. Your nervous system is thriving in this safe haven you’ve built.",
        "168 Hours of complete peace. Your skin and your nerves are resting beautifully.",
        "Nervous System Remission Level Up! You have survived 100% of your hardest days, and now you're clearing new space.",
        "Another week free of breakthroughs. You are pouring your incredible energy exactly where it belongs.",
        "A full week without a flare-up. Your body is navigating this recovery process with immense power."
    )

    fun getAdherenceSentence(tier: Int): String {
        if (tier < 1) return ""
        val index = (tier - 1) % adherenceSentences.size
        return adherenceSentences[index]
    }

    fun getRemissionSentence(tier: Int): String {
        if (tier < 1) return ""
        val index = (tier - 1) % remissionSentences.size
        return remissionSentences[index]
    }
}
