package de.healthai.eatelligent.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import de.healthai.eatelligent.UserConfigurationScreen
import de.healthai.eatelligent.ui.home.MealHomeScreen
import de.healthai.eatelligent.ui.home.MealViewModel

@Composable
fun EatelligentApp(viewModel: MealViewModel) {
    val userConfiguration by viewModel.userConfiguration.collectAsState()
    val meals by viewModel.meals.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val error by viewModel.error.collectAsState()

    Surface(modifier = Modifier.fillMaxSize()) {
        val currentConfiguration = userConfiguration
        if (currentConfiguration == null) {
            UserConfigurationScreen(
                modifier = Modifier.fillMaxSize(),
                onSave = {
                    viewModel.saveUserConfiguration(it)
                }
            )
        } else {
            MealHomeScreen(
                userConfiguration = currentConfiguration,
                meals = meals,
                isAnalyzing = isAnalyzing,
                errorMessage = error,
                onCaptureMeal = viewModel::analyzeMeal,
                onAddManualMeal = viewModel::addManualMeal
            )
        }
    }
}
