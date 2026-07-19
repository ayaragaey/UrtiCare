package com.example.urticare20.ui.components

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.urticare20.model.EntryType
import com.example.urticare20.model.LogEntry
import com.example.urticare20.ui.theme.*
import com.example.urticare20.viewmodel.TrackerViewModel
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Calendar

private fun parseTimestamp(ts: String): ZonedDateTime {
    return try {
        ZonedDateTime.parse(ts)
    } catch (e: Exception) {
        try {
            Instant.parse(ts).atZone(ZoneId.systemDefault())
        } catch (e2: Exception) {
            try {
                LocalDateTime.parse(ts).atZone(ZoneId.systemDefault())
            } catch (e3: Exception) {
                ZonedDateTime.now()
            }
        }
    }
}

data class ParsedLogEntry(
    val id: String,
    val timestamp: ZonedDateTime,
    val type: EntryType,
    val severity: String,
    val hasAngioedema: Boolean,
    val reasons: List<String>,
    val medName: String,
    val medMgs: String,
    val metadata: String
)

enum class AnalysisPeriod {
    LAST_7_DAYS,
    LAST_30_DAYS,
    CURRENT_MONTH,
    PREVIOUS_MONTH,
    CUSTOM
}

data class PeriodSummaryStats(
    val totalFlares: Int,
    val avgFlaresPerWeek: Float,
    val adherencePercent: Int,
    val topTrigger: String,
    val topMedication: String,
    val angioedemaPercent: Int,
    val antihistamineCount: Int,
    val cortisoneCount: Int
)

private fun calculatePeriodStats(
    entries: List<ParsedLogEntry>,
    start: ZonedDateTime,
    end: ZonedDateTime
): PeriodSummaryStats {
    val flares = entries.filter { it.type == EntryType.FLARE_UP && (it.timestamp.isAfter(start) || it.timestamp.isEqual(start)) && (it.timestamp.isBefore(end) || it.timestamp.isEqual(end)) }
    val meds = entries.filter { it.type != EntryType.FLARE_UP && (it.timestamp.isAfter(start) || it.timestamp.isEqual(start)) && (it.timestamp.isBefore(end) || it.timestamp.isEqual(end)) }
    
    val totalFlares = flares.size
    val days = maxOf(1L, ChronoUnit.DAYS.between(start.toLocalDate(), end.toLocalDate()) + 1)
    val weeks = days / 7f
    val avgFlaresPerWeek = totalFlares / maxOf(1f, weeks)
    
    val daysWithMed = meds.map { it.timestamp.toLocalDate() }.distinct().size
    val adherencePercent = minOf(100, ((daysWithMed.toFloat() / days) * 100).toInt())
    
    // Top trigger
    val triggersMap = mutableMapOf<String, Int>()
    flares.forEach { f ->
        f.reasons.forEach { r ->
            val cleanKey = when {
                r.startsWith("Food Trigger:") -> r.replace("Food Trigger: ", "")
                r.startsWith("Vegetables:") -> r.replace("Vegetables: ", "")
                r.startsWith("Fruits:") -> r.replace("Fruits: ", "")
                r.startsWith("Fruit:") -> r.replace("Fruit: ", "")
                r.startsWith("Illness:") -> "Illness"
                else -> r
            }
            if (cleanKey.trim().isNotEmpty() && cleanKey != "Unknown") {
                triggersMap[cleanKey] = (triggersMap[cleanKey] ?: 0) + 1
            }
        }
    }
    val topTrigger = triggersMap.maxByOrNull { it.value }?.key ?: "None"
    
    // Top medication
    val medsMap = mutableMapOf<String, Int>()
    meds.forEach { m ->
        medsMap[m.medName] = (medsMap[m.medName] ?: 0) + 1
    }
    val topMedication = medsMap.maxByOrNull { it.value }?.key ?: "None"
    
    val angioCount = flares.count { it.hasAngioedema }
    val angioedemaPercent = if (totalFlares > 0) (angioCount.toFloat() / totalFlares * 100).toInt() else 0
    
    val antihistamineCount = meds.count { it.type == EntryType.ANTIHISTAMINE }
    val cortisoneCount = meds.count { it.type == EntryType.CORTISONE }
    
    return PeriodSummaryStats(
        totalFlares = totalFlares,
        avgFlaresPerWeek = avgFlaresPerWeek,
        adherencePercent = adherencePercent,
        topTrigger = topTrigger,
        topMedication = topMedication,
        angioedemaPercent = angioedemaPercent,
        antihistamineCount = antihistamineCount,
        cortisoneCount = cortisoneCount
    )
}

@Composable
fun PatternAnalysisScreen(
    viewModel: TrackerViewModel,
    entries: List<LogEntry>,
    onBack: () -> Unit = {}
) {
    BackHandler(enabled = true) {
        onBack()
    }

    var selectedPeriod by remember { mutableStateOf(AnalysisPeriod.CURRENT_MONTH) }
    var isExpanded by remember { mutableStateOf(false) }

    var showComparisonDialog by remember { mutableStateOf(false) }
    var compPeriodAStart by remember { mutableStateOf(LocalDate.now().minusDays(30)) }
    var compPeriodAEnd by remember { mutableStateOf(LocalDate.now()) }
    var compPeriodBStart by remember { mutableStateOf(LocalDate.now().minusDays(60)) }
    var compPeriodBEnd by remember { mutableStateOf(LocalDate.now().minusDays(30)) }
    var comparisonPreset by remember { mutableStateOf("this_vs_last_month") }

    LaunchedEffect(comparisonPreset) {
        when (comparisonPreset) {
            "this_vs_last_month" -> {
                compPeriodAStart = LocalDate.now().withDayOfMonth(1)
                compPeriodAEnd = LocalDate.now()
                val prevMonth = LocalDate.now().minusMonths(1)
                compPeriodBStart = prevMonth.withDayOfMonth(1)
                compPeriodBEnd = prevMonth.withDayOfMonth(prevMonth.lengthOfMonth())
            }
            "7d_vs_prev_7d" -> {
                compPeriodAStart = LocalDate.now().minusDays(7)
                compPeriodAEnd = LocalDate.now()
                compPeriodBStart = LocalDate.now().minusDays(14)
                compPeriodBEnd = LocalDate.now().minusDays(7)
            }
            "30d_vs_prev_30d" -> {
                compPeriodAStart = LocalDate.now().minusDays(30)
                compPeriodAEnd = LocalDate.now()
                compPeriodBStart = LocalDate.now().minusDays(60)
                compPeriodBEnd = LocalDate.now().minusDays(30)
            }
        }
    }

    val compRangeA = remember(compPeriodAStart, compPeriodAEnd) {
        Pair(
            compPeriodAStart.atStartOfDay(ZoneId.systemDefault()),
            compPeriodAEnd.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault())
        )
    }
    val compRangeB = remember(compPeriodBStart, compPeriodBEnd) {
        Pair(
            compPeriodBStart.atStartOfDay(ZoneId.systemDefault()),
            compPeriodBEnd.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault())
        )
    }

    val context = LocalContext.current
    var customStartDate by remember { mutableStateOf(LocalDate.now().minusDays(30)) }
    var customEndDate by remember { mutableStateOf(LocalDate.now()) }

    // Parse all log entries once
    val parsedEntries = remember(entries) {
        entries.map { entry ->
            val dt = parseTimestamp(entry.timestamp)
            val metadataStr = entry.metadata ?: ""
            val isFlareUp = entry.type == EntryType.FLARE_UP

            val severity = if (isFlareUp) {
                when {
                    metadataStr.contains("Severity: Critical") -> "Critical"
                    metadataStr.contains("Severity: Severe") -> "Severe"
                    metadataStr.contains("Severity: Moderate") -> "Moderate"
                    metadataStr.contains("Severity: Mild") -> "Mild"
                    else -> "Mild"
                }
            } else ""

            val hasAngioedema = isFlareUp && metadataStr.contains("Angioedema: Yes")

            val reasons = if (isFlareUp) {
                metadataStr.split("; ")
                    .filter {
                        !it.startsWith("Severity:") &&
                        !it.startsWith("Angioedema:") &&
                        !it.startsWith("Title:") &&
                        it.trim().isNotEmpty()
                    }
            } else emptyList()

            val (medName, medMgs) = if (!isFlareUp) {
                when (entry.type) {
                    EntryType.XOLAIR_150 -> Pair("Xolair", "150 mg")
                    EntryType.XOLAIR_300 -> Pair("Xolair", "300 mg")
                    else -> {
                        val parts = metadataStr.split(":::")
                        val name = parts.getOrNull(0) ?: when (entry.type) {
                            EntryType.ANTIHISTAMINE -> "Antihistamine"
                            EntryType.CORTISONE -> "Cortisone"
                            EntryType.ALTERNATIVE -> "Alternative"
                            else -> "Medication"
                        }
                        val mgs = parts.getOrNull(1) ?: ""
                        Pair(name, mgs)
                    }
                }
            } else Pair("", "")

            ParsedLogEntry(
                id = entry.id,
                timestamp = dt,
                type = entry.type,
                severity = severity,
                hasAngioedema = hasAngioedema,
                reasons = reasons,
                medName = medName,
                medMgs = medMgs,
                metadata = metadataStr
            )
        }
    }

    // Determine current and comparison period start/end times
    val now = ZonedDateTime.now()
    val startOfToday = now.truncatedTo(ChronoUnit.DAYS)

    val (currentStart, currentEnd) = remember(selectedPeriod, customStartDate, customEndDate) {
        when (selectedPeriod) {
            AnalysisPeriod.LAST_7_DAYS -> Pair(startOfToday.minusDays(7), now)
            AnalysisPeriod.LAST_30_DAYS -> Pair(startOfToday.minusDays(30), now)
            AnalysisPeriod.CURRENT_MONTH -> {
                val start = now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS)
                Pair(start, now)
            }
            AnalysisPeriod.PREVIOUS_MONTH -> {
                val prevMonthVal = now.minusMonths(1)
                val start = prevMonthVal.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS)
                val end = prevMonthVal.withDayOfMonth(prevMonthVal.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59)
                Pair(start, end)
            }
            AnalysisPeriod.CUSTOM -> {
                val start = customStartDate.atStartOfDay(ZoneId.systemDefault())
                val end = customEndDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault())
                Pair(start, end)
            }
        }
    }

    val (compStart, compEnd) = remember(currentStart, currentEnd, selectedPeriod) {
        val duration = Duration.between(currentStart, currentEnd)
        when (selectedPeriod) {
            AnalysisPeriod.LAST_7_DAYS -> Pair(currentStart.minusDays(7), currentStart)
            AnalysisPeriod.LAST_30_DAYS -> Pair(currentStart.minusDays(30), currentStart)
            AnalysisPeriod.CURRENT_MONTH -> {
                val prevMonthVal = now.minusMonths(1)
                val start = prevMonthVal.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS)
                val end = prevMonthVal.withDayOfMonth(prevMonthVal.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59)
                Pair(start, end)
            }
            AnalysisPeriod.PREVIOUS_MONTH -> {
                val prevMonthVal = now.minusMonths(2)
                val start = prevMonthVal.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS)
                val end = prevMonthVal.withDayOfMonth(prevMonthVal.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59)
                Pair(start, end)
            }
            AnalysisPeriod.CUSTOM -> Pair(currentStart.minus(duration), currentStart)
        }
    }

    // Filter current and previous entries
    val currentEntries = remember(parsedEntries, currentStart, currentEnd) {
        parsedEntries.filter { it.timestamp.isAfter(currentStart) && it.timestamp.isBefore(currentEnd) }
    }
    val previousEntries = remember(parsedEntries, compStart, compEnd) {
        parsedEntries.filter { it.timestamp.isAfter(compStart) && it.timestamp.isBefore(compEnd) }
    }

    // Dynamic Calculations using the calculatePeriodStats helper
    val currentStats = remember(parsedEntries, currentStart, currentEnd) {
        calculatePeriodStats(parsedEntries, currentStart, currentEnd)
    }
    val prevStats = remember(parsedEntries, compStart, compEnd) {
        calculatePeriodStats(parsedEntries, compStart, compEnd)
    }

    val currentFlareUps = remember(currentEntries) { currentEntries.filter { it.type == EntryType.FLARE_UP } }
    
    val totalFlareUps = currentStats.totalFlares
    val previousTotalFlareUps = prevStats.totalFlares

    val flareUpChangePercent = remember(totalFlareUps, previousTotalFlareUps) {
        if (previousTotalFlareUps > 0) {
            ((totalFlareUps - previousTotalFlareUps).toFloat() / previousTotalFlareUps * 100).toInt()
        } else if (totalFlareUps > 0) 100 else 0
    }

    val avgFlareUpsPerWeek = currentStats.avgFlaresPerWeek
    val previousAvgFlareUpsPerWeek = prevStats.avgFlaresPerWeek
    val avgFlareUpChangePercent = remember(avgFlareUpsPerWeek, previousAvgFlareUpsPerWeek) {
        if (previousAvgFlareUpsPerWeek > 0f) {
            (((avgFlareUpsPerWeek - previousAvgFlareUpsPerWeek) / previousAvgFlareUpsPerWeek) * 100).toInt()
        } else if (avgFlareUpsPerWeek > 0f) 100 else 0
    }

    val compPeriodLabel = remember(selectedPeriod) {
        when (selectedPeriod) {
            AnalysisPeriod.LAST_7_DAYS -> "last 7 days"
            AnalysisPeriod.LAST_30_DAYS -> "last 30 days"
            AnalysisPeriod.CURRENT_MONTH -> "last month"
            AnalysisPeriod.PREVIOUS_MONTH -> "previous month"
            AnalysisPeriod.CUSTOM -> "previous period"
        }
    }

    // Severity Breakdown
    val severityDistribution = remember(currentFlareUps) {
        mapOf(
            "Mild" to currentFlareUps.count { it.severity == "Mild" },
            "Moderate" to currentFlareUps.count { it.severity == "Moderate" },
            "Severe" to currentFlareUps.count { it.severity == "Severe" },
            "Critical" to currentFlareUps.count { it.severity == "Critical" }
        )
    }

    // Time of Day Breakdown
    val timeOfDayDistribution = remember(currentFlareUps) {
        val distribution = mutableMapOf("Morning" to 0, "Afternoon" to 0, "Evening" to 0, "Night" to 0)
        currentFlareUps.forEach {
            val hour = it.timestamp.hour
            val time = when (hour) {
                in 6..11 -> "Morning"
                in 12..17 -> "Afternoon"
                in 18..23 -> "Evening"
                else -> "Night"
            }
            distribution[time] = (distribution[time] ?: 0) + 1
        }
        distribution
    }

    // Medications Breakdown
    val currentMeds = remember(currentEntries) { currentEntries.filter { it.type != EntryType.FLARE_UP } }
    val previousMeds = remember(previousEntries) { previousEntries.filter { it.type != EntryType.FLARE_UP } }

    val medsFrequency = remember(currentMeds) {
        mapOf(
            "Antihistamine" to currentMeds.count { it.type == EntryType.ANTIHISTAMINE },
            "Cortisone" to currentMeds.count { it.type == EntryType.CORTISONE },
            "Xolair" to currentMeds.count { it.type == EntryType.XOLAIR_150 || it.type == EntryType.XOLAIR_300 },
            "Alternative" to currentMeds.count { it.type == EntryType.ALTERNATIVE }
        )
    }

    val mostUsedMedication = remember(currentMeds) {
        if (currentMeds.isEmpty()) "None" else {
            val grouped = currentMeds.groupBy { it.medName }
            grouped.maxByOrNull { it.value.size }?.key ?: "None"
        }
    }

    val adherencePercent = currentStats.adherencePercent
    val previousAdherencePercent = prevStats.adherencePercent
    val adherenceChangePercent = remember(adherencePercent, previousAdherencePercent) {
        if (previousAdherencePercent > 0) {
            ((adherencePercent - previousAdherencePercent).toFloat() / previousAdherencePercent * 100).toInt()
        } else if (adherencePercent > 0) 100 else 0
    }

    // Potential Reasons / Triggers breakdown
    val triggerCounts = remember(currentFlareUps) {
        val counts = mutableMapOf("Food" to 0, "Weather" to 0, "Missed medication" to 0, "Stress" to 0, "Sleep" to 0, "Exercise" to 0, "Allergens" to 0)
        currentFlareUps.forEach { flare ->
            flare.reasons.forEach { r ->
                val lower = r.lowercase()
                val cat = when {
                    lower.startsWith("food trigger:") || lower.startsWith("vegetables:") || lower.startsWith("fruits:") ||
                    lower.startsWith("fruit:") || lower.contains("black coffee") || lower.contains("tea") ||
                    lower.contains("soda") || lower.contains("juice") || lower.contains("alcohol:") ||
                    lower.contains("chocolate") || lower.contains("beef") || lower.contains("pork") ||
                    lower.contains("wheat") || lower.contains("sesame") || lower.contains("additives") ||
                    lower.contains("preservatives") -> "Food"
                    
                    lower.contains("heat") || lower.contains("shower") || lower.contains("cold") || lower.contains("weather") ||
                    lower.contains("wind") || lower.contains("rain") || lower.contains("sun") || lower.contains("overheating") -> "Weather"
                    
                    lower.contains("missed") || lower.contains("wearing off") -> "Missed medication"
                    
                    lower.contains("stress") || lower.contains("worry") || lower.contains("loop") -> "Stress"
                    
                    lower.contains("sleep") || lower.contains("insomnia") || lower.contains("fatigue") || lower.contains("exhaustion") -> "Sleep"
                    
                    lower.contains("exercise") || lower.contains("workout") || lower.contains("gym") || lower.contains("sweat") -> "Exercise"
                    
                    lower.contains("dust") || lower.contains("pollen") || lower.contains("allergen") || lower.contains("pet") ||
                    lower.contains("cat") || lower.contains("dog") -> "Allergens"
                    
                    else -> null
                }
                if (cat != null) {
                    counts[cat] = (counts[cat] ?: 0) + 1
                }
            }
        }
        counts.filter { it.value > 0 }
    }

    val mostCommonTrigger = remember(triggerCounts) {
        if (triggerCounts.isEmpty()) "None" else {
            triggerCounts.maxByOrNull { it.value }?.key ?: "None"
        }
    }

    val mostCommonSymptom = remember(currentFlareUps) {
        val angioCount = currentFlareUps.count { it.hasAngioedema }
        if (totalFlareUps == 0) "None" else {
            if (angioCount.toFloat() / totalFlareUps >= 0.50f) {
                "Swelling & Hives"
            } else {
                "Itching & Hives"
            }
        }
    }

    // AI-Style written report generator
    val aiInsightText = remember(totalFlareUps, currentFlareUps, currentMeds, mostCommonTrigger, timeOfDayDistribution) {
        if (totalFlareUps < 2) {
            "Not enough data yet to identify reliable patterns."
        } else {
            val peakTime = timeOfDayDistribution.maxByOrNull { it.value }?.let {
                if (it.value > 0) it.key.lowercase() else null
            } ?: "evening"

            // Analyze if meds are taken reactively (within 4 hours after a flare-up)
            var reactiveCount = 0
            currentFlareUps.forEach { flare ->
                val flareTime = flare.timestamp
                val hasReactive = currentMeds.any { med ->
                    val diff = Duration.between(flareTime, med.timestamp).seconds
                    diff in 1..(4 * 3600)
                }
                if (hasReactive) reactiveCount++
            }
            val isReactive = (reactiveCount.toFloat() / totalFlareUps) >= 0.40f
            val medicationInsight = if (isReactive) {
                "Antihistamine or rescue medication usage increased shortly after flare-up events, which may suggest that treatments are being utilized reactively rather than preventively."
            } else {
                "Medication logging patterns appear relatively independent of flare-ups, showing a consistent preventive or routine approach."
            }

            val triggerInsight = if (mostCommonTrigger != "None") {
                "The most common potential reason mentioned in your logs was $mostCommonTrigger, which appears related to the onset of symptoms during this period."
            } else {
                "No specific dietary, environmental, or activity-based triggers were consistently linked in your notes during this duration."
            }

            "During the selected period, flare-ups occurred most frequently in the $peakTime. $medicationInsight $triggerInsight Please note that these are potential reasons drawn from correlation in logs and do not constitute a clinical medical diagnosis."
        }
    }

    val profileOnXolairState by viewModel.profileOnXolair.collectAsState()

    val lastBioEntry = remember(parsedEntries) {
        parsedEntries.filter { it.type == EntryType.XOLAIR_150 || it.type == EntryType.XOLAIR_300 }.maxByOrNull { it.timestamp }
    }

    val lastDoseText = remember(lastBioEntry) {
        if (lastBioEntry != null) {
            val days = ChronoUnit.DAYS.between(lastBioEntry.timestamp.toLocalDate(), LocalDate.now())
            if (days == 0L) "Today"
            else if (days == 1L) "Yesterday"
            else "$days days ago"
        } else {
            "Never logged"
        }
    }

    val bioIntervalText = remember(currentEntries) {
        val currentBioEntries = currentEntries
            .filter { it.type == EntryType.XOLAIR_150 || it.type == EntryType.XOLAIR_300 }
            .sortedBy { it.timestamp }

        if (currentBioEntries.size < 2) {
            "Longest: N/A, Shortest: N/A, Average: N/A"
        } else {
            val intervals = mutableListOf<Long>()
            for (i in 0 until currentBioEntries.size - 1) {
                val days = ChronoUnit.DAYS.between(
                    currentBioEntries[i].timestamp.toLocalDate(),
                    currentBioEntries[i + 1].timestamp.toLocalDate()
                )
                intervals.add(days)
            }
            val longest = intervals.maxOrNull() ?: 0L
            val shortest = intervals.minOrNull() ?: 0L
            val average = intervals.average()
            "Longest: $longest days, Shortest: $shortest days, Average: ${String.format("%.1f", average)} days"
        }
    }

    val wearingOffText = remember(currentEntries) {
        val currentBio = currentEntries.filter { it.type == EntryType.XOLAIR_150 || it.type == EntryType.XOLAIR_300 }
        val durations = currentBio.mapNotNull { entry ->
            val metadata = entry.metadata ?: ""
            val parts = metadata.split(":::")
            val wearingOffPart = parts.find { it.startsWith("wearing_off:") }
            if (wearingOffPart != null) {
                val timestampStr = wearingOffPart.removePrefix("wearing_off:")
                try {
                    val wearingOffTime = parseTimestamp(timestampStr)
                    val entryTime = entry.timestamp
                    Duration.between(entryTime, wearingOffTime).toHours() / 24f
                } catch (e: Exception) {
                    null
                }
            } else null
        }
        if (durations.isEmpty()) "N/A"
        else "${String.format("%.1f", durations.average())} days"
    }

    val heatmapSummaryText = remember(currentFlareUps) {
        if (currentFlareUps.isEmpty()) {
            "No flare-up data logged for this period."
        } else {
            val weekdays = listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
            val dayCounts = IntArray(7) { 0 }
            val timeCounts = IntArray(4) { 0 }
            
            currentFlareUps.forEach { flare ->
                val dayIndex = weekdays.indexOf(flare.timestamp.dayOfWeek)
                val hour = flare.timestamp.hour
                val timeIndex = when (hour) {
                    in 6..11 -> 0
                    in 12..17 -> 1
                    in 18..23 -> 2
                    else -> 3
                }
                if (dayIndex != -1) dayCounts[dayIndex]++
                timeCounts[timeIndex]++
            }
            
            val maxDayIdx = dayCounts.indices.maxByOrNull { dayCounts[it] } ?: 0
            val maxTimeIdx = timeCounts.indices.maxByOrNull { timeCounts[it] } ?: 0
            
            val dayNames = listOf("Mondays", "Tuesdays", "Wednesdays", "Thursdays", "Fridays", "Saturdays", "Sundays")
            val timeNames = listOf("Morning (6 AM - 12 PM)", "Afternoon (12 PM - 6 PM)", "Evening (6 PM - 12 AM)", "Night (12 AM - 6 AM)")
            
            "Symptoms were most active on ${dayNames[maxDayIdx]} during the ${timeNames[maxTimeIdx]}."
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .background(DarkCard, shape = CircleShape)
                        .border(1.dp, DarkBorder, CircleShape)
                        .size(40.dp)
                ) {
                    Text(
                        text = "\u2190",
                        color = LightGray,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "\uD83D\uDCCA Pattern Insights",
                    color = Color(0xFF1A7E97),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Period Selector horizontal pills row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        AnalysisPeriod.CURRENT_MONTH to "This Month",
                        AnalysisPeriod.LAST_7_DAYS to "7 Days",
                        AnalysisPeriod.LAST_30_DAYS to "30 Days",
                        AnalysisPeriod.PREVIOUS_MONTH to "Last Month",
                        AnalysisPeriod.CUSTOM to "Custom"
                    ).forEach { (period, label) ->
                        val isSelected = selectedPeriod == period
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isSelected) Color(0xFF1A7E97) else DarkCard,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color(0xFF1A7E97) else DarkBorder,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { selectedPeriod = period }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else MutedGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Custom Date Range Pickers card
                AnimatedVisibility(visible = selectedPeriod == AnalysisPeriod.CUSTOM) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Start Date button
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Start Date", color = MutedGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(DarkCard, RoundedCornerShape(8.dp))
                                        .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                                        .clickable {
                                            DatePickerDialog(
                                                context,
                                                { _, year, month, day ->
                                                    customStartDate = LocalDate.of(year, month + 1, day)
                                                },
                                                customStartDate.year,
                                                customStartDate.monthValue - 1,
                                                customStartDate.dayOfMonth
                                            ).show()
                                        }
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        customStartDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                                        color = LightGray,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text("\uD83D\uDCC5", fontSize = 11.sp)
                                }
                            }

                            // End Date button
                            Column(modifier = Modifier.weight(1f)) {
                                Text("End Date", color = MutedGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(DarkCard, RoundedCornerShape(8.dp))
                                        .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                                        .clickable {
                                            DatePickerDialog(
                                                context,
                                                { _, year, month, day ->
                                                    customEndDate = LocalDate.of(year, month + 1, day)
                                                },
                                                customEndDate.year,
                                                customEndDate.monthValue - 1,
                                                customEndDate.dayOfMonth
                                            ).show()
                                        }
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        customEndDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                                        color = LightGray,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text("\uD83D\uDCC5", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // Compare Insights Button
                Button(
                    onClick = { showComparisonDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1A7E97),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("\uD83D\uDCCA", fontSize = 14.sp)
                        Text("Compare Insights", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Expandable description card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isExpanded = !isExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("\uD83D\uDCA1", fontSize = 13.sp)
                                Text(
                                    text = "About Pattern Insights",
                                    color = LightGray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = if (isExpanded) "\u25B2" else "\u25BC",
                                color = MutedGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = DarkBorder)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Pattern Insights analyzes your logged logs to find correlations between flare-up triggers, severity, timing, and medication intake. These charts and observations can help you understand potential relationships but should never replace formal medical counsel or clinical diagnoses.",
                                color = MutedGray,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Summary cards grid
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Summary Metrics",
                        color = Color(0xFF1A7E97),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Card 1: Total Flare-ups
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = DarkCard),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text("Total Flare-ups", color = MutedGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "$totalFlareUps",
                                        color = LightGray,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (flareUpChangePercent != 0) {
                                        val isIncrease = flareUpChangePercent > 0
                                        Text(
                                            text = if (isIncrease) "\u25B2 +$flareUpChangePercent%" else "\u25BC $flareUpChangePercent%",
                                            color = if (isIncrease) AlertRed else Color(0xFF509729),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "vs $compPeriodLabel",
                                    color = MutedGray,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Card 2: Average Flareups / Week
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = DarkCard),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text("Avg Flare-ups / Wk", color = MutedGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = String.format("%.1f", avgFlareUpsPerWeek),
                                        color = LightGray,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (avgFlareUpChangePercent != 0) {
                                        val isIncrease = avgFlareUpChangePercent > 0
                                        Text(
                                            text = if (isIncrease) "\u25B2 +$avgFlareUpChangePercent%" else "\u25BC $avgFlareUpChangePercent%",
                                            color = if (isIncrease) AlertRed else Color(0xFF509729),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "vs $compPeriodLabel",
                                    color = MutedGray,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Card 3: Angioedema Rate
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = DarkCard),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text("Angioedema Rate", color = MutedGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val angioRate = currentStats.angioedemaPercent
                                    val prevAngioRate = prevStats.angioedemaPercent
                                    val angioChangePercent = if (prevAngioRate > 0) {
                                        ((angioRate - prevAngioRate).toFloat() / prevAngioRate * 100).toInt()
                                    } else if (angioRate > 0) 100 else 0
                                    
                                    Text(
                                        text = "$angioRate%",
                                        color = LightGray,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (angioChangePercent != 0) {
                                        val isIncrease = angioChangePercent > 0
                                        Text(
                                            text = if (isIncrease) "\u25B2 +$angioChangePercent%" else "\u25BC $angioChangePercent%",
                                            color = if (isIncrease) AlertRed else Color(0xFF509729),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "vs $compPeriodLabel",
                                    color = MutedGray,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Card 4: Medication Adherence
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = DarkCard),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text("Med Adherence Rate", color = MutedGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "$adherencePercent%",
                                        color = LightGray,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (adherenceChangePercent != 0) {
                                        val isIncrease = adherenceChangePercent > 0
                                        Text(
                                            text = if (isIncrease) "\u25B2 +$adherenceChangePercent%" else "\u25BC $adherenceChangePercent%",
                                            color = if (isIncrease) Color(0xFF509729) else AlertRed,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "vs $compPeriodLabel",
                                    color = MutedGray,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Card 5: Most Common triggers (Full Width)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text("Top Triggers", color = MutedGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            val sortedTriggers = remember(triggerCounts) {
                                triggerCounts.toList().sortedByDescending { it.second }
                            }
                            
                            if (sortedTriggers.isEmpty()) {
                                Text(
                                    text = "None",
                                    color = LightGray,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                val topTrigger = sortedTriggers[0]
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = topTrigger.first,
                                        color = LightGray,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "(${topTrigger.second} time${if (topTrigger.second > 1) "s" else ""})",
                                        color = Color(0xFF1A7E97),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }
                                
                                val secondTrigger = sortedTriggers.getOrNull(1)
                                val thirdTrigger = sortedTriggers.getOrNull(2)
                                
                                if (secondTrigger != null || thirdTrigger != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (secondTrigger != null) {
                                            Text(
                                                text = "2nd: ${secondTrigger.first} (${secondTrigger.second}x)",
                                                color = MutedGray,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        if (thirdTrigger != null) {
                                            Text(
                                                text = "3rd: ${thirdTrigger.first} (${thirdTrigger.second}x)",
                                                color = MutedGray,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Biological Treatment Status & Wearing Off Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("\uD83D\uDC89", fontSize = 14.sp)
                            Text(
                                text = "Biological Treatment Status",
                                color = Color(0xFF1A7E97),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        HorizontalDivider(color = DarkBorder)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Treatment Status", color = MutedGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (profileOnXolairState) "Active" else "Inactive",
                                    color = if (profileOnXolairState) Color(0xFF509729) else MutedGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Last Dose Taken", color = MutedGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = lastDoseText,
                                    color = LightGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Intake Interval (Chosen Period)", color = MutedGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = bioIntervalText,
                                    color = LightGray,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Avg Wearing Off Duration", color = MutedGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = wearingOffText,
                                    color = LightGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Medication Intake Summary Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("\uD83D\uDC8A", fontSize = 14.sp)
                            Text(
                                text = "Medication Intake Summary",
                                color = Color(0xFF1A7E97),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        HorizontalDivider(color = DarkBorder)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Antihistamines
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Antihistamines Doses", color = MutedGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                val antihistamineCount = currentStats.antihistamineCount
                                val prevAntihistamineCount = prevStats.antihistamineCount
                                val antiDiff = antihistamineCount - prevAntihistamineCount
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "$antihistamineCount",
                                        color = LightGray,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (antiDiff != 0) {
                                        Text(
                                            text = if (antiDiff > 0) "\u25B2 +$antiDiff" else "\u25BC $antiDiff",
                                            color = if (antiDiff > 0) AlertRed else Color(0xFF509729),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("vs $compPeriodLabel ($prevAntihistamineCount)", color = MutedGray, fontSize = 8.sp)
                            }
                            
                            // Corticosteroids
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Corticosteroids Doses", color = MutedGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                val cortisoneCount = currentStats.cortisoneCount
                                val prevCortisoneCount = prevStats.cortisoneCount
                                val cortDiff = cortisoneCount - prevCortisoneCount
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "$cortisoneCount",
                                        color = LightGray,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (cortDiff != 0) {
                                        Text(
                                            text = if (cortDiff > 0) "\u25B2 +$cortDiff" else "\u25BC $cortDiff",
                                            color = if (cortDiff > 0) AlertRed else Color(0xFF509729),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("vs $compPeriodLabel ($prevCortisoneCount)", color = MutedGray, fontSize = 8.sp)
                            }
                        }
                    }
                }

                // AI insight report card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF1A7E97).copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A7E97).copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("\uD83E\uDD16", fontSize = 14.sp)
                            Text(
                                text = "Pattern Observations",
                                color = Color(0xFF1A7E97),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = aiInsightText,
                            color = LightGray,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // If no flare-ups data, show fallback message for graphs
                if (totalFlareUps == 0 && currentMeds.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Not enough data yet to identify reliable patterns.",
                            color = MutedGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // Charts Section

                    // 1. Flare-ups over time line chart
                    if (totalFlareUps > 0) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Flare-ups Trend",
                                color = Color(0xFF1A7E97),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = DarkCard),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    LineChart(
                                        flareUps = currentFlareUps,
                                        startDate = currentStart,
                                        endDate = currentEnd,
                                        themeColor = Color(0xFF1A7E97)
                                    )
                                }
                            }
                        }
                    }

                    // 2. Medication counts bar chart
                    if (currentMeds.isNotEmpty()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Medication Intake Count",
                                color = Color(0xFF1A7E97),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = DarkCard),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    BarChart(
                                        medsMap = medsFrequency,
                                        themeColor = Color(0xFF1A7E97)
                                    )
                                }
                            }
                        }
                    }

                    // 3. Severity Pie/Donut Chart & Trigger Breakdown together
                    if (totalFlareUps > 0) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Flare-up Severity Distribution",
                                color = Color(0xFF1A7E97),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = DarkCard),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    DonutChart(
                                        severityMap = severityDistribution
                                    )
                                }
                            }
                        }

                        // 4. Heatmap View
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Flare-up Heatmap (Day of Week vs Time of Day)",
                                color = Color(0xFF1A7E97),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = DarkCard),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    HeatmapGrid(
                                        flareUps = currentFlareUps,
                                        themeColor = Color(0xFF1A7E97)
                                    )
                                    HorizontalDivider(color = DarkBorder)
                                    Text(
                                        text = heatmapSummaryText,
                                        color = LightGray,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // Timeline Log View
                if (currentEntries.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Timeline View (Recent events)",
                            color = Color(0xFF1A7E97),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        val sortedTimeline = remember(currentEntries) {
                            currentEntries.sortedByDescending { it.timestamp }.take(10)
                        }

                        sortedTimeline.forEach { entry ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, DarkBorder, RoundedCornerShape(10.dp)),
                                colors = CardDefaults.cardColors(containerColor = DarkCard),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Icon Circle
                                    val (emoji, bg) = when (entry.type) {
                                        EntryType.FLARE_UP -> Pair("\uD83D\uDCCA", Color(0xFF1A7E97).copy(alpha = 0.15f))
                                        EntryType.ANTIHISTAMINE -> Pair("\uD83D\uDC8A", Color(0xFF509729).copy(alpha = 0.15f))
                                        EntryType.CORTISONE -> Pair("\uD83D\uDC8A", Color(0xFFEF5350).copy(alpha = 0.15f))
                                        else -> Pair("\uD83D\uDC89", Color(0xFF1A7E97).copy(alpha = 0.15f))
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(bg, shape = CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(emoji, fontSize = 14.sp)
                                    }

                                    // Content text
                                    Column(modifier = Modifier.weight(1f)) {
                                        val title = when (entry.type) {
                                            EntryType.FLARE_UP -> "${entry.severity} Flare-up"
                                            else -> entry.medName
                                        }
                                        Text(
                                            text = title,
                                            color = LightGray,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val subtitle = when (entry.type) {
                                            EntryType.FLARE_UP -> {
                                                val details = entry.reasons.joinToString(", ")
                                                if (details.isNotEmpty()) details else "No trigger recorded"
                                            }
                                            else -> if (entry.medMgs.isNotEmpty()) "${entry.medMgs} Dosage" else "Routine Intake"
                                        }
                                        Text(
                                            text = subtitle,
                                            color = MutedGray,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Timestamp
                                    Text(
                                        text = entry.timestamp.format(DateTimeFormatter.ofPattern("MMM dd, h:mm a")),
                                        color = MutedGray,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Comparison Overlay Dialog
        AnimatedVisibility(
            visible = showComparisonDialog,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(targetOffsetY = { it })
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AmoledBlack)
                    .statusBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showComparisonDialog = false },
                            modifier = Modifier
                                .background(DarkCard, shape = CircleShape)
                                .border(1.dp, DarkBorder, CircleShape)
                                .size(40.dp)
                        ) {
                            Text(
                                text = "\u2190",
                                color = LightGray,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Compare Insights",
                            color = Color(0xFF1A7E97),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Preset buttons row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            "this_vs_last_month" to "This vs Last Month",
                            "7d_vs_prev_7d" to "7 Days vs Prev 7 Days",
                            "30d_vs_prev_30d" to "30 Days vs Prev 30 Days",
                            "custom" to "Custom Comparison"
                        ).forEach { (preset, label) ->
                            val isSelected = comparisonPreset == preset
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isSelected) Color(0xFF1A7E97) else DarkCard,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) Color(0xFF1A7E97) else DarkBorder,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .clickable { comparisonPreset = preset }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else MutedGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Date pickers for Period A and Period B if custom
                    if (comparisonPreset == "custom") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("Period A Selection", color = LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Start Date", color = MutedGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(DarkCard, RoundedCornerShape(8.dp))
                                                .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                                                .clickable {
                                                    DatePickerDialog(
                                                        context,
                                                        { _, year, month, day ->
                                                            compPeriodAStart = LocalDate.of(year, month + 1, day)
                                                        },
                                                        compPeriodAStart.year,
                                                        compPeriodAStart.monthValue - 1,
                                                        compPeriodAStart.dayOfMonth
                                                    ).show()
                                                }
                                                .padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(compPeriodAStart.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")), color = LightGray, fontSize = 10.sp)
                                            Text("\uD83D\uDCC5", fontSize = 10.sp)
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("End Date", color = MutedGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(DarkCard, RoundedCornerShape(8.dp))
                                                .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                                                .clickable {
                                                    DatePickerDialog(
                                                        context,
                                                        { _, year, month, day ->
                                                            compPeriodAEnd = LocalDate.of(year, month + 1, day)
                                                        },
                                                        compPeriodAEnd.year,
                                                        compPeriodAEnd.monthValue - 1,
                                                        compPeriodAEnd.dayOfMonth
                                                    ).show()
                                                }
                                                .padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(compPeriodAEnd.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")), color = LightGray, fontSize = 10.sp)
                                            Text("\uD83D\uDCC5", fontSize = 10.sp)
                                        }
                                    }
                                }

                                HorizontalDivider(color = DarkBorder)

                                Text("Period B Selection", color = LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Start Date", color = MutedGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(DarkCard, RoundedCornerShape(8.dp))
                                                .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                                                .clickable {
                                                    DatePickerDialog(
                                                        context,
                                                        { _, year, month, day ->
                                                            compPeriodBStart = LocalDate.of(year, month + 1, day)
                                                        },
                                                        compPeriodBStart.year,
                                                        compPeriodBStart.monthValue - 1,
                                                        compPeriodBStart.dayOfMonth
                                                    ).show()
                                                }
                                                .padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(compPeriodBStart.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")), color = LightGray, fontSize = 10.sp)
                                            Text("\uD83D\uDCC5", fontSize = 10.sp)
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("End Date", color = MutedGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(DarkCard, RoundedCornerShape(8.dp))
                                                .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                                                .clickable {
                                                    DatePickerDialog(
                                                        context,
                                                        { _, year, month, day ->
                                                            compPeriodBEnd = LocalDate.of(year, month + 1, day)
                                                        },
                                                        compPeriodBEnd.year,
                                                        compPeriodBEnd.monthValue - 1,
                                                        compPeriodBEnd.dayOfMonth
                                                    ).show()
                                                }
                                                .padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(compPeriodBEnd.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")), color = LightGray, fontSize = 10.sp)
                                            Text("\uD83D\uDCC5", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Side-by-side comparison table
                    val rangeAEntries = remember(parsedEntries, compRangeA) {
                        parsedEntries.filter { it.timestamp.isAfter(compRangeA.first) && it.timestamp.isBefore(compRangeA.second) }
                    }
                    val rangeBEntries = remember(parsedEntries, compRangeB) {
                        parsedEntries.filter { it.timestamp.isAfter(compRangeB.first) && it.timestamp.isBefore(compRangeB.second) }
                    }

                    val statsA = remember(rangeAEntries, compRangeA) {
                        calculatePeriodStats(rangeAEntries, compRangeA.first, compRangeA.second)
                    }
                    val statsB = remember(rangeBEntries, compRangeB) {
                        calculatePeriodStats(rangeBEntries, compRangeB.first, compRangeB.second)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .border(1.dp, DarkBorder, RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("Comparison Table", color = LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                            // Table columns
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Metric", color = MutedGray, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Text("Period A", color = MutedGray, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                Text("Period B", color = MutedGray, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                Text("Change", color = MutedGray, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
                            }
                            HorizontalDivider(color = DarkBorder)

                            // Metric row helper
                            @Composable
                            fun ComparisonRow(
                                label: String,
                                valA: String,
                                valB: String,
                                changeText: String,
                                changeColor: Color
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(label, color = LightGray, fontSize = 10.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                    Text(valA, color = LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                    Text(valB, color = LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                    Text(changeText, color = changeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
                                }
                            }

                            // 1. Total Flareups
                            val flareDiffPercent = if (statsB.totalFlares > 0) {
                                ((statsA.totalFlares - statsB.totalFlares).toFloat() / statsB.totalFlares * 100).toInt()
                            } else if (statsA.totalFlares > 0) 100 else 0
                            val flareDiffColor = when {
                                flareDiffPercent < 0 -> Color(0xFF509729) // improvement (green)
                                flareDiffPercent > 0 -> AlertRed // deterioration (red)
                                else -> LightGray
                            }
                            val flareDiffText = if (flareDiffPercent > 0) "\u25B2 +$flareDiffPercent%" else if (flareDiffPercent < 0) "\u25BC $flareDiffPercent%" else "0%"

                            ComparisonRow(
                                label = "Total Flare-ups",
                                valA = "${statsA.totalFlares}",
                                valB = "${statsB.totalFlares}",
                                changeText = flareDiffText,
                                changeColor = flareDiffColor
                            )
                            HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))

                            // 2. Avg Flareups per Week
                            val avgDiffPercent = if (statsB.avgFlaresPerWeek > 0f) {
                                (((statsA.avgFlaresPerWeek - statsB.avgFlaresPerWeek) / statsB.avgFlaresPerWeek) * 100).toInt()
                            } else if (statsA.avgFlaresPerWeek > 0f) 100 else 0
                            val avgDiffColor = when {
                                avgDiffPercent < 0 -> Color(0xFF509729)
                                avgDiffPercent > 0 -> AlertRed
                                else -> LightGray
                            }
                            val avgDiffText = if (avgDiffPercent > 0) "\u25B2 +$avgDiffPercent%" else if (avgDiffPercent < 0) "\u25BC $avgDiffPercent%" else "0%"

                            ComparisonRow(
                                label = "Avg Flare-ups / Wk",
                                valA = String.format("%.1f", statsA.avgFlaresPerWeek),
                                valB = String.format("%.1f", statsB.avgFlaresPerWeek),
                                changeText = avgDiffText,
                                changeColor = avgDiffColor
                            )
                            HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))

                            // 3. Med Adherence
                            val adherenceDiffPercent = if (statsB.adherencePercent > 0) {
                                ((statsA.adherencePercent - statsB.adherencePercent).toFloat() / statsB.adherencePercent * 100).toInt()
                            } else if (statsA.adherencePercent > 0) 100 else 0
                            val adherenceDiffColor = when {
                                adherenceDiffPercent > 0 -> Color(0xFF509729) // improvement (green)
                                adherenceDiffPercent < 0 -> AlertRed // deterioration (red)
                                else -> LightGray
                            }
                            val adherenceDiffText = if (adherenceDiffPercent > 0) "\u25B2 +$adherenceDiffPercent%" else if (adherenceDiffPercent < 0) "\u25BC $adherenceDiffPercent%" else "0%"

                            ComparisonRow(
                                label = "Med Adherence",
                                valA = "${statsA.adherencePercent}%",
                                valB = "${statsB.adherencePercent}%",
                                changeText = adherenceDiffText,
                                changeColor = adherenceDiffColor
                            )
                            HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))

                            // 4. Angioedema Rate
                            val angioDiffPercent = if (statsB.angioedemaPercent > 0) {
                                ((statsA.angioedemaPercent - statsB.angioedemaPercent).toFloat() / statsB.angioedemaPercent * 100).toInt()
                            } else if (statsA.angioedemaPercent > 0) 100 else 0
                            val angioDiffColor = when {
                                angioDiffPercent < 0 -> Color(0xFF509729)
                                angioDiffPercent > 0 -> AlertRed
                                else -> LightGray
                            }
                            val angioDiffText = if (angioDiffPercent > 0) "\u25B2 +$angioDiffPercent%" else if (angioDiffPercent < 0) "\u25BC $angioDiffPercent%" else "0%"

                            ComparisonRow(
                                label = "Angioedema Rate",
                                valA = "${statsA.angioedemaPercent}%",
                                valB = "${statsB.angioedemaPercent}%",
                                changeText = angioDiffText,
                                changeColor = angioDiffColor
                            )
                            HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))

                            // Antihistamines Count Row
                            val antiDiff = statsA.antihistamineCount - statsB.antihistamineCount
                            val antiDiffPercent = if (statsB.antihistamineCount > 0) {
                                (antiDiff.toFloat() / statsB.antihistamineCount * 100).toInt()
                            } else if (statsA.antihistamineCount > 0) 100 else 0
                            val antiDiffColor = when {
                                antiDiffPercent < 0 -> Color(0xFF509729)
                                antiDiffPercent > 0 -> AlertRed
                                else -> LightGray
                            }
                            val antiDiffText = if (antiDiffPercent > 0) "\u25B2 +$antiDiffPercent%" else if (antiDiffPercent < 0) "\u25BC $antiDiffPercent%" else "0%"

                            ComparisonRow(
                                label = "Antihistamine Doses",
                                valA = "${statsA.antihistamineCount}",
                                valB = "${statsB.antihistamineCount}",
                                changeText = antiDiffText,
                                changeColor = antiDiffColor
                            )
                            HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))

                            // Corticosteroids Count Row
                            val cortDiff = statsA.cortisoneCount - statsB.cortisoneCount
                            val cortDiffPercent = if (statsB.cortisoneCount > 0) {
                                (cortDiff.toFloat() / statsB.cortisoneCount * 100).toInt()
                            } else if (statsA.cortisoneCount > 0) 100 else 0
                            val cortDiffColor = when {
                                cortDiffPercent < 0 -> Color(0xFF509729)
                                cortDiffPercent > 0 -> AlertRed
                                else -> LightGray
                            }
                            val cortDiffText = if (cortDiffPercent > 0) "\u25B2 +$cortDiffPercent%" else if (cortDiffPercent < 0) "\u25BC $cortDiffPercent%" else "0%"

                            ComparisonRow(
                                label = "Corticosteroid Doses",
                                valA = "${statsA.cortisoneCount}",
                                valB = "${statsB.cortisoneCount}",
                                changeText = cortDiffText,
                                changeColor = cortDiffColor
                            )
                            HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))

                            // 5. Top Trigger & Medication
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Top Trigger", color = LightGray, fontSize = 10.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                Text(statsA.topTrigger, color = LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(statsB.topTrigger, color = LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("-", color = MutedGray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
                            }
                            HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Top Medication", color = LightGray, fontSize = 10.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                Text(statsA.topMedication, color = LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(statsB.topMedication, color = LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("-", color = MutedGray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LineChart(
    flareUps: List<ParsedLogEntry>,
    startDate: ZonedDateTime,
    endDate: ZonedDateTime,
    themeColor: Color
) {
    val dataPoints = remember(flareUps, startDate, endDate) {
        val days = maxOf(1L, ChronoUnit.DAYS.between(startDate, endDate))
        val intervalsCount = 7
        val intervalDays = maxOf(1f, days / intervalsCount.toFloat())
        
        val points = mutableListOf<Float>()
        for (i in 0 until intervalsCount) {
            val chunkStart = startDate.plusDays((i * intervalDays).toLong())
            val chunkEnd = startDate.plusDays(((i + 1) * intervalDays).toLong())
            val count = flareUps.count { it.timestamp.isAfter(chunkStart) && it.timestamp.isBefore(chunkEnd) }
            points.add(count.toFloat())
        }
        points
    }

    val maxVal = remember(dataPoints) {
        maxOf(1f, dataPoints.maxOrNull() ?: 1f)
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
    ) {
        val width = size.width
        val height = size.height
        val padding = 16.dp.toPx()
        
        val usableWidth = width - (padding * 2)
        val usableHeight = height - (padding * 2)

        // Draw helper grid lines
        val gridLines = 3
        for (i in 0..gridLines) {
            val y = padding + (usableHeight / gridLines) * i
            drawLine(
                color = DarkBorder,
                start = Offset(padding, y),
                end = Offset(width - padding, y),
                strokeWidth = 0.5.dp.toPx()
            )
        }

        if (dataPoints.isNotEmpty()) {
            val stepX = usableWidth / (dataPoints.size - 1)
            val path = Path()
            val fillPath = Path()

            dataPoints.forEachIndexed { index, value ->
                val x = padding + (index * stepX)
                val y = height - padding - ((value / maxVal) * usableHeight)
                
                if (index == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, height - padding)
                    fillPath.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
                
                if (index == dataPoints.size - 1) {
                    fillPath.lineTo(x, height - padding)
                    fillPath.close()
                }

                // Draw dots on each node
                drawCircle(
                    color = themeColor,
                    radius = 3.5.dp.toPx(),
                    center = Offset(x, y)
                )
            }

            // Draw line
            drawPath(
                path = path,
                color = themeColor,
                style = Stroke(width = 2.dp.toPx())
            )

            // Draw gradient fill
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(themeColor.copy(alpha = 0.25f), Color.Transparent)
                )
            )
        }
    }
}

@Composable
fun BarChart(
    medsMap: Map<String, Int>,
    themeColor: Color
) {
    val items = remember(medsMap) { medsMap.toList() }
    val maxVal = remember(items) {
        maxOf(1f, items.maxOf { it.second.toFloat() })
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        items.forEach { (label, count) ->
            val fraction = count.toFloat() / maxVal
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .fillMaxHeight(fraction * 0.75f)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(themeColor, themeColor.copy(alpha = 0.6f))
                            ),
                            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                        )
                )
                Text(
                    text = "$count",
                    color = LightGray,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = label,
                    color = MutedGray,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun DonutChart(
    severityMap: Map<String, Int>
) {
    val severities = remember(severityMap) { severityMap.toList() }
    val total = remember(severities) { severities.sumOf { it.second } }

    val colors = listOf(
        Color(0xFF26A69A), // Mild (Teal)
        Color(0xFFFFA726), // Moderate (Orange)
        Color(0xFFEF5350), // Severe (Red)
        Color(0xFFAB47BC)  // Critical (Purple)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(
            modifier = Modifier
                .size(100.dp)
                .weight(1f)
        ) {
            val strokeWidth = 12.dp.toPx()
            if (total == 0) {
                drawCircle(
                    color = DarkBorder,
                    radius = (size.minDimension - strokeWidth) / 2,
                    style = Stroke(width = strokeWidth)
                )
            } else {
                var startAngle = -90f
                severities.forEachIndexed { index, (_, count) ->
                    if (count > 0) {
                        val sweepAngle = (count.toFloat() / total) * 360f
                        drawArc(
                            color = colors[index % colors.size],
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = strokeWidth)
                        )
                        startAngle += sweepAngle
                    }
                }
            }
        }

        // Legend Column
        Column(
            modifier = Modifier.weight(1.2f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            severities.forEachIndexed { index, (label, count) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(colors[index % colors.size], RoundedCornerShape(2.dp))
                    )
                    val percent = if (total > 0) (count.toFloat() / total * 100).toInt() else 0
                    Text(
                        text = "$label: $count ($percent%)",
                        color = LightGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun HeatmapGrid(
    flareUps: List<ParsedLogEntry>,
    themeColor: Color
) {
    // 4 rows (Morning, Afternoon, Evening, Night) x 7 columns (Mon, Tue, Wed, Thu, Fri, Sat, Sun)
    val weekdays = listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
    val times = listOf("Morning", "Afternoon", "Evening", "Night")

    val gridCounts = remember(flareUps) {
        val counts = Array(4) { IntArray(7) { 0 } }
        flareUps.forEach { flare ->
            val dayIndex = weekdays.indexOf(flare.timestamp.dayOfWeek)
            val hour = flare.timestamp.hour
            val timeIndex = when (hour) {
                in 6..11 -> 0
                in 12..17 -> 1
                in 18..23 -> 2
                else -> 3
            }
            if (dayIndex != -1) {
                counts[timeIndex][dayIndex]++
            }
        }
        counts
    }

    val maxCount = remember(gridCounts) {
        var max = 1
        for (r in 0..3) {
            for (c in 0..6) {
                max = maxOf(max, gridCounts[r][c])
            }
        }
        max.toFloat()
    }

    val dayLetters = listOf("M", "T", "W", "T", "F", "S", "S")

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Weekday headers row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Row(
                modifier = Modifier.width(180.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dayLetters.forEach { letter ->
                    Text(
                        text = letter,
                        color = MutedGray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(22.dp)
                    )
                }
            }
        }

        // Grid rows
        times.forEachIndexed { timeIndex, timeLabel ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeLabel,
                    color = LightGray,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(65.dp)
                )

                Row(
                    modifier = Modifier.width(180.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (dayIndex in 0..6) {
                        val count = gridCounts[timeIndex][dayIndex]
                        val cellAlpha = if (count > 0) {
                            0.15f + (count.toFloat() / maxCount) * 0.8f
                        } else 0.04f
                        val cellColor = if (count > 0) themeColor.copy(alpha = cellAlpha) else DarkBorder

                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(cellColor, RoundedCornerShape(4.dp))
                                .border(0.5.dp, DarkBorder.copy(alpha = 0.5f), RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (count > 0) {
                                Text(
                                    text = "$count",
                                    color = if (cellAlpha > 0.5f) Color.White else LightGray,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


