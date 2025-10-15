package de.healthai.eatelligent.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val CHAT_ENDPOINT = "https://api.openai.com/v1/chat/completions"

private const val SYSTEM_PROMPT = """
    Du bist ein empathischer Ernährungscoach für Familien. Antworte stets auf Deutsch,
    beziehe die vorhandenen Mahlzeiten- und Profildaten ein und formuliere konkrete,
    umsetzbare Tipps. Wenn du Nährwerte nennst, liefere sie für die komplette Mahlzeit
    statt pro 100 g und teile trotz Unsicherheit deine beste Schätzung. Halte den Ton
    warm, klar und ermutigend.
"""

class OpenAiChatAssistant(private val apiKey: String) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .build()

    suspend fun generateReply(
        profileSummary: String,
        history: List<ChatHistoryEntry>,
        carryOverSummary: String?
    ): String = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) {
            "OpenAI API key is missing. Please provide it in the .env file."
        }

        val attempts = listOf<(String, List<ChatHistoryEntry>, String?) -> Request>(
            ::buildPrimaryRequest,
            ::buildFallbackRequest
        )

        var lastError: IllegalStateException? = null

        for (builder in attempts) {
            val request = builder(profileSummary, history, carryOverSummary)
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (response.isSuccessful) {
                    return@withContext parseResponse(body.orEmpty())
                }

                lastError = IllegalStateException(
                    "OpenAI chat request failed: ${response.code} ${body ?: ""}"
                )
            }
        }

        throw lastError ?: IllegalStateException("OpenAI chat request failed with unknown reason")
    }

    private fun buildPrimaryRequest(
        profileSummary: String,
        history: List<ChatHistoryEntry>,
        carryOverSummary: String?
    ): Request {
        val payload = JSONObject()
            .put("model", "gpt-4.1")
            .put("messages", buildMessages(profileSummary, history, carryOverSummary))
            .put("temperature", 0.5)
            .put("max_tokens", 600)
            .toString()
            .toRequestBody("application/json".toMediaType())

        return baseRequest(payload)
    }

    private fun buildFallbackRequest(
        profileSummary: String,
        history: List<ChatHistoryEntry>,
        carryOverSummary: String?
    ): Request {
        val payload = JSONObject()
            .put("model", "gpt-4o")
            .put("messages", buildMessages(profileSummary, history, carryOverSummary))
            .put("temperature", 0.5)
            .put("max_tokens", 600)
            .toString()
            .toRequestBody("application/json".toMediaType())

        return baseRequest(payload)
    }

    private fun baseRequest(body: okhttp3.RequestBody): Request =
        Request.Builder()
            .url(CHAT_ENDPOINT)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

    private fun buildMessages(
        profileSummary: String,
        history: List<ChatHistoryEntry>,
        carryOverSummary: String?
    ): JSONArray {
        val messages = JSONArray()
        messages.put(
            JSONObject()
                .put("role", "system")
                .put("content", SYSTEM_PROMPT.trimIndent())
        )

        val contextBuilder = StringBuilder()
            .appendLine("Profilübersicht:")
            .appendLine(profileSummary.trim())
        if (!carryOverSummary.isNullOrBlank()) {
            contextBuilder.appendLine().appendLine("Wichtige Punkte aus früheren Gesprächen:")
            contextBuilder.appendLine(carryOverSummary.trim())
        }

        messages.put(
            JSONObject()
                .put("role", "system")
                .put("content", contextBuilder.toString().trimEnd())
        )

        history.forEach { entry ->
            messages.put(
                JSONObject()
                    .put("role", entry.role.apiRole)
                    .put("content", entry.content)
            )
        }

        return messages
    }

    private fun parseResponse(raw: String): String {
        val json = JSONObject(raw)
        val text = when {
            json.has("output") -> parseResponsesOutput(json)
            json.has("choices") -> parseChatChoices(json)
            else -> throw IllegalStateException("Unexpected OpenAI response structure")
        }
        return text.trim().ifBlank {
            throw IllegalStateException("Assistant response was empty")
        }
    }

    private fun parseResponsesOutput(json: JSONObject): String {
        val output = json.optJSONArray("output") ?: throw IllegalStateException("No output from OpenAI")
        val firstMessage = output.optJSONObject(0)
            ?.optJSONArray("content")
            ?.optJSONObject(0)
            ?: throw IllegalStateException("Missing assistant message content")
        return firstMessage.optString("text")
    }

    private fun parseChatChoices(json: JSONObject): String {
        val choices = json.optJSONArray("choices") ?: throw IllegalStateException("No choices from OpenAI")
        val message = choices.optJSONObject(0)
            ?.optJSONObject("message")
            ?: throw IllegalStateException("Missing assistant message")
        val contentArray = message.optJSONArray("content")
        if (contentArray != null && contentArray.length() > 0) {
            val firstPart = contentArray.optJSONObject(0)
            val text = firstPart?.optString("text")
            if (!text.isNullOrBlank()) return text
        }
        val content = message.optString("content")
        if (content.isNullOrBlank()) {
            throw IllegalStateException("Assistant message contained no text content")
        }
        return content
    }
}

enum class ChatHistoryRole { USER, ASSISTANT }

data class ChatHistoryEntry(val role: ChatHistoryRole, val content: String)

private val ChatHistoryRole.apiRole: String
    get() = when (this) {
        ChatHistoryRole.USER -> "user"
        ChatHistoryRole.ASSISTANT -> "assistant"
    }
