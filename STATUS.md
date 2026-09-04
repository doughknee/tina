# Marketing site: where things stand

Written at the point work stopped, so the next session does not have to re-derive any of it.
`PLAN.md` has what is left. Everything is committed on `site/overnight`. Nothing is deployed —
no push, no workflow run, by design.

**Phases 1, 2 and 3 are complete. Phase 4 has had one cycle. Every rubric item now scores 4 or
higher.** PLAN sections 7 and 8 are deliberately untouched (out of scope).

## Tooling

**chrome: unavailable.** `tabs_context_mcp` is not present in this session either. Per the
protocol it was not retried. All visual review is Playwright: headless Chromium, `networkidle`
plus 500 ms, both `prefers-color-scheme` values.

`site/review.py` is the committed harness. Three throwaway probes were used alongside it and
live in the session scratchpad, not the repo — an overflow measurer, a segmented capture (a
full-page PNG of a 13,620 px page is illegible once scaled back), and a contrast measurer. If
you want them permanently, say so and they can be folded into `review.py`.

Lighthouse needed `CHROME_PATH` pointed at the Playwright binary
(`~/AppData/Local/ms-playwright/chromium-1234/chrome-win64/chrome.exe`); it finds no Chrome on
this machine otherwise.

## Rubric scores

Scored from captures of the served page, not from the CSS.

| # | Item | Score | Why |
|---|---|---|---|
| 1 | Hero communicates what Peggy is in under 5 s | **4** | Headline, lede and the capture shot land "capture anything fast". The one-surface idea — calendar, tasks and notes together — is not in the hero at all; you first meet it at the Tags section, well below the fold. |
| 2 | Type scale deliberate, has personality | **4** | Clamped scale, −.022em on headings, .14em uppercase eyebrow, 1.65 body leading — chosen, not defaults. Capped at 4 by `system-ui`: there is no typeface of its own, because there are deliberately no external requests. |
| 3 | Screenshots framed in context, never floating | **5** | Every shot sits in a bezelled `.phone` with a shadow and a caption. None float raw on the background. |
| 4 | Every line specific to Peggy, zero filler | **5** | The parse table shows literal input strings; claims name mechanics ("one line of text, no forms, no pickers"). Found no benefit-speak. Two claims that were *specific but false* were fixed this session. |
| 5 | Reads at 390/820/1440, no horizontal scroll | **5** | `documentElement.scrollWidth` equals the viewport at all six width × theme combinations. Hero stacks below 900, `.pair` duos stack at 390, grid reflows 4/3/1. |
| 6 | Lighthouse performance above 95 | **5** | 100 on the desktop preset and 100 on mobile. Every metric scores 1; CLS is exactly 0. |
| 7 | Motion purposeful, never delays reading | **4** | Improved from 3 this cycle (see LOG). 34 of 184 samples in the reading band are still mid-fade during a fast scroll, worst 0.54. Zero means deleting the reveal, which the brief wants kept. |
| 8 | Dark theme designed, not an inversion | **4** | Tokens are separately chosen — brand lightens #4f5fd6 → #9aa5ff, on-brand flips to near-black, panel and line are picked not derived. Held at 4 because all eight app screenshots are light-theme, so the dark page shows light phones. |

## For Doni to decide

Each of these is what stands between a 4 and a 5, and each is a judgement call I should not
make for you.

1. **The hero does not say "one surface".** Your brief lists three things a visitor must get in
   ten seconds; the claims strip covers "under two seconds" and "no account / no signal", which
   is points 1 and 3 twice over, and never point 2. The obvious fix is to trade one of the three
   claims for a one-surface claim, but that spends the offline point to buy it. I did not
   touch it — the hero scores 4, and the rules say do not rewrite a section scoring 4 from
   scratch.
2. **Dark-theme screenshots.** Peggy has a real dark theme; the site never shows it. Fixing this
   means re-seeding and re-shooting, and `demo_data.py` is banned this session for good reason.
   It is also a design question: light phones on a dark page arguably read fine because the
   bezel contains them.
3. **A typeface.** Item 2 is capped at 4 by `system-ui`. A self-hosted font (no CDN, so the
   no-external-requests promise survives) would lift it, at the cost of bytes and of the
   restraint the brief asked for.
4. **`calendar.webp` is built but shown nowhere.** Eight shots are encoded and committed, seven
   are referenced. It costs nothing at runtime (unreferenced, never fetched) and the manifest is
   the right place for it if a Calendar section is ever added. Left as is; documented as
   deliberate in `site/README.md`. Remove it from `screenshots.json` if you disagree.
5. **The build scripts are served.** `build.py`, `screenshots.py`, `demo_data.py` and `review.py`
   sit inside the published directory, so Pages will serve them at
   `doughknee.com/build.py` and so on. Harmless — they hold no secrets — but untidy. Moving them
   up a level means touching every path in them, which is not a thing to do unreviewed.

## What changed this session

Full detail in `LOG.md`; the short version:

- **Phase 2.** Verified responsive and theme behaviour at all six width × theme combinations.
  Fixed a footer that stranded its tagline against the right edge once it wrapped (breakpoint
  measured at exactly 760 px, not guessed). Dropped a hard-coded `aspect-ratio` that would have
  squashed any future non-1080×2400 shot. Confirmed the nav "Get Peggy" pill renders white on
  purple after the `:not(.btn)` specificity fix.
- **Phase 2, accessibility.** Added a skip link and a `:focus-visible` ring — the
  `outline-offset` is load-bearing, without it a brand ring vanishes into the brand button.
  Fixed an h1 → h3 skip on the home page. Measured contrast for 16 text roles in both themes;
  everything passes, worst 5.16:1.
- **Phase 2, content.** Two claims on the page were false and are now phrasings the corpus test
  proves: "every second Tuesday" parses to no recurrence at all, and "pay rent on the 1st" is
  not monthly without "every month". Cut the paid-tier FAQ paragraph per the brief.
- **Phase 3.** Wrote `site/README.md`, the deliverable that was actually asked for, and pointed
  the repo README at it. Added Pillow to the Pages workflow so a newly dropped PNG is actually
  re-encoded on CI instead of silently deploying the old picture.
- **Phase 4, cycle 1.** Motion 3 → 4: the observer's `rootMargin` was negative, delaying the
  fade until content was already on screen. Flipped it positive and shortened the fade.

## Gotchas already paid for

Do not rediscover these. The originals from session 1 still stand — the accessibility dump
lying mid-animation, the agenda mid-compose race, the calendar being a persisted toggle, demo
mode needing three separate corrections, ASCII-only `print()` in `build.py`, Pillow being
absent on CI, `site/demo_data.sql` being generated. Added since:

**Colour cannot be judged from a scaled capture.** A full-page shot of this page is over
13,000 px tall. Downscaled for review, the dark-theme privacy link read as dark-on-dark and I
logged it as a contrast bug. It is not: rgb(154,165,255) on rgb(23,23,31), comfortably passing.
A 2× crop settled it. Crop before believing anything about colour.

**Specificity, not the cascade, was eating the nav button.** `.bar nav a` is (0,2,1) and beats
`.btn` at (0,1,0), so the pill inherited the nav's muted colour *and* its padding and radius.
`:not(.btn)` on the nav rules is what fixes it; reordering the rules would not have.

**`git stash` will not pop over rebuilt output.** Stashing `build.py` to measure a before/after
and then rebuilding leaves the generated HTML dirty, and the pop aborts. `git checkout --` the
three generated pages first.

**Lighthouse finds no Chrome here.** Export `CHROME_PATH` to the Playwright Chromium.

## Environment notes

- Emulator `emulator-5554` holds the demo data and the 1.8.3 dev build (`com.peggy.app.dev`).
  `site/demo_data.py` is destructive to that app on that device and has never been pointed at a
  phone. It was **not** run this session, and neither was `screenshots.py`; the eight PNGs are
  untouched inputs.
- The phone (`10.205.0.144:46743`) still has two stray captures from earlier automation, a
  ".,vprobe" item in Sort and an "Untitled" note in Ideas. Harmless, safe to delete by hand.
- A static server may still be running on port 8899 from tonight (`python -m http.server 8899`
  started in `site/`). Stop it if it is in the way.
- `adb` is not on the path in Git Bash; the scripts default to
  `~/AppData/Local/Android/Sdk/platform-tools/adb.exe` and take `--adb` to override.
