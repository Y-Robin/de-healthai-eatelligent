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
import de.healthai.eatelligent.data.UserConfigurationStorage
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MealViewModel(
    private val analyzer: OpenAiMealAnalyzer,
    private val repository: MealRepository,
    private val userConfigurationStorage: UserConfigurationStorage
) :
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

    private val _isLoadingUserConfiguration = MutableStateFlow(true)
    val isLoadingUserConfiguration: StateFlow<Boolean> =
        _isLoadingUserConfiguration.asStateFlow()

    private var lastSavedFingerprint: Pair<String, Long>? = null

    init {
        viewModelScope.launch {
            runCatching { repository.readMeals() }
                .onSuccess { _meals.value = it }
                .onFailure { _error.value = it.message }
        }
        viewModelScope.launch {
            runCatching { userConfigurationStorage.read() }
                .onSuccess { stored -> _userConfiguration.value = stored }
                .onFailure { throwable -> _error.value = throwable.message }
            _isLoadingUserConfiguration.value = false
        }
    }

    fun saveUserConfiguration(configuration: UserConfiguration) {
        _userConfiguration.value = configuration
        viewModelScope.launch {
            runCatching { userConfigurationStorage.write(configuration) }
                .onFailure { throwable ->
                    _error.value = throwable.message
                }
        }
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

    fun addManualMeal(
        description: String,
        fatGrams: Double,
        carbGrams: Double,
        proteinGrams: Double
    ) {
        val trimmed = description.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val manualEntry = MealEntry(
                id = UUID.randomUUID().toString(),
                recordedAt = Instant.now(),
                description = trimmed,
                fatGrams = fatGrams,
                carbGrams = carbGrams,
                proteinGrams = proteinGrams
            )
            persistMealEntry(manualEntry)
        }
    }

    fun updateMeal(entry: MealEntry) {
        viewModelScope.launch {
            val updatedMeals = _meals.value.map { existing ->
                if (existing.id == entry.id) entry else existing
            }
            writeMeals(updatedMeals, entry)
        }
    }

    fun deleteMeal(id: String) {
        viewModelScope.launch {
            val updatedMeals = _meals.value.filterNot { it.id == id }
            writeMeals(updatedMeals, latest = null, removedId = id)
        }
    }

    private fun encodeBitmap(bitmap: Bitmap): String {
        val buffer = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, buffer)
        val bytes = buffer.toByteArray()
        return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
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
        persistMealEntry(newEntry)
    }

    private suspend fun persistMealEntry(newEntry: MealEntry) {
        val fingerprint = mealFingerprint(newEntry)
        val now = System.currentTimeMillis()
        val lastSaved = lastSavedFingerprint
        if (lastSaved != null && lastSaved.first == fingerprint &&
            now - lastSaved.second < DUPLICATE_WINDOW_MS
        ) {
            _latestResult.value = newEntry
            return
        }

        val existingDuplicate = _meals.value.firstOrNull { existing ->
            mealFingerprint(existing) == fingerprint &&
                kotlin.math.abs(existing.recordedAt.toEpochMilli() - newEntry.recordedAt.toEpochMilli()) <=
                STORED_DUPLICATE_WINDOW_MS
        }

        if (existingDuplicate != null) {
            _latestResult.value = existingDuplicate
            return
        }
        val updatedMeals = _meals.value + newEntry
        writeMeals(updatedMeals, newEntry)
        lastSavedFingerprint = fingerprint to now
    }

    private fun mealFingerprint(entry: MealEntry): String = buildString {
        append(entry.description.trim())
        append('|')
        append(String.format(Locale.US, "%.2f", entry.fatGrams))
        append('|')
        append(String.format(Locale.US, "%.2f", entry.carbGrams))
        append('|')
        append(String.format(Locale.US, "%.2f", entry.proteinGrams))
    }

    companion object {
        private const val DUPLICATE_WINDOW_MS = 5_000L
        private const val STORED_DUPLICATE_WINDOW_MS = 30_000L

        fun provideFactory(context: android.content.Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val storage = MealStorage(context.applicationContext)
                    val repository = MealRepositoryImpl(storage)
                    val analyzer = OpenAiMealAnalyzer(BuildConfig.OPENAI_API_KEY)
                    val configurationStorage = UserConfigurationStorage(context.applicationContext)
                    @Suppress("UNCHECKED_CAST")
                    return MealViewModel(analyzer, repository, configurationStorage) as T
                }
            }
    }

    private fun writeMeals(
        updatedMeals: List<MealEntry>,
        latest: MealEntry?,
        removedId: String? = null
    ) {
        val writeResult = runCatching { repository.writeMeals(updatedMeals) }
        writeResult.onSuccess {
            _meals.value = updatedMeals
            if (removedId != null && _latestResult.value?.id == removedId) {
                _latestResult.value = null
            } else {
                _latestResult.value = latest
            }
        }
        writeResult.onFailure { throwable ->
            _error.value = throwable.message
        }
    }
}
