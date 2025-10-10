package de.healthai.eatelligent.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface MealRepository {
    suspend fun readMeals(): List<MealEntry>
    suspend fun writeMeals(meals: List<MealEntry>)
}

class MealRepositoryImpl(private val storage: MealStorage) : MealRepository {
    override suspend fun readMeals(): List<MealEntry> =
        withContext(Dispatchers.IO) { storage.read() }

    override suspend fun writeMeals(meals: List<MealEntry>) {
        withContext(Dispatchers.IO) { storage.write(meals) }
    }
}
