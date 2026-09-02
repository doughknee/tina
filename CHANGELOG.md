# Changelog

User-facing changes per release. Engineering rationale lives in `DECISIONS.md`; the plan in `docs/ROADMAP.md`.

## v1.8.0 (dev)

### Sort is now the decisions page
- Everything that needs a call from you, grouped: new captures with no date, overdue tasks, snoozed reminders, and someday items untouched for 30 days.
- Each group has its own one-tap answers (Today, Done, Keep, Drop, and the rest); swipes stay, and everything is undoable.
- A capture with no date now lands on Sort instead of today's list. Settings → Capture turns that off.
- The tab badge and Plan's "to sort" card count every decision owed, not just the inbox.
- The overdue nudge opens Sort.

## v1.7.3 (closed testing)

- All-day events stay on their date when you change time zone. They used to slide a day.
- Quiet hours (Settings → Notifications): reminders due in the window ring when it ends. Digests keep the times you chose.
- A proper Peggy palette when dynamic colour is off, and on desktop: the launcher blue as the seed of a full light and dark scheme instead of the Material defaults.

## v1.7.2 (internal testing)

- New icon: a pin with the check inside, on the brand blue. Themed (monochrome) icon, splash, Play icon, feature graphic and the website use the same glyph.

## v1.7.1 (internal testing)

- First-run cards: capture, sort, reminders (with the notification permission), skippable in one tap.
- Peggy asks for a Play rating once, after the twentieth capture, never before.

## v1.7 "Trust" (1.7.0, first Play internal build)

### Reminders
- Repeating tasks ring for every occurrence, not just the first.
- Reminders fire exactly on time even without the exact-alarm grant (alarm-clock fallback), and survive app updates, reboots and time-zone changes.
- Tapping a reminder opens the item. Done ticks off today's occurrence of a repeating item. Several reminders stack under one line.
- The daily agenda lists only today's events; the overdue nudge time is editable.

### Never lose anything
- Backups now include the trash, habit history (which days you ticked) and Ask conversations. Importing a backup twice changes nothing.
- Restoring a backup no longer overwrites your settings silently; it offers to.
- The app can no longer wipe its database on a botched update.
- Your AI key is encrypted on the device and never written to a backup or cloud backup.
- Weekly auto-backups are written atomically.

### Play readiness
- The app is now called Peggy. Same data format: a tina backup imports into Peggy unchanged.
- Real launcher icon (with themed icon), themed splash, no white flash in dark mode.
- Release builds are 6× smaller (11.7 MB from 69 MB).
- Shared text is capped at 5,000 characters; AI endpoints over plain http are limited to your own network.
- Version shown in About is the build's version.

### Ask
- Failures say what went wrong and where to fix it (AI off, no model, key rejected, model not found, rate limit, Wi-Fi only, provider error) instead of one generic line.
- Continuous integration builds and tests every push; tags build a signed bundle.

### tina Pro (groundwork)
- Settings → tina Pro: the paywall page with the three plans, the free trial called out, Restore purchases and Manage subscription. Nothing is gated yet; the Pro features arrive with v1.9.

### Everywhere
- Sheets and the capture bar stop growing at 640 dp on tablets and desktop; the Ideas grid adds columns as the window widens.
- Day cells, rows and swipe actions are announced by screen readers, and rows expose their actions to TalkBack.
- Deleting from an item's page shows Undo on the page you return to.

## v1.6 — Swipe to sort, settings hub, series editing (2026-09-02)
See the GitHub release notes.

## v1.5 — Plan, Sort, Ideas (2026-09-02)
See the GitHub release notes.
