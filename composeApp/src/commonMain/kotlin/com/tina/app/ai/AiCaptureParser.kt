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
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
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
const val ANTHROPIC_DEFAULT_BASE_URL = "https://api.anthropic.com"

data class AnthropicModel(val id: String, val label: String)

/** Current-generation Claude models, best-first; ids are Anthropic API model strings. */
val ANTHROPIC_MODELS = listOf(
    AnthropicModel("claude-opus-5", "Claude Opus 5"),
    AnthropicModel("claude-fable-5", "Claude Fable 5"),
    AnthropicModel("claude-sonnet-5", "Claude Sonnet 5"),
    AnthropicModel("claude-haiku-4-5", "Claude Haiku 4.5"),
)
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

private fun nextWeekdayAfter(from: LocalDate, target: kotlinx.datetime.DayOfWeek): LocalDate {
    val delta = (target.isoDayNumber - from.dayOfWeek.isoDayNumber).let { if (it <= 0) it + 7 else it }
    return from.plus(delta, kotlinx.datetime.DateTimeUnit.DAY)
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
Weekday convention (strict): a bare weekday name means its next occurrence after today, never today;
"next <weekday>" means one week after that. Resolved against today: "friday" is
${nextWeekdayAfter(now.date, DayOfWeek.FRIDAY)}, "next friday" is
${nextWeekdayAfter(now.date, DayOfWeek.FRIDAY).plus(7, kotlinx.datetime.DateTimeUnit.DAY)}.

Text: ${json.encodeToString(kotlinx.serialization.serializer<String>(), raw)}
""".trimIndent()

/**
 * The deterministic parser is authoritative for everything it actually found —
 * its tokens have defined app semantics ("next friday" = bare friday + 7).
 * The AI only fills the gaps: fuzzy times, implied types, cleaned titles.
 */
fun mergeParses(local: ParsedCapture, ai: ParsedCapture): ParsedCapture {
    val merged = ai.copy(
        date = local.date ?: ai.date,
        time = local.time ?: ai.time,
        durationMinutes = local.durationMinutes ?: ai.durationMinutes,
        rrule = local.rrule ?: ai.rrule,
        priority = if (local.priority != Priority.NONE) local.priority else ai.priority,
        tags = local.tags.ifEmpty { ai.tags },
    )
    val type = when {
        ai.type == ItemType.NOTE -> ItemType.NOTE
        merged.rrule != null || merged.time != null -> ItemType.EVENT
        merged.date != null -> ItemType.TASK
        else -> ai.type
    }
    return merged.copy(type = type)
}

class AiCaptureParser(
    private val http: HttpClient,
    private val settingsRepository: SettingsRepository,
    private val network: com.tina.app.data.NetworkStatus,
) {
    /** Wi-Fi only holds cloud calls off metered connections; Ollama is on your own network. */
    private fun blockedByMeteredNetwork(settings: Settings): Boolean =
        settings.aiWifiOnly && settings.aiProvider != AiProvider.OLLAMA && !network.isUnmetered

    /** One best-effort completion against whichever provider is configured. Null on any failure. */
    suspend fun complete(prompt: String): String? {
        val settings = settingsRepository.settings.first()
        if (settings.aiProvider == AiProvider.OFF || settings.aiModel.isBlank()) return null
        if (blockedByMeteredNetwork(settings)) return null
        return runCatching {
            when (settings.aiProvider) {
                AiProvider.ANTHROPIC -> anthropicComplete(settings, prompt)
                else -> openAiComplete(settings, prompt)
            }
        }.getOrNull()
    }

    /** Null on any failure — AI refinement is always best-effort. */
    suspend fun refine(raw: String, now: LocalDateTime, firstDayOfWeek: DayOfWeek): ParsedCapture? {
        val text = complete(buildParsePrompt(raw, now, firstDayOfWeek)) ?: return null
        return aiJsonToParsedCapture(text, raw)
    }

    /** Same path as [refine], but reports what went wrong. Null = success. */
    suspend fun testConnection(now: LocalDateTime, firstDayOfWeek: DayOfWeek): String? {
        val settings = settingsRepository.settings.first()
        if (settings.aiProvider == AiProvider.OFF) return "provider off"
        if (settings.aiModel.isBlank()) return "no model set"
        val prompt = buildParsePrompt("lunch with sam tomorrow at noon", now, firstDayOfWeek)
        val text = try {
            when (settings.aiProvider) {
                AiProvider.ANTHROPIC -> anthropicComplete(settings, prompt)
                else -> openAiComplete(settings, prompt)
            }
        } catch (e: Throwable) {
            val message = e.message
            return (
                if (message != null && message.startsWith("HTTP ")) message
                else "${e::class.simpleName}: $message"
                ).take(200)
        } ?: return "empty response from server"
        return if (aiJsonToParsedCapture(text, "x") != null) null else "model returned unusable JSON"
    }

    private suspend fun openAiComplete(settings: Settings, prompt: String): String? {
        val baseUrl = settings.aiBaseUrl.ifBlank {
            when (settings.aiProvider) {
                AiProvider.OLLAMA -> OLLAMA_DEFAULT_BASE_URL
                AiProvider.OPENAI -> OPENAI_DEFAULT_BASE_URL
                else -> return null
            }
        }.trimEnd('/')
        if (!isAllowedAiEndpoint(baseUrl)) error("insecure endpoint: use https or a LAN address")
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
            val key = settings.aiApiKey.trim()
            if (key.isNotEmpty()) header("Authorization", "Bearer $key")
            setBody(body.toString())
        }
        if (!response.status.isSuccess()) {
            error("HTTP ${response.status.value}: ${response.bodyAsText().take(200)}")
        }
        return json.parseToJsonElement(response.bodyAsText())
            .jsonObject["choices"]?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content
    }

    private suspend fun anthropicComplete(settings: Settings, prompt: String): String? {
        val baseUrl = settings.aiBaseUrl.ifBlank { ANTHROPIC_DEFAULT_BASE_URL }.trimEnd('/')
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
            header("x-api-key", settings.aiApiKey.trim())
            header("anthropic-version", "2023-06-01")
            // multi-workspace identity-linked keys must name their target workspace
            settings.aiWorkspaceId.trim().takeIf { it.isNotEmpty() }?.let {
                header("anthropic-workspace-id", it)
            }
            setBody(body.toString())
        }
        if (!response.status.isSuccess()) {
            error("HTTP ${response.status.value}: ${response.bodyAsText().take(200)}")
        }
        val payload = json.parseToJsonElement(response.bodyAsText()).jsonObject
        if (payload["stop_reason"]?.jsonPrimitive?.content == "refusal") return null
        return payload["content"]?.jsonArray
            ?.firstOrNull { it.jsonObject["type"]?.jsonPrimitive?.content == "text" }
            ?.jsonObject?.get("text")?.jsonPrimitive?.content
    }
}
