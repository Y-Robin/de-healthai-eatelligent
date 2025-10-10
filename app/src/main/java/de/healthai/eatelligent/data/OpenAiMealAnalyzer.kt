package de.healthai.eatelligent.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class OpenAiMealAnalyzer(private val apiKey: String) {
    private val client = OkHttpClient()

    suspend fun analyze(base64Image: String): MealAnalysis = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) {
            "OpenAI API key is missing. Please provide it in the .env file."
        }

        val attempts = listOf<(String) -> RequestBody>(
            ::buildModernRequestBody,
            ::buildLegacyRequestBody
        )

        var firstError: IllegalStateException? = null

        for ((index, bodyBuilder) in attempts.withIndex()) {
            val request = buildRequest(bodyBuilder(base64Image))
            client.newCall(request).execute().use { response ->
                val rawBody = response.body?.string()

                if (response.isSuccessful) {
                    return@withContext parseResponse(rawBody.orEmpty())
                }

                val error = IllegalStateException(
                    "OpenAI request failed: ${response.code} ${rawBody ?: ""}"
                )

                if (index == 0 && shouldRetryWithLegacy(response.code, rawBody)) {
                    firstError = error
                } else {
                    throw error
                }
            }
        }

        throw firstError ?: IllegalStateException("OpenAI request failed with unknown reason")
    }

    private fun buildRequest(requestBody: RequestBody): Request =
        Request.Builder()
            .url("https://api.openai.com/v1/responses")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

    private fun buildModernRequestBody(base64Image: String) =
        JSONObject()
            .put("model", "gpt-4.1-mini")
            .put("modalities", JSONArray(listOf("text")))
            .put("response_format", jsonSchemaResponseFormat())
            .put("input", sharedInput(base64Image))
            .toString()
            .toRequestBody("application/json".toMediaType())

    private fun buildLegacyRequestBody(base64Image: String) =
        JSONObject()
            .put("model", "gpt-4.1-mini")
            .put("input", sharedInput(base64Image))
            .put("response_format", jsonSchemaResponseFormat())
            .toString()
            .toRequestBody("application/json".toMediaType())

    private fun jsonSchemaResponseFormat(): JSONObject =
        JSONObject().apply {
            put("type", "json_schema")
            put(
                "json_schema",
                JSONObject().apply {
                    put("name", "meal_analysis")
                    put("schema", analysisSchema())
                }
            )
        }

    private fun numberSchema(description: String): JSONObject =
        JSONObject()
            .put("type", "number")
            .put("description", description)

    private fun sharedInput(base64Image: String) =
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
                            put(imageContent(base64Image))
                        }
                    )
                }
            )
        }

    private fun imageContent(base64Image: String): JSONObject {
        val sanitized = base64Image.trim()
        val dataUrl = if (sanitized.startsWith("data:")) {
            sanitized
        } else {
            "data:image/jpeg;base64,$sanitized"
        }

        return JSONObject()
            .put("type", "input_image")
            .put("image_url", JSONObject().put("url", dataUrl))
    }

    private fun analysisSchema(): JSONObject =
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

    private fun shouldRetryWithLegacy(code: Int, errorBody: String?): Boolean {
        if (code != 400) return false
        val normalized = errorBody?.lowercase() ?: return false
        return listOf(
            "unknown parameter",
            "response_format",
            "response format",
            "text.format"
        ).any(normalized::contains)
    }

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
