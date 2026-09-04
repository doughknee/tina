package com.tina.app.notes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.FormatStrikethrough
import androidx.compose.material.icons.outlined.FormatUnderlined
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
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
import com.tina.app.resources.event_color
import com.tina.app.resources.fmt_bold
import com.tina.app.resources.fmt_checklist
import com.tina.app.resources.fmt_bullets
import com.tina.app.resources.fmt_heading
import com.tina.app.resources.fmt_hint
import com.tina.app.resources.fmt_italic
import com.tina.app.resources.fmt_numbered
import com.tina.app.resources.fmt_strike
import com.tina.app.resources.fmt_subheading
import com.tina.app.resources.fmt_underline
import com.tina.app.resources.note_add_tag
import com.tina.app.resources.note_share
import com.tina.app.resources.note_copy_markdown
import com.tina.app.resources.note_duplicate
import com.tina.app.resources.note_edited
import com.tina.app.resources.note_more
import com.tina.app.resources.note_pin
import com.tina.app.resources.note_start_writing
import com.tina.app.resources.note_to_task
import com.tina.app.resources.note_unpin
import com.tina.app.resources.note_untitled
import com.tina.app.resources.note_words
import com.tina.app.resources.state_off
import com.tina.app.resources.state_on
import com.tina.app.resources.tag_sheet_one
import com.tina.app.ui.ColorSwatchRow
import com.tina.app.ui.relativeAge
import kotlin.time.Clock
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val HEADING = SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
private val SUBHEADING = SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)

/** Paper, not a form: a wrapping title, one quiet meta line, a body with room, tools only while typing. */
@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun NoteEditorScreen(
    noteId: Long,
    onBack: () -> Unit,
    onOpenNote: (Long) -> Unit = {},
    onOpenTag: (String) -> Unit = {},
    viewModel: NoteEditorViewModel = koinViewModel(key = "note-$noteId") { parametersOf(noteId) },
    notesViewModel: NotesViewModel = koinViewModel(),
) {
    val item by viewModel.item.collectAsState()
    val allTags by notesViewModel.allTags.collectAsState()
    val richTextState = rememberRichTextState()
    var titleText by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }
    var showColors by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showTags by remember { mutableStateOf(false) }
    val bodyInteraction = remember { MutableInteractionSource() }
    val actions = com.tina.app.ui.settings.rememberPlatformActions()
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val untitledText = stringResource(Res.string.note_untitled)
    val bodyFocused by bodyInteraction.collectIsFocusedAsState()

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

    // markdown input rules: "- " bullet, "1. " numbered, "# " heading; zero UI, what makes it feel fast
    LaunchedEffect(loaded) {
        if (!loaded) return@LaunchedEffect
        snapshotFlow { richTextState.annotatedString.text to richTextState.selection }
            .collect { (text, selection) ->
                if (!selection.collapsed) return@collect
                val cursor = selection.end
                if (cursor > text.length) return@collect
                // inside a list the paragraph text carries its bullet ("• ", "1. ") as a prefix
                val lineStart = richTextState.contentStart()
                val line = text.substring(lineStart, cursor)
                val rule: (() -> Unit)? = when (line) {
                    "- ", "* " -> if (richTextState.isUnorderedList) null else ({ richTextState.toggleUnorderedList() })
                    "1. " -> if (richTextState.isOrderedList) null else ({ richTextState.toggleOrderedList() })
                    "# " -> ({ richTextState.toggleSpanStyle(HEADING) })
                    "## " -> ({ richTextState.toggleSpanStyle(SUBHEADING) })
                    // a checklist item is a bullet whose text starts with a box; the card reads the box
                    "[] ", "[ ] " -> ({ richTextState.startChecklistItem(UNCHECKED) })
                    "[x] ", "[X] " -> ({ richTextState.startChecklistItem(CHECKED) })
                    else -> null
                }
                if (rule != null) {
                    richTextState.removeTextRange(TextRange(lineStart, cursor))
                    rule()
                }
            }
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
                    val pinned = item?.pinned == true
                    IconButton(onClick = viewModel::togglePin) {
                        Icon(
                            if (pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            stringResource(if (pinned) Res.string.note_unpin else Res.string.note_pin),
                            tint = if (pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { showColors = !showColors }) {
                        Icon(
                            Icons.Outlined.Palette,
                            stringResource(Res.string.event_color),
                            tint = if (showColors) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Outlined.MoreVert, stringResource(Res.string.note_more))
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            if (actions.supportsShare) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.note_share)) },
                                    leadingIcon = { Icon(Icons.Outlined.Share, null) },
                                    onClick = {
                                        showMenu = false
                                        actions.share(titleText.ifBlank { untitledText }, noteMarkdown(titleText, richTextState))
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.note_copy_markdown)) },
                                leadingIcon = { Icon(Icons.Outlined.ContentPaste, null) },
                                onClick = {
                                    showMenu = false
                                    clipboard.setText(AnnotatedString(noteMarkdown(titleText, richTextState)))
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.note_duplicate)) },
                                leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) },
                                onClick = {
                                    showMenu = false
                                    viewModel.duplicate(onOpenNote)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.note_to_task)) },
                                leadingIcon = { Icon(Icons.Outlined.TaskAlt, null) },
                                onClick = {
                                    showMenu = false
                                    viewModel.convertToTask(onBack)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.delete)) },
                                leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                                onClick = {
                                    showMenu = false
                                    item?.let { notesViewModel.delete(it) }
                                    onBack()
                                },
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            // only while the body has focus: the tools belong to typing, not to reading
            AnimatedVisibility(
                visible = bodyFocused,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                Surface(tonalElevation = 3.dp) {
                    Column(Modifier.imePadding()) {
                        FormatToolbar(
                            state = richTextState,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                        Text(
                            stringResource(Res.string.fmt_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            val titleStyle = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.onSurface)
            BasicTextField(
                value = titleText,
                onValueChange = { titleText = it },
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp),
                textStyle = titleStyle,
                maxLines = 3,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    Box {
                        if (titleText.isEmpty()) {
                            Text(
                                stringResource(Res.string.note_untitled),
                                style = titleStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        inner()
                    }
                },
            )

            item?.let { current ->
                val words = richTextState.annotatedString.text.split(Regex("\\s+")).count { it.isNotBlank() }
                val age = relativeAge(Clock.System.now().toEpochMilliseconds() - current.updatedAt)
                Text(
                    "${stringResource(Res.string.note_edited, age)} · ${stringResource(Res.string.note_words, words)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp),
                )
            }

            AnimatedVisibility(
                visible = showColors,
                enter = com.tina.app.ui.expandEnter(),
                exit = com.tina.app.ui.expandExit(),
            ) {
                Row(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    ColorSwatchRow(selected = item?.color, onSelect = viewModel::setColor)
                }
            }

            RichTextEditor(
                state = richTextState,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                placeholder = { Text(stringResource(Res.string.note_start_writing), style = MaterialTheme.typography.bodyLarge) },
                minLines = 6,
                interactionSource = bodyInteraction,
                colors = RichTextEditorDefaults.richTextEditorColors(
                    containerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            )

            // tags sit at the end of the body flow; the dashed chip adds one
            FlowRow(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item?.tags.orEmpty().forEach { tag ->
                    AssistChip(onClick = { onOpenTag(tag) }, label = { Text("#$tag") })
                }
                AssistChip(
                    onClick = { showTags = true },
                    label = { Text(stringResource(Res.string.note_add_tag)) },
                    colors = AssistChipDefaults.assistChipColors(labelColor = MaterialTheme.colorScheme.onSurfaceVariant),
                )
            }
        }
    }

    if (showTags) {
        TagSheet(
            title = stringResource(Res.string.tag_sheet_one),
            tags = allTags,
            checked = item?.tags.orEmpty().toSet(),
            onToggle = viewModel::setTag,
            onDismiss = { showTags = false },
        )
    }
}

/** Three clusters split by hairlines: B I U S · H1 H2 · bullet number. */
@Composable
private fun FormatToolbar(state: RichTextState, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        FormatButton(
            active = state.currentSpanStyle.fontWeight == FontWeight.Bold &&
                state.currentSpanStyle.fontSize != HEADING.fontSize &&
                state.currentSpanStyle.fontSize != SUBHEADING.fontSize,
            onClick = { state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) },
            icon = { Icon(Icons.Outlined.FormatBold, stringResource(Res.string.fmt_bold)) },
        )
        FormatButton(
            active = state.currentSpanStyle.fontStyle == FontStyle.Italic,
            onClick = { state.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) },
            icon = { Icon(Icons.Outlined.FormatItalic, stringResource(Res.string.fmt_italic)) },
        )
        FormatButton(
            active = state.currentSpanStyle.textDecoration?.contains(TextDecoration.Underline) == true,
            onClick = { state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline)) },
            icon = { Icon(Icons.Outlined.FormatUnderlined, stringResource(Res.string.fmt_underline)) },
        )
        FormatButton(
            active = state.currentSpanStyle.textDecoration?.contains(TextDecoration.LineThrough) == true,
            onClick = { state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) },
            icon = { Icon(Icons.Outlined.FormatStrikethrough, stringResource(Res.string.fmt_strike)) },
        )
        ToolbarDivider()
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
                    modifier = Modifier.padding(3.dp),
                )
            },
        )
        ToolbarDivider()
        FormatButton(
            active = state.isUnorderedList && state.currentLineMarker() == null,
            onClick = { state.toggleUnorderedList() },
            icon = { Icon(Icons.AutoMirrored.Outlined.FormatListBulleted, stringResource(Res.string.fmt_bullets)) },
        )
        FormatButton(
            active = state.isOrderedList,
            onClick = { state.toggleOrderedList() },
            icon = { Icon(Icons.Outlined.FormatListNumbered, stringResource(Res.string.fmt_numbered)) },
        )
        FormatButton(
            active = state.currentLineMarker() != null,
            onClick = { state.cycleChecklistMarker() },
            icon = { Icon(Icons.Outlined.Checklist, stringResource(Res.string.fmt_checklist)) },
        )
    }
}

/** Start of the line the cursor is on, in the editor's plain text. */
private fun RichTextState.currentLineStart(): Int {
    // annotatedString joins paragraphs with a space; toText() uses newlines at the same indices
    val plain = toText()
    val cursor = selection.end.coerceIn(0, plain.length)
    return plain.lastIndexOf('\n', cursor - 1) + 1
}

/**
 * Where the user's own text starts on the cursor's line: past the bullet or number the list
 * paragraph prepends ("• ", "1. "), which the editor keeps in the plain text.
 */
private fun RichTextState.contentStart(): Int {
    val start = currentLineStart()
    if (!isList) return start
    val text = annotatedString.text
    val space = text.indexOf(' ', start)
    if (space <= start) return start
    val prefix = text.substring(start, space)
    val isMarker = prefix.all { !it.isLetter() && it != UNCHECKED && it != CHECKED }
    return if (isMarker) space + 1 else start
}

/** The box at the start of the cursor's line, if it is a checklist item. */
private fun RichTextState.currentLineMarker(): Char? {
    val text = annotatedString.text
    return text.getOrNull(contentStart())?.takeIf { it == UNCHECKED || it == CHECKED }
}

/** Make the cursor's line a checklist item: a bullet whose text starts with [marker]. */
private fun RichTextState.startChecklistItem(marker: Char) {
    if (!isUnorderedList) toggleUnorderedList()
    addTextAfterSelection("$marker ")
}

/** Toolbar: plain line → unticked box → ticked box → plain line. */
private fun RichTextState.cycleChecklistMarker() {
    val start = contentStart()
    when (currentLineMarker()) {
        null -> {
            if (!isUnorderedList) toggleUnorderedList()
            addTextAtIndex(contentStart(), "$UNCHECKED ")
        }
        UNCHECKED -> {
            removeTextRange(TextRange(start, start + 1))
            addTextAtIndex(start, CHECKED.toString())
        }
        else -> {
            val end = if (annotatedString.text.getOrNull(start + 1) == ' ') start + 2 else start + 1
            removeTextRange(TextRange(start, end))
        }
    }
}

@Composable
private fun ToolbarDivider() {
    VerticalDivider(
        Modifier.padding(horizontal = 4.dp).height(24.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun FormatButton(active: Boolean, onClick: () -> Unit, icon: @Composable () -> Unit) {
    val stateText = stringResource(if (active) Res.string.state_on else Res.string.state_off)
    IconToggleButton(
        checked = active,
        onCheckedChange = { onClick() },
        modifier = Modifier.size(40.dp).semantics { stateDescription = stateText },
        colors = IconButtonDefaults.iconToggleButtonColors(
            checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        content = icon,
    )
}


/** Title as a heading, body from the editor, checklist markers as task-list syntax. */
private fun noteMarkdown(title: String, state: RichTextState): String {
    val body = state.toMarkdown()
        .replace("- $UNCHECKED ", "- [ ] ")
        .replace("- $CHECKED ", "- [x] ")
        .replace("* $UNCHECKED ", "- [ ] ")
        .replace("* $CHECKED ", "- [x] ")
    return buildString {
        if (title.isNotBlank()) append("# ").append(title.trim()).append("\n\n")
        append(body.trim())
    }
}
