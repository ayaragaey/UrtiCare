package com.example.urticare20.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.urticare20.ui.theme.AmoledBlack
import com.example.urticare20.ui.theme.DarkCard
import com.example.urticare20.ui.theme.DarkSurface
import com.example.urticare20.ui.theme.DarkBorder
import com.example.urticare20.ui.theme.LightGray
import com.example.urticare20.ui.theme.SoftPurple

@Composable
fun PatternAnalysisScreen(onBack: () -> Unit = {}) {
    BackHandler(enabled = true) {
        onBack()
    }

    var isExpanded by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            // Header Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp)
            ) {
                // Back Button
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .background(DarkCard, shape = CircleShape)
                        .border(1.dp, DarkBorder, CircleShape)
                        .size(40.dp)
                ) {
                    Text(
                        text = "←",
                        color = LightGray,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Centered Title
                Text(
                    text = "📊 Pattern Analysis",
                    color = SoftPurple,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Expandable/Collapsible Description Card
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopEnd
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(bottom = 16.dp)
                        .border(
                            width = 1.dp,
                            color = DarkBorder,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header Clickable Row to toggle expansion
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isExpanded = !isExpanded }
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "💡",
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "What is Pattern Analysis?",
                                    color = LightGray,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Text(
                                text = if (isExpanded) "▲" else "▼",
                                color = LightGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = DarkBorder
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "This feature helps you identify patterns behind your flare-ups by tracking consumptions, medications, and health events. It analyzes possible links between flare-ups, streak interruptions, short intervals between antihistamine doses, and recurring corticosteroid use to help you better understand potential triggers.",
                                color = Color(0xFF737373),
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }

            // Central content placeholder (leave it empty now)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                // Left completely empty as requested
            }
        }
    }
}
