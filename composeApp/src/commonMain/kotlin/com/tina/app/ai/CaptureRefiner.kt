package com.tina.app.ai

import com.tina.app.data.Item
import com.tina.app.data.ItemRepository
import com.tina.app.data.SettingsRepository
import com.tina.app.data.itemFromCapture
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Best-effort background refinement of a just-captured item. The built-in parse
 * is always the floor: this only ever upgrades an item after the fact, and never
 * touches one the user has edited in the meantime.
 */
class CaptureRefiner(
    private val repository: ItemRepository,
    private val settingsRepository: SettingsRepository,
    private val aiParser: AiCaptureParser,
) {
    // own scope: refinement must survive the ViewModel/Activity that started it
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun refineInBackground(
        itemId: Long,
        raw: String,
        onRefined: ((original: Item, refined: Item) -> Unit)? = null,
    ) {
        scope.launch {
            val original = repository.get(itemId) ?: return@launch
            val settings = settingsRepository.settings.first()
            val tz = TimeZone.currentSystemDefault()
            // anchor "now" at capture time so defaults (next round hour) stay stable
            val capturedAt = Instant.fromEpochMilliseconds(original.createdAt).toLocalDateTime(tz)
            val aiParsed = aiParser.refine(raw, capturedAt, settings.firstDayOfWeek) ?: return@launch
            val localParsed = com.tina.app.capture.parseCapture(raw, capturedAt, settings.firstDayOfWeek)
            val parsed = mergeParses(localParsed, aiParsed)
            val candidate = itemFromCapture(parsed, capturedAt, tz, settings.defaultReminderMinutes).copy(
                id = original.id,
                createdAt = original.createdAt,
                updatedAt = original.updatedAt,
                sortOrder = original.sortOrder,
                completed = original.completed,
                completedAt = original.completedAt,
                pinned = original.pinned,
                color = original.color,
            )
            if (!meaningfullyDifferent(original, candidate)) return@launch
            val current = repository.get(itemId) ?: return@launch
            if (current != original) return@launch // user touched it while we were thinking
            repository.update(candidate)
            onRefined?.invoke(original, candidate)
        }
    }

    private fun meaningfullyDifferent(a: Item, b: Item): Boolean =
        a.type != b.type || a.title != b.title || a.dueDate != b.dueDate || a.dueTime != b.dueTime ||
            a.startAt != b.startAt || a.endAt != b.endAt || a.priority != b.priority ||
            a.tags != b.tags || a.recurrence != b.recurrence || a.body != b.body
}
