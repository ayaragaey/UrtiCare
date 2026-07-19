package com.example.urticare20.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.urticare20.model.PotentialReasonsDirectory
import com.example.urticare20.ui.theme.*
import com.example.urticare20.viewmodel.TrackerViewModel

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
    maxHeight: Int = 300
) {
    var searchQuery by remember { mutableStateOf("") }
    val expandedCategories = remember { mutableStateMapOf<String, Boolean>() }

    val entries by viewModel.entries.collectAsState()

    val quickReasons = remember(entries) {
        val countMap = mutableMapOf<String, Int>()
        entries.filter { it.type == com.example.urticare20.model.EntryType.FLARE_UP }.forEach { entry ->
            val metadataStr = entry.metadata ?: ""
            if (metadataStr.isNotEmpty()) {
                metadataStr.split("; ")
                    .filter {
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

        val defaultCommonTriggers = listOf(
            "Idiopathic Flare-Up (Spontaneous)" to "Spontaneous",
            "Psychological stress" to "Stress",
            "Ambient heat and overheating (including hot rooms or hot, scalding showers)" to "Heat/Shower",
            "Sleep deprivation" to "Sleep Loss",
            "Exhaustion/Fatigue" to "Fatigue",
            "Missed Antihestamine Dosage" to "Missed Med",
            "Food Trigger: Spicy Food / Spices" to "Spicy Food",
            "The vicious \"stress-itch\" loop" to "Stress-Itch Loop"
        )

        // Sort dynamic reasons by count descending
        val sortedDynamic = countMap.entries
            .sortedByDescending { it.value }
            .map { it.key }

        val defaultLabels = defaultCommonTriggers.toMap()

        fun getFriendlyLabel(reason: String): String {
            val label = defaultLabels[reason]
            if (label != null) return label
            return reason.removePrefix("Food Trigger: ").removePrefix("Vegetables: ").removePrefix("Fruits: ").removePrefix("Fruit: ").take(20)
        }

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

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search Reasons...", color = MutedGray, fontSize = 12.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Text(
                    text = "🔍",
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color(0xFF737373),
                unfocusedTextColor = Color(0xFF737373),
                focusedBorderColor = themeColor,
                unfocusedBorderColor = DarkBorder,
                focusedContainerColor = DarkCard,
                unfocusedContainerColor = DarkCard
            )
        )

        // Quick Selection
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "QUICK SELECTION",
                color = MutedGray,
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
                quickReasons.forEach { (reason, label) ->
                    val isSelected = selectedReasons.contains(reason)
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isSelected) themeColor.copy(alpha = 0.15f) else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) themeColor else DarkBorder,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                if (isSelected) {
                                    selectedReasons.remove(reason)
                                } else {
                                    selectedReasons.add(reason)
                                }
                            }
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
                                color = if (isSelected) themeColor else Color(0xFF737373),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Categories List (Clean styled Accordion cards)
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
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        border = BorderStroke(1.dp, DarkBorder),
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
                                        val isChecked = when (reason) {
                                            "Illness" -> selectedReasons.contains("Illness") || selectedReasons.any { it.startsWith("Illness:") }
                                            "Vegetables: Other" -> selectedReasons.contains("Vegetables: Other") || selectedReasons.any { it.startsWith("Vegetables: Other:") }
                                            "Fruit: Other" -> selectedReasons.contains("Fruit: Other") || selectedReasons.any { it.startsWith("Fruit: Other:") }
                                            "Medicine : Other" -> selectedReasons.contains("Medicine : Other") || selectedReasons.any { it.startsWith("Medicine : Other:") }
                                            "Insect Bite/Sting" -> selectedReasons.contains("Insect Bite/Sting") || selectedReasons.any { it.startsWith("Insect Bite/Sting:") }
                                            else -> selectedReasons.contains(reason)
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (isChecked) {
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
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = null,
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = themeColor,
                                                    uncheckedColor = MutedGray,
                                                    checkmarkColor = AmoledBlack
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = reason,
                                                color = Color(0xFF737373),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        // Specific Input Boxes
                                        if (reason == "Illness" && isChecked) {
                                            OutlinedTextField(
                                                value = illnessText.value,
                                                onValueChange = { illnessText.value = it },
                                                label = { Text("Specify illness", color = MutedGray, fontSize = 11.sp) },
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = Color(0xFF737373),
                                                    unfocusedTextColor = Color(0xFF737373),
                                                    focusedBorderColor = themeColor,
                                                    unfocusedBorderColor = DarkBorder,
                                                    focusedContainerColor = DarkCard,
                                                    unfocusedContainerColor = DarkCard
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
                                                label = { Text("Specify vegetable", color = MutedGray, fontSize = 11.sp) },
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = Color(0xFF737373),
                                                    unfocusedTextColor = Color(0xFF737373),
                                                    focusedBorderColor = themeColor,
                                                    unfocusedBorderColor = DarkBorder,
                                                    focusedContainerColor = DarkCard,
                                                    unfocusedContainerColor = DarkCard
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
                                                label = { Text("Specify fruit", color = MutedGray, fontSize = 11.sp) },
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = Color(0xFF737373),
                                                    unfocusedTextColor = Color(0xFF737373),
                                                    focusedBorderColor = themeColor,
                                                    unfocusedBorderColor = DarkBorder,
                                                    focusedContainerColor = DarkCard,
                                                    unfocusedContainerColor = DarkCard
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
                                                label = { Text("Specify medicine name", color = MutedGray, fontSize = 11.sp) },
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = Color(0xFF737373),
                                                    unfocusedTextColor = Color(0xFF737373),
                                                    focusedBorderColor = themeColor,
                                                    unfocusedBorderColor = DarkBorder,
                                                    focusedContainerColor = DarkCard,
                                                    unfocusedContainerColor = DarkCard
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(start = 32.dp, bottom = 4.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            OutlinedTextField(
                                                value = medicineOtherCauseText.value,
                                                onValueChange = { medicineOtherCauseText.value = it },
                                                label = { Text("Cause", color = MutedGray, fontSize = 11.sp) },
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = Color(0xFF737373),
                                                    unfocusedTextColor = Color(0xFF737373),
                                                    focusedBorderColor = themeColor,
                                                    unfocusedBorderColor = DarkBorder,
                                                    focusedContainerColor = DarkCard,
                                                    unfocusedContainerColor = DarkCard
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
                                                label = { Text("Specify insect", color = MutedGray, fontSize = 11.sp) },
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = Color(0xFF737373),
                                                    unfocusedTextColor = Color(0xFF737373),
                                                    focusedBorderColor = themeColor,
                                                    unfocusedBorderColor = DarkBorder,
                                                    focusedContainerColor = DarkCard,
                                                    unfocusedContainerColor = DarkCard
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
