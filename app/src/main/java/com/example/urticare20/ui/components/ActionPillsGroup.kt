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
import com.example.urticare20.ui.theme.AlertRed
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush

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
    var isAngioedemaOnlySelected by remember { mutableStateOf(false) }
    var selectedSeverity by remember { mutableStateOf("Mild") }
    var showSeverityLevelsInfo by remember { mutableStateOf(false) }
    var isAngioYesSelected by remember { mutableStateOf(false) }
    var isAngioNoSelected by remember { mutableStateOf(true) }
    val selectedReasons = remember { mutableStateListOf<String>() }
    val vegetablesText = remember { mutableStateOf("") }
    val fruitsText = remember { mutableStateOf("") }
    val illnessText = remember { mutableStateOf("") }
    val medicineOtherNameText = remember { mutableStateOf("") }
    val medicineOtherCauseText = remember { mutableStateOf("") }
    val insectText = remember { mutableStateOf("") }

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
                selectedSeverity = "Mild"
                isAngioYesSelected = false
                isAngioNoSelected = true
                isAngioedemaOnlySelected = false
                selectedReasons.clear()
                vegetablesText.value = ""
                fruitsText.value = ""
                illnessText.value = ""
                medicineOtherNameText.value = ""
                medicineOtherCauseText.value = ""
                insectText.value = ""
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
                    ?: viewModel.profileAntihistamines.value.firstOrNull()
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
                    text = if (isAngioedemaOnlySelected) "Log Angioedema" else "Log Symptom Flare Up",
                    color = Color(0xFF1A7E97),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Symptom Metrics Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        border = BorderStroke(1.dp, DarkBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Severity Dropdown Row
                            var severityExpanded by remember { mutableStateOf(false) }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Severity",
                                    color = Color(0xFF1A7E97),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(85.dp)
                                )
                                Box {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .background(DarkSurface, shape = RoundedCornerShape(8.dp))
                                            .border(1.dp, DarkBorder, shape = RoundedCornerShape(8.dp))
                                            .clickable { severityExpanded = true }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = selectedSeverity,
                                            color = Color(0xFF737373),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "▼",
                                            color = Color(0xFF1A7E97),
                                            fontSize = 10.sp
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = severityExpanded,
                                        onDismissRequest = { severityExpanded = false },
                                        modifier = Modifier.background(DarkSurface)
                                    ) {
                                        listOf("Mild", "Moderate", "Severe", "Critical").forEach { option ->
                                            DropdownMenuItem(
                                                text = { Text(option, color = Color(0xFF737373), fontSize = 12.sp) },
                                                onClick = {
                                                    selectedSeverity = option
                                                    severityExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Understand Severity Levels link
                            Text(
                                text = "Understand Severity Levels",
                                color = Color(0xFF1A7E97),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                style = androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline),
                                modifier = Modifier
                                    .padding(start = 85.dp)
                                    .clickable { showSeverityLevelsInfo = true }
                            )

                            // Critical Warning
                            if (selectedSeverity == "Critical") {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = AlertRed.copy(alpha = 0.15f)),
                                    border = BorderStroke(1.dp, AlertRed),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "🚨 WARNING: Please seek immediate emergency medical care if you are experiencing swelling of deep skin tissue (face, lips, tongue, throat) or any difficulty breathing or swallowing.",
                                        color = AlertRed,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }

                            // Angioedema Only Checkbox Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        isAngioedemaOnlySelected = !isAngioedemaOnlySelected 
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isAngioedemaOnlySelected,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFF1A7E97),
                                        uncheckedColor = MutedGray,
                                        checkmarkColor = AmoledBlack
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Angioedema Only",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if (!isAngioedemaOnlySelected) {
                                // Angioedema Radio Button Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Angioedema",
                                        color = Color(0xFF1A7E97),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(85.dp)
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
                                            modifier = Modifier.size(20.dp),
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = Color(0xFF1A7E97),
                                                unselectedColor = MutedGray
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "Yes", color = Color(0xFF737373), fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
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
                                            modifier = Modifier.size(20.dp),
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = Color(0xFF1A7E97),
                                                unselectedColor = MutedGray
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "No", color = Color(0xFF737373), fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Potential Reasons Divider/Title
                    Text(
                        text = "Potential Reasons (optional)",
                        color = Color(0xFF1A7E97),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
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
                        maxHeight = 360
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val severityStr = "Severity: $selectedSeverity"
                        val angioStr = if (isAngioedemaOnlySelected || isAngioYesSelected) "Angioedema: Yes" else "Angioedema: No"
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
                        val reasonsStr = finalReasons.joinToString("; ")
                        val metadata = if (isAngioedemaOnlySelected) {
                            if (reasonsStr.isNotEmpty()) "$severityStr; Angioedema: Yes; Title: Angioedema; $reasonsStr" else "$severityStr; Angioedema: Yes; Title: Angioedema"
                        } else {
                            if (reasonsStr.isNotEmpty()) "$severityStr; $angioStr; $reasonsStr" else "$severityStr; $angioStr"
                        }
                        
                        viewModel.addEntry(EntryType.FLARE_UP, ZonedDateTime.now().toString(), metadata = metadata)
                        showFlareUpDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A7E97), contentColor = Color.White)
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

    if (showSeverityLevelsInfo) {
        AlertDialog(
            onDismissRequest = { showSeverityLevelsInfo = false },
            containerColor = DarkSurface,
            title = {
                Text("Severity Levels", color = LightGray, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🟢 1. Mild Flare-up\nHives Count: Fewer than 20 individual hives/wheals over a 24-hour period.\nItch Severity: Mild, noticeable but not troublesome or annoying.\nImpact: No interference with sleep or daily activities.",
                        color = Color(0xFF737373),
                        fontSize = 12.sp
                    )
                    Text(
                        text = "🟡 2. Moderate Flare-up\nHives Count: Between 20 and 50 hives/wheals over a 24-hour period.\nItch Severity: Troublesome and annoying, but manageable.\nImpact: Mildly distracting, but does not prevent sleep or routine daily activities.",
                        color = Color(0xFF737373),
                        fontSize = 12.sp
                    )
                    Text(
                        text = "🔴 3. Severe Flare-up\nHives Count: More than 50 hives/wheals (or large confluent/merged areas of hives).\nItch Severity: Intense, distressing, and difficult to ignore.\nImpact: Severely interferes with daily tasks and causes sleep deprivation.",
                        color = Color(0xFF737373),
                        fontSize = 12.sp
                    )
                    Text(
                        text = "🚨 4. Critical (Emergency/Angioedema)\nSymptoms: Swelling of deep skin tissue (face, lips, tongue, or throat).\nImpact: Any difficulty breathing, swallowing, or voice changes.",
                        color = Color(0xFF737373),
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSeverityLevelsInfo = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A7E97), contentColor = Color.White)
                ) {
                    Text("Got it 👍", fontWeight = FontWeight.Bold)
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

    // Compute top (lighter) and bottom (darker) gradient colors for the 3D dome look
    val topGradientColor = Color(
        red = (color.red + (1f - color.red) * 0.22f).coerceIn(0f, 1f),
        green = (color.green + (1f - color.green) * 0.22f).coerceIn(0f, 1f),
        blue = (color.blue + (1f - color.blue) * 0.22f).coerceIn(0f, 1f)
    )
    val bottomGradientColor = Color(
        red = (color.red * 0.72f).coerceIn(0f, 1f),
        green = (color.green * 0.72f).coerceIn(0f, 1f),
        blue = (color.blue * 0.72f).coerceIn(0f, 1f)
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
            // 3D Dome Gradient
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(topGradientColor, bottomGradientColor)
                )
            )
            // Outer 3D Bevel Highlight
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(Color.White.copy(alpha = 0.45f), Color.Black.copy(alpha = 0.35f))
                ),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Increased ring size factor from 0.76 to 0.85 to provide more space for text
        val ringSize = (size * 0.85).dp

        // 1. Ring Shadow (shifted slightly down-right)
        Box(
            modifier = Modifier
                .size(ringSize)
                .offset(x = 0.6.dp, y = 1.2.dp)
                .border(
                    width = 2.dp,
                    color = Color.Black.copy(alpha = 0.18f),
                    shape = CircleShape
                )
        )

        // 2. White Ring (contains text)
        Box(
            modifier = Modifier
                .size(ringSize)
                .border(
                    width = 2.2.dp,
                    color = Color.White,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(4.dp)
            ) {
                Text(
                    text = label,
                    color = Color.White, // Pure white for perfect readability in the dome
                    fontSize = if (size > 95) 10.5.sp else 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    lineHeight = if (size > 95) 12.sp else 10.5.sp
                )
                subLabel?.let {
                    Text(
                        text = it,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
            }
        }
    }
}
