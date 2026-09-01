# Decisions

Running log of choices made without asking, newest last.

## Phase 1 — data model

- **One `items` table** with nullable type-specific columns, as specced. Changing type is a single-column update; no data is lost because every field exists on every row.
- **Timestamps vs wall-clock**: `createdAt`, `updatedAt`, `completedAt`, `startAt`, `endAt` are UTC epoch millis. `dueDate` is stored as **epoch day** (days since 1970-01-01) and `dueTime` as **minute of day** because a due date is a wall-clock concept — a task due "Jan 5" stays due Jan 5 if the timezone changes. Conversion happens at the edge with kotlinx-datetime.
- **Columns renamed** `start`/`end` → `startAt`/`endAt` to avoid SQL keyword quoting in hand-written queries.
- **Tags** stored as a comma-separated lowercase string via a TypeConverter. Good enough for one user; a join table is overkill.
- **DB version 2 with destructive migration** from the scaffold's placeholder schema. The only installed copy has an empty database; nothing to migrate.
- **Parser priority markers**: `!` = medium, `!!` = high (two markers map onto the top two of the four levels; low is only settable in editors).
- **Parser type rules**, in order: ≥3 sentences or >200 chars → NOTE (prose beats an embedded time word); recurrence keyword → EVENT (all-day if no time); time signal → EVENT; date only → TASK; nothing → TASK with no date.
- **"Ambiguous stays INBOX" is defined as**: two or more date tokens that resolve differently ("today … tomorrow"), or a capture whose title is empty after stripping tokens ("3pm"). In both cases the original text is kept as the title and no fields are set.
- **Recurrence lives on EVENT only** (per spec field list). "every day" therefore produces a recurring all-day EVENT, not a recurring task.
- **Extra parser niceties** beyond spec, because they were nearly free: `tonight` (today 20:00), `tmrw`, `next <weekday>` = the occurrence after the next one, weekday recurrence ("every friday"), ordinal dates ("jan 5th").
- **Numeric dates are M/D** (US order), matching the spec's "1/5 = jan 5".
- **Bare weekday** ("monday") = next occurrence, never today, 1–7 days out. "next monday" = that + 7.
- **RRULE support is a subset**: FREQ (DAILY/WEEKLY/MONTHLY/YEARLY), INTERVAL, BYDAY (weekly), COUNT, UNTIL. Monthly on the 31st skips short months; Feb 29 yearly skips non-leap years. Expansion capped at ~3700 iterations.
- **kotlinx-datetime 0.8.0**; `Instant`/`Clock` come from `kotlin.time` (stdlib) as of 0.7+. `toEpochDays()` returns Long there; converted to Int at the storage edge.
- **Accepted build warnings** (structural, not fixable while the spec wants a single KMP module that is also the Android app on AGP 9): the deprecation notices for `android.builtInKotlin=false` / `android.newDsl=false`, the old `android {}` DSL, and KMP↔`com.android.application` compatibility. The migration path AGP wants is a separate `androidApp` module; revisit when AGP 10 forces it. The "SDK XML versions up to 3" warning is an AGP-vs-cmdline-tools metadata version gap, harmless.
- **Compose plugin DSL accessors** (`compose.runtime` etc.) are deprecated in CMP 1.12 — replaced with explicit catalog coordinates; material3 is independently versioned (1.9.0).

## Phase 2 — capture

- **material3 is force-pinned to 1.9.0**: richeditor 1.1.0 declares material3 1.11.0-alpha07, whose Android artifact (1.5.0-alpha17, built against foundation 1.11.0-beta02) throws AbstractMethodError in TextFieldDefaults on foundation 1.12.0. If the rich editor turns out to need alpha material3 APIs in Phase 6, revisit.
- **Mic uses the system RecognizerIntent dialog** instead of a raw SpeechRecognizer: no RECORD_AUDIO permission, on-device preferred (EXTRA_PREFER_OFFLINE), and the system UI handles errors. Result text is appended to the field and flows through the same parser.
- **The home-screen widget cannot host a real text input** — RemoteViews has no EditText. It renders as a search-bar-shaped tap target that opens Capture with the keyboard up (same pattern as Google Keep's widget).
- **Share intent saves immediately** with no UI (translucent activity, parse → insert → toast → finish). Zero decisions; mis-parses are fixable later in Inbox.
- **Chip taps**: any parsed chip tap removes that attribute (one tap to fix); the type chip cycles Inbox → Task → Event → Note. Removing the time from an event demotes it to a task.
- **Event default when captured for a future date with no time**: 9:00. Same-day default stays "next round hour".
- **Time labels are 12-hour until Phase 3 settings** add the 12/24h preference.
- **material-icons-extended pinned at 1.7.3** — the artifact is frozen there (icons moved out of the M3 release train) but remains compatible; R8 strips unused icons on Android.

## Phase 4 — Today and Inbox

- **Row tap = inline title edit** (the 90% fix); the full detail screen sits behind a trailing chevron. Long-press stays reserved for drag.
- **Drag reorder applies where manual order exists**: Today's Anytime section. Timed sections are ordered by clock time and Inbox by capture time — dragging there would silently fight the sort.
- **Events are not completable** — no checkbox, no swipe-complete; they render with a color dot and support delete/reschedule/detail.
- **Snackbars with Undo appear for destructive or screen-leaving actions** (delete, inbox triage). Complete/uncomplete undo is the checkbox itself; reschedule undo is the same chip again — a snackbar there would be noise.
- **"This week" triage = end of the current ISO week** (Sunday). "Overdue rolls forward" is purely a query property (due ≤ today), no data rewriting.
- **Completed-today tasks stay visible** struck-through in Today (satisfying, and un-checkable), disappearing after midnight.
- **Shared element list↔detail transitions deferred to Phase 8 polish** — Navigation 3's CMP artifact needs its animated-scope wiring verified, and the phases are more useful shipped than blocked on a transition.

## Phase 5 — calendar

- **Calendar 2.10 multiplatform builds on kotlinx-datetime's own `YearMonth`** (no library YearMonth type); month math comes from the lib's extensions.
- **Long-press capture prefills a numeric date token ("9/15 ")** — parser-friendly and locale-free, so calendar capture rides the exact same parse-and-save path as typed capture.
- **Task rows in the agenda quick-reschedule; events don't** — moving an event means times, which is the editor's job.

## Phase 6 — notes

- **Rich text is stored as HTML** from the editor's own serializer; previews strip tags with a regex.
- **"Checklists" are bulleted/numbered lists** — the rich editor library has no interactive checkbox list; a bullet list is the honest nearest thing. Revisit if the library grows todo-list support.
- **Headings are font-size spans** (24sp/20sp bold) rather than semantic H1/H2 — same rendering, simpler toggle model.
- **Editor-initiated deletes surface their undo snackbar on the Notes list** via a shared activity-scoped NotesViewModel flag, so undo survives the back navigation.
- **Parameterized ViewModels get per-item Koin keys** (`detail-$id`, `event-$id`, `note-$id`) — without a key the activity-scoped store would hand every screen the first item's VM.
