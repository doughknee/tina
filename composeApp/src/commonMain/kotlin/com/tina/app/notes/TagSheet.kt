package com.tina.app.notes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.tina.app.resources.Res
import com.tina.app.resources.tag_create
import com.tina.app.resources.tag_new
import org.jetbrains.compose.resources.stringResource

/** "#Kitchen Stuff" and "kitchen-stuff" are the same tag. */
fun normalizeTag(raw: String): String =
    raw.trim().removePrefix("#").lowercase().replace(Regex("\\s+"), "-")

/**
 * Label sheet for a note or a selection: checkboxes apply instantly, no Save. Rows list every
 * tag in the app with its count; typing a new name adds a Create row.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagSheet(
    title: String,
    tags: List<TagCount>,
    checked: Set<String>,
    onToggle: (tag: String, add: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var newTag by remember { mutableStateOf("") }
    val candidate = normalizeTag(newTag)
    val known = tags.map { it.name }.toSet()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.imePadding().navigationBarsPadding()) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            LazyColumn(Modifier.heightIn(max = 360.dp)) {
                // tags this note carries that no other item has yet still need a row
                val rows = tags + checked.filter { it !in known }.map { TagCount(it, 0) }
                items(rows, key = { it.name }) { tag ->
                    val on = tag.name in checked
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .toggleable(value = on, role = Role.Checkbox, onValueChange = { onToggle(tag.name, it) })
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = on, onCheckedChange = null)
                        Text("#${tag.name}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f).padding(start = 8.dp))
                        if (tag.count > 0) {
                            Text(tag.count.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (candidate.isNotBlank() && candidate !in known && candidate !in checked) {
                    item("create") {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onToggle(candidate, true)
                                    newTag = ""
                                }
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.Add, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(
                                stringResource(Res.string.tag_create, candidate),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 20.dp),
                            )
                        }
                    }
                }
            }
            OutlinedTextField(
                value = newTag,
                onValueChange = { newTag = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(Res.string.tag_new)) },
                singleLine = true,
                prefix = { Text("#") },
            )
            Spacer(Modifier.size(16.dp))
        }
    }
}
