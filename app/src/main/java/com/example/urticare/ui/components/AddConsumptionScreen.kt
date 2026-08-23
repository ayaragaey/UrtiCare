package com.example.urticare.ui.components

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.geometry.Offset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.urticare.model.EntryType
import com.example.urticare.model.ConsumableItem
import com.example.urticare.viewmodel.TrackerViewModel
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

// Local state class representing an item added to the active selection batch
data class SelectedConsumableState(
    val id: String,
    val consumableId: String?, // null if custom item
    val displayName: String,
    val category: String,
    val quantity: String,
    val unit: String,
    val status: String, // "Logged" or "Trigger"
    val notes: String,
    val brand: String = "",
    val ingredients: String = "",
    val reaction: String = "",
    val timestamp: String = "", // empty if using shared timestamp
    val requiresDosageConfirmation: Boolean = false,
    val allowedUnits: String = ""
)

private fun formatFriendlyDate(timestampStr: String): String {
    return try {
        val dt = ZonedDateTime.parse(timestampStr).withZoneSameInstant(java.time.ZoneId.systemDefault())
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
        
        val hour = dt.hour
        val minute = dt.minute
        val ampm = if (hour >= 12) "PM" else "AM"
        val displayHour = if (hour % 12 == 0) 12 else hour % 12
        val displayMinute = String.format("%02d", minute)
        
        "$relativeDate • $displayHour:$displayMinute $ampm"
    } catch (e: Exception) {
        timestampStr
    }
}

data class PickerConfig(
    val initialTimestamp: String,
    val onConfirm: (String) -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddConsumptionScreen(
    viewModel: TrackerViewModel,
    onBack: () -> Unit,
    prefilledItemName: String? = null,
    prefilledCategory: String? = null,
    prefilledTimestamp: String? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val recentlyUsedState by viewModel.recentlyUsedConsumables.collectAsState()
    val favoritesState by viewModel.favoriteConsumableNames.collectAsState()
    val favoriteIds by viewModel.favoriteConsumableIds.collectAsState()
    val entries by viewModel.entries.collectAsState()

    // Multi-selection state maps unique temp UUIDs to selected details
    val selectedConsumables = remember { mutableStateMapOf<String, SelectedConsumableState>() }
    var sharedTimestamp by remember { mutableStateOf(prefilledTimestamp ?: ZonedDateTime.now().toString()) }
    var showPickerState by remember { mutableStateOf<PickerConfig?>(null) }

    var mode by remember { mutableStateOf(AndroidScreenMode.MAIN) }
    val history = remember { mutableStateListOf<AndroidScreenMode>() }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<CategoryItem?>(null) }
    var seeAllType by remember { mutableStateOf<String?>(null) }

    // Dialog flags
    var duplicateCheckItem by remember { mutableStateOf<ConsumableItem?>(null) }
    var recentLogCheckItem by remember { mutableStateOf<ConsumableItem?>(null) }

    val addConsumableToBatch = { item: ConsumableItem ->
        val tempId = "temp_${UUID.randomUUID()}"
        selectedConsumables[tempId] = SelectedConsumableState(
            id = tempId,
            consumableId = item.id,
            displayName = item.displayName,
            category = item.category,
            quantity = "",
            unit = "",
            status = "Logged",
            notes = "",
            requiresDosageConfirmation = false,
            allowedUnits = ""
        )
    }

    val onConsumableSelected = { item: ConsumableItem ->
        val name = item.displayName
        // 1. Check if already selected in the current batch
        val alreadySelected = selectedConsumables.values.find { it.displayName.equals(name, ignoreCase = true) }
        if (alreadySelected != null) {
            Toast.makeText(context, "${item.displayName} is already selected", Toast.LENGTH_SHORT).show()
        } else {
            // 2. Check if logged recently (within 5 minutes) in previous sessions
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
                Toast.makeText(context, "$name added to selection", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Prefill logic
    LaunchedEffect(prefilledItemName) {
        if (!prefilledItemName.isNullOrBlank()) {
            val item = viewModel.searchConsumables(prefilledItemName).firstOrNull { it.displayName.equals(prefilledItemName, ignoreCase = true) }
            if (item != null) {
                onConsumableSelected(item)
                mode = AndroidScreenMode.DETAILS
            } else {
                // Add custom prefilled item
                val tempId = "temp_${UUID.randomUUID()}"
                selectedConsumables[tempId] = SelectedConsumableState(
                    id = tempId,
                    consumableId = null,
                    displayName = prefilledItemName,
                    category = prefilledCategory ?: "Other",
                    quantity = "",
                    unit = "",
                    status = "Logged",
                    notes = ""
                )
                mode = AndroidScreenMode.DETAILS
            }
        }
    }

    val navigateTo = { newMode: AndroidScreenMode ->
        history.add(mode)
        mode = newMode
    }

    val handleBackPress = {
        if (history.isNotEmpty()) {
            val prev = history.removeAt(history.size - 1)
            mode = prev
        } else {
            onBack()
        }
    }

    BackHandler(enabled = true) {
        handleBackPress()
    }

    val logConsumptionBatch = {
        val batchId = "batch_${UUID.randomUUID()}"
        val createdIds = mutableListOf<String>()
        val savedNames = mutableListOf<String>()

        selectedConsumables.values.forEach { selected ->
            // Enforce registration of custom items in catalog first
            if (selected.consumableId == null) {
                viewModel.addCustomConsumable(selected.displayName, selected.category, selected.unit)
            }

            val finalAmount = ""

            val timestamp = if (selected.timestamp.isNotEmpty()) selected.timestamp else sharedTimestamp
            // Compile metadata format: Consumption:::$name:::$cat:::$amt:::$notes:::$status:::$brand:::$ingr:::$react:::$batchId
            val metadata = "Consumption:::${selected.displayName}:::${selected.category}:::$finalAmount:::${selected.notes}:::${selected.status}:::${selected.brand}:::${selected.ingredients}:::${selected.reaction}:::$batchId"

            val entryId = "${EntryType.CONSUMPTION.name}_${UUID.randomUUID()}"
            viewModel.addEntry(EntryType.CONSUMPTION, timestamp, metadata)
            createdIds.add(entryId)
            savedNames.add(selected.displayName)
        }

        // Show Snackbar with Undo action
        val count = selectedConsumables.size
        coroutineScope.launch {
            val message = if (count == 1) "${savedNames.first()} logged successfully" else "$count consumptions logged successfully"
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                // Fetch the entries just saved by batchId and delete them
                val allLogs = viewModel.entries.value
                val batchEntryIds = allLogs.filter { log ->
                    val parts = log.metadata?.split(":::")
                    parts != null && parts.size >= 10 && parts[9] == batchId
                }.map { it.id }
                viewModel.deleteEntries(batchEntryIds)
                Toast.makeText(context, "Logged batch undone successfully", Toast.LENGTH_SHORT).show()
            }
        }

        selectedConsumables.clear()
        onBack()
    }

    // Dialog 1: Duplicate item in current batch prompt
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
                        val firstEntry = selectedConsumables.values.find { it.displayName.equals(item.displayName, ignoreCase = true) }
                        if (firstEntry != null) {
                            val currentQty = firstEntry.quantity.toDoubleOrNull() ?: 1.0
                            val newQty = (currentQty + 1.0).toString().removeSuffix(".0")
                            selectedConsumables[firstEntry.id] = firstEntry.copy(quantity = newQty)
                            Toast.makeText(context, "Increased ${item.displayName} quantity to $newQty", Toast.LENGTH_SHORT).show()
                        }
                        duplicateCheckItem = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF78325))
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
                        Text("Add Separately", color = Color(0xFFF78325), fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { duplicateCheckItem = null }) {
                        Text("Cancel", color = Color(0xFF64748B))
                    }
                }
            }
        )
    }

    // Dialog 2: Recent log warning dialog
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF78325))
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (mode) {
                                AndroidScreenMode.MAIN -> "Add Consumption"
                                AndroidScreenMode.CATEGORY_LIST -> selectedCategory?.name ?: "Category List"
                                AndroidScreenMode.SEE_ALL -> if (seeAllType == "recents") "Recently Used" else "Favorites"
                                AndroidScreenMode.DETAILS -> "Selected Items Summary"
                            },
                            color = Color(0xFF0F172A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { handleBackPress() },
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(36.dp)
                            .background(Color.White, shape = CircleShape)
                            .border(1.dp, Color(0xFFE2E8F0), shape = CircleShape)
                    ) {
                        ArrowBackIcon(color = Color(0xFF0F172A))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                modifier = Modifier.shadow(1.dp)
            )
        },
        bottomBar = {
            // Persistent selection tray shown at the bottom of search / category landing screens
            if (mode != AndroidScreenMode.DETAILS && selectedConsumables.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedConsumables.size} item${if (selectedConsumables.size > 1) "s" else ""} selected",
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )

                            TextButton(
                                onClick = {
                                    searchQuery = ""
                                    navigateTo(AndroidScreenMode.MAIN)
                                }
                            ) {
                                Text("+ Add Another Item", color = Color(0xFFF78325), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        // Scrollable row of selected item chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            selectedConsumables.values.forEach { selected ->
                                SuggestionChip(
                                    onClick = { selectedConsumables.remove(selected.id) },
                                    label = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(selected.displayName, fontSize = 11.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.SemiBold)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("✕", fontSize = 10.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                                        }
                                    },
                                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color(0xFFF1F5F9)),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                )
                            }
                        }

                        Button(
                            onClick = { navigateTo(AndroidScreenMode.DETAILS) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF78325)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text("Log Selected (${selectedConsumables.size})", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.White
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
        ) {
            when (mode) {
                AndroidScreenMode.MAIN -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 24.dp)
                    ) {
                        // Search Field
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    "Search for what you consumed",
                                    color = Color(0xFF94A3B8),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            },
                            leadingIcon = { SearchIcon() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(24.dp),
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

                        if (searchQuery.isNotEmpty()) {
                            // Search Results
                            val filtered = viewModel.searchConsumables(searchQuery).take(50)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Text(
                                    text = "Search Results",
                                    color = Color(0xFF0F172A),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                if (filtered.isNotEmpty()) {
                                    filtered.forEach { item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable(enabled = item.selectable) {
                                                    onConsumableSelected(item)
                                                }
                                                .padding(vertical = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = item.displayName,
                                                    color = if (item.selectable) Color(0xFF0F172A) else Color(0xFF94A3B8),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                val parentPart = if (!item.parentId.isNullOrEmpty()) {
                                                    val parent = filtered.find { it.id == item.parentId }
                                                    if (parent != null) " (${parent.displayName})" else ""
                                                } else ""
                                                Text("${item.category} > ${item.subcategory}$parentPart", color = Color(0xFF64748B), fontSize = 11.sp)
                                            }
                                            if (item.selectable) {
                                                KeyboardArrowRightIcon(color = Color(0xFF64748B), modifier = Modifier.size(12.dp))
                                            }
                                        }
                                        HorizontalDivider(color = Color(0xFFE2E8F0))
                                    }
                                }

                                // Custom Option Card
                                Card(
                                    onClick = {
                                        // Add custom item
                                        val tempId = "temp_${UUID.randomUUID()}"
                                        selectedConsumables[tempId] = SelectedConsumableState(
                                            id = tempId,
                                            consumableId = null,
                                            displayName = searchQuery,
                                            category = "Other",
                                            quantity = "",
                                            unit = "",
                                            status = "Logged",
                                            notes = ""
                                        )
                                        Toast.makeText(context, "Added \"$searchQuery\" to selection", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEAD2)),
                                    border = BorderStroke(1.dp, Color(0xFFF78325)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Add custom item: \"$searchQuery\"",
                                            color = Color(0xFFF78325),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            // Recently Used Section
                            if (recentlyUsedState.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Recently Used",
                                        color = Color(0xFF0F172A),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        "See all",
                                        color = Color(0xFFF78325),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable {
                                            seeAllType = "recents"
                                            navigateTo(AndroidScreenMode.SEE_ALL)
                                        }
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    recentlyUsedState.take(8).forEach { name ->
                                        SuggestionChip(
                                            onClick = {
                                                val resolved = viewModel.searchConsumables(name).firstOrNull { it.displayName.equals(name, ignoreCase = true) }
                                                if (resolved != null) {
                                                    onConsumableSelected(resolved)
                                                } else {
                                                    // Custom prefilled
                                                    val tempId = "temp_${UUID.randomUUID()}"
                                                    selectedConsumables[tempId] = SelectedConsumableState(
                                                        id = tempId,
                                                        consumableId = null,
                                                        displayName = name,
                                                        category = "Other",
                                                        quantity = "",
                                                        unit = "",
                                                        status = "Logged",
                                                        notes = ""
                                                    )
                                                }
                                            },
                                            label = { Text(name, color = Color(0xFF0F172A), fontWeight = FontWeight.SemiBold) },
                                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color.White),
                                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                        )
                                    }
                                }
                            }

                            // Favorites Section
                            if (favoritesState.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Favorites",
                                        color = Color(0xFF0F172A),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        "See all",
                                        color = Color(0xFFF78325),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable {
                                            seeAllType = "favorites"
                                            navigateTo(AndroidScreenMode.SEE_ALL)
                                        }
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    favoritesState.take(8).forEach { name ->
                                        SuggestionChip(
                                            onClick = {
                                                val resolved = viewModel.searchConsumables(name).firstOrNull { it.displayName.equals(name, ignoreCase = true) }
                                                if (resolved != null) {
                                                    onConsumableSelected(resolved)
                                                } else {
                                                    // Custom prefilled
                                                    val tempId = "temp_${UUID.randomUUID()}"
                                                    selectedConsumables[tempId] = SelectedConsumableState(
                                                        id = tempId,
                                                        consumableId = null,
                                                        displayName = name,
                                                        category = "Other",
                                                        quantity = "",
                                                        unit = "",
                                                        status = "Logged",
                                                        notes = ""
                                                    )
                                                }
                                            },
                                            label = { Text(name, color = Color(0xFF0F172A), fontWeight = FontWeight.SemiBold) },
                                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color.White),
                                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Choose a Category",
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )

                            // Categories List
                            CATEGORIES.forEach { cat ->
                                Card(
                                    onClick = {
                                        selectedCategory = cat
                                        navigateTo(AndroidScreenMode.CATEGORY_LIST)
                                    },
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                        .shadow(1.dp, shape = RoundedCornerShape(12.dp))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(cat.color, shape = CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(cat.emoji, fontSize = 20.sp)
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = cat.name,
                                                color = Color(0xFF0F172A),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                            Text(
                                                text = cat.description,
                                                color = Color(0xFF64748B),
                                                fontSize = 12.sp
                                            )
                                        }

                                        KeyboardArrowRightIcon(color = Color(0xFF64748B))
                                    }
                                }
                            }
                        }
                    }
                }

                AndroidScreenMode.CATEGORY_LIST -> {
                    val categoryId = selectedCategory?.id ?: ""
                    val items = remember(categoryId) { viewModel.getConsumablesByCategory(categoryId) }
                    val subcategories = remember(items) { items.groupBy { it.subcategory }.toList().sortedBy { it.first } }
                    val expandedSubcats = remember { mutableStateMapOf<String, Boolean>() }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 80.dp) // Offset for persistent bottom tray
                    ) {
                        Text(
                            text = "Browse ${selectedCategory?.name} Subcategories:",
                            color = Color(0xFF64748B),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(16.dp)
                        )

                        subcategories.forEach { pair ->
                            val subcatName = pair.first
                            val subcatItems = pair.second
                            val isExpanded = expandedSubcats[subcatName] == true
                            Card(
                                onClick = { expandedSubcats[subcatName] = !isExpanded },
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = subcatName,
                                            color = Color(0xFF0F172A),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = if (isExpanded) "Collapse" else "Expand (${subcatItems.size})",
                                            color = Color(0xFFF78325),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    if (isExpanded) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFFF8FAFC))
                                                .padding(bottom = 8.dp)
                                        ) {
                                            val groups = subcatItems.groupBy { item: ConsumableItem -> item.itemGroup ?: "" }
                                            groups.forEach { entry ->
                                                val groupName = entry.key
                                                val groupItems = entry.value
                                                if (groupName.isNotEmpty()) {
                                                    Text(
                                                        text = groupName,
                                                        color = Color(0xFF64748B),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
                                                    )
                                                }
                                                groupItems.forEach { item ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable(enabled = item.selectable) {
                                                                onConsumableSelected(item)
                                                            }
                                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column {
                                                            Text(
                                                                text = item.displayName,
                                                                color = if (item.selectable) Color(0xFF0F172A) else Color(0xFF94A3B8),
                                                                fontWeight = FontWeight.SemiBold,
                                                                fontSize = 13.sp
                                                            )
                                                            if (!item.parentId.isNullOrEmpty()) {
                                                                val parentName = subcatItems.find { it.id == item.parentId }?.displayName
                                                                if (parentName != null) {
                                                                    Text(
                                                                        text = "Subtype of $parentName",
                                                                        color = Color(0xFF94A3B8),
                                                                        fontSize = 10.sp
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        if (item.selectable) {
                                                            KeyboardArrowRightIcon(color = Color(0xFF64748B), modifier = Modifier.size(12.dp))
                                                        } else {
                                                            Text("Not selectable", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                                        }
                                                    }
                                                    HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(horizontal = 16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                AndroidScreenMode.SEE_ALL -> {
                    val listToDisplay = if (seeAllType == "recents") recentlyUsedState else favoritesState
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 80.dp)
                    ) {
                        Text(
                            text = "Tapping an item adds it to your active selection:",
                            color = Color(0xFF64748B),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            listToDisplay.forEach { name ->
                                val resolvedItem = remember(name) { viewModel.searchConsumables(name).firstOrNull { it.displayName.equals(name, ignoreCase = true) } }
                                val category = resolvedItem?.category ?: "Other"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (resolvedItem != null) {
                                                onConsumableSelected(resolvedItem)
                                            } else {
                                                // Custom prefilled
                                                val tempId = "temp_${UUID.randomUUID()}"
                                                selectedConsumables[tempId] = SelectedConsumableState(
                                                    id = tempId,
                                                    consumableId = null,
                                                    displayName = name,
                                                    category = category,
                                                    quantity = "",
                                                    unit = "",
                                                    status = "Logged",
                                                    notes = ""
                                                )
                                            }
                                        }
                                        .padding(vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(name, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text(category, fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                                            colors = SuggestionChipDefaults.suggestionChipColors(
                                                labelColor = Color(0xFFF78325),
                                                containerColor = Color(0xFFFFEAD2)
                                            ),
                                            border = BorderStroke(0.dp, Color.Transparent),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                    }
                                    KeyboardArrowRightIcon(color = Color(0xFF64748B), modifier = Modifier.size(12.dp))
                                }
                                HorizontalDivider(color = Color(0xFFE2E8F0))
                            }
                        }
                    }
                }

                AndroidScreenMode.DETAILS -> {
                    // Selected Items Summary Editor Screen
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 24.dp)
                    ) {
                        // Shared Date & Time configuration card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Shared Date & Time (Default)", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Applies to all selected items unless overridden individually.", color = Color(0xFF64748B), fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        showPickerState = PickerConfig(sharedTimestamp) { newTime ->
                                            sharedTimestamp = newTime
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(formatFriendlyDate(sharedTimestamp), color = Color(0xFF0F172A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Independent item editor cards
                        selectedConsumables.values.forEach { selected ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .shadow(2.dp, shape = RoundedCornerShape(16.dp))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(selected.displayName, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            Text("Category: ${selected.category}", color = Color(0xFF64748B), fontSize = 11.sp)
                                        }

                                        IconButton(onClick = { selectedConsumables.remove(selected.id) }) {
                                            Text("✕", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                    }

                                    HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 10.dp))

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Detailed form fields
                                    OutlinedTextField(
                                        value = selected.notes,
                                        onValueChange = { n -> selectedConsumables[selected.id] = selected.copy(notes = n) },
                                        placeholder = { Text("Notes (Optional)", fontSize = 12.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Individual Date Override Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Time Override:", color = Color(0xFF0F172A), fontSize = 11.sp, fontWeight = FontWeight.Bold)

                                        if (selected.timestamp.isNotEmpty()) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Button(
                                                    onClick = {
                                                        showPickerState = PickerConfig(selected.timestamp) { newTime ->
                                                            selectedConsumables[selected.id] = selected.copy(timestamp = newTime)
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Text(formatFriendlyDate(selected.timestamp), color = Color(0xFF0F172A), fontSize = 10.sp)
                                                }
                                                TextButton(
                                                    onClick = { selectedConsumables[selected.id] = selected.copy(timestamp = "") },
                                                    contentPadding = PaddingValues(0.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Text("Clear", color = Color(0xFFEF4444), fontSize = 11.sp)
                                                }
                                            }
                                        } else {
                                            Button(
                                                onClick = {
                                                    showPickerState = PickerConfig(sharedTimestamp) { newTime ->
                                                        selectedConsumables[selected.id] = selected.copy(timestamp = newTime)
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text("Override Shared Time", color = Color(0xFFF78325), fontSize = 10.sp)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Status Toggle
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { selectedConsumables[selected.id] = selected.copy(status = "Logged") },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (selected.status == "Logged") Color(0xFF509729) else Color(0xFFF1F5F9),
                                                contentColor = if (selected.status == "Logged") Color.White else Color(0xFF64748B)
                                            ),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(34.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("Logged", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = { selectedConsumables[selected.id] = selected.copy(status = "Trigger") },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (selected.status == "Trigger") Color(0xFFF78325) else Color(0xFFF1F5F9),
                                                contentColor = if (selected.status == "Trigger") Color.White else Color(0xFF64748B)
                                            ),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(34.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("Trigger", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Submit logs block
                        val valid = true
                        Button(
                            onClick = { logConsumptionBatch() },
                            enabled = valid && selectedConsumables.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (valid) Color(0xFFF78325) else Color(0xFFE2E8F0),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .height(50.dp)
                        ) {
                            Text("Confirm Log (${selectedConsumables.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
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

data class CategoryItem(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String,
    val color: Color
)

val CATEGORIES = listOf(
    CategoryItem("food", "Food", "Salad, Chocolate, Bread, Nuts...", "🥗", Color(0xFFFEF3C7)),
    CategoryItem("drinks", "Drinks", "Coffee, Milk, Alcohol, Juice...", "☕", Color(0xFFE0F2FE)),
    CategoryItem("medications", "Medications", "Paracetamol, Antihistamines, Cortisone...", "💊", Color(0xFFF3E8FF)),
    CategoryItem("supplements", "Supplements", "Vitamin D, Zinc, Omega-3...", "🧬", Color(0xFFDCFCE7)),
    CategoryItem("other", "Other", "Tobacco, Cosmetics, Household items...", "📦", Color(0xFFF1F5F9))
)

enum class AndroidScreenMode {
    MAIN,
    CATEGORY_LIST,
    SEE_ALL,
    DETAILS
}

@Composable
fun ArrowBackIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.7f, h * 0.3f)
            lineTo(w * 0.4f, h * 0.5f)
            lineTo(w * 0.7f, h * 0.7f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawLine(
            color = color,
            start = Offset(w * 0.4f, h * 0.5f),
            end = Offset(w * 0.75f, h * 0.5f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun SearchIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        drawCircle(
            color = Color(0xFF64748B),
            radius = w * 0.3f,
            center = Offset(w * 0.4f, h * 0.4f),
            style = Stroke(width = 2.dp.toPx())
        )
        drawLine(
            color = Color(0xFF64748B),
            start = Offset(w * 0.61f, h * 0.61f),
            end = Offset(w * 0.85f, h * 0.85f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun KeyboardArrowRightIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(16.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.35f, h * 0.3f)
            lineTo(w * 0.65f, h * 0.5f)
            lineTo(w * 0.35f, h * 0.7f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}
