package com.tina.app.notes

import com.tina.app.data.Item

/** How a card renders, decided once per note here rather than in the composable. */
enum class NoteShape { TITLED, SCRAP, LIST }

/** One list line; [checked] is null for a plain bullet, set for a checklist item. */
data class NoteItem(val text: String, val checked: Boolean? = null)

data class NotePreview(
    val shape: NoteShape,
    /** Empty for a scrap: the text is the whole card. */
    val title: String,
    /** Body preview for a titled note, the full text for a scrap, empty for a list. */
    val text: String,
    val items: List<NoteItem> = emptyList(),
    val moreItems: Int = 0,
    /** Checklist totals over every item, not just the ones shown. */
    val done: Int = 0,
    val total: Int = 0,
) {
    val isChecklist: Boolean get() = items.any { it.checked != null }
}

/**
 * A checklist item is a list line that starts with one of these. The editor has no task-list
 * model, so the state lives in the text itself and survives export, search and plain paste.
 */
const val UNCHECKED = '☐' // ☐
const val CHECKED = '☑' // ☑

private val LIST_ITEM = Regex("<li[^>]*>(.*?)</li>", RegexOption.DOT_MATCHES_ALL)
private const val PREVIEW_ITEMS = 4
/** Above this a one-line capture is a thought, not a headline. */
private const val TITLE_MAX = 80

fun previewOf(item: Item): NotePreview {
    val body = item.body
    if (body != null) {
        val items = LIST_ITEM.findAll(body).map { noteItem(htmlPreview(it.groupValues[1])) }.filter { it.text.isNotBlank() }.toList()
        if (items.isNotEmpty()) {
            return NotePreview(
                NoteShape.LIST, item.title, "",
                items = items.take(PREVIEW_ITEMS),
                moreItems = (items.size - PREVIEW_ITEMS).coerceAtLeast(0),
                done = items.count { it.checked == true },
                total = items.count { it.checked != null },
            )
        }
        val text = htmlPreview(body)
        if (text.isNotBlank()) return NotePreview(NoteShape.TITLED, item.title, text)
    }
    return if (isProse(item.title)) NotePreview(NoteShape.SCRAP, "", item.title)
    else NotePreview(NoteShape.TITLED, item.title, "")
}

private fun noteItem(line: String): NoteItem = when (line.firstOrNull()) {
    UNCHECKED -> NoteItem(line.drop(1).trim(), checked = false)
    CHECKED -> NoteItem(line.drop(1).trim(), checked = true)
    else -> NoteItem(line)
}

/**
 * Flip the [index]th list item's box in the stored HTML. Only the marker character changes,
 * so formatting inside the item is untouched. Returns the input unchanged if that item is
 * not a checklist item.
 */
fun toggleChecklistItem(html: String, index: Int): String {
    var seen = 0
    return LIST_ITEM.replace(html) { m ->
        val i = seen++
        if (i != index) return@replace m.value
        val inner = m.groupValues[1]
        // the marker may sit behind inline tags (<b>☐ milk</b>): flip the first marker char
        val pos = inner.indexOfFirst { it == UNCHECKED || it == CHECKED }
        if (pos < 0) return@replace m.value
        val flipped = if (inner[pos] == UNCHECKED) CHECKED else UNCHECKED
        m.value.replaceRange(m.value.indexOf(inner) + pos, m.value.indexOf(inner) + pos + 1, flipped.toString())
    }
}

/** A sentence rather than a label: long, or punctuated like one. */
fun isProse(text: String): Boolean {
    val t = text.trim()
    return t.length > 40 || t.lastOrNull() in listOf('.', '?', '!') || t.count { it == ',' } >= 2
}

/**
 * Capture puts everything in the title. Split so a long thought reads as a note: the first
 * line, or the first sentence when the text runs past [TITLE_MAX], becomes the title.
 */
fun splitIdea(text: String): Pair<String, String?> {
    val t = text.trim()
    val nl = t.indexOf('\n')
    if (nl > 0) return t.substring(0, nl).trim() to t.substring(nl + 1).trim().ifEmpty { null }
    if (t.length <= TITLE_MAX) return t to null
    val m = Regex("""[.!?]\s+""").find(t) ?: return t to null
    return t.substring(0, m.range.first + 1) to t.substring(m.range.last + 1).trim().ifEmpty { null }
}
