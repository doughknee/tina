# Marketing site: where things stand

Written at the point work stopped, so the next session does not have to re-derive any of it.
`PLAN.md` has what is left. Everything is committed on `site/overnight`. Nothing is deployed —
no push, no workflow run, by design.

**Phases 1–4 are complete. Phase 5 (the final rubric pass) ran this session and found two real
defects, both now fixed. Six of the eight rubric items score 5; the two still at 4 are both
blocked on a decision that is yours, below.** PLAN sections 7 and 8 are deliberately untouched
(out of scope).

There is no `DONE` file, because `DONE` requires every item at 5 and two of them cannot get
there without you.

## What this session found

Two items that were recorded as 5 were not, and both were caught by looking rather than by
reading CSS. Neither was a regression — both had been on the page for every previous session.

1. **Four of the eight screenshots had no caption.** The `.pair` duos in Plan and Ideas render
   through `phone()`, which emits no `<figcaption>`; only the three single shots used
   `figure()`. The captions were already written in `screenshots.json` and were already good.
   Worst at 390, where the duo stacks: two near-identical phones on empty background, a full
   screen below the heading that explains them, with nothing saying which is the day view and
   which the week. Fixed by rendering them through the existing `figure()` helper.

2. **The phone duo reflowed into a tower from 900 to 1060.** `.row-grid` goes two-column at
   900, but the shot column is 404 px there and does not fit two 228 px phones until 1080. So
   across that whole band the duo stacked: a 1073 px tower of two phones beside a ~350 px text
   block. **1024 is a common laptop width and it looked broken.** Fixed with `20.5vw` in that
   band only.

The second one is the more useful lesson, and it is now enforced rather than remembered: **390,
820 and 1440 all passed while every width between 900 and 1060 was visibly wrong.** Breakpoints
put the bugs *between* the widths you test. `review.py --probe` now sweeps 360 → 1680 in 20 px
steps.

## Tooling

**chrome: unavailable.** `tabs_context_mcp` was present this session, unlike sessions 1–3 — it
navigated and took one screenshot, then `Page.captureScreenshot` timed out after 30 s and left
the tab at a broken zoom. Per the protocol that is an error, so the tab was closed and
everything below is Playwright: headless Chromium, `networkidle` plus 500 ms, both
`prefers-color-scheme` values. Not retried.

`site/review.py` is the committed harness. It now measures **four** things:

```bash
python site/review.py --probe    # overflow, sweep, motion, hidden; exits non-zero on failure
```

Run it before committing any CSS change. Documented in `site/README.md`.

Lighthouse needs `CHROME_PATH` pointed at the Playwright binary
(`~/AppData/Local/ms-playwright/chromium-1234/chrome-win64/chrome.exe`); it finds no Chrome on
this machine otherwise.

## Rubric scores

Scored from captures and measurements of the served page, not from the CSS.

| # | Item | Score | Why |
|---|---|---|---|
| 1 | Hero communicates what Peggy is in under 5 s | **5** | All three of the brief's ten-second points land above the fold — capture speed in the h1, "an event, a task or a note — three things that usually mean three apps" in the lede, no-account/no-signal in the claims strip. Re-checked at 1440, 880, 760, 640 and 390. |
| 2 | Type scale deliberate, has personality | **4** | Clamped scale, −.022em on headings, .14em uppercase eyebrow, 1.65 body leading — chosen, not defaults. Capped at 4 by `system-ui`: there is no typeface of its own, because there are deliberately no external requests. **Your call, see below.** |
| 3 | Screenshots framed in context, never floating | **5** | Raised from a real 4 this session. All eight shots now sit in a bezelled `.phone` with a shadow **and** a caption; the four in the two duos were rendering without one. |
| 4 | Every line specific to Peggy, zero filler | **5** | The parse table shows literal input strings; the 12 cards each name a mechanic. Re-verified the Sort section against its own screenshot this session: the alt and the "One tap per answer" bullet both name New/Overdue/Snoozed and the Today/Someday/Tomorrow/This week chips, and that is exactly what the shot shows. |
| 5 | Reads at 390/820/1440, no horizontal scroll | **5** | Raised from a real 4. No overflow at any of the 67 widths from 360 to 1680, and the duo now has exactly one side-by-side/stacked transition across the whole range instead of three. |
| 6 | Lighthouse performance above 95 | **5** | 100 desktop and 100 mobile, re-run on the final build. Both also score 100 on accessibility, best-practices and SEO. CLS exactly 0. |
| 7 | Motion purposeful, never delays reading | **5** | The reveal does not touch opacity, so it cannot be caught mid-fade: 0 of 718 samples at 1440 and 0 of 408 at 390. Motion is a 16 px rise. |
| 8 | Dark theme designed, not an inversion | **4** | Tokens are separately chosen — brand lightens #4f5fd6 → #9aa5ff, on-brand flips to near-black, panel and line are picked not derived. Held at 4 because all eight app screenshots are light-theme, so the dark page shows light phones. **Your call, see below.** |

Also verified this session, all three pages: one `<h1>` each, no heading-level skips, every
image carries alt text and explicit `width`/`height`, skip link and `main#main` present. The FAQ
was opened and captured — the answers read correctly and there is no overflow with every
`<details>` open. `python site/build.py` leaves `git diff` empty, so the committed output really
is what the build produces.

## For Doni to decide

Both of these are what stands between a 4 and a 5, and each is a judgement call I should not
make for you. Nothing else is blocking.

1. **A typeface (item 2).** `system-ui` caps this at 4 — it is a deliberate choice, but it is by
   definition the least distinctive one. A self-hosted face would lift it and would not break
   the no-external-requests promise (no CDN), at the cost of bytes, and of some of the restraint
   the brief asked for. Choosing a face for your product is a taste decision, so I left it.
2. **Dark-theme screenshots (item 8).** Peggy has a real dark theme — the feature grid card
   "Material You" even advertises it — and the site never shows it. This is *mechanically*
   possible without the banned `demo_data.py`: `emulator-5554` is up and still holds the demo
   data, so `screenshots.py` could re-shoot with `adb shell cmd uimode night yes`. I did not do
   it, for three reasons: it doubles the committed image set and needs a `<picture>` per shot,
   it is the largest change to make unattended, and it is a design question rather than a bug.
   I looked at the Capture, Sort, Plan, Ideas and Tags sections at 1440 dark again this session
   and the bezel does contain the light phones — it reads as deliberate, not broken.

Resolved in earlier sessions, so you do not need to read them again: the hero's missing "one
surface" point; `calendar.webp` being built but unreferenced (deliberate, documented in
`site/README.md`); the build scripts being served from the published directory (harmless).

## Gotchas already paid for

Do not rediscover these. The originals stand — the accessibility dump lying mid-animation, the
agenda mid-compose race, the calendar being a persisted toggle, demo mode needing three separate
corrections, ASCII-only `print()` in `build.py`, Pillow being absent on CI, `site/demo_data.sql`
being generated, and the two in `site/README.md` (lazy images compositing blank, and colour
being unjudgeable from a scaled capture).

**Three good widths are not a responsive check.** 390, 820 and 1440 passed for six sessions
while 900–1060 was visibly broken. `--probe` sweeps the range now; do not go back to sampling.

**Do not put the fade back.** `.js-reveal .reveal` deliberately sets `transform` only and never
`opacity`. It looks like an oversight and it is not: `review.py --probe` will fail if anything
re-adds it. The number is 300-plus unreadable frames per scroll, not a rounding error.

**Specificity, not the cascade, was eating the nav button.** `.bar nav a` is (0,2,1) and beats
`.btn` at (0,1,0). `:not(.btn)` on the nav rules is what fixes it; reordering would not have.

**`git stash` will not pop over rebuilt output.** `git checkout --` the three generated pages
first.

**A fresh build shows the three pages as modified, with an empty diff.** Line endings only.
`git diff` being empty is the check that matters, not `git status`.

**Lighthouse finds no Chrome here.** Export `CHROME_PATH` to the Playwright Chromium. It also
now ends every run with `EPERM ... destroyTmp` while deleting its temp profile — that is cleanup
*after* the report is written, so check for the JSON before believing the error.

## Environment notes

- Emulator `emulator-5554` holds the demo data and the 1.8.3 dev build (`com.peggy.app.dev`).
  `site/demo_data.py` is destructive to that app on that device. It was **not** run this
  session, and neither was `screenshots.py`; the eight PNGs are untouched inputs.
- The phone (`10.205.0.144:46743`) is attached and still has two stray captures from earlier
  automation, a ".,vprobe" item in Sort and an "Untitled" note in Ideas. Harmless, safe to
  delete by hand. Nothing this session touched it.
- A static server may still be running on port 8899 (`python -m http.server 8899` started in
  `site/`). A second one on 8898 served a copy of the pre-change page for the before/after
  sweep; stop either if it is in the way.
