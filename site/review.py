"""Screenshots the built site for visual review.

    python site/review.py --widths 1280 --scheme light
    python site/review.py --full            # all three widths, both themes
    python site/review.py --nojs            # JS disabled, to prove nothing is hidden

Writes PNGs to site/review/. That directory is gitignored: these are working
images for a review pass, not site content.
"""
import argparse
import pathlib
import sys

from playwright.sync_api import sync_playwright

OUT = pathlib.Path(__file__).resolve().parent / "review"
SIZES = {390: 844, 820: 1180, 1440: 900, 1280: 900}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--url", default="http://127.0.0.1:8899/peggy/")
    ap.add_argument("--widths", default="390,820,1440", help="comma separated")
    ap.add_argument("--scheme", default="light,dark", help="light, dark or both")
    ap.add_argument("--tag", default="", help="suffix for the filenames")
    ap.add_argument("--full-page", action="store_true", help="whole page, not just the fold")
    ap.add_argument("--nojs", action="store_true", help="load with JavaScript disabled")
    a = ap.parse_args()

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
                page.wait_for_timeout(500)
                name = f"{w}-{scheme}{'-nojs' if a.nojs else ''}{a.tag}.png"
                page.screenshot(path=str(OUT / name), full_page=a.full_page)
                written.append(name)
            ctx.close()
        browser.close()

    for n in written:
        print("wrote", n)


if __name__ == "__main__":
    sys.exit(main())
