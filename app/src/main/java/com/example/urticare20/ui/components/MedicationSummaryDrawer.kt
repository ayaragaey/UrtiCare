package com.example.urticare20.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.urticare20.model.EntryType
import com.example.urticare20.ui.theme.*
import com.example.urticare20.viewmodel.TrackerViewModel
import java.time.ZonedDateTime

private val MONTH_NAMES = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
)

@Composable
fun MedicationSummaryDrawer(viewModel: TrackerViewModel, entries: List<com.example.urticare20.model.LogEntry>) {
    var isOpen by remember { mutableStateOf(true) } // default expanded for stunning landing views

    val navDate = viewModel.navigatedDate.value
    val monthName = MONTH_NAMES[navDate.monthValue - 1]
    val targetYear = navDate.year

    // Fetch lookback counts dynamically
    val ah24 = viewModel.getMovingWindowCount(EntryType.ANTIHISTAMINE, 24)
    val ah48 = viewModel.getMovingWindowCount(EntryType.ANTIHISTAMINE, 48)
    val ah72 = viewModel.getMovingWindowCount(EntryType.ANTIHISTAMINE, 72)

    // Fetch monthly counts reactively based on navigated month
    val targetMonth = navDate.monthValue

    val monthlyAH = remember(entries, targetYear, targetMonth) {
        entries.count { entry ->
            entry.type == EntryType.ANTIHISTAMINE && try {
                val t = ZonedDateTime.parse(entry.timestamp)
                t.year == targetYear && t.monthValue == targetMonth
            } catch (e: Exception) { false }
        }
    }

    val monthlyCortisone = remember(entries, targetYear, targetMonth) {
        entries.count { entry ->
            entry.type == EntryType.CORTISONE && try {
                val t = ZonedDateTime.parse(entry.timestamp)
                t.year == targetYear && t.monthValue == targetMonth
            } catch (e: Exception) { false }
        }
    }

    val monthlyFlare = remember(entries, targetYear, targetMonth) {
        entries.count { entry ->
            entry.type == EntryType.FLARE_UP && try {
                val t = ZonedDateTime.parse(entry.timestamp)
                t.year == targetYear && t.monthValue == targetMonth
            } catch (e: Exception) { false }
        }
    }

    val highMedicationUsageOccurrences = remember(entries, targetYear, targetMonth) {
        val countsByDay = mutableMapOf<String, Int>()
        entries.forEach { entry ->
            if (entry.type == EntryType.ANTIHISTAMINE) {
                try {
                    val t = ZonedDateTime.parse(entry.timestamp)
                    if (t.year == targetYear && t.monthValue == targetMonth) {
                        val dayKey = "${t.year}-${t.monthValue}-${t.dayOfMonth}"
                        countsByDay[dayKey] = (countsByDay[dayKey] ?: 0) + 1
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
        countsByDay.values.count { it > 2 }
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
            
            // Collapsible header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCard)
                    .clickable { isOpen = !isOpen }
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Medication Summary",
                        color = LightGray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp
                    )
                }

                Text(
                    text = if (isOpen) "▲" else "▼",
                    color = MutedGray,
                    fontSize = 10.sp
                )
            }

            // Expanded body
            AnimatedVisibility(
                visible = isOpen,
                enter = expandVertically(animationSpec = tween(300)),
                exit = shrinkVertically(animationSpec = tween(300))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface)
                        .padding(16.dp)
                ) {
                    // Lookback Matrix Title
                    Text(
                        text = "ANTIHISTAMINE INTAKE",
                        color = Color(0xFF737373),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    // Lookbacks row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LookbackCard(title = "LAST 24H", count = ah24, unit = if (ah24 == 1) "pill" else "pills")
                        LookbackCard(title = "LAST 48H", count = ah48, unit = if (ah48 == 1) "pill" else "pills")
                        LookbackCard(title = "LAST 72H", count = ah72, unit = if (ah72 == 1) "pill" else "pills")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DarkBorder))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Calendar Title & Pagination chevrons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MONTHLY OVERVIEW",
                            color = Color(0xFF737373),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Calendar controls card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkCard, shape = RoundedCornerShape(8.dp))
                            .border(1.dp, DarkBorder, shape = RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.previousMonth() },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkBorder),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.size(width = 36.dp, height = 28.dp)
                        ) {
                            Text(text = "◀", color = MutedGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Text(
                            text = "$monthName $targetYear",
                            color = LightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Button(
                            onClick = { viewModel.nextMonth() },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkBorder),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.size(width = 36.dp, height = 28.dp)
                        ) {
                            Text(text = "▶", color = MutedGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Monthly counts card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkCard, shape = RoundedCornerShape(10.dp))
                            .border(1.dp, DarkBorder, shape = RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        MonthlySummaryRow(color = SoftYellow, label = "Antihistamines Taken", count = monthlyAH, unit = if (monthlyAH == 1) "pill" else "pills", countColor = SoftYellow)
                        MonthlySummaryRow(color = Color(0xFF1A7E97), label = "Corticosteroids", count = monthlyCortisone, unit = if (monthlyCortisone == 1) "dose" else "doses", countColor = Color(0xFF1A7E97))
                        MonthlySummaryRow(color = CoralPink, label = "Symptom Flare-ups", count = monthlyFlare, unit = if (monthlyFlare == 1) "event" else "events", countColor = SoftPurple)
                        if (highMedicationUsageOccurrences > 0) {
                            MonthlySummaryRow(color = Color(0xFFFAA18F), label = "High Medication Usage", count = highMedicationUsageOccurrences, unit = "occurances", countColor = Color(0xFFFAA18F))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.LookbackCard(title: String, count: Int, unit: String) {
    Card(
        modifier = Modifier.weight(1f),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.dp, DarkBorder),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = MutedGray,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Text(
                text = count.toString(),
                color = SoftYellow,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = unit.uppercase(),
                color = MutedGray,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun MonthlySummaryRow(color: Color, label: String, count: Int, unit: String, countColor: Color = SoftPurple) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(color, shape = RoundedCornerShape(3.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = LightGray.copy(alpha = 0.8f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Text(
            text = "$count $unit",
            color = countColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
