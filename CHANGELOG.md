# Changelog

User-facing changes per release. Engineering rationale lives in `DECISIONS.md`; the plan in `docs/ROADMAP.md`.

## Unreleased (v1.7 "Trust", in progress)

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
- Real launcher icon (with themed icon), themed splash, no white flash in dark mode.
- Release builds are 6× smaller (11.7 MB from 69 MB).
- Shared text is capped at 5,000 characters; AI endpoints over plain http are limited to your own network.
- Version shown in About is the build's version.
- Continuous integration builds and tests every push; tags build a signed bundle.

## v1.6 — Swipe to sort, settings hub, series editing (2026-09-02)
See the GitHub release notes.

## v1.5 — Plan, Sort, Ideas (2026-09-02)
See the GitHub release notes.
