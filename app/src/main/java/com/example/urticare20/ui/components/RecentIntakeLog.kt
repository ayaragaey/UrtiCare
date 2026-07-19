package com.example.urticare20.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.example.urticare20.model.MedicationDirectory
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import java.time.Duration
import java.time.ZonedDateTime

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecentIntakeLog(viewModel: TrackerViewModel, entries: List<LogEntry>) {
    val context = LocalContext.current

    val antihistamines by viewModel.profileAntihistamines.collectAsState()
    val profileCortisones by viewModel.profileCortisones.collectAsState()

    var editingEntry by remember { mutableStateOf<LogEntry?>(null) }
    var editTimestamp by remember { mutableStateOf("") }
    var editMedName by remember { mutableStateOf("") }
    var editMedMgs by remember { mutableStateOf("") }
    var editEffectStarted by remember { mutableStateOf("") }
    var showAutocompleteSuggestions by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(-1) }

    // Filter for Pills only (Antihistamine and Cortisone)
    val pillEntries = remember(entries) {
        entries.filter { it.type == EntryType.ANTIHISTAMINE || it.type == EntryType.CORTISONE }
    }

    val displayEntries = remember(pillEntries) {
        pillEntries.take(4)
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
        val nextOlder = pillEntries.getOrNull(pillEntries.indexOf(item) + 1) ?: return null
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

    // Check if consecutive pills gap is less than 8 hours
    fun isPillIntervalAlert(item: LogEntry): Boolean {
        val nextOlder = pillEntries.getOrNull(pillEntries.indexOf(item) + 1) ?: return false
        return try {
            val d1 = ZonedDateTime.parse(item.timestamp).withZoneSameInstant(java.time.ZoneId.systemDefault())
            val d2 = ZonedDateTime.parse(nextOlder.timestamp).withZoneSameInstant(java.time.ZoneId.systemDefault())
            val diffMins = Math.abs(Duration.between(d1, d2).toMinutes())
            diffMins < 8 * 60
        } catch (e: Exception) {
            false
        }
    }

    // Trigger simple Date & Time picker
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

    fun showEditDialog(item: LogEntry) {
        editingEntry = item
        editTimestamp = item.timestamp
        if (item.type == EntryType.ANTIHISTAMINE || item.type == EntryType.CORTISONE) {
            val parts = (item.metadata ?: "").split(":::")
            val rawName = if (parts.size >= 2) parts[0].trim() else (item.metadata ?: "").trim()
            val rawMgs = if (parts.size >= 2) parts[1].trim() else ""
            
            if (item.type == EntryType.ANTIHISTAMINE) {
                val mainMed = antihistamines.find { it.isMain } ?: antihistamines.firstOrNull()
                val otherMed = antihistamines.find { !it.isMain } ?: antihistamines.firstOrNull()
                val resolvedMed = when {
                    rawName.equals("Cetirizine", ignoreCase = true) -> mainMed
                    rawName.equals("Loratadine", ignoreCase = true) -> otherMed
                    else -> antihistamines.find { it.name.trim().lowercase() == rawName.lowercase() } ?: mainMed
                }
                if (resolvedMed != null) {
                    editMedName = resolvedMed.name
                    editMedMgs = resolvedMed.mgs.replace("mg", "").trim()
                } else {
                    editMedName = rawName
                    editMedMgs = rawMgs.replace("mg", "").trim()
                }
                editEffectStarted = if (parts.size >= 3) parts[2] else ""
            } else {
                val mainMed = profileCortisones.firstOrNull()
                val resolvedMed = profileCortisones.find { it.name.trim().lowercase() == rawName.lowercase() } ?: mainMed
                if (resolvedMed != null) {
                    editMedName = resolvedMed.name
                    editMedMgs = resolvedMed.mgs.replace("mg", "").trim()
                } else {
                    editMedName = rawName
                    editMedMgs = rawMgs.replace("mg", "").trim()
                }
                editEffectStarted = ""
            }
        } else {
            editMedName = ""
            editMedMgs = ""
            editEffectStarted = ""
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCard)
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Medication Intake Log",
                    color = LightGray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                if (displayEntries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No pill entries logged yet",
                            color = MutedGray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    displayEntries.forEachIndexed { idx, item ->
                        val friendlyLabel = when (item.type) {
                            EntryType.ANTIHISTAMINE -> {
                                val mainMed = antihistamines.find { it.isMain } ?: antihistamines.firstOrNull()
                                if (mainMed != null) {
                                    val cleanMgs = mainMed.mgs.replace("mg", "").trim()
                                    "${mainMed.name} ($cleanMgs mg)"
                                } else {
                                    if (!item.metadata.isNullOrEmpty()) {
                                        val parts = item.metadata.split(":::")
                                        val dose = if (parts.size >= 2) parts[1].trim() else ""
                                        val name = parts[0].trim()
                                        val cleanDose = dose.replace("mg", "").trim()
                                        if (cleanDose.isNotEmpty()) "$name ($cleanDose mg)" else name
                                    } else {
                                        "Antihestamine"
                                    }
                                }
                            }
                            EntryType.CORTISONE -> {
                                val mainMed = profileCortisones.firstOrNull()
                                if (mainMed != null) {
                                    val cleanMgs = mainMed.mgs.replace("mg", "").trim()
                                    "${mainMed.name} ($cleanMgs mg)"
                                } else {
                                    if (!item.metadata.isNullOrEmpty()) {
                                        val parts = item.metadata.split(":::")
                                        val dose = if (parts.size >= 2) parts[1].trim() else ""
                                        val name = parts[0].trim()
                                        val cleanDose = dose.replace("mg", "").trim()
                                        if (cleanDose.isNotEmpty()) "$name ($cleanDose mg)" else name
                                    } else {
                                        "Cortisone"
                                    }
                                }
                            }
                            else -> "Pill"
                        }

                        val gapText = getGapText(item)
                        val isAlertRed = isPillIntervalAlert(item)

                        val itemColor = when {
                            isAlertRed -> AlertRed
                            item.type == EntryType.ANTIHISTAMINE -> Color(0xFF509729)
                            item.type == EntryType.CORTISONE -> Color(0xFF1A7E97)
                            else -> Color(0xFF737373)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(itemColor, shape = RoundedCornerShape(3.dp))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    
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
                                    color = Color(0xFF737373),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                val streakBreakReason = if (item.type == EntryType.ANTIHISTAMINE || item.type == EntryType.CORTISONE) {
                                    val parts = (item.metadata ?: "").split(":::")
                                        .filter { !it.startsWith("wearing_off:") && !it.startsWith("Ongoing Medication:") }
                                    parts.find { it.startsWith("Streak Break Reason:") }
                                } else {
                                    null
                                }

                                if (streakBreakReason != null) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = streakBreakReason,
                                        color = Color(0xFF737373),
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
                                        color = Color(0xFF737373),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                            }

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

    if (editingEntry != null) {
        val entry = editingEntry!!
        AlertDialog(
            onDismissRequest = { editingEntry = null },
            containerColor = DarkSurface,
            title = {
                Text(
                    text = "Edit Log Entry",
                    color = if (entry.type == EntryType.ANTIHISTAMINE) Color(0xFF737373)
                            else if (entry.type == EntryType.CORTISONE) SoftYellow
                            else PastelIceBlue,
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
                            color = if (entry.type == EntryType.ANTIHISTAMINE) Color(0xFF737373) else Color.White,
                            fontSize = 12.sp
                        )
                    }

                    if (entry.type == EntryType.ANTIHISTAMINE || entry.type == EntryType.CORTISONE) {
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
                                            containerColor = if (editMedName == med.name && editMedMgs == med.mgs) SoftYellow.copy(alpha = 0.2f) else DarkCard
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            if (editMedName == med.name && editMedMgs == med.mgs) SoftYellow else DarkBorder
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "${med.name} (${med.mgs} mg)",
                                                color = if (editMedName == med.name && editMedMgs == med.mgs) SoftYellow else Color(0xFF737373),
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

                        if (entry.type == EntryType.ANTIHISTAMINE) {
                            Spacer(modifier = Modifier.height(8.dp))
                            if (editEffectStarted.isEmpty()) {
                                OutlinedButton(
                                    onClick = {
                                        val d1 = try { ZonedDateTime.parse(editTimestamp) } catch (e: Exception) { ZonedDateTime.now() }
                                        val d2 = ZonedDateTime.now()
                                        val duration = java.time.Duration.between(d1, d2)
                                        val diffMins = Math.max(0L, duration.toMinutes())
                                        editEffectStarted = if (diffMins < 60) {
                                            "$diffMins mins"
                                        } else {
                                            val hrs = diffMins / 60
                                            val mins = diffMins % 60
                                            if (mins > 0) "$hrs hrs $mins mins" else "$hrs hrs"
                                        }
                                    },
                                    border = BorderStroke(1.dp, Color(0xFF509729)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF509729)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(40.dp)
                                ) {
                                    Text("Medication Response Time", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = "Optional",
                                    color = MutedGray,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                )
                            } else {
                                OutlinedTextField(
                                    value = editEffectStarted,
                                    onValueChange = { editEffectStarted = it },
                                    label = { Text("Effect Started", color = MutedGray, fontSize = 11.sp) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color(0xFF737373),
                                        unfocusedTextColor = Color(0xFF737373),
                                        focusedBorderColor = Color(0xFF509729),
                                        unfocusedBorderColor = DarkBorder,
                                        focusedContainerColor = DarkCard,
                                        unfocusedContainerColor = DarkCard
                                    ),
                                    trailingIcon = {
                                        IconButton(onClick = { editEffectStarted = "" }) {
                                            Text(
                                                text = "✕",
                                                color = MutedGray,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
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
                            if (entry.type == EntryType.ANTIHISTAMINE) {
                                val isExist = antihistamines.any { it.name.trim().lowercase() == editMedName.trim().lowercase() }
                                if (!isExist && editMedName.trim().isNotEmpty()) {
                                    viewModel.addProfileAntihistamine(editMedName.trim(), editMedMgs.trim(), "2nd", isMain = false)
                                }
                            } else if (entry.type == EntryType.CORTISONE) {
                                val isExist = profileCortisones.any { it.name.trim().lowercase() == editMedName.trim().lowercase() }
                                if (!isExist && editMedName.trim().isNotEmpty()) {
                                    viewModel.addProfileCortisone(editMedName.trim(), editMedMgs.trim())
                                }
                            }
                            val baseMeta = "${editMedName.trim()}:::${editMedMgs.trim()}"
                            if (entry.type == EntryType.ANTIHISTAMINE) {
                                "$baseMeta:::${editEffectStarted.trim()}"
                            } else {
                                val oldParts = (entry.metadata ?: "").split(":::")
                                val oldReason = if (oldParts.size >= 3) oldParts.drop(2).joinToString(":::") else ""
                                if (oldReason.isNotEmpty()) "$baseMeta:::$oldReason" else baseMeta
                            }
                        } else {
                            entry.metadata
                        }
                        viewModel.updateEntryDetails(entry.id, editTimestamp, metadata, force = false)
                        editingEntry = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (entry.type == EntryType.CORTISONE) SoftYellow
                                          else if (entry.type == EntryType.ANTIHISTAMINE) Color(0xFF737373)
                                          else PastelIceBlue,
                        contentColor = if (entry.type == EntryType.ANTIHISTAMINE) Color.White else AmoledBlack
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
}
