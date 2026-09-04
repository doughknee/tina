# LOG

One line per change: what changed, and the result. Read this before deciding
anything. Never retry an approach logged as failed.

## Session 1 — 2026-09-04

- Brave/`tabs_context_mcp` not present in this session at all; per the protocol,
  did not retry. Playwright (chromium) installed and used for every capture since.
- Added `site/review.py` (Playwright capture harness: widths, both colour
  schemes, `--nojs`, `--full-page`) writing to gitignored `site/review/`.
- **PLAN 1.1 hero blank — FIXED, root cause was not `.reveal`.** The left column
  of `.hero-grid` never carried `.reveal`, so that hypothesis was wrong. Real
  cause: `img{max-width:100%;display:block}` had no `height:auto`, so every
  screenshot rendered at its intrinsic `height="2400"` while `max-width` squashed
  the width to 272px. `.hero-shot` came out 2418px tall; `align-items:center`
  centred the text column against that row and pushed the `<h1>` to y=1167, far
  below the fold. Fixed once at the shared `img` rule rather than at `.phone img`,
  which also un-distorts every other shot on the page. Verified: headline, lede,
  both buttons and the note all visible at 1280 wide.
- **PLAN 1.3 reveal — inverted.** `.reveal{opacity:0}` now only applies under
  `html.js-reveal`, and a tiny head script adds that class before first paint,
  and only when `IntersectionObserver` exists and reduced motion is not asked
  for. Hiding is opt-in, so JS-off and reduced-motion loads never hide anything.
  Deleted the now-dead fallback branch in `REVEAL_JS`. Verified with a JS-off
  full-page capture.
- **`review.py` was lying about full-page captures.** `loading="lazy"` images
  below the fold composite as blank frames; the Ideas and Tags phones came out
  empty white. Added `scroll_through()` (mouse wheel, which works with JS off,
  unlike `page.evaluate`). Any full-page score taken before this fix is wrong.
- **PLAN 1.2 sections — verified.** All twelve present once, in order, JS
  disabled: hero, claims, capture, sort, plan, ideas, tags, 12-card grid,
  privacy band, 6 FAQs, closing CTA, footer. None empty or collapsed.
- **Phase 1 gate is clear.** Phase 2 next.

## Session 2 — 2026-09-04

- **Phase 2 opened.** Server on 8899, Playwright only (chrome still unavailable,
  not retried). Added two scratch harnesses: an overflow probe that measures
  `documentElement.scrollWidth` against the viewport, and a segmented capture
  that walks the page a screenful at a time (a full-page PNG of a 13,620px page
  is unreadable when scaled back down).
- **PLAN 2.1/2.2/2.3 verified.** No horizontal overflow at 390/820/1440 x
  light/dark -- all six measured, not eyeballed. Hero stacks below 900, `.pair`
  duos sit side by side at 820 and stack at 390, grid reflows 4/3/1.
- **Footer wrap fixed.** `.foot .spacer{margin-left:auto}` stranded the tagline
  against the right edge once the footer wrapped. Measured the wrap point at
  exactly 760px and gated the rule there.
- **`.phone img` aspect-ratio removed.** Every img already carries the real PNG
  dimensions from `process_images`, so the ratio is per image now.
- **PLAN 3.4 contrast: measured, all pass.** 16 text roles against their real
  painted backdrop, both themes. Worst is 5.16:1 (light eyebrow) against 4.5.
  NOTE: I first read the dark privacy-band link as dark-on-dark from a
  downscaled 1440 capture. It is not -- rgb(154,165,255) on rgb(23,23,31).
  A 2x crop settled it. Do not trust colour judgements from scaled captures.
- **PLAN 3.1/3.2 added.** Skip link + `<main id="main" tabindex="-1">`, and one
  `:focus-visible` rule. `outline-offset:3px` is load-bearing: without it the
  brand-coloured ring vanishes into the brand-coloured CTA. Verified by driving
  Tab and Enter, and by 2x crops of the ring in both themes.
- **PLAN 3.3 heading order.** Peggy and privacy pages were clean. The home page
  skipped h1 -> h3 because the card heading was an h3 picked for its size.
  Promoted to h2, made the size rule `h3,.card h2`. Outline now has no skips.
- **PLAN 4.1 found two false claims.** "every second Tuesday" parses to no
  recurrence at all (no regex matches "second"), and "pay rent on the 1st"
  does not yield monthly without "every month". Both replaced with strings
  CaptureCorpusTest proves. Everything else on the page traced to code.
- **FAQ paid-tier paragraph cut**, per the BRIEF. Billing genuinely exists, so
  docs/PRIVACY.md's Purchases section is accurate and was left alone.
- **Phase 2 complete.** Phase 3 next: site/README.md and the Pillow step.
- **Phase 3 done.** Wrote `site/README.md` (the explicit deliverable): replacing
  one shot, retaking all of them, adding one, changing demo content, device
  requirements, the two launch constants, and the two review-harness traps.
  Pointed the repo README at it from Development. Added
  `python -m pip install --disable-pip-version-check pillow` to pages.yml --
  same `python` the build already uses, so it is not a second assumption about
  what is on the runner's PATH. Verified `git status` is clean after a fresh
  build, so the committed WebP/og.png really are what the build produces.
- **Phase 4, cycle 1.** Scored all eight rubric lines into STATUS.md from
  captures. Lowest was motion at 3; fixed and re-measured to 4 (before/after in
  the commit). Lighthouse needed `CHROME_PATH` pointed at the Playwright
  Chromium -- it finds no Chrome on this machine -- then scored 100 desktop and
  100 mobile, CLS 0. Everything now scores 4 or higher; the four items still at
  4 are each blocked on a decision that is Doni's, listed in STATUS.md under
  "For Doni to decide". No DONE file: that needs every item at 5.

## Session 3 — 2026-09-04

- **Phase 4, cycle 2. Motion 4 -> 5.** Rebuilt the motion probe (scratchpad,
  `motion_probe.py`): 240 frames of scrolling, sampling computed opacity of every
  `.reveal` overlapping the middle 20-80% of the viewport. Baseline was worse than
  last session's cruder probe reported -- 319/722 mid-fade at 1440, 216/416 at 390,
  worst 0.54. Last cycle's rootMargin tuning only reduced the odds. Deleted the
  opacity half of the reveal instead: it now rises 16px and never fades, so no
  sample can be mid-fade. 0/722 and 0/417 after. Reveal still fires (verified an
  element two viewports down sits at translateY(16px), opacity 1, and settles to
  none on scroll). No overflow regression at any of the six width x theme combos.
- **Phase 4, cycle 3. Hero 4 -> 5.** The hero delivered two of the brief's three
  ten-second points and never said "one surface". Named the destination on the
  clause that already dangled ("and files it") rather than trading away a claims
  slot: "as an event, a task or a note -- three things that usually mean three
  apps". Verified the claim in the app first (`ItemType { INBOX, TASK, EVENT,
  NOTE }`, one table) so it is not benefit-speak. All three points now land above
  the fold at 1440 and 390.
- **Home card copy.** The card on doughknee.com called Peggy "a quick-capture to-do
  app"; the Peggy page's meta description already said "to-do, calendar and notes".
  Same undersell the hero just fixed, on the line most people see first.
- **`review.py --probe` added.** Overflow, motion and hidden-content, exiting
  non-zero on failure -- the three harnesses each of the last three sessions
  rebuilt in a scratchpad and discarded. Proved it fails as well as passes by
  re-introducing the opacity fade: 222/414 and 319/722, reproducing the pre-fix
  baseline exactly, then reverted. Documented in `site/README.md`.
- **Phase 5 verification.** Lighthouse re-run after the changes: 100 desktop, 100
  mobile, and 100 on accessibility, best-practices and SEO, CLS 0. All three pages
  checked at 390 and 1440 in both themes -- no overflow, one h1 each, no heading
  skips, nothing faded. JS-off and reduced-motion loads hide nothing.
- **Phase 4 closed at six 5s and two 4s.** Items 2 (typeface) and 8 (dark-theme
  screenshots) are both decisions for Doni and are written up in STATUS.md. Note
  for the next session: item 8 is now mechanically possible without the banned
  `demo_data.py` -- emulator-5554 is up and still seeded, so `screenshots.py`
  plus `adb shell cmd uimode night yes` would do it. It was not done because it
  is a design call, not a bug. No DONE file: that needs every item at 5.

## Session 4 — 2026-09-04

- **Chrome tried once and it errored.** `tabs_context_mcp` was present this
  session (unlike sessions 1-3), navigated and took one screenshot, then
  `Page.captureScreenshot` timed out at 30 s and left the tab at a broken zoom.
  Per the protocol that is an error: closed the tab, Playwright for the rest of
  the run, no retries. Chrome is still "unavailable" for scoring purposes.
- **Phase 4 -> 5.** Every item was 4 or higher at the start, so phase 4 closed.
  Phase 5 is the final rubric pass: fresh build (`git diff` empty, so the
  committed output is what the build produces), `review.py --probe` all clear,
  then every item rescored from segmented captures at 1440 and 390 in both
  themes.
- **Item 3 was not a 5, and STATUS.md's reason for it was false.** It claimed
  every shot sits in a bezel "and a caption". Four do not: the `.pair` duos in
  Plan and Ideas render through `phone()`, which has no `<figcaption>`. Caught
  it in a 390 capture, not by reading the CSS -- the duo stacks there, so it is
  two unlabelled near-identical phones on empty background a screen below the
  heading that explains them. Rescored 4.
- **Item 3 fixed, 4 -> 5.** The captions were already in `screenshots.json` and
  already specific; only the render dropped them. Swapped the four to the
  existing `figure()` helper, reset the UA `figure` margin that would otherwise
  push the duo apart, and capped `.pair .shot` at the phone width so a caption
  wraps under its own phone. Measured after: duo still one row at 1440 and 820,
  stacks at 390, probe all clear. Single shots unchanged in a before/after
  capture.
- **Item 6 re-verified on the changed page.** Lighthouse 100 on performance,
  accessibility, best-practices and SEO, desktop and mobile, CLS 0. Note for
  next time: the run now ends in `EPERM ... destroyTmp` while deleting its temp
  profile. That is cleanup *after* the report is written -- check for the JSON
  before believing the error.
- **Structural pass, all three pages.** One h1 each, no heading skips, every
  image has alt text and explicit width/height, skip link and `main#main`
  present. Alt text re-read against the shots: the Sort alt names New, Overdue
  and Snoozed and the Today/Someday/Tomorrow/This week chips, and that is what
  the screenshot shows. FAQ opened and captured -- answers read correctly and
  no overflow with every `<details>` open.
- **Item 5 was wrong, and three widths is why. 900-1060 reflow, FIXED.** Swept
  every width 360-1680 instead of the three the rubric names. `.row-grid` goes
  two-column at 900, but the shot column is 404px there and does not fit two
  228px phones until 1080, so across the whole band the duo wrapped into a
  1073px tower beside a ~350px text block. 1024 is a common laptop width and it
  looked broken. Confirmed pre-existing by running the same sweep against
  d87f2ab -- identical 900-1060 band, so it was not the caption change. Fixed
  with `20.5vw` in that band only: widest that fits two-up at 900 (188px
  available) and hands over to 228px at 1080. Pair height at 1024 1073px ->
  506px, and the duo now has exactly one transition across the whole range.
- **The sweep is now in `review.py --probe`.** Fourth check beside overflow,
  motion and hidden. Proved it fails as well as passes: rule disabled it reports
  3 transitions and exits 1, restored it reports 1 and exits 0. Documented in
  `site/README.md`, with the lesson stated plainly -- breakpoints put the bugs
  between the widths you test.
- **Swept the other two pages too, and checked for clipped content.** The probe
  only sweeps `/peggy/`. Ran the same 360-1680 sweep by hand against the home and
  privacy pages: no overflow at any width. Also ran a stricter check across all
  three -- any element whose own content is wider than its box -- and there is
  none at any width. Did not add all three pages to the probe: they share the
  CSS and the peggy page is the one with the layout.
- **Reveal re-verified after the caption change.** Wrapping the duo phones in
  `<figure>` could have moved what the IntersectionObserver watches. It did not:
  29 `.reveal`, `js-reveal` applied, an element below the fold sits at
  `translateY(16px)` with opacity untouched and no `.in`, and after scrolling
  through, zero are left without `.in`.
- **Phase 5 complete, six 5s and two 4s.** Items 2 and 8 are Doni's decisions and
  are unchanged from last session. No `DONE` file: that needs all eight at 5.

## Session 5 — 2026-09-04

- **Chrome got further than ever and still errored.** `tabs_context_mcp` created a
  group, navigated and returned one screenshot; the next `Page.captureScreenshot`
  timed out at 30 s, as in session 4. Protocol says that is an error: tab closed,
  Playwright for the rest, no retries.
- **The one Chrome screenshot showed an empty hero phone, and it was not a bug.**
  Taken before first paint. Settled it by drawing the img to a canvas and
  sampling rather than by staring at the capture: `complete:true`,
  `naturalWidth:640`, average luminance 212/255. Recorded in STATUS.md so nobody
  spends a session fixing it.
- **Item 5 was wrong again, and again only at 390. Anchor jumps landed under the
  sticky header, FIXED.** The header is `sticky;top:0` at 65px and there was no
  `scroll-margin-top` or `scroll-padding-top` anywhere in the CSS. `#main` (the
  skip link, so the keyboard path) buried 17px of the `<h1>`; `#features` (nav)
  buried 8.5px of its eyebrow; `#how` ("See how it works") buried 9px of the
  CAPTURE eyebrow. `#privacy` was clear only because its section opens with a
  padded panel. 820 and 1440 were clean at every anchor, which is why six
  sessions of scoring missed it.
- **Fixed with one line: `html{scroll-padding-top:80px}`.** On the root rather
  than a `scroll-margin-top` per target, so an anchor added later is covered
  without remembering to. Confirmed by looking, not only measuring: at 390 the
  Capture eyebrow and full heading now sit clear of the bar with breathing room.
- **Added as the fifth `--probe` check, and proved it fails as well as passes.**
  Rule removed: 17.0/8.5/9.0px, exits 1. Rule in place: 0.0px at all three
  widths, exits 0. First cut of the check hardcoded `("#main","#how")` and so
  missed the two nav links, which are written `/peggy/#features` — it now
  collects same-page anchors from the DOM instead. Do not go back to matching
  `href^="#"`.
- **`--segments` added to review.py.** One viewport-sized PNG per screenful.
  Sessions 2, 3 and 4 each built this in a scratchpad and threw it away, and a
  scaled full-page capture is what hid the missing figcaptions for five
  sessions. Documented in `site/README.md` alongside the anchors check.
- **Re-verified the rest rather than trusting the table.** Lighthouse re-run on
  the changed build: 100 desktop and 100 mobile across all four categories, CLS
  0, LCP 1.1s. Every internal link and fragment on all three pages resolves (0
  broken). Home and privacy pages read at 390 in both themes, not just swept.
  Build still leaves `git diff` empty.
- **Phase 5 still open at six 5s and two 4s.** Items 2 (typeface) and 8
  (dark-theme screenshots) are Doni's decisions, unchanged. No `DONE` file: that
  needs all eight at 5.

## Session 6 — 2026-09-04

- **Chrome worked, for the first time in six sessions.** `tabs_context_mcp`
  reported no group, `navigate` made one and loaded the page, and two
  `screenshot` calls in a row both returned -- no 30 s `captureScreenshot`
  timeout as in sessions 4 and 5. Hero and Capture section both render
  correctly in real Chrome at 1400 dark. Tab closed after. STATUS.md now says
  `chrome: available`. Playwright still supplies every number, being the only
  one that can set a viewport, a scheme and a root font size.
- **Phase 5, cycle 1. Item 2 was not the 4 it had carried for three sessions.**
  Every font-size on the page is `rem` or `clamp()` except one: `body{font:400
  17px/1.65 ...}`. At root 24px (Chrome's "Very large") 36 of the 90 text
  elements on /peggy/ did not move -- both hero buttons, the skip link, "Read
  the privacy policy", the six FAQ questions and their answers, the claim spans
  -- under an h1 grown to 57.6px and a lede at 28.8px. Looked at it as well:
  the FAQ reads as a different page's typography from the CTA beneath it.
  Scored 3.
- **Fixed with one value, 3 -> 4.** `1.0625rem` is 17px at the default root, so
  nothing moves for a reader who has not changed the setting: the 390 dark
  segment captures are pixel-identical before and after (ImageChops bbox None).
  After: 0 of 90 frozen on /peggy/, 0 of 13 on the home page, 0 of 32 on
  privacy, scrollWidth still 390/1440 at root 24. Still 4, not 5 -- system-ui
  is the cap and the typeface is Doni's call.
- **Sixth `--probe` check: text size.** Reads every element's computed size,
  sets the root to 24px, reads again, fails on any that did not move, and
  re-checks scrollWidth at that size. Proved it fails as well as passes: 17px
  restored gives 36 of 112 frozen at 390 and 1440 and exits 1; 1.0625rem gives
  0 of 112 and exits 0. Documented in site/README.md beside the other five.
  The lesson beside "three good widths are not a responsive check": default
  settings are not a reading check, because at the default a frozen 17px is
  exactly the right 17px.
- **Rescored the other seven from captures, no other change.** Lighthouse re-run
  on the changed build: 100 on performance, accessibility, best-practices and
  SEO, desktop and mobile, CLS 0, LCP 1.1 s mobile / 0.3 s desktop. Probe clean
  on all six checks. Clipping check at root 16 and 24 across all three pages at
  390/820/1440 with every `<details>` open: nothing clipped or outside the
  viewport but the deliberately parked skip link. Item 3 re-checked at 820 light
  and 390 dark, every phone bezelled and captioned.
- **Item 8: tested one dark-theme theory and it was wrong.** The sticky header
  is 82% of the bg plus a blur, and it passes over near-white phone screenshots
  in dark mode; it does not wash out at 390 or 1440, the bar stays dark and the
  wordmark legible. So the light screenshots are genuinely the only thing
  holding item 8 at 4, and that remains Doni's call.
- **Phase 5 still open at six 5s and two 4s.** Items 2 (typeface) and 8
  (dark-theme screenshots) are Doni's decisions, unchanged. No `DONE` file:
  that needs all eight at 5.
