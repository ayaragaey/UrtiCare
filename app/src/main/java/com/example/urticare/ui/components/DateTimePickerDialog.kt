package com.example.urticare.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun DateTimePickerDialog(
    initialTimestamp: String?,
    themeColor: Color = Color(0xFF814B92),
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val context = LocalContext.current
    val parsedDateTime = remember(initialTimestamp) {
        if (!initialTimestamp.isNullOrEmpty()) {
            try {
                ZonedDateTime.parse(initialTimestamp)
            } catch (e: Exception) {
                ZonedDateTime.now()
            }
        } else {
            ZonedDateTime.now()
        }
    }

    var selectedDate by remember { mutableStateOf(parsedDateTime.toLocalDate()) }
    var selectedTime by remember { mutableStateOf(parsedDateTime.toLocalTime()) }
    var selectedPresetLabel by remember { mutableStateOf<String?>("Now") }

    val formattedDate = remember(selectedDate) {
        selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM dd, yyyy"))
    }
    val formattedTime = remember(selectedTime) {
        selectedTime.format(DateTimeFormatter.ofPattern("hh:mm a"))
    }

    val purpleColor = themeColor
    val purpleBg = when (themeColor) {
        Color(0xFF1A7E97) -> Color(0xFFE6F4F8)
        Color(0xFFF78325) -> Color(0xFFFFEAD2)
        Color(0xFF4C9A2A) -> Color(0xFFE8F7EC)
        else -> Color(0xFFF6EEFA)
    }
    val textDark = Color(0xFF1F2937)
    val textMuted = Color(0xFF64748B)
    val borderColor = Color(0xFFE2E8F0)
    val fieldBg = Color(0xFFF8FAFC)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "📅",
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Select Date & Time",
                        color = textDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }

                // Smart Presets Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Smart Presets",
                        color = textMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    val now = ZonedDateTime.now()
                    val presets = remember(now) {
                        listOf(
                            PresetOption("Now", now),
                            PresetOption("15m ago", now.minusMinutes(15)),
                            PresetOption("30m ago", now.minusMinutes(30)),
                            PresetOption("1h ago", now.minusHours(1)),
                            PresetOption("2h ago", now.minusHours(2)),
                            PresetOption("Yesterday", now.minusDays(1))
                        )
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(76.dp)
                    ) {
                        items(presets) { preset ->
                            val isSelected = selectedPresetLabel == preset.label
                            Box(
                                modifier = Modifier
                                    .height(34.dp)
                                    .background(
                                        color = if (isSelected) purpleBg else Color.White,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) purpleColor else borderColor,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        selectedPresetLabel = preset.label
                                        selectedDate = preset.dateTime.toLocalDate()
                                        selectedTime = preset.dateTime.toLocalTime()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = preset.label,
                                    color = purpleColor,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = borderColor, thickness = 1.dp)

                // Date & Time Fields Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Date Field
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Date",
                            color = textMuted,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .background(fieldBg, RoundedCornerShape(10.dp))
                                .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                                .clickable {
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            selectedPresetLabel = null
                                            selectedDate = LocalDate.of(y, m + 1, d)
                                        },
                                        selectedDate.year,
                                        selectedDate.monthValue - 1,
                                        selectedDate.dayOfMonth
                                    ).show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = formattedDate,
                                color = textDark,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Time Field
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Time",
                            color = textMuted,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .background(fieldBg, RoundedCornerShape(10.dp))
                                .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                                .clickable {
                                    TimePickerDialog(
                                        context,
                                        { _, h, m ->
                                            selectedPresetLabel = null
                                            selectedTime = LocalTime.of(h, m)
                                        },
                                        selectedTime.hour,
                                        selectedTime.minute,
                                        false
                                    ).show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = formattedTime,
                                color = textDark,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Bottom Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Cancel",
                            color = textMuted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(
                        onClick = {
                            val finalZonedDateTime = ZonedDateTime.of(
                                selectedDate,
                                selectedTime,
                                parsedDateTime.zone
                            )
                            onConfirm(finalZonedDateTime.toString())
                        }
                    ) {
                        Text(
                            text = "Save",
                            color = purpleColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private data class PresetOption(
    val label: String,
    val dateTime: ZonedDateTime
)
