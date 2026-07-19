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
import androidx.compose.ui.window.Dialog
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

    val currentChatLanguage by viewModel.currentChatLanguage.collectAsState()

    var showLanguageSelectorForNewChat by remember { mutableStateOf(false) }
    var clearPreviousForNewChat by remember { mutableStateOf(false) }

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
                                    clearPreviousForNewChat = true
                                    showLanguageSelectorForNewChat = true
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
                            Text(text = UrtiChatUiTranslations.get("new_chat", currentChatLanguage), fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                                text = UrtiChatUiTranslations.get("resume_last", currentChatLanguage),
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
                                        text = UrtiChatUiTranslations.get("take_breath", currentChatLanguage),
                                        color = Color(0xFF1B8097),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = UrtiChatUiTranslations.get("take_breath_sub", currentChatLanguage),
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
                            text = UrtiChatUiTranslations.get("archived", currentChatLanguage),
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
                                        text = "${session.messages.size} ${UrtiChatUiTranslations.get("messages", currentChatLanguage)}",
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
                                            Toast.makeText(context, if (currentChatLanguage == "AR") "تم حذف المحادثة" else "Chat deleted", Toast.LENGTH_SHORT).show()
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
                        Text(text = UrtiChatUiTranslations.get("back", currentChatLanguage), color = SoftPurple, fontWeight = FontWeight.Bold)
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
                            text = if (isTyping) UrtiChatUiTranslations.get("typing", currentChatLanguage) else UrtiChatUiTranslations.get("guardian", currentChatLanguage),
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
                            text = UrtiChatUiTranslations.get("synced", currentChatLanguage),
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
                                val isRtl = currentChatLanguage == "AR" && message.sender != "User"
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = if (isRtl) Alignment.End else Alignment.Start
                                ) {
                                    Text(
                                        text = message.text,
                                        color = LightGray,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
                                        textAlign = if (isRtl) TextAlign.Right else TextAlign.Left
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
                        placeholder = { Text(text = UrtiChatUiTranslations.get("placeholder", currentChatLanguage), color = MutedGray, fontSize = 13.sp) },
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
                        text = UrtiChatUiTranslations.get(breathPhase, currentChatLanguage),
                        color = Color(0xFF814B92),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Progress indicators
                    Text(
                        text = UrtiChatUiTranslations.get("breath_progress", currentChatLanguage).format(breathCycle),
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
                        Text(text = UrtiChatUiTranslations.get("good_to_go", currentChatLanguage), fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                        text = UrtiChatUiTranslations.get("start_new_title", currentChatLanguage),
                        color = SoftPurple,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = UrtiChatUiTranslations.get("start_new_text", currentChatLanguage),
                        color = LightGray
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            clearPreviousForNewChat = true
                            showNewChatDialog = false
                            showLanguageSelectorForNewChat = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AlertRed,
                            contentColor = Color.White
                        )
                    ) {
                        Text(text = UrtiChatUiTranslations.get("yes_clear", currentChatLanguage), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            clearPreviousForNewChat = false
                            showNewChatDialog = false
                            showLanguageSelectorForNewChat = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SoftPurple,
                            contentColor = AmoledBlack
                        )
                    ) {
                        Text(text = UrtiChatUiTranslations.get("no_save", currentChatLanguage), fontWeight = FontWeight.Bold)
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
                        text = UrtiChatUiTranslations.get("rename_title", currentChatLanguage),
                        color = SoftPurple,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            text = UrtiChatUiTranslations.get("rename_text", currentChatLanguage),
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
                                val renamedSuccess = when (currentChatLanguage) {
                                    "AR" -> "\u062A\u0645\u062A \u0625\u0639\u0627\u062F\u0629 \u0627\u0644\u062A\u0633\u0645\u064A\u0629 \u0628\u0646\u062C\u0627\u062D"
                                    "FR" -> "Renomm\u00E9 avec succ\u00E8s"
                                    "ES" -> "Renombrado con \u00E9xito"
                                    "IT" -> "Rinominato con successo"
                                    "DE" -> "Erfolgreich umbenannt"
                                    "NL" -> "Succesvol hernoemd"
                                    else -> "Renamed successfully"
                                }
                                Toast.makeText(context, renamedSuccess, Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SoftPurple,
                            contentColor = AmoledBlack
                        )
                    ) {
                        Text(text = UrtiChatUiTranslations.get("save", currentChatLanguage), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = null }) {
                        Text(text = UrtiChatUiTranslations.get("cancel", currentChatLanguage), color = MutedGray)
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
                        text = UrtiChatUiTranslations.get("feeling_better", currentChatLanguage),
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
                        Text(text = UrtiChatUiTranslations.get("yes", currentChatLanguage), fontWeight = FontWeight.Bold)
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
                        Text(text = UrtiChatUiTranslations.get("continue", currentChatLanguage), fontWeight = FontWeight.Bold)
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
                        text = UrtiChatUiTranslations.get("encouragement_title", currentChatLanguage),
                        color = SoftPurple,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                },
                text = {
                    Text(
                        text = translateEncouragementMessage(note, currentChatLanguage),
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
                        Text(text = UrtiChatUiTranslations.get("grounded_ready", currentChatLanguage), fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Language Selector Dialog
        if (showLanguageSelectorForNewChat) {
            Dialog(onDismissRequest = { showLanguageSelectorForNewChat = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(1.5.dp, DarkBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = UrtiChatUiTranslations.get("choose_lang_title", currentChatLanguage),
                            color = SoftPurple,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = UrtiChatUiTranslations.get("choose_lang_text", currentChatLanguage),
                            color = LightGray,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        val chatLanguages = listOf(
                            "EN" to "English \uD83C\uDDEC\uD83C\uDDE7",
                            "DE" to "Deutsch \uD83C\uDDE9\uD83C\uDDEA",
                            "ES" to "Espa\u00F1ol \uD83C\uDDEA\uD83C\uDDF8",
                            "FR" to "Fran\u00E7ais \uD83C\uDDEB\uD83C\uDDF7",
                            "IT" to "Italiano \uD83C\uDDEE\uD83C\uDDF9",
                            "NL" to "Nederlands \uD83C\uDDF3\uD83C\uDDF1",
                            "AR" to "\u0627\u0644\u0639\u0631\u0628\u064A\u0629 \uD83C\uDDEA\uD83C\uDDEC"
                        )

                        chatLanguages.forEach { (code, label) ->
                            Button(
                                onClick = {
                                    showLanguageSelectorForNewChat = false
                                    viewModel.startNewChat(clearPreviousForNewChat, code)
                                    screenMode = ChatScreenMode.CHAT
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .border(1.dp, DarkBorder, RoundedCornerShape(8.dp)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(text = label, color = LightGray, fontSize = 14.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(onClick = { showLanguageSelectorForNewChat = false }) {
                            Text(text = UrtiChatUiTranslations.get("cancel", currentChatLanguage), color = SoftPurple, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

object UrtiChatUiTranslations {
    val translations = mapOf(
        "EN" to mapOf(
            "back" to "← Back",
            "typing" to "typing...",
            "guardian" to "Your well-being guardian",
            "synced" to "Urti is synced with your live logs",
            "placeholder" to "Vent, pause or breathe...",
            "new_chat" to "New Chat",
            "resume_last" to "Resume Last Chat",
            "take_breath" to "Take a breath",
            "take_breath_sub" to "Pause for a moment to inhale, exhale, and find peace",
            "archived" to "ARCHIVED CONVERSATIONS",
            "messages" to "messages",
            "start_new_title" to "Start New Chat?",
            "start_new_text" to "Would you like to clear your last conversation? If you choose 'No', Urti will save it to your Archives first.",
            "yes_clear" to "Yes, Clear It",
            "no_save" to "No, Save It",
            "rename_title" to "Rename Conversation",
            "rename_text" to "Enter a friendly title to save this conversation under:",
            "save" to "Save",
            "cancel" to "Cancel",
            "breath_progress" to "Breath %d of 3",
            "good_to_go" to "Good to go",
            "feeling_better" to "Feeling better?",
            "yes" to "Yes",
            "continue" to "Continue",
            "encouragement_title" to "🧘 A Moment of Encouragement",
            "grounded_ready" to "Grounded & Ready",
            "choose_lang_title" to "💬 Chat with Urti",
            "choose_lang_text" to "Choose a language for the conversation:",
            "Breathe in..." to "Breathe in...",
            "Hold..." to "Hold...",
            "Breathe out..." to "Breathe out...",
            "Rest..." to "Rest..."
        ),
        "AR" to mapOf(
            "back" to "← رجوع",
            "typing" to "يكتب...",
            "guardian" to "حارس عافيتك",
            "synced" to "أورتي متزامن مع سجلاتك المباشرة",
            "placeholder" to "فضفض، توقف مؤقتاً أو تنفس...",
            "new_chat" to "محادثة جديدة",
            "resume_last" to "استئناف المحادثة الأخيرة",
            "take_breath" to "خذ نفسًا",
            "take_breath_sub" to "توقف مؤقتًا للاستنشاق والزفير وإيجاد السلام",
            "archived" to "المحادثات المؤرشفة",
            "messages" to "رسائل",
            "start_new_title" to "بدء محادثة جديدة؟",
            "start_new_text" to "هل ترغب في مسح محادثتك الأخيرة؟ إذا اخترت 'لا'، سيقوم أورتي بحفظها في أرشيفك أولاً.",
            "yes_clear" to "نعم، امسحها",
            "no_save" to "لا، احفظها",
            "rename_title" to "إعادة تسمية المحادثة",
            "rename_text" to "أدخل عنوانًا وديًا لحفظ هذه المحادثة تحته:",
            "save" to "حفظ",
            "cancel" to "إلغاء",
            "breath_progress" to "نفس %d من 3",
            "good_to_go" to "جاهز للانطلاق",
            "feeling_better" to "هل تشعر بتحسن؟",
            "yes" to "نعم",
            "continue" to "استمر",
            "encouragement_title" to "🧘 لحظة تشجيع",
            "grounded_ready" to "مستعد ومتماسك",
            "choose_lang_title" to "💬 الدردشة مع أورتي",
            "choose_lang_text" to "اختر لغة للمحادثة:",
            "Breathe in..." to "استنشق...",
            "Hold..." to "اكتم النفس...",
            "Breathe out..." to "ازفر...",
            "Rest..." to "استرح..."
        ),
        "FR" to mapOf(
            "back" to "← Retour",
            "typing" to "écrit...",
            "guardian" to "Votre gardien de bien-être",
            "synced" to "Urti est synchronisé avec vos journaux",
            "placeholder" to "Exprimez-vous, faites une pause...",
            "new_chat" to "Nouvelle discussion",
            "resume_last" to "Reprendre la discussion",
            "take_breath" to "Prendre une respiration",
            "take_breath_sub" to "Faites une pause pour inspirer, expirer et trouver la paix",
            "archived" to "CONVERSATIONS ARCHIVÉES",
            "messages" to "messages",
            "start_new_title" to "Nouvelle discussion ?",
            "start_new_text" to "Voulez-vous effacer votre dernière conversation ? Si vous choisissez 'Non', Urti la sauvegardera d'abord dans vos archives.",
            "yes_clear" to "Oui, effacer",
            "no_save" to "Non, sauvegarder",
            "rename_title" to "Renommer la conversation",
            "rename_text" to "Entrez un titre pour sauvegarder cette conversation :",
            "save" to "Enregistrer",
            "cancel" to "Annuler",
            "breath_progress" to "Respiration %d sur 3",
            "good_to_go" to "Prêt à partir",
            "feeling_better" to "Vous vous sentez mieux ?",
            "yes" to "Oui",
            "continue" to "Continuer",
            "encouragement_title" to "🧘 Un moment d'encouragement",
            "grounded_ready" to "Ancré et prêt",
            "choose_lang_title" to "💬 Chat avec Urti",
            "choose_lang_text" to "Choisissez une langue pour la conversation :",
            "Breathe in..." to "Inspirez...",
            "Hold..." to "Bloquez...",
            "Breathe out..." to "Expirez...",
            "Rest..." to "Pause..."
        ),
        "ES" to mapOf(
            "back" to "← Atrás",
            "typing" to "escribiendo...",
            "guardian" to "Tu guardián de bienestar",
            "synced" to "Urti está sincronizado con tus registros",
            "placeholder" to "Desahógate, haz una pausa o respira...",
            "new_chat" to "Nueva conversación",
            "resume_last" to "Reanudar última conversación",
            "take_breath" to "Tómate un respiro",
            "take_breath_sub" to "Pausa un momento para inhalar, exhalar y encontrar la paz",
            "archived" to "CONVERSACIONES ARCHIVADAS",
            "messages" to "mensajes",
            "start_new_title" to "¿Iniciar nueva conversación?",
            "start_new_text" to "¿Te gustaría borrar tu última conversación? Si eliges 'No', Urti la guardará primero en tus archivos.",
            "yes_clear" to "Sí, borrar",
            "no_save" to "No, guardar",
            "rename_title" to "Renombrar conversación",
            "rename_text" to "Escribe un título para guardar esta conversación:",
            "save" to "Guardar",
            "cancel" to "Cancelar",
            "breath_progress" to "Respiración %d de 3",
            "good_to_go" to "Listo para continuar",
            "feeling_better" to "¿Te sientes mejor?",
            "yes" to "Sí",
            "continue" to "Continuar",
            "encouragement_title" to "🧘 Un momento de aliento",
            "grounded_ready" to "Conectado y listo",
            "choose_lang_title" to "💬 Chat con Urti",
            "choose_lang_text" to "Elige un idioma para la conversación:",
            "Breathe in..." to "Inhala...",
            "Hold..." to "Mantén...",
            "Breathe out..." to "Exhala...",
            "Rest..." to "Descansa..."
        ),
        "IT" to mapOf(
            "back" to "← Indietro",
            "typing" to "sta scrivendo...",
            "guardian" to "Il tuo custode del benessere",
            "synced" to "Urti è sincronizzato con i tuoi registri",
            "placeholder" to "Sfogati, fai una pausa o respira...",
            "new_chat" to "Nuova chat",
            "resume_last" to "Riprendi ultima chat",
            "take_breath" to "Fai un respiro",
            "take_breath_sub" to "Mettiti in pausa per inspirare, espirare e trovare la pace",
            "archived" to "CONVERSAZIONI ARCHIVIATE",
            "messages" to "messaggi",
            "start_new_title" to "Iniziare nuova chat?",
            "start_new_text" to "Vuoi cancellare la tua ultima conversazione? Se scegli 'No', Urti la salverà prima nei tuoi archivi.",
            "yes_clear" to "Sì, cancella",
            "no_save" to "No, salva",
            "rename_title" to "Rinomina conversazione",
            "rename_text" to "Inserisci un titolo descrittivo per cette conversazione:",
            "save" to "Salva",
            "cancel" to "Annulla",
            "breath_progress" to "Respiro %d di 3",
            "good_to_go" to "Pronto per partire",
            "feeling_better" to "Ti senti meglio?",
            "yes" to "Sì",
            "continue" to "Continua",
            "encouragement_title" to "🧘 Un moment de d'encouragement",
            "grounded_ready" to "Radicato e pronto",
            "choose_lang_title" to "💬 Chat con Urti",
            "choose_lang_text" to "Scegli una lingua per la conversazione:",
            "Breathe in..." to "Inspira...",
            "Hold..." to "Trattieni...",
            "Breathe out..." to "Espira...",
            "Rest..." to "Riposa..."
        ),
        "DE" to mapOf(
            "back" to "← Zurück",
            "typing" to "schreibt...",
            "guardian" to "Ihr Wächter des Wohlbefindens",
            "synced" to "Urti ist mit Ihren Live-Protokollen synchronisiert",
            "placeholder" to "Lassen Sie Dampf ab, atmen Sie durch...",
            "new_chat" to "Neuer Chat",
            "resume_last" to "Letzten Chat fortsetzen",
            "take_breath" to "Atmen Sie durch",
            "take_breath_sub" to "Innehalten, einatmen, ausatmen und Ruhe finden",
            "archived" to "ARCHIVIERTE CONVERSATIONEN",
            "messages" to "Nachrichten",
            "start_new_title" to "Neuen Chat starten?",
            "start_new_text" to "Möchten Sie Ihr letztes Gespräch löschen? Wenn Sie 'Nein' wählen, speichert Urti es zuerst im Archiv.",
            "yes_clear" to "Ja, löschen",
            "no_save" to "Nein, speichern",
            "rename_title" to "Gespräch umbenennen",
            "rename_text" to "Geben Sie einen Titel ein, unter dem das Gespräch gespeichert werden soll:",
            "save" to "Speichern",
            "cancel" to "Abbrechen",
            "breath_progress" to "Atemzug %d von 3",
            "good_to_go" to "Bereit zum Fortfahren",
            "feeling_better" to "Fühlen Sie sich besser?",
            "yes" to "Ja",
            "continue" to "Fortfahren",
            "encouragement_title" to "🧘 Ein Moment der Ermutigung",
            "grounded_ready" to "Geerdet & Bereit",
            "choose_lang_title" to "💬 Chat mit Urti",
            "choose_lang_text" to "Wählen Sie eine Sprache für das Gespräch:",
            "Breathe in..." to "Einatmen...",
            "Hold..." to "Anhalten...",
            "Breathe out..." to "Ausatmen...",
            "Rest..." to "Ausruhen..."
        ),
        "NL" to mapOf(
            "back" to "← Terug",
            "typing" to "typt...",
            "guardian" to "Jouw welzijnsbewaker",
            "synced" to "Urti is gesynchroniseerd met je logs",
            "placeholder" to "Lucht je hart, pauzeer of adem...",
            "new_chat" to "Nieuwe chat",
            "resume_last" to "Hervat laatste chat",
            "take_breath" to "Haal adem",
            "take_breath_sub" to "Pauzeer even om in te ademen, uit te ademen en rust te vinden",
            "archived" to "GEARCHIVEERDE GESPREKKEN",
            "messages" to "berichten",
            "start_new_title" to "Nieuwe chat starten?",
            "start_new_text" to "Wil je je laatste gesprek wissen? Als je 'Nee' kiest, slaat Urti het eerst op in je archief.",
            "yes_clear" to "Ja, wissen",
            "no_save" to "Nee, opslaan",
            "rename_title" to "Gesprek hernoemen",
            "rename_text" to "Voer een titel in om dit gesprek onder op te slaan:",
            "save" to "Opslaan",
            "cancel" to "Annuleren",
            "breath_progress" to "Ademhaling %d van 3",
            "good_to_go" to "Klaar om te gaan",
            "feeling_better" to "Voel je je beter?",
            "yes" to "Ja",
            "continue" to "Doorgaan",
            "encouragement_title" to "🧘 Een moment van bemoediging",
            "grounded_ready" to "Geaard & Klaar",
            "choose_lang_title" to "💬 Chat met Urti",
            "choose_lang_text" to "Kies een taal voor het gesprek:",
            "Breathe in..." to "Inademen...",
            "Hold..." to "Vasthouden...",
            "Breathe out..." to "Uitademen...",
            "Rest..." to "Rusten..."
        )
    )

    fun get(key: String, lang: String): String {
        return translations[lang]?.get(key) ?: translations["EN"]?.get(key) ?: ""
    }
}

fun translateEncouragementMessage(message: String, lang: String): String {
    if (lang == "EN") return message
    val translations = mapOf(
        "With that breath, you just reset your day; you are fully capable of handling whatever comes next." to mapOf(
            "AR" to "مع هذا النفس، لقد أعدت ضبط يومك للتو؛ أنت قادر تمامًا على التعامل مع كل ما يأتي بعد ذلك.",
            "FR" to "Avec cette respiration, vous venez de réinitialiser votre journée ; vous êtes pleinement capable de gérer tout ce qui vient ensuite.",
            "ES" to "Con ese respiro, acabas de reiniciar tu día; eres plenamente capaz de manejar lo que venga a continuación.",
            "IT" to "Con quel respiro hai appena resettato la tua giornata; sei perfettamente in grado di gestire qualsiasi cosa accada dopo.",
            "DE" to "Mit diesem Atemzug haben Sie Ihren Tag gerade zurückgesetzt; Sie sind voll und ganz in der Lage, alles zu meistern, was als Nächstes kommt.",
            "NL" to "Met die ademhaling heb je zojuist je dag gereset; je bent volledig in staat om te gaan met wat er hierna komt."
        ),
        "You have survived 100% of your hardest days so far, and you are doing much better than you give yourself credit for." to mapOf(
            "AR" to "لقد نجوت من 100% من أصعب أيامك حتى الآن، وأنت تبلي بلاءً أفضل بكثير مما تنسبه لنفسك.",
            "FR" to "Vous avez survécu à 100 % de vos jours les plus difficiles jusqu'à présent, et vous vous en sortez bien mieux que vous ne le pensez.",
            "ES" to "Has sobrevivido al 100 % de tus días más difíciles hasta ahora, y lo estás haciendo mucho mejor de lo que te reconoces.",
            "IT" to "Finora sei sopravvissuto al 100% dei tuoi giorni più difficili e stai andando molto meglio di quanto ti attribuisci.",
            "DE" to "Sie haben bisher 100 % Ihrer schwersten Tage überstanden, und Sie schlagen sich viel besser, als Sie sich selbst eingestehen.",
            "NL" to "Je hebt tot nu toe 100% van je moeilijkste dagen overleefd, en je doet het veel beter dan je jezelf toeschrijft."
        ),
        "In this exact moment, you are safe, you are grounded, and you are completely in control of your next step." to mapOf(
            "AR" to "في هذه اللحظة بالذات، أنت آمن، أنت ثابت، وأنت مسيطر تمامًا على خطوتك التالية.",
            "FR" to "En ce moment précis, vous êtes en sécurité, vous êtes ancré et vous contrôlez totalement votre prochaine étape.",
            "ES" to "En este preciso momento, estás a salvo, conectado a tierra y tienes el control absoluto de tu próximo paso.",
            "IT" to "In questo preciso momento sei al sicuro, sei radicato e hai il controllo totale del tuo prossimo passo.",
            "DE" to "In diesem exakten Moment bist du in Sicherheit, geerdet und hast die volle Kontrolle über deinen nächsten Schritt.",
            "NL" to "Op dit exacte moment ben je veilig, ben je geaard en heb je de volledige controle over je volgende stap."
        ),
        "Progress isn't about rushing forward; sometimes, the greatest progress is simply pausing, breathing, and choosing to continue." to mapOf(
            "AR" to "التقدم لا يتعلق بالاندفاع إلى الأمام؛ في بعض الأحيان، يكون أعظم تقدم هو ببساطة التوقف، التنفس، واختيار الاستمرار.",
            "FR" to "Le progrès ne consiste pas à se précipiter ; parfois, le plus grand progrès consiste simplement à faire une pause, à respirer et à choisir de continuer.",
            "ES" to "El progreso no se trata de correr hacia adelante; a veces, el mayor progreso es simplemente detenerse, respirar y elegir continuar.",
            "IT" to "Il progresso non consiste nel correre in avanti; a volte, il progresso più grande è semplicemente fermarsi, respirare e scegliere di continuare.",
            "DE" to "Fortschritt bedeutet nicht, nach vorne zu stürmen. Manchmal ist der größte Fortschritt einfach innezuhalten, durchzuatmen und sich fürs Weitermachen zu entscheiden.",
            "NL" to "Vooruitgang gaat niet over vooruit haasten; soms is de grootste vooruitgang simpelweg pauzeren, ademen en ervoor kiezen om door te gaan."
        ),
        "You don't have to figure out the whole mountain right now—just focus on the very next step in front of you." to mapOf(
            "AR" to "ليس عليك معرفة الجبل بأكمله الآن - فقط ركز على الخطوة التالية أمامك مباشرة.",
            "FR" to "Vous n'avez pas besoin de comprendre toute la montagne pour le moment ; concentrez-vous simplement sur la toute prochaine étape devant vous.",
            "ES" to "No tienes que descifrar toda la montaña ahora mismo; solo concéntrate en el siguiente paso que tienes por delante.",
            "IT" to "Non devi scalare l'intera montagna in questo momento; concentrati solo sul passo successivo direttamente di fronte a te.",
            "DE" to "Sie müssen nicht gleich den ganzen Berg bezwingen; konzentrieren Sie sich einfach auf den allernächsten Schritt vor Ihnen.",
            "NL" to "Je hoeft nu niet de hele berg te overzien; concentreer je gewoon op de allereerste volgende stap voor je."
        ),
        "Your strength isn't measured by never feeling overwhelmed, but by your beautiful ability to breathe through it and find your peace again." to mapOf(
            "AR" to "لا تُقاس قوتك بعدم الشعور بالإرهاق أبدًا، بل بقدرتك الجميلة على التنفس من خلاله والعثور على سلامك مرة أخرى.",
            "FR" to "Votre force ne se mesure pas au fait de ne jamais vous sentir dépassé, mais à votre belle capacité à respirer à travers cela et à retrouver votre paix.",
            "ES" to "Tu fuerza no se mide por no sentirte abrumado nunca, sino por tu hermosa capacidad de respirar a través de ello y encontrar tu paz de nuevo.",
            "IT" to "La tua forza non si misura dal non sentirti mai sopraffatto, ma dalla tua bellissima capacità di respirarci dentro e ritrovare la pace.",
            "DE" to "Ihre Stärke misst sich nicht daran, dass Sie sich nie überfordert fühlen, sondern an Ihrer wunderbaren Fähigkeit, tief durchzuatmen und Ihren Frieden wiederzufinden.",
            "NL" to "Je kracht wordt niet gemeten door je nooit overweldigd te voelen, maar door je prachtige vermogen om er doorheen te ademen en je rust weer te vinden."
        ),
        "Let go of what you cannot control in this moment, and pour your incredible energy into what you can." to mapOf(
            "AR" to "اترك ما لا يمكنك التحكم فيه في هذه اللحظة، وصب طاقتك الرائعة فيما يمكنك التحكم فيه.",
            "FR" to "Lâchez ce que vous ne pouvez pas contrôler pour le moment et consacrez votre incroyable énergie à ce que vous pouvez contrôler.",
            "ES" to "Deja ir lo que no puedes controlar en este momento y vierte tu increíble energía en lo que sí puedes.",
            "IT" to "Lascia andare ciò che non puoi controllare in questo momento e riversa la tua incredibile energia in ciò que puoi.",
            "DE" to "Lassen Sie los, was Sie in diesem Moment nicht kontrollieren können, und stecken Sie Ihre unglaubliche Energie in das, was Sie können.",
            "NL" to "Laat los wat je op dit moment niet kunt controleren en steek je ongelooflijke energie in wat je wel kunt."
        ),
        "Slow down, give yourself some grace, and remember that you are allowed to be a masterpiece and a work in progress all at the same time." to mapOf(
            "AR" to "أبطئ قليلاً، وامنح نفسك بعض النعمة، وتذكر أنه يُسمح لك بأن تكون تحفة فنية وعملاً قيد الإنجاز في نفس الوقت.",
            "FR" to "Ralentissez, accordez-vous un peu de grâce et rappelez-vous que vous avez le droit d'être à la fois un chef-d'œuvre et une œuvre en cours.",
            "ES" to "Ve más despacio, date un poco de gracia y recuerda que puedes ser una obra maestra y una obra en progreso al mismo tiempo.",
            "IT" to "Rallenta, concediti un po' di grazia e ricorda che ti è permesso essere un capolavoro e un lavoro in corso allo stesso tempo.",
            "DE" to "Werden Sie langsamer, schenken Sie sich selbst etwas Gnade und denken Sie daran, dass Sie gleichzeitig ein Meisterwerk und eine Baustelle sein dürfen.",
            "NL" to "Doe rustiger aan, wees lief voor jezelf en onthoud dat je tegelijkertijd een meesterwerk en een werk in uitvoering mag zijn."
        ),
        "The weight you’ve been carrying is heavy, but so is your resilience. Trust yourself—you've got this." to mapOf(
            "AR" to "الوزن الذي كنت تحمله ثقيل، ولكن مرونتك ثقيلة أيضًا. ثق بنفسك - يمكنك القيام بذلك.",
            "FR" to "Le poids que vous portez est lourd, mais votre résilience l'est tout autant. Faites-vous confiance, vous pouvez y arriver.",
            "ES" to "El peso que has estado cargando es grande, pero también lo es tu resiliencia. Confía en ti mismo: tú puedes con esto.",
            "IT" to "Il peso che hai portato è pesante, ma lo è anche la tua resilienza. Fidati di te stesso: puoi farcela.",
            "DE" to "Die Last, die Sie getragen haben, ist schwer, aber das gilt auch für Ihre Resilienz. Vertrauen Sie sich selbst – Sie schaffen das.",
            "NL" to "Het gewicht dat je draagt is zwaar, maar dat geldt ook voor je veerkracht. Vertrouw op jezelf — je kunt dit."
        ),
        "Every new breath is a completely fresh start and a reminder that you are strong enough to handle your own story." to mapOf(
            "AR" to "كل نفس جديد هو بداية جديدة تمامًا وتذكير بأنك قوي بما يكفي للتعامل مع قصتك الخاصة.",
            "FR" to "Chaque nouvelle respiration est un tout nouveau départ et un rappel que vous êtes assez fort pour assumer votre propre histoire.",
            "ES" to "Cada nuevo respiro es un comienzo completamente fresco y un recordatorio de que eres lo suficientemente fuerte como para manejar tu propia historia.",
            "IT" to "Ogni nuovo respiro è un inizio completamente nuovo e un promemoria del fatto che sei abbastanza forte da gestire la tua storia.",
            "DE" to "Jeder neue Atemzug ist ein völlig neuer Anfang und eine Erinnerung daran, dass Sie stark genug sind, Ihre eigene Geschichte zu schreiben.",
            "NL" to "Elke nieuwe ademhaling is een volledig frisse start en een herinnering dat je sterk genoeg bent om je eigen verhaal aan te kunnen."
        )
    )
    return translations[message]?.get(lang) ?: message
}
