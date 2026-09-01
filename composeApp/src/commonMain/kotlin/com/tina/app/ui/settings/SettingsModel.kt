package com.tina.app.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The settings page is data, not layout: sections and rows are built once in
 * [rememberSettingsSections] and rendered generically. That is what makes search,
 * platform filtering and scroll-to-row possible without duplicating every row.
 *
 * Titles are resolved Strings rather than StringResources so search can match on
 * them directly — they still come from string resources, just resolved at build time.
 */
data class SettingsSection(
    val id: String,
    val title: String,
    val visible: Boolean = true,
    val rows: List<SettingsRow>,
) {
    val visibleRows: List<SettingsRow> get() = rows.filter { it.visible }
}

sealed interface SettingsRow {
    val id: String
    val title: String
    val supporting: String?

    /** Extra search terms that aren't in the title (e.g. "dark", "night" for Theme). */
    val keywords: List<String>
    val visible: Boolean

    data class Switch(
        override val id: String,
        override val title: String,
        override val supporting: String? = null,
        override val keywords: List<String> = emptyList(),
        override val visible: Boolean = true,
        val checked: Boolean,
        val onCheckedChange: (Boolean) -> Unit,
    ) : SettingsRow

    /** Chevron row leading to a subpage. */
    data class Navigation(
        override val id: String,
        override val title: String,
        override val supporting: String? = null,
        override val keywords: List<String> = emptyList(),
        override val visible: Boolean = true,
        val badge: String? = null,
        val onClick: () -> Unit,
    ) : SettingsRow

    /** Leaves the app (system settings, a web page). */
    data class External(
        override val id: String,
        override val title: String,
        override val supporting: String? = null,
        override val keywords: List<String> = emptyList(),
        override val visible: Boolean = true,
        val onClick: () -> Unit,
    ) : SettingsRow

    /** Read-only fact, e.g. the version string. */
    data class Value(
        override val id: String,
        override val title: String,
        override val supporting: String? = null,
        override val keywords: List<String> = emptyList(),
        override val visible: Boolean = true,
    ) : SettingsRow

    /**
     * Segmented single choice. Named for the expressive ButtonGroup it will use once
     * material3 ships one; renders as SingleChoiceSegmentedButtonRow today.
     */
    data class ButtonGroupRow(
        override val id: String,
        override val title: String,
        override val supporting: String? = null,
        override val keywords: List<String> = emptyList(),
        override val visible: Boolean = true,
        val options: List<Option>,
        val selectedIndex: Int,
        val onSelect: (Int) -> Unit,
    ) : SettingsRow {
        data class Option(val label: String, val icon: ImageVector? = null)
    }

    /** Horizontally scrolling single choice; never wraps to a second line. */
    data class ChipRailRow(
        override val id: String,
        override val title: String,
        override val supporting: String? = null,
        override val keywords: List<String> = emptyList(),
        override val visible: Boolean = true,
        val options: List<String>,
        val selectedIndex: Int,
        val onSelect: (Int) -> Unit,
    ) : SettingsRow

    /** Tapping opens a time picker — a picker, not a confirmation. */
    data class TimeRow(
        override val id: String,
        override val title: String,
        override val supporting: String? = null,
        override val keywords: List<String> = emptyList(),
        override val visible: Boolean = true,
        val timeLabel: String,
        val onClick: () -> Unit,
    ) : SettingsRow

    /** Instant + undoable action wearing error colors. */
    data class Destructive(
        override val id: String,
        override val title: String,
        override val supporting: String? = null,
        override val keywords: List<String> = emptyList(),
        override val visible: Boolean = true,
        val actionLabel: String,
        val onAction: () -> Unit,
    ) : SettingsRow

    /** Escape hatch for one-off content (the AI collapse, hold-to-delete). */
    data class Custom(
        override val id: String,
        override val title: String,
        override val supporting: String? = null,
        override val keywords: List<String> = emptyList(),
        override val visible: Boolean = true,
        /** When false the row draws its own surface and sits outside the group card. */
        val inGroup: Boolean = true,
        val content: @Composable () -> Unit,
    ) : SettingsRow
}

/** Flattened (section, row) pairs for search; only visible rows are indexed. */
fun List<SettingsSection>.searchable(): List<Pair<SettingsSection, SettingsRow>> =
    filter { it.visible }.flatMap { section -> section.visibleRows.map { section to it } }

/** Case-insensitive match over title + supporting + keywords. */
fun SettingsRow.matches(query: String): Boolean {
    val q = query.trim()
    if (q.isEmpty()) return false
    return title.contains(q, ignoreCase = true) ||
        supporting?.contains(q, ignoreCase = true) == true ||
        keywords.any { it.contains(q, ignoreCase = true) }
}
