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
