package com.example.urticare.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.BackHandler
import com.example.urticare.R
import com.example.urticare.ui.theme.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.urticare.model.ChatMessage
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import com.example.urticare.data.LibrarySection
import com.example.urticare.data.LibraryTopic
import com.example.urticare.data.LibraryDataHolder

fun getUrtiGreeting(lang: String): String {
    return when (lang) {
        "AR" -> "👋 مرحبًا، أنا أورتي. لقد تم تدريبي على جميع المعلومات الموجودة في المكتبة ويمكنني المساعدة في شرح الأعراض والعلاجات والمحفزات والأدوية ونصائح نمط الحياة والمزيد.\n\nماذا تحب أن تتعلم اليوم؟"
        "FR" -> "👋 Salut, je suis Urti. J'ai été entraîné sur toutes les informations de la Bibliothèque et je peux vous aider à expliquer les symptômes, les traitements, les déclencheurs, les médicaments, les conseils de style de vie, et plus encore.\n\nQu'aimeriez-vous apprendre aujourd'hui ?"
        "ES" -> "👋 Hola, soy Urti. He sido entrenado con toda la información de la Biblioteca y puedo ayudarte a explicar los síntomas, tratamientos, desencadenantes, medicamentos, consejos sobre el estilo de vida y más.\n\n¿De qué te gustaría aprender hoy?"
        "IT" -> "👋 Ciao, sono Urti. Sono stato addestrato su tutte le informazioni presenti nella Biblioteca e posso aiutarti a spiegare sintomi, trattamenti, fattori scatenanti, farmaci, consigli sullo stile di vita e altro ancora.\n\nCosa vorresti imparare oggi?"
        "DE" -> "👋 Hallo, ich bin Urti. Ich wurde mit allen Informationen aus der Bibliothek trainiert und kann helfen, Symptome, Behandlungen, Auslöser, Medikamente, Tipps zum Lebensstil und mehr zu erklären.\n\nWorüber möchtest du heute mehr erfahren?"
        "NL" -> "👋 Hallo, ik ben Urti. Ik ben getraind op alle informatie in de Bibliotheek en kan helpen bij het uitleggen van symptomen, behandelingen, triggers, medicijnen, lifestyletips en meer.\n\nWaar wil je vandaag meer over leren?"
        else -> "👋 Hi, I'm Urti. I've been trained on all the information in the Library and can help explain symptoms, treatments, triggers, medications, lifestyle tips, and more.\n\nWhat would you like to learn about today?"
    }
}

fun getFallbackMessage(lang: String): String {
    return when (lang) {
        "AR" -> "لم أتمكن من العثور على قسم محدد في المقالات يجيب على ذلك. حاول السؤال عن الأعراض، أو العلاجات، أو المحفزات، أو الأدوية، أو نصائح نمط الحياة، أو موضوعات محددة مثل مضادات الهستامين، أو الشرى الكوليني، أو التوتر."
        "FR" -> "Je n'ai pas trouvé de section spécifique dans les articles pour répondre à cela. Essayez de poser des questions sur les symptômes, les traitements, les déclencheurs, les médicaments, les conseils de style de vie ou des sujets spécifiques comme les antihistaminiques, l'urticaire cholinergique ou le stress."
        "ES" -> "No pude encontrar una sección específica en los artículos que responda a eso. Intenta preguntar sobre síntomas, tratamientos, desencadenantes, medicamentos, consejos de estilo de vida o temas específicos como antihistamínicos, urticaria colinérgica o estrés."
        "IT" -> "Non sono riuscito a trovare una sezione specifica negli articoli che risponda a questa domanda. Prova a chiedere informazioni su sintomi, trattamenti, fattori scatenanti, farmaci, consigli sullo stile di vita o argomenti specifici come antistaminici, orticaria colinergica o stress."
        "DE" -> "Ich konnte keinen spezifischen Abschnitt in den Artikeln finden, der das beantwortet. Fragen Sie gerne nach Symptomen, Behandlungen, Auslösern, Medikamenten, Tipps zum Lebensstil oder speziellen Themen wie Antihistaminika, cholinergische Urtikaria oder Stress."
        "NL" -> "Ik kon geen specifiek gedeelte in de artikelen vinden dat dat beantwoordt. Probeer te vragen naar symptomen, behandelingen, triggers, medicijnen, lifestyletips of specifieke onderwerpen zoals antihistaminica, cholinergische urticaria of stress."
        else -> "I couldn't find a specific section in the articles answering that. Try asking about symptoms, treatments, triggers, medications, lifestyle tips, or specific topics like antihistamines, cholinergic urticaria, or stress."
    }
}

fun resolveArticleTitle(title: String, lang: String, context: Context): String {
    val article = LibraryDataHolder.getArticles(context)[title]
    return article?.titles?.get(lang) ?: title
}

fun resolveArticleBody(title: String, lang: String, fallbackSummary: String, context: Context): String {
    val article = LibraryDataHolder.getArticles(context)[title]
    return article?.content?.get(lang) ?: fallbackSummary
}

fun searchLibraryArticles(query: String, lang: String, sections: List<LibrarySection>, context: Context): String {
    val queryWords = query.lowercase().split(Regex("\\s+"))
        .map { it.replace(Regex("[^a-zA-Z0-9\\u0600-\\u06FF\\u00C0-\\u017F]"), "") }
        .filter { it.length > 2 }

    if (queryWords.isEmpty()) {
        return getFallbackMessage(lang)
    }

    var bestTopic: LibraryTopic? = null
    var bestTitle: String = ""
    var bestScore = 0
    var bestBody: String = ""

    for (section in sections) {
        for (topic in section.topics) {
            val englishTitle = resolveArticleTitle(topic.title, "EN", context)
            val englishBody = resolveArticleBody(topic.title, "EN", topic.summary, context)

            var score = 0
            val titleLower = englishTitle.lowercase()
            val bodyLower = englishBody.lowercase()

            for (word in queryWords) {
                if (titleLower.contains(word)) {
                    score += 15
                }
                var index = bodyLower.indexOf(word)
                while (index != -1) {
                    score += 3
                    index = bodyLower.indexOf(word, index + word.length)
                }
            }

            if (score > bestScore) {
                bestScore = score
                bestTopic = topic
                bestTitle = resolveArticleTitle(topic.title, lang, context)
                bestBody = resolveArticleBody(topic.title, lang, topic.summary, context)
            }
        }
    }

    if (bestScore > 0 && bestTopic != null) {
        val paragraphs = bestBody.split(Regex("\\n\\s*\\n+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        var bestPara1 = ""
        var bestPara2 = ""
        var pScore1 = -1
        var pScore2 = -1

        for (para in paragraphs) {
            var pScore = 0
            val paraLower = para.lowercase()
            for (word in queryWords) {
                if (paraLower.contains(word)) {
                    pScore += 10
                }
            }
            if (pScore > pScore1) {
                pScore2 = pScore1
                bestPara2 = bestPara1

                pScore1 = pScore
                bestPara1 = para
            } else if (pScore > pScore2) {
                pScore2 = pScore
                bestPara2 = para
            }
        }

        val resultBody = if (bestPara1.isNotEmpty()) {
            if (bestPara2.isNotEmpty() && pScore2 > 0) {
                "$bestPara1\n\n$bestPara2"
            } else {
                bestPara1
            }
        } else {
            paragraphs.firstOrNull() ?: bestBody
        }

        return when (lang) {
            "AR" -> "بناءً على مقال المكتبة **$bestTitle**، إليك ما وجدته:\n\n$resultBody"
            "FR" -> "D'après l'article de la bibliothèque **$bestTitle**, voici ce que j'ai trouvé :\n\n$resultBody"
            "ES" -> "Según el artículo de la biblioteca **$bestTitle**, esto es lo que encontré:\n\n$resultBody"
            "IT" -> "In base all'articolo della biblioteca **$bestTitle**, ecco cosa ho trovato:\n\n$resultBody"
            "DE" -> "Basierend auf dem Bibliotheksartikel **$bestTitle** habe ich Folgendes gefunden:\n\n$resultBody"
            "NL" -> "Op basis van het bibliotheekartikel **$bestTitle** is dit wat ik heb gevonden:\n\n$resultBody"
            else -> "Based on the Library article **$bestTitle**, here is what I found:\n\n$resultBody"
        }
    }

    return getFallbackMessage(lang)
}

@Composable
fun LibraryScreen(onBack: () -> Unit = {}) {
    var expandedSectionIndex by remember { mutableStateOf<Int?>(null) }
    var selectedTopic by remember { mutableStateOf<LibraryTopic?>(null) }

    var showLanguageSelector by remember { mutableStateOf(false) }
    var showUrtiChat by remember { mutableStateOf(false) }
    var selectedChatLang by remember { mutableStateOf("EN") }
    val urtiChatMessages = remember { mutableStateListOf<ChatMessage>() }

    BackHandler(enabled = selectedTopic == null && !showUrtiChat && !showLanguageSelector) {
        onBack()
    }
    var selectedLibraryLang by remember(selectedTopic) { mutableStateOf("EN") }

    val context = LocalContext.current
    val sections = remember(context) { getLibraryData(context) }

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                GradientBackButton(onClick = onBack, size = 34.dp)

                val brandTitleGradient = androidx.compose.ui.graphics.Brush.horizontalGradient(
                    listOf(Color(0xFF814B92), Color(0xFF509729), Color(0xFF1A7E97))
                )

                // Centered Title
                Text(
                    text = "Library",
                    style = TextStyle(
                        brush = brandTitleGradient,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                // Balanced spacer replaces the removed Urti Mascot logo to keep the title perfectly centered
                Spacer(modifier = Modifier.size(34.dp))
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp)
            ) {
                // Welcome / Intro Layout (Not in a box)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Learn about urticaria subtypes, mast-cell biological loops, medications, lifestyle triggers, and guidelines. Tap any section to view topics, and tap a topic to read its summary.",
                        color = Color(0xFF737373),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }

                // Ask Urti Round Button
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.urti_avatar),
                        contentDescription = "Ask Urti",
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.68f))
                            .border(1.5.dp, SoftPurple.copy(alpha = 0.8f), CircleShape)
                            .clickable { showLanguageSelector = true }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val brandTitleGradient = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(Color(0xFF814B92), Color(0xFF509729), Color(0xFF1A7E97))
                    )
                    Text(
                        text = "Ask Urti",
                        style = TextStyle(
                            brush = brandTitleGradient,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    )
                }

                // Collapsible Sections List
                sections.forEachIndexed { index, section ->
                    val isExpanded = expandedSectionIndex == index
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.68f)),
                        border = BorderStroke(1.dp, if (isExpanded) SoftPurple.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column {
                            // Section Header Clickable Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedSectionIndex = if (isExpanded) null else index
                                    }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = section.title,
                                        color = SoftPurple,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = section.description,
                                        color = Color(0xFF1B8097),
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    text = if (isExpanded) "▲" else "▼",
                                    color = SoftPurple,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }

                            // Topics list inside section
                            AnimatedVisibility(visible = isExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Transparent)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                  ) {
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.5f), thickness = 1.dp, modifier = Modifier.padding(bottom = 4.dp))
                                    section.topics.forEach { topic ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedTopic = topic }
                                                .padding(vertical = 10.dp, horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = "📄",
                                                    fontSize = 14.sp,
                                                    modifier = Modifier.padding(end = 10.dp)
                                                )
                                                Text(
                                                    text = topic.title,
                                                    color = Color(0xFF737373),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                            Text(
                                                text = "→",
                                                color = PastelIceBlue,
                                                fontSize = 14.sp
                                            )
                                        }
                                        HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Reading Dialog Popup
    if (selectedTopic != null) {
        Dialog(onDismissRequest = { selectedTopic = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.75f)
                    .border(1.5.dp, DarkBorder, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Title Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📚 Therapeutic Guide",
                            color = SoftPurple,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { selectedTopic = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Text("❌", color = Color.White, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val currentTitle = resolveArticleTitle(selectedTopic!!.title, selectedLibraryLang, context)
                    val currentBody = resolveArticleBody(selectedTopic!!.title, selectedLibraryLang, selectedTopic!!.summary, context)
                    
                    val article = LibraryDataHolder.getArticles(context)[selectedTopic!!.title]
                    val isRtl = selectedLibraryLang == "AR"
                    val hasTranslations = article != null && article.titles.size > 1

                    Text(
                        text = currentTitle,
                        color = Color(0xFF509729),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = if (isRtl) TextAlign.Right else TextAlign.Left,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )

                    val availableLanguages = if (hasTranslations) {
                        val order = listOf("EN", "AR", "FR", "ES", "IT", "DE", "NL")
                        article?.titles?.keys?.sortedBy { order.indexOf(it) }?.toList() ?: listOf("EN")
                    } else {
                        listOf("EN")
                    }

                    if (availableLanguages.size > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .background(DarkGray, shape = RoundedCornerShape(8.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            availableLanguages.forEach { lang ->
                                val isSelected = selectedLibraryLang == lang
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedLibraryLang = lang }
                                        .background(
                                            color = if (isSelected) SoftPurple else Color.Transparent,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = lang,
                                        color = if (isSelected) Color.White else MutedGray,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = DarkBorder, thickness = 1.dp, modifier = Modifier.padding(bottom = 12.dp))

                    // Summary Scrollable Container
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = currentBody,
                            color = LightGray,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            textAlign = if (isRtl) TextAlign.Right else TextAlign.Justify,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Close Button
                    Button(
                        onClick = { selectedTopic = null },
                        colors = ButtonDefaults.buttonColors(containerColor = SoftPurple),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                    ) {
                        Text(
                            text = "Close",
                            color = Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Language Selector Dialog
    if (showLanguageSelector) {
        Dialog(onDismissRequest = { showLanguageSelector = false }) {
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
                        text = "💬 Chat with Urti",
                        color = SoftPurple,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Choose a language for the conversation:",
                        color = LightGray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val chatLanguages = listOf(
                        "EN" to "English 🇬🇧",
                        "DE" to "Deutsch 🇩🇪",
                        "ES" to "Español 🇪🇸",
                        "FR" to "Français 🇫🇷",
                        "IT" to "Italiano 🇮🇹",
                        "NL" to "Nederlands 🇳🇱",
                        "AR" to "العربية 🇪🇬"
                    )

                    chatLanguages.forEach { (code, label) ->
                        Button(
                            onClick = {
                                selectedChatLang = code
                                showLanguageSelector = false
                                urtiChatMessages.clear()
                                urtiChatMessages.add(ChatMessage(sender = "Urti", text = getUrtiGreeting(code), timestamp = getCurrentTime()))
                                showUrtiChat = true
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
                    TextButton(onClick = { showLanguageSelector = false }) {
                        Text(text = "Cancel", color = SoftPurple, fontSize = 14.sp)
                    }
                }
            }
        }
    }

    // Urti Chat Dialog
    if (showUrtiChat) {
        var chatInput by remember { mutableStateOf("") }
        val listState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()

        Dialog(onDismissRequest = { showUrtiChat = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .border(1.5.dp, SoftPurple.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    // Chat Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.urti_avatar),
                                contentDescription = "Urti AI",
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Urti",
                                    color = SoftPurple,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Language: " + selectedChatLang,
                                    color = Color(0xFF1B8097),
                                    fontSize = 10.sp
                                )
                            }
                        }
                        IconButton(
                            onClick = { showUrtiChat = false },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Text("❌", color = Color.White, fontSize = 14.sp)
                        }
                    }

                    HorizontalDivider(color = DarkBorder, thickness = 1.dp, modifier = Modifier.padding(bottom = 8.dp))

                    // Message List
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(urtiChatMessages) { msg ->
                            val isRtl = selectedChatLang == "AR" && msg.sender != "User"
                            val isUser = msg.sender == "User"
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (isUser) SoftPurple else DarkSurface,
                                            shape = RoundedCornerShape(
                                                topStart = 12.dp,
                                                topEnd = 12.dp,
                                                bottomStart = if (isUser) 12.dp else 2.dp,
                                                bottomEnd = if (isUser) 2.dp else 12.dp
                                            )
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isUser) Color.Transparent else DarkBorder,
                                            shape = RoundedCornerShape(
                                                topStart = 12.dp,
                                                topEnd = 12.dp,
                                                bottomStart = if (isUser) 12.dp else 2.dp,
                                                bottomEnd = if (isUser) 2.dp else 12.dp
                                            )
                                        )
                                        .padding(12.dp)
                                        .widthIn(max = 260.dp)
                                ) {
                                    Text(
                                        text = parseMarkdown(msg.text),
                                        color = if (isUser) Color(0xFF1E1035) else LightGray,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                        textAlign = if (isRtl) TextAlign.Right else TextAlign.Left
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Input Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurface, shape = RoundedCornerShape(24.dp))
                            .border(1.dp, DarkBorder, RoundedCornerShape(24.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = chatInput,
                            onValueChange = { chatInput = it },
                            textStyle = TextStyle(color = LightGray, fontSize = 13.sp),
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 8.dp),
                            cursorBrush = SolidColor(SoftPurple),
                            decorationBox = { innerTextField ->
                                if (chatInput.isEmpty()) {
                                    Text(
                                        text = when (selectedChatLang) {
                                            "AR" -> "اسأل أورتي..."
                                            "DE" -> "Frage Urti..."
                                            "ES" -> "Preguntar a Urti..."
                                            "FR" -> "Poser une question..."
                                            "IT" -> "Chiedi a Urti..."
                                            "NL" -> "Vraag Urti..."
                                            else -> "Ask Urti..."
                                        },
                                        color = MutedGray,
                                        fontSize = 13.sp
                                    )
                                }
                                innerTextField()
                            }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (chatInput.trim().isNotEmpty()) {
                                    val userMsg = chatInput.trim()
                                    urtiChatMessages.add(ChatMessage(sender = "User", text = userMsg, timestamp = getCurrentTime()))
                                    chatInput = ""

                                    // Auto scroll
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(urtiChatMessages.size - 1)
                                    }

                                    // Search logic
                                    val response = searchLibraryArticles(userMsg, selectedChatLang, sections, context)
                                    urtiChatMessages.add(ChatMessage(sender = "Urti", text = response, timestamp = getCurrentTime()))

                                    // Auto scroll after response
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(urtiChatMessages.size - 1)
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .background(SoftPurple, CircleShape)
                        ) {
                            Text("➔", color = Color.Black, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun getCurrentTime(): String {
    return java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
}

private fun parseMarkdown(text: String): AnnotatedString {
    val builder = AnnotatedString.Builder()
    val parts = text.split("**")
    var isBold = false
    for (part in parts) {
        if (isBold) {
            builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            builder.append(part)
            builder.pop()
        } else {
            builder.append(part)
        }
        isBold = !isBold
    }
    return builder.toAnnotatedString()
}

private fun getLibraryData(context: Context): List<LibrarySection> {
    return LibraryDataHolder.getSections(context)
}
