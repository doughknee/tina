"""Generates the static pages for doughknee.com/peggy from docs/PRIVACY.md.

Run `python site/build.py`, then publish the `site/` folder (GitHub Pages with the custom
domain, or any static host). No dependencies beyond the standard library.
"""
import html
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parent.parent
SITE = ROOT / "site"

STYLE = """
:root { color-scheme: light dark; --bg: #fbfbfd; --fg: #1c1c22; --muted: #5d5f6b; --accent: #4f5fd6; --card: #ffffff; --line: #e6e6ee; }
@media (prefers-color-scheme: dark) { :root { --bg: #121216; --fg: #ececf2; --muted: #a0a2b0; --accent: #9aa5ff; --card: #1c1c22; --line: #2a2a33; } }
* { box-sizing: border-box; }
body { margin: 0; background: var(--bg); color: var(--fg); font: 17px/1.6 system-ui, -apple-system, "Segoe UI", Roboto, sans-serif; }
main { max-width: 680px; margin: 0 auto; padding: 48px 24px 96px; }
h1 { font-size: 2.4rem; line-height: 1.15; margin: 0 0 8px; letter-spacing: -0.02em; }
h2 { font-size: 1.25rem; margin: 40px 0 8px; }
p, li { color: var(--fg); }
.muted { color: var(--muted); }
a { color: var(--accent); }
.hero { padding: 48px 0 24px; }
.claim { font-size: 1.35rem; color: var(--muted); margin: 0 0 28px; }
.btn { display: inline-block; background: var(--accent); color: #fff; text-decoration: none; padding: 12px 20px; border-radius: 999px; font-weight: 600; }
.grid { display: grid; gap: 12px; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); margin: 32px 0; }
.card { background: var(--card); border: 1px solid var(--line); border-radius: 16px; padding: 16px 18px; }
.card strong { display: block; margin-bottom: 4px; }
footer { margin-top: 64px; font-size: 0.9rem; color: var(--muted); }
footer a { color: inherit; }
.icon { width: 72px; height: 72px; border-radius: 20px; background: #4f5fd6; display: grid; place-items: center; margin-bottom: 20px; }
"""

PLAY_URL = "https://play.google.com/store/apps/details?id=com.peggy.app"
ICON_SVG = (
    '<svg width="44" height="44" viewBox="0 0 108 108" fill="none" xmlns="http://www.w3.org/2000/svg">'
    '<path d="M30 56 L47 72 L80 38" stroke="#fff" stroke-width="10" stroke-linecap="round" stroke-linejoin="round"/>'
    '<path d="M30 86 H80" stroke="#fff" stroke-opacity="0.7" stroke-width="8" stroke-linecap="round"/></svg>'
)


def page(title, body, description):
    return f"""<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{html.escape(title)}</title>
<meta name="description" content="{html.escape(description)}">
<style>{STYLE}</style>
</head>
<body>
<main>
{body}
<footer>© Brandon Harris · <a href="/peggy/">Peggy</a> · <a href="/peggy/privacy/">Privacy</a></footer>
</main>
</body>
</html>
"""


def inline(text):
    text = html.escape(text, quote=False)
    text = re.sub(r"\*\*(.+?)\*\*", r"<strong>\1</strong>", text)
    text = re.sub(r"`(.+?)`", r"<code>\1</code>", text)
    text = re.sub(r"\[(.+?)\]\((.+?)\)", r'<a href="\2">\1</a>', text)
    return text


def markdown(md):
    """Headings, paragraphs, bullet lists, bold, code, links. That is all PRIVACY.md uses."""
    out, para, items = [], [], []

    def flush():
        nonlocal para, items
        if para:
            out.append(f"<p>{inline(' '.join(para))}</p>")
            para = []
        if items:
            out.append("<ul>" + "".join(f"<li>{inline(i)}</li>" for i in items) + "</ul>")
            items = []

    for line in md.splitlines():
        if line.startswith("# "):
            flush(); out.append(f"<h1>{inline(line[2:])}</h1>")
        elif line.startswith("## "):
            flush(); out.append(f"<h2>{inline(line[3:])}</h2>")
        elif line.startswith("- "):
            if para: flush()
            items.append(line[2:])
        elif line.strip() == "":
            flush()
        else:
            if items: flush()
            para.append(line.strip())
    flush()
    return "\n".join(out)


def build():
    privacy_md = (ROOT / "docs" / "PRIVACY.md").read_text(encoding="utf-8")
    privacy = SITE / "peggy" / "privacy" / "index.html"
    privacy.parent.mkdir(parents=True, exist_ok=True)
    privacy.write_text(page("Peggy privacy policy", markdown(privacy_md), "How Peggy handles your data: on your device, no account, no analytics."), encoding="utf-8")

    landing = f"""
<div class="hero">
  <div class="icon">{ICON_SVG}</div>
  <h1>Peggy</h1>
  <p class="claim">Two seconds from thought to done. Sort it later.</p>
  <a class="btn" href="{PLAY_URL}">Get it on Google Play</a>
</div>
<div class="grid">
  <div class="card"><strong>Capture</strong>Type "dentist thursday 3pm" and it is a scheduled task. From the widget, the quick-settings tile, or the share sheet, without opening the app.</div>
  <div class="card"><strong>Sort</strong>Everything lands in one inbox. Swipe to put it on today, someday, or in the bin. Undo everywhere.</div>
  <div class="card"><strong>Plan</strong>Day, week, month. Repeating tasks that ring every time, and reminders that survive reboots and time zones.</div>
  <div class="card"><strong>Yours</strong>No account, no cloud, works offline. Your data stays on your phone, and you can export all of it any time.</div>
</div>
<p class="muted">Free forever for capture, plan and sort. Peggy Pro adds hosted AI, themes and more.</p>
"""
    (SITE / "peggy").mkdir(parents=True, exist_ok=True)
    (SITE / "peggy" / "index.html").write_text(page("Peggy", landing, "Peggy: capture, plan, sort. A quick-capture to-do app for Android that keeps your data on your device."), encoding="utf-8")

    home = """
<div class="hero">
  <h1>doughknee</h1>
  <p class="claim">Apps by Brandon Harris.</p>
</div>
<div class="grid">
  <div class="card"><strong><a href="/peggy/">Peggy</a></strong>Two seconds from thought to done. A quick-capture to-do app for Android.</div>
</div>
"""
    (SITE / "index.html").write_text(page("doughknee", home, "Apps by Brandon Harris."), encoding="utf-8")
    (SITE / "CNAME").write_text("doughknee.com\n", encoding="utf-8")
    print("built:", SITE / "index.html", SITE / "peggy" / "index.html", privacy)


if __name__ == "__main__":
    build()
