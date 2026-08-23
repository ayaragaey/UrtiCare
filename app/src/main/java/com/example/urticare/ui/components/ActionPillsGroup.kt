package com.example.urticare.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.urticare.model.EntryType
import com.example.urticare.R
import com.example.urticare.ui.theme.*
import com.example.urticare.viewmodel.TrackerViewModel
import java.time.ZonedDateTime
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.ColorFilter

@Composable
fun ActionPillsGroup(
    viewModel: TrackerViewModel,
    onOpenAddConsumption: () -> Unit = {}
) {
    val antihistamines by viewModel.profileAntihistamines.collectAsState()
    var showFlareChoiceDialog by remember { mutableStateOf(false) }
    var showMedicationChoiceDialog by remember { mutableStateOf(false) }
    var showConsumptionsDialog by remember { mutableStateOf(false) }
    var customConsumptionText by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Flare Up Action (Purple)
        QuickActionButton(
            modifier = Modifier.weight(1f),
            label = "Flare Up",
            imageRes = R.drawable.ic_btn_flareup_new,
            borderColor = Color(0xFFE2D8EC), // Subtle purple border to match design
            onClick = {
                showFlareChoiceDialog = true
            }
        )

        // 2. Medication Action (Green)
        QuickActionButton(
            modifier = Modifier.weight(1f),
            label = "Medication",
            imageRes = R.drawable.ic_btn_medication_new,
            borderColor = Color(0xFFD1EAD6), // Subtle green border to match design
            onClick = {
                showMedicationChoiceDialog = true
            }
        )

        // 3. Consumptions Action (Orange/Peach Pastel)
        QuickActionButton(
            modifier = Modifier.weight(1f),
            label = "Consumptions",
            imageRes = R.drawable.ic_btn_consumptions_new,
            borderColor = Color(0xFFFBE1CD), // Subtle orange border to match design
            onClick = {
                onOpenAddConsumption()
            }
        )
    }

    if (showFlareChoiceDialog) {
        Dialog(onDismissRequest = { showFlareChoiceDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight(),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Option 1: Flare Up outline card button
                    Card(
                        onClick = {
                            showFlareChoiceDialog = false
                            viewModel.addEntry(EntryType.FLARE_UP, ZonedDateTime.now().toString())
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        border = BorderStroke(1.5.dp, PrimaryPurple),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Flare Up",
                                color = PrimaryPurple,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Option 2: Angioedema Only outline card button
                    Card(
                        onClick = {
                            showFlareChoiceDialog = false
                            viewModel.addEntry(EntryType.FLARE_UP, ZonedDateTime.now().toString(), "Severity: Mild; Angioedema: Yes; Title: Angioedema")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        border = BorderStroke(1.5.dp, PrimaryPurple),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Angioedema Only",
                                color = PrimaryPurple,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    if (showMedicationChoiceDialog) {
        Dialog(onDismissRequest = { showMedicationChoiceDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight(),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Option 1: Antihistamine outline card button
                    Card(
                        onClick = {
                            showMedicationChoiceDialog = false
                            val mainAntihistamine = antihistamines.find { it.isMain } ?: antihistamines.firstOrNull()
                            val metadata = mainAntihistamine?.let { "${it.name}:::${it.mgs}" }
                            viewModel.addEntry(EntryType.ANTIHISTAMINE, ZonedDateTime.now().toString(), metadata)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        border = BorderStroke(1.5.dp, SuccessGreen),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Antihistamine",
                                color = SuccessGreen,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Option 2: Corticosteroids outline card button
                    Card(
                        onClick = {
                            showMedicationChoiceDialog = false
                            viewModel.addEntry(EntryType.CORTISONE, ZonedDateTime.now().toString())
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        border = BorderStroke(1.5.dp, SecondaryTeal),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Corticosteroids",
                                color = SecondaryTeal,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    if (showConsumptionsDialog) {
        AlertDialog(
            onDismissRequest = { showConsumptionsDialog = false },
            containerColor = DarkSurface,
            title = {
                Text(
                    text = "Log Consumption",
                    color = Color(0xFFF97316),
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
                        text = "Select a quick option or enter custom consumption below:",
                        color = LightGray,
                        fontSize = 12.sp
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("Spicy Food", "Seafood", "Coffee").forEach { opt ->
                                Card(
                                    onClick = {
                                        showConsumptionsDialog = false
                                        viewModel.addEntry(
                                            EntryType.ALTERNATIVE,
                                            ZonedDateTime.now().toString(),
                                            "Consumption: $opt"
                                        )
                                    },
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF262626)),
                                    border = BorderStroke(1.dp, Color(0xFFF97316).copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = opt,
                                            color = Color(0xFFF97316),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("Tea", "Alcohol", "Dairy").forEach { opt ->
                                Card(
                                    onClick = {
                                        showConsumptionsDialog = false
                                        viewModel.addEntry(
                                            EntryType.ALTERNATIVE,
                                            ZonedDateTime.now().toString(),
                                            "Consumption: $opt"
                                        )
                                    },
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF262626)),
                                    border = BorderStroke(1.dp, Color(0xFFF97316).copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = opt,
                                            color = Color(0xFFF97316),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    androidx.compose.material3.OutlinedTextField(
                        value = customConsumptionText,
                        onValueChange = { customConsumptionText = it },
                        placeholder = { Text("Enter food, drink, or trigger...", color = MutedGray, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                        singleLine = true,
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFF97316),
                            unfocusedBorderColor = Color(0xFF374151),
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF1E293B)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customConsumptionText.isNotBlank()) {
                            showConsumptionsDialog = false
                            viewModel.addEntry(
                                EntryType.ALTERNATIVE,
                                ZonedDateTime.now().toString(),
                                "Consumption: ${customConsumptionText.trim()}"
                            )
                            customConsumptionText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316), contentColor = Color.White)
                ) {
                    Text("Log", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showConsumptionsDialog = false
                        customConsumptionText = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = MutedGray)
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun QuickActionButton(
    modifier: Modifier = Modifier,
    label: String,
    imageRes: Int,
    borderColor: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .heightIn(min = 100.dp)
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(Color.White, RoundedCornerShape(12.dp))
                .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = label,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            color = Color(0xFF0F172A),
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun SelectionOptionCard(
    title: String,
    description: String,
    iconRes: Int,
    themeColor: Color,
    backgroundColor: Color,
    borderColor: Color,
    iconContainerColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon container
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(iconContainerColor, shape = RoundedCornerShape(12.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    colorFilter = ColorFilter.tint(themeColor),
                    contentScale = ContentScale.Fit
                )
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            
            // Title & Description
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    color = themeColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    color = Color(0xFF64748B),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Normal
                )
            }
            
            // Chevron arrow
            Canvas(modifier = Modifier.size(14.dp)) {
                val path = Path().apply {
                    moveTo(size.width * 0.3f, size.height * 0.2f)
                    lineTo(size.width * 0.7f, size.height * 0.5f)
                    lineTo(size.width * 0.3f, size.height * 0.8f)
                }
                drawPath(
                    path = path,
                    color = themeColor,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}

