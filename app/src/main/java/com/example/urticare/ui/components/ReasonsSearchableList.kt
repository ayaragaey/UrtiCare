package com.example.urticare.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.urticare.model.PotentialReasonsDirectory
import com.example.urticare.ui.theme.*
import com.example.urticare.viewmodel.TrackerViewModel

@Composable
fun ReasonsSearchableList(
    selectedReasons: SnapshotStateList<String>,
    vegetablesText: MutableState<String>,
    fruitsText: MutableState<String>,
    illnessText: MutableState<String>,
    medicineOtherNameText: MutableState<String>,
    medicineOtherCauseText: MutableState<String>,
    insectText: MutableState<String>,
    viewModel: TrackerViewModel,
    themeColor: Color = CoralPink,
    maxHeight: Int = 300,
    isLightModal: Boolean = true
) {
    var searchQuery by remember { mutableStateOf("") }
    val expandedCategories = remember { mutableStateMapOf<String, Boolean>() }

    val entries by viewModel.entries.collectAsState()

    // DRY Helpers for check state & toggle logic
    fun isReasonChecked(reason: String): Boolean {
        return when (reason) {
            "Illness" -> selectedReasons.contains("Illness") || selectedReasons.any { it.startsWith("Illness:") }
            "Vegetables: Other" -> selectedReasons.contains("Vegetables: Other") || selectedReasons.any { it.startsWith("Vegetables: Other:") }
            "Fruit: Other" -> selectedReasons.contains("Fruit: Other") || selectedReasons.any { it.startsWith("Fruit: Other:") }
            "Medicine : Other" -> selectedReasons.contains("Medicine : Other") || selectedReasons.any { it.startsWith("Medicine : Other:") }
            "Insect Bite/Sting" -> selectedReasons.contains("Insect Bite/Sting") || selectedReasons.any { it.startsWith("Insect Bite/Sting:") }
            else -> selectedReasons.contains(reason)
        }
    }

    fun toggleReason(reason: String) {
        val checked = isReasonChecked(reason)
        if (checked) {
            when (reason) {
                "Illness" -> selectedReasons.removeAll { it == "Illness" || it.startsWith("Illness:") }
                "Vegetables: Other" -> selectedReasons.removeAll { it == "Vegetables: Other" || it.startsWith("Vegetables: Other:") }
                "Fruit: Other" -> selectedReasons.removeAll { it == "Fruit: Other" || it.startsWith("Fruit: Other:") }
                "Medicine : Other" -> selectedReasons.removeAll { it == "Medicine : Other" || it.startsWith("Medicine : Other:") }
                "Insect Bite/Sting" -> selectedReasons.removeAll { it == "Insect Bite/Sting" || it.startsWith("Insect Bite/Sting:") }
                else -> selectedReasons.remove(reason)
            }
        } else {
            selectedReasons.add(reason)
        }
    }

    // Default common triggers list
    val defaultCommonTriggers = remember {
        listOf(
            "Idiopathic Flare-Up (Spontaneous)" to "Spontaneous",
            "Psychological stress" to "Stress",
            "Ambient heat and overheating (including hot rooms or hot, scalding showers)" to "Heat/Shower",
            "Sleep deprivation" to "Sleep Loss",
            "Exhaustion/Fatigue" to "Fatigue",
            "Missed Antihestamine Dosage" to "Missed Med",
            "Food Trigger: Spicy Food / Spices" to "Spicy Food",
            "The vicious \"stress-itch\" loop" to "Stress-Itch Loop"
        )
    }
    val defaultLabels = remember(defaultCommonTriggers) { defaultCommonTriggers.toMap() }

    // Helper to get friendly labels for display chips
    fun getFriendlyLabel(reason: String): String {
        val label = defaultLabels[reason]
        if (label != null) return label
        return reason.removePrefix("Food Trigger: ").removePrefix("Vegetables: ").removePrefix("Fruits: ").removePrefix("Fruit: ").take(20)
    }

    // Extract dynamic reasons sorted by count descending (frequency)
    val quickReasons = remember(entries) {
        val countMap = mutableMapOf<String, Int>()
        entries.filter { it.type == com.example.urticare.model.EntryType.FLARE_UP }.forEach { entry ->
            val metadataStr = entry.metadata ?: ""
            if (metadataStr.isNotEmpty()) {
                metadataStr.split("; ")
                    .filter {
                        it.trim().isNotEmpty() &&
                        it != "Severity: Very Severe" &&
                        it != "Severity: Severe" && 
                        it != "Severity: Mild" && 
                        it != "Severity: Moderate" && 
                        it != "Severity: Critical" && 
                        it != "Angioedema: Yes" && 
                        it != "Angioedema: No" && 
                        it != "Title: Angioedema" && 
                        !it.startsWith("Ongoing Medication:") &&
                        it != "Potentially caused by Autoimmune Progesterone Dermatitis (APD)" &&
                        it != "Possibly a pregnancy-associated flare-up"
                    }
                    .forEach { r ->
                        val cleanReason = when {
                            r.startsWith("Illness:") -> "Illness"
                            r.startsWith("Vegetables: Other:") || r.startsWith("Food Trigger: Vegetables:") -> "Vegetables: Other"
                            r.startsWith("Fruit: Other:") || r.startsWith("Food Trigger: Fruits:") -> "Fruit: Other"
                            r.startsWith("Medicine : Other:") -> "Medicine : Other"
                            r.startsWith("Insect Bite/Sting:") -> "Insect Bite/Sting"
                            else -> r
                        }
                        countMap[cleanReason] = (countMap[cleanReason] ?: 0) + 1
                    }
            }
        }

        // Sort dynamic reasons by count descending
        val sortedDynamic = countMap.entries
            .sortedByDescending { it.value }
            .map { it.key }

        val finalReasonsList = mutableListOf<Pair<String, String>>()
        sortedDynamic.forEach { reason ->
            finalReasonsList.add(reason to getFriendlyLabel(reason))
        }

        defaultCommonTriggers.forEach { (reason, label) ->
            if (finalReasonsList.none { it.first == reason }) {
                finalReasonsList.add(reason to label)
            }
        }

        finalReasonsList.take(8)
    }

    // Extract recents reasons (most recently selected, sorted by entry timestamp descending)
    val recentReasons = remember(entries) {
        val list = mutableListOf<String>()
        entries.filter { it.type == com.example.urticare.model.EntryType.FLARE_UP }.forEach { entry ->
            val metadataStr = entry.metadata ?: ""
            if (metadataStr.isNotEmpty()) {
                metadataStr.split("; ")
                    .filter {
                        it.trim().isNotEmpty() &&
                        it != "Severity: Very Severe" &&
                        it != "Severity: Severe" && 
                        it != "Severity: Mild" && 
                        it != "Severity: Moderate" && 
                        it != "Severity: Critical" && 
                        it != "Angioedema: Yes" && 
                        it != "Angioedema: No" && 
                        it != "Title: Angioedema" && 
                        !it.startsWith("Ongoing Medication:") &&
                        it != "Potentially caused by Autoimmune Progesterone Dermatitis (APD)" &&
                        it != "Possibly a pregnancy-associated flare-up"
                    }
                    .forEach { r ->
                        val cleanReason = when {
                            r.startsWith("Illness:") -> "Illness"
                            r.startsWith("Vegetables: Other:") || r.startsWith("Food Trigger: Vegetables:") -> "Vegetables: Other"
                            r.startsWith("Fruit: Other:") || r.startsWith("Food Trigger: Fruits:") -> "Fruit: Other"
                            r.startsWith("Medicine : Other:") -> "Medicine : Other"
                            r.startsWith("Insect Bite/Sting:") -> "Insect Bite/Sting"
                            else -> r
                        }
                        if (cleanReason.isNotEmpty() && !list.contains(cleanReason)) {
                            list.add(cleanReason)
                        }
                    }
            }
        }
        list.take(6)
    }

    val recentReasonsWithLabels = remember(recentReasons) {
        recentReasons.map { it to getFriendlyLabel(it) }
    }

    // Filter out recents from common triggers to prevent duplicate display space
    val commonReasons = remember(quickReasons, recentReasons) {
        quickReasons.filter { qr -> recentReasons.none { it == qr.first } }.take(6)
    }

    // Flatten all predefined reasons from PotentialReasonsDirectory
    val allPredefinedReasons = remember {
        PotentialReasonsDirectory.categories.values.flatten()
    }

    // Check if the current search query has an exact predefined match
    val isExactMatch = remember(searchQuery) {
        allPredefinedReasons.any { it.equals(searchQuery.trim(), ignoreCase = true) }
    }

    // Filter predefined reasons by search query
    val filteredResults = remember(searchQuery) {
        if (searchQuery.isEmpty()) {
            emptyList()
        } else {
            allPredefinedReasons.filter {
                it.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val cardBg = if (isLightModal) Color(0xFFF8FAFC) else DarkCard
    val cardBorder = if (isLightModal) Color(0xFFE2E8F0) else DarkBorder
    val itemTextColor = if (isLightModal) Color(0xFF1E293B) else Color(0xFF737373)
    val subheadColor = if (isLightModal) Color(0xFF64748B) else MutedGray
    val chipUnselectedBg = if (isLightModal) Color(0xFFF1F5F9) else Color.Transparent
    val chipUnselectedBorder = if (isLightModal) Color(0xFFCBD5E1) else DarkBorder
    val chipUnselectedText = if (isLightModal) Color(0xFF334155) else LightGray
    val inputBg = if (isLightModal) Color.White else DarkCard

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Search Field with clear action
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search Reasons...", color = subheadColor, fontSize = 12.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = {
                Text(
                    text = "🔍",
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Text(
                            text = "✕",
                            color = subheadColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = itemTextColor,
                unfocusedTextColor = itemTextColor,
                focusedBorderColor = themeColor,
                unfocusedBorderColor = cardBorder,
                focusedContainerColor = inputBg,
                unfocusedContainerColor = inputBg
            )
        )

        // Selected Reasons Section (with filled chips and delete buttons)
        if (selectedReasons.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "SELECTED REASONS (${selectedReasons.size})",
                    color = themeColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    selectedReasons.forEach { reason ->
                        Box(
                            modifier = Modifier
                                .background(
                                    color = themeColor,
                                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
                                )
                                .clickable {
                                    // Remove selected reason cleanly
                                    if (reason.startsWith("Illness:")) {
                                        selectedReasons.remove(reason)
                                        selectedReasons.removeAll { it == "Illness" || it.startsWith("Illness:") }
                                    } else if (reason.startsWith("Vegetables: Other:")) {
                                        selectedReasons.remove(reason)
                                        selectedReasons.removeAll { it == "Vegetables: Other" || it.startsWith("Vegetables: Other:") }
                                    } else if (reason.startsWith("Fruit: Other:")) {
                                        selectedReasons.remove(reason)
                                        selectedReasons.removeAll { it == "Fruit: Other" || it.startsWith("Fruit: Other:") }
                                    } else if (reason.startsWith("Medicine : Other:")) {
                                        selectedReasons.remove(reason)
                                        selectedReasons.removeAll { it == "Medicine : Other" || it.startsWith("Medicine : Other:") }
                                    } else if (reason.startsWith("Insect Bite/Sting:")) {
                                        selectedReasons.remove(reason)
                                        selectedReasons.removeAll { it == "Insect Bite/Sting" || it.startsWith("Insect Bite/Sting:") }
                                    } else {
                                        selectedReasons.remove(reason)
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = reason,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "✕",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Centralized input fields for special reasons
        val showIllnessField = isReasonChecked("Illness")
        val showVegetableField = isReasonChecked("Vegetables: Other")
        val showFruitField = isReasonChecked("Fruit: Other")
        val showMedicineField = isReasonChecked("Medicine : Other")
        val showInsectField = isReasonChecked("Insect Bite/Sting")

        if (showIllnessField || showVegetableField || showFruitField || showMedicineField || showInsectField) {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, cardBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "ADDITIONAL DETAILS",
                        color = themeColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    if (showIllnessField) {
                        OutlinedTextField(
                            value = illnessText.value,
                            onValueChange = { illnessText.value = it },
                            label = { Text("Specify illness", color = subheadColor, fontSize = 11.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = itemTextColor,
                                unfocusedTextColor = itemTextColor,
                                focusedBorderColor = themeColor,
                                unfocusedBorderColor = cardBorder,
                                focusedContainerColor = inputBg,
                                unfocusedContainerColor = inputBg
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (showVegetableField) {
                        OutlinedTextField(
                            value = vegetablesText.value,
                            onValueChange = { vegetablesText.value = it },
                            label = { Text("Specify vegetable", color = subheadColor, fontSize = 11.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = itemTextColor,
                                unfocusedTextColor = itemTextColor,
                                focusedBorderColor = themeColor,
                                unfocusedBorderColor = cardBorder,
                                focusedContainerColor = inputBg,
                                unfocusedContainerColor = inputBg
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (showFruitField) {
                        OutlinedTextField(
                            value = fruitsText.value,
                            onValueChange = { fruitsText.value = it },
                            label = { Text("Specify fruit", color = subheadColor, fontSize = 11.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = itemTextColor,
                                unfocusedTextColor = itemTextColor,
                                focusedBorderColor = themeColor,
                                unfocusedBorderColor = cardBorder,
                                focusedContainerColor = inputBg,
                                unfocusedContainerColor = inputBg
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (showMedicineField) {
                        OutlinedTextField(
                            value = medicineOtherNameText.value,
                            onValueChange = { medicineOtherNameText.value = it },
                            label = { Text("Specify medicine name", color = subheadColor, fontSize = 11.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = itemTextColor,
                                unfocusedTextColor = itemTextColor,
                                focusedBorderColor = themeColor,
                                unfocusedBorderColor = cardBorder,
                                focusedContainerColor = inputBg,
                                unfocusedContainerColor = inputBg
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = medicineOtherCauseText.value,
                            onValueChange = { medicineOtherCauseText.value = it },
                            label = { Text("Cause", color = subheadColor, fontSize = 11.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = itemTextColor,
                                unfocusedTextColor = itemTextColor,
                                focusedBorderColor = themeColor,
                                unfocusedBorderColor = cardBorder,
                                focusedContainerColor = inputBg,
                                unfocusedContainerColor = inputBg
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (showInsectField) {
                        OutlinedTextField(
                            value = insectText.value,
                            onValueChange = { insectText.value = it },
                            label = { Text("Specify insect", color = subheadColor, fontSize = 11.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = itemTextColor,
                                unfocusedTextColor = itemTextColor,
                                focusedBorderColor = themeColor,
                                unfocusedBorderColor = cardBorder,
                                focusedContainerColor = inputBg,
                                unfocusedContainerColor = inputBg
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Search Results OR Category Accordion List
        if (searchQuery.isNotEmpty()) {
            // Flat Search Suggestions List
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardBg, shape = RoundedCornerShape(12.dp))
                    .border(1.dp, cardBorder, shape = RoundedCornerShape(12.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val trimmedQuery = searchQuery.trim()

                // Add Custom option at the top of results if it is not an exact match
                if (trimmedQuery.isNotEmpty() && !isExactMatch) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (!selectedReasons.contains(trimmedQuery)) {
                                    selectedReasons.add(trimmedQuery)
                                }
                                searchQuery = ""
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "➕ Add custom: \"$trimmedQuery\"",
                            color = themeColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HorizontalDivider(color = cardBorder, thickness = 1.dp)
                }

                if (filteredResults.isEmpty() && (isExactMatch || trimmedQuery.isEmpty())) {
                    Text(
                        text = "No other matches found",
                        color = subheadColor,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                } else {
                    filteredResults.forEach { reason ->
                        val isChecked = isReasonChecked(reason)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { toggleReason(reason) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = themeColor,
                                        uncheckedColor = if (isLightModal) Color(0xFF94A3B8) else MutedGray,
                                        checkmarkColor = Color.White
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = reason,
                                    color = itemTextColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }

                            // Auto-type button (copies suggestion back into search query text field)
                            IconButton(
                                onClick = { searchQuery = reason },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text(
                                    text = "↗",
                                    color = themeColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Recents row (only if searchQuery is empty)
            if (recentReasonsWithLabels.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "RECENTS",
                        color = subheadColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        recentReasonsWithLabels.forEach { (reason, label) ->
                            val isSelected = isReasonChecked(reason)
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isSelected) themeColor.copy(alpha = 0.15f) else chipUnselectedBg,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) themeColor else chipUnselectedBorder,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { toggleReason(reason) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("🕒", fontSize = 10.sp)
                                    if (isSelected) {
                                        Text("✓", color = themeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        text = label,
                                        color = if (isSelected) themeColor else chipUnselectedText,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Common triggers row (only if searchQuery is empty)
            if (commonReasons.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "COMMON TRIGGERS",
                        color = subheadColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        commonReasons.forEach { (reason, label) ->
                            val isSelected = isReasonChecked(reason)
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isSelected) themeColor.copy(alpha = 0.15f) else chipUnselectedBg,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) themeColor else chipUnselectedBorder,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { toggleReason(reason) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isSelected) {
                                        Text("✓", color = themeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        text = label,
                                        color = if (isSelected) themeColor else chipUnselectedText,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Standard Categories List (Accordion cards)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PotentialReasonsDirectory.categories.forEach { (category, reasons) ->
                    val filteredReasons = reasons.filter {
                        it.contains(searchQuery, ignoreCase = true) || category.contains(searchQuery, ignoreCase = true)
                    }

                    if (filteredReasons.isNotEmpty()) {
                        val isExpanded = expandedCategories[category] ?: (searchQuery.isNotEmpty())

                        Card(
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            border = BorderStroke(1.dp, cardBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            expandedCategories[category] = !isExpanded
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = category.uppercase(),
                                        color = themeColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isExpanded) "▼" else "▶",
                                        color = themeColor,
                                        fontSize = 10.sp
                                    )
                                }

                                if (isExpanded) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        filteredReasons.forEach { reason ->
                                            val isChecked = isReasonChecked(reason)

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { toggleReason(reason) }
                                                    .padding(vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = isChecked,
                                                    onCheckedChange = null,
                                                    colors = CheckboxDefaults.colors(
                                                        checkedColor = themeColor,
                                                        uncheckedColor = if (isLightModal) Color(0xFF94A3B8) else MutedGray,
                                                        checkmarkColor = Color.White
                                                    )
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = reason,
                                                    color = itemTextColor,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Normal
                                                )
                                            }

                                            // Specific Input Boxes (Fallback inline support)
                                            if (reason == "Illness" && isChecked) {
                                                OutlinedTextField(
                                                    value = illnessText.value,
                                                    onValueChange = { illnessText.value = it },
                                                    label = { Text("Specify illness", color = subheadColor, fontSize = 11.sp) },
                                                    singleLine = true,
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedTextColor = itemTextColor,
                                                        unfocusedTextColor = itemTextColor,
                                                        focusedBorderColor = themeColor,
                                                        unfocusedBorderColor = cardBorder,
                                                        focusedContainerColor = inputBg,
                                                        unfocusedContainerColor = inputBg
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(start = 32.dp, bottom = 4.dp)
                                                )
                                            }

                                            if (reason == "Vegetables: Other" && isChecked) {
                                                OutlinedTextField(
                                                    value = vegetablesText.value,
                                                    onValueChange = { vegetablesText.value = it },
                                                    label = { Text("Specify vegetable", color = subheadColor, fontSize = 11.sp) },
                                                    singleLine = true,
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedTextColor = itemTextColor,
                                                        unfocusedTextColor = itemTextColor,
                                                        focusedBorderColor = themeColor,
                                                        unfocusedBorderColor = cardBorder,
                                                        focusedContainerColor = inputBg,
                                                        unfocusedContainerColor = inputBg
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(start = 32.dp, bottom = 4.dp)
                                                )
                                            }

                                            if (reason == "Fruit: Other" && isChecked) {
                                                OutlinedTextField(
                                                    value = fruitsText.value,
                                                    onValueChange = { fruitsText.value = it },
                                                    label = { Text("Specify fruit", color = subheadColor, fontSize = 11.sp) },
                                                    singleLine = true,
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedTextColor = itemTextColor,
                                                        unfocusedTextColor = itemTextColor,
                                                        focusedBorderColor = themeColor,
                                                        unfocusedBorderColor = cardBorder,
                                                        focusedContainerColor = inputBg,
                                                        unfocusedContainerColor = inputBg
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(start = 32.dp, bottom = 4.dp)
                                                )
                                            }

                                            if (reason == "Medicine : Other" && isChecked) {
                                                OutlinedTextField(
                                                    value = medicineOtherNameText.value,
                                                    onValueChange = { medicineOtherNameText.value = it },
                                                    label = { Text("Specify medicine name", color = subheadColor, fontSize = 11.sp) },
                                                    singleLine = true,
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedTextColor = itemTextColor,
                                                        unfocusedTextColor = itemTextColor,
                                                        focusedBorderColor = themeColor,
                                                        unfocusedBorderColor = cardBorder,
                                                        focusedContainerColor = inputBg,
                                                        unfocusedContainerColor = inputBg
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(start = 32.dp, bottom = 4.dp)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                OutlinedTextField(
                                                    value = medicineOtherCauseText.value,
                                                    onValueChange = { medicineOtherCauseText.value = it },
                                                    label = { Text("Cause", color = subheadColor, fontSize = 11.sp) },
                                                    singleLine = true,
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedTextColor = itemTextColor,
                                                        unfocusedTextColor = itemTextColor,
                                                        focusedBorderColor = themeColor,
                                                        unfocusedBorderColor = cardBorder,
                                                        focusedContainerColor = inputBg,
                                                        unfocusedContainerColor = inputBg
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(start = 32.dp, bottom = 4.dp)
                                                )
                                            }

                                            if (reason == "Insect Bite/Sting" && isChecked) {
                                                OutlinedTextField(
                                                    value = insectText.value,
                                                    onValueChange = { insectText.value = it },
                                                    label = { Text("Specify insect", color = subheadColor, fontSize = 11.sp) },
                                                    singleLine = true,
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedTextColor = itemTextColor,
                                                        unfocusedTextColor = itemTextColor,
                                                        focusedBorderColor = themeColor,
                                                        unfocusedBorderColor = cardBorder,
                                                        focusedContainerColor = inputBg,
                                                        unfocusedContainerColor = inputBg
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(start = 32.dp, bottom = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
