package com.tina.app.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.tina.app.resources.Res
import com.tina.app.resources.tag_create
import com.tina.app.resources.tag_new
import org.jetbrains.compose.resources.stringResource

/** "#Kitchen Stuff" and "kitchen-stuff" are the same tag. */
fun normalizeTag(raw: String): String =
    raw.trim().removePrefix("#").lowercase().replace(Regex("\\s+"), "-")

/**
 * Tags as chips inline, never a sheet: every tag in the app as a toggle that applies instantly,
 * then a field that creates a new one (Enter, or the Create chip that appears while typing).
 * Used at the end of the note editor and above the Ideas grid for a selection.
 */
@Composable
fun TagPicker(
    tags: List<TagCount>,
    checked: Set<String>,
    onToggle: (tag: String, add: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var newTag by remember { mutableStateOf("") }
    val candidate = normalizeTag(newTag)
    val known = tags.map { it.name }.toSet()
    // tags this note carries that no other item has yet still need a chip
    val all = tags + checked.filter { it !in known }.map { TagCount(it, 0) }
    val canCreate = candidate.isNotBlank() && candidate !in known && candidate !in checked
    fun create() {
        if (!canCreate) return
        onToggle(candidate, true)
        newTag = ""
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            all.forEach { tag ->
                val on = tag.name in checked
                FilterChip(selected = on, onClick = { onToggle(tag.name, !on) }, label = { Text("#${tag.name}") })
            }
            if (canCreate) {
                AssistChip(
                    onClick = ::create,
                    leadingIcon = { Icon(Icons.Outlined.Add, null, Modifier.size(18.dp)) },
                    label = { Text(stringResource(Res.string.tag_create, candidate)) },
                )
            }
        }
        OutlinedTextField(
            value = newTag,
            onValueChange = { newTag = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(Res.string.tag_new)) },
            singleLine = true,
            prefix = { Text("#") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { create() }),
        )
    }
}
