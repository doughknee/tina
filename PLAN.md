# Marketing site: what is left to ship

Everything below is unfinished. `STATUS.md` covers what already works and the traps that are
already paid for. Tasks are in dependency order; each says what "done" looks like and the
command that proves it.

Serve the built site locally for any visual step:

```bash
cd site && python -m http.server 8899 --bind 127.0.0.1
# then open http://127.0.0.1:8899/peggy/
```

---

## 1. Rendering correctness

- [x] **Confirm the hero actually renders.** The one screenshot taken of the built page at
      1280×900 showed the nav and the phone image, but the left column (the `<h1>`, the lede and
      the two CTA buttons) was blank. Unresolved: it may be a paint-timing artifact of the
      screenshot, or a real CSS bug in `.hero-grid`. Do not treat any later visual task as done
      until this is settled.
      *Done when:* the hero headline, lede, both buttons and the note under them are visible in a
      fresh screenshot at 1280 wide.
      *Proves it:* load `http://127.0.0.1:8899/peggy/`, screenshot, and read the text in it.

- [ ] **Check every section renders in order** — hero, claims strip, capture, sort, plan, ideas,
      tags, feature grid, privacy band, FAQ, closing CTA, footer.
      *Done when:* each of the twelve is present once, in that order, with no empty or collapsed
      block.

- [ ] **Confirm the reveal animation never hides content.** Elements carry `.reveal`
      (`opacity:0`) and only get `.in` from an IntersectionObserver.
      *Done when:* with JavaScript disabled, every `.reveal` block is still readable.
      *Proves it:* load the page with JS off; nothing should be invisible. If it is, invert the
      approach so the animation is opt-in from JS rather than opt-out.

## 2. Responsive and theme QA

- [ ] **Three widths: 390, 820, 1440.** Check the hero stacks, the `.pair` phone duos wrap
      instead of overflowing, the feature grid reflows, and nothing scrolls sideways.
      *Done when:* no horizontal scrollbar at any of the three, and the phone frames never exceed
      the viewport.
- [ ] **Light and dark.** The CSS is `prefers-color-scheme` only, with no manual toggle.
      *Done when:* both themes read correctly, especially the `.claims` strip, `.parse` table and
      `.card` borders, which are the places the token swap is most likely to fall down.
- [ ] **Verify the phone frame against the screenshots.** `.phone img` hard-codes
      `aspect-ratio:1080/2400`; every current PNG is 1080×2400, so a future shot from a
      differently-shaped device would distort.
      *Done when:* either the aspect ratio is derived per image from the manifest, or
      `site/README.md` states the requirement and `build.py` fails on a mismatch.

## 3. Accessibility and semantics

- [ ] **Add a skip link** to the main content. There is none.
- [ ] **Add visible focus styles.** The CSS defines no `:focus-visible`; keyboard users currently
      get only the browser default, which is invisible against some of these backgrounds.
- [ ] **Check heading order.** One `<h1>` per page, no level skipped.
      *Proves it:* read the rendered outline, or run any HTML outline checker.
- [ ] **Check colour contrast** for `--muted` on `--bg` and `--panel` in both themes, and for
      `.btn` text on `--brand`.
      *Done when:* body text and UI labels clear 4.5:1, large headings clear 3:1.
- [ ] **Confirm every screenshot's `alt` describes the screen**, not the file. They are written in
      `site/screenshots.json` and are currently prose; re-read them once the page is final.

## 4. Content accuracy

- [ ] **Re-check every factual claim on the page against the shipped app**, particularly: Android
      12 and up (minSdk 31), reminders surviving reboot and time-zone change, quiet hours,
      widgets and the quick-settings tile, checklists, export and import, app lock, and
      "no analytics, no server".
      *Done when:* each claim traces to something in `CHANGELOG.md` or the code.
- [ ] **Decide the support contact.** The footer deliberately has no email — the Play listing
      carries one, and publishing a personal address on a public page was not mine to decide.
      *Done when:* either an address (or a form/alias) is added to the footer, or it is a
      deliberate no.
- [ ] **Re-read the Pro paragraph in the FAQ.** It promises a paid tier "later" with hosted AI,
      themes and widgets. Confirm that is still the intent before it is public.

## 5. Deploy plumbing

- [ ] **Commit the generated artifacts** so the CI build never depends on Pillow:
      `site/peggy/shots/*.webp` and `site/peggy/og.png`.
      *Done when:* `git status` is clean after a fresh `python site/build.py`.
- [ ] **Add Pillow to the Pages workflow** so a newly dropped PNG is re-encoded on CI rather than
      silently reusing a stale WebP. In `.github/workflows/pages.yml`, before `python
      site/build.py`: `- run: pip install pillow`.
      *Done when:* the workflow file has the step and a push builds green.
- [x] **Ignore the generated SQL.** `site/demo_data.sql` is written by `demo_data.py` on every
      run; add it to `.gitignore` (or have the script write to a temp path instead).
- [ ] **Confirm the workflow path filter still matches.** `pages.yml` triggers on `site/**` and
      `docs/PRIVACY.md`; the new screenshots and manifest live under `site/`, so it should.
      *Done when:* a push touching only `site/screenshots/*.png` triggers a deploy.

## 6. The updating story (the one explicit request)

- [ ] **Write `site/README.md`.** This is the deliverable that makes the whole pipeline useful and
      it does not exist yet. It must cover, in order:
      1. Replacing one screenshot: drop a PNG over `site/screenshots/<name>.png`, run
         `python site/build.py`, commit. Nothing else.
      2. Retaking them all from a device: `python site/demo_data.py` then
         `python site/screenshots.py`, with the `--serial`, `--package` and `--only` flags.
      3. Adding a new screenshot: take it, add an entry to `site/screenshots.json`, reference the
         id from a section in `build.py`.
      4. Changing the demo content: edit `build()` in `site/demo_data.py` — dates are relative to
         today, so the shots never look stale.
      5. The device requirements: a rooted emulator for seeding, the dev build installed, and
         `adb` on the path.
      6. Swapping the CTA at launch: `PRIMARY_URL` / `PRIMARY_LABEL` at the top of `build.py`.
      *Done when:* someone who has not seen tonight can replace a screenshot using only that file.
- [ ] **Point the repo README at it** from the Development section, next to the
      "Keeping Settings honest" note.

## 7. Ship

- [ ] **Build clean and commit.** `python site/build.py` then commit `site/`.
- [ ] **Push to `main`** and watch the Pages workflow.
      *Proves it:* `gh run list --branch main --limit 3` then `gh run watch <id>`.
- [ ] **Verify live**, not just green: `https://doughknee.com/peggy/`,
      `https://doughknee.com/peggy/privacy/` and `https://doughknee.com/`.
      *Done when:* all three return 200, the screenshots load, and the CTA goes to the testing
      opt-in page.
      *Proves it:* `curl -sI https://doughknee.com/peggy/ | head -1` and a browser load.
- [ ] **Check the social card.** Paste the URL somewhere that unfurls, or fetch the page and
      confirm `og:image` resolves to a real 1200×630 PNG.

## 8. After it is up

- [ ] **Add the site link to the Play listing** (Store presence → Main store listing → website).
- [ ] **Update the Play memory and Linear** with the site URL and the update procedure.
- [ ] **When Peggy reaches production**, flip `PRIMARY_URL` to `STORE_URL` and change
      `PRIMARY_LABEL` to "Get it on Google Play", then rebuild and push.
