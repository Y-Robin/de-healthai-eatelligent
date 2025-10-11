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
    val profileSummary = remember(userConfiguration, meals) {
        buildProfileSummary(userConfiguration, meals)
    }

    LaunchedEffect(conversations.size) {
        if (conversations.isNotEmpty() && activeConversationId == null) {
            activeConversationId = conversations.first().id
        }
    }

    fun createConversation(): ChatConversation {
        val conversation = ChatConversation(
            id = UUID.randomUUID().toString(),
            title = "Chat ${conversations.size + 1}",
            messages = mutableStateListOf(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    sender = ChatSender.Assistant,
                    content = "Hier ist eine aktuelle Zusammenfassung deiner Daten:\n\n$profileSummary",
                    contextType = ChatContextType.Intro
                )
            )
        )
        conversations += conversation
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
        val contextPayload = buildConversationContext(profileSummary, conversation.messages)
        conversation.messages += ChatMessage(
            id = UUID.randomUUID().toString(),
            sender = ChatSender.Assistant,
            content = contextPayload,
            contextType = ChatContextType.History
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
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Begleit-Chat",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2B2B2B)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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

                Divider()

                val selectedConversation = conversations.firstOrNull { it.id == activeConversationId }

                Row(modifier = Modifier.fillMaxSize()) {
                    ConversationList(
                        conversations = conversations,
                        activeConversationId = activeConversationId,
                        onSelectConversation = onSelectConversation,
                        modifier = Modifier
                            .fillMaxHeight()
                            .widthIn(min = 180.dp)
                            .background(Color(0xFFF5F1FF))
                    )

                    Divider(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp),
                        color = Color(0xFFDDD6FF)
                    )

                    if (selectedConversation != null) {
                        ConversationDetail(
                            conversation = selectedConversation,
                            onSendMessage = { text ->
                                onSendMessage(selectedConversation.id, text)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Starte einen neuen Chat, um loszulegen.",
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
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
                val background = if (isActive) Color.White else Color.Transparent
                val borderColor = if (isActive) Color(0xFF7048E8) else Color.Transparent
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(Color.White),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(conversation.messages, key = { it.id }) { message ->
                ChatBubble(message = message)
            }
        }

        var input by rememberSaveable(conversation.id) { mutableStateOf("") }
        val focusManager = LocalFocusManager.current
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Nachricht schreiben…") },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (input.isNotBlank()) {
                        onSendMessage(input)
                        input = ""
                        focusManager.clearFocus()
                    }
                })
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
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.sender == ChatSender.User
    val backgroundColor = when {
        message.contextType == ChatContextType.History -> Color(0xFFEAFBF1)
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
                    .widthIn(max = 320.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (message.contextType == ChatContextType.History) {
                    Text(
                        text = "Übermittelte Infos",
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2B2B2B),
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    text = message.content,
                    color = contentColor,
                    fontSize = 14.sp
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
        appendLine("Profil-Überblick:")
        appendLine(profileSummary)
        appendLine()
        appendLine("Bisheriger Chat-Verlauf:")
        append(historySection)
    }.trimEnd()
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

private enum class ChatContextType { Intro, History }

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
