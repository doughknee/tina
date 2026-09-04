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
