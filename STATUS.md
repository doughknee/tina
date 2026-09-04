# Marketing site: where things stand

Written at the point work stopped, so the next session does not have to re-derive any of it.
`PLAN.md` has what is left. Nothing here is deployed — every artifact is local and uncommitted
until the commit that carries this file.

## What works

**The screenshot pipeline, end to end.** This was the part worth getting right, because it is
what makes future updates cheap. Three scripts, each doing one thing:

| File | What it does |
|---|---|
| `site/demo_data.py` | Writes the demo content straight into the app's database over adb. Every date is relative to today, so the screenshots never show a stale week. |
| `site/screenshots.py` | Puts the system UI into demo mode, drives the app, and writes eight PNGs to `site/screenshots/`. |
| `site/screenshots.json` | The manifest: id, file, alt text and caption for each shot. The build fails loudly if a named file is missing. |
| `site/build.py` | Re-encodes the PNGs into `site/peggy/shots/*.webp` and generates all three pages. |

Replacing a picture is: drop a PNG over the same name in `site/screenshots/`, run
`python site/build.py`. Retaking everything is `python site/demo_data.py` then
`python site/screenshots.py` (add `--only sort ideas` to redo just those).

**The eight shots**, all 1080×2400, captured from the 1.8.3 dev build on `emulator-5554`:
`capture`, `plan-day`, `plan-week`, `calendar`, `sort`, `ideas`, `editor`, `tag`. They live in
`site/screenshots/` as PNGs (~150–290 KB each) and in `site/peggy/shots/` as WebP (268 KB for
all eight together).

**The generated pages.** `python site/build.py` produces the landing page (32 KB), the privacy
page rendered from `docs/PRIVACY.md` (12 KB), the doughknee home page (11 KB), plus
`robots.txt`, `sitemap.xml` and a 1200×630 `og.png`. No external requests at all: no web fonts,
no CDN, no analytics. That was deliberate, given what the page claims about privacy.

**What the page says.** Hero, a three-claim strip, five alternating feature rows (capture, sort,
plan, ideas, tags), a twelve-card grid, a privacy band, six FAQs and a closing CTA. The CTA
points at the closed-testing opt-in link, not the store listing, because the store listing is
not public yet and would 404. `PRIMARY_URL` and `PRIMARY_LABEL` at the top of `build.py` are the
two constants to change at launch.

## What is not done

The page has been built but barely looked at. One screenshot of the rendered hero showed the
headline, lede and buttons missing — that is the first thing in `PLAN.md` and it is unresolved.
No responsive pass, no dark-theme pass, no accessibility pass, nothing deployed, and
`site/README.md` — the one thing that was explicitly asked for, the documented way to swap
screenshots — is not written.

## Gotchas already paid for

Do not rediscover these.

**The accessibility dump lies while the screen moves.** `uiautomator dump` regularly comes back
empty or unparseable mid-animation. `Device.nodes()` now retries four times and raises if the
device genuinely stops answering. Any new step that reads the screen should go through it rather
than shelling out to `uiautomator` directly.

**The agenda mid-compose race.** This one silently shipped a wrong screenshot before it was
caught. Right after launch, even with a six-second wait, the first dump can land before the
calendar's day cells exist. The calendar-state check read "no day cells" as "calendar is already
hidden", skipped the toggle, and `capture.png` was taken with a month grid behind the sheet. The
fix is the `d.find(text="Day")` anchor plus a one-second settle at the top of `calendar()`:
wait for something that is definitely composed before concluding anything from an absence.
Absence of a node is only evidence once you have proof the screen is up.

**The calendar is a persisted toggle, not a per-screen state.** It survives app restarts, so
each shot has to put it where it wants it rather than assuming. `calendar(d, visible)` checks,
taps, and re-checks up to three times, then raises. A single blind tap was not reliable.

**Demo mode needs three separate corrections.** `adb shell am broadcast -a
com.android.systemui.demo` gives a clean status bar, but: the first attempt built its arguments
programmatically and silently produced nonsense, so the broadcasts are now written out
explicitly; the wifi icon carries a "!" until you pass `-e fully true`; and the mobile icon
shows "3G" unless you pass `network -e mobile hide`. The script always turns demo mode off in a
`finally`, so an interrupted run does not leave the phone with a fake clock.

**Windows console encoding.** `print()` in `build.py` must stay ASCII. A `→` in a status line
crashed the build under cp1252 after the files had already been written, which looks like a
build failure but is not.

**Pillow is not on the CI runner.** `build.py` imports it inside a `try` and falls back to
reusing the existing WebP files, so the Pages build will not break — but only as long as those
files are committed. Committing them, and adding `pip install pillow` to the workflow so a fresh
PNG actually gets re-encoded, are both in `PLAN.md`.

**`site/demo_data.sql` is generated.** `demo_data.py` rewrites it on every run. It should be
ignored rather than committed.

## Environment notes

- Emulator `emulator-5554` holds the demo data and the 1.8.3 dev build (`com.peggy.app.dev`).
  Re-running `site/demo_data.py` is destructive to that app's data on that device: it deletes
  every row before inserting. It has never been pointed at the phone, and should not be.
- The phone (`10.205.0.144:46743`) still has two stray captures from earlier automation, a
  ".,vprobe" item in Sort and an "Untitled" note in Ideas. Harmless, safe to delete by hand.
- A local static server may still be running on port 8899 from tonight
  (`python -m http.server 8899` started in `site/`). Stop it if it is in the way.
- `adb` is not on the path in Git Bash; the scripts default to
  `~/AppData/Local/Android/Sdk/platform-tools/adb.exe` and take `--adb` to override.

---

# Session 1 — 2026-09-04

## Tooling

**chrome: unavailable.** `tabs_context_mcp` is not present in this session, so the Brave
extension path was never viable. Per the protocol it was tried once and not retried.
All visual review is Playwright: headless Chromium, `networkidle` + 500 ms, both
`prefers-color-scheme` values, captured by `site/review.py` into gitignored `site/review/`.

## Phase 1

**1.1 hero blank — fixed, and the stated hypothesis was wrong.** The left column of
`.hero-grid` does not carry `.reveal`, so the IntersectionObserver was never the cause.
The real bug: `img{max-width:100%;display:block}` had no `height:auto`. Every screenshot
therefore rendered at its intrinsic `height="2400"` while `max-width` squashed the width
to 272 px, making `.hero-shot` 2418 px tall. `align-items:center` then centred the text
column against that row and pushed the `<h1>` down to y=1167 — a full screen below the
fold. The text was never hidden, only displaced. One line at the shared `img` rule fixes
it, and un-distorts every other screenshot on the page at the same time.

## Found while there, not yet fixed

- **`header .btn` renders in `--muted`, not `--on-brand`.** `.bar nav a{color:var(--muted)}`
  (0,2,1) outranks `.btn{color:var(--on-brand)}` (0,1,0), so the "Get Peggy" pill in the
  nav is grey-on-purple. Visible in `site/review/1280-light-gate2.png`. Belongs to
  PLAN 3.4 (contrast) and is queued for Phase 2.
- `site/build.py`, `site/screenshots.py`, `site/demo_data.py` and `site/review.py` all sit
  inside the published directory, so Pages serves them. Harmless but untidy; not touched
  tonight because it is outside every phase.
