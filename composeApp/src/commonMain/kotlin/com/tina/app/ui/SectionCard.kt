package com.tina.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp

/**
 * Wraps one lazy-list row so a run of rows renders as a single contained
 * section card (surfaceContainerLow, 16dp corners) without breaking lazy
 * composition. Draws the inter-row divider on every row but the first.
 */
@Composable
fun SectionCardItem(
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val first = index == 0
    val last = index == count - 1
    val shape = when {
        first && last -> RoundedCornerShape(16.dp)
        first -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        last -> RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
        else -> RectangleShape
    }
    Column(
        modifier
            .padding(horizontal = 16.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(top = if (first) 4.dp else 0.dp, bottom = if (last) 4.dp else 0.dp),
    ) {
        if (!first) {
            HorizontalDivider(
                Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
        content()
    }
}
