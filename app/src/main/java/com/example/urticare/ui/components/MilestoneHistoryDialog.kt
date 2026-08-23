package com.example.urticare.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.urticare.model.LogEntry
import com.example.urticare.R
import com.example.urticare.ui.theme.*
import com.example.urticare.util.MilestoneEvaluator

@Composable
fun MilestoneHistoryDialog(
    entries: List<LogEntry>,
    onDismiss: () -> Unit
) {
    val pastMilestones = MilestoneEvaluator.getPastMilestones(entries)
    val brandGradient = Brush.horizontalGradient(listOf(Color(0xFF814B92), Color(0xFF509729), Color(0xFF1A7E97)))

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = Color(0xFF814B92),
                    spotColor = Color(0xFF1A7E97)
                )
                .border(
                    width = 1.5.dp,
                    brush = brandGradient,
                    shape = RoundedCornerShape(16.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Milestones History",
                        style = TextStyle(
                            brush = brandGradient,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Text(text = "❌", color = MutedGray, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (pastMilestones.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No milestone history recorded yet.\nKeep pacing! 💫",
                            color = MutedGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    val longestMilestone = pastMilestones.maxByOrNull { pm ->
                        if (pm.type == com.example.urticare.viewmodel.MilestoneType.ADHERENCE) pm.value else pm.value * 24
                    }

                    if (longestMilestone != null) {
                        val isAdherence = longestMilestone.type == com.example.urticare.viewmodel.MilestoneType.ADHERENCE
                        val days = if (isAdherence) longestMilestone.value / 24 else longestMilestone.value
                        val hours = if (isAdherence) longestMilestone.value % 24 else 0L

                        val durationStr = buildString {
                            append("$days Day${if (days != 1L) "s" else ""}")
                            if (hours > 0) {
                                append(" $hours Hour${if (hours != 1L) "s" else ""}")
                            }
                        }

                        Text(
                            text = "👑 Longest Streak: $durationStr",
                            color = Color(0xFFD48A00),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE5A93C).copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFE5A93C).copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = DarkBorder, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PAST STREAKS",
                                color = MutedGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pastMilestones.forEach { pm ->
                            MilestoneCard(pm = pm)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MilestoneCard(
    pm: MilestoneEvaluator.PastMilestone
) {
    val isAdherence = pm.type == com.example.urticare.viewmodel.MilestoneType.ADHERENCE
    val themeColor = if (isAdherence) Color(0xFF509729) else CoralPink
    val emoji = if (isAdherence) "🏆" else "💎"
    val durationText = if (isAdherence) {
        if (pm.value <= 48L) {
            "${pm.value} Hours Stable"
        } else {
            val days = pm.value / 24
            val rem = pm.value % 24
            if (rem > 0L) "$days Days $rem Hours Stable" else "$days Days Stable"
        }
    } else {
        "${pm.value} Days Remission"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkCard, shape = RoundedCornerShape(10.dp))
            .border(1.dp, DarkBorder, shape = RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Column
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .background(themeColor.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp))
        ) {
            if (isAdherence) {
                Image(
                    painter = painterResource(id = R.drawable.ic_trophy),
                    contentDescription = "Trophy Icon",
                    modifier = Modifier.size(20.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_gem),
                    contentDescription = "Gem Icon",
                    modifier = Modifier.size(22.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Details Column
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = durationText,
                color = themeColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Broken on: ${pm.dateCompletedStr}",
                color = MutedGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Reason: ${pm.reasonStopped}",
                color = LightGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
