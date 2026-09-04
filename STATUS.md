# Marketing site: where things stand

Written at the point work stopped, so the next session does not have to re-derive any of it.
`PLAN.md` has what is left. Everything is committed on `site/overnight`. Nothing is deployed —
no push, no workflow run, by design.

**Phases 1–5 are complete. This session found no defect.** Session 10 tested five more
conditions, all clean, written up below so nobody spends an eleventh session on them. Six of
the eight rubric items score 5; the two still at 4 are blocked on decisions that are yours,
below. PLAN sections 7 and 8 are deliberately untouched (out of scope).

There is no `DONE` file, because `DONE` requires every item at 5 and two of them cannot get
there without you.

## What session 8 found and fixed (kept: the reasoning still applies)

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

## What session 9 tested, all clean

Five conditions, each of them the intersection of two things that had only ever been checked
apart. That intersection is where the last five defects lived, so they were worth the run; this
time there was nothing there.

- **The reader's own font size against the sticky header.** The text-size check sets the root to
  24px and looks at font sizes and scrollWidth; the anchor check follows every jump at the
  default size. Neither had ever run at the same time, and `.bar` is a hardcoded `height:64px`
  while everything inside it is `rem` — so the bar looked like it should burst and take the
  80px `scroll-padding-top` with it. It does not: at root 24 the header still measures 65px,
  nothing inside it paints outside its box, and all four anchors land **0.0px** clear at 390,
  820 and 1440. Checked on all three pages.
- **Surfaces the tokens cannot reach in dark mode.** Scrollbars, form controls and the
  `<details>` markers are drawn by the browser, not by the two `:root` blocks, so a light
  scrollbar on the dark page would be exactly "an inversion". `:root` declares
  `color-scheme:light dark`, so they follow the theme. Nothing to fix.
- **The sticky header in forced colours.** It is skipped by both colour checks on purpose (it is
  translucent by design), and forced colours keeps its 82% alpha, so page content does read
  through it. Measured the empty strip of the bar between the wordmark and the nav, at 2×,
  scrolled: **1.24–1.55:1** of ghosting in forced colours against **1.21–1.62:1** in normal
  rendering — the same bar, not a worse one. Session 7 judged the frosted bar working and it
  still is. `header.stuck` gets its bottom border in all four palettes (forced colours forces it
  to CanvasText), so the bar keeps a hard edge where it matters.
- **390 in forced colours**, the mobile high-contrast read. 1440 forced was looked at in session
  8 and 390 has only ever been looked at in the two normal themes. Walked all 17 screenfuls in
  both forced palettes: header edge, hero, both outlined buttons, the outlined phone from
  session 8's fix, the parse table with its rows separated, the feature grid, all six FAQ rows
  and the closing CTA. Everything reads.
- **Whether forced colours dims the screenshots.** They looked washed out in the 390 forced
  crop. They are not: mean luminance **0.708** and 5th–95th percentile **0.527–0.957** in
  light, dark, forced light and forced dark — identical to three decimal places. That is the
  scaled-capture trap for the third time in nine sessions; the crop was downscaled to be looked
  at, not the page.

## What session 10 tested, all clean

Five more conditions, each one a gap in the harness itself rather than a gap in the page --
somewhere a check is structurally unable to see. None of them was hiding a defect.

- **The skip link in forced colours.** `.skip:focus` is a brand background plus a `box-shadow`
  and nothing else, which is the exact shape of the session-8 bezel defect, and no check can see
  it because the link sits at `left:-9999px` until it is focused. It survives anyway:
  `:focus-visible` also matches it, and its `outline:3px solid` is kept by the forced palette and
  forced to a system colour. Tabbed to it and looked at all four palettes -- opaque box, hard
  outline, legible label in every one.
- **The FAQ answers, which no surface check has ever seen.** All six `<details>` are closed by
  default, and `SURFACE_JS` drops anything under 24px, so a closed answer has no rect and is
  skipped by both the themes join and the contrast join. Opened all six and re-ran everything:
  no overflow at any of 12 widths from 320 to 1680, and **39 surfaces open, 39 closed** -- the
  answers paint no background of their own, so what those checks were missing was nothing. The
  text-size check was never blind here at all: it reads computed styles, which exist on a closed
  `<details>`, and the count is 112 either way, 0 frozen.
- **Whether the links resolve.** Nine sessions checked the four in-page anchors and never once
  checked that a link goes anywhere. Enumerated every `href` on all three pages: the five local
  targets all return 200 against the server, the three canonicals and the three `sitemap.xml`
  entries agree with the three real pages, and the CTA is
  `https://play.google.com/apps/testing/com.peggy.app` in all eight places it appears.
- **Whether an anchor jump animates.** The anchor check measures where a jump *lands*, never how
  long it takes to get there, so a smooth scroll over a 13,000px page would pass it while doing
  precisely what item 7 forbids. `scroll-behavior` computes to `auto` and the jump is instant:
  sampled every frame for 1.2s, **0 changing frames** at 390 and 1440. The nav on this page uses
  absolute hrefs (`/peggy/#features`), so they were also checked for a re-fetch: **0 document
  requests and 0 load events**, same-document navigation, no flash.
- **An inner box that scrolls sideways.** The sweep reads `documentElement.scrollWidth`, which an
  `overflow-x:auto` wrapper hides from by clipping -- the page would report no overflow while the
  reader still got a sideways scrollbar on the parse table. Walked every element on all three
  pages at 390, 820 and 1440 with the FAQ open: **nothing** has `scrollWidth` past its
  `clientWidth`. There is no such wrapper to hide behind.

### Tested and deliberately not pursued: print

`@media print` does not exist, so print media computes identically to screen, and the item-3
failure shape suggested itself a fifth time -- backgrounds and `box-shadow` are exactly what a
browser drops when "Background graphics" is off, and session 8's outline fix is scoped to
`@media (forced-colors: active)` so it would not apply. Two PDFs were generated, with background
graphics on and off; the difference is **11 rect fills out of 78**, so the frames are not
wholesale removed. **I could not look at the rendered pages** -- there is no PDF renderer on this
machine (no poppler, no PyMuPDF, no Ghostscript) and installing one unattended was not worth it.
Left there on purpose: the rubric scores "a screenshot of the served page", and print is not one
of its reading conditions. If you ever want it judged, that is the missing tool.

### Theories tested in session 8 that were clean

- **The colour checks only ever ran at 1440.** That is the "three good widths" shape pointed at
  the harness itself, so it was worth checking. Ran both the `themes` join and the new
  `contrast` join at 390 and 820 as well, on all three pages: 0 and 0 everywhere. The page
  paints the same 39 surfaces at every width — the responsive changes move boxes, they do not
  add or remove painted surfaces — so 1440 really does cover them, and tripling the probe's
  runtime would buy nothing. Left at 1440 deliberately.
- **Lighthouse re-run against the new CSS**, since item 6 had last been measured in session 6.
  100 on performance, accessibility, best-practices and SEO, desktop *and* mobile; CLS exactly
  0, LCP 0.3 s desktop and 1.1 s mobile, TBT 0. Unchanged.
- **The copy nobody had read: alt text, captions and the meta description.** Item 4 had only
  ever been scored on visible copy. All eight `alt` strings describe what is actually on the
  screen in that shot — "The Plan tab showing a week, with a daily habit rolled into a single
  row that tracks how many days are done" — and every one is 108–122 characters, so a screen
  reader user gets the same specifics a sighted reader gets. Captions and the meta description
  are the same. Nothing generic, nothing filler.
- **`capture.webp` is used twice**, in the hero and again in the Capture section. Not a defect
  on inspection: the hero uses it uncaptioned as the lead image, and the Capture section uses
  the same moment as the evidence for the parse table directly above it, captioned "What Peggy
  understood, before you hit save." It is the app's signature moment, shown once as a picture
  and once as a proof. Left alone. (`calendar.webp` is still built and unreferenced, which is
  deliberate and documented in `site/README.md`.)
- **The motion check runs at 390 and 1440 but not 820**, the rubric's third width. Sampled it
  there by hand: 0 of 737 mid-fade, worst 1.000. Structurally it cannot be otherwise — the
  reveal sets `transform` and never `opacity` at any width — so the probe was left alone rather
  than made a third slower for a value that is 1.000 by construction.
- **Looked at 820 light**, which had been measured for overflow and anchors but never read. The
  hero stacks below the 900 px grid breakpoint: heading, lede, both buttons, the note, then the
  framed phone. Type scale and framing both hold.

### Cycle 2: the screenshots themselves, and a look

- **Every served WebP still matches its PNG source.** The eight PNGs are treated as untouchable
  inputs and nobody had ever checked that what the page actually serves still matches them -- a
  stale WebP would show an out-of-date app in the one place the brief calls the hero. Compared
  all eight, source against served, upscaled back to 1080x2400: mean absolute difference
  **1.48-2.38** of 255, which is lossy WebP plus the downscale to 640 and nothing more. None is
  stale.
- **And they cannot go stale**, which is the better answer: with Pillow present `process_images`
  re-encodes unconditionally on every run, so `site/README.md`'s headline instruction (drop a PNG
  over the old one, run the build, commit) is sound. The reuse-the-existing-WebP branch only runs
  when Pillow is absent, which is exactly what the PLAN 5.2 CI step covers.
- **Looked at the hero at 1440 light and 390 dark**, to ground this session's scores in a capture
  rather than in inherited ones. Both read: headline, lede, both buttons, the note, the framed
  phone, and at 1440 the claims strip beginning below the fold line. The 390 dark capture is also
  the clearest statement of the item-8 ceiling there is -- a light-mode app screenshot sitting on
  a properly designed near-black page, which is the decision below, not a bug.

## Tooling

**chrome: available.** `tabs_context_mcp` answered without error again in session 9 (no tab group; offers to create one). Not used for scoring, for the same reason as session 7 and
one more: the finding was a forced-colours measurement, and Chrome cannot be told to use a
colour scheme, a viewport, a root font size *or* a forced-colours palette. Playwright supplies
every number below.

`site/review.py` is the committed harness. It now measures **eight** things, and has a mode for
looking:

```bash
python site/review.py --probe      # overflow, sweep, motion, anchors, text size, themes, hidden, contrast
python site/review.py --segments   # the whole page, one readable screenful at a time
```

`--probe` takes `--url`, and it was run against **all three pages** in session 9 as well: home
and privacy are `all clear` on all eight checks too. A fresh `python site/build.py` still
leaves `git diff` empty.

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
| 3 | Screenshots framed in context, never floating | **5** | Scored **3** at the start of this session for the third time in three sessions, and for the same underlying reason each time: the frame is a background plus a shadow, and there are reading conditions that take those away. Dark mode took the background (session 7); Windows high contrast takes both (above), and there all eight shots floated. Fixed with an outline in forced colours — the frame measures 21:1 in both high-contrast palettes, 18.3:1 light and 1.39:1 dark — and looked at in all four. Seven of the eight carry a `<figcaption>`; the hero shot does not, and should not. Session 9 walked all 17 screenfuls at 390 in both forced-colours palettes — the outline holds there too — and measured the shots themselves as pixel-identical in luminance across all four palettes. |
| 4 | Every line specific to Peggy, zero filler | **5** | The parse table shows literal input strings that `CaptureCorpusTest` proves; the 12 cards each name a mechanic. Session 6 sampled five claims off the rendered copy and found all five in `composeApp/src`. Re-read at 390 dark and 820 light this session, and this session read the copy nobody had scored: all eight `alt` strings are 108-122 characters describing what is actually on that screen, and the captions and meta description match. |
| 5 | Reads at 390/820/1440, no horizontal scroll | **5** | `--probe` clean: no overflow at any of the 67 widths from 360 to 1680, one duo transition, all four anchors 0.0 px under the sticky header at all three widths. Session 7 extended this below the sweep's floor (340/320/300/280), under forced WCAG text spacing, and at four short/landscape viewports — all clean on all three pages. Above the ceiling, 1920 and 2560 just centre the 1120 px container. Session 9 added the one combination that had been missing: at a root font size of 24px the header still measures 65px, nothing paints outside it, and every anchor still lands 0.0 px clear on all three pages. |
| 6 | Lighthouse performance above 95 | **5** | Re-run this session against the changed CSS: 100 on performance, accessibility, best-practices and SEO, desktop and mobile, CLS exactly 0, LCP 0.3 s desktop / 1.1 s mobile, TBT 0. |
| 7 | Motion purposeful, never delays reading | **5** | The reveal does not touch opacity, so it cannot be caught mid-fade: 0 of 408 samples at 390, 0 of 737 at 820 and 0 of 718 at 1440, worst 1.000. Motion is a 16 px rise. |
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

**The privacy page is `/peggy/privacy/`, not `/privacy/`.** A scratch probe pointed at
`/privacy/` gets the server's 404 listing, which has no `<header>` and no anchors, and either
crashes or quietly reports all clear for a page it never loaded. It cost ten minutes this
session. `review.py --probe --url` takes the full path; use it rather than assembling one.

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
