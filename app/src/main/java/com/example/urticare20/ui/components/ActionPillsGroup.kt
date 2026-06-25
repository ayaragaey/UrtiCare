package com.example.urticare20.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.urticare20.model.EntryType
import com.example.urticare20.ui.theme.AmoledBlack
import com.example.urticare20.ui.theme.CoralPink
import com.example.urticare20.ui.theme.PastelIceBlue
import com.example.urticare20.ui.theme.SoftYellow
import com.example.urticare20.viewmodel.TrackerViewModel
import java.time.ZonedDateTime
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.urticare20.ui.theme.DarkCard
import com.example.urticare20.ui.theme.DarkBorder
import com.example.urticare20.ui.theme.DarkSurface
import com.example.urticare20.ui.theme.LightGray
import com.example.urticare20.ui.theme.MutedGray

@Composable
fun ActionPillsGroup(viewModel: TrackerViewModel) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp

    val profileCortisones by viewModel.profileCortisones.collectAsState()
    var showCortisoneSelection by remember { mutableStateOf(false) }
    var isCustomMode by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }
    var customMgs by remember { mutableStateOf("") }
    var showReminderDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    var showFlareUpDialog by remember { mutableStateOf(false) }
    var isMildSelected by remember { mutableStateOf(true) }
    var isSevereSelected by remember { mutableStateOf(false) }
    var isAngioYesSelected by remember { mutableStateOf(false) }
    var isAngioNoSelected by remember { mutableStateOf(true) }
    val selectedReasons = remember { mutableStateListOf<String>() }
    val vegetablesText = remember { mutableStateOf("") }
    val fruitsText = remember { mutableStateOf("") }
    val illnessText = remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Flare Up Button (Coral Pink, Left)
        CircularActionPill(
            label = "Flare Up",
            subLabel = null,
            color = CoralPink,
            size = 90,
            onClick = {
                isMildSelected = true
                isSevereSelected = false
                isAngioYesSelected = false
                isAngioNoSelected = true
                selectedReasons.clear()
                showFlareUpDialog = true
            }
        )

        // 2. Antihistamine Button (Soft Yellow, Center Focal - scaled exactly 15% larger)
        CircularActionPill(
            label = "Antihistamine",
            subLabel = null,
            color = SoftYellow,
            size = 104,
            glow = true,
            onClick = {
                val mainMed = viewModel.profileAntihistamines.value.find { it.isMain }
                val metadata = if (mainMed != null) "${mainMed.name}:::${mainMed.mgs}" else null
                viewModel.addEntry(EntryType.ANTIHISTAMINE, ZonedDateTime.now().toString(), metadata = metadata)
            }
        )

        // 3. Cortisone Button (Pastel Ice Blue, Right)
        CircularActionPill(
            label = "Cortico-\nsteroids",
            subLabel = null,
            color = PastelIceBlue,
            size = 90,
            onClick = {
                isCustomMode = false
                customName = ""
                customMgs = ""
                showCortisoneSelection = true
            }
        )
    }

    if (showFlareUpDialog) {
        AlertDialog(
            onDismissRequest = { showFlareUpDialog = false },
            containerColor = DarkSurface,
            title = {
                Text(
                    text = "Log Symptom Flare Up",
                    color = CoralPink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Severity",
                            color = CoralPink,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(90.dp)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                isMildSelected = true
                                isSevereSelected = false
                            }
                        ) {
                            RadioButton(
                                selected = isMildSelected,
                                onClick = {
                                    isMildSelected = true
                                    isSevereSelected = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = CoralPink,
                                    unselectedColor = MutedGray
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Mild", color = Color(0xFF737373), fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                isMildSelected = false
                                isSevereSelected = true
                            }
                        ) {
                            RadioButton(
                                selected = isSevereSelected,
                                onClick = {
                                    isMildSelected = false
                                    isSevereSelected = true
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = CoralPink,
                                    unselectedColor = MutedGray
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Severe", color = Color(0xFF737373), fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Angioedema",
                            color = CoralPink,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(90.dp)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                isAngioYesSelected = true
                                isAngioNoSelected = false
                            }
                        ) {
                            RadioButton(
                                selected = isAngioYesSelected,
                                onClick = {
                                    isAngioYesSelected = true
                                    isAngioNoSelected = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = CoralPink,
                                    unselectedColor = MutedGray
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Yes", color = Color(0xFF737373), fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                isAngioYesSelected = false
                                isAngioNoSelected = true
                            }
                        ) {
                            RadioButton(
                                selected = isAngioNoSelected,
                                onClick = {
                                    isAngioYesSelected = false
                                    isAngioNoSelected = true
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = CoralPink,
                                    unselectedColor = MutedGray
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "No", color = Color(0xFF737373), fontSize = 12.sp)
                        }
                    }

                    Text(
                        text = "Potential Reasons (optional)",
                        color = CoralPink,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    ReasonsSearchableList(
                        selectedReasons = selectedReasons,
                        vegetablesText = vegetablesText,
                        fruitsText = fruitsText,
                        illnessText = illnessText,
                        viewModel = viewModel,
                        themeColor = CoralPink,
                        maxHeight = 360
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val severityStr = if (isSevereSelected) "Severity: Severe" else "Severity: Mild"
                        val angioStr = if (isAngioYesSelected) "Angioedema: Yes" else "Angioedema: No"
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
                                else -> finalReasons.add(r)
                            }
                        }
                        val reasonsStr = finalReasons.joinToString("; ")
                        val metadata = if (reasonsStr.isNotEmpty()) "$severityStr; $angioStr; $reasonsStr" else "$severityStr; $angioStr"
                        
                        viewModel.addEntry(EntryType.FLARE_UP, ZonedDateTime.now().toString(), metadata = metadata)
                        showFlareUpDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralPink, contentColor = AmoledBlack)
                ) {
                    Text(text = "Log Entry", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFlareUpDialog = false }) {
                    Text(text = "Cancel", color = MutedGray)
                }
            }
        )
    }

    if (showCortisoneSelection) {
        AlertDialog(
            onDismissRequest = { showCortisoneSelection = false },
            containerColor = DarkSurface,
            title = {
                Text(
                    text = if (isCustomMode) "Log Custom Cortisone" else "Select Corticosteroid",
                    color = SoftYellow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                if (!isCustomMode) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (profileCortisones.isEmpty()) {
                            Text(
                                text = "You haven't saved any Cortisone medications. Select 'Other' to log a custom one.",
                                color = MutedGray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        } else {
                            profileCortisones.forEach { med ->
                                Card(
                                    onClick = {
                                        val metadata = "${med.name}:::${med.mgs}"
                                        viewModel.addEntry(EntryType.CORTISONE, ZonedDateTime.now().toString(), metadata = metadata)
                                        showCortisoneSelection = false
                                        Toast.makeText(context, "Logged Cortisone: ${med.name} (${med.mgs} mg)", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                                    border = BorderStroke(1.dp, DarkBorder),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = med.name,
                                                color = Color(0xFF737373),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${med.mgs} mg",
                                                color = Color(0xFF737373),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Card(
                            onClick = { 
                                customName = ""
                                customMgs = ""
                                isCustomMode = true 
                            },
                            colors = CardDefaults.cardColors(containerColor = DarkCard),
                            border = BorderStroke(1.dp, SoftYellow.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "➕ Other",
                                    color = SoftYellow,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = customName,
                            onValueChange = { customName = it },
                            label = { Text("Medication Name (e.g. Prednisolone)", color = MutedGray, fontSize = 12.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = LightGray,
                                unfocusedTextColor = LightGray,
                                focusedBorderColor = SoftYellow,
                                unfocusedBorderColor = DarkBorder,
                                focusedContainerColor = DarkCard,
                                unfocusedContainerColor = DarkCard
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = customMgs,
                            onValueChange = { customMgs = it },
                            label = { Text("Dosage (mg)", color = MutedGray, fontSize = 12.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = LightGray,
                                unfocusedTextColor = LightGray,
                                focusedBorderColor = SoftYellow,
                                unfocusedBorderColor = DarkBorder,
                                focusedContainerColor = DarkCard,
                                unfocusedContainerColor = DarkCard
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "Note: This will log the intake and save this medication to your profile's Corticosteroids list.",
                            color = MutedGray,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                if (isCustomMode) {
                    Button(
                        onClick = {
                            if (customName.trim().isEmpty()) {
                                Toast.makeText(context, "Please enter medication name", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (customMgs.trim().isEmpty() || customMgs.trim().toDoubleOrNull() == null) {
                                Toast.makeText(context, "Please enter a valid dosage", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            // Save to profile
                            viewModel.addProfileCortisone(customName.trim(), customMgs.trim())

                            // Log intake
                            val metadata = "${customName.trim()}:::${customMgs.trim()}"
                            viewModel.addEntry(EntryType.CORTISONE, ZonedDateTime.now().toString(), metadata = metadata)

                            Toast.makeText(context, "Logged Cortisone: ${customName.trim()} (${customMgs.trim()} mg)", Toast.LENGTH_SHORT).show()

                            // Show reminder before closing selection box
                            showReminderDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SoftYellow, contentColor = AmoledBlack)
                    ) {
                        Text(text = "Log", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (isCustomMode) {
                            isCustomMode = false
                        } else {
                            showCortisoneSelection = false
                        }
                    }
                ) {
                    Text(text = if (isCustomMode) "Back" else "Cancel", color = MutedGray)
                }
            }
        )
    }

    if (showReminderDialog) {
        AlertDialog(
            onDismissRequest = { 
                showReminderDialog = false 
                showCortisoneSelection = false
            },
            containerColor = DarkSurface,
            title = {
                Text(
                    text = "Reminder",
                    color = SoftYellow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "Please remember to add this medication to your profile properly too to configure all details.",
                    color = LightGray,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { 
                        showReminderDialog = false 
                        showCortisoneSelection = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SoftYellow, contentColor = AmoledBlack)
                ) {
                    Text(text = "OK", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun CircularActionPill(
    label: String,
    subLabel: String?,
    color: Color,
    size: Int,
    glow: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Tactile press scale feedback: shrinks slightly by 8% on press, rebounds on release
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        label = "pill_press_scale"
    )

    Box(
        modifier = Modifier
            .size(size.dp)
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale
            )
            .shadow(
                elevation = if (glow) 8.dp else 4.dp,
                shape = CircleShape,
                ambientColor = if (glow) color else Color.Black,
                spotColor = if (glow) color else Color.Black
            )
            .clip(CircleShape)
            .background(color)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current, // Standard Material ripple
                onClick = onClick
            )
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                color = AmoledBlack,
                fontSize = if (size > 95) 12.sp else 11.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
            subLabel?.let {
                Text(
                    text = it,
                    color = AmoledBlack.copy(alpha = 0.8f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
        }
    }
}
