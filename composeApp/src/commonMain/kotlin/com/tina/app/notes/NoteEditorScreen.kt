package com.tina.app.notes

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.FormatUnderlined
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults
import com.tina.app.resources.Res
import com.tina.app.resources.back
import com.tina.app.resources.delete
import com.tina.app.resources.fmt_bold
import com.tina.app.resources.fmt_bullets
import com.tina.app.resources.fmt_heading
import com.tina.app.resources.fmt_italic
import com.tina.app.resources.fmt_numbered
import com.tina.app.resources.fmt_subheading
import com.tina.app.resources.fmt_underline
import com.tina.app.resources.note_pin
import com.tina.app.resources.note_unpin
import com.tina.app.resources.note_untitled
import com.tina.app.ui.ColorSwatchRow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val HEADING = SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
private val SUBHEADING = SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun NoteEditorScreen(
    noteId: Long,
    onBack: () -> Unit,
    viewModel: NoteEditorViewModel = koinViewModel(key = "note-$noteId") { parametersOf(noteId) },
    notesViewModel: NotesViewModel = koinViewModel(),
) {
    val item by viewModel.item.collectAsState()
    val richTextState = rememberRichTextState()
    var titleText by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }

    // Load once; afterwards the editor is the source of truth and autosaves.
    LaunchedEffect(item?.id) {
        val current = item ?: return@LaunchedEffect
        if (!loaded) {
            titleText = current.title
            richTextState.setHtml(current.body ?: "")
            loaded = true
        }
    }

    LaunchedEffect(loaded) {
        if (!loaded) return@LaunchedEffect
        snapshotFlow { richTextState.annotatedString }
            .drop(1)
            .debounce(500)
            .collect { viewModel.saveBody(richTextState.toHtml()) }
    }

    LaunchedEffect(loaded) {
        if (!loaded) return@LaunchedEffect
        snapshotFlow { titleText }
            .drop(1)
            .debounce(400)
            .collect { viewModel.saveTitle(it.trim()) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(Res.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::togglePin) {
                        Icon(
                            Icons.Outlined.PushPin,
                            stringResource(if (item?.pinned == true) Res.string.note_unpin else Res.string.note_pin),
                            tint = if (item?.pinned == true) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    IconButton(onClick = {
                        item?.let { notesViewModel.delete(it) }
                        onBack()
                    }) {
                        Icon(Icons.Outlined.Delete, stringResource(Res.string.delete))
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                FormatToolbar(
                    state = richTextState,
                    modifier = Modifier.fillMaxWidth().imePadding().padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TextField(
                value = titleText,
                onValueChange = { titleText = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.headlineSmall,
                placeholder = {
                    Text(stringResource(Res.string.note_untitled), style = MaterialTheme.typography.headlineSmall)
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                singleLine = true,
            )

            Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                ColorSwatchRow(selected = item?.color, onSelect = viewModel::setColor)
            }

            RichTextEditor(
                state = richTextState,
                modifier = Modifier.fillMaxSize(),
                colors = RichTextEditorDefaults.richTextEditorColors(
                    containerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            )
        }
    }
}

@Composable
private fun FormatToolbar(state: RichTextState, modifier: Modifier = Modifier) {
    Row(modifier.horizontalScroll(rememberScrollState())) {
        FormatButton(
            active = state.currentSpanStyle.fontWeight == FontWeight.Bold &&
                state.currentSpanStyle.fontSize != HEADING.fontSize &&
                state.currentSpanStyle.fontSize != SUBHEADING.fontSize,
            onClick = { state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) },
            icon = { Icon(Icons.Outlined.FormatBold, stringResource(Res.string.fmt_bold)) },
        )
        FormatButton(
            active = state.currentSpanStyle.fontStyle == androidx.compose.ui.text.font.FontStyle.Italic,
            onClick = {
                state.toggleSpanStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
            },
            icon = { Icon(Icons.Outlined.FormatItalic, stringResource(Res.string.fmt_italic)) },
        )
        FormatButton(
            active = state.currentSpanStyle.textDecoration?.contains(TextDecoration.Underline) == true,
            onClick = { state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline)) },
            icon = { Icon(Icons.Outlined.FormatUnderlined, stringResource(Res.string.fmt_underline)) },
        )
        FormatButton(
            active = state.currentSpanStyle.fontSize == HEADING.fontSize,
            onClick = { state.toggleSpanStyle(HEADING) },
            icon = { Icon(Icons.Outlined.Title, stringResource(Res.string.fmt_heading)) },
        )
        FormatButton(
            active = state.currentSpanStyle.fontSize == SUBHEADING.fontSize,
            onClick = { state.toggleSpanStyle(SUBHEADING) },
            icon = {
                Icon(
                    Icons.Outlined.Title,
                    stringResource(Res.string.fmt_subheading),
                    modifier = Modifier.padding(2.dp),
                )
            },
        )
        FormatButton(
            active = state.isUnorderedList,
            onClick = { state.toggleUnorderedList() },
            icon = { Icon(Icons.AutoMirrored.Outlined.FormatListBulleted, stringResource(Res.string.fmt_bullets)) },
        )
        FormatButton(
            active = state.isOrderedList,
            onClick = { state.toggleOrderedList() },
            icon = { Icon(Icons.Outlined.FormatListNumbered, stringResource(Res.string.fmt_numbered)) },
        )
    }
}

@Composable
private fun FormatButton(active: Boolean, onClick: () -> Unit, icon: @Composable () -> Unit) {
    FilledIconToggleButton(checked = active, onCheckedChange = { onClick() }, content = icon)
}
