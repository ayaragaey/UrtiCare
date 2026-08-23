package com.example.urticare.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.urticare.R
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.urticare.model.EntryType
import com.example.urticare.model.LogEntry
import com.example.urticare.ui.theme.*
import com.example.urticare.viewmodel.TrackerViewModel
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.example.urticare.model.MedicationDirectory
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.type as keyType
import androidx.compose.ui.input.key.KeyEventType
import java.time.ZonedDateTime

// Helper function to format timestamp into friendly relative date-time format
private fun getFriendlyRelativeDateTime(timestampStr: String): String {
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
        
        // Format time as hh:mm a
        val hour = dt.hour
        val minute = dt.minute
        val ampm = if (hour >= 12) "PM" else "AM"
        val displayHour = if (hour % 12 == 0) 12 else hour % 12
        val displayMinute = String.format("%02d", minute)
        
        "$relativeDate • $displayHour:$displayMinute $ampm"
    } catch (e: Exception) {
        ""
    }
}

private data class MedicationDetails(
    val name: String,
    val dose: String,
    val category: String,
    val isAntihistamine: Boolean
)

private fun getMedicationDetails(type: EntryType, metadata: String?): MedicationDetails {
    val parts = (metadata ?: "").split(":::")
    val rawName = if (parts.size >= 2) parts[0].trim() else (metadata ?: "").trim()
    val rawMgs = if (parts.size >= 2) parts[1].trim() else ""
    
    // Strip generation information for UI display without changing underlying data
    val cleanName = rawName
        .replace(Regex("(?i)[–\\-—:]\\s*\\d+(?:st|nd|rd|th)?\\s*(?:gen(?:eration)?|line)?.*"), "")
        .replace(Regex("(?i)\\b\\d+(?:st|nd|rd|th)\\s+generation\\b"), "")
        .replace(Regex("(?i)\\((?:1st|2nd|3rd|first|second|third)\\s+gen(?:eration)?\\)"), "")
        .trim()

    val name = if (cleanName.isEmpty()) {
        if (type == EntryType.ANTIHISTAMINE) "Antihistamine" else "Cortisone"
    } else {
        cleanName
    }
    
    val dose = if (rawMgs.isNotEmpty()) {
        val clean = rawMgs.replace("mg", "").trim()
        if (clean.isNotEmpty()) "$clean mg" else ""
    } else {
        ""
    }
    
    val upperName = rawName.uppercase()
    val category = when {
        upperName.contains("XOLAIR") -> "Biological Treatment"
        upperName.contains("DEXAZONE") || type == EntryType.CORTISONE || upperName.contains("PREDNISOLONE") || upperName.contains("CORTISONE") -> "Corticosteroid"
        else -> "Antihistamine"
    }
    
    val isAntihistamine = category == "Antihistamine"
    
    return MedicationDetails(name, dose, category, isAntihistamine)
}

private fun getRelativeTimeText(timestampStr: String): String {
    return try {
        val dt = ZonedDateTime.parse(timestampStr).withZoneSameInstant(java.time.ZoneId.systemDefault())
        val now = ZonedDateTime.now(java.time.ZoneId.systemDefault())
        
        val diffTime = now.toInstant().toEpochMilli() - dt.toInstant().toEpochMilli()
        val diffDays = (diffTime / (1000 * 60 * 60 * 24)).toInt()
        
        when {
            diffDays <= 0 -> "Today"
            else -> "${diffDays}d ago"
        }
    } catch (e: Exception) {
        ""
    }
}

@Composable
private fun RecentEditPencilIcon(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF64748B)
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 1.4.dp.toPx()
        val s = size.minDimension

        withTransform({
            rotate(degrees = 45f, pivot = center)
        }) {
            val cx = center.x
            val cy = center.y
            val halfW = s * 0.16f
            val topY = cy - s * 0.44f
            val eraserLineY = cy - s * 0.26f
            val bodyBottomY = cy + s * 0.20f
            val tipY = cy + s * 0.44f

            // 1. Eraser cap (rounded top)
            val eraserCorner = s * 0.12f
            val eraserPath = Path().apply {
                moveTo(cx - halfW, eraserLineY)
                lineTo(cx - halfW, topY + eraserCorner)
                quadraticTo(cx - halfW, topY, cx - halfW + eraserCorner, topY)
                lineTo(cx + halfW - eraserCorner, topY)
                quadraticTo(cx + halfW, topY, cx + halfW, topY + eraserCorner)
                lineTo(cx + halfW, eraserLineY)
            }
            drawPath(
                eraserPath,
                color = color,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // 2. Collar line
            drawLine(
                color = color,
                start = Offset(cx - halfW, eraserLineY),
                end = Offset(cx + halfW, eraserLineY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // 3. Body shaft (outer lines)
            drawLine(
                color = color,
                start = Offset(cx - halfW, eraserLineY),
                end = Offset(cx - halfW, bodyBottomY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(cx + halfW, eraserLineY),
                end = Offset(cx + halfW, bodyBottomY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Center facet line
            drawLine(
                color = color,
                start = Offset(cx, eraserLineY),
                end = Offset(cx, bodyBottomY),
                strokeWidth = strokeWidth * 0.85f,
                cap = StrokeCap.Round
            )

            // 4. Wood tip cone
            val conePath = Path().apply {
                moveTo(cx - halfW, bodyBottomY)
                lineTo(cx - halfW * 0.35f, bodyBottomY + s * 0.08f)
                lineTo(cx, tipY)
                lineTo(cx + halfW * 0.35f, bodyBottomY + s * 0.08f)
                lineTo(cx + halfW, bodyBottomY)
            }
            drawPath(
                conePath,
                color = color,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // 5. Lead tip fill
            val leadPath = Path().apply {
                val leadTopY = tipY - s * 0.12f
                val leadHalfW = halfW * 0.40f
                moveTo(cx - leadHalfW, leadTopY)
                lineTo(cx, tipY)
                lineTo(cx + leadHalfW, leadTopY)
                close()
            }
            drawPath(leadPath, color = color)
        }
    }
}

@Composable
private fun RecentDeleteTrashIcon(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFE55353)
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 1.4.dp.toPx()
        val w = size.width
        val h = size.height

        // 1. Top handle
        val handleW = w * 0.36f
        val handleH = h * 0.16f
        val handleLeft = (w - handleW) / 2f
        val handleTop = h * 0.04f
        val handleCorner = 2.dp.toPx()

        val handlePath = Path().apply {
            moveTo(handleLeft, handleTop + handleH)
            lineTo(handleLeft, handleTop + handleCorner)
            quadraticTo(handleLeft, handleTop, handleLeft + handleCorner, handleTop)
            lineTo(handleLeft + handleW - handleCorner, handleTop)
            quadraticTo(handleLeft + handleW, handleTop, handleLeft + handleW, handleTop + handleCorner)
            lineTo(handleLeft + handleW, handleTop + handleH)
        }
        drawPath(
            handlePath,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // 2. Lid (horizontal bar)
        val lidH = h * 0.12f
        val lidTop = h * 0.20f
        val lidLeft = w * 0.08f
        val lidW = w * 0.84f
        val lidCorner = 2.5.dp.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(lidLeft, lidTop),
            size = Size(lidW, lidH),
            cornerRadius = CornerRadius(lidCorner, lidCorner),
            style = Stroke(width = strokeWidth)
        )

        // 3. Bin Body (tapered)
        val bodyTop = lidTop + lidH
        val bodyBottom = h * 0.94f
        val bodyTopLeft = w * 0.18f
        val bodyTopRight = w * 0.82f
        val bodyBottomLeft = w * 0.24f
        val bodyBottomRight = w * 0.76f
        val bodyCorner = 3.5.dp.toPx()

        val bodyPath = Path().apply {
            moveTo(bodyTopLeft, bodyTop)
            lineTo(bodyBottomLeft, bodyBottom - bodyCorner)
            quadraticTo(bodyBottomLeft, bodyBottom, bodyBottomLeft + bodyCorner, bodyBottom)
            lineTo(bodyBottomRight - bodyCorner, bodyBottom)
            quadraticTo(bodyBottomRight, bodyBottom, bodyBottomRight, bodyBottom - bodyCorner)
            lineTo(bodyTopRight, bodyTop)
        }
        drawPath(
            bodyPath,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // 4. Three vertical interior lines (ribs)
        val lineTop = bodyTop + h * 0.14f
        val lineBottom = bodyBottom - h * 0.12f
        val line1X = w * 0.36f
        val line2X = w * 0.50f
        val line3X = w * 0.64f

        drawLine(
            color = color,
            start = Offset(line1X, lineTop),
            end = Offset(line1X - w * 0.015f, lineBottom),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(line2X, lineTop),
            end = Offset(line2X, lineBottom),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(line3X, lineTop),
            end = Offset(line3X + w * 0.015f, lineBottom),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecentIntakeLog(
    viewModel: TrackerViewModel,
    entries: List<LogEntry>,
    onClose: () -> Unit,
    onViewHistory: () -> Unit
) {
    val context = LocalContext.current

    val antihistamines by viewModel.profileAntihistamines.collectAsState()
    val profileCortisones by viewModel.profileCortisones.collectAsState()

    var editingEntry by remember { mutableStateOf<LogEntry?>(null) }
    var editTimestamp by remember { mutableStateOf("") }
    var editMedName by remember { mutableStateOf("") }
    var editMedMgs by remember { mutableStateOf("") }
    var editEffectStarted by remember { mutableStateOf("") }
    var showAutocompleteSuggestions by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var showDateTimePicker by remember { mutableStateOf(false) }
    var manualEntryCallback by remember { mutableStateOf<((String) -> Unit)?>(null) }

    // Filter for Pills only (Antihistamine and Cortisone) - limit to last 4 entries
    val pillEntries = remember(entries) {
        entries.filter { it.type == EntryType.ANTIHISTAMINE || it.type == EntryType.CORTISONE }
               .sortedByDescending { it.timestamp }
               .take(4)
    }

    // Get the latest dynamic medication intake record
    val latestIntake = remember(pillEntries) {
        pillEntries.maxByOrNull { it.timestamp }
    }
    val lastTakenText = remember(latestIntake) {
        latestIntake?.let { getFriendlyRelativeDateTime(it.timestamp) } ?: ""
    }

    fun showEditDialog(item: LogEntry) {
        editingEntry = item
        editTimestamp = item.timestamp
        if (item.type == EntryType.ANTIHISTAMINE || item.type == EntryType.CORTISONE) {
            val parts = (item.metadata ?: "").split(":::")
            val rawName = if (parts.size >= 2) parts[0].trim() else (item.metadata ?: "").trim()
            val rawMgs = if (parts.size >= 2) parts[1].trim() else ""
            
            if (item.type == EntryType.ANTIHISTAMINE) {
                val mainMed = antihistamines.find { it.isMain } ?: antihistamines.firstOrNull()
                val otherMed = antihistamines.find { !it.isMain } ?: antihistamines.firstOrNull()
                val resolvedMed = when {
                    rawName.equals("Cetirizine", ignoreCase = true) -> mainMed
                    rawName.equals("Loratadine", ignoreCase = true) -> otherMed
                    else -> antihistamines.find { it.name.trim().lowercase() == rawName.lowercase() } ?: mainMed
                }
                if (resolvedMed != null) {
                    editMedName = resolvedMed.name
                    editMedMgs = resolvedMed.mgs.replace("mg", "").trim()
                } else {
                    editMedName = rawName
                    editMedMgs = rawMgs.replace("mg", "").trim()
                }
                editEffectStarted = if (parts.size >= 3) parts[2] else ""
            } else {
                val mainMed = profileCortisones.firstOrNull()
                val resolvedMed = profileCortisones.find { it.name.trim().lowercase() == rawName.lowercase() } ?: mainMed
                if (resolvedMed != null) {
                    editMedName = resolvedMed.name
                    editMedMgs = resolvedMed.mgs.replace("mg", "").trim()
                } else {
                    editMedName = rawName
                    editMedMgs = rawMgs.replace("mg", "").trim()
                }
                editEffectStarted = ""
            }
        } else {
            editMedName = ""
            editMedMgs = ""
            editEffectStarted = ""
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
                    text = "Recent Medication Intake",
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

        // --- LAST TAKEN INDICATOR STRIP ---
        if (lastTakenText.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .background(Color(0xFFF4FBF7), shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Custom green clock icon
                Canvas(modifier = Modifier.size(14.dp)) {
                    drawCircle(color = Color(0xFF509729), style = Stroke(width = 1.2.dp.toPx()))
                    drawLine(color = Color(0xFF509729), start = center, end = center + Offset(0f, -size.height * 0.3f), strokeWidth = 1.2.dp.toPx())
                    drawLine(color = Color(0xFF509729), start = center, end = center + Offset(size.width * 0.22f, 0f), strokeWidth = 1.2.dp.toPx())
                }
                
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = Color(0xFF475569), fontWeight = FontWeight.Medium)) {
                            append("Last taken:  ")
                        }
                        withStyle(style = SpanStyle(color = Color(0xFF509729), fontWeight = FontWeight.Bold)) {
                            append(lastTakenText)
                        }
                    },
                    fontSize = 13.sp
                )
            }
        }

        // --- MEDICATION TIMELINE (Content Area) ---
        if (pillEntries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No medication records logged yet",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp) // Timeline cards stack with connecting line
            ) {
                pillEntries.forEachIndexed { idx, item ->
                    // Parse precise Date and Time parts
                    val (dateStr, timeStr) = remember(item.timestamp) {
                            try {
                                val dt = ZonedDateTime.parse(item.timestamp).withZoneSameInstant(java.time.ZoneId.systemDefault())
                                val rawMonth = dt.month.name.lowercase().replaceFirstChar { it.uppercase() }
                                val month = if (rawMonth.length > 3) rawMonth.substring(0, 3) else rawMonth
                                
                                val hour = dt.hour
                                val minute = dt.minute
                                val ampm = if (hour >= 12) "PM" else "AM"
                                val displayHour = if (hour % 12 == 0) 12 else hour % 12
                                val displayMinute = String.format("%02d", minute)
                                
                                Pair("$month ${dt.dayOfMonth}, ${dt.year}", "$displayHour:$displayMinute $ampm")
                            } catch (e: Exception) {
                                Pair("", "")
                            }
                        }

                        val isFirst = (idx == 0)
                        val isLast = (idx == pillEntries.lastIndex)
                        
                        val details = remember(item.metadata, item.type) { 
                            getMedicationDetails(item.type, item.metadata) 
                        }
                        val relativeTime = remember(item.timestamp) { 
                            getRelativeTimeText(item.timestamp) 
                        }

                        val isCorticosteroid = details.category == "Corticosteroid" || item.type == EntryType.CORTISONE
                        val isBiological = details.category == "Biological Treatment"

                        val medThemeColor = when {
                            isBiological -> Color(0xFF814B92)
                            isCorticosteroid -> Color(0xFF1A7E97)
                            else -> Color(0xFF509729)
                        }

                        val medBadgeBg = when {
                            isBiological -> Color(0xFF814B92).copy(alpha = 0.12f)
                            isCorticosteroid -> Color(0xFF1A7E97).copy(alpha = 0.12f)
                            else -> Color(0xFF509729).copy(alpha = 0.12f)
                        }

                        val cardBg = when {
                            isBiological -> Color(0xFFFBFBFF)
                            isCorticosteroid -> Color(0xFFF8FCFD)
                            else -> Color(0xFFF7FAF8)
                        }

                        val cardBorder = when {
                            isBiological -> Color(0xFFF2ECF5)
                            isCorticosteroid -> Color(0xFFE6F2F4)
                            else -> Color(0xFFECF2EC)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Timeline dot & vertical connecting line
                            Canvas(modifier = Modifier.fillMaxHeight().width(24.dp)) {
                                val startY = if (isFirst) size.height / 2 else 0f
                                val endY = if (isLast) size.height / 2 else size.height
                                
                                // Draw vertical connecting line (light green/grey)
                                drawLine(
                                    color = Color(0xFFE2E8F0),
                                    start = Offset(size.width / 2, startY),
                                    end = Offset(size.width / 2, endY),
                                    strokeWidth = 2.dp.toPx()
                                )
                                
                                // Draw timeline dot
                                drawCircle(
                                    color = medThemeColor,
                                    radius = 4.dp.toPx(),
                                    center = Offset(size.width / 2, size.height / 2)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Medication entry timeline card
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 6.dp),
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                border = BorderStroke(1.dp, cardBorder),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // Row 1: Name & Dose (Left) + Relative Status (Right)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.weight(1f, fill = false)
                                            ) {
                                                Text(
                                                    text = details.name,
                                                    color = Color(0xFF0F172A),
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                if (details.dose.isNotEmpty()) {
                                                    Box(
                                                        modifier = Modifier
                                                            .background(medBadgeBg, shape = RoundedCornerShape(6.dp))
                                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = details.dose,
                                                            color = medThemeColor,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                            }

                                            if (relativeTime.isNotEmpty()) {
                                                Text(
                                                    text = relativeTime,
                                                    color = Color(0xFF737373),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        // Row 2: Category Badge (Left) + Compact Inline Actions (Right: Edit & Delete)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            // Category Badge
                                            Box(
                                                modifier = Modifier
                                                    .background(medBadgeBg, shape = RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = details.category,
                                                    color = medThemeColor,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                            }

                                            // Inline Actions: Edit & Delete buttons
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                // Edit action icon
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .background(Color(0xFFF1F5F9), shape = CircleShape)
                                                        .clickable {
                                                            showEditDialog(item)
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    RecentEditPencilIcon(
                                                        modifier = Modifier.size(19.dp),
                                                        color = Color(0xFF64748B)
                                                    )
                                                }

                                                // Delete action icon
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .background(Color(0xFFFEF2F2), shape = CircleShape)
                                                        .clickable {
                                                            viewModel.deleteEntry(item.id)
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    RecentDeleteTrashIcon(
                                                        modifier = Modifier.size(19.dp),
                                                        color = Color(0xFFE55353)
                                                    )
                                                }
                                            }
                                        }

                                        // Row 3: Emoji Metadata Row (📅 Date   ⏰ Time)
                                        Text(
                                            text = "📅 $dateStr    ⏰ $timeStr",
                                            color = Color(0xFF64748B),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

        // --- FOOTER SECTION: VIEW FULL HISTORY ---
        Card(
            onClick = { onViewHistory() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFF509729)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Canvas(modifier = Modifier.size(20.dp)) {
                        val rectY = 4.dp.toPx()
                        drawRoundRect(
                            color = Color(0xFF509729),
                            topLeft = Offset(0f, rectY),
                            size = androidx.compose.ui.geometry.Size(size.width, size.height - rectY),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                        drawLine(color = Color(0xFF509729), start = Offset(size.width * 0.25f, 0f), end = Offset(size.width * 0.25f, 6.dp.toPx()), strokeWidth = 1.5.dp.toPx())
                        drawLine(color = Color(0xFF509729), start = Offset(size.width * 0.75f, 0f), end = Offset(size.width * 0.75f, 6.dp.toPx()), strokeWidth = 1.5.dp.toPx())
                        drawLine(color = Color(0xFF509729), start = Offset(0f, size.height * 0.38f), end = Offset(size.width, size.height * 0.38f), strokeWidth = 1.dp.toPx())
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "View full medication history",
                            color = Color(0xFF509729),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "See all your medications and intake details",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Canvas(modifier = Modifier.size(16.dp)) {
                    val path = Path().apply {
                        moveTo(size.width * 0.35f, size.height * 0.2f)
                        lineTo(size.width * 0.65f, size.height * 0.5f)
                        lineTo(size.width * 0.35f, size.height * 0.8f)
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFF64748B),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
        }

        AppUndoDeleteBanner(viewModel = viewModel)
    }

    if (editingEntry != null) {
        val entry = editingEntry!!
        AlertDialog(
            onDismissRequest = { editingEntry = null },
            containerColor = DarkSurface,
            title = {
                Text(
                    text = "Edit Log Entry",
                    color = if (entry.type == EntryType.ANTIHISTAMINE) Color(0xFF737373)
                            else if (entry.type == EntryType.CORTISONE) PastelIceBlue
                            else PastelIceBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Date & Time",
                        color = LightGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = {
                            showDateTimePicker = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkCard),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = getFriendlyRelativeDateTime(editTimestamp),
                            color = if (entry.type == EntryType.ANTIHISTAMINE) Color(0xFF737373) else Color.White,
                            fontSize = 12.sp
                        )
                    }

                    if (entry.type == EntryType.ANTIHISTAMINE || entry.type == EntryType.CORTISONE) {
                        Text(
                            text = "Medication Details",
                            color = LightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        if (entry.type == EntryType.ANTIHISTAMINE && antihistamines.isNotEmpty()) {
                            Text(
                                text = "Choose from profile:",
                                color = MutedGray,
                                fontSize = 10.sp
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                antihistamines.forEach { med ->
                                    Card(
                                        onClick = {
                                            editMedName = med.name
                                            editMedMgs = med.mgs
                                        },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (editMedName == med.name && editMedMgs == med.mgs) Color(0xFF509729).copy(alpha = 0.2f) else DarkCard
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            if (editMedName == med.name && editMedMgs == med.mgs) Color(0xFF509729) else DarkBorder
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "${med.name} (${med.mgs} mg)",
                                                color = if (editMedName == med.name && editMedMgs == med.mgs) Color(0xFF509729) else Color(0xFF737373),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (entry.type == EntryType.CORTISONE && profileCortisones.isNotEmpty()) {
                            Text(
                                text = "Choose from profile:",
                                color = MutedGray,
                                fontSize = 10.sp
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                profileCortisones.forEach { med ->
                                    Card(
                                        onClick = {
                                            editMedName = med.name
                                            editMedMgs = med.mgs
                                        },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (editMedName == med.name && editMedMgs == med.mgs) PastelIceBlue.copy(alpha = 0.2f) else DarkCard
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            if (editMedName == med.name && editMedMgs == med.mgs) PastelIceBlue else DarkBorder
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "${med.name} (${med.mgs} mg)",
                                                color = if (editMedName == med.name && editMedMgs == med.mgs) PastelIceBlue else Color(0xFF737373),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            val query = editMedName.trim().lowercase()
                            val suggestions = remember(query) {
                                if (query.isEmpty() || entry.type != EntryType.ANTIHISTAMINE) emptyList() else {
                                    val list = MedicationDirectory.antihistamines
                                    fun normalize(s: String): String = s.replace("–", "-").replace("—", "-").trim().lowercase()
                                    val normalizedQuery = normalize(query)
                                    val exactMatches = list.filter { med ->
                                        val normalizedFull = normalize(med)
                                        val parts = med.split(" – ")
                                        val normalizedName = if (parts.isNotEmpty()) normalize(parts[0]) else ""
                                        normalizedFull == normalizedQuery || normalizedName == normalizedQuery
                                    }
                                    val startsWithMatches = list.filter { med ->
                                        if (exactMatches.contains(med)) return@filter false
                                        val normalizedFull = normalize(med)
                                        val parts = med.split(" – ")
                                        val normalizedName = if (parts.isNotEmpty()) normalize(parts[0]) else ""
                                        normalizedFull.startsWith(normalizedQuery) || normalizedName.startsWith(normalizedQuery)
                                    }
                                    val containsMatches = list.filter { med ->
                                        if (exactMatches.contains(med) || startsWithMatches.contains(med)) return@filter false
                                        val normalizedFull = normalize(med)
                                        normalizedFull.contains(normalizedQuery)
                                    }
                                    (exactMatches + startsWithMatches + containsMatches).take(8)
                                }
                            }

                            LaunchedEffect(suggestions) {
                                selectedIndex = -1
                            }

                            OutlinedTextField(
                                value = editMedName,
                                onValueChange = { 
                                    editMedName = it
                                    showAutocompleteSuggestions = true
                                },
                                label = { Text("Med Name", color = MutedGray, fontSize = 11.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                     focusedTextColor = Color(0xFF737373),
                                     unfocusedTextColor = Color(0xFF737373),
                                     focusedBorderColor = Color(0xFF509729),
                                     unfocusedBorderColor = DarkBorder,
                                     focusedContainerColor = DarkCard,
                                     unfocusedContainerColor = DarkCard
                                 ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { focusState ->
                                        showAutocompleteSuggestions = focusState.isFocused
                                    }
                                    .onKeyEvent { keyEvent ->
                                        if (showAutocompleteSuggestions && suggestions.isNotEmpty() && keyEvent.keyType == KeyEventType.KeyDown) {
                                            when (keyEvent.key) {
                                                Key.DirectionDown -> {
                                                    selectedIndex = (selectedIndex + 1) % suggestions.size
                                                    true
                                                }
                                                Key.DirectionUp -> {
                                                    selectedIndex = if (selectedIndex <= 0) suggestions.size - 1 else selectedIndex - 1
                                                    true
                                                }
                                                Key.Enter -> {
                                                    if (selectedIndex in suggestions.indices) {
                                                        editMedName = suggestions[selectedIndex]
                                                        showAutocompleteSuggestions = false
                                                        true
                                                    } else {
                                                        false
                                                    }
                                                }
                                                else -> false
                                            }
                                        } else {
                                            false
                                        }
                                    }
                            )
                            
                            if (showAutocompleteSuggestions && editMedName.trim().isNotEmpty()) {
                                Popup(
                                    onDismissRequest = { showAutocompleteSuggestions = false },
                                    properties = PopupProperties(focusable = false)
                                ) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp)),
                                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                                        shape = RoundedCornerShape(8.dp),
                                        elevation = CardDefaults.cardElevation(8.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 240.dp)
                                                .verticalScroll(rememberScrollState())
                                        ) {
                                            if (suggestions.isEmpty()) {
                                                Text(
                                                    text = "No medications found.\nNo matching medication found. You can enter it manually.",
                                                    color = MutedGray,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(12.dp)
                                                )
                                            } else {
                                                suggestions.forEachIndexed { index, med ->
                                                    val isSelected = index == selectedIndex
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(if (isSelected) Color(0xFF509729).copy(alpha = 0.2f) else Color.Transparent)
                                                            .clickable {
                                                                editMedName = med
                                                                showAutocompleteSuggestions = false
                                                            }
                                                            .padding(12.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        val lowercaseMed = med.lowercase()
                                                        if (lowercaseMed.contains(query)) {
                                                            val parts = mutableListOf<Pair<String, Boolean>>()
                                                            var searchIndex = 0
                                                            while (searchIndex < med.length) {
                                                                val idx = lowercaseMed.indexOf(query, searchIndex)
                                                                if (idx == -1) {
                                                                    parts.add(med.substring(searchIndex) to false)
                                                                    break
                                                                }
                                                                if (idx > searchIndex) {
                                                                    parts.add(med.substring(searchIndex, idx) to false)
                                                                }
                                                                parts.add(med.substring(idx, idx + query.length) to true)
                                                                searchIndex = idx + query.length
                                                            }
                                                            Text(
                                                                text = buildAnnotatedString {
                                                                    parts.forEach { (part, isHigh) ->
                                                                        if (isHigh) {
                                                                            withStyle(style = SpanStyle(color = Color(0xFF737373), fontWeight = FontWeight.Bold)) {
                                                                                append(part)
                                                                            }
                                                                        } else {
                                                                            withStyle(style = SpanStyle(color = Color(0xFF737373))) {
                                                                                append(part)
                                                                            }
                                                                        }
                                                                    }
                                                                },
                                                                fontSize = 12.sp
                                                            )
                                                        } else {
                                                            Text(text = med, color = Color(0xFF737373), fontSize = 12.sp)
                                                        }
                                                    }
                                                }
                                            }
                                            
                                            HorizontalDivider(color = DarkBorder)
                                            Text(
                                                text = "Can't find your medication? Continue typing to enter it manually.",
                                                color = PastelIceBlue.copy(alpha = 0.8f),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = editMedMgs,
                            onValueChange = { editMedMgs = it },
                            label = { Text("Dosage (mg)", color = MutedGray, fontSize = 11.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(
                                 focusedTextColor = Color(0xFF737373),
                                 unfocusedTextColor = Color(0xFF737373),
                                 focusedBorderColor = Color(0xFF509729),
                                 unfocusedBorderColor = DarkBorder,
                                 focusedContainerColor = DarkCard,
                                 unfocusedContainerColor = DarkCard
                             ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (entry.type == EntryType.ANTIHISTAMINE) {
                            Spacer(modifier = Modifier.height(8.dp))
                            if (editEffectStarted.isEmpty()) {
                                OutlinedButton(
                                    onClick = {
                                        val d1 = try { ZonedDateTime.parse(editTimestamp) } catch (e: Exception) { ZonedDateTime.now() }
                                        val d2 = ZonedDateTime.now()
                                        val duration = java.time.Duration.between(d1, d2)
                                        val diffMins = Math.max(0L, duration.toMinutes())
                                        editEffectStarted = if (diffMins < 60) {
                                            "$diffMins mins"
                                        } else {
                                            val hrs = diffMins / 60
                                            val mins = diffMins % 60
                                            if (mins > 0) "$hrs hrs $mins mins" else "$hrs hrs"
                                        }
                                    },
                                    border = BorderStroke(1.dp, Color(0xFF509729)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF509729)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(40.dp)
                                ) {
                                    Text("Medication Response Time", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = "Optional",
                                    color = MutedGray,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                )
                            } else {
                                OutlinedTextField(
                                    value = editEffectStarted,
                                    onValueChange = { editEffectStarted = it },
                                    label = { Text("Effect Started", color = MutedGray, fontSize = 11.sp) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color(0xFF737373),
                                        unfocusedTextColor = Color(0xFF737373),
                                        focusedBorderColor = Color(0xFF509729),
                                        unfocusedBorderColor = DarkBorder,
                                        focusedContainerColor = DarkCard,
                                        unfocusedContainerColor = DarkCard
                                    ),
                                    trailingIcon = {
                                        IconButton(onClick = { editEffectStarted = "" }) {
                                            Text(
                                                text = "✕",
                                                color = MutedGray,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val metadata = if (entry.type == EntryType.ANTIHISTAMINE || entry.type == EntryType.CORTISONE) {
                            if (editMedName.trim().isEmpty()) {
                                Toast.makeText(context, "Please enter medication name", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (editMedMgs.trim().isEmpty() || editMedMgs.trim().toDoubleOrNull() == null) {
                                Toast.makeText(context, "Please enter a valid dosage", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (entry.type == EntryType.ANTIHISTAMINE) {
                                val isExist = antihistamines.any { it.name.trim().lowercase() == editMedName.trim().lowercase() }
                                if (!isExist && editMedName.trim().isNotEmpty()) {
                                    viewModel.addProfileAntihistamine(editMedName.trim(), editMedMgs.trim(), "2nd", isMain = false)
                                }
                            } else if (entry.type == EntryType.CORTISONE) {
                                val isExist = profileCortisones.any { it.name.trim().lowercase() == editMedName.trim().lowercase() }
                                if (!isExist && editMedName.trim().isNotEmpty()) {
                                    viewModel.addProfileCortisone(editMedName.trim(), editMedMgs.trim())
                                }
                            }
                            val baseMeta = "${editMedName.trim()}:::${editMedMgs.trim()}"
                            if (entry.type == EntryType.ANTIHISTAMINE) {
                                "$baseMeta:::${editEffectStarted.trim()}"
                            } else {
                                val oldParts = (entry.metadata ?: "").split(":::")
                                val oldReason = if (oldParts.size >= 3) oldParts.drop(2).joinToString(":::") else ""
                                if (oldReason.isNotEmpty()) "$baseMeta:::$oldReason" else baseMeta
                            }
                        } else {
                            entry.metadata
                        }
                        viewModel.updateEntryDetails(entry.id, editTimestamp, metadata, force = false)
                        editingEntry = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (entry.type == EntryType.CORTISONE) PastelIceBlue
                                          else if (entry.type == EntryType.ANTIHISTAMINE) Color(0xFF737373)
                                          else PastelIceBlue,
                        contentColor = if (entry.type == EntryType.ANTIHISTAMINE) Color.White else AmoledBlack
                    )
                ) {
                    Text(text = "Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingEntry = null }) {
                    Text(text = "Cancel", color = MutedGray)
                }
            }
        )
    }

    if (showDateTimePicker) {
        DateTimePickerDialog(
            initialTimestamp = editTimestamp,
            onDismiss = { showDateTimePicker = false },
            onConfirm = { newTime ->
                editTimestamp = newTime
                showDateTimePicker = false
            }
        )
    }

    if (manualEntryCallback != null) {
        DateTimePickerDialog(
            initialTimestamp = ZonedDateTime.now().toString(),
            onDismiss = { manualEntryCallback = null },
            onConfirm = { newTime ->
                manualEntryCallback?.invoke(newTime)
                manualEntryCallback = null
            }
        )
    }
}
