package de.healthai.eatelligent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import de.healthai.eatelligent.ui.EatelligentApp
import de.healthai.eatelligent.ui.home.MealViewModel
import de.healthai.eatelligent.ui.theme.EatelligentTheme

class MainActivity : ComponentActivity() {

    private val mealViewModel: MealViewModel by viewModels {
        MealViewModel.provideFactory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EatelligentTheme {
                EatelligentApp(viewModel = mealViewModel)
            }
        }
    }
}