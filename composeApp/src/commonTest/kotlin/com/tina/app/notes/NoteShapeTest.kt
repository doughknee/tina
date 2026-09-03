package com.tina.app.notes

import com.tina.app.data.Item
import com.tina.app.data.ItemType
import kotlin.test.Test
import kotlin.test.assertEquals

class NoteShapeTest {
    private fun note(title: String, body: String? = null) =
        Item(title = title, body = body, type = ItemType.NOTE, createdAt = 0, updatedAt = 0)

    @Test fun titledNoteKeepsTitleAndPreview() {
        val p = previewOf(note("Recipe: weeknight dal", "<p>Onion, garlic, <b>ginger</b>.</p>"))
        assertEquals(NoteShape.TITLED, p.shape)
        assertEquals("Onion, garlic, ginger.", p.text)
    }

    @Test fun proseWithoutBodyIsAScrap() {
        val p = previewOf(note("Make it work, make it right, make it fast."))
        assertEquals(NoteShape.SCRAP, p.shape)
        assertEquals("", p.title)
        assertEquals("Make it work, make it right, make it fast.", p.text)
    }

    @Test fun shortLabelWithoutBodyStaysTitled() {
        assertEquals(NoteShape.TITLED, previewOf(note("Guest wifi")).shape)
    }

    @Test fun listBodyBecomesItemsWithOverflowCount() {
        val body = "<ul>" + (1..6).joinToString("") { "<li>item $it</li>" } + "</ul>"
        val p = previewOf(note("Trip packing list", body))
        assertEquals(NoteShape.LIST, p.shape)
        assertEquals(listOf("item 1", "item 2", "item 3", "item 4"), p.items)
        assertEquals(2, p.moreItems)
    }

    @Test fun splitUsesFirstLine() {
        assertEquals("Gift ideas for mom" to "A good kettle.", splitIdea("Gift ideas for mom\nA good kettle."))
    }

    @Test fun splitLeavesShortTextAlone() {
        assertEquals("Guest wifi casa-5G" to null, splitIdea("Guest wifi casa-5G"))
    }

    @Test fun splitLongTextAtFirstSentence() {
        val text = "The capture bar should stay put when I switch tabs. Half of what I lose, I lose because I had to go looking for the field."
        val (title, body) = splitIdea(text)
        assertEquals("The capture bar should stay put when I switch tabs.", title)
        assertEquals("Half of what I lose, I lose because I had to go looking for the field.", body)
    }

    @Test fun splitLongSingleSentenceStaysWhole() {
        val text = "What if Sort had a weekly review that just showed everything I never triaged and forgot about entirely"
        assertEquals(text to null, splitIdea(text))
    }
}
