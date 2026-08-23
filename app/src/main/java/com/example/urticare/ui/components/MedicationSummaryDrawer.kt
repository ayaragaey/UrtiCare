package com.example.urticare.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.urticare.R
import com.example.urticare.model.EntryType
import com.example.urticare.model.LogEntry
import com.example.urticare.ui.theme.*
import com.example.urticare.viewmodel.TrackerViewModel
import java.time.ZonedDateTime

private val MONTH_NAMES = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
)

@Composable
fun MedicationSummaryDrawer(
    viewModel: TrackerViewModel,
    entries: List<LogEntry>,
    onNavigateToLogs: (Int) -> Unit,
    onClose: () -> Unit
) {
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // --- HEADER SECTION ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val brandGradient = remember {
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF814B92),
                            Color(0xFF509729),
                            Color(0xFF1A7E97)
                        )
                    )
                }
                Text(
                    text = "Medication Summary",
                    style = TextStyle(
                        brush = brandGradient,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            // Close button (X)
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFFF1F5F9), shape = CircleShape)
            ) {
                Text(
                    text = "✕",
                    color = Color(0xFF64748B),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // --- RECENT INTAKE SECTION (LOOKBACKS) ---
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "RECENT INTAKE",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RecentIntakeCard(title = "Last 24h", count = ah24, unit = if (ah24 == 1) "pill" else "pills")
                RecentIntakeCard(title = "Last 48h", count = ah48, unit = if (ah48 == 1) "pill" else "pills")
                RecentIntakeCard(title = "Last 72h", count = ah72, unit = if (ah72 == 1) "pill" else "pills")
            }
        }

        // Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFF1F5F9))
        )

        // --- MONTHLY OVERVIEW SECTION ---
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Calendar icon + label
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Canvas(modifier = Modifier.size(16.dp)) {
                        val rectY = 3.dp.toPx()
                        drawRoundRect(
                            color = Color(0xFF64748B),
                            topLeft = Offset(0f, rectY),
                            size = androidx.compose.ui.geometry.Size(size.width, size.height - rectY),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                        drawLine(color = Color(0xFF64748B), start = Offset(size.width * 0.25f, 0f), end = Offset(size.width * 0.25f, 5.dp.toPx()), strokeWidth = 1.5.dp.toPx())
                        drawLine(color = Color(0xFF64748B), start = Offset(size.width * 0.75f, 0f), end = Offset(size.width * 0.75f, 5.dp.toPx()), strokeWidth = 1.5.dp.toPx())
                        drawLine(color = Color(0xFF64748B), start = Offset(0f, size.height * 0.38f), end = Offset(size.width, size.height * 0.38f), strokeWidth = 1.dp.toPx())
                    }
                    Text(
                        text = "MONTHLY OVERVIEW",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                // Month navigation row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "◀",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { viewModel.previousMonth() }
                            .padding(horizontal = 6.dp)
                    )

                    Text(
                        text = "$monthName $targetYear",
                        color = Color(0xFF0F172A),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "▶",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { viewModel.nextMonth() }
                            .padding(horizontal = 6.dp)
                    )
                }
            }

            // --- MONTHLY ANALYTICS SECTION ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Row 1: Antihistamines
                AnalyticsCardRow(
                    title = "Antihistamines Taken",
                    value = "$monthlyAH " + if (monthlyAH == 1) "pill" else "pills",
                    iconRes = R.drawable.ic_icon_antihistamine,
                    themeColor = Color(0xFF509729),
                    chartHeights = listOf(0.3f, 0.5f, 0.4f, 0.8f, 0.6f),
                    onClick = {
                        onClose()
                        onNavigateToLogs(0) // Tab 0 = Medication Log
                    }
                )

                // Row 2: Corticosteroids
                AnalyticsCardRow(
                    title = "Corticosteroids",
                    value = "$monthlyCortisone " + if (monthlyCortisone == 1) "dose" else "doses",
                    iconRes = R.drawable.ic_icon_cortisone,
                    themeColor = Color(0xFF1A7E97),
                    chartHeights = listOf(0.1f, 0.2f, 0.1f, 0.3f, 0.15f), // lower/inactive trend heights
                    isInactive = (monthlyCortisone == 0),
                    onClick = {
                        onClose()
                        onNavigateToLogs(0) // Tab 0 = Medication Log
                    }
                )

                // Row 3: Flareups
                AnalyticsCardRow(
                    title = "Symptom Flare Ups",
                    value = "$monthlyFlare " + if (monthlyFlare == 1) "event" else "events",
                    iconRes = R.drawable.ic_icon_flareup,
                    themeColor = Color(0xFF814B92),
                    chartHeights = listOf(0.2f, 0.4f, 0.6f, 0.5f, 0.7f),
                    onClick = {
                        onClose()
                        onNavigateToLogs(1) // Tab 1 = Flare Up
                    }
                )
            }
        }
    }
}

@Composable
fun RowScope.RecentIntakeCard(title: String, count: Int, unit: String) {
    Card(
        modifier = Modifier.weight(1f),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, Color(0xFF737373)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Small Clock Icon
            Canvas(modifier = Modifier.size(12.dp)) {
                drawCircle(color = Color(0xFF737373), style = Stroke(width = 1.dp.toPx()))
                drawLine(color = Color(0xFF737373), start = center, end = center + Offset(0f, -size.height * 0.3f), strokeWidth = 1.dp.toPx())
                drawLine(color = Color(0xFF737373), start = center, end = center + Offset(size.width * 0.2f, 0f), strokeWidth = 1.dp.toPx())
            }

            Text(
                text = title,
                color = Color(0xFF737373),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = count.toString(),
                color = Color(0xFF737373),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = unit.uppercase(),
                color = Color(0xFF737373),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun AnalyticsCardRow(
    title: String,
    value: String,
    iconRes: Int,
    themeColor: Color,
    chartHeights: List<Float>,
    isInactive: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category icon
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(if (isInactive) Color(0xFFE2E8F0) else themeColor.copy(alpha = 0.1f), shape = CircleShape)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    colorFilter = if (isInactive) ColorFilter.tint(Color(0xFF94A3B8)) else null,
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Category name
            Text(
                text = title,
                color = Color(0xFF0F172A),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )

            // Mini bar chart
            Canvas(modifier = Modifier.size(width = 38.dp, height = 18.dp)) {
                val barWidth = 3.5.dp.toPx()
                val gap = 2.5.dp.toPx()
                val color = if (isInactive) Color(0xFFCBD5E1) else themeColor
                for (i in 0 until 5) {
                    val x = i * (barWidth + gap)
                    val h = size.height * chartHeights[i]
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, size.height - h),
                        size = androidx.compose.ui.geometry.Size(barWidth, h),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx())
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Value text
            Text(
                text = value,
                color = if (isInactive) Color(0xFF64748B) else Color(0xFF0F172A),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Chevron arrow
            Canvas(modifier = Modifier.size(10.dp)) {
                val path = Path().apply {
                    moveTo(size.width * 0.3f, size.height * 0.2f)
                    lineTo(size.width * 0.7f, size.height * 0.5f)
                    lineTo(size.width * 0.3f, size.height * 0.8f)
                }
                drawPath(
                    path = path,
                    color = Color(0xFF94A3B8),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}
