package com.example.urticare20.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.urticare20.R
import com.example.urticare20.model.ChatMessage
import com.example.urticare20.model.ChatSession
import com.example.urticare20.ui.theme.*
import androidx.activity.compose.BackHandler
import com.example.urticare20.viewmodel.TrackerViewModel
import kotlinx.coroutines.delay

enum class ChatScreenMode {
    SPLASH,
    CHAT,
    BREATHING,
    BREATHING_INTERCEPT
}

@Composable
fun UrtiChatScreen(viewModel: TrackerViewModel, onBack: () -> Unit = {}) {
    val context = LocalContext.current

    val currentChat by viewModel.currentChat.collectAsState()
    val archivedChats by viewModel.archivedChats.collectAsState()
    val isTyping by viewModel.isUrtiTyping

    var screenMode by remember { mutableStateOf(ChatScreenMode.SPLASH) }

    BackHandler(enabled = true) {
        when (screenMode) {
            ChatScreenMode.BREATHING, ChatScreenMode.BREATHING_INTERCEPT -> {
                if (screenMode == ChatScreenMode.BREATHING_INTERCEPT) {
                    screenMode = ChatScreenMode.CHAT
                } else {
                    screenMode = ChatScreenMode.SPLASH
                }
            }
            ChatScreenMode.CHAT -> {
                screenMode = ChatScreenMode.SPLASH
            }
            ChatScreenMode.SPLASH -> {
                onBack()
            }
        }
    }

    var showNewChatDialog by remember { mutableStateOf(false) }
    
    // Rename session dialog state
    var showRenameDialog by remember { mutableStateOf<ChatSession?>(null) }
    var renameInput by remember { mutableStateOf("") }

    // Message input text state
    var textInput by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()

    // Breathing simulation states
    var breathCycle by remember { mutableIntStateOf(1) } // Counts 1, 2, 3
    var breathPhase by remember { mutableStateOf("Breathe in...") } // "Breathe in...", "Hold...", "Breathe out...", "Rest..."
    var targetScale by remember { mutableStateOf(1.0f) }
    var showBreathDialog by remember { mutableStateOf(false) }
    var encouragementMessage by remember { mutableStateOf<String?>(null) }

    val breathMessages = remember {
        listOf(
            "With that breath, you just reset your day; you are fully capable of handling whatever comes next.",
            "You have survived 100% of your hardest days so far, and you are doing much better than you give yourself credit for.",
            "In this exact moment, you are safe, you are grounded, and you are completely in control of your next step.",
            "Progress isn't about rushing forward; sometimes, the greatest progress is simply pausing, breathing, and choosing to continue.",
            "You don't have to figure out the whole mountain right now—just focus on the very next step in front of you.",
            "Your strength isn't measured by never feeling overwhelmed, but by your beautiful ability to breathe through it and find your peace again.",
            "Let go of what you cannot control in this moment, and pour your incredible energy into what you can.",
            "Slow down, give yourself some grace, and remember that you are allowed to be a masterpiece and a work in progress all at the same time.",
            "The weight you’ve been carrying is heavy, but so is your resilience. Trust yourself—you've got this.",
            "Every new breath is a completely fresh start and a reminder that you are strong enough to handle your own story."
        )
    }

    // Dynamic scale animator for therapeutic circle visual
    val circleScaleAnim by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(
            durationMillis = if (breathPhase == "Hold..." || breathPhase == "Rest...") 1200 else 4000,
            easing = LinearOutSlowInEasing
        )
    )

    // Breathing cycle loops coroutine
    LaunchedEffect(screenMode, breathCycle) {
        if (screenMode == ChatScreenMode.BREATHING || screenMode == ChatScreenMode.BREATHING_INTERCEPT) {
            showBreathDialog = false
            
            // Phase 1: Inhale (4s)
            breathPhase = "Breathe in..."
            targetScale = 2.2f
            delay(4000)

            // Phase 2: Hold (1.2s)
            breathPhase = "Hold..."
            delay(1200)

            // Phase 3: Exhale (4s)
            breathPhase = "Breathe out..."
            targetScale = 1.0f
            delay(4000)

            // Phase 4: Rest (1s)
            breathPhase = "Rest..."
            delay(1000)

            if (breathCycle < 3) {
                // Loop to next breath cycle
                breathCycle++
            } else {
                // 3 breaths finished, show evaluation prompt dialog
                showBreathDialog = true
            }
        }
    }

    // Sentiment intercept observer
    LaunchedEffect(viewModel.pendingSentimentMessage.value) {
        val pendingMsg = viewModel.pendingSentimentMessage.value
        if (pendingMsg != null) {
            breathCycle = 1
            showBreathDialog = false
            screenMode = ChatScreenMode.BREATHING_INTERCEPT
        }
    }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(currentChat.size, isTyping) {
        if (currentChat.isNotEmpty()) {
            lazyListState.animateScrollToItem(currentChat.size - 1)
        }
    }

    // Main layout container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        if (screenMode == ChatScreenMode.SPLASH) {
            // Back button at top-left
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(top = 16.dp, start = 16.dp)
                    .align(Alignment.TopStart)
                    .background(DarkCard, shape = CircleShape)
                    .size(40.dp)
            ) {
                Text(
                    text = "←",
                    color = LightGray,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Mascot Splash Screen
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(top = 68.dp, bottom = 40.dp)
            ) {
                // Mascot centered image
                item {
                    Image(
                        painter = painterResource(id = R.drawable.urti_avatar),
                        contentDescription = "Urti Mascot Avatar",
                        modifier = Modifier
                            .size(200.dp)
                            .padding(bottom = 16.dp),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                }

                // Greeting Bold text - Hi, I'm Urti
                item {
                    Text(
                        text = "Hi, I'm Urti",
                        color = SoftPurple,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Secondary normal weight text
                item {
                    Text(
                        text = "Your well-being guardian and this is your quiet space to find balance and reduce stress",
                        color = MutedGray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                        lineHeight = 18.sp
                    )
                }

                // Main navigation button group
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 1. New Chat Button
                        Button(
                            onClick = {
                                val hasUserMsgs = currentChat.any { it.sender == "User" }
                                if (hasUserMsgs) {
                                    showNewChatDialog = true
                                } else {
                                    viewModel.startNewChat(clearPrevious = true)
                                    screenMode = ChatScreenMode.CHAT
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SoftPurple,
                                contentColor = AmoledBlack
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = "New Chat", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        // 2. Resume Last Chat Button (visible/enabled if we have an active session)
                        val hasActiveSession = currentChat.isNotEmpty()
                        Button(
                            onClick = {
                                viewModel.resumeLastChat()
                                screenMode = ChatScreenMode.CHAT
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkSurface,
                                contentColor = SoftPurple
                            ),
                            border = BorderStroke(1.dp, SoftPurple.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            enabled = hasActiveSession
                        ) {
                            Text(
                                text = "Resume Last Chat",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (hasActiveSession) SoftPurple else MutedGray
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // 3. Take a Breath Simulation Button Card
                        Card(
                            onClick = {
                                breathCycle = 1
                                showBreathDialog = false
                                screenMode = ChatScreenMode.BREATHING
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B8097).copy(alpha = 0.12f)),
                            border = BorderStroke(1.dp, Color(0xFF1B8097).copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(Color(0xFF1B8097).copy(alpha = 0.2f), shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "🧘", fontSize = 18.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Take a breath",
                                        color = Color(0xFF1B8097),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Pause for a moment to inhale, exhale, and find peace",
                                        color = MutedGray,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Saved/Archived Conversations list panel
                if (archivedChats.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(36.dp))
                        
                        Text(
                            text = "ARCHIVED CONVERSATIONS",
                            color = MutedGray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
                            letterSpacing = 1.sp
                        )
                    }

                    items(archivedChats) { session ->
                        Card(
                            onClick = {
                                viewModel.resumeArchive(session.id)
                                screenMode = ChatScreenMode.CHAT
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 5.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            border = BorderStroke(1.dp, DarkBorder),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = session.title,
                                        color = LightGray,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "${session.messages.size} messages",
                                        color = MutedGray,
                                        fontSize = 11.sp
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // Rename archived chat button using Text Emoji
                                    IconButton(
                                        onClick = {
                                            showRenameDialog = session
                                            renameInput = session.title
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Text(text = "✏️", fontSize = 16.sp)
                                    }

                                    // Delete archived chat button using Text Emoji
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteArchive(session.id)
                                            Toast.makeText(context, "Chat deleted", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Text(text = "🗑️", fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (screenMode == ChatScreenMode.CHAT) {
            // Active Chatbot conversation Window screen
            Column(modifier = Modifier.fillMaxSize()) {
                // Header bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface)
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { screenMode = ChatScreenMode.SPLASH }) {
                        Text(text = "← Back", color = SoftPurple, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Image(
                        painter = painterResource(id = R.drawable.urti_avatar),
                        contentDescription = "Urti small icon",
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(text = "Urti", color = LightGray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isTyping) "typing..." else "Your well-being guardian",
                            color = if (isTyping) SoftPurple else MutedGray,
                            fontSize = 11.sp,
                            fontWeight = if (isTyping) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                // Sync Indicator Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBorder.copy(alpha = 0.2f))
                        .padding(vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(color = SoftYellow, shape = CircleShape) // Muted green dot
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Urti is synced with your live logs",
                            color = MutedGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Chat bubble list
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(currentChat) { message ->
                        val isUser = message.sender == "User"
                        val bubbleColor = if (isUser) SoftPurple.copy(alpha = 0.15f) else DarkSurface
                        val bubbleBorder = if (isUser) BorderStroke(1.dp, SoftPurple.copy(alpha = 0.3f)) else BorderStroke(1.dp, DarkBorder)

                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                        ) {
                            Card(
                                modifier = Modifier.widthIn(max = 290.dp),
                                colors = CardDefaults.cardColors(containerColor = bubbleColor),
                                border = bubbleBorder,
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isUser) 16.dp else 4.dp,
                                    bottomEnd = if (isUser) 4.dp else 16.dp
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = message.text,
                                        color = LightGray,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }

                    // Typing Indicator anim
                    if (isTyping) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Card(
                                    modifier = Modifier.widthIn(max = 80.dp),
                                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                    border = BorderStroke(1.dp, DarkBorder),
                                    shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "•••", color = SoftPurple, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Input Box Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AmoledBlack)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text(text = "Vent, pause or breathe...", color = MutedGray, fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface,
                            focusedTextColor = LightGray,
                            unfocusedTextColor = LightGray,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedPlaceholderColor = MutedGray,
                            unfocusedPlaceholderColor = MutedGray
                        ),
                        shape = RoundedCornerShape(25.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (textInput.trim().isNotEmpty()) {
                                viewModel.sendUserMessage(textInput)
                                textInput = ""
                            }
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .background(SoftPurple, shape = CircleShape)
                    ) {
                        Text(text = "▲", color = AmoledBlack, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else if (screenMode == ChatScreenMode.BREATHING || screenMode == ChatScreenMode.BREATHING_INTERCEPT) {
            // Interactive Meditative Breathing Exercise Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Animated breathing visual bubble
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(240.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Outer glowing therapeutic aura ring
                        Box(
                            modifier = Modifier
                                .size((100 * circleScaleAnim).dp)
                                .background(Color(0xFF814B92).copy(alpha = 0.12f), shape = CircleShape)
                                .border(BorderStroke(2.dp, Color(0xFF814B92).copy(alpha = 0.4f)), shape = CircleShape)
                        )

                        // Inner focal breathe circle
                        Box(
                            modifier = Modifier
                                .size((80 * circleScaleAnim).dp)
                                .background(Color(0xFF814B92).copy(alpha = 0.25f), shape = CircleShape)
                                .border(BorderStroke(1.dp, Color(0xFF814B92).copy(alpha = 0.6f)), shape = CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    // Guiding Prompt text
                    Text(
                        text = breathPhase,
                        color = Color(0xFF814B92),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Progress indicators
                    Text(
                        text = "Breath $breathCycle of 3",
                        color = Color(0xFF1B8097),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Good to go button
                    Button(
                        onClick = {
                            if (screenMode == ChatScreenMode.BREATHING_INTERCEPT) {
                                viewModel.pendingSentimentMessage.value = null
                                screenMode = ChatScreenMode.CHAT
                            } else {
                                screenMode = ChatScreenMode.SPLASH
                            }
                        },
                        modifier = Modifier
                            .width(160.dp)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1B8097),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "Good to go", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        // Dialog for starting new chat when one is active
        if (showNewChatDialog) {
            AlertDialog(
                onDismissRequest = { showNewChatDialog = false },
                containerColor = DarkSurface,
                title = {
                    Text(
                        text = "Start New Chat?",
                        color = SoftPurple,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "Would you like to clear your last conversation? If you choose 'No', Urti will save it to your Archives first.",
                        color = LightGray
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.startNewChat(clearPrevious = true)
                            showNewChatDialog = false
                            screenMode = ChatScreenMode.CHAT
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AlertRed,
                            contentColor = Color.White
                        )
                    ) {
                        Text(text = "Yes, Clear It", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            viewModel.startNewChat(clearPrevious = false)
                            showNewChatDialog = false
                            screenMode = ChatScreenMode.CHAT
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SoftPurple,
                            contentColor = AmoledBlack
                        )
                    ) {
                        Text(text = "No, Save It", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Dialog for renaming archived chat title
        showRenameDialog?.let { session ->
            AlertDialog(
                onDismissRequest = { showRenameDialog = null },
                containerColor = DarkSurface,
                title = {
                    Text(
                        text = "Rename Conversation",
                        color = SoftPurple,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Enter a friendly title to save this conversation under:",
                            color = LightGray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        TextField(
                            value = renameInput,
                            onValueChange = { renameInput = it },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = AmoledBlack,
                                unfocusedContainerColor = AmoledBlack,
                                focusedTextColor = LightGray,
                                unfocusedTextColor = LightGray,
                                focusedIndicatorColor = SoftPurple,
                                unfocusedIndicatorColor = DarkBorder
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (renameInput.trim().isNotEmpty()) {
                                viewModel.renameArchive(session.id, renameInput)
                                showRenameDialog = null
                                Toast.makeText(context, "Renamed successfully", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SoftPurple,
                            contentColor = AmoledBlack
                        )
                    ) {
                        Text(text = "Save", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = null }) {
                        Text(text = "Cancel", color = MutedGray)
                    }
                }
            )
        }

        // Meditative Breathing Loop Evaluation Dialog (Yes / Continue)
        if (showBreathDialog) {
            AlertDialog(
                onDismissRequest = { /* Force explicit decision */ },
                containerColor = DarkSurface,
                title = {
                    Text(
                        text = "Feeling better?",
                        color = Color(0xFF1B8097),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    // YES option - Dismiss & show rotating encouragement popup
                    Button(
                        onClick = {
                            // Select and rotate encouragement text
                            val sharedPrefs = context.getSharedPreferences("urticare_prefs", android.content.Context.MODE_PRIVATE)
                            val currentIndex = sharedPrefs.getInt("breath_msg_index", 0)
                            encouragementMessage = breathMessages[currentIndex]
                            sharedPrefs.edit().putInt("breath_msg_index", (currentIndex + 1) % 10).apply()
                            
                            // Exit breathing mode (either return to CHAT for intercept or SPLASH for normal)
                            if (screenMode == ChatScreenMode.BREATHING_INTERCEPT) {
                                screenMode = ChatScreenMode.CHAT
                            } else {
                                screenMode = ChatScreenMode.SPLASH
                            }
                            showBreathDialog = false
                            breathCycle = 1
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B8097), contentColor = AmoledBlack),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Yes", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    // CONTINUE option - restarts loop immediately with no messages
                    Button(
                        onClick = {
                            showBreathDialog = false
                            breathCycle = 1
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurface, contentColor = Color(0xFF1B8097)),
                        border = BorderStroke(1.dp, Color(0xFF1B8097).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Continue", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Encouragement message popup note display
        encouragementMessage?.let { note ->
            AlertDialog(
                onDismissRequest = { 
                    encouragementMessage = null 
                    if (viewModel.pendingSentimentMessage.value != null) {
                        viewModel.completeSentimentBreathing()
                    }
                },
                containerColor = DarkSurface,
                modifier = Modifier.border(1.dp, SoftPurple.copy(alpha = 0.5f), shape = RoundedCornerShape(28.dp)),
                title = {
                    Text(
                        text = "🧘 A Moment of Encouragement",
                        color = SoftPurple,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                },
                text = {
                    Text(
                        text = note,
                        color = LightGray,
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic,
                        lineHeight = 20.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { 
                            encouragementMessage = null 
                            if (viewModel.pendingSentimentMessage.value != null) {
                                viewModel.completeSentimentBreathing()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SoftPurple, contentColor = AmoledBlack)
                    ) {
                        Text(text = "Grounded & Ready", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}
