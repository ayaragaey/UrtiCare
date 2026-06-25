package com.example.urticare20.ui.components

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
                        it != "Angioedema: Yes" && 
                        it != "Angioedema: No" && 
                        it != "Severity: Mild" && 
                        !it.startsWith("Ongoing Medication:") &&
                        it != "Potentially caused by Autoimmune Progesterone Dermatitis (APD)" &&
                        it != "Possibly a pregnancy-associated flare-up"
                    }
                    .forEach { r ->
                        val cleanReason = when {
                            r.startsWith("Illness:") -> "Illness"
                            r.startsWith("Vegetables: Other:") || r.startsWith("Food Trigger: Vegetables:") -> "Vegetables: Other"
                            r.startsWith("Fruit: Other:") || r.startsWith("Food Trigger: Fruits:") -> "Fruit: Other"
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
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Quick Selection
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

        Spacer(modifier = Modifier.height(4.dp))

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search Reasons...", color = MutedGray, fontSize = 12.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color(0xFF737373),
                unfocusedTextColor = Color(0xFF737373),
                focusedBorderColor = themeColor,
                unfocusedBorderColor = DarkBorder,
                focusedContainerColor = DarkCard,
                unfocusedContainerColor = DarkCard
            )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PotentialReasonsDirectory.categories.forEach { (category, reasons) ->
                val filteredReasons = reasons.filter { 
                    it.contains(searchQuery, ignoreCase = true) || category.contains(searchQuery, ignoreCase = true)
                }

                if (filteredReasons.isNotEmpty()) {
                    val isExpanded = expandedCategories[category] ?: (searchQuery.isNotEmpty())

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedCategories[category] = !isExpanded
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = category.uppercase(),
                            color = themeColor,
                            fontSize = 10.sp,
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
                                .padding(start = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            filteredReasons.forEach { reason ->
                                val isChecked = when (reason) {
                                    "Illness" -> selectedReasons.contains("Illness") || selectedReasons.any { it.startsWith("Illness:") }
                                    "Vegetables: Other" -> selectedReasons.contains("Vegetables: Other") || selectedReasons.any { it.startsWith("Vegetables: Other:") }
                                    "Fruit: Other" -> selectedReasons.contains("Fruit: Other") || selectedReasons.any { it.startsWith("Fruit: Other:") }
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
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                selectedReasons.add(reason)
                                            } else {
                                                when (reason) {
                                                    "Illness" -> selectedReasons.removeAll { it == "Illness" || it.startsWith("Illness:") }
                                                    "Vegetables: Other" -> selectedReasons.removeAll { it == "Vegetables: Other" || it.startsWith("Vegetables: Other:") }
                                                    "Fruit: Other" -> selectedReasons.removeAll { it == "Fruit: Other" || it.startsWith("Fruit: Other:") }
                                                    else -> selectedReasons.remove(reason)
                                                }
                                            }
                                        },
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
                            }
                        }
                    }
                }
            }
        }
    }
}
