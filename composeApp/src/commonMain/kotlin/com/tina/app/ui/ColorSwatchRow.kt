package com.tina.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val ITEM_COLOR_PRESETS = listOf(
    0xFFD32F2FL, 0xFFF57C00L, 0xFFFBC02DL, 0xFF388E3CL, 0xFF1976D2L, 0xFF7B1FA2L,
)

/** Default-plus-presets color picker used by events and notes. */
@Composable
fun ColorSwatchRow(selected: Long?, onSelect: (Long?) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        (listOf<Long?>(null) + ITEM_COLOR_PRESETS).forEach { preset ->
            Box(
                Modifier
                    .size(36.dp)
                    .background(preset?.let { Color(it) } ?: MaterialTheme.colorScheme.primary, CircleShape)
                    .let {
                        if (selected == preset) it.border(3.dp, MaterialTheme.colorScheme.outline, CircleShape) else it
                    }
                    .clickable { onSelect(preset) },
                contentAlignment = Alignment.Center,
            ) {
                if (selected == preset) {
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
