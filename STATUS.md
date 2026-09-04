# Marketing site: where things stand

Written at the point work stopped, so the next session does not have to re-derive any of it.
`PLAN.md` has what is left. Everything is committed on `site/overnight`. Nothing is deployed —
no push, no workflow run, by design.

**Phases 1–5 are complete. This session found and fixed one real defect: every in-page link on
the page landed its target underneath the sticky header at 390. Six of the eight rubric items
score 5; the two still at 4 are both blocked on a decision that is yours, below.** PLAN sections
7 and 8 are deliberately untouched (out of scope).

There is no `DONE` file, because `DONE` requires every item at 5 and two of them cannot get
there without you.

## What this session found

**Anchor jumps landed under the sticky header, at 390 only.** The header is
`position:sticky;top:0` and 65 px tall, and nothing in the CSS set `scroll-margin-top` or
`scroll-padding-top`. So following any in-page link put the top of the target underneath it:

| link | who follows it | hidden at 390 |
|---|---|---|
| `#main` | the skip link — keyboard and screen-reader users | 17 px of the `<h1>` |
| `#features` | the nav | 8.5 px of the "EVERYTHING ELSE" eyebrow |
| `#how` | the hero's "See how it works" | 9 px of the "CAPTURE" eyebrow |
| `#privacy` | the nav | none — its section opens with a padded panel |

820 and 1440 were clean, which is why six sessions of scoring never saw it. This is the same
shape of miss as the 900–1060 reflow: **the widths that pass are not evidence about the one that
does not.**

Fixed with one line, `html{scroll-padding-top:80px}`, rather than a `scroll-margin-top` on each
target, so an anchor added later is covered without anyone remembering to. Verified by looking
as well as measuring: at 390 "See how it works" now lands with the eyebrow and the full heading
clear of the bar.

It is now the fifth `--probe` check, and it was proved to fail as well as pass — with the rule
removed it reports 17.0/8.5/9.0 px and exits 1, with it in place 0.0 px at all three widths and
exits 0. The check collects the links **from the page** rather than from a list, because the nav
writes `/peggy/#features`, not `#features`; a list of hrefs beginning with `#` finds two of the
four anchors and misses the two most people actually click.

## Tooling

**chrome: unavailable.** It got further this session than in any previous one — `tabs_context_mcp`
created a group, navigated, and returned one screenshot — and then `Page.captureScreenshot` timed
out after 30 s on the very next call, exactly as in session 4. Per the protocol that is an error,
so the tab was closed and everything below is Playwright. Not retried.

**Do not chase the blank hero phone.** That single Chrome screenshot showed the hero bezel empty,
which looks precisely like a real bug and is not one: it was taken before first paint. Proved it
by drawing the image to a canvas and sampling — `complete:true`, `naturalWidth:640`, average
luminance 212 out of 255. The picture is there. Any capture taken in the same breath as the
navigation can show an empty phone.

`site/review.py` is the committed harness. It now measures **five** things, and has a mode for
looking:

```bash
python site/review.py --probe      # overflow, sweep, motion, anchors, hidden; non-zero on failure
python site/review.py --segments   # the whole page, one readable screenful at a time
```

Run `--probe` before committing any CSS change. `--segments` is how you look at the page: a
full-page PNG of this page is over 13,000 px tall and is unreadable once scaled down to view,
which is how a missing caption survived five sessions of "checked the full-page capture". Three
separate sessions rebuilt a segmented capture in a scratchpad and threw it away; it is in
`review.py` now. Both documented in `site/README.md`.

Lighthouse needs `CHROME_PATH` pointed at the Playwright binary
(`~/AppData/Local/ms-playwright/chromium-1234/chrome-win64/chrome.exe`); it finds no Chrome on
this machine otherwise.

## Rubric scores

Scored from captures and measurements of the served page, not from the CSS. Every item below was
re-checked this session at 390, 820 and 1440 in both themes.

| # | Item | Score | Why |
|---|---|---|---|
| 1 | Hero communicates what Peggy is in under 5 s | **5** | All three of the brief's ten-second points land above the fold — capture speed in the h1, "an event, a task or a note — three things that usually mean three apps" in the lede, no-account/no-signal in the claims strip and again in the CTA note. Re-checked at 1440, 820 and 390. |
| 2 | Type scale deliberate, has personality | **4** | Clamped scale, −.022em on headings, .14em uppercase eyebrow, 1.65 body leading, and a real second voice in the monospace parse table — chosen, not defaults. Capped at 4 by `system-ui`: there is no typeface of its own, because there are deliberately no external requests. **Your call, see below.** |
| 3 | Screenshots framed in context, never floating | **5** | All eight shots sit in a bezelled `.phone` with a shadow and a caption. Confirmed by looking at the stacked duo at 390, which is where the four missing captions were caught last session: each phone now carries its own label. |
| 4 | Every line specific to Peggy, zero filler | **5** | The parse table shows literal input strings that `CaptureCorpusTest` proves; the 12 cards each name a mechanic. Re-read at 390, where the table is narrowest, and it still reads as input → result. |
| 5 | Reads at 390/820/1440, no horizontal scroll | **5** | Raised from a real 4 this session. No overflow at any of the 67 widths from 360 to 1680, the duo has exactly one side-by-side/stacked transition across the range, and — new — all four in-page anchors now land clear of the sticky header at all three widths instead of three of them landing under it at 390. |
| 6 | Lighthouse performance above 95 | **5** | Re-run on the changed build: 100 desktop and 100 mobile, and 100 on accessibility, best-practices and SEO on both. CLS exactly 0, LCP 1.1 s mobile. |
| 7 | Motion purposeful, never delays reading | **5** | The reveal does not touch opacity, so it cannot be caught mid-fade: 0 of 718 samples at 1440 and 0 of 408 at 390. Motion is a 16 px rise. |
| 8 | Dark theme designed, not an inversion | **4** | Tokens are separately chosen, not derived — brand lightens #4f5fd6 → #9aa5ff, on-brand flips to near-black, panel #17171f sits distinctly above bg #0e0e13. Held at 4 because all eight app screenshots are light-theme, so the dark page shows light phones. **Your call, see below.** |

Also verified this session: `python site/build.py` leaves `git diff` empty, so the committed
output really is what the build produces; every internal link and every fragment on all three
pages resolves (0 broken); and the home and privacy pages were read at 390 in both themes rather
than only swept — both are clean.

## For Doni to decide

Both of these are what stands between a 4 and a 5, and each is a judgement call I should not
make for you. Nothing else is blocking. They are unchanged from the last two sessions.

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
   Looking at the dark captures again this session, the bezel does contain the light phones and
   it reads as deliberate rather than broken — but it is the one thing on the page that a
   designer would call out.

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
while 900–1060 was visibly broken, and passed again for six while three of the four anchors
landed under the header at 390. `--probe` sweeps the range and follows every anchor now; do not
go back to sampling.

**Do not put the fade back.** `.js-reveal .reveal` deliberately sets `transform` only and never
`opacity`. It looks like an oversight and it is not: `review.py --probe` will fail if anything
re-adds it. The number is 300-plus unreadable frames per scroll, not a rounding error.

**A screenshot taken in the same breath as the navigation can show an empty phone.** Above.

**Specificity, not the cascade, was eating the nav button.** `.bar nav a` is (0,2,1) and beats
`.btn` at (0,1,0). `:not(.btn)` on the nav rules is what fixes it; reordering would not have.

**`git stash` will not pop over rebuilt output.** `git checkout --` the three generated pages
first.

**A fresh build shows the three pages as modified, with an empty diff.** Line endings only.
`git diff` being empty is the check that matters, not `git status`.

**Lighthouse finds no Chrome here.** Export `CHROME_PATH` to the Playwright Chromium. It also
ends every run with `EPERM ... destroyTmp` while deleting its temp profile — that is cleanup
*after* the report is written, so check for the JSON before believing the error.

## Environment notes

- Emulator `emulator-5554` holds the demo data and the 1.8.3 dev build (`com.peggy.app.dev`).
  `site/demo_data.py` is destructive to that app on that device. It was **not** run this
  session, and neither was `screenshots.py`; the eight PNGs are untouched inputs.
- The phone (`10.205.0.144:46743`) is attached and still has two stray captures from earlier
  automation, a ".,vprobe" item in Sort and an "Untitled" note in Ideas. Harmless, safe to
  delete by hand. Nothing this session touched it.
- A static server may still be running on port 8899 (`python -m http.server 8899` started in
  `site/`). It was already up at the start of this session and was reused.
