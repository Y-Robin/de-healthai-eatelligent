package de.healthai.eatelligent.ui.home

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.healthai.eatelligent.UserConfiguration
import de.healthai.eatelligent.data.MealEntry
import java.util.Locale

private enum class MealHomeTab { Capture, History }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealHomeScreen(
    userConfiguration: UserConfiguration,
    meals: List<MealEntry>,
    latestResult: MealEntry?,
    isAnalyzing: Boolean,
    errorMessage: String?,
    onCaptureMeal: (Bitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableStateOf(MealHomeTab.Capture) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = {
                val name = userConfiguration.name.ifBlank { "Freund" }
                Text("Hallo $name")
            })
        },
        bottomBar = {
            NavigationBar {
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
            }
        }
    ) { padding ->
        when (selectedTab) {
            MealHomeTab.Capture -> MealCaptureScreen(
                latestResult = latestResult,
                isAnalyzing = isAnalyzing,
                errorMessage = errorMessage,
                onCaptureMeal = onCaptureMeal,
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            )

            MealHomeTab.History -> MealHistoryScreen(
                meals = meals,
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            )
        }
    }
}

@Composable
private fun MealCaptureScreen(
    latestResult: MealEntry?,
    isAnalyzing: Boolean,
    errorMessage: String?,
    onCaptureMeal: (Bitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            onCaptureMeal(bitmap)
        }
    }

    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Fotografiere deine Mahlzeit und wir schätzen die Nährwerte!",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Button(
            onClick = { launcher.launch(null) },
            enabled = !isAnalyzing
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isAnalyzing) "Wird analysiert…" else "Foto aufnehmen")
        }
        if (isAnalyzing) {
            Text("Bitte warten – ich analysiere das Bild…", color = Color.Gray)
        }
        errorMessage?.let {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFFFFE0E0))
            ) {
                Text(
                    text = it,
                    color = Color(0xFF8A0000),
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        latestResult?.let { meal ->
            MealResultCard(meal)
        }
    }
}

@Composable
private fun MealResultCard(meal: MealEntry) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Letzte Analyse", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(meal.description)
            NutrientRow(label = "Fett", value = meal.fatGrams)
            NutrientRow(label = "Kohlenhydrate", value = meal.carbGrams)
            NutrientRow(label = "Protein", value = meal.proteinGrams)
            Text("Aufgenommen am ${meal.formattedTimestamp()}", color = Color.Gray)
        }
    }
}

@Composable
private fun NutrientRow(label: String, value: Double) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = FontWeight.Medium)
        Text(String.format(Locale.getDefault(), "%.1f g", value))
    }
}

@Composable
private fun MealHistoryScreen(meals: List<MealEntry>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(16.dp)) {
        Text("Deine gespeicherten Mahlzeiten", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))
        if (meals.isEmpty()) {
            Text("Noch keine Mahlzeiten gespeichert. Starte mit einem Foto!", color = Color.Gray)
        } else {
            HeaderRow()
            Divider()
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(meals.reversed(), key = { it.id }) { meal ->
                    MealHistoryRow(meal)
                    Divider()
                }
            }
        }
    }
}

@Composable
private fun HeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Datum", fontWeight = FontWeight.Bold)
        Text("Beschreibung", fontWeight = FontWeight.Bold)
        Text("Fett", fontWeight = FontWeight.Bold)
        Text("Kohlenhydrate", fontWeight = FontWeight.Bold)
        Text("Protein", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MealHistoryRow(meal: MealEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(meal.formattedTimestamp(), modifier = Modifier.weight(1.2f))
        Text(meal.description, modifier = Modifier.weight(1.6f))
        Text(String.format(Locale.getDefault(), "%.1f g", meal.fatGrams), modifier = Modifier.weight(0.8f))
        Text(String.format(Locale.getDefault(), "%.1f g", meal.carbGrams), modifier = Modifier.weight(0.9f))
        Text(String.format(Locale.getDefault(), "%.1f g", meal.proteinGrams), modifier = Modifier.weight(0.9f))
    }
}
