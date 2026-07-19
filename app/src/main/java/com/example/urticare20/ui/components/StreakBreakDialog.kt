package com.example.urticare20.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.urticare20.ui.theme.*
import com.example.urticare20.viewmodel.TrackerViewModel

@Composable
fun StreakBreakDialog(viewModel: TrackerViewModel) {
    val breakInfo = viewModel.pendingStreakBreak.value ?: return

    val selectedReasons = remember { mutableStateListOf<String>() }
    var isOtherSelected by remember { mutableStateOf(false) }
    var otherText by remember { mutableStateOf("") }
    val vegetablesText = remember { mutableStateOf("") }
    val fruitsText = remember { mutableStateOf("") }
    val illnessText = remember { mutableStateOf("") }
    val medicineOtherNameText = remember { mutableStateOf("") }
    val medicineOtherCauseText = remember { mutableStateOf("") }
    val insectText = remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { viewModel.pendingStreakBreak.value = null },
        containerColor = DarkSurface,
        title = {
            Text(
                text = "Milestone Streak Broken 😢",
                color = Color(0xFF1A7E97),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Your ${breakInfo.streakText} was broken. Don't sweat it, the progress still counts.\n\nWhat potentially caused this?",
                    color = LightGray,
                    fontSize = 13.sp
                )

                Text(
                    text = "Potential Reasons",
                    color = Color(0xFF1A7E97),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )

                ReasonsSearchableList(
                    selectedReasons = selectedReasons,
                    vegetablesText = vegetablesText,
                    fruitsText = fruitsText,
                    illnessText = illnessText,
                    medicineOtherNameText = medicineOtherNameText,
                    medicineOtherCauseText = medicineOtherCauseText,
                    insectText = insectText,
                    viewModel = viewModel,
                    themeColor = Color(0xFF1A7E97),
                    maxHeight = 300
                )

                // Other option checkbox & textfield
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isOtherSelected = !isOtherSelected }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isOtherSelected,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF1A7E97),
                            uncheckedColor = MutedGray,
                            checkmarkColor = AmoledBlack
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Other",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (isOtherSelected) {
                    OutlinedTextField(
                        value = otherText,
                        onValueChange = { otherText = it },
                        label = { Text("Specify other reason", color = MutedGray, fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF1A7E97),
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
                    val finalReasons = mutableListOf<String>()
                    selectedReasons.forEach { r ->
                        when (r) {
                            "Vegetables: Other" -> {
                                if (vegetablesText.value.trim().isNotEmpty()) {
                                    finalReasons.add("Vegetables: Other: ${vegetablesText.value.trim()}")
                                } else {
                                    finalReasons.add("Vegetables: Other")
                                }
                            }
                            "Fruit: Other" -> {
                                if (fruitsText.value.trim().isNotEmpty()) {
                                    finalReasons.add("Fruit: Other: ${fruitsText.value.trim()}")
                                } else {
                                    finalReasons.add("Fruit: Other")
                                }
                            }
                            "Illness" -> {
                                if (illnessText.value.trim().isNotEmpty()) {
                                    finalReasons.add("Illness: ${illnessText.value.trim()}")
                                } else {
                                    finalReasons.add("Illness")
                                }
                            }
                            "Medicine : Other" -> {
                                val name = medicineOtherNameText.value.trim()
                                val cause = medicineOtherCauseText.value.trim()
                                if (name.isNotEmpty()) {
                                    val causePart = if (cause.isNotEmpty()) " (Cause: $cause)" else ""
                                    finalReasons.add("Medicine : Other: $name$causePart")
                                } else {
                                    finalReasons.add("Medicine : Other")
                                }
                            }
                            "Insect Bite/Sting" -> {
                                val insect = insectText.value.trim()
                                if (insect.isNotEmpty()) {
                                    finalReasons.add("Insect Bite/Sting: $insect")
                                } else {
                                    finalReasons.add("Insect Bite/Sting")
                                }
                            }
                            else -> finalReasons.add(r)
                        }
                    }
                    if (isOtherSelected && otherText.trim().isNotEmpty()) {
                        finalReasons.add("Other: ${otherText.trim()}")
                    }

                    val reasonsStr = if (finalReasons.isNotEmpty()) finalReasons.joinToString("; ") else ""
                    
                    val finalMetadata = if (breakInfo.type == com.example.urticare20.model.EntryType.FLARE_UP) {
                        val severityStr = if (breakInfo.metadata?.contains("Severity: Severe") == true) "Severity: Severe"
                                           else if (breakInfo.metadata?.contains("Severity: Mild") == true) "Severity: Mild"
                                           else "Severity: Mild"
                        
                        val cleanOrig = (breakInfo.metadata ?: "")
                            .replace("Severity: Severe", "").replace("Angioedema: Yes", "").replace("Angioedema: No", "")
                            .replace("Severity: Mild", "")
                            .trim()
                            .removePrefix(";")
                            .removeSuffix(";")
                            .trim()
                        
                        val mergedReasons = mutableListOf<String>()
                        if (cleanOrig.isNotEmpty()) {
                            mergedReasons.addAll(cleanOrig.split("; ").map { it.trim() })
                        }
                        finalReasons.forEach { r ->
                            if (!mergedReasons.contains(r)) {
                                mergedReasons.add(r)
                            }
                        }
                        val mergedStr = if (mergedReasons.isNotEmpty()) mergedReasons.joinToString("; ") else ""
                        if (mergedStr.isNotEmpty()) "$severityStr; $mergedStr" else severityStr
                    } else {
                        val streakBreakReasonStr = if (reasonsStr.isNotEmpty()) "Streak Break Reason: $reasonsStr" else "Streak Break Reason: Unspecified"
                        if (breakInfo.metadata.isNullOrEmpty()) {
                            streakBreakReasonStr
                        } else {
                            "${breakInfo.metadata}:::${streakBreakReasonStr}"
                        }
                    }

                    viewModel.addEntry(breakInfo.type, breakInfo.timestamp, finalMetadata, force = true)
                    viewModel.pendingStreakBreak.value = null
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1A7E97),
                    contentColor = AmoledBlack
                )
            ) {
                Text(text = "Submit", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        // Cancel closes the dialog and aborts/discards the log entry
                        viewModel.pendingStreakBreak.value = null
                    }
                ) {
                    Text(text = "Cancel", color = MutedGray)
                }
                TextButton(
                    onClick = {
                        // Skip logs the entry without any streak break reasons
                        viewModel.addEntry(breakInfo.type, breakInfo.timestamp, breakInfo.metadata, force = true)
                        viewModel.pendingStreakBreak.value = null
                    }
                ) {
                    Text(text = "Skip", color = Color(0xFF1A7E97), fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}
