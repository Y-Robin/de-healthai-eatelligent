package de.healthai.eatelligent.ui.home

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.healthai.eatelligent.BuildConfig
import de.healthai.eatelligent.UserConfiguration
import de.healthai.eatelligent.data.MealAnalysis
import de.healthai.eatelligent.data.MealEntry
import de.healthai.eatelligent.data.MealRepository
import de.healthai.eatelligent.data.MealRepositoryImpl
import de.healthai.eatelligent.data.MealStorage
import de.healthai.eatelligent.data.OpenAiMealAnalyzer
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MealViewModel(private val analyzer: OpenAiMealAnalyzer, private val repository: MealRepository) :
    ViewModel() {

    private val _meals = MutableStateFlow<List<MealEntry>>(emptyList())
    val meals: StateFlow<List<MealEntry>> = _meals.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _latestResult = MutableStateFlow<MealEntry?>(null)
    val latestResult: StateFlow<MealEntry?> = _latestResult.asStateFlow()

    private val _userConfiguration = MutableStateFlow<UserConfiguration?>(null)
    val userConfiguration: StateFlow<UserConfiguration?> = _userConfiguration.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { repository.readMeals() }
                .onSuccess { _meals.value = it }
                .onFailure { _error.value = it.message }
        }
    }

    fun saveUserConfiguration(configuration: UserConfiguration) {
        _userConfiguration.value = configuration
    }

    fun analyzeMeal(bitmap: Bitmap) {
        if (_isAnalyzing.value) return
        _error.value = null
        _isAnalyzing.value = true
        viewModelScope.launch {
            val base64Image = encodeBitmap(bitmap)
            val analysis = runCatching { analyzer.analyze(base64Image) }
            analysis.onFailure { throwable ->
                _error.value = throwable.message
            }
            val result = analysis.getOrNull()
            if (result != null) {
                persistAnalysis(result)
            }
            _isAnalyzing.value = false
        }
    }

    private fun encodeBitmap(bitmap: Bitmap): String {
        val buffer = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, buffer)
        val bytes = buffer.toByteArray()
        return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
    }

    fun addManualMeal(description: String, fatGrams: Double, carbGrams: Double, proteinGrams: Double) {
        viewModelScope.launch {
            val newEntry = MealEntry(
                id = UUID.randomUUID().toString(),
                recordedAt = Instant.now(),
                description = description,
                fatGrams = fatGrams,
                carbGrams = carbGrams,
                proteinGrams = proteinGrams
            )
            persistEntry(newEntry)
        }
    }

    private suspend fun persistAnalysis(analysis: MealAnalysis) {
        val newEntry = MealEntry(
            id = UUID.randomUUID().toString(),
            recordedAt = Instant.now(),
            description = analysis.description,
            fatGrams = analysis.fatGrams,
            carbGrams = analysis.carbGrams,
            proteinGrams = analysis.proteinGrams
        )
        persistEntry(newEntry)
    }

    private suspend fun persistEntry(newEntry: MealEntry) {
        val updatedMeals = _meals.value + newEntry
        val writeResult = runCatching { repository.writeMeals(updatedMeals) }
        writeResult.onSuccess {
            _meals.value = updatedMeals
            _latestResult.value = newEntry
        }
        writeResult.onFailure { throwable ->
            _error.value = throwable.message
        }
    }

    companion object {
        fun provideFactory(context: android.content.Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val storage = MealStorage(context.applicationContext)
                    val repository = MealRepositoryImpl(storage)
                    val analyzer = OpenAiMealAnalyzer(BuildConfig.OPENAI_API_KEY)
                    @Suppress("UNCHECKED_CAST")
                    return MealViewModel(analyzer, repository) as T
                }
            }
    }
}
