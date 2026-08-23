package com.example.urticare.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.urticare.model.EntryType
import com.example.urticare.model.LogEntry
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.ZonedDateTime

private fun formatElapsedSecs(secs: Long): String {
    if (secs < 0) return "0 hours 0 mins"
    val totalHrs = secs / 3600
    val mins = (secs % 3600) / 60
    return if (totalHrs >= 48) {
        val days = totalHrs / 24
        val remHrs = totalHrs % 24
        if (remHrs > 0) "${days} days ${remHrs} hours" else "${days} days"
    } else if (totalHrs > 0) {
        "${totalHrs} hours ${mins} mins"
    } else {
        "${mins} mins"
    }
}

@Composable
fun LiveCounter(entries: List<LogEntry>, onNavigateToLogs: (Int) -> Unit = {}) {
    var activeViewIndex by remember { mutableIntStateOf(0) } // 0: Antihistamine, 1: Flare-Up, 2: Corticosteroids, 3: Bio. Treatment, 4: Consumables

    // 1. Antihistamine Timestamp & Timer
    val lastAHTimestamp = remember(entries) {
        entries.firstOrNull { it.type == EntryType.ANTIHISTAMINE }?.timestamp
    }
    var ahElapsedText by remember(lastAHTimestamp) {
        mutableStateOf(if (lastAHTimestamp != null) "0 hours 0 mins" else "--")
    }

    // 2. Flare-up Timestamp & Timer
    val lastFlareTimestamp = remember(entries) {
        entries.firstOrNull { it.type == EntryType.FLARE_UP }?.timestamp
    }
    var flareElapsedText by remember(lastFlareTimestamp) {
        mutableStateOf(if (lastFlareTimestamp != null) "0 hours 0 mins" else "--")
    }

    // 3. Corticosteroids Timestamp & Timer (Use last logged CORTISONE entry, if none -> "--")
    val lastCortisoneTimestamp = remember(entries) {
        entries.firstOrNull { it.type == EntryType.CORTISONE }?.timestamp
    }
    var cortisoneElapsedText by remember(lastCortisoneTimestamp) {
        mutableStateOf(if (lastCortisoneTimestamp != null) "0 hours 0 mins" else "--")
    }

    // 4. Biological Treatment Timestamp & Timer (XOLAIR_150, XOLAIR_300, ALTERNATIVE)
    val lastBioTimestamp = remember(entries) {
        entries.firstOrNull { 
            it.type == EntryType.XOLAIR_150 || 
            it.type == EntryType.XOLAIR_300 || 
            it.type == EntryType.ALTERNATIVE 
        }?.timestamp
    }
    var bioElapsedText by remember(lastBioTimestamp) {
        mutableStateOf(if (lastBioTimestamp != null) "0 hours 0 mins" else "--")
    }

    // 5. Consumables Timestamp, Timer & Trigger Name
    val lastConsumableEntry = remember(entries) {
        entries.firstOrNull { entry ->
            if (entry.type == EntryType.CONSUMPTION) {
                val parts = entry.metadata?.split(":::")
                parts != null && parts.size >= 6 && parts[5] == "Trigger"
            } else {
                false
            }
        }
    }
    val lastConsumableTimestamp = remember(lastConsumableEntry) {
        lastConsumableEntry?.timestamp
    }
    var consumableElapsedText by remember(lastConsumableTimestamp) {
        mutableStateOf(if (lastConsumableTimestamp != null) "0 hours 0 mins" else "--")
    }
    val lastConsumableName = remember(lastConsumableEntry) {
        val metadataStr = lastConsumableEntry?.metadata ?: ""
        val parts = metadataStr.split(":::")
        if (parts.size >= 2) parts[1] else null
    }
    val lastConsumableDateFormatted = remember(lastConsumableTimestamp) {
        if (lastConsumableTimestamp != null) {
            try {
                val date = ZonedDateTime.parse(lastConsumableTimestamp).withZoneSameInstant(java.time.ZoneId.systemDefault())
                val rawMonth = date.month.name.lowercase().replaceFirstChar { it.uppercase() }
                val month = if (rawMonth.length > 3) rawMonth.substring(0, 3) else rawMonth
                val day = date.dayOfMonth
                "$month $day"
            } catch (e: Exception) {
                ""
            }
        } else {
            ""
        }
    }

    // Live Timer Engine
    LaunchedEffect(lastAHTimestamp, lastFlareTimestamp, lastCortisoneTimestamp, lastBioTimestamp, lastConsumableTimestamp) {
        while (true) {
            // Antihistamine
            if (lastAHTimestamp != null) {
                try {
                    val lastDate = ZonedDateTime.parse(lastAHTimestamp)
                    val secs = Duration.between(lastDate, ZonedDateTime.now()).seconds
                    ahElapsedText = formatElapsedSecs(secs)
                } catch (e: Exception) {
                    ahElapsedText = "--"
                }
            } else {
                ahElapsedText = "--"
            }

            // Flare-Up
            if (lastFlareTimestamp != null) {
                try {
                    val lastDate = ZonedDateTime.parse(lastFlareTimestamp)
                    val secs = Duration.between(lastDate, ZonedDateTime.now()).seconds
                    flareElapsedText = formatElapsedSecs(secs)
                } catch (e: Exception) {
                    flareElapsedText = "--"
                }
            } else {
                flareElapsedText = "--"
            }

            // Corticosteroid
            if (lastCortisoneTimestamp != null) {
                try {
                    val lastDate = ZonedDateTime.parse(lastCortisoneTimestamp)
                    val secs = Duration.between(lastDate, ZonedDateTime.now()).seconds
                    cortisoneElapsedText = formatElapsedSecs(secs)
                } catch (e: Exception) {
                    cortisoneElapsedText = "--"
                }
            } else {
                cortisoneElapsedText = "--"
            }

            // Biological Treatment
            if (lastBioTimestamp != null) {
                try {
                    val lastDate = ZonedDateTime.parse(lastBioTimestamp)
                    val secs = Duration.between(lastDate, ZonedDateTime.now()).seconds
                    bioElapsedText = formatElapsedSecs(secs)
                } catch (e: Exception) {
                    bioElapsedText = "--"
                }
            } else {
                bioElapsedText = "--"
            }

            // Consumables
            if (lastConsumableTimestamp != null) {
                try {
                    val lastDate = ZonedDateTime.parse(lastConsumableTimestamp)
                    val secs = Duration.between(lastDate, ZonedDateTime.now()).seconds
                    consumableElapsedText = formatElapsedSecs(secs)
                } catch (e: Exception) {
                    consumableElapsedText = "--"
                }
            } else {
                consumableElapsedText = "--"
            }

            delay(1000)
        }
    }

    // Tricolor gradient mix: Purple (#814b92) -> Green (#509729) -> Teal (#1a7e97)
    val ringGradient = remember {
        Brush.sweepGradient(
            listOf(
                Color(0xFF814B92),
                Color(0xFF509729),
                Color(0xFF1A7E97),
                Color(0xFF814B92)
            )
        )
    }

    Column(
        modifier = Modifier.padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var totalDragAmount by remember { mutableStateOf(0f) }

        // Main Container with Circle
        Box(
            contentAlignment = Alignment.Center
        ) {
            // Circular Component (~176dp)
            Box(
                modifier = Modifier
                    .size(176.dp)
                    .shadow(elevation = 4.dp, shape = CircleShape, ambientColor = Color(0x100F172A), spotColor = Color(0x100F172A))
                    .background(Color.White, shape = CircleShape)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (totalDragAmount > 25f) {
                                    activeViewIndex = (activeViewIndex + 4) % 5 // Swipe Right -> Previous
                                } else if (totalDragAmount < -25f) {
                                    activeViewIndex = (activeViewIndex + 1) % 5 // Swipe Left -> Next
                                }
                                totalDragAmount = 0f
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                totalDragAmount += dragAmount
                            }
                        )
                    }
                    .clickable {
                        if (activeViewIndex == 4 && lastConsumableName != null) {
                            onNavigateToLogs(2)
                        } else {
                            activeViewIndex = (activeViewIndex + 1) % 5
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Dynamic Event-Based Gradient Arc Frame
                val ringColor = when (activeViewIndex) {
                    0 -> Color(0xFF509729) // Antihistamine -> Green
                    1 -> Color(0xFF814B92) // Flare Up -> Purple
                    2 -> Color(0xFF1A7E97) // Corticosteroids -> Teal
                    3 -> Color(0xFF1A7E97) // Biological -> Teal
                    else -> Color(0xFFF78325) // Consumables -> Orange
                }

                val hasData = when (activeViewIndex) {
                    0 -> lastAHTimestamp != null
                    1 -> lastFlareTimestamp != null
                    2 -> lastCortisoneTimestamp != null
                    3 -> lastBioTimestamp != null
                    else -> lastConsumableTimestamp != null
                }

                Canvas(modifier = Modifier.size(176.dp)) {
                    val strokeWidth = 5.dp.toPx()
                    val radius = (size.width - strokeWidth) / 2
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                    val arcSize = Size(radius * 2, radius * 2)

                    drawArc(
                        color = Color(0xFFE2E8F0),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth)
                    )

                    if (hasData) {
                        drawArc(
                            color = ringColor,
                            startAngle = -90f,
                            sweepAngle = 270f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }

                // Inner Circle Content
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(10.dp)
                ) {
                    when (activeViewIndex) {
                        0 -> {
                            // 1. Antihistamine View
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFFECFDF5), shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.size(18.dp)) {
                                    drawRoundRect(
                                        color = Color(0xFF509729),
                                        topLeft = Offset(size.width * 0.25f, 0f),
                                        size = Size(size.width * 0.5f, size.height),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.25f),
                                        style = Stroke(width = 1.5.dp.toPx())
                                    )
                                    drawLine(
                                        color = Color(0xFF509729),
                                        start = Offset(size.width * 0.25f, size.height * 0.5f),
                                        end = Offset(size.width * 0.75f, size.height * 0.5f),
                                        strokeWidth = 1.5.dp.toPx()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = ahElapsedText,
                                color = Color(0xFF0F172A),
                                fontSize = if (ahElapsedText.length > 12) 17.sp else 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp,
                                maxLines = 1
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = if (lastAHTimestamp != null) "Since your last antihistamine" else "No antihistamine logged yet",
                                color = Color(0xFF64748B),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                        1 -> {
                            // 2. Flare-Up View
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFFF5F3FF), shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.size(18.dp)) {
                                    val dropPath = Path().apply {
                                        moveTo(size.width * 0.5f, size.height * 0.15f)
                                        quadraticTo(size.width * 0.85f, size.height * 0.55f, size.width * 0.85f, size.height * 0.7f)
                                        cubicTo(size.width * 0.85f, size.height * 0.9f, size.width * 0.15f, size.height * 0.9f, size.width * 0.15f, size.height * 0.7f)
                                        quadraticTo(size.width * 0.15f, size.height * 0.55f, size.width * 0.5f, size.height * 0.15f)
                                    }
                                    drawPath(
                                        path = dropPath,
                                        color = Color(0xFF814B92),
                                        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = flareElapsedText,
                                color = Color(0xFF0F172A),
                                fontSize = if (flareElapsedText.length > 12) 17.sp else 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp,
                                maxLines = 1
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = if (lastFlareTimestamp != null) "Since your last flare-up" else "No flare-up logged yet",
                                color = Color(0xFF64748B),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                        2 -> {
                            // 3. Corticosteroids View
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFFF0FDFA), shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.size(18.dp)) {
                                    drawCircle(
                                        color = Color(0xFF1A7E97),
                                        center = Offset(size.width * 0.5f, size.height * 0.5f),
                                        radius = size.width * 0.4f,
                                        style = Stroke(width = 1.5.dp.toPx())
                                    )
                                    drawLine(
                                        color = Color(0xFF1A7E97),
                                        start = Offset(size.width * 0.1f, size.height * 0.5f),
                                        end = Offset(size.width * 0.9f, size.height * 0.5f),
                                        strokeWidth = 1.5.dp.toPx()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = cortisoneElapsedText,
                                color = Color(0xFF0F172A),
                                fontSize = if (cortisoneElapsedText == "None") 20.sp else if (cortisoneElapsedText.length > 12) 17.sp else 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp,
                                maxLines = 1
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                             Text(
                                 text = if (lastCortisoneTimestamp != null) "Since your last corticosteroid" else "No corticosteroid logged yet",
                                 color = Color(0xFF64748B),
                                 fontSize = 10.5.sp,
                                 fontWeight = FontWeight.Medium,
                                 maxLines = 1
                             )
                        }
                        3 -> {
                            // 4. Biological Treatment View
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFFF0FDFA), shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.size(18.dp)) {
                                    val w = size.width
                                    val h = size.height
                                    val strokePx = 1.5.dp.toPx()
                                    val colorTeal = Color(0xFF1A7E97)
                                    
                                    // Needle line
                                    drawLine(
                                        color = colorTeal,
                                        start = Offset(w * 0.5f, h * 0.1f),
                                        end = Offset(w * 0.5f, h * 0.3f),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                    
                                    // Barrel
                                    drawRoundRect(
                                        color = colorTeal,
                                        topLeft = Offset(w * 0.35f, h * 0.3f),
                                        size = Size(w * 0.3f, h * 0.4f),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx()),
                                        style = Stroke(width = strokePx)
                                    )
                                    
                                    // Graduation marks
                                    drawLine(
                                        color = colorTeal,
                                        start = Offset(w * 0.42f, h * 0.42f),
                                        end = Offset(w * 0.52f, h * 0.42f),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                    drawLine(
                                        color = colorTeal,
                                        start = Offset(w * 0.42f, h * 0.55f),
                                        end = Offset(w * 0.52f, h * 0.55f),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                    
                                    // Plunger rod
                                    drawLine(
                                        color = colorTeal,
                                        start = Offset(w * 0.5f, h * 0.7f),
                                        end = Offset(w * 0.5f, h * 0.85f),
                                        strokeWidth = strokePx
                                    )
                                    
                                    // Plunger cap
                                    drawLine(
                                        color = colorTeal,
                                        start = Offset(w * 0.4f, h * 0.85f),
                                        end = Offset(w * 0.6f, h * 0.85f),
                                        strokeWidth = strokePx
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = bioElapsedText,
                                color = Color(0xFF0F172A),
                                fontSize = if (bioElapsedText == "None") 20.sp else if (bioElapsedText.length > 12) 17.sp else 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp,
                                maxLines = 1
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                             Text(
                                 text = if (lastBioTimestamp != null) "Since your last bio. treatment" else "No bio. treatment logged yet",
                                 color = Color(0xFF64748B),
                                 fontSize = 10.5.sp,
                                 fontWeight = FontWeight.Medium,
                                 maxLines = 1
                             )
                        }
                        else -> {
                            // 5. Consumables View
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFFFFF7ED), shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.size(18.dp)) {
                                    val w = size.width
                                    val h = size.height
                                    val strokePx = 1.5.dp.toPx()
                                    val colorOrange = Color(0xFFF78325)
                                    
                                    // Warning triangle path
                                    val trianglePath = Path().apply {
                                        moveTo(w * 0.5f, h * 0.15f)
                                        lineTo(w * 0.88f, h * 0.82f)
                                        lineTo(w * 0.12f, h * 0.82f)
                                        close()
                                    }
                                    
                                    drawPath(
                                        path = trianglePath,
                                        color = colorOrange,
                                        style = Stroke(width = strokePx, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
                                    )
                                    
                                    // Exclamation point line
                                    drawLine(
                                        color = colorOrange,
                                        start = Offset(w * 0.5f, h * 0.38f),
                                        end = Offset(w * 0.5f, h * 0.58f),
                                        strokeWidth = 1.5.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                    
                                    // Exclamation point dot
                                    drawCircle(
                                        color = colorOrange,
                                        radius = 1.dp.toPx(),
                                        center = Offset(w * 0.5f, h * 0.72f)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))

                            if (lastConsumableName != null) {
                                Text(
                                    text = "Last trigger was",
                                    color = Color(0xFF64748B),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = lastConsumableName,
                                    color = Color(0xFF0F172A),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = "on $lastConsumableDateFormatted",
                                    color = Color(0xFF64748B),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                            } else {
                                Text(
                                    text = "No trigger logged",
                                    color = Color(0xFF0F172A),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Pager Indicator Dots (5 Dots)
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until 5) {
                val isActive = activeViewIndex == i
                val dotColor = when (i) {
                    0 -> Color(0xFF509729)
                    1 -> Color(0xFF814B92)
                    2 -> Color(0xFF1A7E97)
                    3 -> Color(0xFF1A7E97)
                    else -> Color(0xFFF78325)
                }
                Box(
                    modifier = Modifier
                        .size(if (isActive) 7.dp else 5.dp)
                        .background(
                            color = if (isActive) dotColor else Color(0xFFCBD5E1),
                            shape = CircleShape
                        )
                )
            }
        }
    }
}
