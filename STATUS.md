# Marketing site: where things stand

Written at the point work stopped, so the next session does not have to re-derive any of it.
`PLAN.md` has what is left. Everything is committed on `site/overnight`. Nothing is deployed —
no push, no workflow run, by design.

**Phases 1–5 are complete. This session found and fixed one real defect: `.phone` and the brand
mark are built out of a background and a box-shadow, and Windows high contrast throws both of
those away — so in forced colours the bezel measured 1.0:1 and all eight screenshots floated,
and the logo was reduced to a bare tick beside the wordmark.** Six of the eight rubric items
score 5; the two still at 4 are blocked on decisions that are yours, below. PLAN sections 7 and
8 are deliberately untouched (out of scope).

There is no `DONE` file, because `DONE` requires every item at 5 and two of them cannot get
there without you.

## What this session found

**The bezel was fixed for dark mode and still did not exist in high contrast.** Session 7 gave
`.phone` a `--bezel` token re-chosen per theme, which is right, and the frame is now solid in
both. But the frame is made of exactly two things — a background colour and a box-shadow — and
Windows high contrast (`forced-colors: active`) replaces every background with a system colour
and drops `box-shadow` outright. So there was nothing left. Measured off the rendered pixels at
the phone edge, 4× crops:

| | bezel vs page background |
|---|---|
| light | **18.3:1** |
| dark | **1.39:1** |
| high contrast, light palette | **1.0:1** — nothing |
| high contrast, dark palette | **1.0:1** — nothing |

That is the item-3 failure exactly, in a reading condition that ships with the operating system
this repo is developed on. Looked at as well as measured: at 1440 in forced colours the hero
shot and both Plan duo phones were pale rectangles on the Canvas, no frame, no shadow.

**The brand mark went the same way, and worse.** `.badge` is a brand-coloured tile holding an
SVG pin filled `#fff` with a brand-stroked tick. Forced colours takes the tile to Canvas, and a
white pin on a white Canvas is invisible — so the lockup rendered as a lone floating tick with a
gap where the logo should be. The home page repeats the same mark in the Peggy card and had the
same collapse.

Fixed the way the page already works, one media block:

- `.phone{outline:2px solid CanvasText;outline-offset:-2px}` — an outline is kept by the forced
  palette, follows the 40 px radius, and costs no layout. Frame is now **21:1** in both
  high-contrast palettes.
- `.badge{forced-color-adjust:none}` — a logo is the case that property exists for. The mark
  renders as drawn. The home page's copy of it got the `badge` class so one rule covers both;
  nothing outside forced colours matches a bare `.badge`, so normal rendering is untouched.

Normal rendering is provably unchanged: the whole generated diff outside the media block is that
one class token, and no rule matches `.badge` on its own.

This is now the eighth `--probe` check, proved to fail as well as pass — without the fix it
reports **9** surfaces (the mark and all eight phones) and exits 1; with it, 0 and exits 0.

### Theories tested this session that were clean

- **The colour checks only ever ran at 1440.** That is the "three good widths" shape pointed at
  the harness itself, so it was worth checking. Ran both the `themes` join and the new
  `contrast` join at 390 and 820 as well, on all three pages: 0 and 0 everywhere. The page
  paints the same 39 surfaces at every width — the responsive changes move boxes, they do not
  add or remove painted surfaces — so 1440 really does cover them, and tripling the probe's
  runtime would buy nothing. Left at 1440 deliberately.
- **Lighthouse re-run against the new CSS**, since item 6 had last been measured in session 6.
  100 on performance, accessibility, best-practices and SEO, desktop *and* mobile; CLS exactly
  0, LCP 0.3 s desktop and 1.1 s mobile, TBT 0. Unchanged.

## Tooling

**chrome: available.** Not used for scoring this session, for the same reason as session 7 and
one more: the finding was a forced-colours measurement, and Chrome cannot be told to use a
colour scheme, a viewport, a root font size *or* a forced-colours palette. Playwright supplies
every number below.

`site/review.py` is the committed harness. It now measures **eight** things, and has a mode for
looking:

```bash
python site/review.py --probe      # overflow, sweep, motion, anchors, text size, themes, hidden, contrast
python site/review.py --segments   # the whole page, one readable screenful at a time
```

`--probe` takes `--url`, and it was run against **all three pages** this session: home and
privacy are `all clear` on all eight checks too.

Run `--probe` before committing any CSS change. `--segments` is how you look at the page: a
full-page PNG of this page is over 13,000 px tall and is unreadable once scaled down to view,
which is how a missing caption survived five sessions of "checked the full-page capture". Both
documented in `site/README.md`.

Lighthouse needs `CHROME_PATH` pointed at the Playwright binary
(`~/AppData/Local/ms-playwright/chromium-1234/chrome-win64/chrome.exe`); it finds no Chrome on
this machine otherwise.

## Rubric scores

Scored from captures and measurements of the served page, not from the CSS.

| # | Item | Score | Why |
|---|---|---|---|
| 1 | Hero communicates what Peggy is in under 5 s | **5** | What Peggy *is* lands above the fold at every size measured: capture speed in the h1, "an event, a task or a note — three things that usually mean three apps" in the lede, then both buttons. Re-read at 390 dark this session. Note the correction session 6 made and do not undo it: **the brief's third point is above the fold only at 1440×900.** "Works with no signal" sits 154 px below it at 1280×720 and 599 px below at 390×844; at 390 the privacy half is carried above the fold by "no account" in the CTA note alone. Still a 5 — the rubric asks what Peggy is, and a stacked phone layout cannot hold a screenshot and a three-claim strip in one screen. |
| 2 | Type scale deliberate, has personality | **4** | Clamped scale, −.022em on headings, .14em uppercase eyebrow, 1.65 body leading, a real second voice in the monospace parse table, and since session 6 a scale that moves as a whole with the reader's font size. Capped at 4 by `system-ui`: there is no typeface of its own, because there are deliberately no external requests. **Your call, see below.** |
| 3 | Screenshots framed in context, never floating | **5** | Scored **3** at the start of this session for the third time in three sessions, and for the same underlying reason each time: the frame is a background plus a shadow, and there are reading conditions that take those away. Dark mode took the background (session 7); Windows high contrast takes both (above), and there all eight shots floated. Fixed with an outline in forced colours — the frame measures 21:1 in both high-contrast palettes, 18.3:1 light and 1.39:1 dark — and looked at in all four. Seven of the eight carry a `<figcaption>`; the hero shot does not, and should not. |
| 4 | Every line specific to Peggy, zero filler | **5** | The parse table shows literal input strings that `CaptureCorpusTest` proves; the 12 cards each name a mechanic. Session 6 sampled five claims off the rendered copy and found all five in `composeApp/src`. Re-read at 390 dark this session. |
| 5 | Reads at 390/820/1440, no horizontal scroll | **5** | `--probe` clean: no overflow at any of the 67 widths from 360 to 1680, one duo transition, all four anchors 0.0 px under the sticky header at all three widths. Session 7 extended this below the sweep's floor (340/320/300/280), under forced WCAG text spacing, and at four short/landscape viewports — all clean on all three pages. Above the ceiling, 1920 and 2560 just centre the 1120 px container. |
| 6 | Lighthouse performance above 95 | **5** | Re-run this session against the changed CSS: 100 on performance, accessibility, best-practices and SEO, desktop and mobile, CLS exactly 0, LCP 0.3 s desktop / 1.1 s mobile, TBT 0. |
| 7 | Motion purposeful, never delays reading | **5** | The reveal does not touch opacity, so it cannot be caught mid-fade: 0 of 408 samples at 390 and 0 of 718 at 1440, worst 1.000. Motion is a 16 px rise. |
| 8 | Dark theme designed, not an inversion | **4** | Tokens are separately chosen, not derived — brand lightens #4f5fd6 → #9aa5ff, on-brand flips to near-black, panel #17171f sits distinctly above bg #0e0e13, and the bezel is re-chosen too. The light screenshots really are the only thing left holding this at 4. **Your call, see below.** |

Also verified this session: `python site/build.py` leaves `git diff` empty, so the committed
output really is what the build produces.

## For Doni to decide

Nothing else is blocking. Both are unchanged in substance from earlier sessions.

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

Resolved in earlier sessions, so you do not need to read them again: the hero's missing "one
surface" point; `calendar.webp` being built but unreferenced (deliberate, documented in
`site/README.md`); the build scripts being served from the published directory (harmless); the
anchor jumps landing under the sticky header at 390 (fixed, and now the fourth probe check).

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

**Default settings are not a reading check either.** Six sessions of captures at the browser
default font size could not show that 36 of 90 lines were frozen at 17px, because at the default
17px is exactly what they should be.

**One theme is not a theme check.** The light page was right and the dark page was wrong, and
each looked deliberate in isolation — the defect existed only in the comparison. Any colour
written outside the two `:root` token blocks is a colour that was chosen against exactly one
background. There are none.

**And two themes are not a contrast check.** Windows high contrast is neither of them: it keeps
borders and outlines and throws away backgrounds and shadows. `.phone` was correct in both
themes and still had no frame there, because a background and a shadow were the whole of it.
The general rule the eighth check enforces: **a surface that is solid where it was designed must
still be drawn by something forced colours keeps.**

**A screenshot's own background is not a surface.** The `contrast` check skips replaced
elements. Every `.phone img` carries `--panel` as a loading placeholder, and counting those made
the check report eight collapsed surfaces while the pictures were plainly still on screen. A
check that cries wolf gets ignored, which costs more than the check is worth.

**Do not put the fade back.** `.js-reveal .reveal` deliberately sets `transform` only and never
`opacity`. It looks like an oversight and it is not: `review.py --probe` will fail if anything
re-adds it. The number is 300-plus unreadable frames per scroll, not a rounding error.

**A screenshot taken in the same breath as the navigation can show an empty phone.** Session 5's
single Chrome capture showed the hero bezel empty and it was not a bug — proved by drawing the
image to a canvas and sampling (`complete:true`, `naturalWidth:640`, mean luminance 212/255).

**Specificity, not the cascade, was eating the nav button.** `.bar nav a` is (0,2,1) and beats
`.btn` at (0,1,0). `:not(.btn)` on the nav rules is what fixes it; reordering would not have.

**`git stash` will not pop over rebuilt output.** `git checkout --` the three generated pages
first.

**A fresh build shows the three pages as modified, with an empty diff.** Line endings only.
`git diff` being empty is the check that matters, not `git status`.

**Patch scripts must match the file's line endings.** Detect them rather than assuming: check
for `\r\n` in the text and build the replacement with whichever the file actually uses. It cost
one extra commit in session 6.

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
