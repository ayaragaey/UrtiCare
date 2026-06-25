package com.example.urticare20

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.example.urticare20.ui.components.*
import com.example.urticare20.ui.theme.*
import com.example.urticare20.viewmodel.TrackerViewModel
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.example.urticare20.model.LogEntry
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.graphics.toColorInt
import androidx.activity.compose.BackHandler

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: TrackerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Retrieve ViewModel securely using standard Android ViewModelProvider
        viewModel = ViewModelProvider(this)[TrackerViewModel::class.java]

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }

        setContent {
            Urticare20Theme {
                var showSplashScreen by remember { mutableStateOf(true) }

                if (showSplashScreen) {
                    VideoSplashScreen(onVideoFinished = { showSplashScreen = false })
                } else {
                    val entries by viewModel.entries.collectAsState(initial = emptyList())
                    val scrollState = rememberScrollState()
                    var selectedTab by remember { mutableIntStateOf(0) } // Default to "Home"
                    var showPatternAnalysis by remember { mutableStateOf(false) }

                    LaunchedEffect(selectedTab) {
                        showPatternAnalysis = false
                    }

                    BackHandler(enabled = selectedTab == 1 && !showPatternAnalysis) {
                        selectedTab = 0
                    }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = AmoledBlack,
                        bottomBar = {
                            NavigationBar(
                                containerColor = DarkSurface,
                                contentColor = LightGray
                            ) {
                                NavigationBarItem(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 },
                                    icon = { Text("🏠", fontSize = 20.sp) },
                                    label = { Text("Home") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = PastelIceBlue,
                                        selectedTextColor = PastelIceBlue,
                                        unselectedIconColor = MutedGray,
                                        unselectedTextColor = MutedGray,
                                        indicatorColor = DarkCard
                                    )
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 },
                                    icon = { Text("📋", fontSize = 20.sp) },
                                    label = { Text("Insights") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = PastelIceBlue,
                                        selectedTextColor = PastelIceBlue,
                                        unselectedIconColor = MutedGray,
                                        unselectedTextColor = MutedGray,
                                        indicatorColor = DarkCard
                                    )
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 2,
                                    onClick = { selectedTab = 2 },
                                    icon = {
                                        Image(
                                            painter = painterResource(id = R.drawable.urti_avatar),
                                            contentDescription = "Talk to Urti",
                                            modifier = Modifier.size(36.dp)
                                        )
                                    },
                                    label = { Text("Talk to Urti") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = PastelIceBlue,
                                        selectedTextColor = PastelIceBlue,
                                        unselectedIconColor = MutedGray,
                                        unselectedTextColor = MutedGray,
                                        indicatorColor = DarkCard
                                    )
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 3,
                                    onClick = { selectedTab = 3 },
                                    icon = { Text("📚", fontSize = 20.sp) },
                                    label = { Text("Library") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = PastelIceBlue,
                                        selectedTextColor = PastelIceBlue,
                                        unselectedIconColor = MutedGray,
                                        unselectedTextColor = MutedGray,
                                        indicatorColor = DarkCard
                                    )
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            when (selectedTab) {
                                0 -> {
                                    // Home Tab (Quick tracker logging actions and overview)
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(AmoledBlack)
                                            .verticalScroll(scrollState)
                                            .padding(bottom = 32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                             modifier = Modifier
                                                 .fillMaxWidth()
                                                 .statusBarsPadding()
                                                 .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                                         ) {
                                             // Brand Logo in the exact center (expanded)
                                             Image(
                                                 painter = painterResource(id = R.drawable.logo),
                                                 contentDescription = "Urticare Logo",
                                                 modifier = Modifier
                                                     .height(160.dp)
                                                     .width(270.dp)
                                                     .align(Alignment.Center),
                                                 contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                             )

                                             // Profile button Column on the right of the logo
                                             Column(
                                                 horizontalAlignment = Alignment.CenterHorizontally,
                                                 modifier = Modifier
                                                     .align(Alignment.CenterEnd)
                                                     .clickable { selectedTab = 4 }
                                                     .padding(top = 4.dp, bottom = 4.dp)
                                             ) {
                                                 Box(
                                                     modifier = Modifier
                                                         .size(44.dp)
                                                         .background(DarkCard, shape = CircleShape)
                                                         .border(1.dp, DarkBorder, CircleShape),
                                                     contentAlignment = Alignment.Center
                                                 ) {
                                                     Text(
                                                         text = "👤",
                                                         color = PastelIceBlue,
                                                         fontSize = 20.sp
                                                     )
                                                 }
                                                 Spacer(modifier = Modifier.height(4.dp))
                                                 Text(
                                                     text = "Profile",
                                                     color = PastelIceBlue,
                                                     fontSize = 11.sp,
                                                     fontWeight = FontWeight.Medium
                                                 )
                                             }
                                         }

                                        // Circular Action Pills Group
                                        ActionPillsGroup(viewModel = viewModel)

                                        // Isolated High-Frequency ticking Live Counter
                                        LiveCounter(entries = entries)

                                        // Milestone Progress Banner directly beneath LiveCounter
                                        MilestoneProgressBanner(viewModel = viewModel, entries = entries)

                                        // Recent Intake Log (Simple list showing last 4 pill intakes, colored blue/yellow/red, showing gaps)
                                        RecentIntakeLog(viewModel = viewModel, entries = entries)

                                        // Collapsible Analytics drawer
                                        MedicationSummaryDrawer(viewModel = viewModel, entries = entries)
                                    }
                                }
                                1 -> {
                                    // Insights Tab (Comprehensive tabbed log manager)
                                    if (showPatternAnalysis) {
                                        PatternAnalysisScreen(onBack = { showPatternAnalysis = false })
                                    } else {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(AmoledBlack)
                                                .verticalScroll(rememberScrollState())
                                                .padding(bottom = 32.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Spacer(modifier = Modifier.height(16.dp))
                                            HistoryLogList(
                                                viewModel = viewModel,
                                                entries = entries,
                                                onPatternAnalysisClick = { showPatternAnalysis = true },
                                                onBack = { selectedTab = 0 }
                                            )
                                        }
                                    }
                                }
                                2 -> {
                                    // Talk to Urti Tab (Empathetic chatbot guardian screen)
                                    UrtiChatScreen(viewModel = viewModel, onBack = { selectedTab = 0 })
                                }
                                3 -> {
                                    // Library Tab (Placeholder for future development)
                                    LibraryScreen(onBack = { selectedTab = 0 })
                                }
                                4 -> {
                                    // Profile Screen (opened via header button)
                                    ProfileScreen(viewModel = viewModel, onBack = { selectedTab = 0 })
                                }
                            }

                            // Milestone alert overlay at the top
                            MilestoneAlertOverlay(viewModel = viewModel)

                            // Floating Undo Delete Banner
                            UndoDeleteBanner(viewModel = viewModel)
                        }

                        // Duplicate minute collision warning alert dialog
                        CollisionWarningDialog(viewModel = viewModel)

                        // Streak break warning dialog
                        StreakBreakDialog(viewModel = viewModel)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) {
            viewModel.loadEntries()
        }
    }
}

@Composable
fun MilestoneProgressBanner(viewModel: TrackerViewModel, entries: List<LogEntry>) {
    val adherenceStreak by viewModel.adherenceStreakHours.collectAsState()
    val remissionStreak by viewModel.remissionStreakDays.collectAsState()

    val stabilityText = if (adherenceStreak != null) {
        val lastBrokenHours = com.example.urticare20.util.MilestoneEvaluator.getLastBrokenMilestoneHours(entries)

        if (adherenceStreak!! < 48L && lastBrokenHours != null && lastBrokenHours >= 48L) {
            val days = lastBrokenHours / 24
            val rem = lastBrokenHours % 24
            val daysHoursStr = if (rem > 0L) "$days Days $rem Hours" else "$days Days"
            "Last Milestone: $daysHoursStr"
        } else if (adherenceStreak!! <= 48L) {
            "$adherenceStreak Hours Stable"
        } else {
            val days = adherenceStreak!! / 24
            val rem = adherenceStreak!! % 24
            if (rem > 0L) "$days Days $rem Hours Stable" else "$days Days Stable"
        }
    } else {
        "-- Hours Stable"
    }

    val remissionText = if (remissionStreak != null) {
        val lastBrokenRemissionDays = com.example.urticare20.util.MilestoneEvaluator.getLastBrokenRemissionDays(entries)

        if (remissionStreak!! < 7L && lastBrokenRemissionDays != null && lastBrokenRemissionDays >= 7L) {
            "Last Remission: $lastBrokenRemissionDays Days"
        } else {
            "$remissionStreak Days Remission"
        }
    } else {
        "-- Days Remission"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(AmoledBlack),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "💎 $stabilityText  |  $remissionText 💎",
            color = Color(0xFF737373),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MilestoneAlertOverlay(viewModel: TrackerViewModel) {
    val activeAlerts by viewModel.activeMilestoneAlerts.collectAsState()
    val alert = activeAlerts.firstOrNull()

    if (alert != null) {
        val themeColor = Color(alert.borderHex.toColorInt())
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { viewModel.dismissMilestoneAlert(alert.id) }
        ) {
            val scale = remember { androidx.compose.animation.core.Animatable(0.5f) }
            LaunchedEffect(alert) {
                scale.animateTo(
                    targetValue = 1.0f,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                    )
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .graphicsLayer(
                        scaleX = scale.value,
                        scaleY = scale.value
                    )
                    .border(
                        width = 2.dp,
                        color = themeColor,
                        shape = RoundedCornerShape(16.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = AmoledBlack),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Celebrating Icon/Emoji Section
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(110.dp)
                            .padding(bottom = 12.dp)
                    ) {
                        // Glowing Background
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                        colors = listOf(themeColor.copy(alpha = 0.25f), Color.Transparent)
                                    )
                                )
                        )
                        // Central Emoji
                        Text(
                            text = if (alert.type == com.example.urticare20.viewmodel.MilestoneType.ADHERENCE) "🏆" else "💎",
                            fontSize = 48.sp
                        )
                        // Celebrating sparkles and confetti
                        Text(
                            text = "✨",
                            fontSize = 18.sp,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(x = 10.dp, y = 10.dp)
                        )
                        Text(
                            text = "🎉",
                            fontSize = 18.sp,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = (-10).dp, y = (-10).dp)
                        )
                        Text(
                            text = "🥳",
                            fontSize = 16.sp,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-12).dp, y = 12.dp)
                        )
                        Text(
                            text = "🎉",
                            fontSize = 16.sp,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .offset(x = 12.dp, y = (-12).dp)
                        )
                    }

                    // Congratulations Title
                    Text(
                        text = "MILESTONE UNLOCKED!",
                        color = themeColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Message
                    Text(
                        text = alert.message,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 20.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Awesome button
                    Button(
                        onClick = { viewModel.dismissMilestoneAlert(alert.id) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = themeColor,
                            contentColor = AmoledBlack
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = "Awesome! 🎉",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
