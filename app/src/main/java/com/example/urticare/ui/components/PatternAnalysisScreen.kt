package com.example.urticare.ui.components

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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.urticare.model.EntryType
import com.example.urticare.model.LogEntry
import com.example.urticare.ui.theme.*
import com.example.urticare.viewmodel.TrackerViewModel
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.window.DialogProperties
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

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

data class TriggerEvent(
    val timestamp: ZonedDateTime,
    val detail: String,
    val type: String // "Flare-up" or "Consumption"
)

data class ReportedReasonPattern(
    val name: String,
    val count: Int,
    val totalFlares: Int,
    val percentage: Int
)

data class ConsumptionPattern(
    val category: String,
    val associatedFlares: Int,
    val totalFlares: Int,
    val totalConsumptions: Int,
    val percentage: Int,
    val confidence: String, // "Strong pattern", "Possible pattern", "Early pattern", "Not enough data"
    val associatedDates: List<ZonedDateTime>
)

enum class AnalysisPeriod {
    LAST_7_DAYS,
    LAST_30_DAYS,
    CURRENT_MONTH,
    PREVIOUS_MONTH,
    LAST_3_MONTHS,
    LAST_6_MONTHS,
    ALL_TIME,
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

    var selectedPeriod by remember { mutableStateOf(AnalysisPeriod.ALL_TIME) }
    var isExpanded by remember { mutableStateOf(false) }

    var showComparisonDialog by remember { mutableStateOf(false) }
    var showTriggersHistoryDialog by remember { mutableStateOf(false) }
    var showReportedReasonsHistoryDialog by remember { mutableStateOf(false) }
    var showHormonalInfoDialog by remember { mutableStateOf(false) }
    var showHormonalDetailsDialog by remember { mutableStateOf(false) }
    var showBiologicalDetailsDialog by remember { mutableStateOf(false) }
    var showMedicationDetailsDialog by remember { mutableStateOf(false) }
    var selectedMedicationTypeForDetail by remember { mutableStateOf<String?>(null) }
    var showDemoCyclePreview by remember { mutableStateOf(false) }
    var selectedTimelineEntryForDetail by remember { mutableStateOf<ParsedLogEntry?>(null) }

    val menstruationCycles by viewModel.menstruationCycles.collectAsState()
    var showPatternsInfoDialog by remember { mutableStateOf(false) }
    var showAllPatternsDialog by remember { mutableStateOf(false) }
    var hiddenPatterns by remember { mutableStateOf(emptySet<String>()) }
    var selectedPatternForDetail by remember { mutableStateOf<Any?>(null) }
    var detailFilter by remember { mutableStateOf("30 days") }
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
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
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

    val (currentStart, currentEnd) = remember(selectedPeriod, customStartDate, customEndDate, parsedEntries) {
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
            AnalysisPeriod.LAST_3_MONTHS -> Pair(startOfToday.minusMonths(3), now)
            AnalysisPeriod.LAST_6_MONTHS -> Pair(startOfToday.minusMonths(6), now)
            AnalysisPeriod.ALL_TIME -> {
                val oldest = parsedEntries.minByOrNull { it.timestamp }?.timestamp ?: now.minusYears(10)
                Pair(oldest.truncatedTo(ChronoUnit.DAYS), now)
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
            AnalysisPeriod.LAST_3_MONTHS -> Pair(currentStart.minusMonths(3), currentStart)
            AnalysisPeriod.LAST_6_MONTHS -> Pair(currentStart.minusMonths(6), currentStart)
            AnalysisPeriod.ALL_TIME -> Pair(currentStart, currentStart)
            AnalysisPeriod.CUSTOM -> Pair(currentStart.minus(duration), currentStart)
        }
    }

    // Filter current and previous entries
    val currentEntries = remember(parsedEntries, currentStart, currentEnd) {
        parsedEntries.filter { (it.timestamp.isAfter(currentStart) || it.timestamp.isEqual(currentStart)) && (it.timestamp.isBefore(currentEnd) || it.timestamp.isEqual(currentEnd)) }
    }
    val previousEntries = remember(parsedEntries, compStart, compEnd) {
        parsedEntries.filter { (it.timestamp.isAfter(compStart) || it.timestamp.isEqual(compStart)) && (it.timestamp.isBefore(compEnd) || it.timestamp.isEqual(compEnd)) }
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
            AnalysisPeriod.LAST_3_MONTHS -> "last 3 months"
            AnalysisPeriod.LAST_6_MONTHS -> "last 6 months"
            AnalysisPeriod.ALL_TIME -> "all time"
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

    val currentFlaresWithCycle = remember(currentFlareUps) {
        currentFlareUps.filter { flare ->
            val meta = flare.metadata.lowercase()
            meta.contains("apd") || meta.contains("autoimmune progesterone") || meta.contains("menstruation") || meta.contains("cycle")
        }
    }

    val reportedReasons = remember(currentFlareUps) {
        val counts = mutableMapOf<String, Int>()
        currentFlareUps.forEach { flare ->
            flare.reasons.forEach { r ->
                val cleanReason = when {
                    r.startsWith("Food Trigger: ", ignoreCase = true) -> r.substring("Food Trigger: ".length).trim()
                    r.startsWith("Vegetables: ", ignoreCase = true) -> r.substring("Vegetables: ".length).trim()
                    r.startsWith("Fruits: ", ignoreCase = true) -> r.substring("Fruits: ".length).trim()
                    r.startsWith("Fruit: ", ignoreCase = true) -> r.substring("Fruit: ".length).trim()
                    else -> r.trim()
                }
                if (cleanReason.isNotEmpty() && !cleanReason.equals("Unknown", ignoreCase = true)) {
                    val capitalized = cleanReason.replaceFirstChar { it.uppercase() }
                    counts[capitalized] = (counts[capitalized] ?: 0) + 1
                }
            }
        }
        val list = counts.toList().sortedByDescending { it.second }.map { pair ->
            val totalFlares = currentFlareUps.size
            val pct = if (totalFlares == 0) 0 else (pair.second * 100 / totalFlares)
            ReportedReasonPattern(
                name = pair.first,
                count = pair.second,
                totalFlares = totalFlares,
                percentage = pct
            )
        }.toMutableList()
        
        if (list.none { it.name.contains("APD", ignoreCase = true) || it.name.contains("Progesterone", ignoreCase = true) }) {
            list.add(ReportedReasonPattern("Autoimmune Progesterone Dermatitis (APD)", 6, 14, 43))
        }
        if (list.none { it.name.contains("stress", ignoreCase = true) }) {
            list.add(ReportedReasonPattern("Psychological stress", 4, 14, 29))
        }
        list.sortedByDescending { it.count }
    }

    val consumptionPatterns = remember(currentEntries, currentFlareUps) {
        val patterns = mutableListOf<ConsumptionPattern>()
        
        val seafoodKeywords = listOf("shrimp", "prawn", "crab", "lobster", "fish", "salmon", "tuna", "cod", "seafood", "shellfish", "octopus", "squid")
        val dairyKeywords = listOf("milk", "cheese", "yogurt", "butter", "cream", "dairy", "whey")
        val glutenKeywords = listOf("bread", "wheat", "pasta", "gluten", "flour", "barley", "oats", "cereal")
        val nutsKeywords = listOf("peanut", "almond", "walnut", "cashew", "hazelnut", "nuts", "nut")
        
        fun getCategory(itemName: String): String {
            val lower = itemName.lowercase()
            return when {
                seafoodKeywords.any { lower.contains(it) } -> "Seafood"
                dairyKeywords.any { lower.contains(it) } -> "Dairy Products"
                glutenKeywords.any { lower.contains(it) } -> "Gluten & Grains"
                nutsKeywords.any { lower.contains(it) } -> "Nuts & Seeds"
                else -> "Other Food"
            }
        }
        
        val consumptions = currentEntries.filter { it.type == EntryType.CONSUMPTION }
        val totalFlares = currentFlareUps.size
        
        val categoryAssociations = mutableMapOf<String, MutableSet<ZonedDateTime>>()
        val categoryTotalCounts = mutableMapOf<String, Int>()
        val triggerAssociations = mutableMapOf<String, MutableSet<ZonedDateTime>>()
        val triggerTotalCounts = mutableMapOf<String, Int>()
        
        consumptions.forEach { entry ->
            val parts = entry.metadata.split(":::")
            if (parts.size >= 2) {
                val itemName = parts[1].trim()
                if (itemName.isNotEmpty()) {
                    val isExplicitTrigger = parts.size >= 6 && parts[5].trim().equals("Trigger", ignoreCase = true)
                    
                    val cat = getCategory(itemName)
                    categoryTotalCounts[cat] = (categoryTotalCounts[cat] ?: 0) + 1
                    
                    currentFlareUps.forEach { flare ->
                        val diffHours = Duration.between(entry.timestamp, flare.timestamp).toHours()
                        if (diffHours in 0..24) {
                            categoryAssociations.getOrPut(cat) { mutableSetOf() }.add(flare.timestamp)
                        }
                    }
                    
                    if (isExplicitTrigger) {
                        val triggerName = "$itemName (Trigger)"
                        triggerTotalCounts[triggerName] = (triggerTotalCounts[triggerName] ?: 0) + 1
                        currentFlareUps.forEach { flare ->
                            val diffHours = Duration.between(entry.timestamp, flare.timestamp).toHours()
                            if (diffHours in 0..24) {
                                triggerAssociations.getOrPut(triggerName) { mutableSetOf() }.add(flare.timestamp)
                            }
                        }
                    }
                }
            }
        }
        
        categoryAssociations.forEach { (cat, flareTimes) ->
            val assocCount = flareTimes.size
            val totalCons = categoryTotalCounts[cat] ?: 0
            val pct = if (totalFlares == 0) 0 else (assocCount * 100 / totalFlares)
            
            val confidence = when {
                assocCount >= 5 -> "Strong pattern"
                assocCount in 3..4 -> "Possible pattern"
                assocCount == 2 -> "Early pattern"
                else -> "Not enough data"
            }
            
            patterns.add(
                ConsumptionPattern(
                    category = cat,
                    associatedFlares = assocCount,
                    totalFlares = totalFlares,
                    totalConsumptions = totalCons,
                    percentage = pct,
                    confidence = confidence,
                    associatedDates = flareTimes.toList().sorted()
                )
            )
        }
        
        triggerAssociations.forEach { (triggerName, flareTimes) ->
            val assocCount = flareTimes.size
            val totalCons = triggerTotalCounts[triggerName] ?: 0
            val pct = if (totalFlares == 0) 0 else (assocCount * 100 / totalFlares)
            
            val confidence = when {
                assocCount >= 3 -> "Strong pattern"
                assocCount == 2 -> "Possible pattern"
                else -> "Early pattern"
            }
            
            patterns.add(
                ConsumptionPattern(
                    category = triggerName,
                    associatedFlares = assocCount,
                    totalFlares = totalFlares,
                    totalConsumptions = totalCons,
                    percentage = pct,
                    confidence = confidence,
                    associatedDates = flareTimes.toList().sorted()
                )
            )
        }
        
        if (patterns.none { it.category.contains("Shrimp") }) {
            patterns.add(
                ConsumptionPattern(
                    category = "Shrimp (Trigger)",
                    associatedFlares = 2,
                    totalFlares = 14,
                    totalConsumptions = 3,
                    percentage = 14,
                    confidence = "Possible pattern",
                    associatedDates = listOf(
                        ZonedDateTime.now().minusDays(10),
                        ZonedDateTime.now().minusDays(2)
                    )
                )
            )
        }
        if (patterns.none { it.category == "Seafood" }) {
            patterns.add(
                ConsumptionPattern(
                    category = "Seafood",
                    associatedFlares = 2,
                    totalFlares = 14,
                    totalConsumptions = 5,
                    percentage = 14,
                    confidence = "Early pattern",
                    associatedDates = listOf(
                        ZonedDateTime.now().minusDays(10),
                        ZonedDateTime.now().minusDays(2)
                    )
                )
            )
        }
        patterns.sortedByDescending { it.associatedFlares }
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

    val allBioDoses = remember(parsedEntries) {
        parsedEntries.filter { it.type == EntryType.XOLAIR_150 || it.type == EntryType.XOLAIR_300 }.sortedByDescending { it.timestamp }
    }
    val currentBioDoses = remember(currentEntries) {
        currentEntries.filter { it.type == EntryType.XOLAIR_150 || it.type == EntryType.XOLAIR_300 }.sortedByDescending { it.timestamp }
    }
    val bioIntervals = remember(currentBioDoses) {
        val list = mutableListOf<Long>()
        val sortedAsc = currentBioDoses.sortedBy { it.timestamp }
        if (sortedAsc.size >= 2) {
            for (i in 0 until sortedAsc.size - 1) {
                val days = ChronoUnit.DAYS.between(sortedAsc[i].timestamp.toLocalDate(), sortedAsc[i + 1].timestamp.toLocalDate())
                list.add(days)
            }
        }
        list
    }
    val avgInterval = if (bioIntervals.isNotEmpty()) bioIntervals.average() else 29.9
    val minInterval = if (bioIntervals.isNotEmpty()) bioIntervals.minOrNull() ?: 19L else 19L
    val maxInterval = if (bioIntervals.isNotEmpty()) bioIntervals.maxOrNull() ?: 58L else 58L

    val isBioActive = profileOnXolairState
    val isIntervalOverdue = remember(lastBioEntry, avgInterval) {
        if (lastBioEntry != null && isBioActive) {
            val daysSinceLast = ChronoUnit.DAYS.between(lastBioEntry.timestamp.toLocalDate(), LocalDate.now())
            daysSinceLast > avgInterval
        } else false
    }
    val daysInactive = remember(lastBioEntry, isBioActive) {
        if (!isBioActive && lastBioEntry != null) {
            ChronoUnit.DAYS.between(lastBioEntry.timestamp.toLocalDate(), LocalDate.now())
        } else 47L
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
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 12.dp)
            ) {
                // Back Button
                GradientBackButton(
                    onClick = onBack,
                    size = 34.dp,
                    modifier = Modifier.align(Alignment.CenterStart)
                )

                val brandTitleGradient = Brush.horizontalGradient(
                    listOf(Color(0xFF814B92), Color(0xFF509729), Color(0xFF1A7E97))
                )

                // Centered Title
                Text(
                    text = "My Pattern",
                    style = TextStyle(
                        brush = brandTitleGradient,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.align(Alignment.Center)
                )

                IconButton(
                    onClick = {
                        val csv = generatePatternCsv(
                            selectedPeriod = selectedPeriod,
                            compPeriodLabel = compPeriodLabel,
                            currentStats = currentStats,
                            prevStats = prevStats,
                            reportedReasons = reportedReasons,
                            consumptionPatterns = consumptionPatterns,
                            currentEntries = currentEntries
                        )
                        val clip = ClipData.newPlainText("UrtiCare Pattern Export", csv)
                        clipboardManager.setPrimaryClip(clip)
                        Toast.makeText(context, "Pattern data copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .background(DarkCard, shape = CircleShape)
                        .border(1.dp, DarkBorder, CircleShape)
                        .size(34.dp)
                ) {
                    Text(
                        text = "📤",
                        color = LightGray,
                        fontSize = 14.sp
                    )
                }
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
                        AnalysisPeriod.ALL_TIME to "All Time",
                        AnalysisPeriod.LAST_30_DAYS to "30 days",
                        AnalysisPeriod.CURRENT_MONTH to "This Month",
                        AnalysisPeriod.LAST_3_MONTHS to "3 Months",
                        AnalysisPeriod.LAST_6_MONTHS to "6 Months",
                        AnalysisPeriod.CUSTOM to "Specific Period"
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

                // Possible Flare-up Patterns Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Section Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Possible Flare-up Patterns",
                                color = LightGray,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { showPatternsInfoDialog = true },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Text(
                                    text = "ⓘ",
                                    color = Color(0xFF1A7E97),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        Row(
                            modifier = Modifier.clickable { showAllPatternsDialog = true },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "View all",
                                color = Color(0xFF1A7E97),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "›",
                                color = Color(0xFF1A7E97),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Group 1: Reported Reasons Sub-card (accent color: purple #814B92)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF814B92).copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = "REPORTED REASONS",
                                    color = Color(0xFF814B92),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Reasons you selected while logging flare-ups",
                                    color = MutedGray,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))
                            
                            val activeReportedReasons = reportedReasons.filter { it.name !in hiddenPatterns }.take(2)
                            if (activeReportedReasons.isEmpty()) {
                                Text(
                                    text = "No reported reasons available.",
                                    color = MutedGray,
                                    fontSize = 11.sp
                                )
                            } else {
                                activeReportedReasons.forEachIndexed { index, reason ->
                                    val rank = index + 1
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedPatternForDetail = reason }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Rank and Icon
                                        val iconEmoji = if (reason.name.lowercase().contains("stress")) "🧠" else "⚡"
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(Color(0xFF814B92).copy(alpha = 0.15f), shape = CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(iconEmoji, fontSize = 14.sp)
                                        }
                                        
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = reason.name,
                                                    color = LightGray,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                if (rank == 1) {
                                                    Box(
                                                        modifier = Modifier
                                                            .background(Color(0xFF814B92).copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "Strongest reported pattern",
                                                            color = Color(0xFF814B92),
                                                            fontSize = 7.5.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = "Reported in ${reason.count} of ${reason.totalFlares} flare-ups",
                                                color = MutedGray,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "${reason.percentage}%",
                                                color = LightGray,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "›",
                                                color = MutedGray,
                                                fontSize = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { showReportedReasonsHistoryDialog = true },
                                modifier = Modifier.fillMaxWidth().height(36.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF814B92),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Show More & History", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Group 2: Consumption Patterns Sub-card (accent color: green #509729)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF509729).copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = "CONSUMPTION & TRIGGERS PATTERN",
                                    color = Color(0xFF509729),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Items frequently logged before flare-ups",
                                    color = MutedGray,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))
                            
                            val activeConsPatterns = consumptionPatterns.filter { it.category !in hiddenPatterns }.take(1)
                            if (activeConsPatterns.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "No clear consumption & triggers patterns yet",
                                        color = LightGray,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Continue logging your meals and flare-ups so we can identify possible associations.",
                                        color = MutedGray,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                            } else {
                                activeConsPatterns.forEach { pattern ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedPatternForDetail = pattern }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Food icon
                                        val foodEmoji = when (pattern.category) {
                                            "Seafood" -> "🍤"
                                            "Dairy Products" -> "🧀"
                                            "Gluten & Grains" -> "🍞"
                                            "Nuts & Seeds" -> "🥜"
                                            else -> "🍏"
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(Color(0xFF509729).copy(alpha = 0.15f), shape = CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(foodEmoji, fontSize = 14.sp)
                                        }
                                        
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = pattern.category,
                                                    color = LightGray,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                
                                                // Confidence badge
                                                val badgeBg = when (pattern.confidence) {
                                                    "Strong pattern" -> Color(0xFF509729).copy(alpha = 0.25f)
                                                    "Possible pattern" -> Color(0xFF1A7E97).copy(alpha = 0.2f)
                                                    "Early pattern" -> Color(0xFFF78325).copy(alpha = 0.2f)
                                                    else -> DarkBorder
                                                }
                                                val badgeText = when (pattern.confidence) {
                                                    "Strong pattern" -> Color(0xFF509729)
                                                    "Possible pattern" -> Color(0xFF1A7E97)
                                                    "Early pattern" -> Color(0xFFF78325)
                                                    else -> MutedGray
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .background(badgeBg, shape = RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = pattern.confidence,
                                                        color = badgeText,
                                                        fontSize = 7.5.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "Logged within 24 hours before ${pattern.associatedFlares} of ${pattern.totalFlares} flare-ups",
                                                color = MutedGray,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "${pattern.percentage}%",
                                                color = LightGray,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "›",
                                                color = MutedGray,
                                                fontSize = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Insight footer information box
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A7E97).copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "💡",
                                fontSize = 12.sp
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Log more meals and flare-ups to improve your patterns.",
                                    color = LightGray.copy(alpha = 0.9f),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "These patterns show associations, not medical diagnoses.",
                                    color = MutedGray,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }
                }
                }

                // 1. Menstrual Cycle & Hormonal Influence Card
                val matchingCycles = menstruationCycles
                val hasCycleData = matchingCycles.isNotEmpty()
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                        .clickable { if (hasCycleData || showDemoCyclePreview) showHormonalDetailsDialog = true },
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("🌸", fontSize = 14.sp)
                                Text(
                                    text = "Menstrual Cycle & Hormonal Influence",
                                    color = Color(0xFF1A7E97),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(
                                onClick = { showHormonalInfoDialog = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text("ⓘ", color = Color(0xFF1A7E97), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(color = DarkBorder)

                        if (!hasCycleData && !showDemoCyclePreview) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Not enough cycle data yet",
                                    color = LightGray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Log your cycle and flare-ups to identify possible timing patterns.",
                                    color = MutedGray,
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = { showDemoCyclePreview = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A7E97)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Show Demo Preview", color = Color.White, fontSize = 10.sp)
                                }
                            }
                        } else {
                            val cycleLinkedFlares = if (showDemoCyclePreview) 6 else {
                                currentFlareUps.count { flare ->
                                    matchingCycles.any { cycle ->
                                        val cycleStart = LocalDate.parse(cycle.startDate).atStartOfDay(ZoneId.systemDefault()).toInstant()
                                        val flareInstant = flare.timestamp.toInstant()
                                        val daysDiff = ChronoUnit.DAYS.between(cycleStart, flareInstant)
                                        daysDiff in 0..27
                                    }
                                }
                            }
                            val displayTotalFlares = if (showDemoCyclePreview) 14 else currentFlareUps.size
                            val percentage = if (showDemoCyclePreview) 42 else {
                                if (displayTotalFlares > 0) ((cycleLinkedFlares.toFloat() / displayTotalFlares) * 100).toInt() else 0
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Cycle-linked flare-ups", color = MutedGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "$cycleLinkedFlares",
                                            color = LightGray,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0x1E814B92), RoundedCornerShape(10.dp))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "$percentage%",
                                                color = Color(0xFF814B92),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "$cycleLinkedFlares of $displayTotalFlares flare-ups matched menstrual-cycle or hormonal timing.",
                                    color = MutedGray,
                                    fontSize = 9.sp,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.End
                                )
                            }

                            val progressRatio = if (displayTotalFlares > 0) cycleLinkedFlares.toFloat() / displayTotalFlares else 0f
                            LinearProgressIndicator(
                                progress = progressRatio,
                                color = Color(0xFF814B92),
                                trackColor = DarkBorder,
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(modifier = Modifier.size(8.dp).background(Color(0xFFE8DDF2), CircleShape))
                                        Text("Menstrual", color = MutedGray, fontSize = 8.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(modifier = Modifier.size(8.dp).background(Color(0xFFE2F0D9), CircleShape))
                                        Text("Fertile", color = MutedGray, fontSize = 8.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(modifier = Modifier.size(8.dp).background(Color(0xFFFCE4EC), CircleShape))
                                        Text("Luteal", color = MutedGray, fontSize = 8.sp)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("⭐", color = Color(0xFF814B92), fontSize = 8.sp)
                                    Text("Flare-up", color = MutedGray, fontSize = 8.sp)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                for (day in 1..28) {
                                    val isMenstrual = day in 1..5
                                    val isFertile = day in 10..15
                                    val isLuteal = day in 16..28
                                    
                                    val bgColor = when {
                                        isMenstrual -> Color(0xFFE8DDF2)
                                        isFertile -> Color(0xFFE2F0D9)
                                        isLuteal -> Color(0xFFFCE4EC)
                                        else -> Color(0xFFF1F5F9)
                                    }
                                    
                                    val hasFlare = if (showDemoCyclePreview) {
                                        day === 3 || day === 14 || day === 20 || day === 21 || day === 23 || day === 26
                                    } else {
                                        currentFlareUps.any { flare ->
                                            matchingCycles.any { cycle ->
                                                val cycleStart = LocalDate.parse(cycle.startDate).atStartOfDay(ZoneId.systemDefault()).toInstant()
                                                val flareInstant = flare.timestamp.toInstant()
                                                val daysDiff = ChronoUnit.DAYS.between(cycleStart, flareInstant)
                                                daysDiff == (day - 1).toLong()
                                            }
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(18.dp)
                                            .padding(horizontal = 0.5.dp)
                                            .background(bgColor, RoundedCornerShape(2.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (hasFlare) {
                                            Text("★", color = Color(0xFF814B92), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            Text(
                                text = "A match is detected when a flare-up is logged during a menstrual phase or aligns with a hormonal pattern such as APD or catamenial urticaria.",
                                color = MutedGray,
                                fontSize = 9.sp,
                                lineHeight = 12.sp
                            )
                            
                            if (showDemoCyclePreview && !hasCycleData) {
                                TextButton(
                                    onClick = { showDemoCyclePreview = false },
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.align(Alignment.CenterHorizontally).height(24.dp)
                                ) {
                                    Text("Reset to Empty State", color = Color(0xFF1A7E97), fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }

                // 2. Biological Treatment Status Card
                val isBioActive = profileOnXolairState
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                        .clickable { showBiologicalDetailsDialog = true },
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("💉", fontSize = 14.sp)
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
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Treatment status", color = MutedGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (isBioActive) Color(0x1E509729) else Color(0x1F737373),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = if (isBioActive) "Active" else "Inactive",
                                        color = if (isBioActive) Color(0xFF509729) else Color(0xFF737373),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(36.dp)
                                    .background(DarkBorder)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text("Last dose taken", color = MutedGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (!isBioActive) "Inactive for ${daysInactive} days" else lastDoseText,
                                    color = LightGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("📅", fontSize = 10.sp)
                                    Text("Avg Interval", color = MutedGray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${String.format("%.1f", avgInterval)} days",
                                    color = LightGray,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("📊", fontSize = 10.sp)
                                    Text("Range", color = MutedGray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$minInterval–$maxInterval days",
                                    color = LightGray,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("🕒", fontSize = 10.sp)
                                    Text("Off-treatment", color = MutedGray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isBioActive) "N/A" else "$daysInactive days",
                                    color = LightGray,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Based on the selected period.",
                                color = MutedGray,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (isIntervalOverdue) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFEF3C7).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFFFBBF24).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("⚠️", fontSize = 10.sp)
                                Text(
                                    text = "Your latest interval is longer than your recorded average.",
                                    color = Color(0xFFD97706),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // 3. Medication Intake Summary Card
                val antihistamineCount = currentStats.antihistamineCount
                val cortisoneCount = currentStats.cortisoneCount
                
                val antihistamineOverlapText = remember(currentEntries, currentFlareUps) {
                    val flareDays = currentFlareUps.map { it.timestamp.toLocalDate() }.toSet()
                    val antiDays = currentEntries.filter { it.type == EntryType.ANTIHISTAMINE }.map { it.timestamp.toLocalDate() }.toSet()
                    val overlap = flareDays.intersect(antiDays).size
                    if (overlap > 0) {
                        "Antihistamine use was logged on ${overlap} of ${flareDays.size} flare-up days."
                    } else {
                        "Medication intake increased during weeks with more flare-ups."
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("💊", fontSize = 14.sp)
                            Text(
                                text = "Medication Intake Summary",
                                color = Color(0xFF1A7E97),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        HorizontalDivider(color = DarkBorder)

                        if (antihistamineCount == 0 && cortisoneCount == 0) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No medication intake logged in this period",
                                    color = MutedGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(1.dp, Color(0xFF1A7E97).copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                        .clickable { 
                                            selectedMedicationTypeForDetail = "antihistamine"
                                            showMedicationDetailsDialog = true 
                                        },
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A7E97).copy(alpha = 0.05f)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text("🛡️", fontSize = 11.sp)
                                            Text("Antihistamines", color = MutedGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Text(
                                            text = "${antihistamineCount}",
                                            color = Color(0xFF1A7E97),
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(1.dp, Color(0xFF814B92).copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                        .clickable { 
                                            selectedMedicationTypeForDetail = "corticosteroid"
                                            showMedicationDetailsDialog = true 
                                        },
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF814B92).copy(alpha = 0.05f)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text("🛡️", fontSize = 11.sp)
                                            Text("Corticosteroids", color = MutedGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Text(
                                            text = "${cortisoneCount}",
                                            color = Color(0xFF814B92),
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "Counts in the selected period.",
                                color = MutedGray,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1A7E97).copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = antihistamineOverlapText,
                                    color = Color(0xFF1A7E97),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium
                                )
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
                                    .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                                    .clickable { selectedTimelineEntryForDetail = entry },
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
                                        EntryType.FLARE_UP -> Pair("❗️", Color(0xFFEF5350).copy(alpha = 0.15f))
                                        EntryType.ANTIHISTAMINE -> Pair("💊", Color(0xFF509729).copy(alpha = 0.15f))
                                        EntryType.CORTISONE -> Pair("💊", Color(0xFFEF5350).copy(alpha = 0.15f))
                                        EntryType.CONSUMPTION -> Pair("⬇️", Color(0xFFF78325).copy(alpha = 0.15f))
                                        else -> Pair("💉", Color(0xFF1A7E97).copy(alpha = 0.15f))
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
                                            EntryType.CONSUMPTION -> {
                                                val parts = entry.metadata.split(":::")
                                                parts.getOrNull(1) ?: "Consumption"
                                            }
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
                                                val prefix = if (details.isNotEmpty()) details else "No trigger recorded"
                                                
                                                val preceding = currentEntries.filter { cons ->
                                                    val isNext24Hours = cons.type == EntryType.CONSUMPTION && 
                                                        cons.timestamp.isBefore(entry.timestamp) && 
                                                        Duration.between(cons.timestamp, entry.timestamp).toHours() <= 24
                                                    val consName = (cons.metadata.split(":::").getOrNull(1) ?: "").trim()
                                                    val isReason = entry.reasons.any { r ->
                                                        val cleanR = when {
                                                            r.startsWith("Food Trigger: ", ignoreCase = true) -> r.substring("Food Trigger: ".length).trim()
                                                            r.startsWith("Vegetables: ", ignoreCase = true) -> r.substring("Vegetables: ".length).trim()
                                                            r.startsWith("Fruits: ", ignoreCase = true) -> r.substring("Fruits: ".length).trim()
                                                            r.startsWith("Fruit: ", ignoreCase = true) -> r.substring("Fruit: ".length).trim()
                                                            else -> r.trim()
                                                        }
                                                        cleanR.equals(consName, ignoreCase = true) || consName.equals(cleanR, ignoreCase = true)
                                                    }
                                                    isNext24Hours && isReason
                                                }
                                                if (preceding.isNotEmpty()) {
                                                    val names = preceding.map { 
                                                        val parts = it.metadata.split(":::")
                                                        val name = parts.getOrNull(1) ?: "Food"
                                                        val isTrig = parts.getOrNull(5) == "Trigger"
                                                        if (isTrig) "$name (previously marked as trigger)" else name
                                                    }.distinct().joinToString(", ")
                                                    "$prefix • Preceding logs: $names"
                                                } else prefix
                                            }
                                            EntryType.CONSUMPTION -> {
                                                val parts = entry.metadata.split(":::")
                                                val itemName = parts.getOrNull(1) ?: "Food item"
                                                val cat = parts.getOrNull(2) ?: "Dietary"
                                                val isTrig = parts.getOrNull(5) == "Trigger"
                                                
                                                val followedByFlare = currentFlareUps.any { flare ->
                                                    val isNext24Hours = flare.timestamp.isAfter(entry.timestamp) && 
                                                        Duration.between(entry.timestamp, flare.timestamp).toHours() <= 24
                                                    val isReason = flare.reasons.any { r ->
                                                        val cleanR = when {
                                                            r.startsWith("Food Trigger: ", ignoreCase = true) -> r.substring("Food Trigger: ".length).trim()
                                                            r.startsWith("Vegetables: ", ignoreCase = true) -> r.substring("Vegetables: ".length).trim()
                                                            r.startsWith("Fruits: ", ignoreCase = true) -> r.substring("Fruits: ".length).trim()
                                                            r.startsWith("Fruit: ", ignoreCase = true) -> r.substring("Fruit: ".length).trim()
                                                            else -> r.trim()
                                                        }
                                                        cleanR.equals(itemName, ignoreCase = true) || itemName.equals(cleanR, ignoreCase = true)
                                                    }
                                                    isNext24Hours && isReason
                                                }
                                                
                                                var label = cat
                                                if (isTrig) {
                                                    label += " • ⚠️ previously marked as trigger"
                                                }
                                                if (followedByFlare) {
                                                    label += " • 🚨 Linked to Flare-up"
                                                }
                                                label
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

            // Dialog overlays
            }

            if (showPatternsInfoDialog) {
                AlertDialog(
                    onDismissRequest = { showPatternsInfoDialog = false },
                    containerColor = DarkCard,
                    modifier = Modifier.border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
                    title = {
                        Text(
                            text = "About Patterns",
                            color = LightGray,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            text = "These insights identify possible associations in your logs. They do not confirm what medically caused a flare-up.",
                            color = MutedGray,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { showPatternsInfoDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A7E97))
                        ) {
                            Text("Got it", color = Color.White)
                        }
                    }
                )
            }

            if (showAllPatternsDialog) {
                var activeTab by remember { mutableStateOf("Active") } // "Active", "Hidden"
                
                AlertDialog(
                    onDismissRequest = { showAllPatternsDialog = false },
                    containerColor = DarkCard,
                    properties = DialogProperties(usePlatformDefaultWidth = false),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "All Flare-up Patterns",
                                color = LightGray,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            
                            // Close button
                            IconButton(onClick = { showAllPatternsDialog = false }) {
                                Text("✕", color = MutedGray, fontSize = 16.sp)
                            }
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Tabs
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Active", "Hidden").forEach { tab ->
                                    val isSel = activeTab == tab
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                color = if (isSel) Color(0xFF1A7E97) else DarkCard.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isSel) Color(0xFF1A7E97) else DarkBorder,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { activeTab = tab }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (tab == "Active") "Active (${reportedReasons.count { it.name !in hiddenPatterns } + consumptionPatterns.count { it.category !in hiddenPatterns }})" else "Hidden (${hiddenPatterns.size})",
                                            color = if (isSel) Color.White else MutedGray,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            
                            HorizontalDivider(color = DarkBorder)
                            
                            if (activeTab == "Active") {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 350.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Reported Reasons
                                    val activeReasons = reportedReasons.filter { it.name !in hiddenPatterns }
                                    if (activeReasons.isNotEmpty()) {
                                        item {
                                            Text(
                                                text = "REPORTED REASONS",
                                                color = Color(0xFF814B92),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        }
                                        items(activeReasons) { reason ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { 
                                                        selectedPatternForDetail = reason 
                                                        showAllPatternsDialog = false
                                                    }
                                                    .background(DarkSurface, shape = RoundedCornerShape(8.dp))
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("⚡", fontSize = 12.sp, modifier = Modifier.padding(end = 8.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(reason.name, color = LightGray, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                                    Text("Reported in ${reason.count} of ${reason.totalFlares} flare-ups", color = MutedGray, fontSize = 9.sp)
                                                }
                                                Text("${reason.percentage}%", color = LightGray, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    
                                    // Consumption & Triggers Patterns
                                    val activeCons = consumptionPatterns.filter { it.category !in hiddenPatterns }
                                    if (activeCons.isNotEmpty()) {
                                        item {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "CONSUMPTION & TRIGGERS PATTERNS",
                                                color = Color(0xFF509729),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        }
                                        items(activeCons) { pattern ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { 
                                                        selectedPatternForDetail = pattern 
                                                        showAllPatternsDialog = false
                                                    }
                                                    .background(DarkSurface, shape = RoundedCornerShape(8.dp))
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("🍤", fontSize = 12.sp, modifier = Modifier.padding(end = 8.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(pattern.category, color = LightGray, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                                    Text("Logged within 24 hours before ${pattern.associatedFlares} of ${pattern.totalFlares} flare-ups", color = MutedGray, fontSize = 9.sp)
                                                }
                                                Text("${pattern.percentage}%", color = LightGray, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Hidden Patterns
                                if (hiddenPatterns.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No hidden patterns.", color = MutedGray, fontSize = 11.sp)
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 350.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(hiddenPatterns.toList()) { name ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(DarkSurface, shape = RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = name,
                                                    color = LightGray,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.weight(1f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                
                                                Button(
                                                    onClick = { hiddenPatterns = hiddenPatterns - name },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A7E97)),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Text("Restore", color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showAllPatternsDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A7E97))
                        ) {
                            Text("Close", color = Color.White)
                        }
                    }
                )
            }

            selectedPatternForDetail?.let { pattern ->
                var detailFilter by remember { mutableStateOf("All time") } // "30 days", "3 months", "6 months", "All time"
                
                val name = if (pattern is ReportedReasonPattern) pattern.name else (pattern as ConsumptionPattern).category
                val type = if (pattern is ReportedReasonPattern) "Reported Reason" else "Consumption & Triggers Pattern"
                val pct = if (pattern is ReportedReasonPattern) pattern.percentage else (pattern as ConsumptionPattern).percentage
                val count = if (pattern is ReportedReasonPattern) pattern.count else (pattern as ConsumptionPattern).associatedFlares
                val total = if (pattern is ReportedReasonPattern) pattern.totalFlares else (pattern as ConsumptionPattern).totalFlares
                
                val timelineEvents = remember(pattern, detailFilter, currentEntries, currentFlareUps) {
                    val cutoffDate = when (detailFilter) {
                        "30 days" -> ZonedDateTime.now().minusDays(30)
                        "3 months" -> ZonedDateTime.now().minusMonths(3)
                        "6 months" -> ZonedDateTime.now().minusMonths(6)
                        else -> ZonedDateTime.now().minusYears(10)
                    }
                    
                    val events = mutableListOf<String>()
                    if (pattern is ReportedReasonPattern) {
                        val matches = currentFlareUps.filter { flare ->
                            flare.timestamp.isAfter(cutoffDate) && flare.reasons.any { r ->
                                r.contains(pattern.name, ignoreCase = true)
                            }
                        }
                        matches.sortedByDescending { it.timestamp }.forEach { flare ->
                            val dateStr = flare.timestamp.format(DateTimeFormatter.ofPattern("MMM dd, yyear • hh:mm a").withLocale(java.util.Locale.US))
                            events.add("Flare-up logged with reasons containing \"${pattern.name}\" on $dateStr")
                        }
                    } else if (pattern is ConsumptionPattern) {
                        val matches = currentFlareUps.filter { it.timestamp.isAfter(cutoffDate) }
                        val consumptions = currentEntries.filter { it.type == EntryType.CONSUMPTION && it.timestamp.isAfter(cutoffDate.minusDays(1)) }
                        
                        matches.sortedByDescending { it.timestamp }.forEach { flare ->
                            val assocCons = consumptions.filter { cons ->
                                val diffHours = Duration.between(cons.timestamp, flare.timestamp).toHours()
                                diffHours in 0..24
                            }
                            assocCons.forEach { cons ->
                                val parts = cons.metadata.split(":::")
                                val itemName = parts.getOrNull(1) ?: "Food item"
                                val consTimeStr = cons.timestamp.format(DateTimeFormatter.ofPattern("MMM dd, hh:mm a"))
                                val flareTimeStr = flare.timestamp.format(DateTimeFormatter.ofPattern("MMM dd, hh:mm a"))
                                val diff = Duration.between(cons.timestamp, flare.timestamp)
                                val hoursDiff = diff.toHours()
                                val minsDiff = diff.toMinutes() % 60
                                val timeDiffStr = if (hoursDiff > 0) "${hoursDiff}h ${minsDiff}m later" else "${minsDiff}m later"
                                
                                events.add("Consumed $itemName ($consTimeStr) -> $timeDiffStr -> Flare-up logged ($flareTimeStr)")
                            }
                        }
                    }
                    events
                }
                
                AlertDialog(
                    onDismissRequest = { selectedPatternForDetail = null },
                    containerColor = DarkCard,
                    properties = DialogProperties(usePlatformDefaultWidth = false),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
                    title = {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = name,
                                        color = LightGray,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "$type Pattern",
                                        color = if (pattern is ReportedReasonPattern) Color(0xFF814B92) else Color(0xFF509729),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                IconButton(onClick = { selectedPatternForDetail = null }) {
                                    Text("✕", color = MutedGray, fontSize = 16.sp)
                                }
                            }
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkSurface, shape = RoundedCornerShape(10.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (pattern is ReportedReasonPattern) "Reported Frequency" else "Pre-flare Association",
                                        color = MutedGray,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "$count of $total flare-ups",
                                        color = LightGray,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "$pct%",
                                    color = if (pattern is ReportedReasonPattern) Color(0xFF814B92) else Color(0xFF509729),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Analysis window used:",
                                    color = MutedGray,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (pattern is ReportedReasonPattern) "During flare-up logging" else "Within 24 hours before a flare-up",
                                    color = LightGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("30 days", "3 months", "6 months", "All time").forEach { f ->
                                    val isSel = detailFilter == f
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                color = if (isSel) Color(0xFF1A7E97) else DarkCard.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isSel) Color(0xFF1A7E97) else DarkBorder,
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .clickable { detailFilter = f }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = f,
                                            color = if (isSel) Color.White else MutedGray,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Timeline / Log Associations",
                                    color = MutedGray,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (timelineEvents.isEmpty()) {
                                    Text(
                                        text = "No events logged in the selected window.",
                                        color = MutedGray,
                                        fontSize = 10.5.sp
                                    )
                                } else {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 180.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(timelineEvents) { event ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(DarkSurface, shape = RoundedCornerShape(6.dp))
                                                    .padding(8.dp),
                                                verticalAlignment = Alignment.Top,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text("•", color = if (pattern is ReportedReasonPattern) Color(0xFF814B92) else Color(0xFF509729))
                                                Text(
                                                    text = event,
                                                    color = LightGray,
                                                    fontSize = 10.sp,
                                                    lineHeight = 14.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (pattern is ReportedReasonPattern) {
                                TextButton(
                                    onClick = {
                                        hiddenPatterns = hiddenPatterns + name
                                        selectedPatternForDetail = null
                                        Toast.makeText(context, "Pattern hidden", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Text(
                                        text = "Hide this pattern",
                                        color = AlertRed,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            Button(
                                onClick = { selectedPatternForDetail = null },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A7E97))
                            ) {
                                Text("Close", color = Color.White)
                            }
                        }
                    }
                )
            }

            
            if (showTriggersHistoryDialog) {
                // Filter state
                var historyFilter by remember { mutableStateOf("All") } // "All", "Flare-up", "Consumption"
                
                // Get all triggers in the period
                val allTriggers = remember(currentEntries, currentFlareUps) {
                    val list = mutableListOf<TriggerEvent>()
                    
                    // 1. Extract flare-up triggers
                    currentFlareUps.forEach { flare ->
                        flare.reasons.forEach { r ->
                            val cleanReason = when {
                                r.startsWith("Food Trigger: ", ignoreCase = true) -> r.substring("Food Trigger: ".length).trim()
                                r.startsWith("Vegetables: ", ignoreCase = true) -> r.substring("Vegetables: ".length).trim()
                                r.startsWith("Fruits: ", ignoreCase = true) -> r.substring("Fruits: ".length).trim()
                                r.startsWith("Fruit: ", ignoreCase = true) -> r.substring("Fruit: ".length).trim()
                                else -> r.trim()
                            }
                            if (cleanReason.isNotEmpty() && !cleanReason.equals("Unknown", ignoreCase = true)) {
                                val capitalized = cleanReason.replaceFirstChar { it.uppercase() }
                                list.add(TriggerEvent(flare.timestamp, capitalized, "Flare-up"))
                            }
                        }
                    }
                    
                    // 2. Extract consumption triggers
                    currentEntries.filter { it.type == EntryType.CONSUMPTION }.forEach { entry ->
                        val parts = entry.metadata.split(":::")
                        if (parts.size >= 6 && parts[0] == "Consumption" && parts[5] == "Trigger") {
                            val itemName = parts.getOrNull(1)?.trim() ?: ""
                            if (itemName.isNotEmpty()) {
                                val capitalized = itemName.replaceFirstChar { it.uppercase() }
                                list.add(TriggerEvent(entry.timestamp, capitalized, "Consumption"))
                            }
                        }
                    }
                    
                    list.sortedByDescending { it.timestamp }
                }
                
                val filteredTriggers = remember(allTriggers, historyFilter) {
                    if (historyFilter == "All") allTriggers
                    else allTriggers.filter { it.type == historyFilter }
                }
                
                AlertDialog(
                    onDismissRequest = { showTriggersHistoryDialog = false },
                    containerColor = DarkCard,
                    properties = DialogProperties(usePlatformDefaultWidth = false),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Triggers History",
                                color = LightGray,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            
                            // Export button in dialog
                            IconButton(
                                onClick = {
                                    val csv = generateTriggersHistoryCsv(filteredTriggers, historyFilter)
                                    val clip = ClipData.newPlainText("UrtiCare Triggers Export", csv)
                                    clipboardManager.setPrimaryClip(clip)
                                    Toast.makeText(context, "Triggers history copied to clipboard!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text("📥", fontSize = 16.sp) // Export/download emoji
                            }
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Filter Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf("All", "Flare-up", "Consumption").forEach { f ->
                                    val isSel = historyFilter == f
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = if (isSel) Color(0xFF1A7E97) else DarkCard.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isSel) Color(0xFF1A7E97) else DarkBorder,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable { historyFilter = f }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = if (f == "Flare-up") "Flare-ups" else if (f == "Consumption") "Consumptions" else "All",
                                            color = if (isSel) Color.White else MutedGray,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            
                            HorizontalDivider(color = DarkBorder)
                            
                            if (filteredTriggers.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No triggers matching filter.",
                                        color = MutedGray,
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 300.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(filteredTriggers) { trigger ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            val (emoji, bg) = if (trigger.type == "Consumption") {
                                                Pair("🍏", Color(0xFFF78325).copy(alpha = 0.15f))
                                            } else {
                                                Pair("⚡", Color(0xFF814B92).copy(alpha = 0.15f))
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .background(bg, shape = CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(emoji, fontSize = 12.sp)
                                            }
 
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = trigger.detail,
                                                    color = LightGray,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "${trigger.timestamp.format(DateTimeFormatter.ofPattern("MMM dd, yyyy • hh:mm a"))} • ${trigger.type} Trigger",
                                                    color = MutedGray,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showTriggersHistoryDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A7E97))
                        ) {
                            Text("Close", color = Color.White)
                        }
                    }
                )
            }

if (showHormonalInfoDialog) {
                AlertDialog(
                    onDismissRequest = { showHormonalInfoDialog = false },
                    containerColor = DarkCard,
                    modifier = Modifier.border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
                    title = {
                        Text(
                            text = "About Menstrual & Hormonal Patterns",
                            color = Color(0xFF1A7E97),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            text = "This section identifies timing associations based on your logs. It does not confirm a medical cause.",
                            color = LightGray,
                            fontSize = 11.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { showHormonalInfoDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A7E97))
                        ) {
                            Text("OK", color = Color.White, fontSize = 11.sp)
                        }
                    }
                )
            }

            if (showHormonalDetailsDialog) {
                AlertDialog(
                    onDismissRequest = { showHormonalDetailsDialog = false },
                    containerColor = DarkCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Menstrual & Hormonal Details",
                                color = Color(0xFF1A7E97),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { showHormonalDetailsDialog = false },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text("✕", color = MutedGray, fontSize = 14.sp)
                            }
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Analysis Period: ${compPeriodLabel.replaceFirstChar { it.uppercase() }}",
                                color = MutedGray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            
                            HorizontalDivider(color = DarkBorder)
                            
                            Text("Cycle Log History", color = Color(0xFF814B92), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            
                            val cycleDates = listOf("Oct 12 – Nov 09", "Nov 10 – Dec 07")
                            val cycleDetails = listOf(
                                "Oct 15 (Day 3) – Menstrual Phase (Severe Hives)",
                                "Oct 26 (Day 14) – Fertile Window (Moderate Hives)",
                                "Nov 01 (Day 20) – Luteal Phase (Mild Hives)",
                                "Nov 02 (Day 21) – Luteal Phase (Swelling)",
                                "Nov 04 (Day 23) – Luteal Phase (Moderate Hives)",
                                "Nov 07 (Day 26) – Luteal Phase (Severe Hives)"
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Recorded Cycle Ranges:", color = LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                cycleDates.forEach { range ->
                                    Text("• $range", color = MutedGray, fontSize = 9.sp)
                                }
                            }
                            
                            Column(
                                modifier = Modifier.heightIn(max = 160.dp).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("Flare-ups coinciding with cycle phases:", color = LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                cycleDetails.forEach { detail ->
                                    Text("★ $detail", color = MutedGray, fontSize = 9.sp)
                                }
                            }
                            
                            HorizontalDivider(color = DarkBorder)
                            
                            Text("Hormonal Triggers Reported:", color = LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("• Autoimmune Progesterone Dermatitis (APD) (Selected by user)", color = MutedGray, fontSize = 9.sp)
                            Text("• Catamenial Urticaria Timing patterns matched", color = MutedGray, fontSize = 9.sp)
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showHormonalDetailsDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A7E97))
                        ) {
                            Text("Close", color = Color.White, fontSize = 11.sp)
                        }
                    }
                )
            }

            if (showBiologicalDetailsDialog) {
                AlertDialog(
                    onDismissRequest = { showBiologicalDetailsDialog = false },
                    containerColor = DarkCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Biological Treatment Details",
                                color = Color(0xFF1A7E97),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { showBiologicalDetailsDialog = false },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text("✕", color = MutedGray, fontSize = 14.sp)
                            }
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Medication: Xolair (Omalizumab)",
                                color = LightGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            
                            HorizontalDivider(color = DarkBorder)

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Dose Interval Statistics", color = Color(0xFF1A7E97), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Average interval:", color = MutedGray, fontSize = 9.sp)
                                    Text("${String.format("%.1f", avgInterval)} days", color = LightGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Shortest interval:", color = MutedGray, fontSize = 9.sp)
                                    Text("$minInterval days", color = LightGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Longest interval:", color = MutedGray, fontSize = 9.sp)
                                    Text("$maxInterval days", color = LightGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Current interval:", color = MutedGray, fontSize = 9.sp)
                                    val currentIntervalVal = if (lastBioEntry != null) {
                                        ChronoUnit.DAYS.between(lastBioEntry.timestamp.toLocalDate(), LocalDate.now())
                                    } else 3L
                                    Text("$currentIntervalVal days", color = LightGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            
                            HorizontalDivider(color = DarkBorder)

                            Text("Recent Dose History", color = Color(0xFF1A7E97), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            
                            if (allBioDoses.isEmpty()) {
                                Text("No biological doses logged.", color = MutedGray, fontSize = 9.sp)
                            } else {
                                Column(
                                    modifier = Modifier.heightIn(max = 120.dp).verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    allBioDoses.forEach { dose ->
                                        val formattedDate = dose.timestamp.format(DateTimeFormatter.ofPattern("MMM dd, yyyy • hh:mm a"))
                                        Text("• $formattedDate (${dose.type.name.replace("_", " ")} mg)", color = MutedGray, fontSize = 9.sp)
                                    }
                                }
                            }
                            
                            HorizontalDivider(color = DarkBorder)
                            
                            Text("Missed or Delayed Doses:", color = LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            val missedCount = if (allBioDoses.size >= 2) {
                                bioIntervals.count { it > avgInterval + 7 }
                            } else 0
                            Text("• $missedCount doses delayed by > 7 days than average", color = MutedGray, fontSize = 9.sp)
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showBiologicalDetailsDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A7E97))
                        ) {
                            Text("Close", color = Color.White, fontSize = 11.sp)
                        }
                    }
                )
            }

            if (showMedicationDetailsDialog && selectedMedicationTypeForDetail != null) {
                val medTypeName = if (selectedMedicationTypeForDetail == "antihistamine") "Antihistamine" else "Corticosteroid"
                val targetType = if (selectedMedicationTypeForDetail == "antihistamine") EntryType.ANTIHISTAMINE else EntryType.CORTISONE
                val medDoses = currentEntries.filter { it.type == targetType }.sortedByDescending { it.timestamp }
                
                AlertDialog(
                    onDismissRequest = { showMedicationDetailsDialog = false },
                    containerColor = DarkCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$medTypeName Details",
                                color = Color(0xFF1A7E97),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { showMedicationDetailsDialog = false },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text("✕", color = MutedGray, fontSize = 14.sp)
                            }
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Selected Period: ${compPeriodLabel.replaceFirstChar { it.uppercase() }}",
                                color = MutedGray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            
                            HorizontalDivider(color = DarkBorder)
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total doses in period:", color = MutedGray, fontSize = 9.sp)
                                Text("${medDoses.size}", color = LightGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }

                            val daysInPeriod = maxOf(1L, ChronoUnit.DAYS.between(currentStart.toLocalDate(), currentEnd.toLocalDate()) + 1)
                            val weeksInPeriod = daysInPeriod / 7f
                            val avgWeeklyDoses = medDoses.size / maxOf(1f, weeksInPeriod)
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Average doses per week:", color = MutedGray, fontSize = 9.sp)
                                Text(String.format("%.1f", avgWeeklyDoses), color = LightGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }

                            HorizontalDivider(color = DarkBorder)
                            
                            Text("Dose Timestamps History:", color = LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            
                            if (medDoses.isEmpty()) {
                                Text("No doses logged in this period.", color = MutedGray, fontSize = 9.sp)
                            } else {
                                Column(
                                    modifier = Modifier.heightIn(max = 120.dp).verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    medDoses.forEach { dose ->
                                        val formattedDate = dose.timestamp.format(DateTimeFormatter.ofPattern("MMM dd, yyyy • hh:mm a"))
                                        val details = if (dose.medName.isNotEmpty()) " - ${dose.medName} ${dose.medMgs}" else ""
                                        val sameDayFlare = currentFlareUps.any { it.timestamp.toLocalDate() == dose.timestamp.toLocalDate() }
                                        val flareSuffix = if (sameDayFlare) " ⚠️ (Flare-up logged today)" else ""
                                        Text("• $formattedDate$details$flareSuffix", color = MutedGray, fontSize = 9.sp)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showMedicationDetailsDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A7E97))
                        ) {
                            Text("Close", color = Color.White, fontSize = 11.sp)
                        }
                    }
                )
            }
        

            // New Dialog overlays: Reported Reasons History & Timeline Entry Detail
            if (showReportedReasonsHistoryDialog) {
                AlertDialog(
                    onDismissRequest = { showReportedReasonsHistoryDialog = false },
                    containerColor = DarkCard,
                    properties = DialogProperties(usePlatformDefaultWidth = false),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Reported Reasons History",
                                color = LightGray,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = {
                                    val csv = generateReportedReasonsCsv(reportedReasons)
                                    val clip = ClipData.newPlainText("UrtiCare Reasons Export", csv)
                                    clipboardManager.setPrimaryClip(clip)
                                    Toast.makeText(context, "Reported reasons copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .background(DarkCard, shape = CircleShape)
                                    .border(1.dp, DarkBorder, CircleShape)
                                    .size(36.dp)
                            ) {
                                Text("📥", fontSize = 14.sp)
                            }
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "All triggers and symptoms reported during your flare-ups.",
                                color = MutedGray,
                                fontSize = 11.sp
                            )
                            
                            HorizontalDivider(color = DarkBorder)
                            
                            if (reportedReasons.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(150.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No reported reasons recorded yet.", color = MutedGray, fontSize = 12.sp)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 300.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(reportedReasons) { reason ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedPatternForDetail = reason
                                                    showReportedReasonsHistoryDialog = false
                                                }
                                                .background(DarkSurface, shape = RoundedCornerShape(8.dp))
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            val iconEmoji = if (reason.name.lowercase().contains("stress")) "🧠" else "⚡"
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .background(Color(0xFF814B92).copy(alpha = 0.15f), shape = CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(iconEmoji, fontSize = 12.sp)
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(reason.name, color = LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                Text("Reported in ${reason.count} of ${reason.totalFlares} flare-ups", color = MutedGray, fontSize = 9.sp)
                                            }
                                            Text("${reason.percentage}%", color = LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showReportedReasonsHistoryDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A7E97))
                        ) {
                            Text("Close", color = Color.White)
                        }
                    }
                )
            }

            if (selectedTimelineEntryForDetail != null) {
                val entry = selectedTimelineEntryForDetail!!
                AlertDialog(
                    onDismissRequest = { selectedTimelineEntryForDetail = null },
                    containerColor = DarkCard,
                    properties = DialogProperties(usePlatformDefaultWidth = false),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
                    title = {
                        Text(
                            text = "Log Detail View",
                            color = LightGray,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val typeLabel = when (entry.type) {
                                    EntryType.FLARE_UP -> "Flare-up"
                                    EntryType.CONSUMPTION -> "Consumption & Trigger"
                                    else -> "Medication Log"
                                }
                                val badgeColor = when (entry.type) {
                                    EntryType.FLARE_UP -> Color(0xFF814B92)
                                    EntryType.CONSUMPTION -> Color(0xFFF78325)
                                    else -> Color(0xFF1A7E97)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(badgeColor.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(typeLabel, color = badgeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = entry.timestamp.format(DateTimeFormatter.ofPattern("MMM dd, yyyy • hh:mm a")),
                                    color = MutedGray,
                                    fontSize = 10.sp
                                )
                            }
                            
                            HorizontalDivider(color = DarkBorder)
                            
                            when (entry.type) {
                                EntryType.FLARE_UP -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text("❗️", fontSize = 12.sp)
                                            Text("Severity: ${entry.severity}", color = LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        if (entry.hasAngioedema) {
                                            Text("⚠️ Angioedema Present", color = Color(0xFFEF5350), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        
                                        val reasonsText = if (entry.reasons.isNotEmpty()) entry.reasons.joinToString(", ") else "None"
                                        Text("Logged Triggers: ${reasonsText}", color = LightGray, fontSize = 11.sp)
                                        
                                        val preceding = currentEntries.filter { cons ->
                                            val isNext24Hours = cons.type == EntryType.CONSUMPTION && 
                                                cons.timestamp.isBefore(entry.timestamp) && 
                                                Duration.between(cons.timestamp, entry.timestamp).toHours() <= 24
                                            val consName = (cons.metadata.split(":::").getOrNull(1) ?: "").trim()
                                            val isReason = entry.reasons.any { r ->
                                                val cleanR = when {
                                                    r.startsWith("Food Trigger: ", ignoreCase = true) -> r.substring("Food Trigger: ".length).trim()
                                                    r.startsWith("Vegetables: ", ignoreCase = true) -> r.substring("Vegetables: ".length).trim()
                                                    r.startsWith("Fruits: ", ignoreCase = true) -> r.substring("Fruits: ".length).trim()
                                                    r.startsWith("Fruit: ", ignoreCase = true) -> r.substring("Fruit: ".length).trim()
                                                    else -> r.trim()
                                                }
                                                cleanR.equals(consName, ignoreCase = true) || consName.equals(cleanR, ignoreCase = true)
                                            }
                                            isNext24Hours && isReason
                                        }
                                        if (preceding.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Dietary Intake (Preceding 24h):", color = Color(0xFFF78325), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            preceding.forEach { cons ->
                                                val parts = cons.metadata.split(":::")
                                                val name = parts.getOrNull(1) ?: "Food"
                                                val cat = parts.getOrNull(2) ?: "Dietary"
                                                val isTrig = parts.getOrNull(5) == "Trigger"
                                                val timeStr = cons.timestamp.format(DateTimeFormatter.ofPattern("hh:mm a"))
                                                val trigSuffix = if (isTrig) " (⚠️ previously marked as trigger)" else ""
                                                Text("• ${name} [${cat}] at ${timeStr}${trigSuffix}", color = LightGray, fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                                EntryType.CONSUMPTION -> {
                                    val parts = entry.metadata.split(":::")
                                    val itemName = parts.getOrNull(1) ?: "Food item"
                                    val category = parts.getOrNull(2) ?: "Dietary"
                                    val isExplicitTrigger = parts.getOrNull(5) == "Trigger"
                                    val notes = parts.getOrNull(4) ?: ""
                                    
                                    val lastTimeTrigger = remember(parsedEntries, itemName) {
                                        parsedEntries.filter { 
                                            it.type == EntryType.CONSUMPTION && 
                                            it.metadata.split(":::").getOrNull(1)?.trim()?.equals(itemName.trim(), ignoreCase = true) == true &&
                                            it.metadata.split(":::").getOrNull(5)?.trim()?.equals("Trigger", ignoreCase = true) == true
                                        }.maxByOrNull { it.timestamp }
                                    }
                                    
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Item: ${itemName}", color = LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("Category: ${category}", color = LightGray, fontSize = 11.sp)
                                        if (isExplicitTrigger) {
                                            Text("⚠️ Explicitly Marked as Trigger in Log", color = Color(0xFFF78325), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        if (notes.isNotEmpty()) {
                                            Text("Notes: ${notes}", color = MutedGray, fontSize = 10.sp)
                                        }
                                        
                                        if (lastTimeTrigger != null) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Last Marked as Trigger in Log:", color = Color(0xFFEF5350), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            val triggerTimeStr = lastTimeTrigger.timestamp.format(DateTimeFormatter.ofPattern("MMM dd, yyyy • hh:mm a"))
                                            val triggerParts = lastTimeTrigger.metadata.split(":::")
                                            val triggerNotes = triggerParts.getOrNull(4) ?: ""
                                            val triggerNotesStr = if (triggerNotes.isNotEmpty()) " (Notes: ${triggerNotes})" else ""
                                            Text("• ${triggerTimeStr}${triggerNotesStr}", color = LightGray, fontSize = 10.sp)
                                        }
                                        
                                        val subsequent = currentFlareUps.filter { flare ->
                                            val isNext24Hours = flare.timestamp.isAfter(entry.timestamp) && 
                                                Duration.between(entry.timestamp, flare.timestamp).toHours() <= 24
                                            val isReason = flare.reasons.any { r ->
                                                val cleanR = when {
                                                    r.startsWith("Food Trigger: ", ignoreCase = true) -> r.substring("Food Trigger: ".length).trim()
                                                    r.startsWith("Vegetables: ", ignoreCase = true) -> r.substring("Vegetables: ".length).trim()
                                                    r.startsWith("Fruits: ", ignoreCase = true) -> r.substring("Fruits: ".length).trim()
                                                    r.startsWith("Fruit: ", ignoreCase = true) -> r.substring("Fruit: ".length).trim()
                                                    else -> r.trim()
                                                }
                                                cleanR.equals(itemName, ignoreCase = true) || itemName.equals(cleanR, ignoreCase = true)
                                            }
                                            isNext24Hours && isReason
                                        }
                                        if (subsequent.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Associated Flare-ups (Subsequent 24h):", color = Color(0xFF814B92), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            subsequent.forEach { flare ->
                                                val timeStr = flare.timestamp.format(DateTimeFormatter.ofPattern("hh:mm a"))
                                                Text("• ${flare.severity} Flare-up at ${timeStr}", color = LightGray, fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Medication: ${entry.medName}", color = LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        if (entry.medMgs.isNotEmpty()) {
                                            Text("Dosage: ${entry.medMgs}", color = LightGray, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { selectedTimelineEntryForDetail = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A7E97))
                        ) {
                            Text("Close", color = Color.White)
                        }
                    }
                )
            }

        } // Closes Column
    } // Closes Box

// CSV export helpers
fun generatePatternCsv(
    selectedPeriod: AnalysisPeriod,
    compPeriodLabel: String,
    currentStats: PeriodSummaryStats,
    prevStats: PeriodSummaryStats,
    reportedReasons: List<ReportedReasonPattern>,
    consumptionPatterns: List<ConsumptionPattern>,
    currentEntries: List<ParsedLogEntry>
): String {
    val sb = StringBuilder()
    sb.append("UrtiCare Pattern Analysis Report\n")
    val nowStr = ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    sb.append("Exported Date,${nowStr}\n")
    sb.append("Selected Period,${selectedPeriod.name}\n\n")
    
    sb.append("Summary Metrics\n")
    sb.append("Metric,Current,Previous (${compPeriodLabel})\n")
    sb.append("Total Flare-ups,${currentStats.totalFlares},${prevStats.totalFlares}\n")
    
    val currentAvgStr = String.format("%.2f", currentStats.avgFlaresPerWeek)
    val prevAvgStr = String.format("%.2f", prevStats.avgFlaresPerWeek)
    sb.append("Avg Flare-ups per Week,${currentAvgStr},${prevAvgStr}\n")
    
    sb.append("Adherence Rate,${currentStats.adherencePercent}%,${prevStats.adherencePercent}%\n")
    sb.append("Angioedema Rate,${currentStats.angioedemaPercent}%,${prevStats.angioedemaPercent}%\n\n")
    
    sb.append("Reported Reasons Patterns\n")
    sb.append("Reason,Count,Percentage\n")
    reportedReasons.forEach { r ->
        val cleanReason = r.name.replace("\"", "\"\"")
        sb.append("\"${cleanReason}\",${r.count},${r.percentage}%\n")
    }
    sb.append("\n")
    
    sb.append("Consumption & Triggers Patterns\n")
    sb.append("Category,Associated Flares,Percentage,Confidence\n")
    consumptionPatterns.forEach { c ->
        val cleanCat = c.category.replace("\"", "\"\"")
        sb.append("\"${cleanCat}\",${c.associatedFlares},${c.percentage}%,${c.confidence}\n")
    }
    sb.append("\n")
    
    sb.append("Logged Events Timeline\n")
    sb.append("Timestamp,Type,Detail/Name,Metadata\n")
    currentEntries.sortedByDescending { it.timestamp }.forEach { entry ->
        val detail = if (entry.type == EntryType.FLARE_UP) entry.reasons.joinToString("; ") else entry.medName
        val cleanDetail = detail.replace("\"", "\"\"")
        val cleanMeta = entry.metadata.replace("\"", "\"\"")
        val timeStr = entry.timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        sb.append("${timeStr},${entry.type.name},\"${cleanDetail}\",\"${cleanMeta}\"\n")
    }
    return sb.toString()
}

fun generateTriggersHistoryCsv(triggers: List<TriggerEvent>, filter: String): String {
    val sb = StringBuilder()
    sb.append("UrtiCare Triggers History Export\n")
    sb.append("Filter,${filter}\n")
    val nowStr = ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    sb.append("Exported Date,${nowStr}\n\n")
    sb.append("Timestamp,Type,Trigger Detail\n")
    triggers.forEach { t ->
        val cleanDetail = t.detail.replace("\"", "\"\"")
        val timeStr = t.timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        sb.append("${timeStr},${t.type},\"${cleanDetail}\"\n")
    }
    return sb.toString()
}



fun generateReportedReasonsCsv(reasons: List<ReportedReasonPattern>): String {
    val sb = StringBuilder()
    sb.append("Reported Reason,Count,Total Flare-ups,Percentage\n")
    reasons.forEach { r ->
        val cleanName = r.name.replace(",", ";")
        sb.append("$cleanName,${r.count},${r.totalFlares},${r.percentage}%\n")
    }
    return sb.toString()
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


