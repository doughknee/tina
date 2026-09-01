package com.tina.app.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BackupTest {
    @Test fun roundTrip() {
        val items = listOf(
            Item(id = 1, title = "task", type = ItemType.TASK, createdAt = 10, updatedAt = 20, dueDate = 20696),
            Item(
                id = 2, title = "event", type = ItemType.EVENT, createdAt = 11, updatedAt = 21,
                startAt = 100L, endAt = 200L, recurrence = "FREQ=DAILY", tags = listOf("work"),
            ),
            Item(id = 3, title = "note", type = ItemType.NOTE, createdAt = 12, updatedAt = 22, body = "<p>hi</p>"),
        )
        val decoded = decodeBackup(encodeBackup(items, exportedAt = 99))
        assertEquals(99, decoded?.exportedAt)
        assertEquals(items, decoded?.items)
    }

    @Test fun garbageIsRejected() {
        assertNull(decodeBackup("not json"))
        assertNull(decodeBackup("""{"some":"other json"}"""))
    }
}
