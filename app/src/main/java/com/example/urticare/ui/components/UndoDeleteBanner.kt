package com.example.urticare.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.urticare.model.EntryType
import com.example.urticare.viewmodel.AppDeletedItem
import com.example.urticare.viewmodel.TrackerViewModel
import com.example.urticare.ui.theme.*

@Composable
fun AppUndoDeleteBanner(viewModel: TrackerViewModel, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        UndoDeleteBanner(viewModel = viewModel)
    }
}

@Composable
fun BoxScope.UndoDeleteBanner(viewModel: TrackerViewModel) {
    val lastDeleted = viewModel.lastDeletedItem.value

    AnimatedVisibility(
        visible = lastDeleted != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (lastDeleted != null) {
            val details = when (lastDeleted) {
                is AppDeletedItem.SingleEntry -> {
                    val entry = lastDeleted.entry
                    when (entry.type) {
                        EntryType.FLARE_UP -> {
                            val sev = when {
                                entry.metadata?.contains("Severity: Critical") == true -> "Critical"
                                entry.metadata?.contains("Severity: Very Severe") == true -> "Very Severe"
                                entry.metadata?.contains("Severity: Severe") == true -> "Severe"
                                entry.metadata?.contains("Severity: Moderate") == true -> "Moderate"
                                else -> "Mild"
                            }
                            "Symptom Flare-up ($sev) deleted"
                        }
                        EntryType.ANTIHISTAMINE, EntryType.CORTISONE -> {
                            val metadata = entry.metadata
                            if (!metadata.isNullOrEmpty()) {
                                val parts = metadata.split(":::")
                                val name = if (parts.isNotEmpty()) parts[0] else if (entry.type == EntryType.CORTISONE) "Cortisone" else "Antihistamine"
                                val mg = if (parts.size >= 2) " (${parts[1]} mg)" else ""
                                "Log: $name$mg deleted"
                            } else {
                                if (entry.type == EntryType.CORTISONE) "Cortisone log deleted" else "Antihistamine log deleted"
                            }
                        }
                        EntryType.ALTERNATIVE -> {
                            val metadata = entry.metadata
                            if (!metadata.isNullOrEmpty()) {
                                val parts = metadata.split(":::")
                                val name = if (parts.isNotEmpty()) parts[0] else "Alternative"
                                "Log: $name deleted"
                            } else {
                                "Alternative med log deleted"
                            }
                        }
                        EntryType.XOLAIR_150 -> "Log: Xolair 150 mg deleted"
                        EntryType.XOLAIR_300 -> "Log: Xolair 300 mg deleted"
                        EntryType.CONSUMPTION -> {
                            val parts = entry.metadata?.split(":::")
                            val name = if (parts != null && parts.size >= 2 && parts[0] == "Consumption") parts[1] else "Consumption"
                            "Log: $name deleted"
                        }
                    }
                }
                is AppDeletedItem.MultipleEntries -> "Multiple logs cleared"
                is AppDeletedItem.Menstruation -> "Menstruation cycle (${lastDeleted.cycle.startDate}) deleted"
                is AppDeletedItem.Antihistamine -> "Antihistamine profile (${lastDeleted.med.name}) deleted"
                is AppDeletedItem.Cortisone -> "Cortisone profile (${lastDeleted.med.name}) deleted"
                is AppDeletedItem.ChronicIllness -> "Chronic illness (${lastDeleted.name}) deleted"
                is AppDeletedItem.ChatArchive -> "Chat archive (${lastDeleted.session.title}) deleted"
                is AppDeletedItem.OtherMed -> "Other medication (${lastDeleted.med.name}) deleted"
                is AppDeletedItem.Allergy -> "Allergy (${lastDeleted.name}) deleted"
            }

            val themeColor = when (lastDeleted) {
                is AppDeletedItem.SingleEntry -> {
                    when (lastDeleted.entry.type) {
                        EntryType.FLARE_UP -> CoralPink
                        EntryType.ANTIHISTAMINE -> SoftYellow
                        EntryType.CORTISONE -> PastelIceBlue
                        else -> SoftPurple
                    }
                }
                is AppDeletedItem.Menstruation -> CoralPink
                is AppDeletedItem.Antihistamine -> SoftYellow
                is AppDeletedItem.Cortisone -> PastelIceBlue
                is AppDeletedItem.OtherMed -> SoftPurple
                else -> SoftPurple
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, themeColor, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = details,
                        color = LightGray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "UNDO",
                            color = PastelIceBlue,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { viewModel.undoDelete() }
                        )

                        Text(
                            text = "❌",
                            color = MutedGray,
                            fontSize = 12.sp,
                            modifier = Modifier.clickable { viewModel.lastDeletedItem.value = null }
                        )
                    }
                }
            }
        }
    }
}
