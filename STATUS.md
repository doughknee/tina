# Marketing site: where things stand

Written at the point work stopped, so the next session does not have to re-derive any of it.
`PLAN.md` has what is left. Everything is committed on `site/overnight`. Nothing is deployed —
no push, no workflow run, by design.

**Phases 1, 2 and 3 are complete. Phase 4 has had three cycles. Six of the eight rubric items
now score 5; the two still at 4 are both blocked on a decision that is yours, below.** PLAN
sections 7 and 8 are deliberately untouched (out of scope).

There is no `DONE` file, because `DONE` requires every item at 5 and two of them cannot get
there without you.

## Tooling

**chrome: unavailable.** `tabs_context_mcp` is not present in this session either. Per the
protocol it was not retried. All visual review is Playwright: headless Chromium, `networkidle`
plus 500 ms, both `prefers-color-scheme` values.

`site/review.py` is the committed harness, and it now measures as well as screenshots:

```bash
python site/review.py --probe    # overflow, motion and hidden-content, exits non-zero on failure
```

That replaces the throwaway probes that the last three sessions each rebuilt in a scratchpad
and threw away. Run it before committing any CSS change. It is documented in `site/README.md`.

Lighthouse needs `CHROME_PATH` pointed at the Playwright binary
(`~/AppData/Local/ms-playwright/chromium-1234/chrome-win64/chrome.exe`); it finds no Chrome on
this machine otherwise.

## Rubric scores

Scored from captures and measurements of the served page, not from the CSS.

| # | Item | Score | Why |
|---|---|---|---|
| 1 | Hero communicates what Peggy is in under 5 s | **5** | Raised from 4 this session. All three of the brief's ten-second points now land above the fold: capture speed in the h1, "an event, a task or a note — three things that usually mean three apps" in the lede, and no-account/no-signal in the claims strip. |
| 2 | Type scale deliberate, has personality | **4** | Clamped scale, −.022em on headings, .14em uppercase eyebrow, 1.65 body leading — chosen, not defaults. Capped at 4 by `system-ui`: there is no typeface of its own, because there are deliberately no external requests. **Your call, see below.** |
| 3 | Screenshots framed in context, never floating | **5** | Every shot sits in a bezelled `.phone` with a shadow and a caption. Re-checked every section this session; none float raw on the background. |
| 4 | Every line specific to Peggy, zero filler | **5** | The parse table shows literal input strings; the 12 cards each name a mechanic. The new hero claim was checked against `ItemType { INBOX, TASK, EVENT, NOTE }` before it was written. Also fixed the home card, which still called Peggy "a to-do app". |
| 5 | Reads at 390/820/1440, no horizontal scroll | **5** | `documentElement.scrollWidth` equals the viewport at all six width × theme combinations, on all three pages. |
| 6 | Lighthouse performance above 95 | **5** | 100 desktop and 100 mobile, re-run after this session's changes. Mobile also scores 100 on accessibility, best-practices and SEO. CLS exactly 0. |
| 7 | Motion purposeful, never delays reading | **5** | Raised from 4. The reveal no longer touches opacity, so it cannot be caught mid-fade: 0 of 722 samples at 1440 and 0 of 417 at 390, against 319 and 216 before. Motion is kept as a 16 px rise. |
| 8 | Dark theme designed, not an inversion | **4** | Tokens are separately chosen — brand lightens #4f5fd6 → #9aa5ff, on-brand flips to near-black, panel and line are picked not derived. Held at 4 because all eight app screenshots are light-theme, so the dark page shows light phones. **Your call, see below.** |

## For Doni to decide

Both of these are what stands between a 4 and a 5, and each is a judgement call I should not
make for you. Nothing else is blocking.

1. **A typeface (item 2).** `system-ui` caps this at 4 — it is a deliberate choice, but it is by
   definition the least distinctive one. A self-hosted face would lift it and would not break
   the no-external-requests promise (no CDN), at the cost of bytes, and of some of the restraint
   the brief asked for. Choosing a face for your product is a taste decision, so I left it.
2. **Dark-theme screenshots (item 8).** Peggy has a real dark theme — the feature grid even
   advertises it — and the site never shows it. I checked, and this is now *mechanically*
   possible: `emulator-5554` is up and still holds the demo data, so `screenshots.py` could
   re-shoot against it with `adb shell cmd uimode night yes` and **no** run of the banned
   `demo_data.py`. I did not do it, for three reasons: it doubles the committed image set and
   needs a `<picture>` per shot, it is the largest change of the night to make unattended, and
   it is a design question rather than a bug. Light phones on a dark page arguably read fine —
   I looked at two segments at 1440 dark and the bezel does contain them.

Resolved since the last session, so you do not need to read them again: the hero's missing
"one surface" point (fixed, in the lede, without spending a claims slot); `calendar.webp` being
built but unreferenced (documented as deliberate in `site/README.md`); the build scripts being
served from the published directory (harmless, they hold no secrets).

## What changed this session

Full detail in `LOG.md`; the short version:

- **Motion 4 → 5.** The reveal faded opacity 0 → 1 over .32 s, and a fast scroll always outruns
  that, so text arrived in the reading band still half transparent. Last session's `rootMargin`
  tuning only made it rarer. Deleting the fade — the reveal now only rises — makes it
  impossible. Measured before and after.
- **Hero 4 → 5.** The lede ended on "and files it", leaving the destination unnamed. Named it
  there rather than trading away one of the three claims. Verified against `ItemType` first.
- **The home card called Peggy "a to-do app".** It is three things, and the Peggy page's own
  meta description already said so.
- **`review.py --probe`.** The three measurements are now in the repo instead of being rebuilt
  from scratch every session, and are documented in `site/README.md`.

## Gotchas already paid for

Do not rediscover these. The originals still stand — the accessibility dump lying mid-animation,
the agenda mid-compose race, the calendar being a persisted toggle, demo mode needing three
separate corrections, ASCII-only `print()` in `build.py`, Pillow being absent on CI,
`site/demo_data.sql` being generated, and the two in `site/README.md` (lazy images compositing
blank, and colour being unjudgeable from a scaled capture). Added since:

**Do not put the fade back.** `.js-reveal .reveal` deliberately sets `transform` only and never
`opacity`. It looks like an oversight and it is not: `review.py --probe` will fail if anything
re-adds it. The number is 300-plus unreadable frames per scroll, not a rounding error.

**Specificity, not the cascade, was eating the nav button.** `.bar nav a` is (0,2,1) and beats
`.btn` at (0,1,0), so the pill inherited the nav's muted colour *and* its padding and radius.
`:not(.btn)` on the nav rules is what fixes it; reordering the rules would not have.

**`git stash` will not pop over rebuilt output.** Stashing `build.py` to measure a before/after
and then rebuilding leaves the generated HTML dirty, and the pop aborts. `git checkout --` the
three generated pages first.

**A fresh build shows the three pages as modified, with an empty diff.** That is line endings
only — `git checkout` restores LF, Python writes CRLF, and git normalises back on commit.
`git diff` being empty is the check that matters, not `git status`.

**Lighthouse finds no Chrome here.** Export `CHROME_PATH` to the Playwright Chromium.

## Environment notes

- Emulator `emulator-5554` holds the demo data and the 1.8.3 dev build (`com.peggy.app.dev`).
  `site/demo_data.py` is destructive to that app on that device and has never been pointed at a
  phone. It was **not** run this session, and neither was `screenshots.py`; the eight PNGs are
  untouched inputs.
- The phone (`10.205.0.144:46743`) is attached and still has two stray captures from earlier
  automation, a ".,vprobe" item in Sort and an "Untitled" note in Ideas. Harmless, safe to
  delete by hand. Nothing this session touched it.
- A static server may still be running on port 8899 (`python -m http.server 8899` started in
  `site/`). Stop it if it is in the way.
