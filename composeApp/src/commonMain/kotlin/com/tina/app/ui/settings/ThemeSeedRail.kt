package com.tina.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tina.app.data.ThemeSeed

/**
 * One circle per theme seed, the chosen one ticked. Everything but the brand seed is Pro, so
 * a locked swatch shows a padlock and [onLocked] opens the paywall instead of picking it.
 */
@Composable
fun ThemeSeedRail(
    title: String,
    selected: ThemeSeed,
    isPro: Boolean,
    labels: Map<ThemeSeed, String>,
    onPick: (ThemeSeed) -> Unit,
    onLocked: () -> Unit,
) {
    Column(Modifier.padding(top = 12.dp, bottom = 4.dp)) {
        // a Custom row draws its own header, like the AI config block does
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                labels[selected] ?: selected.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ThemeSeed.entries.forEach { seed ->
            val locked = seed != ThemeSeed.PEGGY && !isPro
            val chosen = seed == selected
            val label = labels[seed] ?: seed.name
            Box(
                Modifier
                    .size(48.dp)
                    .semantics { contentDescription = if (locked) "$label, Peggy Pro" else label }
                    .selectable(selected = chosen, role = Role.RadioButton) { if (locked) onLocked() else onPick(seed) }
                    .padding(4.dp)
                    .background(Color(seed.argb), CircleShape)
                    .border(if (chosen) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    chosen -> Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White)
                    locked -> Icon(Icons.Outlined.Lock, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
    }
}
