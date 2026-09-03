package com.tina.app.notes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tina.app.data.Item
import com.tina.app.resources.Res
import com.tina.app.resources.date_today
import com.tina.app.resources.date_yesterday
import com.tina.app.resources.months_short
import com.tina.app.resources.note_untitled
import com.tina.app.resources.notes_more_items
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

/**
 * "2d" under a week, "Aug 12" beyond, nothing inside the last hour: recency is not news.
 */
@Composable
fun noteDateLabel(millis: Long, nowMillis: Long, today: LocalDate): String? {
    if (nowMillis - millis < 60 * 60 * 1000L) return null
    val date = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault()).date
    val days = date.daysUntil(today)
    return when {
        days <= 0 -> stringResource(Res.string.date_today)
        days == 1 -> stringResource(Res.string.date_yesterday)
        days < 7 -> "${days}d"
        else -> {
            val month = stringArrayResource(Res.array.months_short)[date.month.number - 1]
            if (date.year == today.year) "$month ${date.day}" else "$month ${date.day}, ${date.year}"
        }
    }
}

/** Tag chips (max two, then "+n") and the date, only when either earns the space. */
@Composable
fun NoteMeta(item: Item, nowMillis: Long, today: LocalDate, modifier: Modifier = Modifier) {
    val date = noteDateLabel(item.updatedAt, nowMillis, today)
    if (item.tags.isEmpty() && date == null) return
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        item.tags.take(2).forEach { tag ->
            Text(
                "#$tag",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(5.dp))
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            )
        }
        if (item.tags.size > 2) {
            Text("+${item.tags.size - 2}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (date != null) {
            Spacer(Modifier.weight(1f))
            Text(date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

/**
 * One card, three shapes: titled, scrap (prose with no title row) and list. Uncoloured cards are
 * surface with a hairline; coloured ones drop the border. The pin shows only when pinned.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    item: Item,
    nowMillis: Long,
    today: LocalDate,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    showPin: Boolean = true,
) {
    val preview = remember(item) { previewOf(item) }
    val colored = item.color != null
    val border = when {
        selected -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        colored -> null
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }
    val untitled = stringResource(Res.string.note_untitled)
    val description = remember(preview) {
        listOf(preview.title, preview.text, preview.items.joinToString(", ")).filter { it.isNotBlank() }.joinToString(". ").ifBlank { untitled }
    }
    val interaction = if (selectionMode) {
        Modifier.toggleable(value = selected, role = Role.Checkbox, onValueChange = { onClick() })
    } else {
        Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
    }
    Box(modifier) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = item.color?.let { Color(it).copy(alpha = 0.32f) } ?: MaterialTheme.colorScheme.surface,
            border = border,
            modifier = Modifier
                .fillMaxWidth()
                .then(interaction)
                .semantics(mergeDescendants = true) { contentDescription = description },
        ) {
            Column(Modifier.padding(14.dp)) {
                if (showPin && item.pinned) {
                    Icon(
                        Icons.Filled.PushPin,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp).align(Alignment.End),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                when (preview.shape) {
                    NoteShape.SCRAP -> Text(
                        preview.text,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                    )
                    NoteShape.TITLED -> {
                        if (preview.title.isNotBlank() || preview.text.isBlank()) {
                            Text(
                                preview.title.ifBlank { untitled },
                                style = MaterialTheme.typography.titleMedium,
                                color = if (preview.title.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (preview.text.isNotBlank()) {
                            Text(
                                preview.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = if (preview.title.isNotBlank()) 4.dp else 0.dp),
                            )
                        }
                    }
                    NoteShape.LIST -> {
                        if (preview.title.isNotBlank()) {
                            Text(preview.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            preview.items.forEach { line ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier.padding(end = 8.dp).size(6.dp)
                                            .background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape),
                                    )
                                    Text(
                                        line,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                        if (preview.moreItems > 0) {
                            Text(
                                stringResource(Res.string.notes_more_items, preview.moreItems),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                }
                NoteMeta(item, nowMillis, today, Modifier.fillMaxWidth().padding(top = 8.dp))
            }
        }
        if (selected) {
            Box(
                Modifier.padding(6.dp).size(22.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

/** A list-layout row: pin, title + one-line preview, date. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteRow(
    item: Item,
    nowMillis: Long,
    today: LocalDate,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    selectionMode: Boolean = false,
) {
    val preview = remember(item) { previewOf(item) }
    val untitled = stringResource(Res.string.note_untitled)
    val interaction = if (selectionMode) {
        Modifier.toggleable(value = selected, role = Role.Checkbox, onValueChange = { onClick() })
    } else {
        Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
    }
    Row(
        modifier
            .fillMaxWidth()
            .then(interaction)
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.width(16.dp).padding(top = 3.dp)) {
            if (selected) {
                Icon(Icons.Outlined.Check, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            } else if (item.pinned) {
                Icon(Icons.Filled.PushPin, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        Column(Modifier.weight(1f)) {
            val headline = preview.title.ifBlank { preview.text }.ifBlank { untitled }
            Text(
                headline,
                style = if (preview.shape == NoteShape.SCRAP) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleMedium,
                maxLines = if (preview.shape == NoteShape.SCRAP) 2 else 1,
                overflow = TextOverflow.Ellipsis,
            )
            val sub = when (preview.shape) {
                NoteShape.LIST -> preview.items.joinToString(" · ")
                NoteShape.TITLED -> preview.text
                NoteShape.SCRAP -> ""
            }
            if (sub.isNotBlank()) {
                Text(
                    sub,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        noteDateLabel(item.updatedAt, nowMillis, today)?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp))
        }
    }
}
