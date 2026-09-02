# Decisions

## Settings rebuild (REL-146, Sep 2026)

- **material3 1.9.0 ships no expressive components.** Probed the artifact directly:
  `ButtonGroup`, `ToggleButton`, `LargeFlexibleTopAppBar` and `LoadingIndicator`
  are absent; `MotionScheme` and both opt-in markers are present. Upgrading is not
  an option — 1.9.0 is pinned against richeditor's ABI-broken 1.11.0-alpha07, which
  crashed the app before. So the design's documented fallbacks are used
  (`SingleChoiceSegmentedButtonRow`, `LargeTopAppBar`, `CircularProgressIndicator`),
  each marked `// TODO(expressive)`.
- Settings are **data, not layout**: `SettingsSection`/`SettingsRow` are built once
  and rendered generically, which is what makes search, platform filtering and
  scroll-to-row possible without duplicating rows. Row titles are resolved Strings
  rather than StringResources so search can match them directly.
- Platform differences **hide** rows via `expect object Platform`; a dead toggle is
  worse than no toggle.
- Grouped list = 2dp gaps + per-row corner morphing, no dividers.
- **Placeholder rows.** Settings that are designed but not yet wired render greyed
  with a "Soon" tag instead of silently doing nothing. This is deliberately different
  from platform hiding: an unimplemented setting is a promise, a platform-irrelevant
  one is noise.
- **App lock uses the system credential prompt** (`KeyguardManager.createConfirm
  DeviceCredentialIntent`) rather than androidx.biometric — same PIN/pattern/biometric
  result, one fewer dependency, which the brief required.
- **Digests are inexact alarms.** Daily agenda / overdue nudge / inbox reminder are
  summaries, so they use `setAndAllowWhileIdle` and re-arm themselves; the exact-alarm
  budget stays reserved for reminders the user actually set on an item.
- **Launch at login writes a Startup-folder .cmd**, not a registry key: no elevation,
  and the user can see and delete it in Explorer.
- **The desktop global capture hotkey is the one unimplemented row.** A hotkey that
  works while the app is unfocused needs a native key hook (JNativeHook or similar),
  i.e. a new dependency the brief rules out. It keeps its "Soon" tag.
- **Soft delete (schema v4).** `deletedAt` column; `delete()` moves an item to Trash
  and a launch-time job purges past the retention window. `get()`/`observe()` exclude
  trashed rows deliberately — otherwise reminders could fire and the Ask page could
  act on deleted items. `getAnyById()` is the restore path's escape hatch, and
  `restore()` clears the flag rather than re-inserting, so undo can't duplicate a row.
  A pre-existing repository test caught exactly this and was right to.

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

## Page transitions

- **One horizontal axis for the whole app.** Depth reads as direction: pushing a page slides it in from the trailing edge while the page beneath parallaxes a quarter-width and fades; popping plays that in reverse; switching nav-bar tabs uses the same axis at a tenth of the distance, because tabs are peers with no hierarchy to express. Navigation 3's stock fade-and-zoom is replaced outright — scaling a full page reads as a dialog, not as travel.
- **Pop inverts z-order** (`targetContentZIndex = -1f`). The page you're leaving has to stay *above* the one being revealed, otherwise the outgoing slide happens behind an opaque page and the whole transition is invisible.
- **Predictive back honours the swipe edge** — nav3 passes it to `predictivePopTransitionSpec`, and a right-edge swipe mirrors the pop so the page follows the thumb.
- **The expressive spring constants are copied, not imported.** `MotionScheme` is public in material3's JVM jar but `internal` in the KMP metadata at 1.9.0, so `MaterialTheme.motionScheme` and `MaterialExpressiveTheme` don't compile from commonMain. The two token pairs (spatial 0.8/380, effects 1.0/1600) are lifted from `ExpressiveMotionTokens` into `ui/Motion.kt`. Delete them and use the real API once material3 exposes it.
- **Reduce motion collapses every transition to a cross-fade** on the same springs, not to a hard cut — the setting removes movement, not feedback.

## Capture layout and focus

- **The composer sits at the bottom**, above the nav bar, with the suggestions and recents filling the page above it. Shell's `imePadding` already lifts the whole shell, so the field, the nav bar and the chips ride the keyboard together with no extra inset work. It is the same composer as Ask — a filled 28dp pill with the send button beside it rather than inside it, and the mic as the field's own trailing icon — because a bare transparent field at the bottom of an open page reads as loose text rather than an input, and the app should only have one composer design.
- **The chips moved with the field**, not with the content — they describe what's currently being typed, so they stay directly above the composer.
- **The keyboard no longer opens with the app.** `autoFocusCapture` defaults off: opening tina to type nothing and having half the screen covered was the single most common annoyance. The toggle ("Keyboard on open") is in the Capture section for anyone who wants the old behaviour.
- **Quick-capture entry points override the setting.** The widget and Quick Settings tile exist purely to get you typing, so they pass `EXTRA_FOCUS_CAPTURE` and focus the field regardless. That signal is `CaptureFocus`, a `StateFlow` rather than an event on `KeyBus` — a cold start would race a `SharedFlow` emission against composition and silently drop it. Shell watches it to switch to the Capture tab (the app may be set to open elsewhere), CaptureScreen focuses and clears it.
- **The desktop focus shortcut routes through the same signal**, so Ctrl+N still lands in the field now that composition no longer auto-focuses.

## IA consolidation (REL-149, Part A)

- **Seven destinations became three.** Agenda (Today + Calendar), Library (Notes + Inbox + Search + Done), Ask. Today and the calendar's day agenda were rendering the same rows for the same date; Notes, Inbox and Search were three list screens over one table with different filters. One list per destination, with the filter as UI state instead of a route.
- **Capture is a bar, not a tab.** `ui/capture/CaptureBar` sits above the nav bar on every top-level screen, so capture is zero taps from anywhere. The TRY examples and recents from the old tab live in the bar's expanded empty state. Recents stayed tappable — the earlier "make recents interactive" request outranks the handoff's "non-interactive" note, and a tap costs nothing.
- **The bar's mode toggle is not saveable state.** It flips to ask mode, and it resets on every app start and destination change so the fast path can never be left switched off. Capture text and ask text are kept separately so a half-typed capture survives a detour into a question.
- **Ask is a sheet drawn inside the shell's content area**, not a `ModalBottomSheet`: a modal sheet is a window-level dialog and would cover the capture bar, which has to stay visible under the sheet as the input. Scrim tap, handle tap and system back dismiss it; drag-to-dismiss is deferred. Answer action chips (spec A4) are deferred too — the model already applies actions in write mode, and turning them into chips is a prompt change, not a UI one.
- **`ui-backhandler` is declared, not added.** It was already in the dependency graph via navigation3; declaring it just puts `BackHandler` on the compile classpath for the sheet.
- **Agenda in Part A is the DAY range** of the B2 builder: the four-range switcher and the other range layouts are Part B. The builder is already wired, so Part B is UI over data that exists.
- **Deleted routes can't be deep-linked wrongly** — they were Kotlin objects, so their removal is compile-checked. The widget and QS tile use the `CaptureFocus` signal, which now also drops ask mode; "Open app to Capture" maps to Agenda with the field focused.
- **Desktop row selection (arrow-select, Enter, Delete) was dropped** with TodayScreen; arrows still move the date. Bring it back if desktop use asks for it.

## Day / Week / Month / All (REL-149, Part B)

- **Schema v5: `occurrence_completions`.** Repeating items are stored once with a rule, so "3 of 7 done" needs a table keyed on (item, day). Rows carry a `skipped` flag: skipped occurrences advance "next" without counting as done. Additive migration; items untouched.
- **Occurrence completion ignores type.** A single event row still isn't completable, but a rolled-up series row is — the checkbox completes the next occurrence, the week dot strip completes a specific day. The parser still routes anything recurring to EVENT; recurring *tasks* only arise via type changes, which is fine because completion is per occurrence, not per item.
- **Week is the seven days from the selected date** (matches `range-1-week.png`'s "Sep 1–7"), drawn as its own pilled row rather than the calendar library's Monday-start week. Tapping a day re-anchors the window on it. Swiping the list moves a full week.
- **`SingleChoiceSegmentedButtonRow` stands in for `ButtonGroup`** (`// TODO(expressive)`), same fallback as Settings.
- **"Keep both" on a duplicate just closes the sheet.** Persisting a "these are not duplicates" mark needs another table for a case that hasn't happened yet; Merge is the real action (extras go to Trash, so undo is restore).
- **Series expansion in Month / All is a checkbox list of dates**, not a second row renderer. Done state comes from the builder (`doneDates`), so the list and the dot strip can never disagree.
- **Swipe-to-change-range is on the list, not the rows.** Rows consume horizontal drags for complete/delete, so the gesture works on headers and empty space. Good enough; the switcher is always there.
- **Digests, widgets and Ask still read the repository directly**, not `buildAgenda`. The spec wants them on the same function; that's a follow-up once the agenda itself has settled.
- **Dismissing the keyboard folds the capture panel.** With "keep keyboard up" on, the field kept focus after a save with the keyboard down, and the TRY/RECENT panel squatted on half the screen; nothing else in the layout takes focus in touch mode, so it never collapsed. On Android the IME inset going to zero now clears focus. Desktop has no IME and is left alone.
- **One `ShellSheet` for Ask and for capture suggestions.** The TRY / RECENT panel was an inline expansion inside the bottom bar; it is now the same in-shell sheet Ask uses — scrim, handle, rising above the bar — because the two should feel like one thing. Both now drag-to-dismiss on the handle (80dp), which closes the "drag deferred" note above. Dismissing the suggestions sheet clears the field's focus, which also drops the keyboard.
- **The draft-discard prompt is the app's one confirmation dialog**, added at the user's explicit request. The capture sheet now survives the keyboard going away while a draft exists; back or a drag on the handle with a draft asks "Discard this draft?" rather than throwing text away. The undo-flavoured alternative (dismiss, snackbar with Undo) was offered and declined for now.

## Recurring tasks

- **A repeat without a clock time is a task.** The parser used to route anything with a rule to EVENT, so every habit became an all-day event with a dot instead of a task with a checkbox — and the whole Week dot-strip / per-occurrence completion model is built for habits. Now only a clock time makes an event; `itemFromCapture` keeps the rule on tasks.
- **An occurrence row completes the occurrence, not the item.** `AgendaRow.Single` carries its date and done state for repeating items; the checkbox writes to `occurrence_completions`, and the item's own `completed` flag is never touched by the agenda for a repeat. Reschedule is hidden on occurrence rows — moving one day of a series is a different feature.
- **A repeat's due date is its anchor, never a deadline**, so repeating tasks are excluded from Overdue.
- **Part-of-day words need context.** "morning pages every day" captured as "pages" at 9 AM because `morning` was always a time. It now counts as a time only after a day word ("tomorrow morning", "in the evening", "friday afternoon") or at the end of the phrase ("dentist afternoon"); otherwise it stays in the title. `noon`/`midnight` are unconditional — nobody names a task "noon".
- **Widgets and digests still see a repeating task on its anchor date only**; rolling them through `buildAgenda` remains the open follow-up.

## material3 1.12.0-alpha03 (the expressive unlock)

- **Upgraded from the 1.9.0 pin.** The pin existed because richeditor 1.1.0 pulled material3 1.11.0-alpha07, compiled against foundation 1.11.0-beta02, which threw `AbstractMethodError` on our foundation 1.12.0. richeditor 1.2.0 was built against material3 1.12.0-alpha03, and that alpha is compiled against foundation/ui 1.12.0-beta01 — the same minor line as the CMP 1.12.0 we run — so the mismatch that caused the crash cannot recur. Verified on the phone: Agenda, Settings (text fields), Library, Ask, and the rich note editor all render and type with zero source changes and no runtime errors.
- **No stable material3 past 1.9.0 exists for CMP** (only 1.10/1.11/1.12 alphas), so an alpha is the price of expressive components. Accepted for a single-user app; the alternative is hand-copying tokens forever.
- **What the alpha unlocks** (probed in the jar): `ButtonGroup`, `ToggleButton`, `LoadingIndicator`, `SplitButton`, `FloatingActionButtonMenu`, `MaterialShapes`, a public `MotionScheme`. Every `// TODO(expressive)` fallback and the copied spring constants in `ui/Motion.kt` can now be replaced with the real thing.

## Theme and motion on the expressive scheme

- **`MaterialExpressiveTheme` replaces `MaterialTheme`.** With material3 1.12 the expressive theme is public and carries the spring `MotionScheme`; the hand-copied spatial/effects constants in `ui/Motion.kt` are gone, and every transition reads `MaterialTheme.motionScheme` instead.
- **Two springs, everywhere.** Spatial (`defaultSpatialSpec`) moves things — page slides, sheet rises, expand/collapse, the chevron rotation, the sheet settling after a drag. Effects (`defaultEffectsSpec`) fades and recolours — cross-fades, the swipe background, the settings highlight, the burst fading out. `fastSpatialSpec` is used exactly twice, where a bounce is the point: the send button popping in and the save burst.
- **`AppMotion` is the one vocabulary** for page and sheet transitions (`push`/`pop`/`lateral`/`sheetEnter`/`sheetExit`/`popIn`/`fadeSwap`), all collapsing to a 100ms fade under Reduce motion, so a screen never invents its own spec.
- **The hold-to-confirm timer keeps its `tween`.** It is a two-second linear countdown, not motion; a spring there would be wrong.
- **Every segmented control is now a connected `ToggleButton` group** (`ui/ConnectedButtonGroup.kt`): 2dp gaps, leading/middle/trailing connected shapes, the checked button morphing to its checked shape, radio-button semantics. Used by the Agenda range switcher, the Settings group rows (Theme, First day of week, Default reminder, contrast…), Trash retention, the item detail's type and priority, and the event editor's repeat frequency. `SingleChoiceSegmentedButtonRow` is gone from the app.
- **`LoadingIndicator` replaces every spinner** (Ask thinking, AI improve, the AI connection test) and Settings uses `LargeFlexibleTopAppBar`. There are no `// TODO(expressive)` markers left.
- **Containers sit on the expressive shape scale.** The capture bar, the Library and Settings search fields use `shapes.extraLarge`; the pilled week and the settings search sheet use `shapes.largeIncreased`; the inbox entry row `shapes.medium`; the "Soon" tag `shapes.small`. Section cards stay at 16dp because that *is* `shapes.large` and their per-position corner morphing needs the literal. The 2dp handle and 4dp day pill are decorations, not containers, and keep their literals.
- **Titles use the emphasized type roles** — screen titles (`titleLargeEmphasized`), the item/event/note title fields, sheet and card headers (`titleMediumEmphasized`), and settings subpage titles (`headlineSmallEmphasized`). Body, label and section-header roles stay regular so the emphasis means something.
- **The save burst is `MaterialShapes.SoftBurst`**, settling from a quarter-turn on the fast spatial spring — the one animation in the app that is purely celebratory, so it gets the one purely decorative shape.
- **The Library FAB stays a plain FAB.** It has one action; a `FloatingActionButtonMenu` for one item is a menu with nothing to choose.
- **System bar icons follow the app's theme, not the phone's.** `enableEdgeToEdge` chooses icon colour from the system dark flag, so forcing tina light on a dark phone drew white status icons on a lavender bar. `SyncSystemBars(dark)` in `AppTheme` sets the appearance from the resolved theme on every change; desktop is a no-op.
