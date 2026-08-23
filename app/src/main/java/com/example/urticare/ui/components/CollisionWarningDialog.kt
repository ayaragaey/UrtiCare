package com.example.urticare.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import com.example.urticare.model.EntryType
import com.example.urticare.ui.theme.AmoledBlack
import com.example.urticare.ui.theme.CoralPink
import com.example.urticare.ui.theme.DarkSurface
import com.example.urticare.ui.theme.LightGray
import com.example.urticare.ui.theme.MutedGray
import com.example.urticare.viewmodel.TrackerViewModel

@Composable
fun CollisionWarningDialog(viewModel: TrackerViewModel) {
    val warning = viewModel.collisionWarning.value ?: return

    val friendlyName = when (warning.type) {
        EntryType.FLARE_UP -> "Symptom Flare-up"
        EntryType.ANTIHISTAMINE -> "Antihistamine Intake"
        EntryType.CORTISONE -> "Cortisone/Steroid Dose"
        EntryType.XOLAIR_150 -> "Xolair 150 mg Injection"
        EntryType.XOLAIR_300 -> "Xolair 300 mg Injection"
        EntryType.ALTERNATIVE -> "Alternative Medication Intake"
        EntryType.CONSUMPTION -> "Consumption Entry"
    }

    AlertDialog(
        onDismissRequest = { viewModel.dismissCollision() },
        containerColor = DarkSurface,
        title = {
            Text(
                text = "TEMPORAL COLLISION",
                color = CoralPink,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "You already logged a $friendlyName inside this exact minute timeframe. Would you like to automatically merge this timestamp and overwrite the duplicate?",
                color = LightGray
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (warning.isEdit && warning.entryId != null) {
                        viewModel.updateEntry(warning.entryId, warning.timestamp, force = true)
                    } else {
                        viewModel.addEntry(warning.type, warning.timestamp, force = true)
                    }
                    viewModel.dismissCollision()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CoralPink,
                    contentColor = AmoledBlack
                )
            ) {
                Text(text = "Merge Entries", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = { viewModel.dismissCollision() }
            ) {
                Text(text = "Cancel", color = MutedGray)
            }
        }
    )
}
