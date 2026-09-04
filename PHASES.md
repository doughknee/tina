# PHASES

Work the current phase to completion, then advance. Advance only when every item
for the phase is done, or scores 4 or higher on `RUBRIC.md`.

| Phase | Budget | Scope |
|---|---|---|
| 1 | gate | `PLAN.md` section 1 — rendering correctness |
| 2 | 1.5h | `PLAN.md` sections 2, 3, 4 — responsive/theme, a11y, content accuracy |
| 3 | 1h | `PLAN.md` section 6, plus section 5's `.gitignore` and Pillow workflow steps |
| 4 | 4h | `RUBRIC.md` loop |
| 5 | 0.5h | Final rubric pass, clean build, commit |

## Phase 1 is a gate

Nothing else starts until the hero headline, lede, both buttons and the note
under them are visible in a fresh screenshot at 1280 wide. Not "the CSS looks
right" — visible, in a capture, read back.

## Phases 1-3: the loop

1. Take the next unchecked `PLAN.md` item for the phase.
2. Do it.
3. Verify with the command that item names.
4. Check it off.
5. Commit.

## Phases 4-5: the loop

1. Score every `RUBRIC.md` line into `STATUS.md`, one line of reason each.
2. Fix **only the lowest-scoring item**.
3. Rebuild, recapture, rescore.
4. If the score did not improve, `git` revert it and note in `STATUS.md` what
   failed, so it is never tried again.
5. Commit naming the item and the score change.
6. Append one line to `LOG.md`: what changed, and the result.

## Standing rules

- Read `LOG.md` before deciding anything. Never retry an approach logged failed.
- Never rewrite a section scoring 4 or higher from scratch.
- The build must pass before any commit. Never leave a regression.
- Branch `site/overnight` only. Never commit or push `main`. No deploy commands.
- `PLAN.md` sections 7 and 8 are out of scope.
- Never run `site/demo_data.py`. The eight screenshots are inputs, not outputs.
- `print()` in `build.py` stays ASCII.

## Out of my hands

Take the safe option, note it in `STATUS.md` under "For Doni to decide", move on:

- **Support email in the footer** — leave it out.
- **The Pro paragraph in the FAQ** — cut the tier claim rather than publish an
  unconfirmed promise.

## Done

All phases complete and every rubric item at 5: create a file named `DONE` at the
repo root.
