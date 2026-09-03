package com.tina.app.notes

import com.tina.app.data.Item

/** How a card renders, decided once per note here rather than in the composable. */
enum class NoteShape { TITLED, SCRAP, LIST }

data class NotePreview(
    val shape: NoteShape,
    /** Empty for a scrap: the text is the whole card. */
    val title: String,
    /** Body preview for a titled note, the full text for a scrap, empty for a list. */
    val text: String,
    val items: List<String> = emptyList(),
    val moreItems: Int = 0,
)

private val LIST_ITEM = Regex("<li[^>]*>(.*?)</li>", RegexOption.DOT_MATCHES_ALL)
private const val PREVIEW_ITEMS = 4
/** Above this a one-line capture is a thought, not a headline. */
private const val TITLE_MAX = 80

fun previewOf(item: Item): NotePreview {
    val body = item.body
    if (body != null) {
        val items = LIST_ITEM.findAll(body).map { htmlPreview(it.groupValues[1]) }.filter { it.isNotBlank() }.toList()
        if (items.isNotEmpty()) {
            return NotePreview(
                NoteShape.LIST, item.title, "",
                items = items.take(PREVIEW_ITEMS),
                moreItems = (items.size - PREVIEW_ITEMS).coerceAtLeast(0),
            )
        }
        val text = htmlPreview(body)
        if (text.isNotBlank()) return NotePreview(NoteShape.TITLED, item.title, text)
    }
    return if (isProse(item.title)) NotePreview(NoteShape.SCRAP, "", item.title)
    else NotePreview(NoteShape.TITLED, item.title, "")
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
