package com.tina.app.ai

import com.tina.app.capture.ParsedCapture
import com.tina.app.data.AiProvider
import com.tina.app.data.ItemType
import com.tina.app.data.Priority
import com.tina.app.data.Settings
import com.tina.app.data.SettingsRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

const val ANTHROPIC_DEFAULT_MODEL = "claude-opus-5"
const val OLLAMA_DEFAULT_BASE_URL = "http://localhost:11434/v1"
const val OPENAI_DEFAULT_BASE_URL = "https://api.openai.com/v1"

private val json = Json { ignoreUnknownKeys = true; isLenient = true }

@Serializable
private data class AiParseDto(
    val title: String? = null,
    val type: String? = null,
    val date: String? = null,
    val time: String? = null,
    val durationMinutes: Int? = null,
    val priority: String? = null,
    val tags: List<String> = emptyList(),
    val rrule: String? = null,
    val body: String? = null,
)

/** Tolerant mapping from whatever JSON the model produced to a ParsedCapture. Null = unusable. */
fun aiJsonToParsedCapture(text: String, fallbackTitle: String): ParsedCapture? {
    val start = text.indexOf('{')
    val end = text.lastIndexOf('}')
    if (start < 0 || end <= start) return null
    val dto = runCatching { json.decodeFromString<AiParseDto>(text.substring(start, end + 1)) }
        .getOrNull() ?: return null
    val type = ItemType.entries.firstOrNull { it.name.equals(dto.type, ignoreCase = true) } ?: return null
    return ParsedCapture(
        title = dto.title?.trim().takeUnless { it.isNullOrBlank() } ?: fallbackTitle,
        type = type,
        date = dto.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
        time = dto.time?.let { runCatching { LocalTime.parse(it) }.getOrNull() },
        durationMinutes = dto.durationMinutes?.takeIf { it > 0 },
        priority = Priority.entries.firstOrNull { it.name.equals(dto.priority, ignoreCase = true) }
            ?: Priority.NONE,
        tags = dto.tags.map { it.trim().lowercase().removePrefix("#") }.filter { it.isNotEmpty() },
        rrule = dto.rrule?.takeUnless { it.isBlank() },
        body = if (type == ItemType.NOTE) dto.body?.takeUnless { it.isBlank() } ?: fallbackTitle else null,
    )
}

fun buildParsePrompt(raw: String, now: LocalDateTime, firstDayOfWeek: DayOfWeek): String = """
You convert one captured line of text into structured JSON for a personal task/calendar/notes app.
Today is ${now.date} (${now.date.dayOfWeek}), current time ${now.time}. The week starts on $firstDayOfWeek.

Return ONLY a JSON object, no prose, with exactly these fields:
{"title": string, "type": "INBOX"|"TASK"|"EVENT"|"NOTE", "date": "YYYY-MM-DD" or null,
 "time": "HH:MM" (24h) or null, "durationMinutes": int or null,
 "priority": "NONE"|"LOW"|"MEDIUM"|"HIGH", "tags": [strings], "rrule": RFC5545 RRULE string or null,
 "body": string or null}

Rules: a concrete clock time means EVENT; a date without a time means TASK; no signals means TASK
with null date; multi-sentence prose means NOTE (body = full text); keep INBOX only if genuinely
undecidable. "title" is the text with all parsed tokens removed, cleaned up. Resolve relative dates
against today. "!" means MEDIUM, "!!" means HIGH. "#word" entries become tags.

Text: ${json.encodeToString(kotlinx.serialization.serializer<String>(), raw)}
""".trimIndent()

class AiCaptureParser(
    private val http: HttpClient,
    private val settingsRepository: SettingsRepository,
) {
    /** Null on any failure — AI refinement is always best-effort. */
    suspend fun refine(raw: String, now: LocalDateTime, firstDayOfWeek: DayOfWeek): ParsedCapture? {
        val settings = settingsRepository.settings.first()
        if (settings.aiProvider == AiProvider.OFF || settings.aiModel.isBlank()) return null
        val prompt = buildParsePrompt(raw, now, firstDayOfWeek)
        val text = runCatching {
            when (settings.aiProvider) {
                AiProvider.ANTHROPIC -> anthropicComplete(settings, prompt)
                else -> openAiComplete(settings, prompt)
            }
        }.getOrNull() ?: return null
        return aiJsonToParsedCapture(text, raw)
    }

    private suspend fun openAiComplete(settings: Settings, prompt: String): String? {
        val baseUrl = settings.aiBaseUrl.ifBlank {
            when (settings.aiProvider) {
                AiProvider.OLLAMA -> OLLAMA_DEFAULT_BASE_URL
                AiProvider.OPENAI -> OPENAI_DEFAULT_BASE_URL
                else -> return null
            }
        }.trimEnd('/')
        val body = buildJsonObject {
            put("model", settings.aiModel)
            put("temperature", 0)
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    put("content", prompt)
                })
            })
        }
        val response = http.post("$baseUrl/chat/completions") {
            contentType(ContentType.Application.Json)
            if (settings.aiApiKey.isNotBlank()) header("Authorization", "Bearer ${settings.aiApiKey}")
            setBody(body.toString())
        }
        if (!response.status.isSuccess()) return null
        return json.parseToJsonElement(response.bodyAsText())
            .jsonObject["choices"]?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content
    }

    private suspend fun anthropicComplete(settings: Settings, prompt: String): String? {
        val baseUrl = settings.aiBaseUrl.ifBlank { "https://api.anthropic.com" }.trimEnd('/')
        val body = buildJsonObject {
            put("model", settings.aiModel)
            put("max_tokens", 1024)
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    put("content", prompt)
                })
            })
        }
        val response = http.post("$baseUrl/v1/messages") {
            contentType(ContentType.Application.Json)
            header("x-api-key", settings.aiApiKey)
            header("anthropic-version", "2023-06-01")
            setBody(body.toString())
        }
        if (!response.status.isSuccess()) return null
        val payload = json.parseToJsonElement(response.bodyAsText()).jsonObject
        if (payload["stop_reason"]?.jsonPrimitive?.content == "refusal") return null
        return payload["content"]?.jsonArray
            ?.firstOrNull { it.jsonObject["type"]?.jsonPrimitive?.content == "text" }
            ?.jsonObject?.get("text")?.jsonPrimitive?.content
    }
}
