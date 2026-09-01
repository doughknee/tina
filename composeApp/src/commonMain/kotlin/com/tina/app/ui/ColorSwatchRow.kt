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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tina.app.resources.Res
import com.tina.app.resources.color_blue
import com.tina.app.resources.color_default
import com.tina.app.resources.color_green
import com.tina.app.resources.color_orange
import com.tina.app.resources.color_purple
import com.tina.app.resources.color_red
import com.tina.app.resources.color_yellow
import org.jetbrains.compose.resources.stringResource

val ITEM_COLOR_PRESETS = listOf(
    0xFFD32F2FL, 0xFFF57C00L, 0xFFFBC02DL, 0xFF388E3CL, 0xFF1976D2L, 0xFF7B1FA2L,
)

/** Spoken name for a preset swatch (null = the theme default). */
@Composable
fun colorName(color: Long?): String = stringResource(
    when (color) {
        0xFFD32F2FL -> Res.string.color_red
        0xFFF57C00L -> Res.string.color_orange
        0xFFFBC02DL -> Res.string.color_yellow
        0xFF388E3CL -> Res.string.color_green
        0xFF1976D2L -> Res.string.color_blue
        0xFF7B1FA2L -> Res.string.color_purple
        else -> Res.string.color_default
    },
)

/** Default-plus-presets color picker used by events and notes. */
@Composable
fun ColorSwatchRow(selected: Long?, onSelect: (Long?) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        (listOf<Long?>(null) + ITEM_COLOR_PRESETS).forEach { preset ->
            val name = colorName(preset)
            val isSelected = selected == preset
            Box(
                Modifier
                    .size(36.dp)
                    .background(preset?.let { Color(it) } ?: MaterialTheme.colorScheme.primary, CircleShape)
                    .let {
                        if (isSelected) it.border(3.dp, MaterialTheme.colorScheme.outline, CircleShape) else it
                    }
                    .clickable { onSelect(preset) }
                    .semantics {
                        contentDescription = name
                        this.selected = isSelected
                    },
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
