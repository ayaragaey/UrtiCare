package com.example.urticare.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.urticare.R

@Composable
fun ProfileCompletionReminderDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onCompleteProfile: () -> Unit
) {
    if (!visible) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 16.dp)
                .shadow(16.dp, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Close 'X' Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xFFF1F5F9), shape = CircleShape)
                            .clip(CircleShape)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✕",
                            color = Color(0xFF64748B),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Friendly healthcare illustration
                Image(
                    painter = painterResource(id = R.drawable.onboarding_illustration),
                    contentDescription = "Profile Completion Illustration",
                    modifier = Modifier
                        .height(150.dp)
                        .fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Title: Complete your profile (with "your profile" highlighted in green)
                val titleAnnotated = buildAnnotatedString {
                    append("Complete ")
                    withStyle(
                        SpanStyle(
                            color = Color(0xFF388E3C),
                            fontWeight = FontWeight.ExtraBold
                        )
                    ) {
                        append("your profile")
                    }
                }

                Text(
                    text = titleAnnotated,
                    color = Color(0xFF0F172A),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Subtitle
                Text(
                    text = "Set up your profile to get the most out of UrtiCare and make tracking your health journey easier.",
                    color = Color(0xFF64748B),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Benefits
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Benefit 1
                    BenefitCard(
                        iconRes = R.drawable.ic_onboarding_profile,
                        iconTint = Color(0xFF814B92),
                        title = "Personalized health tracking",
                        desc = "Track your symptoms, medications, and condition based on your information."
                    )

                    // Benefit 2
                    BenefitCard(
                        iconRes = R.drawable.ic_onboarding_insights,
                        iconTint = Color(0xFF814B92),
                        title = "Better pattern insights",
                        desc = "Understand your symptoms and triggers through your logged health patterns."
                    )

                    // Benefit 3
                    BenefitCard(
                        iconRes = R.drawable.ic_onboarding_lightning,
                        iconTint = Color(0xFF814B92),
                        title = "Quick logging experience",
                        desc = "Easily record medications, flare-ups, and daily updates faster."
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Button(
                    onClick = onCompleteProfile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Complete your profile",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    border = BorderStroke(1.5.dp, Color(0xFF388E3C)),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Later",
                        color = Color(0xFF388E3C),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun BenefitCard(
    iconRes: Int,
    iconTint: Color,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF4FBF7), shape = RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFE8F5E9), shape = RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color(0xFFE8F5E9), shape = CircleShape)
                .clip(CircleShape)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                colorFilter = ColorFilter.tint(iconTint),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color(0xFF0F172A),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                color = Color(0xFF64748B),
                fontSize = 11.5.sp,
                lineHeight = 15.sp
            )
        }
    }
}
