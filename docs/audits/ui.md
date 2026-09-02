# Audit: UI polish, accessibility, platform completeness

Read-only audit performed 2026-09-02 against v1.6.0 (`aaf28e5`). Paths relative to `composeApp/src/commonMain/kotlin/com/tina/app/` unless noted.

## 0. Blockers

- B1 No launcher icon (no mipmap, no adaptive/monochrome icon, no `android:icon`).
- B2 Platform light-only theme; white flash on cold start; no splash styling.
- B3 Three version numbers disagree: gradle 1.6.0, About shows `APP_VERSION = "1.4.0"` (`ui/settings/SettingsScreen.kt:272`), What's new tops out at 1.5.
- B4 `allowBackup` default with no rules; plaintext key in DataStore.
- B5 `SCHEDULE_EXACT_ALARM` only.
- B6 App-wide cleartext.
- B7 No `android:supportsRtl`.
- B8 No R8.

## 1. Accessibility (ranked)

1. Calendar day cells unlabelled: no month/weekday, selected, today, has-items, role, or long-click label (`agenda/AgendaScreen.kt:1003-1054`).
2. Series expand/collapse `IconButton` has `contentDescription = null` (`:707-713`).
3. Swipe actions have no `customActions`; TalkBack users cannot complete/delete/triage from lists (`ui/ItemRow.kt:130-202`).
4. Row tap starts inline rename with no `onClickLabel`/role (`ui/ItemRow.kt:238-245`).
5. `BasicTextField`s (capture bar, search) have no accessible label once text exists.
6. No `heading()` anywhere.
7. No `liveRegion` for snackbars, "Thinking…", errors, chip updates.

Touch targets under 48 dp: mic and send (40), mode ToggleButton (40 h), note pin (~40), colour swatches (36), DotStrip day (16 w), ConnectedButtonGroup (44 min), HoldToConfirm (44).

Missing roles/state: note pin, API-key visibility toggle, removable chips, Ask bubbles (activatable no-op), inbox badge not merged, several rows without merged semantics.

Colour-only: event dot, DotStrip states, trash urgency, priority tint (mitigated by label).

Fixed heights: search field 56, settings search 56, mode toggle 40, HoldToConfirm 44, MoreRow 48, DotStrip 48; weekday header single letters.

Done well: settings switch/radio rows, hold-to-confirm long-click semantic, DotStrip custom actions, ConnectedButtonGroup radio semantics, chip removal semantics.

## 2. Localization debt

Feature UI has no hardcoded strings. Debt: Widgets/Shortcuts/What's new/Licenses subpage content; undo `"$it s"`; trash type label; ~40 English settings-search keyword lists; desktop tray strings; two agenda fallback strings; AI prompts. No translations, no `localeConfig`, no plurals, 24 h `timeLabel` unpadded, 29 dead string keys.

## 3. Unfinished

Four "Soon" rows whose implementations exist and work (Language & region, Quick Settings tile, Sound & vibration, Diagnostics): `rememberSettingsSections` never references `actions`. Notes grid/list toggle exists in the VM but not the UI. `RequestAiNetworkPermissions` never called. `PlaceholderTab` dead. `KeyCommand.CONFIRM/DELETE` emitted but unhandled. "Open app to" is decorative. Shortcuts row says 12 bindings, subpage lists 4. Duplicate sheet "Keep both" is a no-op.

## 4. Large screens

`NavigationSuiteScaffold` adapts to a rail; nothing else does: no window size classes, sheets and bar are `fillMaxWidth`, notes grid fixed at 2 columns, settings full-bleed, Ask bubbles fixed 320 dp, desktop window has no minimum size; landscape phones get rail + bottom bar and cramped sheets.

## 5. Desktop

Reminders are a no-op yet all notification settings show. Bare key bindings (`N`, arrows, Enter, Delete) fire regardless of text focus. Baseline M3 purple palette (no brand seed). LaunchAtLogin Windows-only but shown everywhere. Voice toggle shown though unavailable. Tray icon is a vector glyph. No app lock or auto-backup. BackHandler and insets are safe.

## 6. Theming

No brand palette (dynamic colour or stock M3). Contrast mode is a hand-rolled lerp. Pure black gated on DARK only. Hardcoded settings hues and colour presets; **white check on yellow/orange swatches fails contrast**. Note cards tint at 0.18 alpha. Expressive adoption good; legacy `TopAppBar` on five screens; `SaveBurst` ignores reduce-motion; desktop reduce-motion always false.

## 7. States and the undo rule

Missing: agenda loading state (blank body during first read); empty-state flash before first emission on Inbox/Notes/Trash/Tags; notes search has no "no results" copy; all AI failures collapse to one message (Wi-Fi-only included); Improve sheet likewise; search sheet has no idle state; suggestion cache in memory only; reminder banner permanent on API 31-32; backup import failure generic.

Undo violations: Detail and Event editor delete have no undo (snackbar host pops with the page); Trash restore has no undo; the discard-draft dialog is the one plain confirmation.

## 8. Onboarding

None. First launch: white flash, robot icon, Agenda, keyboard closed, empty state as the only teaching, starter chips as the second, reminder banner as the only permission ask. AI, Ask, widgets, tile, lock, backup are never introduced. No what's-new gate on upgrade.

## Suggested ordering

P0: icon, splash/theme, version alignment, backup rules + key storage, exact-alarm declaration, RTL, undo on detail/event delete, agenda loading state.
P1: the seven a11y items, touch targets, brand colour scheme, swatch contrast, differentiated AI errors, notes no-results, API 31/32 banner, desktop reminder switches, desktop key bindings.
P2: enable the four built rows, adaptive grid and max widths, list-detail on expanded, dead strings, resource-ise subpage content and keywords, plurals, SaveBurst reduce-motion, pure black under SYSTEM, "Open app to".
