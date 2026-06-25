package com.example.urticare20.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.urticare20.model.EntryType
import com.example.urticare20.model.LogEntry
import com.example.urticare20.ui.theme.*
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.ZonedDateTime

@Composable
fun LiveCounter(entries: List<LogEntry>) {
    // Memoize the latest Antihistamine entry's timestamp.
    // This component will ONLY re-evaluate when the primary dose changes,
    // and the 1000ms ticking operates locally, preventing entire screen recomposition loops.
    val lastAHTimestamp = remember(entries) {
        entries.firstOrNull { it.type == EntryType.ANTIHISTAMINE }?.timestamp
    }

    var elapsedText by remember(lastAHTimestamp) {
        mutableStateOf(if (lastAHTimestamp != null) "0h 0m 0s" else "No dosage recorded")
    }

    LaunchedEffect(lastAHTimestamp) {
        if (lastAHTimestamp == null) return@LaunchedEffect
        while (true) {
            try {
                val lastDate = ZonedDateTime.parse(lastAHTimestamp)
                val duration = Duration.between(lastDate, ZonedDateTime.now())
                val secsTotal = duration.seconds
                
                if (secsTotal < 0) {
                    elapsedText = "0h 0m 0s"
                } else {
                    val hrs = secsTotal / 3600
                    val mins = (secsTotal % 3600) / 60
                    val secs = secsTotal % 60
                    elapsedText = "${hrs}h ${mins}m ${secs}s"
                }
            } catch (e: Exception) {
                elapsedText = "0h 0m 0s"
            }
            delay(1000)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = if (lastAHTimestamp != null) SoftYellow else MutedGray,
                            shape = RoundedCornerShape(4.dp)
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "LAST ANTIHISTAMINE WAS",
                    color = MutedGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }

            Text(
                text = elapsedText,
                color = if (lastAHTimestamp != null) SoftYellow else MutedGray,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            )
        }
    }
}
