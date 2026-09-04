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
