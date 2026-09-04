package com.tina.app.ui.settings

/**
 * One entry per feature release, newest first: the version's major.minor and one paragraph.
 * The Settings subpage lists all of them; the upgrade sheet shows the first one whose version
 * matches the running build. Keep the first entry's version equal to the build's major.minor.
 */
val WHATS_NEW: List<Pair<String, String>> = listOf(
    "1.8" to
        "Ideas rebuilt: cards that read like notes, checklists you can tick from the card, a pinned " +
            "section, a tag rail, long-press selection, sort and layout, a paper-like editor, and a tag " +
            "page that gathers notes, tasks and events. Sort became the decisions page. Capture " +
            "understands far more phrasings. Widgets, quiet hours, and undo everywhere.",
    "1.7" to
        "Repeating reminders ring every time, backups carry everything, your AI key is " +
            "encrypted, Ask asks before big changes, Peggy Pro groundwork, and a real icon.",
    "1.6" to
        "Swipe triage on Sort, the settings hub, series editing, empty states and a " +
            "quick-settings tile for ideas.",
    "1.5" to
        "Pages named for what you do there, Idea mode for notes, one calendar for every " +
            "zoom level, search as a sheet, and a smoother keyboard.",
    "1.4" to
        "Agenda, Library and Ask; capture from anywhere; Day / Week / Month / All with " +
            "repeats rolled up and completed per day.",
    "1.3.1" to
        "The keyboard no longer opens with the app, the composer moved to the " +
            "bottom, and pages slide instead of zooming.",
    "1.3" to
        "Grouped and searchable settings, Trash with restore, tag manager, " +
            "daily summaries, app lock, auto-backup.",
    "1.2" to "Chat with your data, optional write access, saved conversations, browsable tags.",
    "1.1" to "Ollama / Claude / OpenAI refinement, AI improve, and the Material 3 redesign.",
    "1.0" to "Capture, Today, Calendar, Notes, reminders, widgets, backup.",
)

/** "1.8.2-dev" → "1.8": the feature release a build belongs to. */
fun featureVersion(versionName: String): String =
    versionName.substringBefore('-').split('.').take(2).joinToString(".")
