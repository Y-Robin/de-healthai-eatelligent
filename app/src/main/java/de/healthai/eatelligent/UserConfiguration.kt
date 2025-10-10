package de.healthai.eatelligent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.healthai.eatelligent.ui.theme.EatelligentTheme
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter

@Composable
fun UserConfigurationView() {
    Column {
        Text("")

    }
}

@Preview(showBackground = true)
@Composable
fun UserConfigurationPreview() {
    EatelligentTheme {
        UserConfigurationView()
    }
}


// --- Domain model ------------------------------------------------------------

enum class Gender { Girl, Boy }

data class UserConfiguration(
    val name: String = "",
    val birthday: LocalDate? = null,
    val gender: Gender = Gender.Girl,
    val diagnosis: String = ""
)

// --- Screen -----------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserConfigurationScreen(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onSave: ((UserConfiguration) -> Unit)? = null,
) {
    var value by remember { mutableStateOf(UserConfiguration()) }

    // Friendly, kid-focused palette (warm & approachable)
    val peach = Color(0xFFFFB38E)
    val sunshine = Color(0xFFFFD66B)
    val mint = Color(0xFFB7F0AD)
    val sky = Color(0xFFAEE7FF)
    val lilac = Color(0xFFCFB7FF)

    val bgGradient = Brush.verticalGradient(
        0f to sky,
        0.5f to mint,
        1f to peach
    )

    val scroll = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "Dein Profil",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        onBack?.let {
                            IconButton(onClick = it) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Zurück"
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                BottomAppBar(actions = {}, floatingActionButton = {
                    FloatingActionButton(
                        onClick = { onSave?.invoke(value) },
                        containerColor = sunshine,
                        contentColor = Color(0xFF4A3A00)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Speichern")
                    }
                })
            }
        ) { inner ->
            Box(
                modifier
                    .fillMaxSize()
                    .background(bgGradient)
                    .padding(inner)
            ) {
                // Decorative header bubble
                Box(
                    Modifier
                        .padding(top = 8.dp)
                        .align(Alignment.TopCenter)
                        .size(width = 220.dp, height = 60.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(lilac.copy(alpha = 0.3f))
                )

                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(scroll)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Lass uns dich kennenlernen!",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2B2B2B)
                    )

                    // Card container for inputs
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                        shape = RoundedCornerShape(28.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            val dateLabel =
                                value.birthday?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                                    ?: "Geburtsdatum"
                            val openDate = remember { mutableStateOf(false) }

                            // Name
                            OutlinedTextField(
                                value = value.name,
                                onValueChange = { value = value.copy(name = it) },
                                label = { Text("Name") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val age = remember(value.birthday) {
                                    value.birthday?.let {
                                        Period.between(
                                            it,
                                            LocalDate.now()
                                        ).years
                                    }
                                }

                                AssistChip(
                                    onClick = { openDate.value = true },
                                    label = { Text(dateLabel) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.DateRange,
                                            contentDescription = null
                                        )
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color(0x33000000))
                                )
                                AssistChip(
                                    onClick = { },
                                    label = { Text("Alter: ${age ?: 0} Jahre") },
                                    leadingIcon = {
                                        Box(
                                            Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(peach)
                                        )
                                    }
                                )
                            }
                            // Birthday
                            AnimatedVisibility(visible = openDate.value) {
                                BirthdayPicker(
                                    initial = value.birthday,
                                    onConfirm = {
                                        value = value.copy(birthday = it)
                                        openDate.value = false
                                    },
                                    onDismiss = { openDate.value = false }
                                )
                            }


                            // Gender
                            Text(
                                text = "Geschlecht",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                GenderChip(
                                    selected = value.gender == Gender.Girl,
                                    label = "Mädchen",
                                    icon = {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null
                                        )
                                    },
                                    color = Color(0xFFFF80AB)
                                ) { value = value.copy(gender = Gender.Girl) }
                                GenderChip(
                                    selected = value.gender == Gender.Boy,
                                    label = "Junge",
                                    icon = {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null
                                        )
                                    },
                                    color = Color(0xFF80D8FF)
                                ) { value = value.copy(gender = Gender.Boy) }
                            }

                            // Diagnosis
                            OutlinedTextField(
                                value = value.diagnosis,
                                onValueChange = { value = value.copy(diagnosis = it) },
                                label = { Text("Diagnose (Name) - Optional") },
                                placeholder = { Text("z. B. Zöliakie, Laktoseintoleranz…") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Save hint
                    Text(
                        text = "Tipp: Unten rechts kannst du speichern",
                        fontSize = 12.sp,
                        color = Color(0xFF4A4A4A)
                    )

                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }
}

// --- Gender Chip -------------------------------------------------------------

@Composable
private fun GenderChip(
    selected: Boolean,
    label: String,
    icon: @Composable (() -> Unit)? = null,
    color: Color,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = icon,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color.copy(alpha = 0.3f),
            selectedLabelColor = Color.Black,
            containerColor = Color.White.copy(alpha = 0.8f)
        ),
        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected)
    )
}

// --- Birthday Picker (Material3 DatePickerDialog) ----------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthdayPicker(
    initial: LocalDate?,
    onConfirm: (LocalDate?) -> Unit,
    onDismiss: () -> Unit
) {
    val today = remember { LocalDate.now() }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initial?.atStartOfDay(java.time.ZoneId.systemDefault())
            ?.toInstant()?.toEpochMilli()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = datePickerState.selectedDateMillis
                val date = millis?.let {
                    java.time.Instant.ofEpochMilli(it)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
                }
                onConfirm(date)
            }) { Text("Übernehmen") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    ) {
        DatePicker(
            state = datePickerState,
            showModeToggle = false,
        )
    }
}

// --- Preview -----------------------------------------------------------------

@Preview(showBackground = true, backgroundColor = 0xFFF3F3F3)
@Composable
private fun UserConfigurationScreen_Preview() {
    val (value, setValue) = remember { mutableStateOf(UserConfiguration()) }
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = Color(0xFF5E60CE),
            secondary = Color(0xFF80D8FF)
        )
    ) {
        UserConfigurationScreen(
            onBack = {},
            onSave = {}
        )
    }
}

// --- Notes -------------------------------------------------------------------
// • Compose Material3 DatePicker requires material3 >= 1.2.0 and opt-in ExperimentalMaterial3Api.
// • Hook this screen to your ViewModel: expose a State<UserConfiguration> and forward callbacks.
// • Colors chosen to be friendly for children while maintaining contrast (check WCAG in your theme).
