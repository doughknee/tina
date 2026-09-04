# Marketing site: where things stand

Written at the point work stopped, so the next session does not have to re-derive any of it.
`PLAN.md` has what is left. Everything is committed on `site/overnight`. Nothing is deployed —
no push, no workflow run, by design.

**Phases 1–5 are complete. This session found and fixed one real defect: a single `px` in the
`font:` shorthand on `body` meant 36 of the 90 lines on the page ignored the reader's own
font-size setting while the headings around them grew. Six of the eight rubric items score 5;
the two still at 4 are both blocked on a decision that is yours, below.** PLAN sections 7 and 8
are deliberately untouched (out of scope).

There is no `DONE` file, because `DONE` requires every item at 5 and two of them cannot get
there without you.

## What this session found

**The type scale half-ignored the reader.** Every font-size on the page is `rem` or `clamp()`
except one: `body{font:400 17px/1.65 …}`. Everything inheriting from `body` was therefore nailed
to 17px no matter what the reader had set as their browser font size, while everything sized in
`rem` grew with it.

Measured at root 24px (Chrome's "Very large"), at 390 and 1440: **36 of the 90 text elements on
`/peggy/` did not move.** Among them —

| frozen at 17px | who reads it |
|---|---|
| `Join the test on Google Play` | the one action the whole page exists for |
| `See how it works` | the hero's second button |
| `Skip to content` | the keyboard and screen-reader path |
| the six FAQ questions and every answer | anyone with a question before installing |
| the claim spans (`No forms.` …) | the three claims under the hero |

…under an `h1` that had grown to 57.6px and a lede at 28.8px. Looked at it as well as measured
it: the FAQ block reads as a different page's typography from the CTA directly beneath it.

Fixed with one value, `1.0625rem`, which is 17px at the default root — so nothing moves for a
reader who has not changed the setting. The 390 dark segment captures are **pixel-identical**
before and after (`ImageChops` bbox `None`). After: 0 of 90 frozen on `/peggy/`, 0 of 13 on the
home page, 0 of 32 on privacy, and `scrollWidth` still 390/1440 at root 24, so the larger text
does not introduce overflow.

This is the same shape of miss as the 900–1060 reflow and the anchor landings: **a defect that
no capture taken at default settings can show.** It is now the sixth `--probe` check, proved to
fail as well as pass — with `17px` put back it reports 36 of 112 frozen at both widths and exits
1; with `1.0625rem` it reports 0 of 112 and exits 0.

## Tooling

**chrome: available.** For the first time in six sessions it worked, and kept working:
`tabs_context_mcp` reported no group, `navigate` created one and loaded the page, and two
`screenshot` calls in a row both returned — no 30 s `Page.captureScreenshot` timeout. The hero
rendered correctly in real Chrome at 1400 in dark, phone image and all, and so did the Capture
section after scrolling. The tab was closed afterwards. Sessions 4 and 5 both saw the second
capture time out; if it happens again, that is an error and Playwright takes over for the rest
of the run.

Playwright is still where every *number* below comes from — it is the only one of the two that
can set a viewport, a colour scheme and a root font size.

`site/review.py` is the committed harness. It now measures **six** things, and has a mode for
looking:

```bash
python site/review.py --probe      # overflow, sweep, motion, anchors, text size, hidden
python site/review.py --segments   # the whole page, one readable screenful at a time
```

Run `--probe` before committing any CSS change. `--segments` is how you look at the page: a
full-page PNG of this page is over 13,000 px tall and is unreadable once scaled down to view,
which is how a missing caption survived five sessions of "checked the full-page capture". Both
documented in `site/README.md`.

Lighthouse needs `CHROME_PATH` pointed at the Playwright binary
(`~/AppData/Local/ms-playwright/chromium-1234/chrome-win64/chrome.exe`); it finds no Chrome on
this machine otherwise.

## Rubric scores

Scored from captures and measurements of the served page, not from the CSS. Every item below was
re-checked this session.

| # | Item | Score | Why |
|---|---|---|---|
| 1 | Hero communicates what Peggy is in under 5 s | **5** | All three of the brief's ten-second points land above the fold — capture speed in the h1, "an event, a task or a note — three things that usually mean three apps" in the lede, no-account/no-signal in the claims strip and again in the CTA note. Confirmed this session in real Chrome at 1400 as well as in Playwright at 390 dark. |
| 2 | Type scale deliberate, has personality | **4** | Scored **3** at the start of this session, not the 4 it had carried for three: 36 of 90 lines ignored the reader's font size (above). Fixed, and back to 4 — clamped scale, −.022em on headings, .14em uppercase eyebrow, 1.65 body leading, a real second voice in the monospace parse table, and now a scale that moves as a whole. Still capped at 4 by `system-ui`: there is no typeface of its own, because there are deliberately no external requests. **Your call, see below.** |
| 3 | Screenshots framed in context, never floating | **5** | All eight shots sit in a bezelled `.phone` with a shadow and a caption. Re-checked this session at 820 light (the tag phone, captioned "One tag, everything under it.") and 390 dark (the stacked duo, each phone carrying its own label). |
| 4 | Every line specific to Peggy, zero filler | **5** | The parse table shows literal input strings that `CaptureCorpusTest` proves; the 12 cards each name a mechanic. Re-read this session in Chrome at 1400 and in the 390 dark segments. |
| 5 | Reads at 390/820/1440, no horizontal scroll | **5** | `--probe` clean: no overflow at any of the 67 widths from 360 to 1680, one duo transition, all four anchors 0.0 px under the sticky header at all three widths. Also checked at root 24px on all three pages with every `<details>` forced open — nothing clipped, nothing outside the viewport except the skip link, which is deliberately parked at `left:-9999px` until focused. |
| 6 | Lighthouse performance above 95 | **5** | Re-run on the changed build: **100** on performance, accessibility, best-practices and SEO, desktop and mobile. CLS exactly 0, LCP 1.1 s mobile and 0.3 s desktop. |
| 7 | Motion purposeful, never delays reading | **5** | The reveal does not touch opacity, so it cannot be caught mid-fade: 0 of 408 samples at 390 and 0 of 718 at 1440, worst 1.000. Motion is a 16 px rise. |
| 8 | Dark theme designed, not an inversion | **4** | Tokens are separately chosen, not derived — brand lightens #4f5fd6 → #9aa5ff, on-brand flips to near-black, panel #17171f sits distinctly above bg #0e0e13. New this session: looked at the translucent sticky header where it passes over a light phone screenshot at 390 and 1440 dark, on the theory that 82% of the bg plus a blur would wash out over near-white — it does not, the bar stays dark and the wordmark stays legible. So the light screenshots really are the only thing holding this at 4. **Your call, see below.** |

Also verified this session: `python site/build.py` leaves `git diff` empty, so the committed
output really is what the build produces.

## For Doni to decide

Both of these are what stands between a 4 and a 5, and each is a judgement call I should not
make for you. Nothing else is blocking. They are unchanged from the last three sessions.

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
   The dark page reads as deliberate rather than broken — but it is the one thing on the page
   that a designer would call out.

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
17px is exactly what they should be. If a size is written in `px` anywhere on this page again,
the sixth `--probe` check catches it; that check is the only reason to trust this now.

**Do not put the fade back.** `.js-reveal .reveal` deliberately sets `transform` only and never
`opacity`. It looks like an oversight and it is not: `review.py --probe` will fail if anything
re-adds it. The number is 300-plus unreadable frames per scroll, not a rounding error.

**A screenshot taken in the same breath as the navigation can show an empty phone.** Session 5's
single Chrome capture showed the hero bezel empty and it was not a bug — proved by drawing the
image to a canvas and sampling (`complete:true`, `naturalWidth:640`, mean luminance 212/255).
This session's Chrome capture, taken after the page had settled, shows the phone.

**Specificity, not the cascade, was eating the nav button.** `.bar nav a` is (0,2,1) and beats
`.btn` at (0,1,0). `:not(.btn)` on the nav rules is what fixes it; reordering would not have.

**`git stash` will not pop over rebuilt output.** `git checkout --` the three generated pages
first.

**A fresh build shows the three pages as modified, with an empty diff.** Line endings only.
`git diff` being empty is the check that matters, not `git status`.

**Patch scripts must match the file's line endings.** `site/README.md` is CRLF; an `\n` literal
in a Python replacement silently matches nothing. It cost one extra commit this session.

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
