package com.tina.app.ai

import com.tina.app.data.Item
import com.tina.app.data.ItemType
import com.tina.app.data.Priority
import com.tina.app.data.SettingsRepository
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val improveJson = Json { ignoreUnknownKeys = true; isLenient = true }

@Serializable
data class ImproveQuestion(
    val id: String = "",
    val question: String = "",
    val options: List<String> = emptyList(),
    val allowCustom: Boolean = true,
)

@Serializable
private data class ImprovePatchDto(
    val title: String? = null,
    val type: String? = null,
    val date: String? = null,
    val time: String? = null,
    val durationMinutes: Int? = null,
    val priority: String? = null,
    val tags: List<String>? = null,
    val rrule: String? = null,
    val body: String? = null,
    val reminderOffsetMinutes: Int? = null,
    val rationale: String? = null,
    val questions: List<ImproveQuestion> = emptyList(),
)

/** A partial change-set for one item; null field = leave it alone. */
data class ImprovePatch(
    val title: String? = null,
    val type: ItemType? = null,
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val durationMinutes: Int? = null,
    val priority: Priority? = null,
    val tags: List<String>? = null,
    val rrule: String? = null,
    val body: String? = null,
    val reminderOffsetMinutes: Int? = null,
    val rationale: String? = null,
    val questions: List<ImproveQuestion> = emptyList(),
) {
    val isEmpty: Boolean
        get() = title == null && type == null && date == null && time == null &&
            durationMinutes == null && priority == null && tags == null && rrule == null &&
            body == null && reminderOffsetMinutes == null
}

/** Stable keys for the toggleable change chips. */
object ImproveField {
    const val TITLE = "title"
    const val TYPE = "type"
    const val DATE = "date"
    const val TIME = "time"
    const val DURATION = "duration"
    const val PRIORITY = "priority"
    const val TAGS = "tags"
    const val RRULE = "rrule"
    const val BODY = "body"
    const val REMINDER = "reminder"
}

/** Tolerant JSON → patch; drops anything unparseable, null only when there is no JSON at all. */
fun parseImprovePatch(text: String, item: Item): ImprovePatch? {
    val start = text.indexOf('{')
    val end = text.lastIndexOf('}')
    if (start < 0 || end <= start) return null
    val dto = runCatching {
        improveJson.decodeFromString<ImprovePatchDto>(text.substring(start, end + 1))
    }.getOrNull() ?: return null
    val type = ItemType.entries.firstOrNull { it.name.equals(dto.type, ignoreCase = true) }
    val tags = dto.tags?.map { it.trim().lowercase().removePrefix("#") }?.filter { it.isNotEmpty() }
    val patch = ImprovePatch(
        title = dto.title?.trim().takeUnless { it.isNullOrBlank() || it == item.title },
        type = type?.takeUnless { it == item.type },
        date = dto.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
        time = dto.time?.let { runCatching { LocalTime.parse(it) }.getOrNull() },
        durationMinutes = dto.durationMinutes?.takeIf { it > 0 },
        priority = Priority.entries.firstOrNull { it.name.equals(dto.priority, ignoreCase = true) }
            ?.takeUnless { it == item.priority },
        tags = tags?.takeUnless { it == item.tags },
        rrule = dto.rrule?.takeUnless { it.isBlank() || it == item.recurrence },
        body = dto.body?.takeUnless { it.isBlank() || it == item.body },
        reminderOffsetMinutes = dto.reminderOffsetMinutes
            ?.takeUnless { it == item.reminderOffsetMinutes },
        rationale = dto.rationale?.takeUnless { it.isBlank() },
        questions = dto.questions.filter { it.question.isNotBlank() }.take(3),
    )
    return patch
}

/**
 * Applies the enabled subset of a patch. Schedule fields are normalized per
 * target type: events carry startAt/endAt, tasks carry dueDate/dueTime.
 */
fun applyImprovePatch(item: Item, patch: ImprovePatch, enabled: Set<String>, tz: TimeZone): Item {
    fun on(key: String) = key in enabled
    var out = item
    if (on(ImproveField.TITLE) && patch.title != null) out = out.copy(title = patch.title)
    if (on(ImproveField.PRIORITY) && patch.priority != null) out = out.copy(priority = patch.priority)
    if (on(ImproveField.TAGS) && patch.tags != null) out = out.copy(tags = patch.tags)
    if (on(ImproveField.BODY) && patch.body != null) out = out.copy(body = patch.body)
    if (on(ImproveField.RRULE) && patch.rrule != null) out = out.copy(recurrence = patch.rrule)
    if (on(ImproveField.REMINDER) && patch.reminderOffsetMinutes != null) {
        out = out.copy(reminderOffsetMinutes = patch.reminderOffsetMinutes)
    }

    val targetType = if (on(ImproveField.TYPE) && patch.type != null) patch.type else out.type
    val newDate = if (on(ImproveField.DATE)) patch.date else null
    val newTime = if (on(ImproveField.TIME)) patch.time else null
    val duration = if (on(ImproveField.DURATION)) patch.durationMinutes else null

    val scheduleTouched = targetType != out.type || newDate != null || newTime != null || duration != null
    if (!scheduleTouched) return out

    // current schedule as local date/time regardless of representation
    val currentStart = out.startAt?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(tz) }
    val baseDate = newDate ?: currentStart?.date ?: out.dueLocalDate
    val baseTime = newTime ?: currentStart?.time
        ?: out.dueTime?.let { LocalTime(it / 60, it % 60) }

    return when (targetType) {
        ItemType.EVENT -> {
            val date = baseDate ?: return out.copy(type = targetType)
            val time = baseTime ?: LocalTime(9, 0)
            val startMillis = LocalDateTime(date, time).toInstant(tz).toEpochMilliseconds()
            val minutes = duration
                ?: out.endAt?.let { e -> out.startAt?.let { s -> ((e - s) / 60_000L).toInt() } }
                ?: 60
            out.copy(
                type = ItemType.EVENT,
                startAt = startMillis,
                endAt = startMillis + minutes * 60_000L,
                dueDate = null,
                dueTime = null,
            )
        }
        ItemType.TASK, ItemType.INBOX -> out.copy(
            type = targetType,
            dueDate = baseDate?.toEpochDays()?.toInt(),
            dueTime = baseTime?.let { it.hour * 60 + it.minute },
            startAt = null,
            endAt = null,
            recurrence = if (targetType == ItemType.INBOX) null else out.recurrence,
        )
        ItemType.NOTE -> out.copy(
            type = ItemType.NOTE,
            dueDate = null,
            dueTime = null,
            startAt = null,
            endAt = null,
            recurrence = null,
        )
    }
}

/** In-memory stash of background suggestions (SUGGEST mode); lost on process death by design. */
object SuggestionCache {
    val patches = MutableStateFlow<Map<Long, ImprovePatch>>(emptyMap())

    fun put(id: Long, patch: ImprovePatch) {
        patches.value = patches.value + (id to patch)
    }

    fun remove(id: Long) {
        patches.value = patches.value - id
    }
}

class AiImprover(
    private val parser: AiCaptureParser,
    private val settingsRepository: SettingsRepository,
) {
    private fun itemSummary(item: Item, tz: TimeZone): String = buildString {
        append("{\"title\":").append(improveJson.encodeToString(item.title))
        append(",\"type\":\"").append(item.type.name).append('"')
        item.dueLocalDate?.let { append(",\"date\":\"").append(it).append('"') }
        item.dueTime?.let { append(",\"time\":\"").append(LocalTime(it / 60, it % 60)).append('"') }
        item.startAt?.let {
            val s = Instant.fromEpochMilliseconds(it).toLocalDateTime(tz)
            append(",\"date\":\"").append(s.date).append("\",\"time\":\"").append(s.time).append('"')
        }
        if (item.allDay) append(",\"allDay\":true")
        append(",\"priority\":\"").append(item.priority.name).append('"')
        if (item.tags.isNotEmpty()) append(",\"tags\":").append(improveJson.encodeToString(item.tags))
        item.recurrence?.let { append(",\"rrule\":").append(improveJson.encodeToString(it)) }
        item.reminderOffsetMinutes?.let { append(",\"reminderOffsetMinutes\":").append(it) }
        item.body?.let { append(",\"body\":").append(improveJson.encodeToString(it.take(500))) }
        append('}')
    }

    private suspend fun basePrompt(item: Item, now: LocalDateTime, tz: TimeZone): String {
        val instructions = settingsRepository.settings.first().aiInstructions.trim()
        return """
You improve one existing item in a personal task/calendar/notes app.
Today is ${now.date} (${now.date.dayOfWeek}), current time ${now.time}.

Item: ${itemSummary(item, tz)}
${if (instructions.isNotEmpty()) "The user's standing improvement instructions (follow them):\n$instructions\n" else ""}
Return ONLY a JSON object. Include ONLY the fields you would change — omit anything that should
stay as it is. Available fields:
{"title": string, "type": "INBOX"|"TASK"|"EVENT"|"NOTE", "date": "YYYY-MM-DD", "time": "HH:MM",
 "durationMinutes": int, "priority": "NONE"|"LOW"|"MEDIUM"|"HIGH", "tags": [strings],
 "rrule": RFC5545 RRULE, "body": string, "reminderOffsetMinutes": int,
 "rationale": one short sentence explaining the suggestions,
 "questions": [{"id": string, "question": string, "options": [strings], "allowCustom": bool}]}

Suggest conservatively: clean up wording, resolve implied times of day ("when they open" is about
09:00, "after work" about 17:30), promote clearly timed tasks to events, add obviously missing
tags or reminders. Never invent facts. Ask at most 2 questions, and only when the answer would
materially improve the item; keep options short (2-4 words).
""".trimIndent()
    }

    /** First round: analyze the item, propose changes and optional questions. Null = provider failure. */
    suspend fun suggest(item: Item): ImprovePatch? {
        val tz = TimeZone.currentSystemDefault()
        val now = kotlin.time.Clock.System.now().toLocalDateTime(tz)
        val text = parser.complete(basePrompt(item, now, tz)) ?: return null
        return parseImprovePatch(text, item)
    }

    /** Second round: same contract, with the user's answers folded in. No further questions. */
    suspend fun finalize(item: Item, answers: Map<String, String>): ImprovePatch? {
        val tz = TimeZone.currentSystemDefault()
        val now = kotlin.time.Clock.System.now().toLocalDateTime(tz)
        val answerBlock = answers.entries.joinToString("\n") { (q, a) -> "Q: $q\nA: $a" }
        val prompt = basePrompt(item, now, tz) +
            "\n\nThe user answered your questions:\n$answerBlock\n" +
            "Now return the final JSON change-set. Do not include \"questions\"."
        val text = parser.complete(prompt) ?: return null
        return parseImprovePatch(text, item)?.copy(questions = emptyList())
    }
}
