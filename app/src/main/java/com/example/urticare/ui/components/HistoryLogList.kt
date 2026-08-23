package com.example.urticare.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import com.example.urticare.model.MedicationDirectory
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.urticare.model.EntryType
import com.example.urticare.model.LogEntry
import com.example.urticare.ui.theme.*
import com.example.urticare.data.LibraryDataHolder
import com.example.urticare.viewmodel.TrackerViewModel
import java.time.Duration
import java.time.ZonedDateTime



@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryLogList(
    viewModel: TrackerViewModel,
    entries: List<LogEntry>,
    initialTab: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    onPatternAnalysisClick: () -> Unit = {},
    onOpenAddConsumption: (String?) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val selectedTab = initialTab

    var selectedConsumptionCategoryFilter by remember { mutableStateOf<String?>(null) }
    var activeAntihistamineFilter by remember { mutableStateOf<String?>(null) }
    var isBioTreatmentActive by remember { mutableStateOf(false) }

    LaunchedEffect(selectedTab) {
        selectedConsumptionCategoryFilter = null
        activeAntihistamineFilter = null
        isBioTreatmentActive = false
    }

    // State for manual entry dialog pill choice
    var showDateTimePickerDialog by remember { mutableStateOf(false) }
    var showPillChoiceDialog by remember { mutableStateOf(false) }
    var showXolairChoiceDialog by remember { mutableStateOf(false) }
    var pendingManualTimestamp by remember { mutableStateOf("") }

    val savedAlternatives by viewModel.customAlternatives.collectAsState(initial = emptyList())
    val antihistamines by viewModel.profileAntihistamines.collectAsState()
    val profileCortisones by viewModel.profileCortisones.collectAsState()

    val profileOnXolairState by viewModel.profileOnXolair.collectAsState()
    val profileBiologicalMedicationState by viewModel.profileBiologicalMedication.collectAsState()
    val profileBiologicalMgState by viewModel.profileBiologicalMg.collectAsState()

    val isCustomBiologicalActive = profileOnXolairState &&
            profileBiologicalMedicationState.trim().isNotEmpty() &&
            profileBiologicalMgState.trim().isNotEmpty()

    var editingEntry by remember { mutableStateOf<LogEntry?>(null) }
    var editTimestamp by remember { mutableStateOf("") }
    var editMedName by remember { mutableStateOf("") }
    var editMedMgs by remember { mutableStateOf("") }
    var editWearingOffTimestamp by remember { mutableStateOf<String?>(null) }
    val collapsedFlareUps = remember { mutableStateListOf<String>() }

    var showFlareUpManualDialog by remember { mutableStateOf(false) }
    val selectedManualReasons = remember { mutableStateListOf<String>() }
    val editMedicineOtherNameText = remember { mutableStateOf("") }
    val editMedicineOtherCauseText = remember { mutableStateOf("") }
    val editInsectText = remember { mutableStateOf("") }
    val manualMedicineOtherNameText = remember { mutableStateOf("") }
    val manualMedicineOtherCauseText = remember { mutableStateOf("") }
    val manualInsectText = remember { mutableStateOf("") }
    val selectedEditReasons = remember { mutableStateListOf<String>() }
    val editVegetablesText = remember { mutableStateOf("") }
    val editFruitsText = remember { mutableStateOf("") }
    val editIllnessText = remember { mutableStateOf("") }
    val manualVegetablesText = remember { mutableStateOf("") }
    val manualFruitsText = remember { mutableStateOf("") }
    val manualIllnessText = remember { mutableStateOf("") }
    var manualSeverity by remember { mutableStateOf("Mild") }
    var manualAngioedema by remember { mutableStateOf("No") }
    var manualSeverityDropdownExpanded by remember { mutableStateOf(false) }
    var manualAngioDropdownExpanded by remember { mutableStateOf(false) }
    var editSeverity by remember { mutableStateOf("Mild") }
    var editAngioedema by remember { mutableStateOf("No") }
    var editSeverityDropdownExpanded by remember { mutableStateOf(false) }
    var editAngioDropdownExpanded by remember { mutableStateOf(false) }

    var showCriticalSafetyDialog by remember { mutableStateOf(false) }
    var previousManualSeverity by remember { mutableStateOf("Mild") }
    var showEditCriticalSafetyDialog by remember { mutableStateOf(false) }
    var previousEditSeverity by remember { mutableStateOf("Mild") }

    fun getSeverityColor(sev: String): Color {
        return when (sev) {
            "Mild" -> Color(0xFF509729)
            "Moderate" -> Color(0xFFE5B93D)
            "Severe" -> Color(0xFFF78325)
            "Critical", "Very Severe" -> Color(0xFFD64545)
            else -> Color(0xFF509729)
        }
    }

    fun getSeverityDot(sev: String): String {
        return when (sev) {
            "Mild" -> "🟢"
            "Moderate" -> "🟡"
            "Severe" -> "🟠"
            "Critical", "Very Severe" -> "🔴"
            else -> "🟢"
        }
    }

    var showAntihistamineManualDialog by remember { mutableStateOf(false) }
    var showCortisoneManualDialog by remember { mutableStateOf(false) }
    var showCortisoneManualWarning by remember { mutableStateOf(false) }
    var manualMedName by remember { mutableStateOf("") }
    var manualMedMgs by remember { mutableStateOf("") }
    var showAutocompleteSuggestions by remember { mutableStateOf(false) }
    var showManualAutocompleteSuggestions by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var selectedManualIndex by remember { mutableIntStateOf(-1) }
    var showApdExplanationDialog by remember { mutableStateOf(false) }

    var showResetConfirmDialog by remember { mutableStateOf(false) }

    // Active tab-specific color schemes
    val themeColor = when (selectedTab) {
        0 -> Color(0xFF509729)
        1 -> CoralPink
        2 -> Color(0xFFF78325)
        else -> Color(0xFF509729)
    }

    // Filter logs for the active tab
    val filteredEntries = remember(entries, selectedTab, selectedConsumptionCategoryFilter, activeAntihistamineFilter, isBioTreatmentActive) {
        when (selectedTab) {
            0 -> {
                entries.filter { entry ->
                    if (isBioTreatmentActive) {
                        entry.type == EntryType.XOLAIR_150 || 
                        entry.type == EntryType.XOLAIR_300 || 
                        entry.type == EntryType.ALTERNATIVE ||
                        entry.type == EntryType.CORTISONE
                    } else {
                        if (entry.type != EntryType.ANTIHISTAMINE && entry.type != EntryType.CORTISONE) return@filter false
                        if (activeAntihistamineFilter != null && entry.type == EntryType.ANTIHISTAMINE) {
                            val shortName = entry.metadata?.split(":::")?.getOrNull(0) ?: ""
                            shortName.lowercase() == activeAntihistamineFilter!!.lowercase()
                        } else {
                            true
                        }
                    }
                }
            }
            1 -> entries.filter { it.type == EntryType.FLARE_UP }
            2 -> {
                val raw = entries.filter { it.type == EntryType.CONSUMPTION }
                if (selectedConsumptionCategoryFilter == null) {
                    raw
                } else {
                    raw.filter { e ->
                        val parts = e.metadata?.split(":::")
                        parts != null && parts.size >= 3 && parts[0] == "Consumption" && parts[2].equals(selectedConsumptionCategoryFilter, ignoreCase = true)
                    }
                }
            }
            else -> emptyList()
        }
    }

    val displayEntries = remember(filteredEntries) {
        filteredEntries
    }

    // Format date using Recent friendly relative date-time system
    fun formatEntryDate(isoString: String): String {
        return try {
            val dt = ZonedDateTime.parse(isoString).withZoneSameInstant(java.time.ZoneId.systemDefault())
            val now = ZonedDateTime.now(java.time.ZoneId.systemDefault())
            
            val relativeDate = when {
                dt.toLocalDate().isEqual(now.toLocalDate()) -> "Today"
                dt.toLocalDate().isEqual(now.toLocalDate().minusDays(1)) -> "Yesterday"
                else -> {
                    val rawMonth = dt.month.name.lowercase().replaceFirstChar { it.uppercase() }
                    val month = if (rawMonth.length > 3) rawMonth.substring(0, 3) else rawMonth
                    "$month ${dt.dayOfMonth}, ${dt.year}"
                }
            }
            
            // Format time as hh:mm a
            val hour = dt.hour
            val minute = dt.minute
            val ampm = if (hour >= 12) "PM" else "AM"
            val displayHour = if (hour % 12 == 0) 12 else hour % 12
            val displayMinute = String.format("%02d", minute)
            
            "$relativeDate • $displayHour:$displayMinute $ampm"
        } catch (e: Exception) {
            isoString
        }
    }

    // Time difference between consecutive entries in Medication and Flare Up (e.g. 1d5h, 2d, 8h, 30m)
    fun getGapText(item: LogEntry): String? {
        val sameTypeEntries = filteredEntries.filter {
            if (item.type == EntryType.ANTIHISTAMINE || item.type == EntryType.CORTISONE) {
                it.type == EntryType.ANTIHISTAMINE || it.type == EntryType.CORTISONE
            } else {
                it.type == item.type
            }
        }
        val nextOlder = sameTypeEntries.getOrNull(sameTypeEntries.indexOf(item) + 1) ?: return null
        return try {
            val d1 = ZonedDateTime.parse(item.timestamp).withZoneSameInstant(java.time.ZoneId.systemDefault())
            val d2 = ZonedDateTime.parse(nextOlder.timestamp).withZoneSameInstant(java.time.ZoneId.systemDefault())
            val diffMins = Math.abs(Duration.between(d2, d1).toMinutes())
            val days = diffMins / (24 * 60)
            val remainingMinsAfterDays = diffMins % (24 * 60)
            val hours = remainingMinsAfterDays / 60
            val mins = remainingMinsAfterDays % 60

            when {
                days > 0 -> {
                    if (hours > 0) "${days}d${hours}h" else "${days}d"
                }
                hours > 0 -> {
                    if (mins > 0) "${hours}h${mins}m" else "${hours}h"
                }
                else -> "${mins}m"
            }
        } catch (e: Exception) {
            null
        }
    }

    // Check if consecutive pills gap is less than 8 hours (for Pills Log tab styling)
    fun isPillIntervalAlert(item: LogEntry): Boolean {
        if (selectedTab != 0) return false
        val nextOlder = filteredEntries.getOrNull(filteredEntries.indexOf(item) + 1) ?: return false
        return try {
            val d1 = ZonedDateTime.parse(item.timestamp).withZoneSameInstant(java.time.ZoneId.systemDefault())
            val d2 = ZonedDateTime.parse(nextOlder.timestamp).withZoneSameInstant(java.time.ZoneId.systemDefault())
            val diffMins = Math.abs(Duration.between(d1, d2).toMinutes())
            diffMins < 8 * 60
        } catch (e: Exception) {
            false
        }
    }

    // Finds the next older pill entry that also violated the 8-hour compliance interval
    fun getLastAlertTime(item: LogEntry): String? {
        val currentIndex = filteredEntries.indexOf(item)
        if (currentIndex == -1) return null
        for (i in (currentIndex + 1) until filteredEntries.size) {
            val currentItem = filteredEntries[i]
            val nextOlder = filteredEntries.getOrNull(i + 1) ?: continue
            try {
                val d1 = ZonedDateTime.parse(currentItem.timestamp).withZoneSameInstant(java.time.ZoneId.systemDefault())
                val d2 = ZonedDateTime.parse(nextOlder.timestamp).withZoneSameInstant(java.time.ZoneId.systemDefault())
                val diffMins = Math.abs(Duration.between(d1, d2).toMinutes())
                if (diffMins < 8 * 60) {
                    return formatEntryDate(currentItem.timestamp)
                }
            } catch (e: Exception) {
                // ignore
            }
        }
        return null
    }

    // Trigger simple native Date & Time picker overlay
    fun performManualEntryFlow(onCompleted: (String) -> Unit) {
        val currentDateTime = ZonedDateTime.now()
        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val timePickerDialog = TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        try {
                            val selectedDateTime = ZonedDateTime.of(
                                year, month + 1, dayOfMonth,
                                hourOfDay, minute, 0, 0,
                                currentDateTime.zone
                            )
                            onCompleted(selectedDateTime.toString())
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error constructing date", Toast.LENGTH_SHORT).show()
                        }
                    },
                    currentDateTime.hour,
                    currentDateTime.minute,
                    true
                )
                timePickerDialog.show()
            },
            currentDateTime.year,
            currentDateTime.monthValue - 1,
            currentDateTime.dayOfMonth
        )
        datePickerDialog.show()
    }

    // Export CSV data filtered within a date range via Native Share Chooser
    fun performExportFlow() {
        if (filteredEntries.isEmpty()) {
            Toast.makeText(context, "No entries available to export.", Toast.LENGTH_SHORT).show()
            return
        }

        // 1. Pick Start Date
        val current = ZonedDateTime.now()
        DatePickerDialog(
            context,
            { _, sYear, sMonth, sDay ->
                val startDate = ZonedDateTime.of(sYear, sMonth + 1, sDay, 0, 0, 0, 0, current.zone)
                
                // 2. Pick End Date
                DatePickerDialog(
                    context,
                    { _, eYear, eMonth, eDay ->
                        val endDate = ZonedDateTime.of(eYear, eMonth + 1, eDay, 23, 59, 59, 0, current.zone)
                        
                        if (startDate.isAfter(endDate)) {
                            Toast.makeText(context, "Start date cannot be after end date.", Toast.LENGTH_SHORT).show()
                            return@DatePickerDialog
                        }

                        // 3. Filter entries in range
                        val rangeEntries = filteredEntries.filter { e ->
                            try {
                                val t = ZonedDateTime.parse(e.timestamp)
                                !t.isBefore(startDate) && !t.isAfter(endDate)
                            } catch (err: Exception) {
                                false
                            }
                        }

                        if (rangeEntries.isEmpty()) {
                            Toast.makeText(context, "No entries found in this date range.", Toast.LENGTH_SHORT).show()
                            return@DatePickerDialog
                        }

                        // 4. Construct CSV spreadsheet string
                        val csv = StringBuilder()
                        csv.append("Date/Time,Log Category,Dosage/MetadataDetails\n")
                        
                        rangeEntries.forEach { e ->
                            val readableDate = formatEntryDate(e.timestamp)
                            val typeLabel = when (e.type) {
                                EntryType.FLARE_UP -> "Symptom Flare Up"
                                EntryType.ANTIHISTAMINE -> "Antihistamine Intake"
                                EntryType.CORTISONE -> "Corticosteroids"
                                EntryType.XOLAIR_150 -> "Xolair Injection"
                                EntryType.XOLAIR_300 -> "Xolair Injection"
                                EntryType.ALTERNATIVE -> "Alternative Medication"
                                EntryType.CONSUMPTION -> "Consumption Entry"
                            }
                            val detail = if (e.type == EntryType.FLARE_UP) {
                                if (e.metadata.isNullOrEmpty()) {
                                    "Recorded"
                                } else {
                                    val severity = when {
                                        e.metadata.contains("Severity: Critical") -> "Critical"
                                        e.metadata.contains("Severity: Very Severe") -> "Very Severe"
                                        e.metadata.contains("Severity: Severe") -> "Severe"
                                        e.metadata.contains("Severity: Moderate") -> "Moderate"
                                        else -> "Mild"
                                    }
                                    val angio = if (e.metadata.contains("Angioedema: Yes")) "Yes" else "No"
                                    val cleanReasons = e.metadata
                                        .replace("Severity: Very Severe", "")
                                        .replace("Severity: Severe", "")
                                        .replace("Severity: Moderate", "")
                                        .replace("Severity: Mild", "")
                                        .replace("Severity: Critical", "")
                                        .replace("Angioedema: Yes", "")
                                        .replace("Angioedema: No", "")
                                        .replace("Title: Angioedema", "")
                                        .trim()
                                        .removePrefix(";")
                                        .removeSuffix(";")
                                        .trim()
                                        .split("; ")
                                        .map { it.trim() }
                                        .filter { it.isNotEmpty() }
                                        .joinToString(", ")
                                    if (cleanReasons.isNotEmpty()) {
                                        "Severity: $severity | Angioedema: $angio | Reasons: $cleanReasons"
                                    } else {
                                        "Severity: $severity | Angioedema: $angio"
                                    }
                                }
                            } else if (e.type == EntryType.ANTIHISTAMINE || e.type == EntryType.CORTISONE) {
                                if (e.metadata.isNullOrEmpty()) {
                                    "Recorded"
                                } else {
                                    val parts = e.metadata.split(":::")
                                    val base = if (parts.size >= 2) "${parts[0]} (${parts[1]} mg)" else parts[0]
                                    val reason = if (parts.size >= 3) parts.drop(2).joinToString(":::") else ""
                                    if (reason.isNotEmpty()) "$base | $reason" else base
                                }
                            } else if (e.type == EntryType.ALTERNATIVE) {
                                val baseVal = if (e.metadata.isNullOrEmpty()) {
                                    "Recorded"
                                } else {
                                    val parts = e.metadata.split(":::")
                                    val base = parts[0]
                                    val reasonParts = if (parts.size >= 2) parts.drop(1).filter { !it.startsWith("wearing_off:") } else emptyList()
                                    val reason = reasonParts.joinToString(":::")
                                    if (reason.isNotEmpty()) "$base | $reason" else base
                                }
                                val wearingOffTime = getWearingOffTimestamp(e)
                                val wearingOffSuffix = if (wearingOffTime != null) {
                                    val duration = calculateWearingOffDuration(e.timestamp, wearingOffTime)
                                    if (duration.isNotEmpty()) " | Wore off: $duration" else ""
                                } else ""
                                "$baseVal$wearingOffSuffix"
                            } else {
                                val cleanMeta = e.metadata?.split(":::").orEmpty().filter { !it.startsWith("wearing_off:") }.firstOrNull()
                                val baseVal = cleanMeta ?: when (e.type) {
                                    EntryType.XOLAIR_150 -> "150 mg"
                                    EntryType.XOLAIR_300 -> "300 mg"
                                    else -> "Recorded"
                                }
                                val wearingOffTime = getWearingOffTimestamp(e)
                                val wearingOffSuffix = if (wearingOffTime != null) {
                                    val duration = calculateWearingOffDuration(e.timestamp, wearingOffTime)
                                    if (duration.isNotEmpty()) " | Wore off: $duration" else ""
                                } else ""
                                "$baseVal$wearingOffSuffix"
                            }
                            csv.append("\"$readableDate\",\"$typeLabel\",\"$detail\"\n")
                        }

                        // 5. Trigger Android Send Intent to copy/share text spreadsheet
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Urticare Logs Export (${sMonth+1}/$sDay - ${eMonth+1}/$eDay)")
                            putExtra(Intent.EXTRA_TEXT, csv.toString())
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Export Spreadsheet Logs"))
                    },
                    current.year, current.monthValue - 1, current.dayOfMonth
                ).apply {
                    setTitle("Select End Date")
                }.show()
            },
            current.year, current.monthValue - 1, current.dayOfMonth
        ).apply {
            setTitle("Select Start Date")
        }.show()
    }

    // Retrospective Date/Time picker dialog triggers
    fun showEditDialog(item: LogEntry) {
        editingEntry = item
        editTimestamp = item.timestamp
        editWearingOffTimestamp = getWearingOffTimestamp(item)
        selectedEditReasons.clear()
        editIllnessText.value = ""
        editVegetablesText.value = ""
        editFruitsText.value = ""
        editMedicineOtherNameText.value = ""
        editMedicineOtherCauseText.value = ""
        editInsectText.value = ""
        if (item.type == EntryType.FLARE_UP) {
            val metadataStr = item.metadata ?: ""
            editSeverity = when {
                metadataStr.contains("Severity: Critical") -> "Critical"
                metadataStr.contains("Severity: Very Severe") -> "Critical"
                metadataStr.contains("Severity: Severe") -> "Severe"
                metadataStr.contains("Severity: Moderate") -> "Moderate"
                else -> "Mild"
            }
            previousEditSeverity = editSeverity
            showEditCriticalSafetyDialog = false
            editAngioedema = if (metadataStr.contains("Angioedema: Yes")) "Yes" else "No"
            editSeverityDropdownExpanded = false
            editAngioDropdownExpanded = false
            if (metadataStr.isNotEmpty()) {
                val cleanReasons = metadataStr.split("; ")
                    .filter { 
                        it != "Severity: Very Severe" && 
                        it != "Severity: Severe" && 
                        it != "Severity: Moderate" && 
                        it != "Severity: Mild" && 
                        it != "Severity: Critical" && 
                        it != "Angioedema: Yes" && 
                        it != "Angioedema: No" && 
                        it != "Title: Angioedema" && 
                        !it.startsWith("Ongoing Medication:") &&
                        it != "Potentially caused by Autoimmune Progesterone Dermatitis (APD)" &&
                        it != "Possibly a pregnancy-associated flare-up"
                    }
                
                cleanReasons.forEach { r ->
                    when {
                        r.startsWith("Illness:") -> {
                            selectedEditReasons.add("Illness")
                            editIllnessText.value = r.removePrefix("Illness:").trim()
                        }
                        r.startsWith("Vegetables: Other:") -> {
                            selectedEditReasons.add("Vegetables: Other")
                            editVegetablesText.value = r.removePrefix("Vegetables: Other:").trim()
                        }
                        r.startsWith("Fruit: Other:") -> {
                            selectedEditReasons.add("Fruit: Other")
                            editFruitsText.value = r.removePrefix("Fruit: Other:").trim()
                        }
                        r.startsWith("Medicine : Other:") -> {
                            selectedEditReasons.add("Medicine : Other")
                            val valPart = r.removePrefix("Medicine : Other:").trim()
                            if (valPart.contains(" (Cause: ")) {
                                val splitIdx = valPart.indexOf(" (Cause: ")
                                editMedicineOtherNameText.value = valPart.substring(0, splitIdx).trim()
                                editMedicineOtherCauseText.value = valPart.substring(splitIdx + " (Cause: ".length).removeSuffix(")").trim()
                            } else {
                                editMedicineOtherNameText.value = valPart
                            }
                        }
                        r.startsWith("Insect Bite/Sting:") -> {
                            selectedEditReasons.add("Insect Bite/Sting")
                            editInsectText.value = r.removePrefix("Insect Bite/Sting:").trim()
                        }
                        else -> selectedEditReasons.add(r)
                    }
                }
            }
        }
        if (item.type == EntryType.ANTIHISTAMINE || item.type == EntryType.CORTISONE) {
            val parts = (item.metadata ?: "").split(":::")
            if (parts.size >= 2) {
                editMedName = parts[0]
                editMedMgs = parts[1]
            } else {
                editMedName = item.metadata ?: ""
                editMedMgs = ""
            }
        } else if (item.type == EntryType.ALTERNATIVE) {
            val metadata = item.metadata?.split(":::")?.firstOrNull() ?: ""
            var name = metadata
            var mgs = ""
            if (metadata.endsWith(" mg")) {
                val trimmed = metadata.substring(0, metadata.length - 3).trim()
                val spaceIdx = trimmed.lastIndexOf(' ')
                if (spaceIdx != -1) {
                    val mgStr = trimmed.substring(spaceIdx + 1)
                    if (mgStr.toDoubleOrNull() != null) {
                        name = trimmed.substring(0, spaceIdx).trim()
                        mgs = mgStr
                    }
                }
            }
            editMedName = name
            editMedMgs = mgs
        } else {
            editMedName = ""
            editMedMgs = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Button
            GradientBackButton(onClick = onBack, size = 34.dp)

            val brandTitleGradient = Brush.horizontalGradient(
                listOf(Color(0xFF814B92), Color(0xFF509729), Color(0xFF1A7E97))
            )

            // Centered Title
            Text(
                text = "My Logs",
                style = TextStyle(
                    brush = brandTitleGradient,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            // Balanced spacer to keep the title perfectly centered
            Spacer(modifier = Modifier.size(34.dp))
        }

        // Custom M3 Insights Tab Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFEEF1F7), shape = RoundedCornerShape(10.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf("Medication", "Flare Up", "Consumptions")
            tabs.forEachIndexed { index, label ->
                val isActive = selectedTab == index
                val tabBg = if (isActive) {
                    when (index) {
                        0 -> Color(0xFFE8F7EC)
                        1 -> Color(0xFFF3E8FF)
                        2 -> Color(0xFFFFEAD2)
                        else -> Color(0xFFEAF9F0)
                    }
                } else {
                    Color(0xFFEEF1F7)
                }
                val textColor = if (isActive) {
                    when (index) {
                        0 -> Color(0xFF4C9A2A)
                        1 -> Color(0xFF814B92)
                        2 -> Color(0xFFF78325)
                        else -> Color(0xFF4C9A2A)
                    }
                } else {
                    Color(0xFF64748B)
                }
                val underlineColor = when (index) {
                    0 -> Color(0xFF4C9A2A)
                    1 -> Color(0xFF814B92)
                    2 -> Color(0xFFF78325)
                    else -> Color(0xFF4C9A2A)
                }

                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Card(
                        onClick = { onTabSelected(index) },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        colors = CardDefaults.cardColors(containerColor = tabBg),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = label,
                                    color = textColor,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                if (isActive) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(24.dp)
                                            .height(2.5.dp)
                                            .background(underlineColor, RoundedCornerShape(1.25.dp))
                                    )
                                }
                            }
                        }
                    }
                    if (index < tabs.lastIndex) {
                        Spacer(
                            modifier = Modifier
                                .width(1.dp)
                                .height(18.dp)
                                .background(Color(0xFFCBD5E1))
                        )
                    }
                }
            }
        }

        if (selectedTab == 0) {
            if (antihistamines.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    antihistamines.forEachIndexed { index, med ->
                        val isSelected = activeAntihistamineFilter == med.name
                        val isFavorite = med.isMain || index == 0
                        MedicationCard(
                            name = med.name,
                            dosage = med.mgs,
                            isFavorite = isFavorite,
                            isSelected = isSelected,
                            onClick = {
                                activeAntihistamineFilter = if (isSelected) null else med.name
                                isBioTreatmentActive = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            BiologicalTreatmentCard(
                isActive = isBioTreatmentActive,
                onClick = {
                    isBioTreatmentActive = !isBioTreatmentActive
                    activeAntihistamineFilter = null
                }
            )
        }

        if (selectedTab == 2) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val categories = listOf("All", "Food", "Drinks", "Medications", "Supplements", "Other")
                categories.forEach { cat ->
                    val isSelected = (cat == "All" && selectedConsumptionCategoryFilter == null) || (selectedConsumptionCategoryFilter == cat)
                    val activeColor = Color(0xFFF78325)
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .background(
                                color = if (isSelected) activeColor else Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(17.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color.Transparent else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(17.dp)
                            )
                            .clickable { 
                                selectedConsumptionCategoryFilter = if (cat == "All") null else cat
                            }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cat,
                            color = if (isSelected) Color.White else Color(0xFF64748B),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Actions panel (Add Entry, Export, Reset Log)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.2.dp, Color(0xFFE5E7EB)),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp, horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Button 1: Add Entry
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            showDateTimePickerDialog = true
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    PlusInCircleIcon(color = Color(0xFF4C9A2A))
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Add Entry",
                        color = Color(0xFF1F2937),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Divider 1
                Spacer(
                    modifier = Modifier
                        .width(1.dp)
                        .height(22.dp)
                        .background(Color(0xFFE5E7EB))
                )

                // Button 2: Export
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { performExportFlow() },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    DownloadArrowIcon(color = Color(0xFF4C9A2A))
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Export",
                        color = Color(0xFF1F2937),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Divider 2
                Spacer(
                    modifier = Modifier
                        .width(1.dp)
                        .height(22.dp)
                        .background(Color(0xFFE5E7EB))
                )

                // Button 3: Reset Log
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showResetConfirmDialog = true },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    TrashCanIcon(color = Color(0xFFE53935))
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Reset Log",
                        color = Color(0xFFE53935),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }


        Spacer(modifier = Modifier.height(14.dp))

        // Logs Render List Container (Unified single box card)
        if (displayEntries.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No entries found for this tracking window",
                        color = MutedGray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    displayEntries.forEachIndexed { idx, item ->
                        val friendlyLabel = when (item.type) {
                            EntryType.FLARE_UP -> "Flare Up"
                            EntryType.ANTIHISTAMINE -> {
                                if (!item.metadata.isNullOrEmpty()) {
                                    val parts = item.metadata.split(":::")
                                    val rawName = if (parts.size >= 2) cleanMedicationName(parts[0]) else cleanMedicationName(item.metadata)
                                    val dose = if (parts.size >= 2) " (${parts[1]} mg)" else ""
                                    "$rawName$dose"
                                } else {
                                    "Antihistamine"
                                }
                            }
                            EntryType.CORTISONE -> {
                                if (!item.metadata.isNullOrEmpty()) {
                                    val parts = item.metadata.split(":::")
                                    val rawName = if (parts.size >= 2) cleanMedicationName(parts[0]) else cleanMedicationName(item.metadata)
                                    val dose = if (parts.size >= 2) " (${parts[1]} mg)" else ""
                                    "$rawName$dose"
                                } else {
                                    "Corticosteroid"
                                }
                            }
                            EntryType.XOLAIR_150 -> "Xolair 150 mg"
                            EntryType.XOLAIR_300 -> "Xolair 300 mg"
                            EntryType.ALTERNATIVE -> item.metadata?.split(":::")?.firstOrNull() ?: "Alternative Med"
                            EntryType.CONSUMPTION -> {
                                if (!item.metadata.isNullOrEmpty()) {
                                    val parts = item.metadata.split(":::")
                                    if (parts.size >= 2 && parts[0] == "Consumption") parts[1] else item.metadata
                                } else {
                                    "Consumption"
                                }
                            }
                        }

                        val gapText = getGapText(item)

                        // Determine type-based styling & values
                        var dotColor = Color(0xFF4C9A2A)
                        var lineColor = Color(0xFFC8E6C9)
                        var pillBg = Color(0xFFE8F7EC)
                        var pillText = Color(0xFF2E7D32)
                        var displayTitle = friendlyLabel
                        var detailsContent: @Composable (() -> Unit)? = null

                        if (item.type == EntryType.ANTIHISTAMINE || item.type == EntryType.CORTISONE) {
                            val isCortisone = item.type == EntryType.CORTISONE
                            dotColor = if (isCortisone) Color(0xFF1A7E97) else Color(0xFF4C9A2A)
                            lineColor = if (isCortisone) Color(0xFFA8D8D8) else Color(0xFFC8E6C9)
                            pillBg = if (isCortisone) Color(0xFFE6F4F8) else Color(0xFFE8F7EC)
                            pillText = if (isCortisone) Color(0xFF1A7E97) else Color(0xFF2E7D32)
                        } else if (item.type == EntryType.FLARE_UP) {
                            dotColor = Color(0xFF814B92)
                            lineColor = Color(0xFF814B92).copy(alpha = 0.3f)
                            pillBg = Color(0xFFF3E8FF)
                            pillText = Color(0xFF814B92)
                            displayTitle = if (item.metadata?.contains("Title: Angioedema") == true) "Angioedema Only" else "Flare Up"
                            
                            val severity = when {
                                item.metadata?.contains("Severity: Critical") == true -> "Critical"
                                item.metadata?.contains("Severity: Very Severe") == true -> "Very Severe"
                                item.metadata?.contains("Severity: Severe") == true -> "Severe"
                                item.metadata?.contains("Severity: Moderate") == true -> "Moderate"
                                else -> "Mild"
                            }
                            val severityColor = when (severity) {
                                "Mild" -> Color(0xFF10B981)
                                "Moderate" -> Color(0xFFD97706)
                                "Severe", "Very Severe" -> Color(0xFFEA580C)
                                "Critical" -> Color(0xFFEF4444)
                                else -> Color(0xFF64748B)
                            }
                            val angio = if (item.metadata?.contains("Angioedema: Yes") == true) "Yes" else "No"

                            detailsContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Severity: ",
                                        color = Color(0xFF64748B),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = severity,
                                        color = severityColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = " | Angioedema: $angio",
                                        color = Color(0xFF64748B),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        } else if (item.type == EntryType.CONSUMPTION) {
                            dotColor = Color(0xFFF78325)
                            lineColor = Color(0xFFF78325).copy(alpha = 0.3f)
                            pillBg = Color(0xFFFFEAD2)
                            pillText = Color(0xFFF78325)

                            if (!item.metadata.isNullOrEmpty()) {
                                val parts = item.metadata.split(":::")
                                if (parts.size >= 5 && parts[0] == "Consumption") {
                                    val cat = parts[2]
                                    val amt = parts[3]
                                    val nts = parts[4]
                                    val status = if (parts.size >= 6) parts[5] else "Logged"
                                    val isTrigger = status == "Trigger"

                                    val detailsList = mutableListOf<String>()
                                    if (amt.isNotEmpty()) detailsList.add("Amount: $amt")
                                    if (nts.isNotEmpty()) detailsList.add("Notes: $nts")
                                    val detailLabel = detailsList.joinToString(" • ")

                                    detailsContent = {
                                        Column {
                                            if (detailLabel.isNotEmpty()) {
                                                Text(
                                                    text = detailLabel,
                                                    color = Color(0xFF64748B),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                            }
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.padding(top = 4.dp)
                                            ) {
                                                // Mark as Trigger Button
                                                Button(
                                                    onClick = {
                                                        val currentStatus = if (parts.size >= 6) parts[5] else "Logged"
                                                        val newStatus = if (currentStatus == "Trigger") "Logged" else "Trigger"
                                                        val newParts = parts.toMutableList()
                                                        while (newParts.size < 6) {
                                                            newParts.add("")
                                                        }
                                                        newParts[5] = newStatus
                                                        val newMetadata = newParts.joinToString(":::")
                                                        viewModel.updateEntryDetails(item.id, item.timestamp, newMetadata)
                                                        val msg = if (newStatus == "Trigger") "Marked as Trigger" else "Unmarked as Trigger"
                                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (isTrigger) Color(0xFFFEE2E2) else Color(0xFFF1F5F9),
                                                        contentColor = if (isTrigger) Color(0xFFEF4444) else Color(0xFF64748B)
                                                    ),
                                                    border = BorderStroke(1.dp, if (isTrigger) Color(0xFFFCA5A5) else Color(0xFFE2E8F0)),
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.height(28.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = if (isTrigger) "⚠️ Marked as Trigger" else "Mark as Trigger",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }

                                                // Helper text for previous trigger warning
                                                val itemName = parts[1]
                                                val hasBeenTrigger = entries.any { e ->
                                                    e.type == EntryType.CONSUMPTION &&
                                                    e.id != item.id &&
                                                    (e.metadata?.split(":::")?.getOrNull(1)?.lowercase() == itemName.lowercase()) &&
                                                    (e.metadata?.split(":::")?.getOrNull(5) == "Trigger")
                                                }
                                                if (hasBeenTrigger) {
                                                    Text(
                                                        text = "⚠️ Previously marked as trigger",
                                                        color = Color(0xFFF78325),
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            dotColor = Color(0xFF1A7E97)
                            lineColor = Color(0xFFA8D8D8)
                            pillBg = Color(0xFFE6F4F8)
                            pillText = Color(0xFF1A7E97)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left: Timeline line + dot
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(2.dp)
                                        .background(lineColor)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(dotColor, RoundedCornerShape(5.dp))
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Middle: Date/Time + Name & Details
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = formatEntryDate(item.timestamp),
                                    color = Color(0xFF64748B),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = displayTitle,
                                    color = Color(0xFF0F172A),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (detailsContent != null) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    detailsContent()
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Right: Time-since pill + actions
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (gapText != null) {
                                    Box(
                                        modifier = Modifier
                                            .background(pillBg, RoundedCornerShape(12.dp))
                                            .padding(horizontal = 7.dp, vertical = 2.5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = gapText,
                                            color = pillText,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Edit action icon (same circular style as Recent)
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(Color(0xFFF1F5F9), shape = RoundedCornerShape(16.dp))
                                            .clickable {
                                                showEditDialog(item)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        RecentEditPencilIcon(
                                            modifier = Modifier.size(16.dp),
                                            color = Color(0xFF64748B)
                                        )
                                    }

                                    // Delete action icon (same circular style as Recent)
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(Color(0xFFFEF2F2), shape = RoundedCornerShape(16.dp))
                                            .clickable {
                                                viewModel.deleteEntry(item.id)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        RecentDeleteTrashIcon(
                                            modifier = Modifier.size(16.dp),
                                            color = Color(0xFFEF4444)
                                        )
                                    }
                                }
                            }
                        }

                        if (idx < displayEntries.lastIndex) {
                            HorizontalDivider(
                                color = Color(0xFFEDF1F5),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDateTimePickerDialog) {
        DateTimePickerDialog(
            initialTimestamp = null,
            themeColor = when (selectedTab) {
                0 -> if (isBioTreatmentActive) Color(0xFF1A7E97) else Color(0xFF4C9A2A)
                1 -> Color(0xFF814B92)
                2 -> Color(0xFFF78325)
                else -> Color(0xFF814B92)
            },
            onDismiss = { showDateTimePickerDialog = false },
            onConfirm = { timestamp ->
                showDateTimePickerDialog = false
                pendingManualTimestamp = timestamp
                when (selectedTab) {
                    0 -> {
                        if (isBioTreatmentActive) {
                            showXolairChoiceDialog = true
                        } else {
                            showPillChoiceDialog = true
                        }
                    }
                    1 -> {
                        selectedManualReasons.clear()
                        manualIllnessText.value = ""
                        manualVegetablesText.value = ""
                        manualFruitsText.value = ""
                        manualMedicineOtherNameText.value = ""
                        manualMedicineOtherCauseText.value = ""
                        manualInsectText.value = ""
                        manualSeverity = "Mild"
                        previousManualSeverity = "Mild"
                        manualAngioedema = "No"
                        manualSeverityDropdownExpanded = false
                        manualAngioDropdownExpanded = false
                        showCriticalSafetyDialog = false
                        showFlareUpManualDialog = true
                    }
                    2 -> {
                        onOpenAddConsumption(pendingManualTimestamp)
                    }
                }
            }
        )
    }

    // Manual Entry Choice Dialog (Pills)
    if (showPillChoiceDialog) {
        AlertDialog(
            onDismissRequest = { showPillChoiceDialog = false },
            containerColor = DarkSurface,
            title = { Text(text = "Select Pill Type", color = Color(0xFF509729), fontWeight = FontWeight.Bold) },
            text = { Text(text = "Which pill type are you logging retrospectively?", color = LightGray) },
            confirmButton = {
                Button(
                    onClick = {
                        showPillChoiceDialog = false
                        manualMedName = ""
                        manualMedMgs = ""
                        showAntihistamineManualDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF509729), contentColor = AmoledBlack)
                ) {
                    Text(text = "Antihistamine", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showPillChoiceDialog = false
                        if (profileCortisones.isEmpty()) {
                            showCortisoneManualWarning = true
                        } else {
                            showCortisoneManualDialog = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF509729), contentColor = AmoledBlack)
                ) {
                    Text(text = "Cortisone", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Manual Entry Choice Dialog (Xolair / Alt)
    if (showXolairChoiceDialog) {
        AlertDialog(
            onDismissRequest = { showXolairChoiceDialog = false },
            containerColor = DarkSurface,
            title = { Text(text = "Select Injection / Dosage", color = Color(0xFF1A7E97), fontWeight = FontWeight.Bold) },
            text = { Text(text = "Choose which type to log retrospectively:", color = LightGray) },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isCustomBiologicalActive) {
                        val medLabel = "$profileBiologicalMedicationState $profileBiologicalMgState mg"
                        Button(
                            onClick = {
                                val medNameLower = profileBiologicalMedicationState.trim().lowercase()
                                val mgClean = profileBiologicalMgState.trim()
                                if (medNameLower == "xolair" && mgClean == "150") {
                                    viewModel.addEntry(EntryType.XOLAIR_150, pendingManualTimestamp, "150 mg")
                                    Toast.makeText(context, "Xolair 150 mg logged", Toast.LENGTH_SHORT).show()
                                } else if (medNameLower == "xolair" && mgClean == "300") {
                                    viewModel.addEntry(EntryType.XOLAIR_300, pendingManualTimestamp, "300 mg")
                                    Toast.makeText(context, "Xolair 300 mg logged", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.addEntry(EntryType.ALTERNATIVE, pendingManualTimestamp, medLabel)
                                    Toast.makeText(context, "$medLabel logged", Toast.LENGTH_SHORT).show()
                                }
                                showXolairChoiceDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkCard, contentColor = Color(0xFF1A7E97)),
                            border = BorderStroke(1.dp, Color(0xFF1A7E97).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = medLabel, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = {
                                viewModel.addEntry(EntryType.XOLAIR_150, pendingManualTimestamp, "150 mg")
                                showXolairChoiceDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkCard, contentColor = Color(0xFF1A7E97)),
                            border = BorderStroke(1.dp, Color(0xFF1A7E97).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Xolair 150 mg", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                viewModel.addEntry(EntryType.XOLAIR_300, pendingManualTimestamp, "300 mg")
                                showXolairChoiceDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkCard, contentColor = Color(0xFF1A7E97)),
                            border = BorderStroke(1.dp, Color(0xFF1A7E97).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Xolair 300 mg", fontWeight = FontWeight.Bold)
                        }
                    }

                    TextButton(
                        onClick = { showXolairChoiceDialog = false },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(text = "Cancel", color = MutedGray)
                    }
                }
            },
            dismissButton = null
        )
    }

    if (showAntihistamineManualDialog) {
        AlertDialog(
            onDismissRequest = { showAntihistamineManualDialog = false },
            containerColor = DarkSurface,
            title = { Text(text = "Log Antihistamine Entry", color = Color(0xFF509729), fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "Date/Time: ${formatEntryDate(pendingManualTimestamp)}", color = LightGray, fontSize = 11.sp)
                    
                    if (antihistamines.isNotEmpty()) {
                        Text(
                            text = "Choose from profile:",
                            color = MutedGray,
                            fontSize = 10.sp
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            antihistamines.forEach { med ->
                                Card(
                                    onClick = {
                                        manualMedName = med.name
                                        manualMedMgs = med.mgs
                                    },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (manualMedName == med.name && manualMedMgs == med.mgs) Color(0xFF509729).copy(alpha = 0.2f) else DarkCard
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (manualMedName == med.name && manualMedMgs == med.mgs) Color(0xFF509729) else DarkBorder
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${med.name} (${med.mgs} mg)",
                                            color = if (manualMedName == med.name && manualMedMgs == med.mgs) Color(0xFF509729) else LightGray,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        val query = manualMedName.trim().lowercase()
                        val suggestions = remember(query) {
                            if (query.isEmpty()) emptyList() else {
                                val list = MedicationDirectory.antihistamines
                                fun normalize(s: String): String = s.replace("–", "-").replace("—", "-").trim().lowercase()
                                val normalizedQuery = normalize(query)
                                val exactMatches = list.filter { med ->
                                    val normalizedFull = normalize(med)
                                    val parts = med.split(" – ")
                                    val normalizedName = if (parts.isNotEmpty()) normalize(parts[0]) else ""
                                    normalizedFull == normalizedQuery || normalizedName == normalizedQuery
                                }
                                val startsWithMatches = list.filter { med ->
                                    if (exactMatches.contains(med)) return@filter false
                                    val normalizedFull = normalize(med)
                                    val parts = med.split(" – ")
                                    val normalizedName = if (parts.isNotEmpty()) normalize(parts[0]) else ""
                                    normalizedFull.startsWith(normalizedQuery) || normalizedName.startsWith(normalizedQuery)
                                }
                                val containsMatches = list.filter { med ->
                                    if (exactMatches.contains(med) || startsWithMatches.contains(med)) return@filter false
                                    val normalizedFull = normalize(med)
                                    normalizedFull.contains(normalizedQuery)
                                }
                                (exactMatches + startsWithMatches + containsMatches).take(8)
                            }
                        }

                        LaunchedEffect(suggestions) {
                            selectedManualIndex = -1
                        }

                        OutlinedTextField(
                            value = manualMedName,
                            onValueChange = { 
                                manualMedName = it
                                showManualAutocompleteSuggestions = true
                            },
                            label = { Text("Med Name", color = MutedGray, fontSize = 11.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF509729),
                                unfocusedBorderColor = DarkBorder,
                                focusedContainerColor = DarkCard,
                                unfocusedContainerColor = DarkCard
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { focusState ->
                                    showManualAutocompleteSuggestions = focusState.isFocused
                                }
                                .onKeyEvent { keyEvent ->
                                    if (showManualAutocompleteSuggestions && suggestions.isNotEmpty() && keyEvent.type == KeyEventType.KeyDown) {
                                        when (keyEvent.key) {
                                            Key.DirectionDown -> {
                                                selectedManualIndex = (selectedManualIndex + 1) % suggestions.size
                                                true
                                            }
                                            Key.DirectionUp -> {
                                                selectedManualIndex = if (selectedManualIndex <= 0) suggestions.size - 1 else selectedManualIndex - 1
                                                true
                                            }
                                            Key.Enter -> {
                                                if (selectedManualIndex in suggestions.indices) {
                                                    manualMedName = suggestions[selectedManualIndex]
                                                    showManualAutocompleteSuggestions = false
                                                    true
                                                } else {
                                                    false
                                                }
                                            }
                                            else -> false
                                        }
                                    } else {
                                        false
                                    }
                                }
                        )
                        
                        if (showManualAutocompleteSuggestions && manualMedName.trim().isNotEmpty()) {
                            Popup(
                                onDismissRequest = { showManualAutocompleteSuggestions = false },
                                properties = PopupProperties(focusable = false)
                            ) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .border(1.dp, DarkBorder, RoundedCornerShape(8.dp)),
                                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                                    shape = RoundedCornerShape(8.dp),
                                    elevation = CardDefaults.cardElevation(8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 240.dp)
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        if (suggestions.isEmpty()) {
                                            Text(
                                                text = "No medications found.\nNo matching medication found. You can enter it manually.",
                                                color = MutedGray,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(12.dp)
                                            )
                                        } else {
                                            suggestions.forEachIndexed { index, med ->
                                                val isSelected = index == selectedManualIndex
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(if (isSelected) Color(0xFF509729).copy(alpha = 0.2f) else Color.Transparent)
                                                        .clickable {
                                                            manualMedName = med
                                                            showManualAutocompleteSuggestions = false
                                                        }
                                                        .padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    val lowercaseMed = med.lowercase()
                                                    if (lowercaseMed.contains(query)) {
                                                        val parts = mutableListOf<Pair<String, Boolean>>()
                                                        var searchIndex = 0
                                                        while (searchIndex < med.length) {
                                                            val idx = lowercaseMed.indexOf(query, searchIndex)
                                                            if (idx == -1) {
                                                                parts.add(med.substring(searchIndex) to false)
                                                                break
                                                            }
                                                            if (idx > searchIndex) {
                                                                parts.add(med.substring(searchIndex, idx) to false)
                                                            }
                                                            parts.add(med.substring(idx, idx + query.length) to true)
                                                            searchIndex = idx + query.length
                                                        }
                                                        Text(
                                                            text = buildAnnotatedString {
                                                                parts.forEach { (part, isHigh) ->
                                                                    if (isHigh) {
                                                                        withStyle(style = SpanStyle(color = Color(0xFF509729), fontWeight = FontWeight.Bold)) {
                                                                            append(part)
                                                                        }
                                                                    } else {
                                                                        withStyle(style = SpanStyle(color = Color.White)) {
                                                                            append(part)
                                                                        }
                                                                    }
                                                                }
                                                            },
                                                            fontSize = 12.sp
                                                        )
                                                    } else {
                                                        Text(text = med, color = Color.White, fontSize = 12.sp)
                                                    }
                                                }
                                            }
                                        }
                                        
                                        HorizontalDivider(color = DarkBorder)
                                        Text(
                                            text = "Can't find your medication? Continue typing to enter it manually.",
                                            color = PastelIceBlue.copy(alpha = 0.8f),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = manualMedMgs,
                        onValueChange = { manualMedMgs = it },
                        label = { Text("Dosage (mg)", color = MutedGray, fontSize = 11.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF509729),
                            unfocusedBorderColor = DarkBorder,
                            focusedContainerColor = DarkCard,
                            unfocusedContainerColor = DarkCard
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (manualMedName.trim().isEmpty()) {
                            Toast.makeText(context, "Please enter medication name", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (manualMedMgs.trim().isEmpty() || manualMedMgs.trim().toDoubleOrNull() == null) {
                            Toast.makeText(context, "Please enter a valid dosage", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val metadata = "${manualMedName.trim()}:::${manualMedMgs.trim()}"
                        viewModel.addEntry(EntryType.ANTIHISTAMINE, pendingManualTimestamp, metadata)
                        showAntihistamineManualDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF509729), contentColor = AmoledBlack)
                ) {
                    Text(text = "Log Entry", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAntihistamineManualDialog = false }) {
                    Text(text = "Cancel", color = MutedGray)
                }
            }
        )
    }

    if (showCortisoneManualWarning) {
        AlertDialog(
            onDismissRequest = { showCortisoneManualWarning = false },
            containerColor = DarkSurface,
            title = {
                Text(
                    text = "No saved entries",
                    color = Color(0xFF509729),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "You haven't saved any Corticosteroids. Please add them in Your Profile first.",
                    color = LightGray,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showCortisoneManualWarning = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF509729), contentColor = AmoledBlack)
                ) {
                    Text(text = "OK", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showCortisoneManualDialog) {
        AlertDialog(
            onDismissRequest = { showCortisoneManualDialog = false },
            containerColor = DarkSurface,
            title = {
                Text(
                    text = "Select Corticosteroid",
                    color = Color(0xFF1A7E97),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 250.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    profileCortisones.forEach { med ->
                        Card(
                            onClick = {
                                val metadata = "${med.name}:::${med.mgs}"
                                viewModel.addEntry(EntryType.CORTISONE, pendingManualTimestamp, metadata = metadata)
                                showCortisoneManualDialog = false
                                Toast.makeText(context, "Logged Cortisone: ${med.name} (${med.mgs} mg)", Toast.LENGTH_SHORT).show()
                            },
                            colors = CardDefaults.cardColors(containerColor = DarkCard),
                            border = BorderStroke(1.dp, DarkBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = med.name,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${med.mgs} mg",
                                        color = Color(0xFF1A7E97),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCortisoneManualDialog = false }) {
                    Text(text = "Cancel", color = MutedGray)
                }
            }
        )
    }

    if (editingEntry != null) {
        val entry = editingEntry!!
        val isAlt = entry.type == EntryType.ALTERNATIVE
        AlertDialog(
            onDismissRequest = { editingEntry = null },
            containerColor = DarkSurface,
            title = {
                Text(
                    text = "Edit Log Entry",
                    color = if (entry.type == EntryType.CORTISONE) Color(0xFF1A7E97)
                            else if (entry.type == EntryType.ANTIHISTAMINE) Color(0xFF509729)
                            else if (entry.type == EntryType.XOLAIR_150 || entry.type == EntryType.XOLAIR_300) Color(0xFF1A7E97)
                            else if (entry.type == EntryType.ALTERNATIVE) Color(0xFF1A7E97)
                            else CoralPink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Date & Time",
                        color = LightGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = {
                            val currentDateTime = try {
                                ZonedDateTime.parse(editTimestamp)
                            } catch (e: Exception) {
                                ZonedDateTime.now()
                            }
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    TimePickerDialog(
                                        context,
                                        { _, hourOfDay, minute ->
                                            try {
                                                val selectedDateTime = ZonedDateTime.of(
                                                    year, month + 1, dayOfMonth,
                                                    hourOfDay, minute, 0, 0,
                                                    currentDateTime.zone
                                                )
                                                editTimestamp = selectedDateTime.toString()
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Error setting time", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        currentDateTime.hour,
                                        currentDateTime.minute,
                                        true
                                    ).show()
                                },
                                currentDateTime.year,
                                currentDateTime.monthValue - 1,
                                currentDateTime.dayOfMonth
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkCard),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = formatEntryDate(editTimestamp),
                            color = Color(0xFF737373),
                            fontSize = 12.sp
                        )
                    }

                    if (entry.type == EntryType.XOLAIR_150 || entry.type == EntryType.XOLAIR_300 || isAlt) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Treatment Wearing Off Date & Time",
                            color = LightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (editWearingOffTimestamp != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        val currentDateTime = try {
                                            ZonedDateTime.parse(editWearingOffTimestamp)
                                        } catch (e: Exception) {
                                            ZonedDateTime.now()
                                        }
                                        DatePickerDialog(
                                            context,
                                            { _, year, month, dayOfMonth ->
                                                TimePickerDialog(
                                                    context,
                                                    { _, hourOfDay, minute ->
                                                        try {
                                                            val selectedDateTime = ZonedDateTime.of(
                                                                year, month + 1, dayOfMonth,
                                                                hourOfDay, minute, 0, 0,
                                                                currentDateTime.zone
                                                            )
                                                            editWearingOffTimestamp = selectedDateTime.toString()
                                                        } catch (e: Exception) {
                                                            Toast.makeText(context, "Error setting time", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    currentDateTime.hour,
                                                    currentDateTime.minute,
                                                    true
                                                ).show()
                                            },
                                            currentDateTime.year,
                                            currentDateTime.monthValue - 1,
                                            currentDateTime.dayOfMonth
                                        ).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkCard),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = formatEntryDate(editWearingOffTimestamp!!),
                                        color = Color(0xFF737373),
                                        fontSize = 12.sp
                                    )
                                }
                                Button(
                                    onClick = { editWearingOffTimestamp = null },
                                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Text(text = "Clear", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Button(
                                onClick = {
                                    editWearingOffTimestamp = ZonedDateTime.now().toString()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkCard),
                                border = BorderStroke(1.dp, Color(0xFF1A7E97).copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Record Treatment Wearing Off Date",
                                    color = Color(0xFF1A7E97),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (entry.type == EntryType.FLARE_UP) {
                        // Severity & Angioedema Side-by-Side (50% / 50%)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Severity Field
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Severity",
                                    color = CoralPink,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                val sevColor = getSeverityColor(editSeverity)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(DarkSurface, RoundedCornerShape(10.dp))
                                        .border(1.dp, sevColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                        .clickable { editSeverityDropdownExpanded = true }
                                        .padding(horizontal = 10.dp, vertical = 10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.weight(1f, fill = false)
                                        ) {
                                            Text(getSeverityDot(editSeverity), fontSize = 11.sp)
                                            Text(
                                                text = editSeverity,
                                                color = sevColor,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text("▼", color = MutedGray, fontSize = 10.sp)
                                    }
                                    DropdownMenu(
                                        expanded = editSeverityDropdownExpanded,
                                        onDismissRequest = { editSeverityDropdownExpanded = false },
                                        modifier = Modifier
                                            .background(DarkSurface)
                                            .border(1.dp, DarkBorder)
                                    ) {
                                        listOf("Mild", "Moderate", "Severe", "Critical").forEach { level ->
                                            val optionColor = getSeverityColor(level)
                                            DropdownMenuItem(
                                                text = {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(getSeverityDot(level), fontSize = 12.sp)
                                                        Text(
                                                            text = level,
                                                            color = optionColor,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    editSeverityDropdownExpanded = false
                                                    if (level == "Critical") {
                                                        previousEditSeverity = editSeverity
                                                        editSeverity = "Critical"
                                                        showEditCriticalSafetyDialog = true
                                                    } else {
                                                        editSeverity = level
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Angioedema Field
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Angioedema",
                                    color = CoralPink,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(DarkSurface, RoundedCornerShape(10.dp))
                                        .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                                        .clickable { editAngioDropdownExpanded = true }
                                        .padding(horizontal = 10.dp, vertical = 10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = editAngioedema,
                                            color = if (editAngioedema == "Yes") CoralPink else Color(0xFF737373),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text("▼", color = MutedGray, fontSize = 10.sp)
                                    }
                                    DropdownMenu(
                                        expanded = editAngioDropdownExpanded,
                                        onDismissRequest = { editAngioDropdownExpanded = false },
                                        modifier = Modifier
                                            .background(DarkSurface)
                                            .border(1.dp, DarkBorder)
                                    ) {
                                        listOf("No", "Yes").forEach { option ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = option,
                                                        color = if (option == "Yes") CoralPink else Color(0xFF737373),
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                },
                                                onClick = {
                                                    editAngioedema = option
                                                    editAngioDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Potential Reasons",
                            color = LightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            ReasonsSearchableList(
                                selectedReasons = selectedEditReasons,
                                vegetablesText = editVegetablesText,
                                fruitsText = editFruitsText,
                                illnessText = editIllnessText,
                                medicineOtherNameText = editMedicineOtherNameText,
                                medicineOtherCauseText = editMedicineOtherCauseText,
                                insectText = editInsectText,
                                viewModel = viewModel,
                                themeColor = CoralPink,
                                maxHeight = 280,
                                isLightModal = false
                            )
                        }
                    }

                    if (entry.type == EntryType.ANTIHISTAMINE || entry.type == EntryType.CORTISONE || isAlt) {
                        Text(
                            text = "Medication Details",
                            color = LightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        if (entry.type == EntryType.ANTIHISTAMINE && antihistamines.isNotEmpty()) {
                            Text(
                                text = "Choose from profile:",
                                color = MutedGray,
                                fontSize = 10.sp
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                antihistamines.forEach { med ->
                                    Card(
                                        onClick = {
                                            editMedName = med.name
                                            editMedMgs = med.mgs
                                        },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (editMedName == med.name && editMedMgs == med.mgs) Color(0xFF509729).copy(alpha = 0.2f) else DarkCard
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            if (editMedName == med.name && editMedMgs == med.mgs) Color(0xFF509729) else DarkBorder
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "${med.name} (${med.mgs} mg)",
                                                color = if (editMedName == med.name && editMedMgs == med.mgs) Color(0xFF509729) else Color(0xFF737373),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (entry.type == EntryType.CORTISONE && profileCortisones.isNotEmpty()) {
                            Text(
                                text = "Choose from profile:",
                                color = MutedGray,
                                fontSize = 10.sp
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                profileCortisones.forEach { med ->
                                    Card(
                                        onClick = {
                                            editMedName = med.name
                                            editMedMgs = med.mgs
                                        },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (editMedName == med.name && editMedMgs == med.mgs) Color(0xFF1A7E97).copy(alpha = 0.2f) else DarkCard
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            if (editMedName == med.name && editMedMgs == med.mgs) Color(0xFF1A7E97) else DarkBorder
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "${med.name} (${med.mgs} mg)",
                                                color = if (editMedName == med.name && editMedMgs == med.mgs) Color(0xFF1A7E97) else Color(0xFF737373),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            val query = editMedName.trim().lowercase()
                            val suggestions = remember(query) {
                                if (query.isEmpty() || entry.type != EntryType.ANTIHISTAMINE) emptyList() else {
                                    val list = MedicationDirectory.antihistamines
                                    fun normalize(s: String): String = s.replace("–", "-").replace("—", "-").trim().lowercase()
                                    val normalizedQuery = normalize(query)
                                    val exactMatches = list.filter { med ->
                                        val normalizedFull = normalize(med)
                                        val parts = med.split(" – ")
                                        val normalizedName = if (parts.isNotEmpty()) normalize(parts[0]) else ""
                                        normalizedFull == normalizedQuery || normalizedName == normalizedQuery
                                    }
                                    val startsWithMatches = list.filter { med ->
                                        if (exactMatches.contains(med)) return@filter false
                                        val normalizedFull = normalize(med)
                                        val parts = med.split(" – ")
                                        val normalizedName = if (parts.isNotEmpty()) normalize(parts[0]) else ""
                                        normalizedFull.startsWith(normalizedQuery) || normalizedName.startsWith(normalizedQuery)
                                    }
                                    val containsMatches = list.filter { med ->
                                        if (exactMatches.contains(med) || startsWithMatches.contains(med)) return@filter false
                                        val normalizedFull = normalize(med)
                                        normalizedFull.contains(normalizedQuery)
                                    }
                                    (exactMatches + startsWithMatches + containsMatches).take(8)
                                }
                            }

                            LaunchedEffect(suggestions) {
                                selectedIndex = -1
                            }

                            OutlinedTextField(
                                value = editMedName,
                                onValueChange = { 
                                    editMedName = it
                                    showAutocompleteSuggestions = true
                                },
                                label = { Text("Med Name", color = MutedGray, fontSize = 11.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                     focusedTextColor = Color(0xFF737373),
                                     unfocusedTextColor = Color(0xFF737373),
                                     focusedBorderColor = if (entry.type == EntryType.CORTISONE || isAlt || entry.type == EntryType.XOLAIR_150 || entry.type == EntryType.XOLAIR_300) Color(0xFF1A7E97) else Color(0xFF509729),
                                     unfocusedBorderColor = DarkBorder,
                                     focusedContainerColor = DarkCard,
                                     unfocusedContainerColor = DarkCard
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { focusState ->
                                        showAutocompleteSuggestions = focusState.isFocused
                                    }
                                    .onKeyEvent { keyEvent ->
                                        if (showAutocompleteSuggestions && suggestions.isNotEmpty() && keyEvent.type == KeyEventType.KeyDown) {
                                            when (keyEvent.key) {
                                                Key.DirectionDown -> {
                                                    selectedIndex = (selectedIndex + 1) % suggestions.size
                                                    true
                                                }
                                                Key.DirectionUp -> {
                                                    selectedIndex = if (selectedIndex <= 0) suggestions.size - 1 else selectedIndex - 1
                                                    true
                                                }
                                                Key.Enter -> {
                                                    if (selectedIndex in suggestions.indices) {
                                                        editMedName = suggestions[selectedIndex]
                                                        showAutocompleteSuggestions = false
                                                        true
                                                    } else {
                                                        false
                                                    }
                                                }
                                                else -> false
                                            }
                                        } else {
                                            false
                                        }
                                    }
                            )
                            
                            if (showAutocompleteSuggestions && editMedName.trim().isNotEmpty()) {
                                Popup(
                                    onDismissRequest = { showAutocompleteSuggestions = false },
                                    properties = PopupProperties(focusable = false)
                                ) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp)),
                                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                                        shape = RoundedCornerShape(8.dp),
                                        elevation = CardDefaults.cardElevation(8.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 240.dp)
                                                .verticalScroll(rememberScrollState())
                                        ) {
                                            if (suggestions.isEmpty()) {
                                                Text(
                                                    text = "No medications found.\nNo matching medication found. You can enter it manually.",
                                                    color = MutedGray,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(12.dp)
                                                )
                                            } else {
                                                suggestions.forEachIndexed { index, med ->
                                                    val isSelected = index == selectedIndex
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(if (isSelected) Color(0xFF509729).copy(alpha = 0.2f) else Color.Transparent)
                                                            .clickable {
                                                                editMedName = med
                                                                showAutocompleteSuggestions = false
                                                            }
                                                            .padding(12.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        val lowercaseMed = med.lowercase()
                                                        if (lowercaseMed.contains(query)) {
                                                            val parts = mutableListOf<Pair<String, Boolean>>()
                                                            var searchIndex = 0
                                                            while (searchIndex < med.length) {
                                                                val idx = lowercaseMed.indexOf(query, searchIndex)
                                                                if (idx == -1) {
                                                                    parts.add(med.substring(searchIndex) to false)
                                                                    break
                                                                }
                                                                if (idx > searchIndex) {
                                                                    parts.add(med.substring(searchIndex, idx) to false)
                                                                }
                                                                parts.add(med.substring(idx, idx + query.length) to true)
                                                                searchIndex = idx + query.length
                                                            }
                                                            Text(
                                                                text = buildAnnotatedString {
                                                                    parts.forEach { (part, isHigh) ->
                                                                        if (isHigh) {
                                                                            withStyle(style = SpanStyle(color = Color(0xFF737373), fontWeight = FontWeight.Bold)) {
                                                                                append(part)
                                                                            }
                                                                        } else {
                                                                            withStyle(style = SpanStyle(color = Color(0xFF737373))) {
                                                                                append(part)
                                                                            }
                                                                        }
                                                                    }
                                                                },
                                                                fontSize = 12.sp
                                                            )
                                                        } else {
                                                            Text(text = med, color = Color(0xFF737373), fontSize = 12.sp)
                                                        }
                                                    }
                                                }
                                            }
                                            
                                            HorizontalDivider(color = DarkBorder)
                                            Text(
                                                text = "Can't find your medication? Continue typing to enter it manually.",
                                                color = PastelIceBlue.copy(alpha = 0.8f),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = editMedMgs,
                            onValueChange = { editMedMgs = it },
                            label = { Text("Dosage (mg)", color = MutedGray, fontSize = 11.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF737373),
                                unfocusedTextColor = Color(0xFF737373),
                                focusedBorderColor = if (entry.type == EntryType.CORTISONE || isAlt || entry.type == EntryType.XOLAIR_150 || entry.type == EntryType.XOLAIR_300) Color(0xFF1A7E97) else Color(0xFF509729),
                                unfocusedBorderColor = DarkBorder,
                                focusedContainerColor = DarkCard,
                                unfocusedContainerColor = DarkCard
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val metadata = if (entry.type == EntryType.ANTIHISTAMINE || entry.type == EntryType.CORTISONE) {
                            if (editMedName.trim().isEmpty()) {
                                Toast.makeText(context, "Please enter medication name", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (editMedMgs.trim().isEmpty() || editMedMgs.trim().toDoubleOrNull() == null) {
                                Toast.makeText(context, "Please enter a valid dosage", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val baseMeta = "${editMedName.trim()}:::${editMedMgs.trim()}"
                            val oldParts = (entry.metadata ?: "").split(":::")
                            val oldReason = if (oldParts.size >= 3) oldParts.drop(2).joinToString(":::") else ""
                            if (oldReason.isNotEmpty()) "$baseMeta:::$oldReason" else baseMeta
                        } else if (isAlt) {
                            if (editMedName.trim().isEmpty()) {
                                Toast.makeText(context, "Please enter medication name", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val mgsSuffix = if (editMedMgs.trim().isNotEmpty()) "${editMedMgs.trim()} mg" else ""
                            val baseMeta = "${editMedName.trim()} $mgsSuffix".trim()
                            val oldParts = (entry.metadata ?: "").split(":::")
                            val oldReason = if (oldParts.size >= 2) oldParts.drop(1).filter { !it.startsWith("wearing_off:") }.joinToString(":::") else ""
                            val baseMetaWithReason = if (oldReason.isNotEmpty()) "$baseMeta:::$oldReason" else baseMeta
                            updateMetadataWearingOff(baseMetaWithReason, editWearingOffTimestamp)
                        } else if (entry.type == EntryType.FLARE_UP) {
                            val severityStr = "Severity: $editSeverity"
                            val angioStr = "Angioedema: $editAngioedema"
                            val finalReasons = mutableListOf<String>()
                            selectedEditReasons.forEach { r ->
                                when (r) {
                                    "Vegetables: Other" -> {
                                        if (editVegetablesText.value.trim().isNotEmpty()) {
                                            finalReasons.add("Vegetables: Other: ${editVegetablesText.value.trim()}")
                                        } else {
                                            finalReasons.add("Vegetables: Other")
                                        }
                                    }
                                    "Fruit: Other" -> {
                                        if (editFruitsText.value.trim().isNotEmpty()) {
                                            finalReasons.add("Fruit: Other: ${editFruitsText.value.trim()}")
                                        } else {
                                            finalReasons.add("Fruit: Other")
                                        }
                                    }
                                    "Illness" -> {
                                        if (editIllnessText.value.trim().isNotEmpty()) {
                                            finalReasons.add("Illness: ${editIllnessText.value.trim()}")
                                        } else {
                                            finalReasons.add("Illness")
                                        }
                                    }
                                    "Medicine : Other" -> {
                                        val name = editMedicineOtherNameText.value.trim()
                                        val cause = editMedicineOtherCauseText.value.trim()
                                        if (name.isNotEmpty()) {
                                            val causePart = if (cause.isNotEmpty()) " (Cause: $cause)" else ""
                                            finalReasons.add("Medicine : Other: $name$causePart")
                                        } else {
                                            finalReasons.add("Medicine : Other")
                                        }
                                    }
                                    "Insect Bite/Sting" -> {
                                        val insect = editInsectText.value.trim()
                                        if (insect.isNotEmpty()) {
                                            finalReasons.add("Insect Bite/Sting: $insect")
                                        } else {
                                            finalReasons.add("Insect Bite/Sting")
                                        }
                                    }
                                    else -> finalReasons.add(r)
                                }
                            }
                            val reasonsStr = finalReasons.joinToString("; ")
                            if (reasonsStr.isNotEmpty()) "$severityStr; $angioStr; $reasonsStr" else "$severityStr; $angioStr"
                        } else if (entry.type == EntryType.XOLAIR_150 || entry.type == EntryType.XOLAIR_300) {
                            updateMetadataWearingOff(entry.metadata, editWearingOffTimestamp)
                        } else {
                            entry.metadata
                        }
                        viewModel.updateEntryDetails(entry.id, editTimestamp, metadata, force = false)
                        editingEntry = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (entry.type == EntryType.CORTISONE) Color(0xFF1A7E97) 
                                          else if (entry.type == EntryType.ANTIHISTAMINE) Color(0xFF509729)
                                          else if (entry.type == EntryType.XOLAIR_150 || entry.type == EntryType.XOLAIR_300) Color(0xFF1A7E97)
                                          else if (entry.type == EntryType.ALTERNATIVE) Color(0xFF1A7E97)
                                          else CoralPink,
                        contentColor = AmoledBlack
                    )
                ) {
                    Text(text = "Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingEntry = null }) {
                    Text(text = "Cancel", color = MutedGray)
                }
            }
        )
    }

    if (showCriticalSafetyDialog) {
        AlertDialog(
            onDismissRequest = {
                manualSeverity = previousManualSeverity
                showCriticalSafetyDialog = false
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "⚠️", fontSize = 22.sp)
                    Text(
                        text = "Seek Medical Help",
                        color = Color(0xFFD64545),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Your symptoms are marked as Critical. Please seek immediate medical attention or contact your healthcare provider.",
                            color = Color(0xFF991B1B),
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCriticalSafetyDialog = false
                        try {
                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:911"))
                            context.startActivity(dialIntent)
                        } catch (e: Exception) {
                            // Dial intent fallback
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD64545),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(text = "Seek Medical Help", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        manualSeverity = previousManualSeverity
                        showCriticalSafetyDialog = false
                    }
                ) {
                    Text(text = "Go Back", color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    if (showEditCriticalSafetyDialog) {
        AlertDialog(
            onDismissRequest = {
                editSeverity = previousEditSeverity
                showEditCriticalSafetyDialog = false
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "⚠️", fontSize = 22.sp)
                    Text(
                        text = "Seek Medical Help",
                        color = Color(0xFFD64545),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Your symptoms are marked as Critical. Please seek immediate medical attention or contact your healthcare provider.",
                            color = Color(0xFF991B1B),
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEditCriticalSafetyDialog = false
                        try {
                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:911"))
                            context.startActivity(dialIntent)
                        } catch (e: Exception) {
                            // Dial intent fallback
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD64545),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(text = "Seek Medical Help", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        editSeverity = previousEditSeverity
                        showEditCriticalSafetyDialog = false
                    }
                ) {
                    Text(text = "Go Back", color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    if (showFlareUpManualDialog) {
        Dialog(
            onDismissRequest = { showFlareUpManualDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.86f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Header
                    Text(
                        text = "Select Potential Reasons",
                        color = PrimaryPurple,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Date/Time: ${formatEntryDate(pendingManualTimestamp)}",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Severity & Angioedema Side-by-Side (50% / 50%)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Severity Field
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Severity",
                                color = Color(0xFF1E293B),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            val sevColor = getSeverityColor(manualSeverity)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(sevColor.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                    .border(1.dp, sevColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                    .clickable { manualSeverityDropdownExpanded = true }
                                    .padding(horizontal = 10.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        Text(getSeverityDot(manualSeverity), fontSize = 11.sp)
                                        Text(
                                            text = manualSeverity,
                                            color = sevColor,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text("▼", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                }
                                DropdownMenu(
                                    expanded = manualSeverityDropdownExpanded,
                                    onDismissRequest = { manualSeverityDropdownExpanded = false },
                                    modifier = Modifier
                                        .background(Color.White)
                                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                ) {
                                    listOf("Mild", "Moderate", "Severe", "Critical").forEach { level ->
                                        val optionColor = getSeverityColor(level)
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(getSeverityDot(level), fontSize = 12.sp)
                                                    Text(
                                                        text = level,
                                                        color = optionColor,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            },
                                            onClick = {
                                                manualSeverityDropdownExpanded = false
                                                if (level == "Critical") {
                                                    previousManualSeverity = manualSeverity
                                                    manualSeverity = "Critical"
                                                    showCriticalSafetyDialog = true
                                                } else {
                                                    manualSeverity = level
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Angioedema Field
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Angioedema",
                                color = Color(0xFF1E293B),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                    .clickable { manualAngioDropdownExpanded = true }
                                    .padding(horizontal = 10.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = manualAngioedema,
                                        color = if (manualAngioedema == "Yes") PrimaryPurple else Color(0xFF334155),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text("▼", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                }
                                DropdownMenu(
                                    expanded = manualAngioDropdownExpanded,
                                    onDismissRequest = { manualAngioDropdownExpanded = false },
                                    modifier = Modifier
                                        .background(Color.White)
                                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                ) {
                                    listOf("No", "Yes").forEach { option ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = option,
                                                    color = if (option == "Yes") PrimaryPurple else Color(0xFF334155),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            },
                                            onClick = {
                                                manualAngioedema = option
                                                manualAngioDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Potential Reasons (optional)",
                        color = PrimaryPurple,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Independently scrollable Reasons list container
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        ReasonsSearchableList(
                            selectedReasons = selectedManualReasons,
                            vegetablesText = manualVegetablesText,
                            fruitsText = manualFruitsText,
                            illnessText = manualIllnessText,
                            medicineOtherNameText = manualMedicineOtherNameText,
                            medicineOtherCauseText = manualMedicineOtherCauseText,
                            insectText = manualInsectText,
                            viewModel = viewModel,
                            themeColor = PrimaryPurple,
                            maxHeight = 600,
                            isLightModal = true
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Sticky Bottom Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showFlareUpManualDialog = false },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Cancel",
                                color = Color(0xFF64748B),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = {
                                val severityStr = "Severity: $manualSeverity"
                                val angioStr = "Angioedema: $manualAngioedema"
                                val finalReasons = mutableListOf<String>()
                                selectedManualReasons.forEach { r ->
                                    when (r) {
                                        "Vegetables: Other" -> {
                                            if (manualVegetablesText.value.trim().isNotEmpty()) {
                                                finalReasons.add("Vegetables: Other: ${manualVegetablesText.value.trim()}")
                                            } else {
                                                finalReasons.add("Vegetables: Other")
                                            }
                                        }
                                        "Fruit: Other" -> {
                                            if (manualFruitsText.value.trim().isNotEmpty()) {
                                                finalReasons.add("Fruit: Other: ${manualFruitsText.value.trim()}")
                                            } else {
                                                finalReasons.add("Fruit: Other")
                                            }
                                        }
                                        "Illness" -> {
                                            if (manualIllnessText.value.trim().isNotEmpty()) {
                                                finalReasons.add("Illness: ${manualIllnessText.value.trim()}")
                                            } else {
                                                finalReasons.add("Illness")
                                            }
                                        }
                                        "Medicine : Other" -> {
                                            val name = manualMedicineOtherNameText.value.trim()
                                            val cause = manualMedicineOtherCauseText.value.trim()
                                            if (name.isNotEmpty()) {
                                                val causePart = if (cause.isNotEmpty()) " (Cause: $cause)" else ""
                                                finalReasons.add("Medicine : Other: $name$causePart")
                                            } else {
                                                finalReasons.add("Medicine : Other")
                                            }
                                        }
                                        "Insect Bite/Sting" -> {
                                            val insect = manualInsectText.value.trim()
                                            if (insect.isNotEmpty()) {
                                                finalReasons.add("Insect Bite/Sting: $insect")
                                            } else {
                                                finalReasons.add("Insect Bite/Sting")
                                            }
                                        }
                                        else -> finalReasons.add(r)
                                    }
                                }
                                val reasonsStr = finalReasons.joinToString("; ")
                                val metadata = if (reasonsStr.isNotEmpty()) "$severityStr; $angioStr; $reasonsStr" else "$severityStr; $angioStr"
                                viewModel.addEntry(EntryType.FLARE_UP, pendingManualTimestamp, metadata)
                                showFlareUpManualDialog = false
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryPurple,
                                contentColor = Color.White
                            )
                        ) {
                            Text(text = "Log Entry", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showApdExplanationDialog) {
        ApdExplanationDialog(onDismiss = { showApdExplanationDialog = false })
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            containerColor = DarkSurface,
            title = {
                Text(
                    text = "Reset Log",
                    color = AlertRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "This will clear all previous logged data, which will reset every insight and analysis related to it.\n\nAre you sure you want to reset your data?",
                    color = LightGray,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (selectedTab) {
                            0 -> viewModel.clearAllEntriesOfTypes(listOf(EntryType.ANTIHISTAMINE, EntryType.CORTISONE))
                            1 -> viewModel.clearAllEntriesOfTypes(listOf(EntryType.FLARE_UP))
                            2 -> viewModel.clearAllEntriesOfTypes(listOf(EntryType.XOLAIR_150, EntryType.XOLAIR_300, EntryType.ALTERNATIVE))
                        }
                        showResetConfirmDialog = false
                        Toast.makeText(context, "Log cleared successfully.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed, contentColor = Color.White)
                ) {
                    Text(text = "Yes", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text(text = "No", color = MutedGray)
                }
            }
        )
    }
}

@Composable
fun CustomPillIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(modifier = modifier.size(width = 13.dp, height = 8.dp)) {
        val strokeWidth = 1.6.dp.toPx()
        val height = size.height
        val width = size.width
        val cornerRadius = height / 2f
        
        // Draw capsule outline
        drawRoundRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2, strokeWidth / 2),
            size = androidx.compose.ui.geometry.Size(width - strokeWidth, height - strokeWidth),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
        )
        
        // Draw center dividing line
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(width / 2f, 0f),
            end = androidx.compose.ui.geometry.Offset(width / 2f, height),
            strokeWidth = strokeWidth
        )
    }
}

@Composable
fun MedicationCard(
    name: String,
    dosage: String,
    isFavorite: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Strip generation from medication name
    val cleanName = name.replace(Regex("\\s*[–-]\\s*\\d+(?:st|nd|rd|th)\\s+generation", RegexOption.IGNORE_CASE), "").trim()
    val backgroundColor = Color(0xFFF1FAF3)
    val borderColor = if (isSelected) Color(0xFF4C9A2A) else Color(0xFFBFD9B8)
    val textColor = Color(0xFF4C9A2A) // Bold green for all names
    val displayLabel = if (dosage.isNotEmpty()) "$cleanName ($dosage mg)" else cleanName

    Card(
        onClick = onClick,
        modifier = modifier
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(1.2.dp, borderColor),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 5.dp, horizontal = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (isFavorite) {
                Text(
                    text = "★",
                    color = Color(0xFFF2C94C),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            CustomPillIcon(
                color = Color(0xFF4C9A2A),
                modifier = Modifier.graphicsLayer(rotationZ = -45f)
            )
            Text(
                text = displayLabel,
                color = textColor,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PlusInCircleIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(modifier = modifier.size(20.dp)) {
        val strokeWidth = 1.8.dp.toPx()
        val sizePx = size.width
        val radius = sizePx / 2f
        
        // Draw outer circle
        drawCircle(
            color = color,
            radius = radius - strokeWidth / 2f,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
        )
        
        // Draw horizontal line
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(sizePx * 0.28f, sizePx / 2f),
            end = androidx.compose.ui.geometry.Offset(sizePx * 0.72f, sizePx / 2f),
            strokeWidth = strokeWidth
        )
        
        // Draw vertical line
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(sizePx / 2f, sizePx * 0.28f),
            end = androidx.compose.ui.geometry.Offset(sizePx / 2f, sizePx * 0.72f),
            strokeWidth = strokeWidth
        )
    }
}

@Composable
fun DownloadArrowIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(modifier = modifier.size(20.dp)) {
        val strokeWidth = 1.8.dp.toPx()
        val sizePx = size.width
        
        // 1. Stem
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(sizePx / 2f, sizePx * 0.15f),
            end = androidx.compose.ui.geometry.Offset(sizePx / 2f, sizePx * 0.6f),
            strokeWidth = strokeWidth
        )
        
        // 2. Left arrow tip
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(sizePx / 2f, sizePx * 0.6f),
            end = androidx.compose.ui.geometry.Offset(sizePx * 0.32f, sizePx * 0.42f),
            strokeWidth = strokeWidth
        )
        
        // 3. Right arrow tip
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(sizePx / 2f, sizePx * 0.6f),
            end = androidx.compose.ui.geometry.Offset(sizePx * 0.68f, sizePx * 0.42f),
            strokeWidth = strokeWidth
        )
        
        // 4. Bottom bar
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(sizePx * 0.23f, sizePx * 0.82f),
            end = androidx.compose.ui.geometry.Offset(sizePx * 0.77f, sizePx * 0.82f),
            strokeWidth = strokeWidth
        )
    }
}

@Composable
fun TrashCanIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(modifier = modifier.size(20.dp)) {
        val strokeWidth = 1.8.dp.toPx()
        val sizePx = size.width
        
        // 1. Lid top handle bar
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(sizePx * 0.35f, sizePx * 0.1f),
            end = androidx.compose.ui.geometry.Offset(sizePx * 0.65f, sizePx * 0.1f),
            strokeWidth = strokeWidth
        )
        
        // 2. Lid tray
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(sizePx * 0.15f, sizePx * 0.22f),
            end = androidx.compose.ui.geometry.Offset(sizePx * 0.85f, sizePx * 0.22f),
            strokeWidth = strokeWidth
        )
        
        // 3. Bin body outline
        drawRoundRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(sizePx * 0.24f, sizePx * 0.28f),
            size = androidx.compose.ui.geometry.Size(sizePx * 0.52f, sizePx * 0.6f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
        )
    }
}

@Composable
fun SyringeOutlineIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(modifier = modifier.size(width = 12.dp, height = 24.dp)) {
        val strokeWidth = 1.5.dp.toPx()
        val width = size.width
        val height = size.height
        
        // 1. Plunger top bar
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(width / 2f - 3.dp.toPx(), 2.dp.toPx()),
            end = androidx.compose.ui.geometry.Offset(width / 2f + 3.dp.toPx(), 2.dp.toPx()),
            strokeWidth = strokeWidth
        )
        // 2. Plunger stem
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(width / 2f, 2.dp.toPx()),
            end = androidx.compose.ui.geometry.Offset(width / 2f, 5.dp.toPx()),
            strokeWidth = strokeWidth
        )
        // 3. Barrel outline
        drawRoundRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(width / 2f - 4.dp.toPx(), 5.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(8.dp.toPx(), 12.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx(), 1.dp.toPx()),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
        )
        // 4. Needle stem
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(width / 2f, 17.dp.toPx()),
            end = androidx.compose.ui.geometry.Offset(width / 2f, 22.dp.toPx()),
            strokeWidth = strokeWidth
        )
    }
}

@Composable
fun BiologicalTreatmentCard(
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = Color(0xFFF0FAFA)
    val borderColor = if (isActive) Color(0xFF238A9C) else Color(0xFFA8D8D8)
    val borderThickness = if (isActive) 1.8.dp else 1.2.dp
    val titleColor = Color(0xFF1F2937) // dark navy
    val subtitleColor = Color(0xFF64748B) // muted blue-grey
    val iconColor = Color(0xFF238A9C) // teal

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(borderThickness, borderColor),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SyringeOutlineIcon(
                    color = iconColor,
                    modifier = Modifier
                        .size(16.dp)
                        .padding(horizontal = 1.dp)
                )
                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = "Biological Treatment Log",
                        color = titleColor,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "View or add treatment doses",
                        color = subtitleColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Text(
                text = "›",
                color = iconColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CircularPillButton(
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private object ApdTranslations {
    val languages = listOf("EN", "AR", "FR", "ES", "IT", "DE")
    
    val languageNames = mapOf(
        "EN" to "EN",
        "AR" to "AR",
        "FR" to "FR",
        "ES" to "ES",
        "IT" to "IT",
        "DE" to "DE"
    )

    fun getTitle(lang: String, context: android.content.Context): String {
        val article = LibraryDataHolder.getArticles(context)["What is Autoimmune Progesterone Dermatitis?"]
        return article?.titles?.get(lang) ?: "What is Autoimmune Progesterone Dermatitis?"
    }

    fun getContent(lang: String, context: android.content.Context): String {
        val article = LibraryDataHolder.getArticles(context)["What is Autoimmune Progesterone Dermatitis?"]
        return article?.content?.get(lang) ?: ""
    }
}

@Composable
fun ApdExplanationDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var selectedLang by remember { mutableStateOf("EN") }
    val title = ApdTranslations.getTitle(selectedLang, context)
    val bodyText = ApdTranslations.getContent(selectedLang, context)
    val isRtl = selectedLang == "AR"

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(
                    width = 1.5.dp,
                    color = CoralPink,
                    shape = RoundedCornerShape(16.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header with Title & Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        color = CoralPink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = if (isRtl) TextAlign.Right else TextAlign.Left
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Text(
                            text = "❌",
                            color = LightGray,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Language selection row (pill tabs)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(AmoledBlack, shape = RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ApdTranslations.languages.forEach { lang ->
                        val isActive = selectedLang == lang
                        val label = ApdTranslations.languageNames[lang] ?: lang
                        Card(
                            onClick = { selectedLang = lang },
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isActive) CoralPink else Color.Transparent
                            ),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isActive) AmoledBlack else MutedGray,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable content body text
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = bodyText,
                        color = LightGray,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        textAlign = if (isRtl) TextAlign.Right else TextAlign.Left,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Close Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoralPink,
                        contentColor = AmoledBlack
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text(
                        text = if (isRtl) "موافق" else "Got it! 👍",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun getWearingOffTimestamp(entry: LogEntry): String? {
    if (entry.metadata.isNullOrEmpty()) return null
    val parts = entry.metadata.split(":::")
    for (part in parts) {
        if (part.startsWith("wearing_off:")) {
            return part.substring("wearing_off:".length)
        }
    }
    return null
}

private fun setWearingOffTimestamp(entry: LogEntry, timestamp: String): String {
    val parts = (entry.metadata ?: "").split(":::")
    val newParts = mutableListOf<String>()
    var updated = false
    for (part in parts) {
        if (part.startsWith("wearing_off:")) {
            newParts.add("wearing_off:$timestamp")
            updated = true
        } else {
            if (part.isNotEmpty()) {
                newParts.add(part)
            }
        }
    }
    if (!updated) {
        newParts.add("wearing_off:$timestamp")
    }
    return newParts.joinToString(":::")
}

private fun clearWearingOffTimestamp(entry: LogEntry): String? {
    val parts = (entry.metadata ?: "").split(":::")
    val newParts = parts.filter { !it.startsWith("wearing_off:") }
    val joined = newParts.joinToString(":::")
    return if (joined.isEmpty()) null else joined
}

private fun calculateWearingOffDuration(d1Str: String, d2Str: String): String {
    return try {
        val d1 = ZonedDateTime.parse(d1Str)
        val d2 = ZonedDateTime.parse(d2Str)
        val duration = Duration.between(d1, d2)
        val days = duration.toDays()
        if (days >= 1) {
            "$days day${if (days > 1) "s" else ""}"
        } else {
            val hours = duration.toHours()
            if (hours >= 1) {
                "$hours hr${if (hours > 1) "s" else ""}"
            } else {
                val mins = duration.toMinutes()
                "$mins min${if (mins != 1L) "s" else ""}"
            }
        }
    } catch (e: Exception) {
        ""
    }
}

private fun updateMetadataWearingOff(currentMetadata: String?, wearingOffTimestamp: String?): String? {
    val parts = (currentMetadata ?: "").split(":::")
    val filtered = parts.filter { !it.startsWith("wearing_off:") && it.isNotEmpty() }
    val newParts = filtered.toMutableList()
    if (wearingOffTimestamp != null) {
        newParts.add("wearing_off:$wearingOffTimestamp")
    }
    val joined = newParts.joinToString(":::")
    return if (joined.isEmpty()) null else joined
}

private fun cleanMedicationName(rawName: String?): String {
    if (rawName.isNullOrBlank()) return ""
    return rawName
        .replace(Regex("""\s*[-–—]\s*(?:1st|2nd|3rd|\d+(?:st|nd|rd|th)?)\s*generation\s*""", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()
}

@Composable
private fun RecentEditPencilIcon(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF64748B)
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 1.4.dp.toPx()
        val s = size.minDimension

        withTransform({
            rotate(degrees = 45f, pivot = center)
        }) {
            val cx = center.x
            val cy = center.y
            val halfW = s * 0.16f
            val topY = cy - s * 0.44f
            val eraserLineY = cy - s * 0.26f
            val bodyBottomY = cy + s * 0.20f
            val tipY = cy + s * 0.44f

            // 1. Eraser cap (rounded top)
            val eraserCorner = s * 0.12f
            val eraserPath = Path().apply {
                moveTo(cx - halfW, eraserLineY)
                lineTo(cx - halfW, topY + eraserCorner)
                quadraticTo(cx - halfW, topY, cx - halfW + eraserCorner, topY)
                lineTo(cx + halfW - eraserCorner, topY)
                quadraticTo(cx + halfW, topY, cx + halfW, topY + eraserCorner)
                lineTo(cx + halfW, eraserLineY)
            }
            drawPath(
                eraserPath,
                color = color,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // 2. Collar line
            drawLine(
                color = color,
                start = Offset(cx - halfW, eraserLineY),
                end = Offset(cx + halfW, eraserLineY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // 3. Body shaft (outer lines)
            drawLine(
                color = color,
                start = Offset(cx - halfW, eraserLineY),
                end = Offset(cx - halfW, bodyBottomY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(cx + halfW, eraserLineY),
                end = Offset(cx + halfW, bodyBottomY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Center facet line
            drawLine(
                color = color,
                start = Offset(cx, eraserLineY),
                end = Offset(cx, bodyBottomY),
                strokeWidth = strokeWidth * 0.85f,
                cap = StrokeCap.Round
            )

            // 4. Wood tip cone
            val conePath = Path().apply {
                moveTo(cx - halfW, bodyBottomY)
                lineTo(cx - halfW * 0.35f, bodyBottomY + s * 0.08f)
                lineTo(cx, tipY)
                lineTo(cx + halfW * 0.35f, bodyBottomY + s * 0.08f)
                lineTo(cx + halfW, bodyBottomY)
            }
            drawPath(
                conePath,
                color = color,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // 5. Lead tip fill
            val leadPath = Path().apply {
                val leadTopY = tipY - s * 0.12f
                val leadHalfW = halfW * 0.40f
                moveTo(cx - leadHalfW, leadTopY)
                lineTo(cx, tipY)
                lineTo(cx + leadHalfW, leadTopY)
                close()
            }
            drawPath(leadPath, color = color)
        }
    }
}

@Composable
private fun RecentDeleteTrashIcon(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFEF4444)
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 1.4.dp.toPx()
        val w = size.width
        val h = size.height

        // 1. Top handle
        val handleW = w * 0.36f
        val handleH = h * 0.16f
        val handleLeft = (w - handleW) / 2f
        val handleTop = h * 0.04f
        val handleCorner = 2.dp.toPx()

        val handlePath = Path().apply {
            moveTo(handleLeft, handleTop + handleH)
            lineTo(handleLeft, handleTop + handleCorner)
            quadraticTo(handleLeft, handleTop, handleLeft + handleCorner, handleTop)
            lineTo(handleLeft + handleW - handleCorner, handleTop)
            quadraticTo(handleLeft + handleW, handleTop, handleLeft + handleW, handleTop + handleCorner)
            lineTo(handleLeft + handleW, handleTop + handleH)
        }
        drawPath(
            handlePath,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // 2. Lid (horizontal bar)
        val lidH = h * 0.12f
        val lidTop = h * 0.20f
        val lidLeft = w * 0.08f
        val lidW = w * 0.84f
        val lidCorner = 2.5.dp.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(lidLeft, lidTop),
            size = Size(lidW, lidH),
            cornerRadius = CornerRadius(lidCorner, lidCorner),
            style = Stroke(width = strokeWidth)
        )

        // 3. Bin Body (tapered)
        val bodyTop = lidTop + lidH
        val bodyBottom = h * 0.94f
        val bodyTopLeft = w * 0.18f
        val bodyTopRight = w * 0.82f
        val bodyBottomLeft = w * 0.24f
        val bodyBottomRight = w * 0.76f
        val bodyCorner = 3.5.dp.toPx()

        val bodyPath = Path().apply {
            moveTo(bodyTopLeft, bodyTop)
            lineTo(bodyBottomLeft, bodyBottom - bodyCorner)
            quadraticTo(bodyBottomLeft, bodyBottom, bodyBottomLeft + bodyCorner, bodyBottom)
            lineTo(bodyBottomRight - bodyCorner, bodyBottom)
            quadraticTo(bodyBottomRight, bodyBottom, bodyBottomRight, bodyBottom - bodyCorner)
            lineTo(bodyTopRight, bodyTop)
        }
        drawPath(
            bodyPath,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // 4. Three vertical interior lines (ribs)
        val lineTop = bodyTop + h * 0.14f
        val lineBottom = bodyBottom - h * 0.12f
        val line1X = w * 0.36f
        val line2X = w * 0.50f
        val line3X = w * 0.64f

        drawLine(
            color = color,
            start = Offset(line1X, lineTop),
            end = Offset(line1X - w * 0.015f, lineBottom),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(line2X, lineTop),
            end = Offset(line2X, lineBottom),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(line3X, lineTop),
            end = Offset(line3X + w * 0.015f, lineBottom),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}


