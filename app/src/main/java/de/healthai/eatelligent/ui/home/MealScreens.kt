package de.healthai.eatelligent.ui.home

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import de.healthai.eatelligent.Gender
import de.healthai.eatelligent.UserConfiguration
import de.healthai.eatelligent.data.MealEntry
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    onAddManualMeal: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableStateOf(MealHomeTab.Capture) }

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
                    modifier = contentModifier
                )

                MealHomeTab.History -> MealHistoryScreen(
                    meals = meals,
                    modifier = contentModifier
                )

                MealHomeTab.Settings -> MealSettingsScreen(
                    configuration = userConfiguration,
                    modifier = contentModifier
                )
            }
        }
    }
}

@Composable
private fun MealCaptureScreen(
    meals: List<MealEntry>,
    isAnalyzing: Boolean,
    errorMessage: String?,
    onCaptureMeal: (Bitmap) -> Unit,
    onAddManualMeal: (String) -> Unit,
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

    var manualDescription by rememberSaveable { mutableStateOf("") }
    var showManualError by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

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
                        text = "Nimm ein Foto auf oder tippe deine Mahlzeit ein.",
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
                if (isAnalyzing) {
                    Text("Bitte warten – ich analysiere das Bild…", color = Color.Gray)
                }

                HorizontalDivider(color = Color(0x143F2A56))

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Oder beschreibe deine Mahlzeit",
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2B2B2B)
                    )
                    OutlinedTextField(
                        value = manualDescription,
                        onValueChange = {
                            manualDescription = it
                            if (showManualError && it.isNotBlank()) {
                                showManualError = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        },
                        placeholder = { Text("Beschreibe deine Mahlzeit…") },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (manualDescription.isBlank()) {
                                    showManualError = true
                                } else {
                                    onAddManualMeal(manualDescription.trim())
                                    manualDescription = ""
                                    showManualError = false
                                    focusManager.clearFocus()
                                }
                            }
                        ),
                        singleLine = false,
                        maxLines = 3
                    )
                    if (showManualError) {
                        Text(
                            text = "Bitte gib eine Beschreibung ein.",
                            color = Color(0xFF8A0000),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Button(
                        onClick = {
                            if (manualDescription.isBlank()) {
                                showManualError = true
                            } else {
                                onAddManualMeal(manualDescription.trim())
                                manualDescription = ""
                                showManualError = false
                                focusManager.clearFocus()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = manualDescription.isNotBlank()
                    ) {
                        Text("Mahlzeit speichern")
                    }
                    Text(
                        text = "Wir merken uns die Mahlzeit ohne Nährwertangaben.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
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
                    MealHistoryCard(meal)
                }
            }
        }
        Spacer(Modifier.height(96.dp))
    }
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
private fun MealHistoryScreen(meals: List<MealEntry>, modifier: Modifier = Modifier) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

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
                    MealHistoryCard(meal)
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
}

@Composable
private fun MealHistoryCard(meal: MealEntry) {
    KidFriendlyCard {
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
        }
    }
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
