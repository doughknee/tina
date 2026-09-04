"""Screenshots the built site for visual review, and measures the three things
a screenshot cannot tell you.

    python site/review.py --widths 1280 --scheme light
    python site/review.py --full            # all three widths, both themes
    python site/review.py --nojs            # JS disabled, to prove nothing is hidden
    python site/review.py --probe           # the numbers behind the rubric

Writes PNGs to site/review/. That directory is gitignored: these are working
images for a review pass, not site content.

--probe answers the rubric lines that cannot be scored by eye, and prints
numbers you can paste into STATUS.md:

  overflow  documentElement.scrollWidth against the viewport, every width x
            theme. Anything over the viewport is a sideways scrollbar.
  motion    fast-scrolls the page and samples the computed opacity of every
            .reveal overlapping the middle 20-80% of the viewport -- where a
            reader's eye is. Any value below 1.0 there is the animation
            delaying reading. This is why the reveal moves but does not fade.
  sweep     every width from 360 to 1680, not only the three the rubric
            names. Overflow anywhere, and the number of times the .pair
            duos flip between side-by-side and stacked -- one transition
            is the design, more than one is a column too narrow for its
            phones over some band in the middle.
  hidden    loads with JavaScript off and with reduced motion asked for, and
            fails if any .reveal is transparent or displaced in either. Hiding
            is opt-in, so both of those loads must show everything.
"""
import argparse
import pathlib
import sys

from playwright.sync_api import sync_playwright

OUT = pathlib.Path(__file__).resolve().parent / "review"
SIZES = {390: 844, 820: 1180, 1440: 900, 1280: 900}


def scroll_through(page, step):
    """Walk the page top to bottom and back.

    loading="lazy" images below the fold are still unloaded when Playwright
    composites a full_page screenshot, so they come out as blank frames. The
    wheel works with JavaScript disabled, which page.evaluate does not.
    """
    for _ in range(60):
        page.mouse.wheel(0, step)
        page.wait_for_timeout(60)
    page.wait_for_timeout(400)
    page.mouse.wheel(0, -60 * step)
    page.wait_for_timeout(400)


# Scrolls the page for 240 frames and records the opacity of every .reveal in
# the reading band. Runs in the page because it has to sample per frame; doing
# it from Python would miss the frames that matter.
SWEEP_JS = """
() => ({
  over: document.documentElement.scrollWidth > innerWidth,
  rows: [...document.querySelectorAll('.pair')].map(pr => {
    const f = [...pr.children];
    return f.length === 2 &&
      Math.abs(f[0].getBoundingClientRect().top - f[1].getBoundingClientRect().top) < 2;
  }),
})
"""

MOTION_JS = """
() => new Promise(resolve => {
  const samples = [];
  const band = [innerHeight * 0.20, innerHeight * 0.80];
  let frames = 0;
  const tick = () => {
    for (const el of document.querySelectorAll('.reveal')) {
      const r = el.getBoundingClientRect();
      if (r.bottom < band[0] || r.top > band[1] || r.height === 0) continue;
      samples.push(+getComputedStyle(el).opacity);
    }
    if (++frames < 240) { scrollBy(0, 42); requestAnimationFrame(tick); }
    else resolve(samples);
  };
  requestAnimationFrame(tick);
})
"""

# A .reveal that is transparent or displaced is content the reader cannot have.
HIDDEN_JS = """() => [...document.querySelectorAll('.reveal')].filter(el => {
  const cs = getComputedStyle(el);
  return +cs.opacity < 0.999 || cs.transform !== 'none';
}).length"""


# Every in-page link on the page. Both land at the top of something that the
# sticky header would otherwise cover: #main is where the skip link puts a
# keyboard user, #how is the hero's "See how it works".
HREFS = ("#main", "#how")

# How far the target's own label is tucked under the sticky header after the
# jump. An empty anchor div stands in front of the section it marks, so measure
# the section, not the div.
ANCHOR_JS = """
(href) => {
  const bar = document.querySelector('header').getBoundingClientRect().height;
  const t = document.querySelector(href);
  const sec = (t.tagName === 'DIV' && !t.children.length) ? t.nextElementSibling : t;
  let worst = {under: 0, what: ''};
  for (const sel of ['.eyebrow', 'h1', 'h2']) {
    const e = sec.querySelector(sel);
    if (!e) continue;
    const r = e.getBoundingClientRect();
    const under = Math.max(0, Math.min(bar, r.bottom) - r.top);
    if (under > worst.under)
      worst = {under, what: sel + ' ' + JSON.stringify(e.innerText.trim().slice(0, 26))};
  }
  return worst;
}
"""


def shoot_segments(page, w, vh, scheme, tag):
    """One viewport-sized PNG per screenful, top to bottom.

    A full-page PNG of a 13,000px page is unreadable once it is scaled back
    down to look at, which is how a caption went missing for five sessions.
    The wheel is used rather than scrollTo so this still works with --nojs.
    """
    names = []
    page.mouse.wheel(0, -vh * 60)
    page.wait_for_timeout(400)
    for i in range(40):
        name = f"{w}-{scheme}{tag}-seg{i:02d}.png"
        page.screenshot(path=str(OUT / name))
        names.append(name)
        before = _scroll_y(page)
        page.mouse.wheel(0, vh)
        page.wait_for_timeout(350)
        if before is not None and _scroll_y(page) == before:
            break
    return names


def _scroll_y(page):
    """None with --nojs, where evaluate has no engine to run in."""
    try:
        return page.evaluate("scrollY")
    except Exception:
        return None


def probe(url):
    """Prints the measurements behind the rubric. Returns 1 if anything fails."""
    bad = 0
    with sync_playwright() as p:
        browser = p.chromium.launch()

        print("overflow  (scrollWidth must equal the viewport)")
        for scheme in ("light", "dark"):
            ctx = browser.new_context(color_scheme=scheme)
            page = ctx.new_page()
            for w, h in ((390, 844), (820, 1180), (1440, 900)):
                page.set_viewport_size({"width": w, "height": h})
                page.goto(url, wait_until="networkidle")
                page.wait_for_timeout(400)
                sw = page.evaluate("document.documentElement.scrollWidth")
                ok = sw <= w
                bad += not ok
                print(f"    {w:>4} {scheme:<5} scrollWidth={sw:<5} {'ok' if ok else 'OVERFLOW'}")
            ctx.close()

        print("sweep     (every width, not just the three the rubric names)")
        ctx = browser.new_context(viewport={"width": 1440, "height": 900})
        page = ctx.new_page()
        page.goto(url, wait_until="networkidle")
        over, flips, prev = [], 0, None
        for w in range(360, 1681, 20):
            page.set_viewport_size({"width": w, "height": 900})
            page.wait_for_timeout(80)
            r = page.evaluate(SWEEP_JS)
            if r["over"]:
                over.append(w)
            # A duo is side by side or stacked. Going side by side, back to
            # stacked, then side by side again is a column too narrow for its
            # phones over some band in the middle: one transition is the
            # design, more than one is the bug.
            cur = all(r["rows"])
            if prev is not None and cur != prev:
                flips += 1
            prev = cur
        ctx.close()
        ok = not over and flips <= 1
        bad += not ok
        print(f"    360-1680   overflow at {over or 'none'}, "
              f"{flips} duo transition{'' if flips == 1 else 's'} "
              f"{'ok' if ok else 'REFLOW BUG'}")

        print("motion    (.reveal opacity in the middle 20-80% during a fast scroll)")
        for w, h in ((390, 844), (1440, 900)):
            ctx = browser.new_context(viewport={"width": w, "height": h})
            page = ctx.new_page()
            page.goto(url, wait_until="networkidle")
            page.wait_for_timeout(500)
            s = page.evaluate(MOTION_JS)
            faded = [x for x in s if x < 0.995]
            bad += bool(faded)
            print(f"    {w:>4}       {len(faded):>4} of {len(s):<5} mid-fade, "
                  f"worst {min(s) if s else 1:.3f} {'ok' if not faded else 'DELAYS READING'}")
            ctx.close()

        print("anchors   (an anchor jump must clear the sticky header)")
        for w, h in ((390, 844), (820, 1180), (1440, 900)):
            ctx = browser.new_context(viewport={"width": w, "height": h})
            page = ctx.new_page()
            page.goto(url, wait_until="networkidle")
            page.wait_for_timeout(400)
            for href in HREFS:
                page.goto(url, wait_until="networkidle")
                page.wait_for_timeout(250)
                page.evaluate(f"location.hash={href!r}")
                page.wait_for_timeout(700)
                worst = page.evaluate(ANCHOR_JS, href)
                bad += bool(worst["under"] > 1)
                print(f"    {w:>5} {href:<7} {worst['under']:>5.1f}px under the bar "
                      f"{'ok' if worst['under'] <= 1 else 'HIDDEN BY HEADER'}"
                      f"  {worst['what']}")
            ctx.close()

        print("hidden    (nothing may be transparent or displaced without JS)")
        for label, kw in (("js-off", {"java_script_enabled": False}),
                          ("reduced", {"reduced_motion": "reduce"})):
            ctx = browser.new_context(viewport={"width": 1440, "height": 900}, **kw)
            page = ctx.new_page()
            page.goto(url, wait_until="networkidle")
            page.wait_for_timeout(600)
            n = page.evaluate(HIDDEN_JS)
            bad += bool(n)
            print(f"    {label:<9} {n:>4} hidden or displaced {'ok' if not n else 'CONTENT LOST'}")
            ctx.close()

        browser.close()
    print("FAILURES:", bad) if bad else print("all clear")
    return 1 if bad else 0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--url", default="http://127.0.0.1:8899/peggy/")
    ap.add_argument("--widths", default="390,820,1440", help="comma separated")
    ap.add_argument("--scheme", default="light,dark", help="light, dark or both")
    ap.add_argument("--tag", default="", help="suffix for the filenames")
    ap.add_argument("--full-page", action="store_true", help="whole page, not just the fold")
    ap.add_argument("--segments", action="store_true",
                    help="the whole page a screenful at a time, readable at 1:1")
    ap.add_argument("--nojs", action="store_true", help="load with JavaScript disabled")
    ap.add_argument("--probe", action="store_true", help="measure instead of screenshot")
    a = ap.parse_args()

    if a.probe:
        return probe(a.url)

    widths = [int(w) for w in a.widths.split(",")]
    schemes = a.scheme.split(",")
    OUT.mkdir(exist_ok=True)
    written = []

    with sync_playwright() as p:
        browser = p.chromium.launch()
        for scheme in schemes:
            ctx = browser.new_context(java_script_enabled=not a.nojs,
                                      color_scheme=scheme, device_scale_factor=1)
            page = ctx.new_page()
            for w in widths:
                page.set_viewport_size({"width": w, "height": SIZES.get(w, 900)})
                page.goto(a.url, wait_until="networkidle")
                vh = SIZES.get(w, 900)
                if a.full_page or a.segments:
                    scroll_through(page, vh)
                page.wait_for_timeout(500)
                if a.segments:
                    written += shoot_segments(page, w, vh, scheme, a.tag)
                    continue
                name = f"{w}-{scheme}{'-nojs' if a.nojs else ''}{a.tag}.png"
                page.screenshot(path=str(OUT / name), full_page=a.full_page)
                written.append(name)
            ctx.close()
        browser.close()

    for n in written:
        print("wrote", n)


if __name__ == "__main__":
    sys.exit(main())
