"""Builds doughknee.com: the Peggy marketing page, the privacy policy, and the home page.

    python site/build.py

Screenshots come from site/screenshots.json and site/screenshots/*.png. The build resizes and
re-encodes them into site/peggy/shots/, so the only thing you ever touch to change a picture
is the PNG itself. See site/README.md.

Standard library only, except Pillow for the image step — and that step is skipped (with the
existing output reused) when Pillow is not installed, so the page always builds.
"""
import html
import json
import pathlib
import re
import shutil

try:
    from PIL import Image
except ImportError:  # the pages still build; the images just are not re-encoded
    Image = None

ROOT = pathlib.Path(__file__).resolve().parent.parent
SITE = ROOT / "site"
SHOTS_SRC = SITE / "screenshots"
SHOTS_OUT = SITE / "peggy" / "shots"

# Peggy is in closed testing until it reaches production; the opt-in page is the public door.
# When it goes live on Play, point PRIMARY_URL at STORE_URL and change PRIMARY_LABEL.
STORE_URL = "https://play.google.com/store/apps/details?id=com.peggy.app"
TEST_URL = "https://play.google.com/apps/testing/com.peggy.app"
PRIMARY_URL = TEST_URL
PRIMARY_LABEL = "Join the test on Google Play"
PRIMARY_NOTE = "Android 12 and up · free · no account"

BRAND = "#4f5fd6"
# The launcher icon, as a path pair: a pin with a tick in it.
MARK = (
    '<path d="M35.6 56.1a22 22 0 1 1 36.8 0L54 84Z" fill="{fill}"/>'
    '<path d="m42 45 8 8 16-16" fill="none" stroke="{stroke}" stroke-width="8" '
    'stroke-linecap="round" stroke-linejoin="round"/>'
)


def mark_svg(size, fill="#fff", stroke=BRAND):
    return (f'<svg width="{size}" height="{size}" viewBox="0 0 108 108" aria-hidden="true" '
            f'xmlns="http://www.w3.org/2000/svg">{MARK.format(fill=fill, stroke=stroke)}</svg>')


CSS = """
*,*::before,*::after{box-sizing:border-box}
:root{
  color-scheme:light dark;
  --bg:#fbfbfd; --panel:#fff; --ink:#15151b; --muted:#5c5e6b; --line:#e7e7ef;
  --brand:#4f5fd6; --on-brand:#fff; --tint:#eef0ff; --shadow:0 24px 60px -32px rgb(21 21 27/.28);
  --max:1120px;
}
@media (prefers-color-scheme:dark){:root{
  --bg:#0e0e13; --panel:#17171f; --ink:#ececf3; --muted:#a2a4b4; --line:#26262f;
  --brand:#9aa5ff; --on-brand:#14142a; --tint:#1b1c2c; --shadow:0 24px 60px -32px rgb(0 0 0/.7);
}}
/* The header is sticky and 65px tall, so an anchor jump would otherwise land
   with the target tucked under it -- 17px of the h1 after the skip link at 390,
   9px of the Capture eyebrow after "See how it works". On html rather than a
   scroll-margin-top per target, so any anchor added later is covered too. */
html{-webkit-text-size-adjust:100%;scroll-padding-top:80px}
/* 1.0625rem is 17px at the default root, so nothing moves for most people --
   but a px here froze 36 of the 90 text elements, including both hero
   buttons, the skip link and every FAQ question, while the rem-sized
   headings around them grew. A reader on a larger font size got a 57.6px
   headline over 17px answers. Everything else on the page is already rem. */
body{margin:0;background:var(--bg);color:var(--ink);
  font:400 1.0625rem/1.65 system-ui,-apple-system,"Segoe UI",Roboto,"Helvetica Neue",sans-serif;
  -webkit-font-smoothing:antialiased}
img{max-width:100%;height:auto;display:block}
a{color:var(--brand)}

/* keyboard access. The outline is offset so that on a brand-coloured button it
   lands on the page background rather than disappearing into the fill. */
.skip{position:fixed;left:-9999px;top:0;z-index:100}
.skip:focus{left:10px;top:10px;padding:11px 18px;border-radius:11px;font-weight:600;
  background:var(--brand);color:var(--on-brand);text-decoration:none;box-shadow:var(--shadow)}
:focus-visible{outline:3px solid var(--brand);outline-offset:3px;border-radius:4px}
/* the skip target itself is focused programmatically; it should not draw a ring
   around the whole page when it receives that focus */
main:focus,main:focus-visible{outline:none}
h1,h2,h3{line-height:1.1;letter-spacing:-.022em;margin:0}
h1{font-size:clamp(2.4rem,6vw,4rem);font-weight:700}
h2{font-size:clamp(1.9rem,4vw,2.7rem);font-weight:700}
/* A card heading is sized by being a card heading, not by its level. On the
   Peggy page the cards sit under an h2 and are h3; on the home page there is
   no h2 above them, so that card is an h2 and must not become a section-sized
   headline. */
h3,.card h2{font-size:1.12rem;font-weight:650;letter-spacing:-.01em}
p{margin:0}
.wrap{max-width:var(--max);margin:0 auto;padding:0 24px}
.lede{font-size:clamp(1.06rem,2vw,1.3rem);color:var(--muted);max-width:56ch}
.muted{color:var(--muted)}
.eyebrow{font-size:.78rem;font-weight:700;letter-spacing:.14em;text-transform:uppercase;color:var(--brand)}

/* header */
header{position:sticky;top:0;z-index:20;background:color-mix(in srgb,var(--bg) 82%,transparent);
  backdrop-filter:saturate(1.6) blur(12px);border-bottom:1px solid transparent;transition:border-color .2s}
header.stuck{border-bottom-color:var(--line)}
.bar{display:flex;align-items:center;gap:16px;height:64px}
.brand{display:flex;align-items:center;gap:10px;font-weight:680;letter-spacing:-.02em;
  font-size:1.06rem;color:var(--ink);text-decoration:none}
.brand .badge{width:30px;height:30px;border-radius:9px;background:var(--brand);display:grid;place-items:center}
.bar nav{margin-left:auto;display:flex;align-items:center;gap:6px}
/* :not(.btn) matters -- .bar nav a is (0,2,1) and would otherwise beat .btn and
   .btn.small, taking over the pill's colour, padding, radius and hover. */
.bar nav a:not(.btn){color:var(--muted);text-decoration:none;font-size:.94rem;padding:8px 12px;border-radius:9px}
.bar nav a:not(.btn):hover{color:var(--ink);background:var(--tint)}
@media (max-width:640px){.bar nav .hide-sm{display:none}}

/* buttons */
.btn{display:inline-flex;align-items:center;gap:9px;background:var(--brand);color:var(--on-brand);
  text-decoration:none;font-weight:620;padding:13px 22px;border-radius:999px;border:1px solid transparent;
  transition:transform .12s ease,filter .12s ease}
.btn:hover{filter:brightness(1.07);transform:translateY(-1px)}
.btn.small{padding:9px 16px;font-size:.92rem}
.btn.ghost{background:transparent;color:var(--ink);border-color:var(--line)}
.btn.ghost:hover{background:var(--panel)}

/* hero */
.hero{padding:clamp(48px,8vw,104px) 0 clamp(40px,6vw,72px)}
.hero-grid{display:grid;gap:clamp(32px,5vw,64px);grid-template-columns:1fr;align-items:center}
@media (min-width:900px){.hero-grid{grid-template-columns:1.05fr .95fr}}
.hero h1{margin-bottom:18px}
.hero .lede{margin-bottom:28px}
.cta{display:flex;flex-wrap:wrap;gap:12px;align-items:center}
.cta-note{font-size:.9rem;color:var(--muted);margin-top:14px}
.hero-shot{justify-self:center}

/* phone frame */
.phone{width:min(272px,70vw);padding:9px;border-radius:40px;background:#0d0d12;
  box-shadow:var(--shadow),0 0 0 1px rgb(255 255 255/.06) inset}
/* No aspect-ratio here on purpose: every img carries the real PNG's width and
   height from the manifest, so the ratio comes per image and a shot from a
   differently-shaped device renders correctly instead of being squashed. */
.phone img{border-radius:32px;background:var(--panel)}
.shot{display:flex;flex-direction:column;align-items:center;gap:14px;margin:0}
.shot figcaption{font-size:.9rem;color:var(--muted);text-align:center;max-width:30ch}
.pair{display:flex;gap:clamp(14px,3vw,28px);justify-content:center;flex-wrap:wrap}
.pair .phone{width:min(228px,58vw)}
.pair .shot{max-width:min(228px,58vw)}
/* .row-grid goes two-column at 900, but the shot column is not wide enough for
   two 228px phones until 1080, so the duo stacked into a 1073px tower beside a
   350px text block. 20.5vw is the widest that fits two-up across the whole band
   (404px column at 900) and hands over to 228px at 1080. */
@media (min-width:900px) and (max-width:1079px){
  .pair .phone{width:min(228px,20.5vw)}
  .pair .shot{max-width:min(228px,20.5vw)}
}

/* claims strip */
.claims{border-block:1px solid var(--line);background:var(--panel)}
.claims ul{list-style:none;margin:0;padding:26px 0;display:grid;gap:20px;grid-template-columns:1fr}
@media (min-width:760px){.claims ul{grid-template-columns:repeat(3,1fr)}}
.claims li{display:flex;gap:12px;align-items:flex-start}
.claims strong{display:block;letter-spacing:-.01em}
.claims span{color:var(--muted);font-size:.94rem}
.claims svg{flex:none;margin-top:2px;color:var(--brand)}

/* alternating feature rows */
.row{padding:clamp(56px,8vw,104px) 0;border-bottom:1px solid var(--line)}
.row-grid{display:grid;gap:clamp(32px,5vw,72px);grid-template-columns:1fr;align-items:center}
@media (min-width:900px){
  .row-grid{grid-template-columns:1fr 1fr}
  .row.flip .row-grid > :first-child{order:2}
}
.row h2{margin:12px 0 16px}
.row .lede{margin-bottom:22px}
.points{list-style:none;margin:0;padding:0;display:grid;gap:12px}
.points li{display:flex;gap:11px;align-items:flex-start;color:var(--muted)}
.points li strong{color:var(--ink);font-weight:620}
.points svg{flex:none;margin-top:5px;color:var(--brand)}

/* parse examples */
.parse{margin:22px 0 0;border:1px solid var(--line);border-radius:16px;overflow:hidden;background:var(--panel)}
.parse div{display:grid;grid-template-columns:1fr;gap:4px;padding:13px 16px}
.parse div + div{border-top:1px solid var(--line)}
@media (min-width:520px){.parse div{grid-template-columns:1.1fr 1fr;gap:16px;align-items:baseline}}
.parse code{font:600 .92rem/1.5 ui-monospace,SFMono-Regular,Menlo,Consolas,monospace;color:var(--ink)}
.parse span{color:var(--muted);font-size:.92rem}

/* feature grid */
.grid{display:grid;gap:14px;grid-template-columns:repeat(auto-fit,minmax(232px,1fr));margin-top:36px}
.card{background:var(--panel);border:1px solid var(--line);border-radius:var(--radius);padding:20px}
.card h3{margin-bottom:6px}
.card p{color:var(--muted);font-size:.95rem}
.card .ico{width:34px;height:34px;border-radius:10px;background:var(--tint);color:var(--brand);
  display:grid;place-items:center;margin-bottom:13px}

/* privacy band */
.band{padding:clamp(56px,8vw,104px) 0;border-bottom:1px solid var(--line)}
.band .inner{background:var(--panel);border:1px solid var(--line);border-radius:24px;
  padding:clamp(28px,5vw,52px)}
.band h2{margin-bottom:16px}

/* faq */
.faq{padding:clamp(56px,8vw,104px) 0}
.faq .items{margin-top:28px;border-top:1px solid var(--line)}
details{border-bottom:1px solid var(--line)}
summary{cursor:pointer;list-style:none;padding:18px 40px 18px 0;font-weight:620;position:relative}
summary::-webkit-details-marker{display:none}
summary::after{content:"";position:absolute;right:8px;top:26px;width:9px;height:9px;
  border-right:2px solid var(--muted);border-bottom:2px solid var(--muted);transform:rotate(45deg);
  transition:transform .2s}
details[open] summary::after{transform:rotate(225deg)}
details p{padding:0 0 20px;color:var(--muted);max-width:70ch}

/* closing cta */
.close{padding:clamp(60px,9vw,116px) 0;text-align:center;border-top:1px solid var(--line)}
.close h2{margin-bottom:14px}
.close .lede{margin:0 auto 28px}
.close .cta{justify-content:center}

footer{border-top:1px solid var(--line);padding:34px 0 56px;color:var(--muted);font-size:.92rem}
.foot{display:flex;flex-wrap:wrap;gap:8px 20px;align-items:center}
footer a{color:var(--muted);text-decoration:none}
footer a:hover{color:var(--ink);text-decoration:underline}
/* Only push the tagline right while the footer is still one line. It wraps at
   760px, and below that margin-left:auto strands it against the right edge. */
@media (min-width:760px){.foot .spacer{margin-left:auto}}

/* article pages (privacy) */
.doc{max-width:720px;margin:0 auto;padding:56px 24px 96px}
.doc h1{font-size:clamp(2rem,5vw,2.6rem);margin-bottom:10px}
.doc h2{font-size:1.22rem;margin:38px 0 8px}
.doc p,.doc li{color:var(--ink)}
.doc ul{padding-left:20px}
.doc li{margin:6px 0}
.doc code{font:600 .9em ui-monospace,SFMono-Regular,Menlo,Consolas,monospace}

/* Hiding is opt-in: only the head script adds .js-reveal, so a page without
   JavaScript -- or with reduced motion asked for -- never hides anything.
   The reveal moves but never fades: text mid-fade is text you cannot read, and
   a fast scroll always outruns the transition. Rising type is legible the whole
   way, so the motion can never delay reading. */
.js-reveal .reveal{transform:translateY(16px)}
.js-reveal .reveal.in{transform:none;transition:transform .32s cubic-bezier(.2,.7,.3,1)}
@media (prefers-reduced-motion:reduce){.btn:hover{transform:none}}
"""

HEAD_JS = ("if('IntersectionObserver' in window&&"
           "!matchMedia('(prefers-reduced-motion:reduce)').matches)"
           "document.documentElement.classList.add('js-reveal')")

REVEAL_JS = """
(function(){
  var h=document.querySelector('header');
  if(h){var s=function(){h.classList.toggle('stuck',window.scrollY>8)};s();
    addEventListener('scroll',s,{passive:true})}
  if(!document.documentElement.classList.contains('js-reveal')){return}
  var io=new IntersectionObserver(function(es){es.forEach(function(e){
    if(e.isIntersecting){e.target.classList.add('in');io.unobserve(e.target)}})},
    {rootMargin:'0px 0px 14% 0px'});
  document.querySelectorAll('.reveal').forEach(function(el){io.observe(el)});
})();
"""


def icon(path, size=18):
    return (f'<svg width="{size}" height="{size}" viewBox="0 0 24 24" fill="none" stroke="currentColor" '
            f'stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">{path}</svg>')


CHECK = icon('<path d="m20 6-11 11-5-5"/>')
ICONS = {
    "bell": '<path d="M18 8a6 6 0 1 0-12 0c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.7 21a2 2 0 0 1-3.4 0"/>',
    "undo": '<path d="M3 7v6h6"/><path d="M3 13a9 9 0 1 0 3-7.7L3 8"/>',
    "widget": '<rect x="3" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="3" width="7" height="7" rx="1.5"/><rect x="3" y="14" width="7" height="7" rx="1.5"/><rect x="14" y="14" width="7" height="7" rx="1.5"/>',
    "palette": '<circle cx="12" cy="12" r="9"/><circle cx="8.5" cy="10" r="1.2" fill="currentColor" stroke="none"/><circle cx="12" cy="7.5" r="1.2" fill="currentColor" stroke="none"/><circle cx="15.5" cy="10" r="1.2" fill="currentColor" stroke="none"/>',
    "check-square": '<rect x="3" y="3" width="18" height="18" rx="4"/><path d="m8 12 3 3 5-6"/>',
    "trash": '<path d="M3 6h18"/><path d="M8 6V4h8v2"/><path d="M6 6l1 14h10l1-14"/>',
    "download": '<path d="M12 3v12"/><path d="m7 11 5 5 5-5"/><path d="M4 20h16"/>',
    "lock": '<rect x="4" y="10" width="16" height="10" rx="2.5"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/>',
    "accessibility": '<circle cx="12" cy="4.5" r="1.6"/><path d="M4.5 8.5 12 10l7.5-1.5"/><path d="M12 10v4"/><path d="m9 20 3-6 3 6"/>',
    "sparkle": '<path d="M12 3.5 13.7 9l5.5 1.7-5.5 1.7L12 18l-1.7-5.6L4.8 10.7 10.3 9z"/>',
    "offline": '<path d="M2 12h20"/><path d="M12 2a15 15 0 0 1 0 20"/><path d="M12 2a15 15 0 0 0 0 20"/>',
    "clock": '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3.5 2"/>',
}


def page(title, description, body, *, path="/peggy/", script=True, doc=False):
    """One template for every page: same head, same header, same footer."""
    nav = f"""
<header>
  <div class="wrap bar">
    <a class="brand" href="/peggy/"><span class="badge">{mark_svg(19)}</span>Peggy</a>
    <nav>
      <a class="hide-sm" href="/peggy/#features">Features</a>
      <a class="hide-sm" href="/peggy/#privacy">Privacy</a>
      <a class="btn small" href="{PRIMARY_URL}">Get Peggy</a>
    </nav>
  </div>
</header>"""
    inner = f'<div class="doc">{body}</div>' if doc else body
    return f"""<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>{html.escape(title)}</title>
<meta name="description" content="{html.escape(description)}">
<meta name="theme-color" content="{BRAND}">
<link rel="canonical" href="https://doughknee.com{path}">
<link rel="icon" href="/peggy/icon.png" sizes="512x512">
<meta property="og:type" content="website">
<meta property="og:title" content="{html.escape(title)}">
<meta property="og:description" content="{html.escape(description)}">
<meta property="og:url" content="https://doughknee.com{path}">
<meta property="og:image" content="https://doughknee.com/peggy/og.png">
<meta name="twitter:card" content="summary_large_image">
<style>{CSS}</style>
{f'<script>{HEAD_JS}</script>' if script else ''}
</head>
<body>
<a class="skip" href="#main">Skip to content</a>
{nav}
<main id="main" tabindex="-1">
{inner}
</main>
<footer>
  <div class="wrap foot">
    <span>© 2026 Brandon Harris</span>
    <a href="/peggy/">Peggy</a>
    <a href="/peggy/privacy/">Privacy</a>
    <a href="{PRIMARY_URL}">Google&nbsp;Play</a>
    <span class="spacer muted">Made on a phone that stays off the internet.</span>
  </div>
</footer>
{f'<script>{REVEAL_JS}</script>' if script else ''}
</body>
</html>
"""


# --------------------------------------------------------------------------------------
# images


def load_shots():
    data = json.loads((SITE / "screenshots.json").read_text(encoding="utf-8"))
    shots = {s["id"]: s for s in data["shots"]}
    missing = [s["file"] for s in shots.values() if not (SHOTS_SRC / s["file"]).exists()]
    if missing:
        raise SystemExit(
            "screenshots.json names files that are not in site/screenshots/: "
            + ", ".join(missing)
            + "\nTake them with `python site/screenshots.py`, or fix the file names."
        )
    return shots


def png_size(path):
    """Width and height straight out of the PNG header, so no dependency is needed to read it."""
    with path.open("rb") as f:
        head = f.read(24)
    if head[:8] != b"\x89PNG\r\n\x1a\n":
        raise SystemExit(f"not a PNG: {path}")
    return int.from_bytes(head[16:20], "big"), int.from_bytes(head[20:24], "big")


DISPLAY_WIDTH = 640  # ~2x the widest the phone frame is ever drawn


def process_images(shots):
    """Down-scale and re-encode each screenshot into site/peggy/shots/."""
    SHOTS_OUT.mkdir(parents=True, exist_ok=True)
    for shot in shots.values():
        src = SHOTS_SRC / shot["file"]
        out = SHOTS_OUT / (pathlib.Path(shot["file"]).stem + ".webp")
        shot["out"] = f"/peggy/shots/{out.name}"
        if Image is None:
            if not out.exists():
                raise SystemExit(
                    f"{out.name} has not been generated and Pillow is not installed.\n"
                    "Install it (`pip install pillow`) and run the build again."
                )
            shot["w"], shot["h"] = png_size(src)
            continue
        with Image.open(src) as im:
            im = im.convert("RGB")
            shot["w"], shot["h"] = im.size
            scale = DISPLAY_WIDTH / im.width
            if scale < 1:
                im = im.resize((DISPLAY_WIDTH, round(im.height * scale)), Image.LANCZOS)
            im.save(out, "WEBP", quality=82, method=6)
    return shots


def phone(shot, *, eager=False):
    loading = "eager" if eager else "lazy"
    return (
        f'<div class="phone"><img src="{shot["out"]}" width="{shot["w"]}" height="{shot["h"]}" '
        f'loading="{loading}" decoding="async" alt="{html.escape(shot["alt"])}"></div>'
    )


def figure(shot, *, eager=False):
    return (f'<figure class="shot">{phone(shot, eager=eager)}'
            f'<figcaption>{html.escape(shot["caption"])}</figcaption></figure>')


# --------------------------------------------------------------------------------------
# the marketing page


def row(shot_html, eyebrow, title, lede, points, *, flip=False, extra=""):
    bullets = "".join(
        f'<li>{CHECK}<span><strong>{t}</strong> {rest}</span></li>' for t, rest in points
    )
    return f"""
<section class="row{' flip' if flip else ''}">
  <div class="wrap row-grid">
    <div class="reveal">
      <p class="eyebrow">{eyebrow}</p>
      <h2>{title}</h2>
      <p class="lede">{lede}</p>
      <ul class="points">{bullets}</ul>
      {extra}
    </div>
    <div class="reveal">{shot_html}</div>
  </div>
</section>"""


def landing(shots):
    parse_rows = [
        ("dentist thursday 3pm", "An event on Thursday at 3 PM, with a reminder."),
        ("water the plants every 3 days", "A task that comes back every third day."),
        ("rent on the 1st every month", "Monthly, on the first."),
        ("#kitchen order the tap", "Filed under the kitchen tag."),
        ("call mum tomorrow urgent", "Tomorrow, flagged high."),
    ]
    parse = '<div class="parse">' + "".join(
        f"<div><code>{html.escape(a)}</code><span>{html.escape(b)}</span></div>"
        for a, b in parse_rows
    ) + "</div>"

    features = [
        ("bell", "Reminders that actually ring",
         "Exact alarms that survive a reboot, a time-zone change and a battery saver. Quiet hours hold anything due overnight until morning."),
        ("undo", "Undo, never “are you sure?”",
         "Nothing in Peggy stops to ask. Every destructive thing happens at once and offers you a few seconds to take it back."),
        ("widget", "Capture without opening it",
         "A home-screen widget, a quick-settings tile, and the Android share sheet all drop straight into the same field."),
        ("check-square", "Checklists in any note",
         "Type “[] ” and the line becomes a box. Tick items from the card without opening the note."),
        ("clock", "Repeats that roll up",
         "Every day, every Monday and Wednesday, every 15th. A habit stays one row on your week "
         "instead of seven."),
        ("palette", "Material You",
         "Colours from your wallpaper, a real dark theme, higher-contrast modes and a reduce-motion switch."),
        ("trash", "A bin, with a way back",
         "Deleted things wait in the trash for as long as you choose, and restore in one tap."),
        ("download", "Your data, in one file",
         "Export everything to JSON whenever you like, import it back, and let a weekly backup run on its own."),
        ("lock", "Lock it if you want",
         "Require your device unlock to open Peggy, and blank the app-switcher preview."),
        ("accessibility", "Built for TalkBack",
         "Labelled controls, headings, swipe actions as real accessibility actions, and touch targets that clear the guidelines."),
        ("sparkle", "AI only if you ask",
         "Parsing runs on your phone. If you want more, bring your own Ollama, Claude or OpenAI key — off until you do."),
        ("offline", "Offline by design",
         "No account, no sync service, no server to be down. Everything works on a plane."),
    ]
    cards = "".join(
        f'<article class="card reveal"><div class="ico">{icon(ICONS[i], 19)}</div>'
        f"<h3>{t}</h3><p>{d}</p></article>"
        for i, t, d in features
    )

    faqs = [
        ("Is Peggy free?",
         "Yes. Capturing, planning, sorting and writing are free and stay free."),
        ("Do I need an account?",
         "No. There is no sign-up, no email, and no profile. Peggy works the moment you install it."),
        ("Where does my data live?",
         "In a database on your phone, and nowhere else. You can export all of it to a file from Settings, and delete all of it from the same place."),
        ("What does the AI see?",
         "Nothing, unless you turn it on. Peggy reads dates and times on your phone with no network at all. If you add your own AI provider, only that feature's text goes to the provider you chose — Ollama can even run on your own machine."),
        ("Is there an iPhone version?",
         "Not today. Peggy is an Android app for Android 12 and up. There is a desktop build for the same data, which is not published yet."),
        ("Why is it in testing?",
         "Google requires a closed test with real testers before a new personal developer account can publish to everyone. Joining the test gets you the app now and helps it reach the store."),
    ]
    faq = "".join(
        f"<details><summary>{q}</summary><p>{a}</p></details>" for q, a in faqs
    )

    claims = [
        ("Under two seconds", "One line of text, no forms, no pickers."),
        ("No account, ever", "Nothing to sign up for and nothing to sign in to."),
        ("Works with no signal", "Everything happens on the phone in your hand."),
    ]
    claim_items = "".join(
        f"<li>{CHECK}<span><strong>{t}</strong><span>{d}</span></span></li>" for t, d in claims
    )

    return f"""
<section class="hero">
  <div class="wrap hero-grid">
    <div>
      <h1>Two seconds from<br>thought to done.</h1>
      <p class="lede">Peggy catches the thing you would otherwise forget on the way to the kitchen.
        Type it the way you would say it. Peggy reads the date, the time and the repeat, and files it
        as an event, a task or a note &mdash; three things that usually mean three apps.</p>
      <div class="cta">
        <a class="btn" href="{PRIMARY_URL}">{PRIMARY_LABEL}</a>
        <a class="btn ghost" href="#how">See how it works</a>
      </div>
      <p class="cta-note">{PRIMARY_NOTE}</p>
    </div>
    <div class="hero-shot">{phone(shots['capture'], eager=True)}</div>
  </div>
</section>

<div class="claims"><div class="wrap"><ul>{claim_items}</ul></div></div>

<div id="how"></div>
{row(figure(shots['capture']), "Capture", "Type it the way you would say it",
     "There is one field and it is always on screen. Whatever you type gets read for a date, a time, a repeat, a tag and a priority, on the phone, with no network.",
     [("No forms.", "No date picker, no category, no required anything."),
      ("Every chip is removable.", "Peggy shows what it understood before you save, and you can drop any part of it."),
      ("From anywhere.", "The widget, the quick-settings tile and the share sheet all reach the same field.")],
     extra=parse)}

{row(figure(shots['sort']), "Sort", "Decide later, in one place",
     "Anything you capture without a date waits on Sort, next to what is overdue, what you snoozed and what you said someday to. It is the only list that ever asks you for a decision.",
     [("One tap per answer.", "Today, Tomorrow, This week, Someday, Done."),
      ("A badge you can trust.", "It counts what is new, overdue or snoozed, and never nags you about someday."),
      ("Undo on everything.", "Including emptying the bin.")],
     flip=True)}

{row(f'<div class="pair">{figure(shots["plan-day"])}{figure(shots["plan-week"])}</div>', "Plan",
     "Your day, in the order it happens",
     "Morning, afternoon, evening, and a place for things with no set time. Zoom out to a week, a month, or everything at once.",
     [("Habits stay one row.", "A daily task shows as a single line with the week's progress, not seven copies."),
      ("Reminders that ring.", "On the exact minute, after a reboot, and after you land in another time zone."),
      ("A month at a glance.", "Dots mark the days with something on them.")])}

{row(f'<div class="pair">{figure(shots["ideas"])}{figure(shots["editor"])}</div>', "Ideas",
     "Notes that keep their shape",
     "A list looks like a list, a stray thought reads as a thought, and a note with a title gets one. Pin what matters and let the rest sit in the grid.",
     [("Checklists you can tick from the grid.", "No need to open the note to cross something off."),
      ("Real writing tools.", "Headings, bold, bullets and checklists, with markdown shortcuts as you type."),
      ("Share it out.", "Send a note anywhere, or copy it as Markdown.")],
     flip=True)}

{row(figure(shots['tag']), "Tags", "A tag is the project",
     "There is no second hierarchy to file things into. Any tag has a page that gathers its notes, its tasks and its events together, and pinning a note to it turns that note into the overview.",
     [("Nothing to set up.", "Type #kitchen anywhere and the page exists."),
      ("All three types together.", "The one thing a notes app cannot do."),
      ("Capture straight into it.", "The bar at the bottom of a tag page tags what you write.")])}

<section class="row" id="features">
  <div class="wrap">
    <p class="eyebrow reveal">Everything else</p>
    <h2 class="reveal">The parts you only notice when they are missing</h2>
    <div class="grid">{cards}</div>
  </div>
</section>

<section class="band" id="privacy">
  <div class="wrap">
    <div class="inner reveal">
      <p class="eyebrow">Privacy</p>
      <h2>It is your phone. It stays that way.</h2>
      <p class="lede">Peggy has no account system, no analytics, no advertising and no server of its own.
        There is no telemetry to switch off, because none was written. The only way anything leaves
        your phone is if you turn on an AI provider yourself and pick where it goes.</p>
      <ul class="points" style="margin-top:22px">
        <li>{CHECK}<span><strong>No sign-up.</strong> Peggy never asks who you are.</span></li>
        <li>{CHECK}<span><strong>No tracking.</strong> Nothing counts your taps or reports a crash without you sending it.</span></li>
        <li>{CHECK}<span><strong>Your key, your provider.</strong> Bring Ollama on your own machine and nothing touches the internet at all.</span></li>
        <li>{CHECK}<span><strong>Take it with you.</strong> Export everything to a file, any time, and delete it all just as easily.</span></li>
      </ul>
      <p style="margin-top:26px"><a href="/peggy/privacy/">Read the privacy policy</a></p>
    </div>
  </div>
</section>

<section class="faq">
  <div class="wrap">
    <p class="eyebrow reveal">Questions</p>
    <h2 class="reveal">Before you install it</h2>
    <div class="items reveal">{faq}</div>
  </div>
</section>

<section class="close">
  <div class="wrap reveal">
    <h2>Catch the next one.</h2>
    <p class="lede">Peggy is in its closed test on Google Play while it works towards release.
      Join it and you get the app now.</p>
    <div class="cta"><a class="btn" href="{PRIMARY_URL}">{PRIMARY_LABEL}</a></div>
    <p class="cta-note">{PRIMARY_NOTE}</p>
  </div>
</section>
"""


# --------------------------------------------------------------------------------------
# privacy + home


def inline_md(text):
    text = html.escape(text, quote=False)
    text = re.sub(r"\*\*(.+?)\*\*", r"<strong>\1</strong>", text)
    text = re.sub(r"`(.+?)`", r"<code>\1</code>", text)
    text = re.sub(r"\[(.+?)\]\((.+?)\)", r'<a href="\2">\1</a>', text)
    return text


def markdown(md):
    """Headings, paragraphs, bullet lists, bold, code, links. All PRIVACY.md uses."""
    out, para, items = [], [], []

    def flush():
        nonlocal para, items
        if para:
            out.append(f"<p>{inline_md(' '.join(para))}</p>")
            para = []
        if items:
            out.append("<ul>" + "".join(f"<li>{inline_md(i)}</li>" for i in items) + "</ul>")
            items = []

    for line in md.splitlines():
        if line.startswith("# "):
            flush(); out.append(f"<h1>{inline_md(line[2:])}</h1>")
        elif line.startswith("## "):
            flush(); out.append(f"<h2>{inline_md(line[3:])}</h2>")
        elif line.startswith("- "):
            if para:
                flush()
            items.append(line[2:])
        elif not line.strip():
            flush()
        else:
            if items:
                flush()
            para.append(line.strip())
    flush()
    return "\n".join(out)


def home():
    return f"""
<section class="hero">
  <div class="wrap">
    <h1>doughknee</h1>
    <p class="lede" style="margin-top:16px">Small, careful software by Brandon Harris.</p>
  </div>
</section>
<section class="row" style="border-top:1px solid var(--line)">
  <div class="wrap">
    <div class="grid" style="margin-top:0">
      <article class="card">
        <div class="ico" style="background:{BRAND}">{mark_svg(19)}</div>
        <h2><a href="/peggy/" style="text-decoration:none">Peggy</a></h2>
        <p>Two seconds from thought to done. A quick-capture to-do, calendar and notes app for
           Android that keeps everything on your phone.</p>
      </article>
    </div>
  </div>
</section>
"""


def og_image():
    """A 1200x630 card for links shared into chats and social."""
    if Image is None:
        return
    from PIL import ImageDraw, ImageFont

    W, H = 1200, 630
    im = Image.new("RGB", (W, H), (0x4F, 0x5F, 0xD6))
    d = ImageDraw.Draw(im)

    def font(size, bold=True):
        for name in (("segoeuib.ttf", "arialbd.ttf") if bold else ("segoeui.ttf", "arial.ttf")):
            try:
                return ImageFont.truetype(name, size)
            except OSError:
                continue
        return ImageFont.load_default()

    src = SITE / "peggy" / "icon.png"
    if src.exists():
        with Image.open(src) as ic:
            ic = ic.convert("RGBA").resize((132, 132), Image.LANCZOS)
            im.paste(ic, (86, 96), ic)
    d.text((86, 268), "Peggy", font=font(104), fill=(255, 255, 255))
    d.text((86, 396), "Two seconds from thought to done.", font=font(46, False), fill=(226, 229, 255))
    d.text((86, 470), "Capture · Sort · Plan · Ideas", font=font(34, False), fill=(196, 202, 250))
    d.text((86, 540), "doughknee.com/peggy", font=font(30, False), fill=(176, 184, 245))
    im.save(SITE / "peggy" / "og.png", optimize=True)


def build():
    shots = process_images(load_shots())

    (SITE / "peggy").mkdir(parents=True, exist_ok=True)
    (SITE / "peggy" / "index.html").write_text(
        page("Peggy — two seconds from thought to done",
             "A quick-capture to-do, calendar and notes app for Android. Type it the way you would "
             "say it. No account, no cloud, everything stays on your phone.",
             landing(shots)),
        encoding="utf-8")

    privacy = SITE / "peggy" / "privacy" / "index.html"
    privacy.parent.mkdir(parents=True, exist_ok=True)
    privacy.write_text(
        page("Peggy privacy policy",
             "How Peggy handles your data: on your device, no account, no analytics.",
             markdown((ROOT / "docs" / "PRIVACY.md").read_text(encoding="utf-8")),
             path="/peggy/privacy/", script=False, doc=True),
        encoding="utf-8")

    (SITE / "index.html").write_text(
        page("doughknee — software by Brandon Harris",
             "Small, careful software by Brandon Harris, including Peggy for Android.",
             home(), path="/"),
        encoding="utf-8")

    (SITE / "CNAME").write_text("doughknee.com\n", encoding="utf-8")
    (SITE / "robots.txt").write_text(
        "User-agent: *\nAllow: /\nSitemap: https://doughknee.com/sitemap.xml\n", encoding="utf-8")
    pages = ["/", "/peggy/", "/peggy/privacy/"]
    (SITE / "sitemap.xml").write_text(
        '<?xml version="1.0" encoding="UTF-8"?>\n'
        '<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n'
        + "".join(f"  <url><loc>https://doughknee.com{p}</loc></url>\n" for p in pages)
        + "</urlset>\n", encoding="utf-8")
    og_image()

    built = [SITE / "index.html", SITE / "peggy" / "index.html", privacy]
    print("built:")
    for p in built:
        print(f"  {p.relative_to(ROOT)}  ({p.stat().st_size // 1024} KB)")
    print(f"  {len(shots)} screenshots in {SHOTS_OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    build()
