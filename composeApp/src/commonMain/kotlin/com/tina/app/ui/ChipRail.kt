package com.tina.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Single-line horizontally scrolling chip row that starts with the selection visible. */
@Composable
fun ChipRail(
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    scrollKey: Any = Unit,
    content: LazyListScope.() -> Unit,
) {
    val state = rememberLazyListState()
    LaunchedEffect(scrollKey) {
        if (selectedIndex > 0) state.scrollToItem(selectedIndex)
    }
    LazyRow(
        modifier,
        state = state,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}
