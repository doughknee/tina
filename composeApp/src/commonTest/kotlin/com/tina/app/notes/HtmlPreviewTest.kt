package com.tina.app.notes

import kotlin.test.Test
import kotlin.test.assertEquals

class HtmlPreviewTest {
    @Test fun stripsTagsAndDecodesEntities() {
        assertEquals(
            "Sam's list: bread & milk",
            htmlPreview("<p>Sam&#39;s list: <b>bread</b> &amp; milk</p>"),
        )
    }

    @Test fun decodesHexAndNamedEntities() {
        assertEquals("a'b \"c\" <d>", htmlPreview("a&#x27;b &quot;c&quot; &lt;d&gt;"))
    }

    @Test fun unknownEntityLeftAlone() {
        assertEquals("&bogus;", htmlPreview("&bogus;"))
    }
}
