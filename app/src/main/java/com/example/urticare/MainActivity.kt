package com.example.urticare

import android.os.Bundle
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.compose.ui.platform.LocalContext
import com.example.urticare.ui.components.*
import com.example.urticare.ui.theme.*
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextOverflow
import com.example.urticare.model.LogEntry
import com.example.urticare.viewmodel.TrackerViewModel
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.layout.ContentScale
import androidx.activity.compose.BackHandler
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
class MainActivity : ComponentActivity() {
    private lateinit var viewModel: TrackerViewModel
    val showWidgetConsumptionDialog = mutableStateOf(false)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("OPEN_CONSUMPTION_FROM_WIDGET", false)) {
            showWidgetConsumptionDialog.value = true
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        viewModel = ViewModelProvider(this)[TrackerViewModel::class.java]

        if (intent?.getBooleanExtra("OPEN_CONSUMPTION_FROM_WIDGET", false) == true) {
            showWidgetConsumptionDialog.value = true
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }

        setContent {
            Urticare20Theme {
                val showDialog by showWidgetConsumptionDialog
                if (showDialog) {
                    var textVal by remember { mutableStateOf("") }
                    Dialog(
                        onDismissRequest = { showWidgetConsumptionDialog.value = false },
                        properties = DialogProperties(usePlatformDefaultWidth = true)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color.White,
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shadowElevation = 8.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Log Consumption",
                                    style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                )
                                Text(
                                    text = "What did you consume?",
                                    style = TextStyle(fontSize = 13.sp, color = Color(0xFF64748B))
                                )
                                OutlinedTextField(
                                    value = textVal,
                                    onValueChange = { textVal = it },
                                    placeholder = { Text("e.g. Morning Coffee, Banana") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFFF8FAFC),
                                        unfocusedContainerColor = Color(0xFFF8FAFC),
                                        focusedIndicatorColor = Color(0xFFF97316),
                                        unfocusedIndicatorColor = Color(0xFFE2E8F0)
                                    )
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = { showWidgetConsumptionDialog.value = false }
                                    ) {
                                        Text("Cancel", color = Color(0xFF64748B))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            if (textVal.trim().isNotEmpty()) {
                                                val timestamp = java.time.ZonedDateTime.now().toString()
                                                val metadata = "Consumption:::${textVal.trim()}:::Other::::::"
                                                viewModel.addEntry(com.example.urticare.model.EntryType.CONSUMPTION, timestamp, metadata)
                                                showWidgetConsumptionDialog.value = false
                                                Toast.makeText(this@MainActivity, "Logged: ${textVal.trim()}", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Log", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }

                var showSplashScreen by remember { mutableStateOf(true) }

                if (showSplashScreen) {
                    VideoSplashScreen(onVideoFinished = { showSplashScreen = false })
                } else {
                    val context = LocalContext.current
                    val entries by viewModel.entries.collectAsState(initial = emptyList())
                    val profileName by viewModel.profileName.collectAsState()
                    val profileAge by viewModel.profileAge.collectAsState()
                    val profileBirthDate by viewModel.profileBirthDate.collectAsState()
                    val profileSex by viewModel.profileSex.collectAsState()

                    var dismissedReminderThisSession by remember { mutableStateOf(false) }

                    val isProfileComplete = remember(profileName, profileAge, profileBirthDate, profileSex) {
                        val hasName = profileName.trim().isNotEmpty()
                        val hasAge = profileAge.trim().isNotEmpty() || profileBirthDate.trim().isNotEmpty()
                        val hasGender = profileSex.trim().isNotEmpty()
                        hasName && hasAge && hasGender
                    }

                    val showProfileReminder = !dismissedReminderThisSession && !isProfileComplete

                    val scrollState = rememberScrollState()
                    var selectedTab by remember { mutableIntStateOf(0) } // 0: Home, 1: Talk to Urti, 2: Profile
                    var activeSubScreen by remember { mutableStateOf("MAIN") } // "MAIN", "LOGS", "PATTERN", "LIBRARY"
                    var logsSelectedTab by remember { mutableIntStateOf(0) }

                    var showQuickLogBottomSheet by remember { mutableStateOf(false) }
                    var prefilledItemName by remember { mutableStateOf<String?>(null) }
                    var prefilledCategory by remember { mutableStateOf<String?>(null) }
                    var prefilledTimestamp by remember { mutableStateOf<String?>(null) }

                    var showCustomSnackbar by remember { mutableStateOf(false) }
                    var customSnackbarMessage by remember { mutableStateOf("") }
                    var snackbarUndoIds by remember { mutableStateOf<List<String>>(emptyList()) }
                    var snackbarPrevRecents by remember { mutableStateOf<List<String>>(emptyList()) }
                    
                    var showRecentDialog by remember { mutableStateOf(false) }
                    var showSummaryDialog by remember { mutableStateOf(false) }
                    var showMilestonesDialog by remember { mutableStateOf(false) }

                    val brandTitleGradient = remember {
                        Brush.horizontalGradient(listOf(Color(0xFF814B92), Color(0xFF509729), Color(0xFF1A7E97)))
                    }


                    LaunchedEffect(selectedTab) {
                        if (selectedTab != 0) {
                            activeSubScreen = "MAIN"
                        }
                    }

                    BackHandler(enabled = selectedTab != 0 || activeSubScreen != "MAIN" || showRecentDialog || showSummaryDialog || showMilestonesDialog) {
                        if (showRecentDialog || showSummaryDialog || showMilestonesDialog) {
                            showRecentDialog = false
                            showSummaryDialog = false
                            showMilestonesDialog = false
                        } else if (activeSubScreen != "MAIN") {
                            activeSubScreen = "MAIN"
                        } else {
                            selectedTab = 0
                        }
                    }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = Color(0xFFF8FAFC),
                        bottomBar = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .background(Color.White)
                                    .border(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // Top 2.5dp Tricolor Gradient Line (#814B92 -> #509729 -> #1A7E97)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(2.5.dp)
                                            .background(BrandLinearGradient)
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp, horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceAround,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        listOf(
                                            Triple(0, "Home", R.drawable.ic_footer_home),
                                            Triple(1, "Talk to Urti", R.drawable.ic_footer_urti),
                                            Triple(2, "Profile", R.drawable.ic_footer_profile)
                                        ).forEach { (tabIndex, label, iconRes) ->
                                            val isSelected = selectedTab == tabIndex

                                            val tabColor = when (tabIndex) {
                                                0 -> Color(0xFF814B92) // Home -> Purple
                                                1 -> Color(0xFF509729) // Talk to Urti -> Green
                                                else -> Color(0xFF1A7E97) // Profile -> Teal
                                            }
                                            val tabBgColor = when (tabIndex) {
                                                0 -> Color(0xFFF5F3FF)
                                                1 -> Color(0xFFECFDF5)
                                                else -> Color(0xFFF0FDFA)
                                            }
                                            val activeColor = if (isSelected) tabColor else tabColor.copy(alpha = 0.65f)

                                            Column(
                                                modifier = Modifier
                                                    .clickable(
                                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                                        indication = null
                                                    ) {
                                                        selectedTab = tabIndex
                                                        if (tabIndex == 0) {
                                                            activeSubScreen = "MAIN"
                                                            showRecentDialog = false
                                                            showSummaryDialog = false
                                                            showMilestonesDialog = false
                                                        }
                                                    }
                                                    .padding(vertical = 2.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            color = if (isSelected) tabBgColor else Color.Transparent,
                                                            shape = RoundedCornerShape(16.dp)
                                                        )
                                                        .border(
                                                            width = if (isSelected) 1.2.dp else 0.dp,
                                                            color = if (isSelected) tabColor.copy(alpha = 0.25f) else Color.Transparent,
                                                            shape = RoundedCornerShape(16.dp)
                                                        )
                                                        .padding(horizontal = 14.dp, vertical = 4.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Image(
                                                        painter = painterResource(id = iconRes),
                                                        contentDescription = label,
                                                        modifier = Modifier
                                                            .size(34.dp)
                                                            .graphicsLayer(alpha = if (isSelected) 1.0f else 0.65f),
                                                        colorFilter = ColorFilter.tint(tabColor),
                                                        contentScale = ContentScale.Fit
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = label,
                                                    color = activeColor,
                                                    fontSize = 10.5.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            when (selectedTab) {
                                0 -> {
                                    // Home Tab
                                    when (activeSubScreen) {
                                        "LOGS" -> {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .verticalScroll(rememberScrollState())
                                                    .padding(bottom = 32.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Spacer(modifier = Modifier.height(16.dp))
                                                HistoryLogList(
                                                    viewModel = viewModel,
                                                    entries = entries,
                                                    initialTab = logsSelectedTab,
                                                    onTabSelected = { logsSelectedTab = it },
                                                    onPatternAnalysisClick = { activeSubScreen = "PATTERN" },
                                                    onOpenAddConsumption = { customTime ->
                                                        prefilledTimestamp = customTime
                                                        activeSubScreen = "ADD_CONSUMPTION"
                                                    },
                                                    onBack = { activeSubScreen = "MAIN" }
                                                )
                                            }
                                        }
                                        "ADD_CONSUMPTION" -> {
                                            AddConsumptionScreen(
                                                viewModel = viewModel,
                                                onBack = {
                                                    activeSubScreen = "MAIN"
                                                    prefilledItemName = null
                                                    prefilledCategory = null
                                                    prefilledTimestamp = null
                                                },
                                                prefilledItemName = prefilledItemName,
                                                prefilledCategory = prefilledCategory,
                                                prefilledTimestamp = prefilledTimestamp
                                            )
                                        }
                                        "PATTERN" -> {
                                            PatternAnalysisScreen(
                                                viewModel = viewModel,
                                                entries = entries,
                                                onBack = { activeSubScreen = "MAIN" }
                                            )
                                        }
                                        "LIBRARY" -> {
                                            LibraryScreen(
                                                onBack = { activeSubScreen = "MAIN" }
                                            )
                                        }
                                        else -> {
                                             // Home Main View
                                             val rawProfileName by viewModel.profileName.collectAsState()
                                             val displayName = if (rawProfileName.isBlank()) "there" else rawProfileName.trim()

                                             Column(
                                                 modifier = Modifier
                                                     .fillMaxSize()
                                                     .padding(bottom = 4.dp),
                                                 horizontalAlignment = Alignment.CenterHorizontally
                                             ) {
                                                 // 1. Top Greeting Header
                                                 Box(
                                                     modifier = Modifier
                                                         .fillMaxWidth()
                                                         .statusBarsPadding()
                                                         .padding(top = 8.dp, bottom = 0.dp, start = 16.dp, end = 16.dp),
                                                     contentAlignment = Alignment.Center
                                                 ) {
                                                     // Logo on the upper-left
                                                     Image(
                                                         painter = painterResource(id = R.drawable.logo),
                                                         contentDescription = "UrtiCare Logo",
                                                         modifier = Modifier
                                                             .align(Alignment.CenterStart)
                                                             .height(31.dp)
                                                             .padding(start = 4.dp),
                                                         contentScale = ContentScale.Fit
                                                     )

                                                     val annotatedGreeting = buildAnnotatedString {
                                                         append("Hi, ")
                                                         withStyle(
                                                             style = SpanStyle(
                                                                 brush = brandTitleGradient,
                                                                 fontWeight = FontWeight.Bold
                                                             )
                                                         ) {
                                                             append(displayName)
                                                         }
                                                     }

                                                     Text(
                                                         text = annotatedGreeting,
                                                         color = Color(0xFF0F172A),
                                                         fontSize = 22.sp,
                                                         fontWeight = FontWeight.Bold,
                                                         textAlign = TextAlign.Center,
                                                         maxLines = 1,
                                                         overflow = TextOverflow.Ellipsis
                                                     )
                                                 }

                                                 Spacer(modifier = Modifier.height(10.dp))

                                                 // 2. Timer Circle - Middle
                                                 Box(
                                                     modifier = Modifier
                                                         .fillMaxWidth()
                                                         .padding(vertical = 0.dp),
                                                     contentAlignment = Alignment.Center
                                                 ) {
                                                      LiveCounter(
                                                          entries = entries,
                                                          onNavigateToLogs = { tabIndex ->
                                                              logsSelectedTab = tabIndex
                                                              activeSubScreen = "LOGS"
                                                          }
                                                      )
                                                 }

                                                 Spacer(modifier = Modifier.height(4.dp))

                                                 // 3. Action Buttons - Middle
                                                 ActionPillsGroup(
                                                     viewModel = viewModel,
                                                     onOpenAddConsumption = { showQuickLogBottomSheet = true }
                                                 )

                                                 Spacer(modifier = Modifier.height(6.dp))

                                                 // 4. Tricolor Thin Gradient Line
                                                 Spacer(
                                                     modifier = Modifier
                                                         .fillMaxWidth()
                                                         .padding(horizontal = 20.dp, vertical = 2.dp)
                                                         .height(1.5.dp)
                                                         .background(brandTitleGradient, shape = RoundedCornerShape(1.dp))
                                                 )

                                                 Spacer(modifier = Modifier.height(28.dp))

                                                 // 5. Six Navigation Cards Grid (3 columns x 2 rows): Recent, Summary, Logs, Pattern, Milestones, Library
                                                 Column(
                                                     modifier = Modifier
                                                         .fillMaxWidth()
                                                         .padding(horizontal = 16.dp),
                                                     verticalArrangement = Arrangement.spacedBy(10.dp)
                                                 ) {
                                                    // Row 1: Recent, Summary, Logs
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                         // Card 1: Recent (Left Column -> Purple)
                                                         Card(
                                                             onClick = { showRecentDialog = true },
                                                             modifier = Modifier
                                                                 .weight(1f)
                                                                 .height(92.dp),
                                                             colors = CardDefaults.cardColors(containerColor = Color.White),
                                                             border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                                             shape = RoundedCornerShape(16.dp),
                                                             elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                                         ) {
                                                             Column(
                                                                 modifier = Modifier
                                                                     .fillMaxSize()
                                                                     .padding(4.dp),
                                                                 horizontalAlignment = Alignment.CenterHorizontally,
                                                                 verticalArrangement = Arrangement.Center
                                                             ) {
                                                                 Box(
                                                                     modifier = Modifier
                                                                         .size(44.dp)
                                                                         .background(Color(0xFFF5F3FF), shape = RoundedCornerShape(12.dp))
                                                                         .border(1.dp, Color(0xFFE2E8F0), shape = RoundedCornerShape(12.dp))
                                                                         .clip(RoundedCornerShape(12.dp))
                                                                         .padding(4.dp),
                                                                     contentAlignment = Alignment.Center
                                                                 ) {
                                                                     Image(
                                                                         painter = painterResource(id = R.drawable.ic_recent),
                                                                         contentDescription = "Recent",
                                                                         modifier = Modifier.fillMaxSize(),
                                                                         colorFilter = ColorFilter.tint(Color(0xFF814B92)),
                                                                         contentScale = ContentScale.Fit
                                                                     )
                                                                 }
                                                                 Spacer(modifier = Modifier.height(4.dp))
                                                                 Text(
                                                                     text = "Recent",
                                                                     color = Color(0xFF814B92),
                                                                     fontSize = 12.sp,
                                                                     fontWeight = FontWeight.ExtraBold,
                                                                     maxLines = 1
                                                                 )
                                                             }
                                                         }

                                                         // Card 2: Logs (Middle Column -> Green)
                                                         Card(
                                                             onClick = { activeSubScreen = "LOGS" },
                                                             modifier = Modifier
                                                                 .weight(1f)
                                                                 .height(92.dp),
                                                             colors = CardDefaults.cardColors(containerColor = Color.White),
                                                             border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                                             shape = RoundedCornerShape(16.dp),
                                                             elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                                         ) {
                                                             Column(
                                                                 modifier = Modifier
                                                                     .fillMaxSize()
                                                                     .padding(4.dp),
                                                                 horizontalAlignment = Alignment.CenterHorizontally,
                                                                 verticalArrangement = Arrangement.Center
                                                             ) {
                                                                 Box(
                                                                     modifier = Modifier
                                                                         .size(44.dp)
                                                                         .background(Color(0xFFECFDF5), shape = RoundedCornerShape(12.dp))
                                                                         .border(1.dp, Color(0xFFE2E8F0), shape = RoundedCornerShape(12.dp))
                                                                         .clip(RoundedCornerShape(12.dp))
                                                                         .padding(4.dp),
                                                                     contentAlignment = Alignment.Center
                                                                 ) {
                                                                     Image(
                                                                         painter = painterResource(id = R.drawable.ic_log),
                                                                         contentDescription = "Logs",
                                                                         modifier = Modifier.fillMaxSize(),
                                                                         colorFilter = ColorFilter.tint(Color(0xFF509729)),
                                                                         contentScale = ContentScale.Fit
                                                                     )
                                                                 }
                                                                 Spacer(modifier = Modifier.height(4.dp))
                                                                 Text(
                                                                     text = "Logs",
                                                                     color = Color(0xFF509729),
                                                                     fontSize = 12.sp,
                                                                     fontWeight = FontWeight.ExtraBold,
                                                                     maxLines = 1
                                                                 )
                                                             }
                                                         }

                                                         // Card 3: Summary (Right Column -> Teal)
                                                         Card(
                                                             onClick = { showSummaryDialog = true },
                                                             modifier = Modifier
                                                                 .weight(1f)
                                                                 .height(92.dp),
                                                             colors = CardDefaults.cardColors(containerColor = Color.White),
                                                             border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                                             shape = RoundedCornerShape(16.dp),
                                                             elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                                         ) {
                                                             Column(
                                                                 modifier = Modifier
                                                                     .fillMaxSize()
                                                                     .padding(4.dp),
                                                                 horizontalAlignment = Alignment.CenterHorizontally,
                                                                 verticalArrangement = Arrangement.Center
                                                             ) {
                                                                 Box(
                                                                     modifier = Modifier
                                                                         .size(44.dp)
                                                                         .background(Color(0xFFF0FDFA), shape = RoundedCornerShape(12.dp))
                                                                         .border(1.dp, Color(0xFFE2E8F0), shape = RoundedCornerShape(12.dp))
                                                                         .clip(RoundedCornerShape(12.dp))
                                                                         .padding(4.dp),
                                                                     contentAlignment = Alignment.Center
                                                                 ) {
                                                                     Image(
                                                                         painter = painterResource(id = R.drawable.ic_summary),
                                                                         contentDescription = "Summary",
                                                                         modifier = Modifier.fillMaxSize(),
                                                                         colorFilter = ColorFilter.tint(Color(0xFF1A7E97)),
                                                                         contentScale = ContentScale.Fit
                                                                     )
                                                                 }
                                                                 Spacer(modifier = Modifier.height(4.dp))
                                                                 Text(
                                                                     text = "Summary",
                                                                     color = Color(0xFF1A7E97),
                                                                     fontSize = 12.sp,
                                                                     fontWeight = FontWeight.ExtraBold,
                                                                     maxLines = 1
                                                                 )
                                                             }
                                                         }
                                                     }

                                                     // Row 2: Pattern, Milestones, Library
                                                     Row(
                                                         modifier = Modifier.fillMaxWidth(),
                                                         horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                     ) {
                                                         // Card 4: Pattern (Left Column -> Purple)
                                                         Card(
                                                             onClick = { activeSubScreen = "PATTERN" },
                                                             modifier = Modifier
                                                                 .weight(1f)
                                                                 .height(92.dp),
                                                             colors = CardDefaults.cardColors(containerColor = Color.White),
                                                             border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                                             shape = RoundedCornerShape(16.dp),
                                                             elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                                         ) {
                                                             Column(
                                                                 modifier = Modifier
                                                                     .fillMaxSize()
                                                                     .padding(4.dp),
                                                                 horizontalAlignment = Alignment.CenterHorizontally,
                                                                 verticalArrangement = Arrangement.Center
                                                             ) {
                                                                 Box(
                                                                     modifier = Modifier
                                                                         .size(44.dp)
                                                                         .background(Color(0xFFF5F3FF), shape = RoundedCornerShape(12.dp))
                                                                         .border(1.dp, Color(0xFFE2E8F0), shape = RoundedCornerShape(12.dp))
                                                                         .clip(RoundedCornerShape(12.dp))
                                                                         .padding(4.dp),
                                                                     contentAlignment = Alignment.Center
                                                                 ) {
                                                                     Image(
                                                                         painter = painterResource(id = R.drawable.ic_pattern),
                                                                         contentDescription = "Pattern",
                                                                         modifier = Modifier.fillMaxSize(),
                                                                         colorFilter = ColorFilter.tint(Color(0xFF814B92)),
                                                                         contentScale = ContentScale.Fit
                                                                     )
                                                                 }
                                                                 Spacer(modifier = Modifier.height(4.dp))
                                                                 Text(
                                                                     text = "Pattern",
                                                                     color = Color(0xFF814B92),
                                                                     fontSize = 12.sp,
                                                                     fontWeight = FontWeight.ExtraBold,
                                                                     maxLines = 1
                                                                 )
                                                             }
                                                         }

                                                         // Card 5: Milestones (Middle Column -> Green)
                                                         Card(
                                                             onClick = { showMilestonesDialog = true },
                                                             modifier = Modifier
                                                                 .weight(1f)
                                                                 .height(92.dp),
                                                             colors = CardDefaults.cardColors(containerColor = Color.White),
                                                             border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                                             shape = RoundedCornerShape(16.dp),
                                                             elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                                         ) {
                                                             Column(
                                                                 modifier = Modifier
                                                                     .fillMaxSize()
                                                                     .padding(4.dp),
                                                                 horizontalAlignment = Alignment.CenterHorizontally,
                                                                 verticalArrangement = Arrangement.Center
                                                             ) {
                                                                 Box(
                                                                     modifier = Modifier
                                                                         .size(44.dp)
                                                                         .background(Color(0xFFECFDF5), shape = RoundedCornerShape(12.dp))
                                                                         .border(1.dp, Color(0xFFE2E8F0), shape = RoundedCornerShape(12.dp))
                                                                         .clip(RoundedCornerShape(12.dp))
                                                                         .padding(4.dp),
                                                                     contentAlignment = Alignment.Center
                                                                 ) {
                                                                     Image(
                                                                         painter = painterResource(id = R.drawable.ic_trophy),
                                                                         contentDescription = "Milestones",
                                                                         modifier = Modifier.fillMaxSize(),
                                                                         colorFilter = ColorFilter.tint(Color(0xFF509729)),
                                                                         contentScale = ContentScale.Fit
                                                                     )
                                                                 }
                                                                 Spacer(modifier = Modifier.height(4.dp))
                                                                 Text(
                                                                     text = "Milestones",
                                                                     color = Color(0xFF509729),
                                                                     fontSize = 12.sp,
                                                                     fontWeight = FontWeight.ExtraBold,
                                                                     maxLines = 1
                                                                 )
                                                             }
                                                         }

                                                         // Card 6: Library (Right Column -> Teal)
                                                         Card(
                                                             onClick = { activeSubScreen = "LIBRARY" },
                                                             modifier = Modifier
                                                                 .weight(1f)
                                                                 .height(92.dp),
                                                             colors = CardDefaults.cardColors(containerColor = Color.White),
                                                             border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                                             shape = RoundedCornerShape(16.dp),
                                                             elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                                         ) {
                                                             Column(
                                                                 modifier = Modifier
                                                                     .fillMaxSize()
                                                                     .padding(4.dp),
                                                                 horizontalAlignment = Alignment.CenterHorizontally,
                                                                 verticalArrangement = Arrangement.Center
                                                             ) {
                                                                 Box(
                                                                     modifier = Modifier
                                                                         .size(44.dp)
                                                                         .background(Color(0xFFF0FDFA), shape = RoundedCornerShape(12.dp))
                                                                         .border(1.dp, Color(0xFFE2E8F0), shape = RoundedCornerShape(12.dp))
                                                                         .clip(RoundedCornerShape(12.dp))
                                                                         .padding(4.dp),
                                                                     contentAlignment = Alignment.Center
                                                                 ) {
                                                                     Image(
                                                                         painter = painterResource(id = R.drawable.ic_library),
                                                                         contentDescription = "Library",
                                                                         modifier = Modifier.fillMaxSize(),
                                                                         colorFilter = ColorFilter.tint(Color(0xFF1A7E97)),
                                                                         contentScale = ContentScale.Fit
                                                                     )
                                                                 }
                                                                 Spacer(modifier = Modifier.height(4.dp))
                                                                 Text(
                                                                     text = "Library",
                                                                     color = Color(0xFF1A7E97),
                                                                     fontSize = 12.sp,
                                                                     fontWeight = FontWeight.ExtraBold,
                                                                     maxLines = 1
                                                                 )
                                                             }
                                                         }
                                                     }
                                                }
                                            }
                                        }
                                    }
                                }
                                1 -> {
                                    // Talk to Urti Tab
                                    UrtiChatScreen(viewModel = viewModel, onBack = { selectedTab = 0 })
                                }
                                2 -> {
                                    // Profile Screen
                                    ProfileScreen(viewModel = viewModel, onBack = { selectedTab = 0 })
                                }
                            }

                            // Bottom Sheet for Recent Medication Intake
                            if (showRecentDialog) {
                                ModalBottomSheet(
                                    onDismissRequest = { showRecentDialog = false },
                                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                                    containerColor = Color.White,
                                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                                    dragHandle = {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 12.dp, bottom = 8.dp)
                                                .width(36.dp)
                                                .height(4.dp)
                                                .background(Color(0xFFE2E8F0), shape = RoundedCornerShape(2.dp))
                                        )
                                    }
                                ) {
                                    RecentIntakeLog(
                                        viewModel = viewModel,
                                        entries = entries,
                                        onClose = { showRecentDialog = false },
                                        onViewHistory = {
                                            showRecentDialog = false
                                            activeSubScreen = "LOGS"
                                        }
                                    )
                                }
                            }

                            // Bottom Sheet for Summary
                            if (showSummaryDialog) {
                                ModalBottomSheet(
                                    onDismissRequest = { showSummaryDialog = false },
                                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                                    containerColor = Color.White,
                                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                                    dragHandle = {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 12.dp, bottom = 8.dp)
                                                .width(36.dp)
                                                .height(4.dp)
                                                .background(Color(0xFFE2E8F0), shape = RoundedCornerShape(2.dp))
                                        )
                                    }
                                ) {
                                    MedicationSummaryDrawer(
                                        viewModel = viewModel,
                                        entries = entries,
                                        onNavigateToLogs = { tabIndex ->
                                            logsSelectedTab = tabIndex
                                            activeSubScreen = "LOGS"
                                        },
                                        onClose = { showSummaryDialog = false }
                                    )
                                }
                            }

                            // Pop-up Dialog for Milestones
                            if (showMilestonesDialog) {
                                Dialog(
                                    onDismissRequest = { showMilestonesDialog = false },
                                    properties = DialogProperties(usePlatformDefaultWidth = false)
                                ) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth(0.92f)
                                            .wrapContentHeight()
                                            .padding(vertical = 16.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        shape = RoundedCornerShape(20.dp),
                                        border = BorderStroke(1.5.dp, brandTitleGradient),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(end = 4.dp, bottom = 4.dp),
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                IconButton(
                                                    onClick = { showMilestonesDialog = false },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Text(
                                                        text = "✕",
                                                        color = Color(0xFF64748B),
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            Box(
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                HomeMilestonesExpandedCard(viewModel = viewModel, entries = entries)
                                            }
                                        }
                                    }
                                }
                            }

                            // Milestone alert overlay at the top
                            MilestoneAlertOverlay(viewModel = viewModel)

                            // Floating Undo Delete Banner
                            UndoDeleteBanner(viewModel = viewModel)

                            // Quick Log Consumption Bottom Sheet
                            QuickLogConsumptionBottomSheet(
                                visible = showQuickLogBottomSheet,
                                onDismiss = { showQuickLogBottomSheet = false },
                                viewModel = viewModel,
                                entries = entries,
                                onLogged = { ids, prevRecents, message ->
                                    customSnackbarMessage = message
                                    snackbarUndoIds = ids
                                    snackbarPrevRecents = prevRecents
                                    showCustomSnackbar = true
                                },
                                onAddMoreDetails = { itemName, category, customTime ->
                                    prefilledItemName = itemName
                                    prefilledCategory = category
                                    prefilledTimestamp = customTime
                                    activeSubScreen = "ADD_CONSUMPTION"
                                }
                            )

                            // Custom Snackbar Banner
                            if (showCustomSnackbar) {
                                LaunchedEffect(showCustomSnackbar) {
                                    kotlinx.coroutines.delay(4000)
                                    showCustomSnackbar = false
                                }
                                Card(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 80.dp)
                                        .shadow(8.dp, RoundedCornerShape(16.dp)),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = customSnackbarMessage,
                                            color = Color(0xFF0F172A),
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (snackbarUndoIds.isNotEmpty()) {
                                            Text(
                                                text = "Undo",
                                                color = Color(0xFF814B92),
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier
                                                    .clickable {
                                                        viewModel.deleteEntries(snackbarUndoIds)
                                                        val sharedPrefs = context.getSharedPreferences("urticare_consumption_prefs", Context.MODE_PRIVATE)
                                                        sharedPrefs.edit().putStringSet("recently_used", snackbarPrevRecents.toSet()).apply()
                                                        customSnackbarMessage = "Logging undone successfully"
                                                        snackbarUndoIds = emptyList()
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Duplicate minute collision warning alert dialog
                        CollisionWarningDialog(viewModel = viewModel)

                        // Streak break warning dialog
                        StreakBreakDialog(viewModel = viewModel)

                        // Profile Completion Reminder Dialog
                        ProfileCompletionReminderDialog(
                            visible = showProfileReminder,
                            onDismiss = {
                                dismissedReminderThisSession = true
                            },
                            onCompleteProfile = {
                                dismissedReminderThisSession = true
                                selectedTab = 2
                                activeSubScreen = "MAIN"
                                showRecentDialog = false
                                showSummaryDialog = false
                                showMilestonesDialog = false
                            }
                        )

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
fun MilestoneAlertOverlay(viewModel: TrackerViewModel) {
    val activeAlerts by viewModel.activeMilestoneAlerts.collectAsState()
    val alert = activeAlerts.firstOrNull()

    if (alert != null) {
        val themeColor = Color(0xFF8B5CF6)
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

            val brandTitleGradient = Brush.horizontalGradient(listOf(Color(0xFF814B92), Color(0xFF509729), Color(0xFF1A7E97)))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .graphicsLayer(
                        scaleX = scale.value,
                        scaleY = scale.value
                    )
                    .border(
                        width = 1.5.dp,
                        brush = brandTitleGradient,
                        shape = RoundedCornerShape(16.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "MILESTONE UNLOCKED!",
                        style = TextStyle(
                            brush = brandTitleGradient,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = alert.message,
                        color = Color(0xFF0F172A),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 20.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedButton(
                        onClick = { viewModel.dismissMilestoneAlert(alert.id) },
                        border = BorderStroke(1.5.dp, brandTitleGradient),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = "AWESOME !!!",
                            style = TextStyle(
                                brush = brandTitleGradient,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeMilestonesExpandedCard(viewModel: TrackerViewModel, entries: List<LogEntry>) {
    val adherenceStreak by viewModel.adherenceStreakHours.collectAsState()
    val remissionStreak by viewModel.remissionStreakDays.collectAsState()
    val lastBrokenHours = com.example.urticare.util.MilestoneEvaluator.getLastBrokenMilestoneHours(entries)

    val lastBrokenText = if (lastBrokenHours != null) {
        if (lastBrokenHours <= 48L) {
            "$lastBrokenHours hours"
        } else {
            val days = lastBrokenHours / 24
            val rem = lastBrokenHours % 24
            if (rem > 0L) "$days days $rem hours" else "$days days"
        }
    } else {
        null
    }

    val stabilityMilestones = (adherenceStreak ?: 0L) / 48
    val remissionMilestones = (remissionStreak ?: 0L) / 7

    val stabilityMilestoneText = if (stabilityMilestones > 0) {
        if (stabilityMilestones == 1L) "Tier 1 (48h Stable)" else "Tier $stabilityMilestones (${stabilityMilestones * 2}d Stable)"
    } else {
        "Underway (Next at 48h)"
    }

    val remissionMilestoneText = if (remissionMilestones > 0) {
        "Tier $remissionMilestones ($remissionMilestones Week${if (remissionMilestones > 1) "s" else ""} Free)"
    } else {
        "Underway (Next at 7d)"
    }

    val brandTitleGradient = Brush.horizontalGradient(listOf(Color(0xFF814B92), Color(0xFF509729), Color(0xFF1A7E97)))
    val pastMilestones = remember(entries) {
        com.example.urticare.util.MilestoneEvaluator.getPastMilestones(entries)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
        border = BorderStroke(1.5.dp, brandTitleGradient),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 480.dp)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 14.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "My Milestones",
                style = TextStyle(
                    brush = brandTitleGradient,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            val isBroken = adherenceStreak != null && adherenceStreak!! < 48L && lastBrokenHours != null && lastBrokenHours >= 48L
            if (isBroken && lastBrokenText != null) {
                Text(
                    text = "Last Milestone broken after $lastBrokenText — Don't sweat it, the progress still counts",
                    color = Color(0xFF737373),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Adherence (Stability) Streak item
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (adherenceStreak != null) {
                            if (adherenceStreak!! <= 48L) {
                                "$adherenceStreak Hours Stable"
                            } else {
                                val days = adherenceStreak!! / 24
                                val rem = adherenceStreak!! % 24
                                if (rem > 0L) "$days Days $rem Hours Stable" else "$days Days Stable"
                            }
                        } else {
                            "-- Hours Stable"
                        },
                        color = Color(0xFF509729),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Without urticaria medication!",
                        color = Color(0xFF64748B),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_diamond),
                            contentDescription = "Diamond",
                            modifier = Modifier.size(14.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stabilityMilestoneText,
                            color = Color(0xFF509729),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Vertical Divider line
                Spacer(
                    modifier = Modifier
                        .width(1.dp)
                        .height(44.dp)
                        .background(Color(0xFFE2E8F0))
                )

                // Remission Streak item
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (remissionStreak != null) "$remissionStreak Days Remission" else "-- Days Remission",
                        color = Color(0xFF814B92),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Flare ups free!",
                        color = Color(0xFF64748B),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_diamond),
                            contentDescription = "Diamond",
                            modifier = Modifier.size(14.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = remissionMilestoneText,
                            color = Color(0xFF475569),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFFE2E8F0))
            )
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Milestones History",
                color = Color(0xFF64748B),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                textAlign = TextAlign.Start
            )

            if (pastMilestones.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No milestone history recorded yet.\nKeep pacing! 💫",
                        color = Color(0xFF64748B),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                pastMilestones.forEach { pm ->
                    val isAdherence = pm.type == com.example.urticare.viewmodel.MilestoneType.ADHERENCE
                    val themeColor = if (isAdherence) Color(0xFF509729) else Color(0xFF814B92)
                    val durationText = if (isAdherence) {
                        if (pm.value <= 48L) {
                            "${pm.value} Hours Stable"
                        } else {
                            val days = pm.value / 24
                            val rem = pm.value % 24
                            if (rem > 0L) "$days Days $rem Hours Stable" else "$days Days Stable"
                        }
                    } else {
                        "${pm.value} Days Remission"
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(Color(0xFFF8FAFC), shape = RoundedCornerShape(10.dp))
                            .border(1.dp, Color(0xFFE2E8F0), shape = RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(32.dp)
                                .background(themeColor.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp))
                        ) {
                            if (isAdherence) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_trophy),
                                    contentDescription = "Trophy Icon",
                                    modifier = Modifier.size(16.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_diamond),
                                    contentDescription = "Gem Icon",
                                    modifier = Modifier.size(16.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = durationText,
                                color = themeColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Broken on: ${pm.dateCompletedStr}",
                                color = Color(0xFF64748B),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (pm.reasonStopped.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Reason: ${pm.reasonStopped}",
                                    color = Color(0xFF475569),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
