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

- **The composer sits at the bottom**, above the nav bar, with the suggestions and recents filling the page above it. The shell rides the keyboard as one lifted layer (see "The keyboard lifts the shell" below), so the field, the nav bar and the chips move together with no extra inset work. It is the same composer as Ask — a filled 28dp pill with the send button beside it rather than inside it, and the mic as the field's own trailing icon — because a bare transparent field at the bottom of an open page reads as loose text rather than an input, and the app should only have one composer design.
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

## Agenda · Inbox · Notes (Library retired)

- **Library added nothing the Agenda's All range didn't.** It was the same rows behind a filter rail; its three real contributions — global search, inbox triage, a Done view — each got a better home. Search is a sheet over whatever page you're on (from the header magnifier or Ctrl+F); Done lives in search; Inbox is a destination again.
- **The nav bar is the app's three verbs**: plan (Agenda), sort (Inbox), write (Notes). Inbox is the one split that isn't redundant with the home — untriaged captures are precisely what the Agenda does not list — and its badge is the natural "you have things to sort" signal for a capture-first app. Material's own guidance is 3–5 destinations; two looked like something was missing.
- **Capture and Ask are modes, not destinations.** The bar's leading control is a labelled expressive `ToggleButton` pill (✎ Capture / ✦ Ask) that morphs shape on switch, so the current mode reads at a glance and the switch is an obvious button. With no AI provider the pill still names the mode but won't flip.
- **The capture bar hides while the search sheet is up.** Search has its own field; two stacked fields would fight for the keyboard. Every other sheet keeps the bar.

## Refinement pass before v1.5: Plan · Sort · Ideas

- **Vocabulary.** Pages are named for what you do there: Plan (was Agenda), Sort (was Inbox), Ideas (was Notes). The bar's modes are Plan / Idea / Ask. Enum names and file names keep their old identifiers (`TinaTab.AGENDA`, `InboxScreen`) — only the labels changed.
- **Capture sheet has its own state.** `captureSheetOpen` is set when the field takes focus and cleared only by scrim, handle, back, tab change or discard. Dismissing the keyboard no longer collapses the sheet; back with the keyboard down closes it (prompting if there is a draft).
- **Bar blends into the sheet.** The bar paints `surfaceContainerLow` whenever a sheet is up so it reads as the sheet's bottom edge rather than a cutout.
- **Mode pill keeps one shape.** The Plan/Idea/Ask pill uses `roundShape` for every state; colour alone signals the mode. The morph made it look glued to the edge.
- **One calendar for every range.** `agendaCalendar` (HIDDEN / WEEK / MONTH) is a persisted setting chosen from the title's dropdown and applied on Day, Week, Month and All alike. The pilled-week header and the per-range special cases are gone.
- **Idea mode replaces note parsing.** The parser never returns NOTE any more (long prose is a task like any other short text). In Idea mode the field is the title and the sheet shows a body field; parser-token starters are hidden. Type cycling skips NOTE. ponytail: tags are still pulled from the title in Idea mode, nothing else is.
- **Settings transition.** NavDisplay is painted with the theme background so the pop/push slide never shows the window behind it, and predictive back uses the same pop spec as a completed back.

## Ask keeps its mode, one calendar button, baseline profiles

- **Ask mode vs Ask overlay.** `askOpen` is the pill; `askSheetOpen` is the overlay. Back, scrim and drag close the overlay only, so back walks keyboard -> overlay -> page exactly as it does for capture, and the bar stays on Ask until the pill is tapped. Focusing the field in ask mode brings the overlay back.
- **Calendar cycles from the top bar.** The title dropdown was two taps in the far corner; now one icon button next to search rotates none -> week -> month and shows the current view.
- **Sideloaded builds got no compile step.** Play compiles an app against its baseline profile at install; `adb install` and GitHub-release APKs never do, so every first pass through a sheet ran cold in the JIT (the "sometimes buttery, sometimes choppy" report). `androidx.profileinstaller` now ships the merged Compose profiles in the APK and installs them on first launch; ART picks them up on its next background dexopt. After an adb install, `cmd package compile -m speed-profile -f com.tina.app` applies them immediately.

## Keyboard-rise jank and the sheet-to-bar gap

- **Measured, not guessed.** `adb screenrecord` at 120 Hz plus frame-gap analysis: the nav bar tracked the keyboard exactly (constant 232 px) in every run, so there was no "snap"; what showed up were dropped frames (16–50 ms gaps) while the keyboard rose with the sheet open. Cause: `imePadding()` on the shell shrinks it every frame, and the page underneath (app bar, calendar, lazy list) was re-measured 120 times a second.
- **Fix.** `keepHeightUnderKeyboard()` measures the page against the keyboard-free height, so its constraints never change and Compose skips its measure; the bar, nav and sheet just cover its bottom. Order matters: it goes *before* `fillMaxSize()`, otherwise the fixed min-height changes per frame and the page re-measures anyway (that variant measured worse than before). After the fix: 8 ms frames throughout, three runs.
- **Sheet tail.** The spring overshoot on the sheet's slide could open a sliver between sheet and bar. The sheet now carries a 32 dp tail offset down behind the bar, so overshoot never exposes the page.

## Settings as a hub of categories

- **Modelled on the system settings page.** The main page is a search pill and one card per category (tinted circle icon, title, the first three row titles as the summary), grouped the way Android groups them: everyday first, About last. Each category opens its own page with the rows it already had. The section data (`rememberSettingsSections`) is unchanged; the hub and section pages are two renderings of it, chosen by `sectionId`.
- **Search lands on the row.** A result opens the category page with that row highlighted (`SettingsSectionRoute(sectionId, highlight)`), instead of scrolling one long page.
- **Summaries are derived**, not written: the first three distinct row titles. ponytail: a hand-written summary per category if a derived one ever reads badly.

## Why the app kept dropping to 60 Hz

- **Cause.** Android 15+ "frame rate power savings": the View toolkit votes a refresh rate per invalidation and trims to 60 Hz when redraws look small. A Compose app is one View, so a caret blink or a chip toggle voted 60 and SurfaceFlinger pinned tina's uid there (`frameRateOverride {uid=… 60}` in `dumpsys display`); the next big animation then ran at half rate until the override aged out. Targeting SDK 35+ opts the app in.
- **What did not work.** Voting `REQUESTED_FRAME_RATE_CATEGORY_HIGH` from the Compose view: on this Pixel "high" maps to 90 Hz (`frameRateCategoryRate {normal=60, high=90}`) and the recordings got *worse* (17–25 ms frames).
- **Fix.** `window.isFrameRatePowerSavingsBalanced = false` in `MainActivity.onCreate` (API 35+). After it the override no longer appears at all and page transitions record at a steady 8 ms; the keyboard rise is 8 ms with the odd 16 ms frame from the IME itself.

## The keyboard lifts the shell; it never resizes it

- **Found with a real profile.** `simpleperf` is blocked on GrapheneOS, so `am profile start --sampling` (ART method sampling, needs `<profileable android:shell="true"/>`, now in the manifest) plus a small trace reader in the scratchpad. Frame time during the keyboard animation was spread thinly across measure of NavigationSuiteScaffold, Scaffold's subcomposition, AnimatedContent's lookahead pass, and placement of every Row/Box in the tree: `imePadding()` on the shell re-measured and re-placed the whole shell 120 times a second. Constant-constraint tricks on the page, bar and sheet shaved it but the structure itself still cost ~8 ms a frame.
- **Now:** the shell is not padded for the keyboard at all. `NavigationSuiteScaffold` gets `offset { -shift }` where shift = ime − navigation-bar inset (so the nav bar lands flush on the keyboard); the page gets `offset { +shift }` in its own layer so it stays put on screen while the bar, nav and sheets ride up over it. `offset {}` places with a layer, so a keyboard frame is two layer translations and no measure. Ask and Search sheets size against the visible height (`visibleHeight`), the only per-frame measure left.
- Result: capture sheet + keyboard open went from 31 % janky / 46–61 ms p90 to 2 % / 11–12 ms p90; keyboard-only rise 12 ms p90.
- **Reverted (v1.8.0).** On the Pixel 9 Pro XL running Android 17 the hand-rolled lift drifted from the keyboard: the IME window appeared ~80 ms after the tap while the offset followed a 300 ms inset ease, so the bar climbed out from under an already-drawn keyboard and, on close, sank through one still on screen. Neither the visible nor the stable nav-bar inset ever stepped in a per-frame probe, so the arithmetic was not the culprit. The shell is back on `imePadding()`, the platform's own inset animation, with the page pushed up rather than covered; Ask and Search sheets take a fraction of the padded height. `imePadding` eases over the keyboard's inset animation; on the user's Pixel (Android 17, Gboard) the keyboard window itself popped in fully drawn in ~60 ms and ignored that animation, so the bar lagged it. Padding by `imeAnimationTarget` (jump to the end value on the first frame) was tried and removes the lag on that phone at the cost of the synchronized ride everywhere else; the user chose to keep the platform default and the ride. There is no "fixed to the keyboard" primitive on Android: the keyboard is another process's window and the app only learns its height through insets, so the tightest coupling available is to match the target, not the tween. If the measure-per-frame cost shows up again in a profile, fix it under this padding, not with a parallel animation.
- **The actual cause of the pop-in (v1.8.0).** It only followed dismissals the app made by clearing focus (sheet drag, scrim tap, tab switch, discard): that ends the input session, and the next show after it appeared without its slide on the Pixel while the app still ran the 300 ms inset ease. Back and Gboard's own button hide first and let focus go afterwards, and those never popped. Every app-side dismissal now hides the keyboard (`SoftwareKeyboardController.hide()`) and leaves focus to CaptureBar, which drops it once the keyboard is gone; desktop keeps `clearFocus()` because it has no keyboard and the bare-key shortcuts need the field released. The user tested Signal on the same phone to rule the platform out.

## Swipe triage on Sort; the agenda title says what you are looking at

- **Swipes are per page, on the one row.** `ItemRow` already owned swipe (right = complete, left = delete). Rather than nest a second `SwipeToDismissBox`, it takes optional `swipeRight` / `swipeLeft` overrides (`SwipeAction`: icon, tone, action). Sort passes Today (right) and Someday (left), both through the same undo path as the chips; delete stays on the row menu there. Everywhere else the defaults are unchanged.
- **Title = day / week / month, relative when it can be.** Day: "Today" over "Wednesday, September 2" (or the weekday over the date); with the month grid up the second line names the month being scrolled. Week: "This week" / "Next week" / "Last week" over the range, else the range over the year. Month: month over year. All: "Everything".

## An idea's snackbar offers Open

- Saving in Idea mode shows "Idea saved · Open" instead of "Captured · Undo": an idea is usually the start of something, so one tap continues it in the note editor. The editor has delete for the rare wrong save; Plan-mode captures keep Undo.

## Series edit in place

- The repeat editor (rail + custom dialog) moved out of the event editor into `ui/RepeatEditor.kt` and the task page uses the same one, so a recurring task's rule is edited where its other fields are. The series row's long-press menu gained "Edit series", which opens that page. Editing the rule keeps per-occurrence completions (they are keyed by item and day, not by rule).

## The shell never leaves composition

- Pages (Settings, item, note, tag) used to be NavDisplay entries above `ShellRoute`, so every return re-composed the shell from scratch: calendar state, bar, sheets, rows, ~120 ms in one frame. Now the shell is composed once in `App` and pages live in their own NavDisplay inside an `AnimatedVisibility` on top. Returning is the pop exit and nothing else (99th percentile 25 ms, was 150). A still `AnimatedContent` gives the shell the animated scope its shared-element rows need.
- **Capture sheet toggle at the bottom.** Plan | Idea sits right above the field: the sheet is bottom-anchored, so that is the one spot that does not move with the number of recents.
- **Empty pages teach the bar.** Plan and Ideas empty states carry one line about what to type below; the first-run Plan (All range, nothing at all) says plans collect here.
- **Idea tile.** A second Quick Settings tile opens the bar in Idea mode via `EXTRA_FOCUS_IDEA`; `CaptureFocus.request(idea)` carries the mode. The Today widget shows Plan's second line (weekday, month day).

## Overnight pass towards v2.0 (2026-09-02, early hours)

Five read-only audits (`docs/audits/`) fed a roadmap (`docs/ROADMAP.md`), a monetization plan (`docs/MONETIZATION.md`), a Play checklist (`docs/PLAY-LAUNCH.md`) and a privacy policy (`docs/PRIVACY.md`). The v1.7 "Trust" milestone was then implemented as far as the emulator could verify it.

- **Destructive fallback scoped to v1 only.** `fallbackToDestructiveMigrationFrom(dropAllTables = true, 1)`: any other missing migration crashes on open instead of silently dropping tables. A JVM `MigrationTest` builds v2 and v5 databases from the exported schema JSON and opens them with today's Room, so a forgotten migration fails in CI.
- **Items have a uuid.** Schema v6 adds `items.uuid` (unique index, backfilled with `randomblob`). Backups dedupe on it, import is idempotent, and it is the identity a future sync will use. Local `id` stays for Room and alarms.
- **Backup v2 carries everything** (trash, occurrence completions, chats) and is imported in one transaction through `BackupService`; the repository no longer owns import/export. Settings inside a backup are returned to the caller and offered through the snackbar ("Restore settings"), never applied as a side effect. A file with a higher version than the build refuses to import rather than silently dropping fields.
- **The AI key is encrypted with the Android keystore** (`KeystoreSecretCipher`, AES-GCM, `enc1:` prefix so old plaintext values still read) and excluded from backups. `settings.preferences_pb` is excluded from Android cloud backup and device transfer; the WAL is checkpointed when the app stops so the database file Android copies is consistent.
- **Cleartext only to private hosts.** `isAllowedAiEndpoint` gates every request builder: https anywhere, http only to localhost/RFC1918/.local. The manifest still allows cleartext (LAN Ollama is the point) but no hand-typed URL can leak the key over the internet.
- **Reminders.** `nextReminderTime` handles repeating tasks and searches strictly after the instant that fired (the old inclusive range could re-pick the current occurrence and cancel the series). The scheduler uses `setExactAndAllowWhileIdle` when granted and `setAlarmClock` otherwise: Doze-exempt, no permission, one status-bar icon. `USE_EXACT_ALARM` is declared (calendar/reminder app; Play form in `PLAY-LAUNCH.md`). One receiver re-arms on boot, package replace, timezone change and exact-alarm grant. A reminder tap opens its item through `OpenItemRequests`; Done on a series writes an occurrence; reminders are grouped with a summary that is dropped with its last child. Verified on the emulator by moving the clock: fires at 17:50:00, re-arms for the next day, survives an update, keeps an event's absolute time across a timezone change.
- **R8 on.** 69 MB to 11.7 MB. Keep rules for serialization, Room entities, widgets, receivers and the rich editor. Capture, Sort, Settings, the note editor and an Ollama round trip verified on the minified build.
- **Launcher icon and theme.** Adaptive icon (check + capture line) with a monochrome layer; `Theme.Tina` paints the pre-Compose window in the theme ground and styles the Android 12+ splash. A proper brand icon is a v1.8 design task.
- **CI.** GitHub Actions runs tests, lint and an unsigned release build on every push; a tag builds a signed bundle from secrets. Signing is conditional on a keystore, README no longer carries a password, version name comes from the build.

### Play Billing scaffold (v1.9 groundwork, built early)

- `ProStore` in commonMain with `PlayProStore` (Android) and `NoProStore` (desktop). The entitlement is cached in the settings DataStore so Pro survives being offline; every launch re-reads Play and a successful "nothing" revokes.
- Purchases are acknowledged on the client. Server-side verification (the relay's `/entitlement`) waits until hosted AI exists, because that is the only thing a forged purchase could steal.
- Play Billing's auto-reconnection never delivers `onBillingSetupFinished` on a device with no Play account (it retries forever with result 3), so `connect()` has an 8-second timeout and the paywall then says Play is unavailable.
- Nothing is gated. `rememberIsPro()` is there for the v1.9 features; gating existing free features would break the "capture stays free forever" promise in MONETIZATION.md.
- A one-row settings section opens its row from the hub. tina Pro was the first such section and the middle page was a wasted tap.
- Large screens: sheets and the bar cap at 640 dp; lists stay full width. A max width on the agenda would have to move the swipe-to-navigate hit area too, so it waits for a real tablet test.
- Ask errors are typed (`AiException(kind)`) at the HTTP layer and turned into one sentence each on the Ask page. Capture parsing stays silent on failure by design: a failed refine falls back to the local parser and the user never waits on it.

### Rename to Peggy (2026-09-02)

- Napkin was rejected: an existing note-taking app (napkin.one) owns the word in this category. Peggy is clear on Play in tasks and notes, says itself out loud, and "peg it down" gives the copy.
- `applicationId` is now `com.peggy.app`. It had to change before the first Play upload, since the id is permanent after that. The Kotlin package, namespace, intent actions, theme name, `TinaApp`, the desktop `~/.tina` folder, `tina.db`, the `TINA_*` signing variables and `tina.proOverride` all stay: nobody sees them and renaming them would only churn history.
- A new applicationId is a new app on a device: existing installs keep their data under tina and move it with Export → Import. Backups are byte-for-byte compatible.
- Pro product ids are `peggy_pro_monthly`, `peggy_pro_yearly`, `peggy_pro_lifetime`; nothing was created in Play Console under the old ids.

### First-run cards and the review prompt (v1.7.1)

- Three cards over the shell on first launch, gated by an `onboardingSeen` flag that is read as "seen" until DataStore answers, so an upgrade never flashes them. Not part of `Settings` or backups: it is state, not a preference.
- The review prompt fires on the twentieth capture, once, through Play's in-app review. A DataStore counter, not a database count, so a restored backup does not trigger it on day one.
- `ForegroundActivity` replaces the per-class activity tracking that billing had; review needs the same thing.
- Once the phone runs the Play build it is signed by Google's key; sideloading upload-key APKs over it fails. Phone updates now go through internal testing, which is what the versionCode bump is for.

### The Peggy icon (v1.7.2)

- A pin with the check inside. The pushpin candidate read as a syringe or a capital H at launcher size; the pin keeps "peg it down" and carries the check the app already used, so the notification icon and the feature graphic stay coherent.
- One geometry, defined in `docs/assets/icon.py`, drives the adaptive foreground, the monochrome layer (outline, since one colour cannot show a check on a filled pin), the Play icon, the feature graphic and the site SVG.

### All-day events across time zones (v1.7.3)

- Kept the local-midnight storage. Instead of epoch-day columns (a migration plus every reader of `startAt`), the app records the zone it last ran in and, when the zone differs, re-anchors every all-day event's midnight from the old zone to the new one, then re-arms reminders. Runs on launch, on the system zone broadcast, and on desktop start.
- The first run in a zone only records it; nothing moves until a change is observed, so existing data is never touched on upgrade.

### Quiet hours (v1.7.3)

- Applied at the one choke point every reminder passes through, `AndroidReminderScheduler.schedule`, as a deferral to the window's end. The scheduler is synchronous, so the app pushes the window into it from the settings flow and re-arms everything when it changes.
- Digests are exempt: their times are the user's own choice. Quiet hours are not in backups yet (BackupSettings is versioned; add them with the next backup bump).

### Brand palette (v1.7.3)

- MaterialKolor generates the scheme from the launcher blue at runtime (it is Google's material-color-utilities ported to KMP). Hand-picking forty tonal roles would have been wrong in the ways that only show on a real screen, and the library also opens the door to real contrast tone sets later.

### Sort becomes the decisions page (v1.8.0)

- The inbox was empty because the app never created inbox items: an undated capture became a task on today's Anytime list. Sort now receives undated captures by default (setting to keep the old behaviour) and also lists overdue tasks, snoozed reminders (a new `snoozedUntil` column, migration 6→7) and every someday item. Plan's Anytime group no longer shows undated tasks: a someday item lives on Sort until it has a date, so Plan only ever shows commitments.
- Same page, same swipe cards, different feeds; the alternative was deleting the tab and going to two, which threw away the one interaction testers liked.
- Recurring tasks are never "overdue" here; they are judged per occurrence on the agenda.

### Parser corpus (v1.8.0)

- `CaptureCorpusTest` is the parser's spec: one real phrasing per line with what it must become. New phrasings go there first; the parser follows. It documents the deliberate trades too ("sunday roast recipe" loses its weekday to the date).
- Time ranges are one clock plus a length, so "2-4pm" never trips the two-times ambiguity rule. A bare start ("10-6pm") flips to am when pm would put it after the end.
- Frequency words ("daily", "weekly") only count when they end the phrase or are followed by a day; "the daily show" and "weekly report draft" stay titles.

### Widget pass (v1.8.0)

- Checked on the emulator's Pixel launcher in light and dark: the capture pill and the Today card render correctly, the pill and the quick-settings tile land in the bar with the field focused, and a Today row now deep-links to its item.
- The Today widget only refreshed when the app went to the background or a reminder fired, so the next morning it still showed yesterday until the app was opened. `updatePeriodMillis` of 30 minutes is the floor Android allows and is enough for the date to roll.
- Picker previews are PNG crops of the rendered widgets (`drawable-nodpi`). Glance cannot supply a preview layout, and an app icon in the picker undersells a widget.

### Undo audit (v1.8.0)

- Every destructive action was walked: deletes on every page, Sort answers, occurrence skips and series ends, duplicate merges, tag deletes, clear completed, trash purge, Ask's write actions. All had undo. Empty trash had only hold-to-confirm; it now keeps the rows in memory for the undo window and re-inserts them. Delete everything stays hold-only: it is the one action meant to be final, and a backup is the undo.
- Habit history (occurrence rows) is not restored by trash undo; the items are. Acceptable for the window involved.

### Ideas redesign (v1.8.0)

- Built from the Claude Design file `Tina Ideas Redesign.dc.html` (turns 5 and 6). The card shape (titled / scrap / list) is decided in `previewOf`, not the composable, so the grid, the list layout, search rows and the tag page all agree. Capture puts everything in `title`; `splitIdea` splits a long one-liner at the first line or sentence so old and new notes both render right.
- No project entity. A project is a tag that has a pinned note carrying it (`TagUi.overview`): pinning promotes, unpinning demotes, nothing to create or archive. The rail chip's underline is the only new visual concept.
- Selection state lives in the ViewModel and the tap handlers read it at tap time. Function references to local funs were being memoised with the `selectionMode` they were created with, so a tap in selection mode opened the note. `KeyBus.pageOpen` became observable so the grid's BackHandler yields to a page pushed above the shell.
- The tag rail chip is drawn by hand: stock `FilterChip` owns its click and a long press on top of it never fired.
- Skipped: manual drag-reorder (the staggered grid has no stock reorder), checklists with real checkbox state (`richeditor-compose` 1.2.0 has no task-list model; list cards show bullets), share / copy-as-Markdown, AI tag suggestions. Add when the editor grows a checklist model or someone asks.
- `NoteEditorViewModel.edit` skips unchanged writes: opening a note re-emits its body and every open was bumping `updatedAt`, which reshuffled "Last edited".
- `htmlPreview` only turns block tags into spaces; inline tags vanish, so "<b>ginger</b>." no longer reads "ginger .".

