package de.healthai.eatelligent.ui.home

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.healthai.eatelligent.Gender
import de.healthai.eatelligent.UserConfiguration
import de.healthai.eatelligent.data.MealEntry
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class MealHomeTab { Capture, History, Settings }

private val Peach = Color(0xFFFFB38E)
private val Mint = Color(0xFFB7F0AD)
private val Sky = Color(0xFFAEE7FF)
private val Lilac = Color(0xFFCFB7FF)

private val FriendlyGradient = Brush.verticalGradient(
    0f to Sky,
    0.45f to Mint,
    1f to Peach
)

private val ChatCenterGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFFF5F1FF),
        Color(0xFFEFF8FF)
    )
)

private const val CHAT_SYSTEM_PROMPT =
    "Beantworte ausschließlich die gestellte Frage in höchstens drei klaren Sätzen."

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealHomeScreen(
    userConfiguration: UserConfiguration,
    meals: List<MealEntry>,
    isAnalyzing: Boolean,
    errorMessage: String?,
    onCaptureMeal: (Bitmap) -> Unit,
    onAddManualMeal: (String, Double, Double, Double) -> Unit,
    onUpdateMeal: (MealEntry) -> Unit,
    onDeleteMeal: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableStateOf(MealHomeTab.Capture) }
    var isChatCenterOpen by rememberSaveable { mutableStateOf(false) }
    var activeConversationId by rememberSaveable { mutableStateOf<String?>(null) }
    val conversations = remember { mutableStateListOf<ChatConversation>() }
    var conversationCounter by rememberSaveable { mutableStateOf(0) }
    val profileSummary = remember(userConfiguration, meals) {
        buildProfileSummary(userConfiguration, meals)
    }

    LaunchedEffect(conversations.size) {
        if (conversations.isNotEmpty() && activeConversationId == null) {
            activeConversationId = conversations.first().id
        }
    }

    fun createConversation(): ChatConversation {
        val carryOverSummary = buildPreviousConversationSummary(conversations)
        val initialMessages = mutableStateListOf(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                sender = ChatSender.Assistant,
                content = "Hier ist eine aktuelle Zusammenfassung deiner Daten:\n\n$profileSummary",
                contextType = ChatContextType.Intro
            )
        )
        carryOverSummary?.let { summary ->
            initialMessages += ChatMessage(
                id = UUID.randomUUID().toString(),
                sender = ChatSender.Assistant,
                content = summary,
                contextType = ChatContextType.CarryOver
            )
        }
        val conversation = ChatConversation(
            id = UUID.randomUUID().toString(),
            title = "Chat ${conversationCounter + 1}",
            messages = initialMessages
        )
        conversationCounter += 1
        conversations.add(0, conversation)
        activeConversationId = conversation.id
        return conversation
    }

    fun sendChatMessage(conversationId: String, message: String) {
        val trimmed = message.trim()
        if (trimmed.isEmpty()) return
        val conversation = conversations.firstOrNull { it.id == conversationId } ?: return
        conversation.messages += ChatMessage(
            id = UUID.randomUUID().toString(),
            sender = ChatSender.User,
            content = trimmed
        )
        conversation.messages.removeAll { it.contextType == ChatContextType.History }
        val plainMessages = conversation.messages.filter { it.contextType == null }
        val contextPayload = buildConversationContext(profileSummary, plainMessages)
        conversation.messages += ChatMessage(
            id = UUID.randomUUID().toString(),
            sender = ChatSender.Assistant,
            content = contextPayload,
            contextType = ChatContextType.History
        )
        val assistantReply = generateAssistantReply(
            userMessage = trimmed,
            meals = meals
        )
        conversation.messages += ChatMessage(
            id = UUID.randomUUID().toString(),
            sender = ChatSender.Assistant,
            content = assistantReply
        )
    }

    fun openChatCenter() {
        if (conversations.isEmpty()) {
            createConversation()
        }
        isChatCenterOpen = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FriendlyGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        val name = userConfiguration.name.ifBlank { "Freund" }
                        Text(
                            "Hallo $name",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color(0xFF2B2B2B)
                    )
                )
            },
            bottomBar = {
                NavigationBar(containerColor = Color.White.copy(alpha = 0.85f)) {
                    NavigationBarItem(
                        selected = selectedTab == MealHomeTab.Capture,
                        onClick = { selectedTab = MealHomeTab.Capture },
                        icon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                        label = { Text("Scanner") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == MealHomeTab.History,
                        onClick = { selectedTab = MealHomeTab.History },
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                        label = { Text("Mahlzeiten") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == MealHomeTab.Settings,
                        onClick = { selectedTab = MealHomeTab.Settings },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("Profil") }
                    )
                }
            }
        ) { padding ->
            val contentModifier = Modifier
                .padding(padding)
                .fillMaxSize()

            when (selectedTab) {
                MealHomeTab.Capture -> MealCaptureScreen(
                    meals = meals,
                    isAnalyzing = isAnalyzing,
                    errorMessage = errorMessage,
                    onCaptureMeal = onCaptureMeal,
                    onAddManualMeal = onAddManualMeal,
                    onDeleteMeal = onDeleteMeal,
                    modifier = contentModifier
                )

                MealHomeTab.History -> MealHistoryScreen(
                    meals = meals,
                    onEditMeal = onUpdateMeal,
                    onDeleteMeal = onDeleteMeal,
                    modifier = contentModifier
                )

                MealHomeTab.Settings -> MealSettingsScreen(
                    configuration = userConfiguration,
                    modifier = contentModifier
                )
            }
        }

        ChatLauncherButton(
            onClick = { openChatCenter() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 96.dp)
        )
    }

    if (isChatCenterOpen) {
        val effectiveActiveId = activeConversationId ?: conversations.firstOrNull()?.id
        ChatCenterDialog(
            conversations = conversations,
            activeConversationId = effectiveActiveId,
            onDismiss = { isChatCenterOpen = false },
            onNewChat = {
                createConversation()
            },
            onSelectConversation = { activeConversationId = it },
            onSendMessage = { id, text -> sendChatMessage(id, text) }
        )
    }
}

@Composable
private fun ChatLauncherButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = Color(0xFF7048E8),
        contentColor = Color.White
    ) {
        Icon(Icons.Default.Chat, contentDescription = "Chat öffnen")
    }
}

@Composable
private fun ChatCenterDialog(
    conversations: SnapshotStateList<ChatConversation>,
    activeConversationId: String?,
    onDismiss: () -> Unit,
    onNewChat: () -> Unit,
    onSelectConversation: (String) -> Unit,
    onSendMessage: (String, String) -> Unit
) {
    var isFocusMode by rememberSaveable { mutableStateOf(false) }
    val selectedConversation = conversations.firstOrNull { it.id == activeConversationId }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x66000000))
        ) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.9f),
                shape = RoundedCornerShape(36.dp),
                tonalElevation = 10.dp,
                color = Color.White.copy(alpha = 0.97f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ChatCenterGradient)
                        .padding(16.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(28.dp),
                        color = Color.White.copy(alpha = 0.92f)
                    ) {
                        if (isFocusMode && selectedConversation != null) {
                            FocusedConversation(
                                conversation = selectedConversation,
                                onSendMessage = { text -> onSendMessage(selectedConversation.id, text) },
                                onBackToOverview = { isFocusMode = false },
                                onNewChat = {
                                    onNewChat()
                                    isFocusMode = true
                                },
                                onDismiss = onDismiss
                            )
                        } else {
                            ChatOverview(
                                conversations = conversations,
                                activeConversationId = activeConversationId,
                                onDismiss = onDismiss,
                                onNewChat = {
                                    onNewChat()
                                    isFocusMode = true
                                },
                                onSelectConversation = {
                                    onSelectConversation(it)
                                    isFocusMode = true
                                },
                                onSendMessage = onSendMessage,
                                selectedConversation = selectedConversation
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatOverview(
    conversations: SnapshotStateList<ChatConversation>,
    activeConversationId: String?,
    onDismiss: () -> Unit,
    onNewChat: () -> Unit,
    onSelectConversation: (String) -> Unit,
    onSendMessage: (String, String) -> Unit,
    selectedConversation: ChatConversation?
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Begleit-Chat",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF211946)
                )
                Text(
                    text = "Sieh dir vergangene Unterhaltungen an oder beginne eine neue Frage.",
                    color = Color(0xFF6B6B7A),
                    fontSize = 13.sp
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onNewChat,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF7048E8),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Neuer Chat")
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Schließen")
                }
            }
        }

        Divider(color = Color(0xFFE4DAFF))

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(min = 300.dp, max = 340.dp)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(28.dp),
                tonalElevation = 2.dp,
                color = Color.White.copy(alpha = 0.95f)
            ) {
                ConversationList(
                    conversations = conversations,
                    activeConversationId = activeConversationId,
                    onSelectConversation = onSelectConversation,
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (selectedConversation != null) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(32.dp),
                    tonalElevation = 4.dp,
                    color = Color.White
                ) {
                    ConversationDetail(
                        conversation = selectedConversation,
                        onSendMessage = { text -> onSendMessage(selectedConversation.id, text) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(32.dp),
                    color = Color(0xFFEDE9FF)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Chat, contentDescription = null, tint = Color(0xFF7048E8))
                            Text(
                                "Starte einen neuen Chat, um loszulegen.",
                                color = Color(0xFF6B6B7A),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FocusedConversation(
    conversation: ChatConversation,
    onSendMessage: (String) -> Unit,
    onBackToOverview: () -> Unit,
    onNewChat: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackToOverview) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Übersicht anzeigen")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = conversation.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Color(0xFF211946)
                    )
                    val lastUserMessage = conversation.messages.lastOrNull { it.sender == ChatSender.User }
                    lastUserMessage?.let {
                        Text(
                            text = "Letzte Frage: ${it.content}",
                            fontSize = 13.sp,
                            color = Color(0xFF6B6B7A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onNewChat) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Neuer Chat")
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Schließen")
                }
            }
        }

        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            shape = RoundedCornerShape(36.dp),
            tonalElevation = 4.dp,
            color = Color.White
        ) {
            ConversationDetail(
                conversation = conversation,
                onSendMessage = onSendMessage,
                modifier = Modifier.fillMaxSize(),
                showHeader = false
            )
        }
    }
}

@Composable
private fun ConversationList(
    conversations: SnapshotStateList<ChatConversation>,
    activeConversationId: String?,
    onSelectConversation: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Top
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(conversations, key = { it.id }) { conversation ->
                val isActive = conversation.id == activeConversationId
                val background = if (isActive) Color(0xFFF4F0FF) else Color.Transparent
                val borderColor = if (isActive) Color(0xFF7048E8).copy(alpha = 0.4f) else Color.Transparent
                Column(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(background)
                        .border(
                            width = 1.dp,
                            color = borderColor,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onSelectConversation(conversation.id) }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = conversation.title,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2B2B2B)
                    )
                    val lastMessage = conversation.messages.lastOrNull()?.content ?: "Noch keine Nachrichten"
                    Text(
                        text = lastMessage,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationDetail(
    conversation: ChatConversation,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    headerContent: (@Composable () -> Unit)? = null,
    showHeader: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(Color.White)
    ) {
        val listState = rememberLazyListState()
        LaunchedEffect(conversation.id, conversation.messages.size) {
            if (conversation.messages.isNotEmpty()) {
                listState.animateScrollToItem(conversation.messages.lastIndex)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            if (showHeader) {
                headerContent?.invoke() ?: DefaultConversationHeader(conversation)
                Divider(color = Color(0xFFEEE5FF))
            } else {
                Spacer(modifier = Modifier.height(12.dp))
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(conversation.messages, key = { it.id }) { message ->
                    ChatBubble(message = message)
                }
            }
        }

        var input by rememberSaveable(conversation.id) { mutableStateOf("") }
        val focusManager = LocalFocusManager.current
        val questionSuggestions = remember(conversation.id) {
            listOf(
                "Wie liege ich heute im Vergleich zu meinen Makrozielen?",
                "Hast du eine Idee für eine ausgewogene nächste Mahlzeit?",
                "Welche Mahlzeit hatte die meisten Proteine?"
            )
        }

        if (questionSuggestions.isNotEmpty()) {
            SuggestionRow(
                suggestions = questionSuggestions,
                onSuggestionSelected = { input = it }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Nachricht oder Frage eingeben…") },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(onSend = {
                    if (input.isNotBlank()) {
                        onSendMessage(input)
                        input = ""
                        focusManager.clearFocus()
                    }
                }),
                minLines = 1,
                maxLines = 4
            )
            Button(
                onClick = {
                    if (input.isNotBlank()) {
                        onSendMessage(input)
                        input = ""
                        focusManager.clearFocus()
                    }
                },
                enabled = input.isNotBlank()
            ) {
                Text("Senden")
            }
        }
    }
}

@Composable
private fun DefaultConversationHeader(conversation: ChatConversation) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = conversation.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = Color(0xFF2B2B2B)
            )
            val lastUserMessage = conversation.messages.lastOrNull { it.sender == ChatSender.User }
            lastUserMessage?.let {
                Text(
                    text = "Letzte Frage: ${it.content}",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuggestionRow(
    suggestions: List<String>,
    onSuggestionSelected: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        suggestions.forEach { suggestion ->
            AssistChip(
                onClick = { onSuggestionSelected(suggestion) },
                label = { Text(suggestion) }
            )
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.sender == ChatSender.User
    val backgroundColor = when {
        message.contextType == ChatContextType.History -> Color(0xFFEAFBF1)
        message.contextType == ChatContextType.CarryOver -> Color(0xFFE3F0FF)
        message.contextType == ChatContextType.Intro -> Color(0xFFF0ECFF)
        isUser -> Color(0xFF7048E8)
        else -> Color(0xFFF6F6F6)
    }
    val contentColor = if (isUser) Color.White else Color(0xFF2B2B2B)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = backgroundColor
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                val header = when (message.contextType) {
                    ChatContextType.History -> "Übermittelte Infos"
                    ChatContextType.CarryOver -> "Überblick aus früheren Chats"
                    else -> null
                }
                if (header != null) {
                    Text(
                        text = header,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2B2B2B),
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    text = message.content,
                    color = contentColor,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

private fun buildProfileSummary(
    configuration: UserConfiguration,
    meals: List<MealEntry>
): String {
    val now = LocalDate.now()
    val ageYears = configuration.birthday?.let { Period.between(it, now).years }
    val todaysMeals = meals.filter {
        it.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate() == now
    }
    val totals = todaysMeals.fold(NutrientTotals()) { acc, meal -> acc + meal }
    val lastMeal = meals.maxByOrNull { it.recordedAt }
    val builder = StringBuilder()
    builder.appendLine("Name: ${configuration.name.ifBlank { "Nicht angegeben" }}")
    builder.appendLine("Geschlecht: ${if (configuration.gender == Gender.Girl) "Mädchen" else "Junge"}")
    builder.appendLine(
        "Alter: " + (ageYears?.let { "$it Jahre" } ?: "Nicht angegeben")
    )
    builder.appendLine(
        "Diagnose: ${configuration.diagnosis.ifBlank { "Nicht angegeben" }}"
    )
    builder.appendLine("Gespeicherte Mahlzeiten: ${meals.size}")
    if (todaysMeals.isNotEmpty()) {
        builder.appendLine(
            "Heutige Makros – Fett: ${String.format(Locale.getDefault(), "%.1f g", totals.fatGrams)}, " +
                "Kohlenhydrate: ${String.format(Locale.getDefault(), "%.1f g", totals.carbGrams)}, " +
                "Protein: ${String.format(Locale.getDefault(), "%.1f g", totals.proteinGrams)}"
        )
    } else {
        builder.appendLine("Heute wurden noch keine Mahlzeiten erfasst.")
    }
    lastMeal?.let { meal ->
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.getDefault())
        builder.appendLine(
            "Letzte Mahlzeit: ${meal.description} am ${meal.recordedAt.atZone(ZoneId.systemDefault()).format(formatter)}"
        )
    }
    return builder.toString().trimEnd()
}

private fun buildConversationContext(
    profileSummary: String,
    messages: List<ChatMessage>
): String {
    val history = messages
        .filter { it.contextType == null }
        .joinToString(separator = "\n\n") { message ->
            val speaker = if (message.sender == ChatSender.User) "Du" else "Begleiter"
            "$speaker: ${message.content}"
        }
    val historySection = if (history.isBlank()) {
        "Bisher gibt es noch keinen Gesprächsverlauf."
    } else {
        history
    }
    return buildString {
        appendLine("System-Anweisung: $CHAT_SYSTEM_PROMPT")
        appendLine()
        appendLine("Profil-Überblick:")
        appendLine(profileSummary)
        appendLine()
        appendLine("Bisheriger Chat-Verlauf:")
        append(historySection)
    }.trimEnd()
}

private fun buildPreviousConversationSummary(
    conversations: List<ChatConversation>
): String? {
    val previousMessages = conversations
        .flatMap { conversation ->
            conversation.messages.filter { it.contextType == null }
        }
        .takeLast(8)
    if (previousMessages.isEmpty()) return null
    val summary = previousMessages.joinToString(separator = "\n") { message ->
        val prefix = if (message.sender == ChatSender.User) "Du" else "Begleiter"
        "• $prefix: ${message.content}"
    }
    return buildString {
        appendLine("Ich habe die wichtigsten Punkte aus früheren Chats übernommen:")
        append(summary)
    }.trimEnd()
}

private fun generateAssistantReply(
    userMessage: String,
    meals: List<MealEntry>
): String {
    val locale = Locale.getDefault()
    val normalizedMessage = userMessage.lowercase(locale)
    val today = LocalDate.now()
    val todaysMeals = meals.filter {
        it.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate() == today
    }
    val totals = todaysMeals.fold(NutrientTotals()) { acc, meal -> acc + meal }
    val macroGoals = mapOf(
        "kohlenhydrate" to 200.0,
        "protein" to 60.0,
        "fett" to 70.0
    )
    val consumedByMacro = mapOf(
        "kohlenhydrate" to totals.carbGrams,
        "protein" to totals.proteinGrams,
        "fett" to totals.fatGrams
    )
    val deficits = macroGoals.mapValues { (key, goal) -> goal - consumedByMacro.getValue(key) }

    fun macroLabel(key: String): String = when (key) {
        "kohlenhydrate" -> "Kohlenhydrate"
        "protein" -> "Protein"
        else -> "Fett"
    }

    fun formatGrams(value: Double): String = String.format(locale, "%.1f g", value)

    fun progressChunk(key: String): String {
        val goal = macroGoals.getValue(key)
        val consumed = consumedByMacro.getValue(key)
        if (goal <= 0.0) return "${macroLabel(key)}: ${formatGrams(consumed)}"
        val percent = ((consumed / goal) * 100).roundToInt()
        val diff = goal - consumed
        val remark = when {
            diff > 5.0 -> "es fehlen ${formatGrams(diff)}"
            diff < -5.0 -> "du liegst ${formatGrams(abs(diff))} über dem Ziel"
            else -> "du liegst im Zielbereich"
        }
        return "${macroLabel(key)}: ${formatGrams(consumed)} (${percent}% des Tagesziels) – $remark"
    }

    fun progressSentence(keys: List<String>): String =
        keys.joinToString(separator = "; ") { progressChunk(it) } + "."

    val macroKeywords = mapOf(
        "protein" to "protein",
        "eiweiß" to "protein",
        "kohlenhydrat" to "kohlenhydrate",
        "kohlenhydrate" to "kohlenhydrate",
        "carb" to "kohlenhydrate",
        "kh" to "kohlenhydrate",
        "fett" to "fett"
    )
    val requestedMacros = macroKeywords.entries
        .filter { normalizedMessage.contains(it.key) }
        .map { it.value }
        .distinct()

    val asksForSuggestion = listOf("idee", "vorschlag", "essen", "snack", "tipp", "empfehl").any {
        normalizedMessage.contains(it)
    }
    val asksForProgress = listOf("ziel", "fortschritt", "stand", "status", "wie liege").any {
        normalizedMessage.contains(it)
    }
    val asksForSummary = normalizedMessage.contains("zusammenfassung") ||
        normalizedMessage.contains("überblick") || normalizedMessage.contains("gesamt")
    val asksForTrend = normalizedMessage.contains("trend") || normalizedMessage.contains("entwicklung")
    val asksForLastMeal = normalizedMessage.contains("letzte") &&
        (normalizedMessage.contains("mahlzeit") || normalizedMessage.contains("gegessen") ||
            normalizedMessage.contains("essen"))

    fun suggestionSentence(): String {
        val primaryNeed = deficits
            .filterValues { it > 5.0 }
            .maxByOrNull { it.value }
            ?.key
        val baseLine = when {
            todaysMeals.isEmpty() -> "Heute wurde noch keine Mahlzeit gespeichert."
            primaryNeed != null -> progressSentence(listOf(primaryNeed))
            else -> "Du liegst heute sehr nah an deinen Zielen."
        }
        val suggestion = when (primaryNeed) {
            "protein" -> "Ein Vollkornbrot mit Hüttenkäse und etwas Gemüse würde dir extra Protein liefern."
            "kohlenhydrate" -> "Haferflocken mit Joghurt und Beeren füllen die Kohlenhydratspeicher sanft auf."
            "fett" -> "Ein kleiner Avocado-Toast oder ein paar Nüsse bringen gesunde Fette dazu."
            else -> "Eine bunte Bowl mit Gemüse, Hülsenfrüchten und etwas Obst hält die Balance."
        }
        return listOf(baseLine, suggestion).joinToString(" ")
    }

    if (requestedMacros.isNotEmpty()) {
        if (todaysMeals.isEmpty()) {
            return "Heute wurde noch keine Mahlzeit gespeichert. Ergänze zuerst eine Mahlzeit, dann zeige ich dir die Werte."
        }
        val macrosToReport = requestedMacros.take(3)
        val progressAnswer = progressSentence(macrosToReport)
        if (macrosToReport.size == 1) {
            val key = macrosToReport.first()
            val highlightMeal = when (key) {
                "protein" -> meals.maxByOrNull { it.proteinGrams }
                "kohlenhydrate" -> meals.maxByOrNull { it.carbGrams }
                else -> meals.maxByOrNull { it.fatGrams }
            }
            val detail = highlightMeal?.let { meal ->
                val value = when (key) {
                    "protein" -> meal.proteinGrams
                    "kohlenhydrate" -> meal.carbGrams
                    else -> meal.fatGrams
                }
                "Am meisten ${macroLabel(key)} lieferte ${meal.description} mit ${formatGrams(value)}."
            }
            return listOfNotNull(progressAnswer, detail).joinToString(" ")
        }
        return progressAnswer
    }

    if (asksForSuggestion) {
        return suggestionSentence()
    }

    if (asksForProgress || asksForSummary || asksForTrend) {
        if (todaysMeals.isEmpty()) {
            return "Heute wurde noch keine Mahlzeit gespeichert. Sobald du eine ergänzt, berechne ich deinen Fortschritt."
        }
        val countSentence = "Heute hast du ${todaysMeals.size} Mahlzeiten gespeichert."
        val overviewSentence = progressSentence(listOf("kohlenhydrate", "protein", "fett"))
        val focusSentence = when {
            asksForTrend || asksForSummary -> deficits
                .maxByOrNull { abs(it.value) }
                ?.let { (key, value) ->
                    val direction = if (value > 5.0) "offen" else if (value < -5.0) "leicht über dem Ziel" else "im Zielbereich"
                    "Größtes Thema sind aktuell ${macroLabel(key)} – du bist dort $direction."
                }
            else -> null
        }
        return listOfNotNull(countSentence, overviewSentence, focusSentence).joinToString(" ")
    }

    if (asksForLastMeal) {
        val lastMeal = meals.maxByOrNull { it.recordedAt }
        return lastMeal?.let { meal ->
            val formattedTime = meal.recordedAt
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", locale))
            "Deine letzte Mahlzeit war ${meal.description} am $formattedTime. Sie brachte ${formatGrams(meal.carbGrams)} Kohlenhydrate, ${formatGrams(meal.proteinGrams)} Protein und ${formatGrams(meal.fatGrams)} Fett."
        } ?: "Es wurden bisher noch keine Mahlzeiten gespeichert."
    }

    if (todaysMeals.isNotEmpty()) {
        val defaultOverview = progressSentence(listOf("kohlenhydrate", "protein", "fett"))
        return "Ich habe deine Frage verstanden, konnte sie aber keinem Bereich zuordnen. $defaultOverview"
    }

    return "Ich habe deine Frage verstanden, aber heute liegt noch keine Mahlzeit vor. Frag gern nach Ideen oder füge zuerst eine Mahlzeit hinzu."
}

private data class ChatConversation(
    val id: String,
    val title: String,
    val messages: SnapshotStateList<ChatMessage>
)

private data class ChatMessage(
    val id: String,
    val sender: ChatSender,
    val content: String,
    val contextType: ChatContextType? = null
)

private enum class ChatSender { User, Assistant }

private enum class ChatContextType { Intro, History, CarryOver }

@Composable
private fun MealCaptureScreen(
    meals: List<MealEntry>,
    isAnalyzing: Boolean,
    errorMessage: String?,
    onCaptureMeal: (Bitmap) -> Unit,
    onAddManualMeal: (String, Double, Double, Double) -> Unit,
    onDeleteMeal: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            onCaptureMeal(bitmap)
        }
    }

    val today = LocalDate.now()
    val todayMeals = remember(meals, today) {
        meals.filter { entry ->
            entry.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate() == today
        }
    }

    var showManualDialog by rememberSaveable { mutableStateOf(false) }
    var mealToDelete by remember { mutableStateOf<MealEntry?>(null) }

    val totals = remember(todayMeals) {
        todayMeals.fold(NutrientTotals()) { acc, meal ->
            acc + meal
        }
    }

    val nutrientGoals = listOf(
        NutrientGoal(label = "Kohlenhydrate", consumed = totals.carbGrams, goal = 200.0, color = Mint),
        NutrientGoal(label = "Protein", consumed = totals.proteinGrams, goal = 60.0, color = Lilac),
        NutrientGoal(label = "Fett", consumed = totals.fatGrams, goal = 70.0, color = Peach)
    )

    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(12.dp))
        KidFriendlyCard {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Deine Tagesziele",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2B2B2B)
                )
                Text(
                    text = "So viel hast du heute schon geschafft!",
                    color = Color.Gray
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    maxItemsInEachRow = 3
                ) {
                    nutrientGoals.forEach { goal ->
                        NutrientGoalRing(goal)
                    }
                }
            }
        }
        errorMessage?.let {
            KidFriendlyCard(containerColor = Color(0xFFFFE0E0)) {
                Text(
                    text = it,
                    color = Color(0xFF8A0000),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        KidFriendlyCard(containerColor = Color.White.copy(alpha = 0.98f)) {
            Column(
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Lilac.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color(0xFF3F2A56)
                    )
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Halte deine nächste Mahlzeit fest",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2B2B2B),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Nimm ein Foto auf oder füge sie manuell hinzu.",
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                }
                Button(
                    onClick = { launcher.launch(null) },
                    enabled = !isAnalyzing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isAnalyzing) "Wird analysiert…" else "Foto aufnehmen")
                }
                OutlinedButton(
                    onClick = { showManualDialog = true },
                    enabled = !isAnalyzing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Manuell eintragen")
                }
                if (isAnalyzing) {
                    Text("Bitte warten – ich analysiere das Bild…", color = Color.Gray)
                }
            }
        }
        if (todayMeals.isEmpty()) {
            KidFriendlyCard {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Noch keine Mahlzeiten heute",
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2B2B2B)
                    )
                    Text(
                        text = "Füge eine Mahlzeit hinzu, um deinen Fortschritt zu sehen.",
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Heutige Mahlzeiten",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF2B2B2B)
                )
                todayMeals.sortedByDescending { it.recordedAt }.forEach { meal ->
                    MealHistoryCard(
                        meal = meal,
                        onLongPress = { mealToDelete = it }
                    )
                }
            }
        }
        Spacer(Modifier.height(96.dp))
    }

    if (showManualDialog) {
        ManualMealDialog(
            onDismiss = { showManualDialog = false },
            onSaveMeal = { description, fat, carb, protein ->
                onAddManualMeal(description, fat, carb, protein)
            }
        )
    }

    mealToDelete?.let { meal ->
        ConfirmDeleteMealDialog(
            meal = meal,
            onDismiss = { mealToDelete = null },
            onConfirm = {
                onDeleteMeal(meal.id)
                mealToDelete = null
            }
        )
    }
}

@Composable
private fun ManualMealDialog(
    onDismiss: () -> Unit,
    onSaveMeal: (String, Double, Double, Double) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var fatInput by remember { mutableStateOf("") }
    var carbInput by remember { mutableStateOf("") }
    var proteinInput by remember { mutableStateOf("") }
    var showDescriptionError by remember { mutableStateOf(false) }
    var showNumberError by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    fun attemptSave() {
        val trimmedDescription = description.trim()
        val fatValue = parseNumber(fatInput)
        val carbValue = parseNumber(carbInput)
        val proteinValue = parseNumber(proteinInput)
        val descriptionInvalid = trimmedDescription.isEmpty()
        val numbersInvalid = fatValue == null || carbValue == null || proteinValue == null
        showDescriptionError = descriptionInvalid
        showNumberError = numbersInvalid
        if (!descriptionInvalid && !numbersInvalid) {
            focusManager.clearFocus()
            onSaveMeal(trimmedDescription, fatValue, carbValue, proteinValue)
            description = ""
            fatInput = ""
            carbInput = ""
            proteinInput = ""
            showDescriptionError = false
            showNumberError = false
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mahlzeit manuell hinzufügen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Beschreibe dein Essen und gib die Nährwerte in Gramm an.",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        if (showDescriptionError && it.isNotBlank()) {
                            showDescriptionError = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Beschreibung") },
                    placeholder = { Text("z. B. Vollkornbrot mit Käse und Tomate") },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    )
                )
                if (showDescriptionError) {
                    Text(
                        text = "Bitte gib eine Beschreibung ein.",
                        color = Color(0xFF8A0000),
                        fontSize = 12.sp
                    )
                }
                NutrientInputRow(
                    fatInput = fatInput,
                    carbInput = carbInput,
                    proteinInput = proteinInput,
                    onFatChange = {
                        fatInput = it
                        if (showNumberError) showNumberError = false
                    },
                    onCarbChange = {
                        carbInput = it
                        if (showNumberError) showNumberError = false
                    },
                    onProteinChange = {
                        proteinInput = it
                        if (showNumberError) showNumberError = false
                    },
                    onDone = { attemptSave() }
                )
                if (showNumberError) {
                    Text(
                        text = "Bitte gib gültige Zahlen ein (z. B. 12,5).",
                        color = Color(0xFF8A0000),
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = "Tipp: Lass ein Feld leer, wenn du den Wert nicht kennst.",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            Button(onClick = { attemptSave() }) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}

private data class NutrientTotals(
    val fatGrams: Double = 0.0,
    val carbGrams: Double = 0.0,
    val proteinGrams: Double = 0.0
) {
    operator fun plus(meal: MealEntry): NutrientTotals = NutrientTotals(
        fatGrams = fatGrams + meal.fatGrams,
        carbGrams = carbGrams + meal.carbGrams,
        proteinGrams = proteinGrams + meal.proteinGrams
    )
}

private data class NutrientGoal(
    val label: String,
    val consumed: Double,
    val goal: Double,
    val color: Color
)

@Composable
private fun NutrientGoalRing(goal: NutrientGoal) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.width(108.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
            Canvas(modifier = Modifier.size(100.dp)) {
                val stroke = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                drawArc(
                    color = goal.color.copy(alpha = 0.2f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = stroke
                )
                val progress = (goal.consumed / goal.goal).coerceIn(0.0, 1.0)
                drawArc(
                    color = goal.color,
                    startAngle = -90f,
                    sweepAngle = (360 * progress).toFloat(),
                    useCenter = false,
                    style = stroke
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format(Locale.getDefault(), "%.0f g", goal.consumed),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2B2B2B)
                )
                Text(goal.label, color = Color.Gray, fontSize = 12.sp)
            }
        }
        Text(
            text = "Ziel: ${String.format(Locale.getDefault(), "%.0f g", goal.goal)}",
            color = Color(0xFF2B2B2B),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun NutrientChip(label: String, value: Double, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(color.copy(alpha = 0.35f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.8f))
        )
        Text(
            text = "$label: ${String.format(Locale.getDefault(), "%.1f g", value)}",
            fontWeight = FontWeight.Medium,
            color = Color(0xFF2B2B2B)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealHistoryScreen(
    meals: List<MealEntry>,
    onEditMeal: (MealEntry) -> Unit,
    onDeleteMeal: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var mealToEdit by remember { mutableStateOf<MealEntry?>(null) }
    var mealToDelete by remember { mutableStateOf<MealEntry?>(null) }

    val filteredMeals = remember(meals, selectedDate) {
        val date = selectedDate
        val zone = ZoneId.systemDefault()
        val base = if (date == null) {
            meals
        } else {
            meals.filter { entry ->
                entry.recordedAt.atZone(zone).toLocalDate() == date
            }
        }
        base.sortedByDescending { it.recordedAt }
    }

    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxSize()
    ) {
        Spacer(Modifier.height(12.dp))
        Text("Deine gespeicherten Mahlzeiten", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AssistChip(
                onClick = { showDatePicker = true },
                label = {
                    Text(
                        selectedDate?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                            ?: "Datum auswählen"
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.DateRange, contentDescription = null)
                },
                border = null
            )
            if (selectedDate != null) {
                TextButton(onClick = { selectedDate = null }) {
                    Text("Zurücksetzen")
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (filteredMeals.isEmpty()) {
            KidFriendlyCard {
                Text(
                    if (selectedDate == null) {
                        "Noch keine Mahlzeiten gespeichert. Starte mit einem Foto!"
                    } else {
                        "Keine Mahlzeiten für dieses Datum gefunden."
                    },
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredMeals, key = { it.id }) { meal ->
                    MealHistoryCard(
                        meal = meal,
                        onEditClick = { mealToEdit = it },
                        onDeleteClick = { mealToDelete = it },
                        onLongPress = { mealToDelete = it }
                    )
                }
                item { Spacer(Modifier.height(96.dp)) }
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = selectedDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedDate = datePickerState.selectedDateMillis?.let { millis ->
                            Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Auswählen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Abbrechen")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    mealToEdit?.let { meal ->
        EditMealDialog(
            meal = meal,
            onDismiss = { mealToEdit = null },
            onSave = { updated ->
                onEditMeal(updated)
                mealToEdit = null
            }
        )
    }

    mealToDelete?.let { meal ->
        ConfirmDeleteMealDialog(
            meal = meal,
            onDismiss = { mealToDelete = null },
            onConfirm = {
                onDeleteMeal(meal.id)
                mealToDelete = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MealHistoryCard(
    meal: MealEntry,
    onEditClick: ((MealEntry) -> Unit)? = null,
    onDeleteClick: ((MealEntry) -> Unit)? = null,
    onLongPress: ((MealEntry) -> Unit)? = null
) {
    val cardModifier = onLongPress?.let { handler ->
        Modifier.combinedClickable(
            onClick = { onEditClick?.invoke(meal) },
            onLongClick = { handler(meal) }
        )
    } ?: Modifier

    KidFriendlyCard(modifier = cardModifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(meal.description, fontWeight = FontWeight.SemiBold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NutrientChip(label = "Fett", value = meal.fatGrams, color = Peach)
                NutrientChip(label = "Kohlenhydrate", value = meal.carbGrams, color = Mint)
                NutrientChip(label = "Protein", value = meal.proteinGrams, color = Lilac)
            }
            Text(meal.formattedTimestamp(), color = Color.Gray, fontSize = 12.sp)
            if (onEditClick != null || onDeleteClick != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    onEditClick?.let { handler ->
                        TextButton(onClick = { handler(meal) }) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Bearbeiten")
                        }
                    }
                    onDeleteClick?.let { handler ->
                        TextButton(onClick = { handler(meal) }) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Löschen")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditMealDialog(
    meal: MealEntry,
    onDismiss: () -> Unit,
    onSave: (MealEntry) -> Unit
) {
    var description by remember(meal) { mutableStateOf(meal.description) }
    var fatInput by remember(meal) { mutableStateOf(meal.fatGrams.toEditableString()) }
    var carbInput by remember(meal) { mutableStateOf(meal.carbGrams.toEditableString()) }
    var proteinInput by remember(meal) { mutableStateOf(meal.proteinGrams.toEditableString()) }
    var showDescriptionError by remember { mutableStateOf(false) }
    var showNumberError by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    fun attemptSave() {
        val trimmedDescription = description.trim()
        val fatValue = parseNumber(fatInput)
        val carbValue = parseNumber(carbInput)
        val proteinValue = parseNumber(proteinInput)
        val descriptionInvalid = trimmedDescription.isEmpty()
        val numbersInvalid = fatValue == null || carbValue == null || proteinValue == null
        showDescriptionError = descriptionInvalid
        showNumberError = numbersInvalid
        if (!descriptionInvalid && !numbersInvalid) {
            focusManager.clearFocus()
            onSave(
                meal.copy(
                    description = trimmedDescription,
                    fatGrams = fatValue,
                    carbGrams = carbValue,
                    proteinGrams = proteinValue
                )
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mahlzeit bearbeiten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        if (showDescriptionError && it.isNotBlank()) {
                            showDescriptionError = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Beschreibung") },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    )
                )
                if (showDescriptionError) {
                    Text(
                        text = "Bitte gib eine Beschreibung ein.",
                        color = Color(0xFF8A0000),
                        fontSize = 12.sp
                    )
                }
                NutrientInputRow(
                    fatInput = fatInput,
                    carbInput = carbInput,
                    proteinInput = proteinInput,
                    onFatChange = {
                        fatInput = it
                        if (showNumberError) showNumberError = false
                    },
                    onCarbChange = {
                        carbInput = it
                        if (showNumberError) showNumberError = false
                    },
                    onProteinChange = {
                        proteinInput = it
                        if (showNumberError) showNumberError = false
                    },
                    onDone = { attemptSave() }
                )
                if (showNumberError) {
                    Text(
                        text = "Bitte gib gültige Zahlen ein (z. B. 12,5).",
                        color = Color(0xFF8A0000),
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { attemptSave() }) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
private fun ConfirmDeleteMealDialog(
    meal: MealEntry,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mahlzeit löschen") },
        text = {
            Text(
                text = "Möchtest du \"${meal.description}\" wirklich löschen?",
                color = Color(0xFF2B2B2B)
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Löschen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
private fun NutrientInputRow(
    fatInput: String,
    carbInput: String,
    proteinInput: String,
    onFatChange: (String) -> Unit,
    onCarbChange: (String) -> Unit,
    onProteinChange: (String) -> Unit,
    onDone: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = fatInput,
            onValueChange = onFatChange,
            modifier = Modifier.weight(1f),
            label = { Text("Fett (g)") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Next) }
            ),
            singleLine = true
        )
        OutlinedTextField(
            value = carbInput,
            onValueChange = onCarbChange,
            modifier = Modifier.weight(1f),
            label = { Text("Kohlenhydrate (g)") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Next) }
            ),
            singleLine = true
        )
        OutlinedTextField(
            value = proteinInput,
            onValueChange = onProteinChange,
            modifier = Modifier.weight(1f),
            label = { Text("Protein (g)") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            singleLine = true
        )
    }
}

private fun parseNumber(value: String): Double? {
    val sanitized = value.trim()
    if (sanitized.isEmpty()) return 0.0
    return sanitized.replace(',', '.').toDoubleOrNull()
}

private fun Double.toEditableString(): String {
    if (this == 0.0) return ""
    val formatted = String.format(Locale.US, "%.2f", this)
    return formatted.trimEnd('0').trimEnd('.')
}

@Composable
private fun MealSettingsScreen(configuration: UserConfiguration, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Text("Dein Profil", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color(0xFF2B2B2B))
        KidFriendlyCard {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoRow(
                    icon = Icons.Default.Person,
                    label = "Name",
                    value = configuration.name.ifBlank { "Nicht angegeben" }
                )
                val age = configuration.birthday?.let { Period.between(it, LocalDate.now()).years }
                InfoRow(
                    icon = Icons.Default.Cake,
                    label = "Geburtstag",
                    value = configuration.birthday?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                        ?: "Nicht angegeben",
                    helper = age?.let { "$it Jahre" }
                )
                InfoRow(
                    icon = Icons.Default.Info,
                    label = "Geschlecht",
                    value = when (configuration.gender) {
                        Gender.Girl -> "Mädchen"
                        Gender.Boy -> "Junge"
                    }
                )
                InfoRow(
                    icon = Icons.Default.Info,
                    label = "Diagnose",
                    value = configuration.diagnosis.ifBlank { "Keine Angaben" }
                )
            }
        }
        Spacer(Modifier.height(96.dp))
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    helper: String? = null
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF2B2B2B))
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, fontWeight = FontWeight.SemiBold, color = Color(0xFF2B2B2B))
            Text(value, color = Color(0xFF3A3A3A))
            helper?.let {
                Text(it, color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun KidFriendlyCard(
    modifier: Modifier = Modifier,
    containerColor: Color = Color.White.copy(alpha = 0.95f),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}
