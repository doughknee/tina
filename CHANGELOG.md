# Changelog

User-facing changes per release. Engineering rationale lives in `DECISIONS.md`; the plan in `docs/ROADMAP.md`.

## v1.8.3 (dev)

### Accessibility
- The calendar's weekday letters are read out in full, so "T" and "S" are no longer ambiguous.
- On multi-day views, a task's row, its checkbox and its details arrow say which day they belong to, so repeated titles are told apart out loud.
- The note editor's formatting buttons are large enough to hit reliably; the row scrolls when they don't all fit.

## v1.8.2 (internal testing, 2026-09-04)

### Ideas
- Checklists: type “[] ” at the start of a line, or use the new toolbar button, and the line becomes a box you can tick. Cards show the boxes, strike through what’s done, and say “2 of 6 done”. Tick items straight from the card.
- Share a note, or copy it as Markdown, from the editor menu. Checklists come out as task lists.

### Small things
- After an update, a short what’s-new sheet appears once per feature release. Developer options can show it again.
- The Sort reminder has its own time instead of riding the morning agenda.
- Auto-backup says when it last ran.
- Ask’s “Thinking…” line is announced to screen readers.
- 26 unused strings removed.

## v1.8.1 (internal testing, 2026-09-03)

- The seven-tap unlock for Developer options now sticks: it is saved, so the section shows in the hub and after a restart.

## v1.8.0 (internal testing, 2026-09-03)

### Ideas, redesigned
- Cards render by what they hold: a titled note, a bare thought with no bold headline, or a list with its first four items and "+2 items". The pin only shows on pinned notes.
- Pinned notes get their own section; the rest sit under "Everything else". Two columns on phones, adaptive on desktop.
- A tag rail under the title filters the grid. Hold a chip to open the tag. A tag with a pinned note is a project and gets an underline.
- Long-press a card to select; pin, colour, tag or delete several at once with one undo.
- Sort by last edited, created or title, and pick Grid, List or Large. Both stick.
- Search shows rows with the matching words highlighted and a count.
- The editor reads like paper: a wrapping title, "Edited 4 minutes ago · 38 words", room around the text, tags at the end, and a format bar that only appears while you type. Type "- ", "1. " or "# " at the start of a line for a bullet, a number or a heading. The menu duplicates a note or turns it into a task.
- A long one-line idea splits into a title and body at capture, so it reads as a note instead of a headline.

### A tag is the project
- Open any tag to see its overview note, open tasks, upcoming events and ideas together. Pin a note carrying the tag and it becomes the overview; unpin it and the tag is just a filter again.
- The bar at the bottom of a tag page captures straight into that tag.

### Small things
- A Developer section in Settings: show the first-run cards again, ask for a store review now, or reset the review ask. Always there in the dev build; in a Play build, tap Version seven times.
- Settings caught up with the app: “Open app to” now offers Plan, Sort, Ideas or the last screen and is honoured; the Sort reminder, day sections and completed-items rows say Plan and Sort; widget and shortcut descriptions match what ships; What’s new has a 1.8 entry.
- The onboarding Sort card describes the decisions page.
- The capture bar and nav bar ride the keyboard cleanly again after you dismiss the suggestions by dragging or tapping outside. They used to climb out from under it on the next open.
- Emptying the trash can be undone for a few seconds, like everything else.
- The week view says "4 more repeats" instead of "4 occurrences hidden".

### Widgets
- The Today widget has a capture button, tapping a row opens that item, and it refreshes itself every half hour so it rolls over at midnight without opening the app.
- Both widgets show a real preview in the widget picker instead of the app icon.

### Capture understands more
- Repeats: every 3 days, every 2 weeks, every mon and wed, every weekend, every morning, every 15th, daily, weekly on friday.
- Times: 2-4pm ranges (start plus length), 3 p.m., 5.30pm, in an hour, in half an hour, end of day, first thing, tomorrow night.
- Dates: this friday, the 15th, 9/15/26, 2026-09-15, september 15th 2027, this weekend, day after tomorrow.
- "urgent" and "asap" set high priority. A leading "the" stays in the title.

### Sort is now the decisions page
- Everything that needs a call from you, grouped: new captures with no date, overdue tasks, snoozed reminders, and every someday item. Somedays live here until they get a date; Plan no longer shows them.
- Each group has its own one-tap answers (Today, Done, Keep, Drop, and the rest); swipes stay, and everything is undoable.
- A capture with no date now lands on Sort instead of today's list. Settings → Capture turns that off.
- The tab badge and Plan's "to sort" card count new, overdue and snoozed. Someday is a section you scroll to, not a number that nags.
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
