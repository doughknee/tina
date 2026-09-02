# tina — roadmap to v2.0.0

Written 2026-09-02 from five code audits (see `docs/audits/`) and the state of `main` at v1.6.0. This is the plan for turning a very good personal app into a product that strangers install from Google Play, pay for, and keep. Companion documents: `MONETIZATION.md` (what is sold and how), `PLAY-LAUNCH.md` (the store checklist), `PRIVACY.md` (the policy to host).

## What tina is, in one paragraph

Capture anything in under two seconds; tina turns it into a task, an event, or an idea and puts it where it belongs. Plan shows the day, week, or month. Sort empties the inbox with a swipe. Ideas holds notes. Ask talks to your own data. Everything lives on the device, nothing needs an account, and every action has undo. The product promise for v2.0 is the same, plus three words: **it never loses anything, it always rings, and it pays for itself.**

## Where v1.6.0 stands

Strengths the audits confirmed: the parser (69 tests), recurrence (leap years, COUNT, UNTIL), the agenda builder, the Expressive UI, frame-time discipline (measured 8 ms frames at 120 Hz), undo everywhere, an unusually clean localization surface (no hardcoded strings in feature UI), and a decision log that is most of a changelog.

Gaps that decide whether this can be sold, in order of severity:

1. **Data can be lost silently.** `fallbackToDestructiveMigration` on every missing migration path; backups exclude trash, recurring-task history, and chats; import overwrites settings; auto-backups die with the app; no migration tests; no CI.
2. **Reminders are not trustworthy.** Recurring tasks ring once; exact alarms are off by default on Android 14+; nothing reschedules after timezone change, app update, or permission grant; reminder taps don't open the item.
3. **Play blockers.** No launcher icon, platform theme, plaintext API key in cloud backup and JSON export, no AAB, no privacy policy, keystore password in the README, app-wide cleartext.
4. **AI is a liability as shipped.** Whole-database context per turn (cost and privacy), prompt-injection into a write-capable agent with no confirmation, opaque errors, wrong Haiku id, no streaming.
5. **Polish debt.** Accessibility (day cells, swipes, headings, live regions), touch targets, no brand palette, no large-screen layout, no onboarding, missing loading/empty/error states.
6. **No safety net around releases.** No CI, lint, crash visibility, screenshot tests, or changelog.

## Principles for the next four releases

- Ship monthly. Every release is installable from Play's testing tracks the day it is tagged.
- Trust before features. Nothing new ships in a release that has an open data-loss or reminder bug.
- The free app stays excellent. Pro is for things that cost money to run or that a power user wants; it is never a ransom on core capture.
- One confirmation dialog, undo everywhere, two-second capture. Non-negotiable.
- Measure on the device. Frame times, alarm delivery, and battery are numbers, not impressions.

## Milestones

### v1.7 — Trust (target: 3 weeks)

Theme: the app never loses data and always rings. This is also the first Play upload, to the internal track.

**Data safety**
- Replace the destructive fallback with `fallbackToDestructiveMigrationFrom(1)`; add a JVM migration test that builds v2 from the exported schema and migrates to current. *Done: no migration can silently drop tables; the test runs in CI.*
- Backup v2: `version` is written and checked; includes trashed items, `occurrence_completions`, chats; items carry a `uuid` (new column, backfilled) and import dedups on it; import runs in one transaction; settings from a backup are applied only after an explicit "Also restore settings?" choice; the API key is never in a backup. *Done: export → wipe → import reproduces the app exactly, including a half-done weekly habit.*
- Auto-backup writes atomically (temp file + rename), reports its last success in Settings → Data, and uses `UPDATE` policy. Optional: let the user pick a folder (SAF tree) so backups survive uninstall.
- Android backup rules: exclude `settings.preferences_pb` from cloud/D2D; checkpoint the WAL when the app stops. API key moves to Keystore-encrypted storage on Android.
- All-day events stored as epoch-day pairs so they stop shifting across time zones. Migration converts existing rows.
- `updatedAt` bumped on every write; occurrence rows deleted with their item; `sortOrder` unified.

**Reminders**
- Recurring tasks re-arm after each occurrence (and skip occurrences already done).
- Declare `USE_EXACT_ALARM` (with the Play form) and use `setAlarmClock` as the no-permission fallback; reschedule everything when the permission state changes, on `MY_PACKAGE_REPLACED`, and on `TIMEZONE_CHANGED`.
- Reminder tap opens the item; Done/Snooze update the widget; reminders grouped with a summary; dedicated notification icon; `BigTextStyle`.
- Digest fixes: today's events only, overdue-nudge time editable, inbox reminder on its own time, quiet hours (a start/end pair in Settings → Notifications) respected by all four.
- Alarm cancels on purge/empty-trash/delete-everything.
- Desktop: reminders via the tray with a clear "desktop reminders are best-effort" note, or hide the section.

**Play blockers**
- Adaptive launcher icon with monochrome layer, Play Store 512 px icon, themed splash (`windowSplashScreenBackground`), day/night window background, `supportsRtl`.
- `bundleRelease` in the runbook and CI; upload key rotated and out of the README.
- Cleartext restricted to private hosts (validation on the base-URL field plus a guard in the HTTP client); non-private hosts must be https.
- `ShareActivity` clamps input and asks once before its first AI refine.
- Version string sourced from Gradle (no more `APP_VERSION` constant).

**Acceptance for v1.7**: migration test green in CI; the export/wipe/import scenario passes; a recurring task rings on two consecutive days on the emulator with the app killed in between; a timezone change moves a reminder correctly; the app installs from an AAB via the internal track with its own icon.

### v1.8 — Polish (target: 4 weeks)

Theme: it looks and feels like a product on every screen size, and blind users can use it.

- **Accessibility**: labelled calendar cells (date, selected, today, has items, long-press label), `customActions` for every swipe (complete, delete, triage), headings on group and section titles, live regions for snackbars and AI status, labelled text fields, 48 dp targets everywhere, state descriptions on toggles, no colour-only signals.
- **Brand**: a seed colour and a full light/dark palette used when dynamic colour is off and on desktop; real M3 contrast tone sets; the six item colours remapped per theme; the swatch check tinted for contrast; note cards tinted from the scheme.
- **Layout**: window size classes; sheets and bar capped at ~640 dp and centred; adaptive notes grid; list-detail on expanded windows (Plan list + item, Ideas grid + editor); landscape phones keep the bottom bar.
- **States**: agenda loading skeleton; no empty-state flash; notes "no matches"; search idle state with recent searches and tags; differentiated AI errors (offline, Wi-Fi-only, bad key, quota, rate-limited, server) shared by Ask, Improve and refine; import error detail; API 31/32 banner logic.
- **Undo**: Detail and Event editor delete return to the list with an undo snackbar; Trash restore undoable.
- **Enable what's built**: language, tile, sound & vibration, diagnostics rows; notes grid/list toggle; "Open app to" actually honoured.
- **Desktop pass**: key bindings only when no text field has focus; Escape closes sheets; brand palette; hide voice/launch-at-login when unavailable; window minimum size and persistence; tray icon.
- **Onboarding**: a three-card first-run (capture, sort, reminders permission) that can be skipped in one tap; a what's-new sheet on upgrade.
- Remove dead strings and `PlaceholderTab`; plurals; resource-ise the subpage content.

**Acceptance**: TalkBack can capture, complete, delete, and triage without sight; Accessibility Scanner reports no touch-target or contrast errors on the five main screens; the app is usable on a Pixel Tablet and a 1200 dp desktop window; first launch explains itself.

### v1.9 — Pro (target: 4 weeks)

Theme: the business. See `MONETIZATION.md` for the model; this is the engineering.

- **Entitlements**: a `ProEntitlement` state (free / pro / trial) read from Play Billing, cached locally, verified on launch and purchase, with a developer override for the maintainer's own device.
- **Play Billing**: one-time "tina Pro" product plus a yearly subscription with a 7-day trial; purchase, restore, pending-purchase, grace and account-hold states; a paywall that shows the actual value (before/after), never a nag.
- **Pro features**: hosted AI (no key needed) through a relay that verifies purchase tokens, pins models, enforces quotas, and streams; unlimited Ask history; encrypted device-to-device sync (see below); custom themes and app icons; multiple widgets; Wear/desktop companions later.
- **AI hardening (free and Pro)**: context built once per chat and cached; Haiku for parse/improve; confirmation and batch cap for write actions; the DATABASE block delimited as data; streaming; retries; token/cost display for BYOK users; correct model ids; conversation restored after process death.
- **Sync groundwork**: `uuid`, `version`, tombstones on occurrences, dirty flags. Sync itself is a v2.0 candidate, not a promise.

**Acceptance**: a fresh install can buy Pro on the internal track with a license tester, restore it after reinstall, lose it when cancelled, and use hosted Ask within its quota; the maintainer's device is Pro without paying.

### v2.0.0 — Launch (target: 3 weeks after v1.9)

Theme: production on Google Play, and the machinery to keep shipping.

- CI (GitHub Actions): tests, lint, detekt, `bundleRelease` on tags, migration test, screenshot tests (Roborazzi) for the five main screens in light and dark.
- Crash visibility: at minimum Play vitals with symbolication uploaded; optionally an opt-in local crash log the user can share.
- R8 with resource shrinking and a real keep-rules file; app-specific baseline profile from capture, agenda scroll, and sheet journeys.
- Store listing complete: screenshots (phone 6.7", tablet 10"), feature graphic, promo video optional, localized listing in 3-5 languages once strings are translated.
- Closed testing (12 testers, 14 days) started during v1.8 so production access is granted by v2.0.
- Staged rollout 10 % → 50 % → 100 % with vitals thresholds.
- CHANGELOG.md generated from DECISIONS.md and tags; what's-new text per release.
- Optional if time allows: device-to-device sync over the user's own storage (Google Drive app folder / WebDAV) using the v1.9 groundwork.

**Definition of done for v2.0.0**
- No open P0/P1 from any audit.
- Migration, import/export, and reminder scenarios automated and green.
- Play policy items: privacy policy hosted, Data Safety accurate, exact-alarm form approved, target audience set, content rating done.
- Accessibility Scanner clean on main screens; TalkBack script passes.
- Frame-time budget held: 90th percentile ≤ 12 ms on a Pixel 9 for capture open/close, tab switch, settings round trip.
- Purchase, restore, and cancel flows verified with license testers.
- Rollback plan documented: previous AAB kept, migrations forward-only and reversible by backup.

## Backlog beyond 2.0 (not scheduled)

Natural-language time zones in capture ("3pm Berlin"), attachments on notes, calendar import/export (ICS), Wear OS tile, Tasks-style shared lists, per-tag colours, focus mode, location reminders, a web companion.

## Effort summary

| Release | Engineering | Testing/QA | Notes |
|---|---|---|---|
| v1.7 Trust | ~12 days | ~3 days | Highest risk; do first, ship to internal track |
| v1.8 Polish | ~15 days | ~5 days | Start closed testing at the end |
| v1.9 Pro | ~15 days | ~5 days | Relay service is a separate small deploy |
| v2.0 Launch | ~8 days | ~7 days | Mostly process and assets |

Solo developer, part-time evenings: roughly four months. Full-time: about eight weeks.

## Status after the overnight pass of 2026-09-02

Everything below is on `main`, built as an R8 release APK, and checked on the emulator (`docs/audits/*.md` has the findings that drove it; `CHANGELOG.md` has the user-facing list).

**v1.7 Trust: done**
- Data safety: migration guard + JVM migration test; backup v2 (uuid, trash, occurrences, chats, settings offered, one transaction, version check); atomic weekly auto-backup; backup rules; WAL checkpoint; API key in Keystore; `updatedAt` bumps; occurrence rows follow their item.
- Reminders: recurring tasks re-arm; `USE_EXACT_ALARM` + `setAlarmClock` fallback; re-arm on update/reboot/timezone/permission change; tap opens the item; Done ticks the day's occurrence and refreshes the widget; grouped with a summary; notification icon; `BigTextStyle`; digests fixed; alarms cancelled on purge.
- Play blockers: adaptive + monochrome icon, splash, night window background, RTL flag, R8 (69 → 12 MB), signing from env/properties, CI + tag workflow, cleartext limited to private hosts, share clamp, version from Gradle.
- Ask hardening: DATABASE block delimited as data, batch cap (10), confirmation card for deletions or >3 changes, send guard, safe reschedule, ordered undo.

**v1.7: not done**
- All-day events as epoch-day pairs (needs a migration and a day of testing on real time-zone moves).
- Quiet hours; inbox reminder on its own time; desktop reminder note.
- Auto-backup "last success" line and SAF folder choice.
- Share sheet's "ask once before the first AI refine".

**v1.8 Polish: done**
- A11y: day cells, rows, swipe actions, headings, labelled fields.
- Layout: sheets and bar capped at 640 dp; adaptive Ideas grid.
- States: agenda loading indicator; Ideas "no matches"; search Recent list; differentiated Ask errors; API 31/32 banner logic; swatch check contrast.
- Undo after page deletes; the four dormant settings rows enabled; reduce-motion honoured by the save burst; desktop keys stay out of text fields.

**v1.8: not done**
- Brand palette when dynamic colour is off; list-detail layouts on expanded windows; onboarding cards; what's-new sheet on upgrade; notes grid/list toggle; live regions; dead strings and plurals; desktop window persistence and tray icon; Accessibility Scanner run (needs a device).

**v1.9 Pro: groundwork done**
- `ProStore` with Play Billing 8, cached entitlement, acknowledge, restore, pending state, maintainer override; Settings → tina Pro paywall page; `rememberIsPro()` for the gates. Products are not created in Play Console yet, so the page says "not on sale yet" on any device today.

**v1.9: not done**
- The relay, hosted AI, the Pro features themselves and their gates, streaming, context caching, sync groundwork beyond `uuid`.

**v2.0: not done**
- Screenshot tests, detekt, baseline profile, crash visibility, listing screenshots, closed testing, rollout. The Play Console and policy steps that only the account owner can do are listed in `PLAY-LAUNCH.md` §8.
