package com.example.urticare20.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.urticare20.ui.theme.*

@Composable
fun VideoEducationScreen(onBack: () -> Unit) {
    val scrollState = rememberScrollState()
    val videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
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

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "📺 Video Channel",
                    color = SoftPurple,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
            ) {
                // Welcome and Introduction Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Welcome to UrtiCare Education",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Text(
                            text = "Watch this educational health stream to better understand the scientific mechanisms of urticaria, Mast Cell activation cycles, and clinical recommendations for long-term management.",
                            color = LightGray,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Video Player Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkCard)
                ) {
                    VideoPlayerComponent(
                        videoUrl = videoUrl,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    )
                }

                // Video Key Takeaways Section
                Text(
                    text = "🔑 Key Concepts in this Stream",
                    color = SoftPurple,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                // Bullet point 1
                TakeawayItem(
                    title = "Mast Cell Dynamics",
                    description = "Urticaria involves mast cells releasing histamine into tissue, causing localized swelling, pink/red hives, and irritation.",
                    color = CoralPink
                )

                // Bullet point 2
                TakeawayItem(
                    title = "Tracking Significance",
                    description = "Documenting flare-ups and daily medication helps target specific triggers and evaluates drug efficacy accurately.",
                    color = PastelIceBlue
                )

                // Bullet point 3
                TakeawayItem(
                    title = "Medication Adherence",
                    description = "Second-generation antihistamines form the first line of defense and are safest when taken consistently as directed.",
                    color = SoftYellow
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun TakeawayItem(title: String, description: String, color: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Colored Bullet indicator
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .offset(y = 4.dp)
                    .background(color, shape = CircleShape)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Text(
                    text = description,
                    color = MutedGray,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
