package de.healthai.eatelligent.data

import android.content.Context
import java.io.File
import java.io.IOException
import java.time.Instant
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

class MealStorage(context: Context) {
    private val file: File = File(context.filesDir, "meals.json")

    fun read(): List<MealEntry> {
        if (!file.exists()) return emptyList()
        val content = file.readText().takeIf { it.isNotBlank() } ?: return emptyList()
        val root = JSONObject(content)
        val mealsArray = root.optJSONArray("meals") ?: JSONArray()
        return buildList {
            for (index in 0 until mealsArray.length()) {
                val item = mealsArray.optJSONObject(index) ?: continue
                add(
                    MealEntry(
                        id = item.optString("id", UUID.randomUUID().toString()),
                        recordedAt = item.optString("recordedAt")
                            .takeIf { it.isNotBlank() }
                            ?.let(Instant::parse)
                            ?: Instant.now(),
                        description = item.optString("description"),
                        fatGrams = item.optDouble("fatGrams", 0.0),
                        carbGrams = item.optDouble("carbGrams", 0.0),
                        proteinGrams = item.optDouble("proteinGrams", 0.0)
                    )
                )
            }
        }
    }

    @Throws(IOException::class)
    fun write(meals: List<MealEntry>) {
        val array = JSONArray().apply {
            meals.forEach { meal ->
                put(
                    JSONObject().apply {
                        put("id", meal.id)
                        put("recordedAt", MealEntry.formatter.format(meal.recordedAt))
                        put("description", meal.description)
                        put("fatGrams", meal.fatGrams)
                        put("carbGrams", meal.carbGrams)
                        put("proteinGrams", meal.proteinGrams)
                    }
                )
            }
        }
        val root = JSONObject().apply { put("meals", array) }
        file.writeText(root.toString())
    }
}
