# Decisions

## Ask chats (REL-145, Sep 2026)

- Schema v2 → v3 uses a **hand-written additive migration**, not destructive
  fallback: it only CREATEs the `chats` / `chat_messages` tables and their
  index. Items must survive app updates — verified on the emulator against a
  populated v2 database before the release went to the phone.
- Chats persist in Room (not DataStore): message lists are unbounded and want
  a real query/ordering story.
- Each chat remembers its own model override and reasoning level, so reopening
  an old conversation restores how it was being run.
- Titles auto-derive from the first message (first 48 chars) — no title-
  generation call, which would cost a round trip for cosmetics.
- Chat deletion is instant with undo (snapshot of the chat + its messages held
  in memory), matching the app's no-confirmation-dialogs rule.
- Ask context is rebuilt from the live database on every question even in an
  old chat, so answers never go stale against a resumed conversation.

## Ask page (REL-142, Sep 2026)

- Architecture: context stuffing, not MCP/tool-calling. The whole database is
  serialized compactly (one line per item, newest first, 80k-char cap with
  oldest dropped) into the system prompt per question. Rationale: the app owns
  the DB — a protocol layer to talk to itself adds nothing; works identically
  on Ollama/Claude/OpenAI including models with weak tool-calling. MCP remains
  the right shape later for exposing tina to EXTERNAL hosts (Claude Desktop).
- Reasoning level (Quick/Balanced/Thorough) is prompt-driven, not an API
  parameter — provider-agnostic and immune to API drift.
- Chat-scoped model picker enumerates models only for Anthropic (fixed list);
  other providers use the settings model (Ollama model listing via /api/tags is
  the obvious upgrade).
- Chat history is session-only (ViewModel memory), not persisted.
- Read-only by design: the system prompt says so; giving chat write access to
  the DB is a separate, deliberate decision for another day.

## Tags + settings backup (REL-140/141, Sep 2026)

- Backups now carry a `settings` block (nullable — v1.0 backups import fine).
  The AI API key is included in plaintext: a backup that cannot restore the AI
  config defeats its purpose for a single-user app; the file lives wherever
  the user saves it, same trust level as the device.
- Tag filtering happens in memory over `observeTagged()` (tags are one joined
  column; a LIKE query risks substring false positives, and a personal app's
  item count makes in-memory filtering free).
- Tag entry points: browse chips on the empty search screen, tappable chips on
  the detail screen. Rows deliberately do not render tags — the design pass
  kept supporting lines to time + priority.

## AI improve (REL-136, Sep 2026)

- Refinement modes: AUTO (apply silently, undo snackbar — original behavior),
  SUGGEST (compute in background, hold until the user opens the improve sheet),
  MANUAL (no background calls at all). Provider OFF still disables everything.
- Per-item "AI improve" sheet on the task detail and event editors: toggleable
  diff chips, at most 2 model-asked multiple-choice questions with a custom
  answer field, a second model round folds answers in, Apply with undo. No
  confirmation dialogs; the sheet itself is the decision surface.
- "Skill" = a free-text improvement-instructions setting injected into the
  improve prompt. Named presets can come later if it earns it.
- SUGGEST-mode results live in an in-memory cache (survive until process
  death); no per-row badge yet — the sheet just opens instantly when a cached
  suggestion exists. ponytail: badge needs per-row cache observation across
  five screens.
- Known quirk: text fields on detail/editor screens remember their local state
  per item id, so an improve applied while the screen is open shows stale
  title/tags text until reopened (chips and rows update live). Keying on
  updatedAt would reset the cursor mid-typing; not worth it.

## Design polish pass (REL-134, Sep 2026)

- Implemented the 12-item external design review (mockups in `design/redesign/`).
  Deviations, each deliberate:
  - No `AnimatedContent` slide on calendar month changes: kizitonwose
    `HorizontalCalendar` is already a pager with its own swipe/scroll animation;
    wrapping it would recreate the calendar per month and fight the pager.
  - Settings toggle rows kept as `Row` + `Modifier.toggleable(role = Switch)`
    rather than converting to `ListItem` — ListItem's built-in 16dp insets would
    double with the screen's existing padding; the accessibility outcome
    (full-row target, switch role announced) is identical.
  - Note-card pin stays interactive (48dp padded touch target around the 16dp
    glyph) — the review's static 16dp icon would have dropped one-tap pinning.
  - Selected-day pill only replaces the dots when the day actually has items.
  - Rows with a date chip lose the chevron (per review); their detail stays
    reachable via search/calendar and the chip's reschedule menu covers the
    common action.

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

## Phase 7 — reminders

- **Scheduling lives in the repository**: every mutation that can change a reminder (insert/update/complete/delete/reschedule/type change) re-arms or cancels through a `ReminderScheduler` expect-style interface; UI code never touches alarms. Desktop binds a no-op.
- **One alarm per item** (PendingIntent keyed by item id). Recurring events re-arm their next occurrence when they fire; snoozing replaces that arm until the snoozed alarm fires and re-arms it. Ceiling: snoozing past the next occurrence's reminder skips that reminder once.
- **Exact alarms degrade gracefully**: without SCHEDULE_EXACT_ALARM the same alarm is set inexact (≤1h window) rather than not at all; the Today banner offers both grants and re-checks on resume. Verified via dumpsys: window=1h before grant, window=0 after.
- **Tasks need a due time to ring** — a date-only task has no meaningful alarm moment.
- **Boot + cold-start both re-arm everything** (alarms don't survive reboot; cold-start covers app updates and force-stops).
- **Notification "Done" completes tasks without opening the app**; events get only snooze actions because "done" means nothing for an event.

## Phase 8 — polish

- **Import is additive, never destructive**: items come in with fresh ids and exact (title, createdAt) duplicates are skipped. No "replace everything" mode — the no-confirmation-dialogs rule makes silent replacement too dangerous, and additive import is idempotent.
- **Search is a LIKE query over title+body** (top 100, recency-ordered). FTS would be faster at a scale a single person's data never reaches.
- **The Today widget refreshes** on its own checkbox actions, on app-close, and on relaunch. It can go stale if data changes only via a reminder action while the app stays closed — acceptable; opening the shade shows the source of truth.
- **Shared element transitions** ride Navigation 3's `LocalNavAnimatedContentScope`: item titles morph between list rows and detail/editor screens; rows without an active `SharedTransitionScope` render normally (desktop safe).
- **M3 audit result**: every control is a stock M3 component (chips, segmented buttons, switches, cards, checkboxes, snackbars, pickers); type comes exclusively from `MaterialTheme.typography`, color exclusively from `colorScheme` (the only literal colors are user-chosen item swatches); spacing sits on the 4/8dp grid; the only custom-drawn things are the save burst (theme colors) and calendar day cells (spec'd shapes). Empty states are Material icons + body text, no image assets.
- **Tap audit (from home screen)**: capture = open + type + send (1 tap past typing; Enter = 0); complete = 1 tap (or 1 swipe); triage = 1 tap; reschedule = 2 taps (chip + choice); capture-from-widget = 1 tap to keyboard-up. Nothing exceeds budget; zero confirmation dialogs anywhere — destructive paths all use undo snackbars.

## Phase 9 — desktop

- **Keyboard commands ride a tiny shared-flow bus** the window emits into and the active screen collects from; Android never emits, so the bus is inert there. Plain-letter shortcuts can't fire while typing because focused text fields consume their keys before the window handler sees them.
- **Enter saves capture, Shift+Enter inserts a newline** — wired on the field itself so hardware keyboards behave the same on both platforms.
- **NavigationSuiteScaffold's automatic rail is the wide-screen layout**; detail screens stay single-pane. At this app's information density a second permanent pane would mostly show an empty editor — deliberately skipped, revisit if desktop use disagrees.
- **Desktop notifications go through the system tray** (`TrayState.sendNotification`); reminder *scheduling* stays a desktop no-op — the desktop app isn't a background process, so alarms there would only ring while it's already open.
- **The desktop distributable is the jpackage app-image** (self-contained folder with `tina.exe`). The MSI target is configured but needs WiX 3.x + admin (+ the .NET 3.5 feature) — not something an unattended build should install; command documented in the README.
- **Desktop ProGuard is off**: Room/Koin/rich-editor reflection trips it and minification buys a personal app nothing.

## AI-assisted capture parsing (REL-130)

- **AI never sits in the hot path.** The regex parser stays the interactive layer (live chips, instant save); the LLM re-parses the raw text *after* save in a background scope that outlives the screen, upgrades the item only if meaningfully different, and shows a "Refined by AI — Undo" snackbar. Any AI failure silently falls back to the local parse; a capture the user edited mid-flight is never clobbered.
- **"Any key" = the OpenAI-compatible chat protocol.** One small Ktor client (`POST {base}/chat/completions`) covers OpenAI, Ollama, and every compatible aggregator via the Custom provider; Anthropic gets a ~40-line native Messages API path (`x-api-key`, `anthropic-version: 2023-06-01`) since there's no official Anthropic KMP SDK to use from commonMain. No agent framework — this is one JSON-extraction call.
- **Claude subscriptions can't be used** — only API keys; subscription auth is Claude-products-only.
- **No `output_config`/effort params on the Anthropic call** — maximum model compatibility (older/smaller Claude models reject them); the parse is trivial either way.
- **Provider switch seeds defaults atomically** (single DataStore edit) so the settings fields populate in the same emission as the provider — two edits raced the UI's local field state.
- **API key lives in DataStore plaintext on-device** — single-user personal device, key never syncs; Android-Keystore encryption is available if this ever matters.
- **`usesCleartextTraffic=true`** so a LAN Ollama (`http://192.168.x.x:11434`) works; INTERNET permission added (the app's first).
- **Anchored "now"**: refinement resolves relative dates against the *capture* timestamp, not the refinement timestamp, so slow models don't shift "in 30 min".
- **Parser upgrades** (still the offline floor): `next month`, `end of week/month`, `in 30 min`/`in 2 hours`, bare `at 5` (1–7 evening, 8–11 morning, 13+ 24h), `every other day/week/month/year`, `every weekday`, `5 jan` day-first dates, and multiple conflicting clock times now land in Inbox like conflicting dates do.
- **Verified live** against a local Ollama (`qwen3-coder:30b`): "coffee with jess thursday around 130ish" → EVENT Thursday 13:00–14:00 titled "coffee with Jess", refined on-device end to end.
- **Android 17 network permissions are a real gate**: INTERNET added via an app update is not auto-granted (user-sensitive), and LAN endpoints need the new `ACCESS_LOCAL_NETWORK` runtime permission — a fresh device denies sockets with EPERM until both are granted. The AI settings section requests them when a provider is enabled, and the test button surfaces the underlying exception instead of a generic failure precisely because this class of problem is invisible otherwise.
- **AI HTTP timeouts are 3 minutes** (10s connect): a cold local 30B model takes ~a minute to load before it can answer; OkHttp's default 10s read timeout made every cold request fail.
