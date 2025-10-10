package de.healthai.eatelligent.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class OpenAiMealAnalyzer(private val apiKey: String) {
    private val client = OkHttpClient()

    suspend fun analyze(base64Image: String): MealAnalysis = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) {
            "OpenAI API key is missing. Please provide it in the .env file."
        }

        val requestBody = buildRequestBody(base64Image)
        val request = Request.Builder()
            .url("https://api.openai.com/v1/responses")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                throw IllegalStateException("OpenAI request failed: ${response.code} ${errorBody ?: ""}")
            }

            val payload = response.body?.string().orEmpty()
            parseResponse(payload)
        }
    }

    private fun buildRequestBody(base64Image: String) =
        JSONObject()
            .put("model", "gpt-4.1-mini")
            .put(
                "response",
                JSONObject().apply {
                    put("modalities", JSONArray(listOf("text")))
                    put(
                        "text",
                        JSONObject().apply {
                            put("format", "json_schema")
                            put(
                                "json_schema",
                                JSONObject().apply {
                                    put("name", "meal_analysis")
                                    put(
                                        "schema",
                                        JSONObject().apply {
                                            put("type", "object")
                                            put(
                                                "properties",
                                                JSONObject()
                                                    .put("description", JSONObject().put("type", "string"))
                                                    .put(
                                                        "macros",
                                                        JSONObject().apply {
                                                            put("type", "object")
                                                            put(
                                                                "properties",
                                                                JSONObject()
                                                                    .put("fatGrams", numberSchema("Total fat in grams"))
                                                                    .put("carbGrams", numberSchema("Total carbohydrates in grams"))
                                                                    .put("proteinGrams", numberSchema("Total protein in grams"))
                                                            )
                                                            put(
                                                                "required",
                                                                JSONArray(listOf("fatGrams", "carbGrams", "proteinGrams"))
                                                            )
                                                        }
                                                    )
                                            )
                                            put("required", JSONArray(listOf("description", "macros")))
                                        }
                                    )
                                }
                            )
                        }
                    )
                }
            )
            .put(
                "input",
                JSONArray().apply {
                    put(
                        JSONObject().apply {
                            put("role", "system")
                            put(
                                "content",
                                JSONArray().apply {
                                    put(
                                        JSONObject().apply {
                                            put("type", "text")
                                            put(
                                                "text",
                                                "You are a pediatric nutrition assistant. Return short kid-friendly " +
                                                    "meal descriptions and macro nutrients in grams."
                                            )
                                        }
                                    )
                                }
                            )
                        }
                    )
                    put(
                        JSONObject().apply {
                            put("role", "user")
                            put(
                                "content",
                                JSONArray().apply {
                                    put(JSONObject().apply {
                                        put("type", "input_image")
                                        put("image_base64", base64Image)
                                    })
                                }
                            )
                        }
                    )
                }
            )
            .toString()
            .toRequestBody("application/json".toMediaType())

    private fun numberSchema(description: String): JSONObject =
        JSONObject()
            .put("type", "number")
            .put("description", description)

    private fun parseResponse(raw: String): MealAnalysis {
        val json = JSONObject(raw)
        val output = json.optJSONArray("output") ?: throw IllegalStateException("No output from OpenAI")
        val firstMessage = output.optJSONObject(0)
            ?.optJSONArray("content")
            ?.optJSONObject(0)
            ?: throw IllegalStateException("Missing assistant message content")
        val text = firstMessage.optString("text")
        val structured = JSONObject(text)
        val macros = structured.optJSONObject("macros") ?: JSONObject()
        return MealAnalysis(
            description = structured.optString("description"),
            fatGrams = macros.optDouble("fatGrams", 0.0),
            carbGrams = macros.optDouble("carbGrams", 0.0),
            proteinGrams = macros.optDouble("proteinGrams", 0.0)
        )
    }
}
