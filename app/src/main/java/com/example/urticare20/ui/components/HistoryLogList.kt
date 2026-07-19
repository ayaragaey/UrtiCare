package com.example.urticare20.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import com.example.urticare20.model.MedicationDirectory
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.urticare20.model.EntryType
import com.example.urticare20.model.LogEntry
import com.example.urticare20.ui.theme.*
import com.example.urticare20.viewmodel.TrackerViewModel
import java.time.Duration
import java.time.ZonedDateTime



@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryLogList(viewModel: TrackerViewModel, entries: List<LogEntry>, onPatternAnalysisClick: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Pill, 1 = Flare Up, 2 = Xolair

    

    // State for manual entry dialog pill choice
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
    var isManualAngioedemaOnlySelected by remember { mutableStateOf(false) }
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
    var isManualMildSelected by remember { mutableStateOf(true) }
    var isManualSevereSelected by remember { mutableStateOf(false) }
    var isEditMildSelected by remember { mutableStateOf(true) }
    var isEditSevereSelected by remember { mutableStateOf(false) }
    var isManualAngioYesSelected by remember { mutableStateOf(false) }
    var isManualAngioNoSelected by remember { mutableStateOf(true) }
    var isEditAngioYesSelected by remember { mutableStateOf(false) }
    var isEditAngioNoSelected by remember { mutableStateOf(true) }
    var isEditAngioedemaOnlySelected by remember { mutableStateOf(false) }

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
    var showMilestonesExpanded by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    // Active tab-specific color schemes
    val themeColor = when (selectedTab) {
        0 -> Color(0xFF509729)
        1 -> CoralPink
        else -> PastelIceBlue
    }

    // Filter logs for the active tab
    val filteredEntries = remember(entries, selectedTab) {
        when (selectedTab) {
            0 -> entries.filter { it.type == EntryType.ANTIHISTAMINE || it.type == EntryType.CORTISONE }
            1 -> entries.filter { it.type == EntryType.FLARE_UP }
            else -> entries.filter { 
                it.type == EntryType.XOLAIR_150 || 
                it.type == EntryType.XOLAIR_300 || 
                it.type == EntryType.ALTERNATIVE 
            }
        }
    }

    val displayEntries = remember(filteredEntries) {
        filteredEntries
    }

    // Format date beautifully
    fun formatEntryDate(isoString: String): String {
        return try {
            val date = ZonedDateTime.parse(isoString).withZoneSameInstant(java.time.ZoneId.systemDefault())
            val rawMonth = date.month.name.lowercase().replaceFirstChar { it.uppercase() }
            val month = if (rawMonth.length > 3) rawMonth.substring(0, 3) else rawMonth
            val day = date.dayOfMonth
            val year = date.year
            val hrs = String.format("%02d", date.hour)
            val mins = String.format("%02d", date.minute)
            "$month $day, $year at $hrs:$mins"
        } catch (e: Exception) {
            isoString
        }
    }

    // Gap calculation relative to previous older entry
    fun getGapText(item: LogEntry): String? {
        val nextOlder = filteredEntries.getOrNull(filteredEntries.indexOf(item) + 1) ?: return null
        return try {
            val d1 = ZonedDateTime.parse(item.timestamp).withZoneSameInstant(java.time.ZoneId.systemDefault())
            val d2 = ZonedDateTime.parse(nextOlder.timestamp).withZoneSameInstant(java.time.ZoneId.systemDefault())
            val duration = Duration.between(d2, d1)
            val diffMins = Math.abs(duration.toMinutes())
            if (diffMins < 24 * 60) {
                val hrs = diffMins / 60
                val mins = diffMins % 60
                "+${hrs}h${if (mins > 0) " ${mins}m" else ""}"
            } else {
                val days = diffMins / (24 * 60)
                "+${days}d"
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
                            }
                            val detail = if (e.type == EntryType.FLARE_UP) {
                                if (e.metadata.isNullOrEmpty()) {
                                    "Recorded"
                                } else {
                                    val severity = if (e.metadata.contains("Severity: Severe")) "Severe" else "Mild"
                                    val angio = if (e.metadata.contains("Angioedema: Yes")) "Yes" else "No"
                                    val cleanReasons = e.metadata
                                        .replace("Severity: Severe", "")
                                        .replace("Severity: Mild", "")
                                        .replace("Angioedema: Yes", "")
                                        .replace("Angioedema: No", "")
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
            isEditSevereSelected = metadataStr.contains("Severity: Severe")
            isEditMildSelected = !isEditSevereSelected
            isEditAngioedemaOnlySelected = metadataStr.contains("Title: Angioedema")
            isEditAngioYesSelected = metadataStr.contains("Angioedema: Yes")
            isEditAngioNoSelected = !isEditAngioYesSelected
            if (metadataStr.isNotEmpty()) {
                val cleanReasons = metadataStr.split("; ")
                    .filter { 
                        it != "Severity: Severe" && 
                        it != "Severity: Mild" && 
                        it != "Angioedema: Yes" && 
                        it != "Angioedema: No" && 
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
        // Branding Container Title with Pattern Analysis Icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(32.dp)
                ) {
                    Text(
                        text = "←",
                        color = LightGray,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Insights",
                    color = LightGray,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
            Button(
                onClick = onPatternAnalysisClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkCard,
                    contentColor = Color(0xFF737373)
                ),
                border = BorderStroke(1.dp, DarkBorder),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.wrapContentWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "📊",
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Pattern Analysis",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        val adherenceStreak by viewModel.adherenceStreakHours.collectAsState()
        val remissionStreak by viewModel.remissionStreakDays.collectAsState()

        val lastBrokenHours = com.example.urticare20.util.MilestoneEvaluator.getLastBrokenMilestoneHours(entries)

        val lastBrokenText = if (lastBrokenHours != null) {
            if (lastBrokenHours <= 48L) {
                "$lastBrokenHours hours"
            } else {
                val days = lastBrokenHours / 24
                val rem = lastBrokenHours % 24
                if (rem > 0L) "$days days $rem hours" else "$days days"
            }
        } else {
            null
        }

        val stabilityMilestones = (adherenceStreak ?: 0L) / 48
        val remissionMilestones = (remissionStreak ?: 0L) / 7

        val stabilityMilestoneText = if (stabilityMilestones > 0) {
            if (stabilityMilestones == 1L) {
                "💎 Tier 1 (48h Stable)"
            } else {
                "💎 Tier $stabilityMilestones (${stabilityMilestones * 2}d Stable)"
            }
        } else {
            "💎 Underway (Next at 48h)"
        }

        val remissionMilestoneText = if (remissionMilestones > 0) {
            "💎 Tier $remissionMilestones ($remissionMilestones Week${if (remissionMilestones > 1) "s" else ""} Free)"
        } else {
            "💎 Underway (Next at 7d)"
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .border(
                    width = 1.dp,
                    color = DarkBorder,
                    shape = RoundedCornerShape(12.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header row click handler to expand/collapse
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showMilestonesExpanded = !showMilestonesExpanded }
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🏆",
                        fontSize = 14.sp
                    )
                    
                    Text(
                        text = "Your Milestones",
                        color = LightGray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    Text(
                        text = if (showMilestonesExpanded) "▲" else "▼",
                        color = LightGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (showMilestonesExpanded) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val isBroken = adherenceStreak != null && adherenceStreak!! < 48L && lastBrokenHours != null && lastBrokenHours >= 48L
                    if (isBroken && lastBrokenText != null) {
                        Text(
                            text = "Last Milestone broken after $lastBrokenText — Don't sweat it, the progress still counts",
                            color = Color(0xFF737373),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Adherence (Stability) Streak item
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (adherenceStreak != null) {
                                    if (adherenceStreak!! <= 48L) {
                                        "$adherenceStreak Hours Stable"
                                    } else {
                                        val days = adherenceStreak!! / 24
                                        val rem = adherenceStreak!! % 24
                                        if (rem > 0L) "$days Days $rem Hours Stable" else "$days Days Stable"
                                    }
                                } else {
                                    "-- Hours Stable"
                                },
                                color = PastelIceBlue,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Without urticaria medication!",
                                color = MutedGray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Normal
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stabilityMilestoneText,
                                color = LightGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Vertical Divider line
                        Spacer(
                            modifier = Modifier
                                .width(1.dp)
                                .height(48.dp)
                                .background(DarkBorder)
                        )

                        // Remission Streak item
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (remissionStreak != null) "$remissionStreak Days Remission" else "-- Days Remission",
                                color = CoralPink,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Flare ups free!",
                                color = MutedGray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Normal
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = remissionMilestoneText,
                                color = LightGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    var showMilestoneHistoryDialog by remember { mutableStateOf(false) }

                    TextButton(
                        onClick = { showMilestoneHistoryDialog = true },
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = "📅 Milestones History",
                            color = Color(0xFF737373),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (showMilestoneHistoryDialog) {
                        MilestoneHistoryDialog(
                            entries = entries,
                            onDismiss = { showMilestoneHistoryDialog = false }
                        )
                    }
                }
            }
        }

        // Custom M3 Insights Tab Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface, shape = RoundedCornerShape(8.dp))
                .padding(3.dp)
                .background(AmoledBlack, shape = RoundedCornerShape(8.dp))
                .padding(1.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val tabs = listOf("Pills Log", "Flare Up Log", "Bio. Med.")
            tabs.forEachIndexed { index, label ->
                val isActive = selectedTab == index
                val tabBg = when {
                    isActive && index == 0 -> Color(0xFF509729).copy(alpha = 0.15f)
                    isActive && index == 1 -> CoralPink.copy(alpha = 0.15f)
                    isActive && index == 2 -> PastelIceBlue.copy(alpha = 0.15f)
                    else -> Color.Transparent
                }
                
                val tabBorder = when {
                    isActive && index == 0 -> BorderStroke(1.dp, Color(0xFF509729).copy(alpha = 0.3f))
                    isActive && index == 1 -> BorderStroke(1.dp, CoralPink.copy(alpha = 0.3f))
                    isActive && index == 2 -> BorderStroke(1.dp, PastelIceBlue.copy(alpha = 0.3f))
                    else -> null
                }

                Card(
                    onClick = { 
                        selectedTab = index 
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    colors = CardDefaults.cardColors(containerColor = tabBg),
                    border = tabBorder,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isActive) LightGray else MutedGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        if (selectedTab == 0 && antihistamines.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            val chunkedMeds = antihistamines.chunked(2)
            chunkedMeds.forEachIndexed { rowIndex, rowMeds ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowMeds.forEach { med ->
                        CircularPillButton(
                            label = "${med.name} (${med.mgs} mg)",
                            color = themeColor,
                            onClick = {
                                val metadata = "${med.name}:::${med.mgs}"
                                viewModel.addEntry(EntryType.ANTIHISTAMINE, ZonedDateTime.now().toString(), metadata = metadata)
                                Toast.makeText(context, "${med.name} (${med.mgs} mg) logged", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowMeds.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                if (rowIndex < chunkedMeds.lastIndex) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Actions panel (Manual Entry, Export, Reset Log) colored based on active tab
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Manual Entry Button
            Button(
                onClick = {
                    performManualEntryFlow { timestamp ->
                        when (selectedTab) {
                            1 -> {
                                pendingManualTimestamp = timestamp
                                selectedManualReasons.clear()
                                manualIllnessText.value = ""
                                manualVegetablesText.value = ""
                                manualFruitsText.value = ""
                                manualMedicineOtherNameText.value = ""
                                manualMedicineOtherCauseText.value = ""
                                manualInsectText.value = ""
                                isManualMildSelected = true
                                isManualSevereSelected = false
                                isManualAngioYesSelected = false
                                isManualAngioNoSelected = true
                                isManualAngioedemaOnlySelected = false
                                showFlareUpManualDialog = true
                            }
                            0 -> {
                                pendingManualTimestamp = timestamp
                                showPillChoiceDialog = true
                            }
                            2 -> {
                                pendingManualTimestamp = timestamp
                                showXolairChoiceDialog = true
                            }
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = themeColor, contentColor = AmoledBlack),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Text(text = "Manual Entry", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            // 2. Export Button
            Button(
                onClick = { performExportFlow() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = themeColor, contentColor = AmoledBlack),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Text(text = "Export", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            // 3. Reset Log Button
            Button(
                onClick = {
                    showResetConfirmDialog = true
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = AlertRed, contentColor = Color.White),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Text(text = "Reset Log", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Sub-panel for Biological Medication Log tab
        if (selectedTab == 2) {
            Spacer(modifier = Modifier.height(14.dp))
            
            if (isCustomBiologicalActive) {
                // Bio. Med. quick actions row - Custom biological treatment only
                val medLabel = "$profileBiologicalMedicationState $profileBiologicalMgState mg"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularPillButton(
                        label = medLabel,
                        color = PastelIceBlue,
                        onClick = {
                            val medNameLower = profileBiologicalMedicationState.trim().lowercase()
                            val mgClean = profileBiologicalMgState.trim()
                            if (medNameLower == "xolair" && mgClean == "150") {
                                viewModel.addEntry(EntryType.XOLAIR_150, ZonedDateTime.now().toString(), "150 mg")
                                Toast.makeText(context, "Xolair 150 mg logged", Toast.LENGTH_SHORT).show()
                            } else if (medNameLower == "xolair" && mgClean == "300") {
                                viewModel.addEntry(EntryType.XOLAIR_300, ZonedDateTime.now().toString(), "300 mg")
                                Toast.makeText(context, "Xolair 300 mg logged", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.addEntry(EntryType.ALTERNATIVE, ZonedDateTime.now().toString(), medLabel)
                                Toast.makeText(context, "$medLabel logged", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                // Xolair quick actions row - Defaults
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularPillButton(
                        label = "Xolair 150 mg",
                        color = PastelIceBlue,
                        onClick = {
                            viewModel.addEntry(EntryType.XOLAIR_150, ZonedDateTime.now().toString(), "150 mg")
                            Toast.makeText(context, "Xolair 150 mg logged", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    )

                    CircularPillButton(
                        label = "Xolair 300 mg",
                        color = PastelIceBlue,
                        onClick = {
                            viewModel.addEntry(EntryType.XOLAIR_300, ZonedDateTime.now().toString(), "300 mg")
                            Toast.makeText(context, "Xolair 300 mg logged", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Saved Alternatives Section (Tactile fast-picking actions panel)
                if (savedAlternatives.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "SAVED ALTERNATIVES (FAST-PICKING)", color = DarkGray, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        savedAlternatives.forEach { alt ->
                            val displayLabel = "${alt.first} ${if (alt.second.isNotEmpty()) "${alt.second} mg" else ""}".trim()
                            Card(
                                onClick = {
                                    viewModel.addEntry(EntryType.ALTERNATIVE, ZonedDateTime.now().toString(), displayLabel)
                                    Toast.makeText(context, "$displayLabel logged", Toast.LENGTH_SHORT).show()
                                },
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                border = BorderStroke(1.dp, PastelIceBlue.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = displayLabel, color = PastelIceBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
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
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkBorder),
                shape = RoundedCornerShape(12.dp)
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
                                    if (parts.size >= 2) "${parts[0]} (${parts[1]} mg)" else item.metadata
                                } else {
                                    "Antihistamine"
                                }
                            }
                            EntryType.CORTISONE -> {
                                if (!item.metadata.isNullOrEmpty()) {
                                    val parts = item.metadata.split(":::")
                                    if (parts.size >= 2) "${parts[0]} (${parts[1]} mg)" else item.metadata
                                } else {
                                    "Cortisone"
                                }
                            }
                            EntryType.XOLAIR_150 -> "Xolair 150 mg"
                            EntryType.XOLAIR_300 -> "Xolair 300 mg"
                            EntryType.ALTERNATIVE -> item.metadata?.split(":::")?.firstOrNull() ?: "Alternative Med"
                        }

                        val gapText = getGapText(item)
                        val isAlertRed = isPillIntervalAlert(item)

                        // Styling color: Red if intervals <8h (Pill tab only), else tab theme color
                        val itemColor = when {
                            isAlertRed -> AlertRed
                            selectedTab == 2 -> Color(0xFF1A7E97)
                            item.type == EntryType.ANTIHISTAMINE -> Color(0xFF509729)
                            item.type == EntryType.CORTISONE -> Color(0xFF509729)
                            item.type == EntryType.XOLAIR_150 || item.type == EntryType.XOLAIR_300 -> Color(0xFF1A7E97)
                            item.type == EntryType.ALTERNATIVE -> Color(0xFF1A7E97)
                            selectedTab == 0 -> Color(0xFF509729)
                            selectedTab == 1 -> CoralPink
                            else -> PastelIceBlue
                        }

                        val isCollapsed = item.type == EntryType.FLARE_UP && collapsedFlareUps.contains(item.id)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (item.type == EntryType.FLARE_UP) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .background(itemColor.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp))
                                                .border(1.dp, itemColor.copy(alpha = 0.5f), shape = RoundedCornerShape(4.dp))
                                                .clickable {
                                                    if (isCollapsed) {
                                                        collapsedFlareUps.remove(item.id)
                                                    } else {
                                                        collapsedFlareUps.add(item.id)
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (isCollapsed) "+" else "-",
                                                color = itemColor,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(itemColor, shape = RoundedCornerShape(3.dp))
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = friendlyLabel,
                                                color = itemColor,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (gapText != null) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "($gapText)",
                                                    color = itemColor,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                        if (item.type == EntryType.CORTISONE) {
                                            Text(
                                                text = "(Corticosteroids)",
                                                color = itemColor.copy(alpha = 0.7f),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Normal
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(3.dp))

                                Text(
                                    text = formatEntryDate(item.timestamp),
                                    color = itemColor.copy(alpha = 0.75f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                if (item.type == EntryType.FLARE_UP) {
                                    val severity = if (item.metadata?.contains("Severity: Severe") == true) "Severe" else "Mild"
                                    val angio = if (item.metadata?.contains("Angioedema: Yes") == true) "Yes" else "No"
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Severity: $severity | Angioedema: $angio",
                                        color = CoralPink,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (selectedTab == 2) {
                                    val wearingOffTime = getWearingOffTimestamp(item)
                                    if (wearingOffTime != null) {
                                        val duration = calculateWearingOffDuration(item.timestamp, wearingOffTime)
                                        if (duration.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "🛑 Wore off: ",
                                                    color = Color(0xFF1A7E97),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Normal
                                                )
                                                Text(
                                                    text = duration,
                                                    color = Color(0xFF1A7E97),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "Clear",
                                                    color = AlertRed,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier
                                                        .clickable {
                                                            val newMetadata = clearWearingOffTimestamp(item)
                                                            viewModel.updateEntryDetails(item.id, item.timestamp, newMetadata)
                                                            Toast.makeText(context, "Cleared wearing off time", Toast.LENGTH_SHORT).show()
                                                        }
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(DarkCard, shape = RoundedCornerShape(6.dp))
                                                .border(1.dp, Color(0xFF1A7E97).copy(alpha = 0.5f), shape = RoundedCornerShape(6.dp))
                                                .clickable {
                                                    val wearingOffTime = ZonedDateTime.now().toString()
                                                    val newMetadata = setWearingOffTimestamp(item, wearingOffTime)
                                                    viewModel.updateEntryDetails(item.id, item.timestamp, newMetadata)
                                                    Toast.makeText(context, "Recorded wearing off time", Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "Treatment Wearing Off",
                                                color = Color(0xFF1A7E97),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                if (isAlertRed) {
                                    val lastTime = getLastAlertTime(item)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (lastTime != null) "Last time this happened: $lastTime" else "First time this happened in logs",
                                        color = AlertRed.copy(alpha = 0.85f),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }

                                val streakBreakReason = if (item.type == EntryType.ANTIHISTAMINE || item.type == EntryType.CORTISONE || item.type == EntryType.ALTERNATIVE) {
                                    val parts = (item.metadata ?: "").split(":::")
                                        .filter { !it.startsWith("wearing_off:") && !it.startsWith("Ongoing Medication:") }
                                    val idx = if (item.type == EntryType.ALTERNATIVE) 1 else 2
                                    if (parts.size > idx) {
                                        val r = parts.drop(idx).joinToString(":::")
                                        if (r.startsWith("Streak Break Reason:")) r else "Streak Break Reason: $r"
                                    } else {
                                        null
                                    }
                                } else {
                                    null
                                }

                                if (streakBreakReason != null) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val breakReasonColor = when (selectedTab) {
                                        0 -> Color(0xFF737373)
                                        2 -> Color(0xFF1A7E97)
                                        else -> CoralPink.copy(alpha = 0.85f)
                                    }
                                    Text(
                                        text = streakBreakReason,
                                        color = breakReasonColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }

                                val ongoingMedForPill = if (item.type == EntryType.ANTIHISTAMINE) {
                                    val parts = (item.metadata ?: "").split(":::")
                                    parts.firstOrNull { it.startsWith("Ongoing Medication:") }
                                } else {
                                    null
                                }

                                if (!ongoingMedForPill.isNullOrEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = ongoingMedForPill,
                                        color = LightGray.copy(alpha = 0.8f),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }

                                if (item.type == EntryType.FLARE_UP && !item.metadata.isNullOrEmpty() && !isCollapsed) {
                                    val cleanMetadata = item.metadata
                                        .replace("Severity: Severe", "")
                                        .replace("Severity: Mild", "")
                                        .trim()
                                        .removePrefix(";")
                                        .removeSuffix(";")
                                        .trim()

                                    val apdStr = "Potentially caused by Autoimmune Progesterone Dermatitis (APD)"
                                    val pregStr = "Possibly a pregnancy-associated flare-up"
                                    val hasApd = cleanMetadata.contains(apdStr)
                                    val hasPreg = cleanMetadata.contains(pregStr)
                                    val allParts = cleanMetadata.split("; ").map { it.trim() }
                                    val ongoingMed = allParts.firstOrNull { it.startsWith("Ongoing Medication:") }
                                    val otherReasons = allParts
                                        .filter { it.isNotEmpty() && it != apdStr && it != pregStr && !it.startsWith("Ongoing Medication:") }
                                        .joinToString(", ")

                                    if (otherReasons.isNotEmpty() && !hasApd) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Potential Reasons: $otherReasons",
                                            color = LightGray.copy(alpha = 0.8f),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Normal
                                        )
                                    }

                                    if (!ongoingMed.isNullOrEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = ongoingMed,
                                            color = LightGray.copy(alpha = 0.8f),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Normal
                                        )
                                    }

                                    if (hasPreg) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = pregStr,
                                            color = CoralPink,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    if (hasApd) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = apdStr,
                                                color = CoralPink,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .background(Color(0xFF2C2C2C), shape = RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        showApdExplanationDialog = true
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "?",
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Edit and Clear Action Buttons
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(
                                    onClick = { showEditDialog(item) },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(text = "Edit", color = LinkBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                                
                                TextButton(
                                    onClick = { viewModel.deleteEntry(item.id) },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(text = "Clear", color = AlertRed, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        if (idx < displayEntries.lastIndex) {
                            HorizontalDivider(
                                color = DarkBorder,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    }
                }
            }
        }
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
            title = { Text(text = "Select Injection / Dosage", color = Color(0xFF509729), fontWeight = FontWeight.Bold) },
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
                            colors = ButtonDefaults.buttonColors(containerColor = DarkCard, contentColor = Color(0xFF509729)),
                            border = BorderStroke(1.dp, Color(0xFF509729).copy(alpha = 0.5f)),
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
                            colors = ButtonDefaults.buttonColors(containerColor = DarkCard, contentColor = Color(0xFF509729)),
                            border = BorderStroke(1.dp, Color(0xFF509729).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Xolair 150 mg", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                viewModel.addEntry(EntryType.XOLAIR_300, pendingManualTimestamp, "300 mg")
                                showXolairChoiceDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkCard, contentColor = Color(0xFF509729)),
                            border = BorderStroke(1.dp, Color(0xFF509729).copy(alpha = 0.5f)),
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
                    color = Color(0xFF509729),
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
                                        color = Color(0xFF509729),
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
                    color = if (entry.type == EntryType.CORTISONE) Color(0xFF509729)
                            else if (entry.type == EntryType.ANTIHISTAMINE) Color(0xFF509729)
                            else if (entry.type == EntryType.XOLAIR_150 || entry.type == EntryType.XOLAIR_300) Color(0xFF509729)
                            else if (entry.type == EntryType.ALTERNATIVE) Color(0xFF509729)
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Severity",
                                color = CoralPink,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(90.dp)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    isEditMildSelected = true
                                    isEditSevereSelected = false
                                }
                            ) {
                                RadioButton(
                                    selected = isEditMildSelected,
                                    onClick = {
                                        isEditMildSelected = true
                                        isEditSevereSelected = false
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = CoralPink,
                                        unselectedColor = MutedGray
                                    )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Mild", color = Color(0xFF737373), fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    isEditMildSelected = false
                                    isEditSevereSelected = true
                                }
                            ) {
                                RadioButton(
                                    selected = isEditSevereSelected,
                                    onClick = {
                                        isEditMildSelected = false
                                        isEditSevereSelected = true
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = CoralPink,
                                        unselectedColor = MutedGray
                                    )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Severe", color = Color(0xFF737373), fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Angioedema Only Checkbox Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    isEditAngioedemaOnlySelected = !isEditAngioedemaOnlySelected 
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isEditAngioedemaOnlySelected,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = CoralPink,
                                    uncheckedColor = MutedGray,
                                    checkmarkColor = AmoledBlack
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Angioedema Only",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (!isEditAngioedemaOnlySelected) {
                            // Angioedema Radio Button Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Angioedema",
                                    color = CoralPink,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(90.dp)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        isEditAngioYesSelected = true
                                        isEditAngioNoSelected = false
                                    }
                                ) {
                                    RadioButton(
                                        selected = isEditAngioYesSelected,
                                        onClick = {
                                            isEditAngioYesSelected = true
                                            isEditAngioNoSelected = false
                                        },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = CoralPink,
                                            unselectedColor = MutedGray
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Yes", color = Color(0xFF737373), fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        isEditAngioYesSelected = false
                                        isEditAngioNoSelected = true
                                    }
                                ) {
                                    RadioButton(
                                        selected = isEditAngioNoSelected,
                                        onClick = {
                                            isEditAngioYesSelected = false
                                            isEditAngioNoSelected = true
                                        },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = CoralPink,
                                            unselectedColor = MutedGray
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "No", color = Color(0xFF737373), fontSize = 12.sp)
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
                            maxHeight = 250
                        )
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
                                     focusedBorderColor = Color(0xFF509729),
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
                                focusedBorderColor = Color(0xFF509729),
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
                            val severityStr = if (isEditSevereSelected) "Severity: Severe" else "Severity: Mild"
                            val angioStr = if (isEditAngioedemaOnlySelected || isEditAngioYesSelected) "Angioedema: Yes" else "Angioedema: No"
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
                            if (isEditAngioedemaOnlySelected) {
                                if (reasonsStr.isNotEmpty()) "$severityStr; Angioedema: Yes; Title: Angioedema; $reasonsStr" else "$severityStr; Angioedema: Yes; Title: Angioedema"
                            } else {
                                if (reasonsStr.isNotEmpty()) "$severityStr; $angioStr; $reasonsStr" else "$severityStr; $angioStr"
                            }
                        } else if (entry.type == EntryType.XOLAIR_150 || entry.type == EntryType.XOLAIR_300) {
                            updateMetadataWearingOff(entry.metadata, editWearingOffTimestamp)
                        } else {
                            entry.metadata
                        }
                        viewModel.updateEntryDetails(entry.id, editTimestamp, metadata, force = false)
                        editingEntry = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (entry.type == EntryType.CORTISONE) Color(0xFF509729) 
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

    if (showFlareUpManualDialog) {
        AlertDialog(
            onDismissRequest = { showFlareUpManualDialog = false },
            containerColor = DarkSurface,
            title = {
                Text(
                    text = if (isManualAngioedemaOnlySelected) "Log Angioedema" else "Select Potential Reasons",
                    color = CoralPink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Date/Time: ${formatEntryDate(pendingManualTimestamp)}",
                        color = LightGray,
                        fontSize = 11.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Severity",
                            color = CoralPink,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(90.dp)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                isManualMildSelected = true
                                isManualSevereSelected = false
                            }
                        ) {
                            RadioButton(
                                selected = isManualMildSelected,
                                onClick = {
                                    isManualMildSelected = true
                                    isManualSevereSelected = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = CoralPink,
                                    unselectedColor = MutedGray
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Mild", color = Color(0xFF737373), fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                isManualMildSelected = false
                                isManualSevereSelected = true
                            }
                        ) {
                            RadioButton(
                                selected = isManualSevereSelected,
                                onClick = {
                                    isManualMildSelected = false
                                    isManualSevereSelected = true
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = CoralPink,
                                    unselectedColor = MutedGray
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Severe", color = Color(0xFF737373), fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Angioedema Only Checkbox Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                isManualAngioedemaOnlySelected = !isManualAngioedemaOnlySelected 
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isManualAngioedemaOnlySelected,
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(
                                checkedColor = CoralPink,
                                uncheckedColor = MutedGray,
                                checkmarkColor = AmoledBlack
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Angioedema Only",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (!isManualAngioedemaOnlySelected) {
                        // Angioedema Radio Button Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Angioedema",
                                color = CoralPink,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(90.dp)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    isManualAngioYesSelected = true
                                    isManualAngioNoSelected = false
                                }
                            ) {
                                RadioButton(
                                    selected = isManualAngioYesSelected,
                                    onClick = {
                                        isManualAngioYesSelected = true
                                        isManualAngioNoSelected = false
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = CoralPink,
                                        unselectedColor = MutedGray
                                    )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Yes", color = Color(0xFF737373), fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    isManualAngioYesSelected = false
                                    isManualAngioNoSelected = true
                                }
                            ) {
                                RadioButton(
                                    selected = isManualAngioNoSelected,
                                    onClick = {
                                        isManualAngioYesSelected = false
                                        isManualAngioNoSelected = true
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = CoralPink,
                                        unselectedColor = MutedGray
                                    )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "No", color = Color(0xFF737373), fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Potential Reasons (optional)",
                        color = CoralPink,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    ReasonsSearchableList(
                        selectedReasons = selectedManualReasons,
                        vegetablesText = manualVegetablesText,
                        fruitsText = manualFruitsText,
                        illnessText = manualIllnessText,
                        medicineOtherNameText = manualMedicineOtherNameText,
                        medicineOtherCauseText = manualMedicineOtherCauseText,
                        insectText = manualInsectText,
                        viewModel = viewModel,
                        themeColor = CoralPink,
                        maxHeight = 400
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val severityStr = if (isManualSevereSelected) "Severity: Severe" else "Severity: Mild"
                        val angioStr = if (isManualAngioedemaOnlySelected || isManualAngioYesSelected) "Angioedema: Yes" else "Angioedema: No"
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
                        val metadata = if (isManualAngioedemaOnlySelected) {
                            if (reasonsStr.isNotEmpty()) "$severityStr; Angioedema: Yes; Title: Angioedema; $reasonsStr" else "$severityStr; Angioedema: Yes; Title: Angioedema"
                        } else {
                            if (reasonsStr.isNotEmpty()) "$severityStr; $angioStr; $reasonsStr" else "$severityStr; $angioStr"
                        }
                        viewModel.addEntry(EntryType.FLARE_UP, pendingManualTimestamp, metadata)
                        showFlareUpManualDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralPink, contentColor = AmoledBlack)
                ) {
                    Text(text = "Log Entry", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFlareUpManualDialog = false }) {
                    Text(text = "Cancel", color = MutedGray)
                }
            }
        )
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
    
    val titles = mapOf(
        "EN" to "What is Autoimmune Progesterone Dermatitis?",
        "AR" to "ما هو التهاب الجلد البروجستروني المناعي الذاتي؟",
        "FR" to "Qu'est-ce que la dermatite auto-immune à la progestérone ?",
        "ES" to "¿Qué es la dermatitis autoinmune por progesterona?",
        "IT" to "Cos'è la dermatite autoimmune da progesterone?",
        "DE" to "Was ist eine Autoimmun-Progesteron-Dermatitis?"
    )

    val content = mapOf(
        "EN" to """When an urticaria flare-up occurs in direct relation to the menstrual cycle, it is most commonly referred to clinical and scientific contexts as Autoimmune Progesterone Dermatitis (APD).

More broadly, when hives cyclically fluctuate or worsen during specific phases of a woman's period, it is also categorized under Catamenial Urticaria (where "catamenial" translates directly to monthly or menstrual).

Here is how these hormonal triggers are identified and tracked in relation to the condition:

• Autoimmune Progesterone Dermatitis (APD): An immune-mediated reaction triggered specifically by progesterone level spikes during the luteal phase (the second half) of the menstrual cycle.

• Catamenial Urticaria: A cyclical flare-up of hives that coordinates closely with menstrual cycle phases and fluctuations.

• Estrogen Baseline Shifts: Sudden drops or changes in estrogen levels prior to menstruation that can disrupt mast cell stability and alter your body's baseline vulnerability to an outbreak.""",

        "AR" to """عندما يحدث تفشي للشرى (الأرتيكاريا) ارتباطاً مباشراً بالدورة الشهرية، فإنه يُشار إليه غالباً في السياقات السريرية والعلمية باسم التهاب الجلد البروجستروني المناعي الذاتي (APD). وعلى نطاق أوسع، عندما تتقلب الشرى بشكل دوري أو تزداد سوءاً خلال مراحل معينة من فترة الطمث لدى المرأة، فإنها تُصنف أيضاً تحت مسمى الشرى الطمثي (Catamenial Urticaria) (حيث تترجم كلمة "طمثي" مباشرة إلى شهري أو حيضي).

وفيما يلي كيفية تحديد هذه المحفزات الهرمونية وتتبعها وعلاقتها بهذه الحالة:

• التهاب الجلد البروجستروني المناعي الذاتي (APD): هو تفاعل بوساطة مناعية يتم تحفيزه وتنشيطه تحديداً بسبب الارتفاعات الحادة في مستويات هرمون البروجسترون خلال الطور الأصفر (النصف الثاني) من الدورة الشهرية.

• الشرى الطمثي (Catamenial Urticaria): تفشي دوري للشرى يتناسق ويتوافق بشكل وثيق مع مراحل الدورة الشهرية وتقلباتها.

• تحولات خط الأساس لهرمون الإستروجين: انخفاضات أو تغيرات مفاجئة في مستويات هرمون الإستروجين قبل الحيض، والتي يمكن أن تخل باستقرار الخلايا البدينة (الصارية) وتغير من قدرة جسمكِ ومستوى ضعفه الأساسي أمام حدوث تفشي للمرض.""",

        "FR" to """Lorsqu'une poussée d'urticaire survient en lien direct avec le cycle menstruel, elle est le plus souvent appelée, dans les contextes cliniques et scientifiques, dermatite auto-immune à la progestérone (APD). De manière plus large, lorsque l'urticaire fluctue de façon cyclique ou s'aggrave au cours de phases spécifiques des règles d'une femme, elle est également classée sous le terme d'urticaire cataméniale (le mot « cataménial » se traduisant directement par mensuel ou lié aux menstruations).

Voici comment ces déclencheurs hormonaux sont identifiés et suivis en relation avec cette affection :

• Dermatite auto-immune à la progestérone (APD) : Une réaction à médiation immunitaire déclenchée spécifiquement par des pics du taux de progestérone pendant la phase lutéale (la seconde moitié) du cycle menstruel.

• Urticaire cataméniale : Une poussée cyclique d'urticaire qui se coordonne étroitement avec les phases et les fluctuations du cycle menstruel.

• Variations du taux de base d'œstrogènes : Des chutes ou des modifications soudaines des taux d'œstrogènes avant les menstruations, qui peuvent perturber la stabilité des mastocytes et modifier la vulnérabilité de base de votre organisme face à une poussée.""",

        "ES" to """Cuando un brote de urticaria ocurre en relación directa con el ciclo menstrual, se le conoce más comúnmente en los contextos clínicos y científicos como dermatitis autoinmune por progesterona (APD). De manera más amplia, cuando los habones (ronchas) fluctúan cíclicamente o empeoran durante fases específicas del periodo de una mujer, también se clasifica bajo el término de urticaria catamenial (donde "catamenial" se traduce directamente como mensual o menstrual).

Así es como se identifican y se realiza el seguimiento de estos desencadenantes hormonales en relación con la afección:

• Dermatitis autoinmune por progesterona (APD): Una reacción de mediación inmunitaria desencadenada específicamente por picos en los niveles de progesterona durante la fase lútea (la segunda mitad) del ciclo menstrual.

• Urticaria catamenial: Un brote cíclico de urticaria que se coordina estrechamente con las fases y fluctuaciones del ciclo menstrual.

• Alteraciones en los niveles base de estrógeno: Caídas o cambios repentinos en los niveles de estrógeno antes de la menstruación que pueden desestabilizar los mastocitos y alterar la vulnerabilidad basal de tu cuerpo ante un brote.""",

        "IT" to """Quando una riacutizzazione dell'orticaria si verifica in relazione diretta con il ciclo mestruale, nel contesto clinico e scientifico viene definita più comunemente come dermatite autoimmune da progesterone (APD). Più in generale, quando i pomfi fluttuano ciclicamente o peggiorano durante fasi specifiche del ciclo di una donna, la condizione viene classificata anche come orticaria catameniale (dove il termine "catameniale" si traduce direttamente con mensile o mestruale).

Ecco come questi fattori scatenanti di natura ormonale vengono identificati e monitorati in relazione a questa condizione:

• Dermatite autoimmune da progesterone (APD): Una reazione immuno-mediata scatenata specificamente da picchi nei livelli di progesterone durante la fase luteale (la seconda metà) del ciclo mestruale.

• Orticaria catameneiale: Una riacutizzazione ciclica dell'orticaria che si coordina strettamente con le fasi e le fluttuazioni del ciclo mestruale.

• Variazioni dei livelli basali di estrogeni: Cali improvvisi o alterazioni dei livelli di estrogeni prima delle mestruazioni, che possono compromettere la stabilità dei mastociti e alterare la vulnerabilità basale dell'organismo nei confronti di un'eruzione cutanea.""",

        "DE" to """Wenn ein Urtikaria-Schub in direktem Zusammenhang mit dem Menstruationszyklus auftritt, wird dies im klinischen und wissenschaftlichen Kontext am häufigsten als Autoimmun-Progesteron-Dermatitis (APD) bezeichnet. Allgemeiner ausgedrückt: Wenn Quaddeln während bestimmter Phasen der Periode einer Frau zyklisch schwanken oder sich verschlimmern, wird dies auch unter dem Begriff katameniale Urtikaria eingeordnet (wobei sich „katamenial“ direkt mit monatlich oder menstruell übersetzen lässt).

Hier ist aufgeführt, wie diese hormonellen Auslöser im Zusammenhang mit der Erkrankung identifiziert und nachverfolgt werden:

• Autoimmun-Progesteron-Dermatitis (APD): Eine immunvermittelte Reaktion, die gezielt durch Progesteronspitzen während der Lutealphase (der zweiten Hälfte) des Menstruationszyklus ausgelöst wird.

• Katameniale Urtikaria: Ein zyklischer Urtikaria-Schub, der eng mit den Phasen und Schwankungen des Menstruationszyklus einhergeht.

• Veränderungen des Östrogen-Basalwerts: Plötzliche Abfälle oder Schwankungen des Östrogenspiegels vor der Menstruation, welche die Stabilität der Mastzellen stören und die grundlegende Anfälligkeit Ihres Körpers für einen Ausbruch verändern können."""
    )
}

@Composable
fun ApdExplanationDialog(onDismiss: () -> Unit) {
    var selectedLang by remember { mutableStateOf("EN") }
    val title = ApdTranslations.titles[selectedLang] ?: ""
    val bodyText = ApdTranslations.content[selectedLang] ?: ""
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


