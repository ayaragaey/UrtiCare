package com.example.urticare.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.urticare.model.EntryType
import com.example.urticare.model.LogEntry
import com.example.urticare.model.ConsumableItem
import com.example.urticare.viewmodel.TrackerViewModel
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

private fun formatFriendlyDate(timestampStr: String): String {
    return try {
        val dt = ZonedDateTime.parse(timestampStr)
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy • hh:mm a")
        dt.format(formatter)
    } catch (e: Exception) {
        timestampStr
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickLogConsumptionBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    viewModel: TrackerViewModel,
    entries: List<LogEntry>,
    onLogged: (List<String>, List<String>, String) -> Unit, // createdIds, previousRecentlyUsed, message
    onAddMoreDetails: (String, String, String?) -> Unit // itemName, category, sharedTimestamp
) {
    if (!visible) return

    val context = LocalContext.current

    // Dynamically retrieve favorites and recents from the SQLite database
    val recentlyUsedState by viewModel.recentlyUsedConsumables.collectAsState()
    val favoritesState by viewModel.favoriteConsumableNames.collectAsState()

    // Multi-selection state
    var selectedItems by remember { mutableStateOf(emptyMap<String, SelectedConsumableState>()) }
    var expandedItem by remember { mutableStateOf<String?>(null) }
    var sharedTimestamp by remember { mutableStateOf(ZonedDateTime.now().toString()) }
    var showPickerState by remember { mutableStateOf<PickerConfig?>(null) }

    // Dialog state
    var duplicateCheckItem by remember { mutableStateOf<ConsumableItem?>(null) }
    var recentLogCheckItem by remember { mutableStateOf<ConsumableItem?>(null) }

    val addConsumableToBatch = { item: ConsumableItem ->
        val tempId = "temp_${UUID.randomUUID()}"
        val lastUsed = viewModel.getLastUsedQuantityAndUnit(item.displayName)
        val initialQty = if (item.requiresDosageConfirmation) "" else (lastUsed?.first ?: "1")
        val initialUnit = if (item.requiresDosageConfirmation) "" else (lastUsed?.second ?: (item.defaultUnit ?: "serving"))

        selectedItems = selectedItems + (tempId to SelectedConsumableState(
            id = tempId,
            consumableId = item.id,
            displayName = item.displayName,
            category = item.category,
            quantity = initialQty,
            unit = initialUnit,
            status = "Logged",
            notes = "",
            requiresDosageConfirmation = item.requiresDosageConfirmation,
            allowedUnits = item.allowedUnits ?: "serving|piece|g|kg|oz|cup|tbsp|tsp|slice|bowl|plate|pack"
        ))

        if (item.requiresDosageConfirmation) {
            expandedItem = tempId
        }
    }

    val onConsumableSelected = { item: ConsumableItem ->
        val name = item.displayName
        val existing = selectedItems.values.find { it.displayName.equals(name, ignoreCase = true) }
        if (existing != null) {
            duplicateCheckItem = item
        } else {
            val now = ZonedDateTime.now()
            val isLoggedRecently = entries.any { e ->
                if (e.type != EntryType.CONSUMPTION) return@any false
                val parts = e.metadata?.split(":::") ?: return@any false
                if (parts.size < 2 || !parts[1].equals(name, ignoreCase = true)) return@any false
                try {
                    val entryTime = ZonedDateTime.parse(e.timestamp)
                    val diffSeconds = Math.abs(entryTime.toEpochSecond() - now.toEpochSecond())
                    diffSeconds < 300
                } catch (ex: Exception) {
                    false
                }
            }

            if (isLoggedRecently) {
                recentLogCheckItem = item
            } else {
                addConsumableToBatch(item)
            }
        }
    }

    val handleToggleItem = { name: String ->
        val resolved = viewModel.searchConsumables(name).firstOrNull { it.displayName.equals(name, ignoreCase = true) }
        if (resolved != null) {
            onConsumableSelected(resolved)
        } else {
            val tempId = "temp_${UUID.randomUUID()}"
            selectedItems = selectedItems + (tempId to SelectedConsumableState(
                id = tempId,
                consumableId = null,
                displayName = name,
                category = "Other",
                quantity = "1",
                unit = "serving",
                status = "Logged",
                notes = ""
            ))
        }
    }

    val isFormValid = { true }

    val proceedLogging = {
        val previousRecents = recentlyUsedState.toList()
        val createdIds = mutableListOf<String>()
        val batchId = "batch_${UUID.randomUUID()}"

        selectedItems.values.forEach { item ->
            val finalAmount = ""

            val timestamp = if (item.timestamp.isNotEmpty()) item.timestamp else sharedTimestamp
            // Create LogEntry in unified metadata format: Consumption:::$name:::$cat:::$amt:::$notes:::$status:::$brand:::$ingr:::$react:::$batchId
            val metadata = "Consumption:::${item.displayName}:::${item.category}:::$finalAmount:::${item.notes}:::${item.status}:::::::$batchId"
            
            val entryId = "${EntryType.CONSUMPTION.name}_${UUID.randomUUID()}"
            viewModel.addEntry(EntryType.CONSUMPTION, timestamp, metadata)
            createdIds.add(entryId)
        }

        onDismiss()

        val successMsg = if (selectedItems.size == 1) {
            "${selectedItems.values.first().displayName} logged successfully"
        } else {
            "${selectedItems.size} consumptions logged successfully"
        }

        onLogged(createdIds, previousRecents, successMsg)
    }

    // Dialog 1: Duplicate selected item batch check
    if (duplicateCheckItem != null) {
        val item = duplicateCheckItem!!
        AlertDialog(
            onDismissRequest = { duplicateCheckItem = null },
            containerColor = Color.White,
            title = { Text("${item.displayName} already selected", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text("Would you like to increase its quantity or add it as a separate entry?", color = Color(0xFF475569), fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        val firstEntry = selectedItems.values.find { it.displayName.equals(item.displayName, ignoreCase = true) }
                        if (firstEntry != null) {
                            val currentQty = firstEntry.quantity.toDoubleOrNull() ?: 1.0
                            val newQty = (currentQty + 1.0).toString().removeSuffix(".0")
                            selectedItems = selectedItems + (firstEntry.id to firstEntry.copy(quantity = newQty))
                            Toast.makeText(context, "Increased ${item.displayName} quantity to $newQty", Toast.LENGTH_SHORT).show()
                        }
                        duplicateCheckItem = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF814B92))
                ) {
                    Text("Increase Quantity")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            addConsumableToBatch(item)
                            Toast.makeText(context, "Added ${item.displayName} separately", Toast.LENGTH_SHORT).show()
                            duplicateCheckItem = null
                        }
                    ) {
                        Text("Add Separately", color = Color(0xFF814B92), fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { duplicateCheckItem = null }) {
                        Text("Cancel", color = Color(0xFF64748B))
                    }
                }
            }
        )
    }

    // Dialog 2: Recent log warning
    if (recentLogCheckItem != null) {
        val item = recentLogCheckItem!!
        AlertDialog(
            onDismissRequest = { recentLogCheckItem = null },
            containerColor = Color.White,
            title = { Text("Log Again?", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text("You logged ${item.displayName} recently. Log it again?", color = Color(0xFF475569), fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        addConsumableToBatch(item)
                        recentLogCheckItem = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF814B92))
                ) {
                    Text("Log Again")
                }
            },
            dismissButton = {
                TextButton(onClick = { recentLogCheckItem = null }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
        ) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Quick Log Consumption",
                    color = Color(0xFF0F172A),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Select what you consumed",
                    color = Color(0xFF64748B),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Shared Time Picker Row
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Shared Time:", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = {
                            showPickerState = PickerConfig(sharedTimestamp) { newTime ->
                                sharedTimestamp = newTime
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(formatFriendlyDate(sharedTimestamp), color = Color(0xFF0F172A), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            HorizontalDivider(color = Color(0xFFF1F5F9))

            // Lists
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                var quickSearchQuery by remember { mutableStateOf("") }

                // Search Field for typing with auto-fill
                OutlinedTextField(
                    value = quickSearchQuery,
                    onValueChange = { quickSearchQuery = it },
                    placeholder = {
                        Text(
                            "Search or type what you consumed",
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = { SearchIcon() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE2E8F0),
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC),
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A)
                    ),
                    singleLine = true
                )

                if (quickSearchQuery.isNotEmpty()) {
                    // Show search results for auto-fill
                    val filtered = viewModel.searchConsumables(quickSearchQuery).take(5)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                            .padding(8.dp)
                    ) {
                        filtered.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = item.selectable) {
                                        onConsumableSelected(item)
                                        quickSearchQuery = ""
                                    }
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = item.displayName,
                                        color = if (item.selectable) Color(0xFF0F172A) else Color(0xFF94A3B8),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp
                                    )
                                    Text("${item.category} > ${item.subcategory}", color = Color(0xFF64748B), fontSize = 10.5.sp)
                                }
                                if (item.selectable) {
                                    KeyboardArrowRightIcon(color = Color(0xFF64748B), modifier = Modifier.size(12.dp))
                                }
                            }
                            HorizontalDivider(color = Color(0xFFE2E8F0))
                        }

                        // Add Custom Item Card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val tempId = "temp_${UUID.randomUUID()}"
                                    selectedItems = selectedItems + (tempId to SelectedConsumableState(
                                        id = tempId,
                                        consumableId = null,
                                        displayName = quickSearchQuery,
                                        category = "Other",
                                        quantity = "1",
                                        unit = "serving",
                                        status = "Logged",
                                        notes = ""
                                    ))
                                    quickSearchQuery = ""
                                    Toast.makeText(context, "Added custom item to selection", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Add custom: \"$quickSearchQuery\"",
                                color = Color(0xFFF78325),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Selection Summary Area
                if (selectedItems.isNotEmpty()) {
                    Text(
                        text = "SELECTED ITEMS (${selectedItems.size})",
                        color = Color(0xFF814B92),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    selectedItems.values.forEach { selected ->
                        val isExpanded = expandedItem == selected.id
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .border(1.dp, Color(0xFFE2E8F0), shape = RoundedCornerShape(12.dp))
                                .background(Color(0xFFFAF8FF), shape = RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(selected.displayName, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                                    val amtText = if (selected.quantity.isNotEmpty()) "${selected.quantity} ${selected.unit}" else selected.unit
                                    Text("Amount: $amtText", color = Color(0xFF64748B), fontSize = 11.sp)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(onClick = { expandedItem = if (isExpanded) null else selected.id }) {
                                        Text(if (isExpanded) "Collapse" else "Quick Edit", color = Color(0xFF814B92), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    IconButton(onClick = { selectedItems = selectedItems.filterKeys { it != selected.id } }) {
                                        Text("✕", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(8.dp))

                                // Quantity & Unit edit inputs
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Quantity", color = Color(0xFF0F172A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        OutlinedTextField(
                                            value = selected.quantity,
                                            onValueChange = { q -> selectedItems = selectedItems + (selected.id to selected.copy(quantity = q)) },
                                            placeholder = { Text("e.g. 1", fontSize = 12.sp) },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Unit", color = Color(0xFF0F172A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        OutlinedTextField(
                                            value = selected.unit,
                                            onValueChange = { u -> selectedItems = selectedItems + (selected.id to selected.copy(unit = u)) },
                                            placeholder = { Text("e.g. serving", fontSize = 12.sp) },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Notes Field
                                Text("Notes", color = Color(0xFF0F172A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                OutlinedTextField(
                                    value = selected.notes,
                                    onValueChange = { n -> selectedItems = selectedItems + (selected.id to selected.copy(notes = n)) },
                                    placeholder = { Text("Optional notes", fontSize = 12.sp) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Individual override timestamp
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Override Time:", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    if (selected.timestamp.isNotEmpty()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Button(
                                                onClick = {
                                                    showPickerState = PickerConfig(selected.timestamp) { newTime ->
                                                        selectedItems = selectedItems + (selected.id to selected.copy(timestamp = newTime))
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                modifier = Modifier.height(26.dp)
                                            ) {
                                                Text(formatFriendlyDate(selected.timestamp), color = Color(0xFF0F172A), fontSize = 9.sp)
                                            }
                                            TextButton(
                                                onClick = { selectedItems = selectedItems + (selected.id to selected.copy(timestamp = "")) },
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text("Clear", color = Color(0xFFEF4444), fontSize = 10.sp)
                                            }
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                showPickerState = PickerConfig(sharedTimestamp) { newTime ->
                                                    selectedItems = selectedItems + (selected.id to selected.copy(timestamp = newTime))
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            modifier = Modifier.height(26.dp)
                                        ) {
                                            Text("Set Override", color = Color(0xFF814B92), fontSize = 9.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Status Toggle
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { selectedItems = selectedItems + (selected.id to selected.copy(status = "Logged")) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (selected.status == "Logged") Color(0xFF509729) else Color(0xFFF1F5F9),
                                            contentColor = if (selected.status == "Logged") Color.White else Color(0xFF64748B)
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f).height(30.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Logged", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { selectedItems = selectedItems + (selected.id to selected.copy(status = "Trigger")) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (selected.status == "Trigger") Color(0xFFF78325) else Color(0xFFF1F5F9),
                                            contentColor = if (selected.status == "Trigger") Color.White else Color(0xFF64748B)
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f).height(30.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Trigger", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Recently Used Section
                if (recentlyUsedState.isNotEmpty()) {
                    Text(
                        text = "RECENTLY USED",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Column(modifier = Modifier.fillMaxWidth()) {
                        recentlyUsedState.forEach { name ->
                            val isSelected = selectedItems.values.any { it.displayName.equals(name, ignoreCase = true) }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { handleToggleItem(name) }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = name,
                                    color = if (isSelected) Color(0xFF814B92) else Color(0xFF0F172A),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.5.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(Color.White, CircleShape)
                                        .border(
                                            width = 1.5.dp,
                                            color = if (isSelected) Color(0xFF814B92) else Color(0xFFCBD5E1),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(Color(0xFF814B92), CircleShape)
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Favorites Section
                if (favoritesState.isNotEmpty()) {
                    Text(
                        text = "FAVORITES",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Column(modifier = Modifier.fillMaxWidth()) {
                        favoritesState.forEach { name ->
                            val isSelected = selectedItems.values.any { it.displayName.equals(name, ignoreCase = true) }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { handleToggleItem(name) }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = name,
                                    color = if (isSelected) Color(0xFF814B92) else Color(0xFF0F172A),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.5.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(Color.White, CircleShape)
                                        .border(
                                            width = 1.5.dp,
                                            color = if (isSelected) Color(0xFF814B92) else Color(0xFFCBD5E1),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(Color(0xFF814B92), CircleShape)
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Add Detailed Entry link
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Add Detailed Entry",
                        color = Color(0xFF814B92),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .clickable {
                                val firstItem = selectedItems.values.firstOrNull()?.displayName ?: ""
                                val category = selectedItems.values.firstOrNull()?.category ?: "Other"
                                onAddMoreDetails(firstItem, category, sharedTimestamp)
                            }
                    )
                }
            }

            // Footer / Log Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                val selectedCount = selectedItems.size
                val valid = isFormValid() && selectedCount > 0
                val logText = if (selectedCount > 0) "Log ($selectedCount)" else "Log"
                Button(
                    onClick = proceedLogging,
                    enabled = valid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF1F5F9),
                        contentColor = Color(0xFFF78325),
                        disabledContainerColor = Color(0xFFF1F5F9).copy(alpha = 0.5f),
                        disabledContentColor = Color(0xFFF78325).copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = logText,
                        color = Color(0xFFF78325),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }

    showPickerState?.let { config ->
        DateTimePickerDialog(
            initialTimestamp = config.initialTimestamp,
            themeColor = Color(0xFFF78325),
            onDismiss = { showPickerState = null },
            onConfirm = { newTime ->
                config.onConfirm(newTime)
                showPickerState = null
            }
        )
    }
}
