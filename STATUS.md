# Marketing site: where things stand

Written at the point work stopped, so the next session does not have to re-derive any of it.
`PLAN.md` has what is left. Everything is committed on `site/overnight`. Nothing is deployed —
no push, no workflow run, by design.

**Phases 1–5 are complete. This session found and fixed one real defect: the phone bezel was a
hardcoded `#0d0d12` on an otherwise fully tokenised page, which is 18.75:1 against the light
background and 1.01:1 against the dark one — so in dark mode there was no frame at all and all
eight screenshots floated.** Seven of the eight rubric items now score 5; the one still at 4 is
blocked on a decision that is yours, below. PLAN sections 7 and 8 are deliberately untouched
(out of scope).

There is no `DONE` file, because `DONE` requires every item at 5 and one of them cannot get
there without you.

## What this session found

**The phone frame only existed in one theme.** Every surface on this page takes its colour from
a token that is declared twice, once per theme — `--bg`, `--panel`, `--line`, `--brand`,
`--tint`, `--shadow`. `.phone` did not. It was written `background:#0d0d12`, a literal, chosen
against the light `--bg` of `#fbfbfd`. The dark `--bg` is `#0e0e13`.

Those two are the same colour to within one RGB unit. Measured off the rendered page:

| | bezel vs page background |
|---|---|
| light | **18.75:1** — a solid black frame, unmistakably a device |
| dark | **1.01:1** — nothing |

So in dark mode all eight screenshots were bright rectangles sitting on an empty background,
which is the exact thing rubric item 3 forbids. Item 3 had been carrying a **5** for three
sessions on a reason that was only ever checked in light.

Caught it by looking at a 1440 dark segment and then *not* trusting the scaled capture: cropped
the phone edge at 4× in both themes and sampled the pixels. Light shows a black bar between page
and screen; dark showed the page background running straight into the screenshot.

Fixed the way the rest of the page already works — a `--bezel` token, re-chosen per theme. Dark
is `#2b2b36`, which is 1.38:1 against the dark bg, one notch above `--line` (1.28:1) so it reads
as a device frame rather than a hairline border. Light keeps `#0d0d12` by construction and is
**pixel-identical** before and after: all ten 1440 light segments give `ImageChops` bbox `None`.

This is the same shape of miss as the `px` body font: **each theme looked deliberate on its own,
and the defect only existed in the comparison between them.** It is now the seventh `--probe`
check, proved to fail as well as pass — with `#0d0d12` put back it reports 8 surfaces at light
18.75:1 / dark 1.01:1 and exits 1; with `#2b2b36` it reports 0 and exits 0.

### Four theories tested this session that were wrong

Recorded so nobody spends a session re-testing them. All four were measured, all four were clean.

- **Below the sweep's floor.** Session 6 checked above the 1680 ceiling; nobody had checked under
  the 360 floor. At 340, 320, 300 and 280 — 320 being 400% browser zoom on a 1280 viewport, a
  WCAG AA reading condition, and 280 being a Fold cover screen — all three pages have
  `scrollWidth` exactly equal to the viewport and nothing outside it.
- **WCAG 1.4.12 text spacing.** The sibling of the font-size finding: line-height 1.5, letter
  spacing .12em, word spacing .16em, paragraph spacing 2em, all forced. Nothing clipped, nothing
  outside the viewport, no overflow, on all three pages at 390, 820 and 1440 with every
  `<details>` open.
- **Short viewports.** 844×390 and 740×360 (phone in landscape), 932×430, 1280×600. The sticky
  bar takes 16–18% of the screen there and every one of the four anchors still lands 0.0 px clear
  of it. No overflow.
- **The sticky header over large heading type.** In a scaled capture the privacy heading appears
  to ghost through the bar sharply. At 3× it does not: the backdrop blur is working, the heading
  dissolves into soft grey and the wordmark stays crisp. That was the scaled-capture trap again,
  not a defect.

## Tooling

**chrome: available.** `tabs_context_mcp` responded without error (no tab group; it offers to
create one). Not used for scoring this session: the finding was a dark-theme colour measurement
and Chrome cannot be told to use a colour scheme, a viewport or a root font size. Sessions 4 and
5 saw the second `screenshot` call time out at 30 s; session 6 and this one did not.

Playwright supplies every number below.

`site/review.py` is the committed harness. It now measures **seven** things, and has a mode for
looking:

```bash
python site/review.py --probe      # overflow, sweep, motion, anchors, text size, themes, hidden
python site/review.py --segments   # the whole page, one readable screenful at a time
```

`--probe` takes `--url`, and it was run against **all three pages** this session: home and
privacy are `all clear` on all seven checks too.

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
| 1 | Hero communicates what Peggy is in under 5 s | **5** | What Peggy *is* lands above the fold at every size measured: capture speed in the h1, "an event, a task or a note — three things that usually mean three apps" in the lede, then both buttons. Re-read at 1440 light and 390 dark this session. Note the correction session 6 made and do not undo it: **the brief's third point is above the fold only at 1440×900.** "Works with no signal" sits 154 px below it at 1280×720 and 599 px below at 390×844; at 390 the privacy half is carried above the fold by "no account" in the CTA note alone. Still a 5 — the rubric asks what Peggy is, and a stacked phone layout cannot hold a screenshot and a three-claim strip in one screen. |
| 2 | Type scale deliberate, has personality | **4** | Clamped scale, −.022em on headings, .14em uppercase eyebrow, 1.65 body leading, a real second voice in the monospace parse table, and since session 6 a scale that moves as a whole with the reader's font size. Capped at 4 by `system-ui`: there is no typeface of its own, because there are deliberately no external requests. **Your call, see below.** |
| 3 | Screenshots framed in context, never floating | **5** | Scored **3** at the start of this session, not the 5 it had carried for three: in dark mode the bezel measured 1.01:1 against the page background and the frame did not exist, so all eight shots floated (above). Fixed, and back to 5 — all eight now sit in a bezelled `.phone` with a real box-shadow **in both themes**, verified at 1440 light, 1440 dark and 390 dark. Seven of the eight carry a `<figcaption>`; the hero shot does not, and should not — it is bracketed by the h1, lede, buttons and note above and the claims panel below. |
| 4 | Every line specific to Peggy, zero filler | **5** | The parse table shows literal input strings that `CaptureCorpusTest` proves; the 12 cards each name a mechanic. Session 6 sampled five claims off the rendered copy and found all five in `composeApp/src`. Re-read at 1440 and 390 this session. |
| 5 | Reads at 390/820/1440, no horizontal scroll | **5** | `--probe` clean: no overflow at any of the 67 widths from 360 to 1680, one duo transition, all four anchors 0.0 px under the sticky header at all three widths. Extended this session below the sweep's floor (340/320/300/280), under forced WCAG text spacing, and at four short/landscape viewports — all clean on all three pages. Above the ceiling, 1920 and 2560 just centre the 1120 px container. |
| 6 | Lighthouse performance above 95 | **5** | 100 on performance, accessibility, best-practices and SEO, desktop and mobile, CLS exactly 0, LCP 1.1 s mobile and 0.3 s desktop. Last run session 6; this session's change is one colour token in the dark block and cannot move it. |
| 7 | Motion purposeful, never delays reading | **5** | The reveal does not touch opacity, so it cannot be caught mid-fade: 0 of 408 samples at 390 and 0 of 718 at 1440, worst 1.000. Motion is a 16 px rise. |
| 8 | Dark theme designed, not an inversion | **4** | Tokens are separately chosen, not derived — brand lightens #4f5fd6 → #9aa5ff, on-brand flips to near-black, panel #17171f sits distinctly above bg #0e0e13, and as of this session the bezel is re-chosen too. **The reason this row carried for three sessions was wrong**: it said the light screenshots were "the only thing" holding it at 4, while a whole component was collapsing into the background. That is now fixed, so the statement is finally true — the light screenshots really are the only thing left. **Your call, see below.** |

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
   With the bezel fixed the dark page now reads as properly designed — but a light screenshot
   inside a dark page is still the one thing a designer would call out.

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

**And one theme is not a theme check.** The light page was right and the dark page was wrong,
and each looked deliberate in isolation — the defect existed only in the comparison. The
seventh probe check compares them element by element. The general rule it enforces: **any
colour written outside the two `:root` token blocks is a colour that was chosen against exactly
one background.** There are now none.

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

**Patch scripts must match the file's line endings.** `site/README.md` is CRLF; an `\n` literal
in a Python replacement silently matches nothing. `STATUS.md` and `LOG.md` are LF. It cost one
extra commit in session 6 and was avoided this session by checking first.

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
