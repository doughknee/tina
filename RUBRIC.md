# RUBRIC — the Peggy site, scored 1-5

Eight items. Score every one into `STATUS.md` each scoring pass, with a one-line
reason. **Never score an item by reading CSS. Score what you saw** in a fresh
capture at the widths and themes the item names. A score you cannot point at a
screenshot for is not a score.

A phase advances only when every item in it is 4 or higher. 5 means there is
nothing left to say about it.

---

## 1. Hero communicates the product in under 5 seconds

Cover everything below the first screen. What is left must say what Peggy does,
who it is for, and what to press — without the visitor reading the lede twice.

- **1** — Blank, broken, or nothing above the fold says what the app is.
- **2** — Words are present but generic; could be any task app.
- **3** — Reads correctly, but takes a second pass or the CTA competes with the nav.
- **4** — The headline lands alone. One obvious button. The screenshot supports it.
- **5** — Headline, shot and button say the same thing three ways in one glance.

## 2. Type scale has real personality

- **1** — Browser defaults, or one size doing every job.
- **2** — Sizes vary but the rhythm is arbitrary; heading and body feel unrelated.
- **3** — A consistent scale, no character. Looks like a framework.
- **4** — Deliberate contrast between display and body; measure holds 60-75ch;
  the page has a voice you would recognise cropped.
- **5** — The scale itself carries the product's plainness. Nothing decorative,
  nothing timid.

## 3. Screenshots shown in context

- **1** — Missing, broken, or floating with no frame.
- **2** — Present but decorative; no relationship to the words beside them.
- **3** — Framed and captioned, but the caption restates the heading.
- **4** — Each shot sits next to the claim it proves, and shows the exact screen
  that claim describes.
- **5** — You could read the page with the words removed and still learn the app.

## 4. Copy specific to Peggy, no filler

Judged against `BRIEF.md`. Every claim traces to `CHANGELOG.md` or the code.

- **1** — Marketing boilerplate. Swap the name and it sells anything.
- **2** — Mostly generic with a few real details.
- **3** — Accurate but flabby; sentences that could be cut without loss.
- **4** — Concrete throughout. Named features, real syntax, no adjective doing a
  screenshot's job.
- **5** — Nothing removable. Reads like the app sounds.

## 5. Responsive at 390 / 820 / 1440

- **1** — Horizontal scroll or overlap at any width.
- **2** — No overflow, but the layout is a stretched desktop at 390.
- **3** — All three usable; awkward gaps or orphaned lines at one of them.
- **4** — Each width looks intended, not tolerated. Phone frames stay inside the
  viewport, `.pair` duos wrap, the grid reflows.
- **5** — The 390 view is the one you would show someone first.

## 6. Lighthouse over 95

Performance, Accessibility, Best Practices, SEO — all four, on the built page.

- **1** — Any category under 70, or the run fails.
- **2** — Any category under 85.
- **3** — All four 90+.
- **4** — All four 95+.
- **5** — All four 100, including the accessibility items Lighthouse cannot
  automate: skip link, visible focus, heading order, contrast.

## 7. Motion purposeful, never blocks reading

- **1** — Content invisible without JS, or motion hides text at any point.
- **2** — Visible without JS, but animation is decorative and everywhere.
- **3** — Restrained, but fires on things that did not need it.
- **4** — Motion only where it earns attention; `prefers-reduced-motion` honoured;
  a JS-off load is fully readable; nothing animates above the fold on arrival.
- **5** — You would not notice it was there, and you would notice if it left.

## 8. Dark mode designed, not inverted

- **1** — No dark mode, or unreadable in it.
- **2** — Colours flip; contrast breaks somewhere (`.claims`, `.parse`, `.card`).
- **3** — Legible but grey and flat; the brand colour is the light-mode one.
- **4** — Surfaces, borders and brand tuned for dark separately. Screenshots do
  not glare. Both themes read as the same product.
- **5** — Dark is the one you would ship if you could only ship one.
