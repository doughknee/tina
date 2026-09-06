"""Emits the design-canvas artboards for the three UX flows from the 2026-09-05 audit.

Colours are sampled from the app as it renders on the Pixel emulator (Peggy blue seed, light):
primary #475d92, surface-container-low #f4f3fa, surface-container #eeedf4, field #e8e7ef.
Run: python design/ux-audit/build.py  -> writes the .dc.html files and canvas.json next to it.
"""
import json
import pathlib

HERE = pathlib.Path(__file__).resolve().parent

CSS = """
    body { margin: 0; font-family: Roboto, system-ui, -apple-system, "Segoe UI", sans-serif; color: #1a1b20; background: #f9f9ff; -webkit-font-smoothing: antialiased; }
    a { color: #475d92; } a:hover { color: #2f4577; }
    .phone { width: 390px; height: 844px; background: #f9f9ff; display: flex; flex-direction: column; overflow: hidden; box-sizing: border-box; }
    .top { display: flex; align-items: center; justify-content: space-between; padding: 44px 16px 8px 16px; }
    .h1 { font-size: 28px; line-height: 34px; font-weight: 400; }
    .sub { font-size: 14px; line-height: 20px; color: #44464f; margin-top: 2px; }
    .icons { display: flex; gap: 14px; color: #44464f; }
    .seg { display: flex; gap: 6px; padding: 8px 16px 0 16px; }
    .seg div { flex: 1; text-align: center; height: 40px; line-height: 40px; border-radius: 20px; background: #eeedf4; font-size: 14px; }
    .seg .on { background: #475d92; color: #fff; font-weight: 500; }
    .card { background: #f4f3fa; border-radius: 16px; margin: 12px 16px 0 16px; padding: 16px; display: flex; flex-direction: column; gap: 12px; }
    .row { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
    .title { font-size: 16px; line-height: 24px; }
    .meta { font-size: 13px; line-height: 18px; color: #44464f; }
    .chips { display: flex; gap: 8px; align-items: center; }
    .chip { height: 32px; line-height: 30px; padding: 0 12px; border: 1px solid #c4c6d0; border-radius: 8px; font-size: 14px; font-weight: 500; color: #1a1b20; white-space: nowrap; box-sizing: border-box; }
    .chip.tonal { background: #dae2ff; border-color: #dae2ff; color: #001a41; }
    .iconbtn { width: 40px; height: 40px; border-radius: 20px; display: flex; align-items: center; justify-content: center; color: #44464f; }
    .section { font-size: 13px; font-weight: 500; color: #44464f; padding: 20px 16px 0 16px; display: flex; gap: 8px; align-items: baseline; }
    .bar { position: absolute; left: 16px; right: 16px; bottom: 92px; height: 56px; border-radius: 28px; background: #e8e7ef; display: flex; align-items: center; padding: 0 12px 0 8px; gap: 10px; box-sizing: border-box; }
    .pill { height: 40px; padding: 0 14px; border-radius: 20px; background: #dcdce4; color: #44464f; font-size: 15px; font-weight: 500; display: flex; align-items: center; gap: 6px; }
    .bar .hint { flex: 1; color: #44464f; font-size: 17px; }
    .nav { position: absolute; left: 0; right: 0; bottom: 0; height: 80px; background: #eeedf4; display: flex; justify-content: space-around; align-items: center; }
    .nav div { display: flex; flex-direction: column; align-items: center; gap: 4px; font-size: 12px; font-weight: 500; color: #44464f; width: 80px; }
    .nav .sel .ind { background: #dae2ff; }
    .ind { width: 56px; height: 32px; border-radius: 16px; display: flex; align-items: center; justify-content: center; }
    .snack { position: absolute; left: 16px; right: 16px; bottom: 160px; background: #2f3036; color: #f2f0f7; border-radius: 4px; padding: 14px 16px; display: flex; justify-content: space-between; font-size: 14px; box-shadow: 0 4px 12px rgba(0,0,0,.25); }
    .snack b { color: #b8c4ff; font-weight: 500; }
    .textbtn { color: #475d92; font-weight: 500; font-size: 14px; padding: 10px 12px; border-radius: 20px; }
    .filled { background: #475d92; color: #fff; font-weight: 500; font-size: 14px; padding: 10px 20px; border-radius: 20px; }
    .tonalbtn { background: #dae2ff; color: #001a41; font-weight: 500; font-size: 14px; padding: 10px 20px; border-radius: 20px; }
    .wrap { position: relative; width: 390px; height: 844px; }
"""

SVG = {
    "check": '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12.5l4.5 4.5L19 7.5"></path></svg>',
    "trash": '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3"></path></svg>',
    "chev": '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 6l6 6-6 6"></path></svg>',
    "gear": '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"></circle><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z"></path></svg>',
    "cal": '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="5" width="18" height="16" rx="2"></rect><path d="M3 10h18M8 3v4M16 3v4"></path></svg>',
    "inbox": '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 4h16v16H4zM4 14h5l1.5 2h3L15 14h5"></path></svg>',
    "lines": '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 7h16M4 12h16M4 17h10"></path></svg>',
    "pen": '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 20h4l10-10-4-4L4 16v4zM13 7l4 4"></path></svg>',
    "mic": '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="3" width="6" height="12" rx="3"></rect><path d="M5 11a7 7 0 0 0 14 0M12 18v3"></path></svg>',
    "search": '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="7"></circle><path d="M20 20l-3.5-3.5"></path></svg>',
    "bell": '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 17V11a6 6 0 0 1 12 0v6l2 2H4zM10 21h4"></path></svg>',
    "close": '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 6l12 12M18 6L6 18"></path></svg>',
}


def page(body, extra_css=""):
    return f"""<!doctype html>
<html>
<head>
  <meta charset="utf-8">
  <script src="./support.js"></script>
</head>
<body>
<x-dc>
<helmet>
  <style>{CSS}{extra_css}</style>
</helmet>
<div class="wrap"><div class="phone">
{body}
</div></div>
</x-dc>
</body>
</html>
"""


def nav(selected):
    items = [("Plan", "cal"), ("Sort", "inbox"), ("Ideas", "lines")]
    out = ['<div class="nav">']
    for name, icon in items:
        cls = ' class="sel"' if name == selected else ""
        badge = '<span style="position:absolute;margin-left:22px;margin-top:-14px;background:#b3261e;color:#fff;border-radius:8px;font-size:11px;padding:0 5px;line-height:16px">4</span>' if name == "Sort" else ""
        out.append(f'<div{cls}><div class="ind">{SVG[icon]}{badge}</div><span>{name}</span></div>')
    out.append("</div>")
    return "".join(out)


def bar(hint="What’s on your mind?", mode="Plan"):
    return f'<div class="bar"><div class="pill">{SVG["pen"]} {mode}</div><div class="hint">{hint}</div>{SVG["mic"]}</div>'


def sort_header():
    return f'<div class="top"><div class="h1">Sort</div><div class="icons">{SVG["gear"]}</div></div>'


def sort_card(title, meta, rail):
    return f'<div class="card"><div class="row"><div><div class="title">{title}</div><div class="meta">{meta}</div></div><div class="iconbtn">{SVG["chev"]}</div></div>{rail}</div>'


# ---- Sort, option A: one row that says what onboarding says --------------------------
rail_a = (
    '<div class="row"><div class="chips"><div class="chip">Today</div><div class="chip">Tomorrow</div><div class="chip">Someday</div></div>'
    f'<div class="chips" style="gap:4px"><div class="iconbtn" title="Done">{SVG["check"]}</div><div class="iconbtn" title="Drop">{SVG["trash"]}</div></div></div>'
)
sort_a = page(
    sort_header()
    + '<div class="section">New <span>4</span></div>'
    + sort_card("read atomic habits", "captured just now", rail_a)
    + sort_card("Call the landlord about the leak", "captured just now", rail_a)
    + sort_card("Idea: app for splitting bills", "captured just now", rail_a)
    + sort_card("renew passport", "captured 2 m ago", rail_a)
    + '<div class="snack"><span>Moved to Someday</span><b>Undo</b></div>'
    + bar() + nav("Sort")
)

# ---- Sort, option B: two lines, every answer visible ---------------------------------
rail_b = (
    '<div style="display:flex;flex-direction:column;gap:10px">'
    '<div class="chips"><div class="chip">Today</div><div class="chip">Tomorrow</div><div class="chip">This week</div><div class="chip">Someday</div></div>'
    f'<div class="row"><div class="chips" style="gap:0"><div class="textbtn" style="padding-left:0;display:flex;gap:6px;align-items:center">{SVG["check"]} Done</div><div class="textbtn" style="color:#b3261e;display:flex;gap:6px;align-items:center">{SVG["trash"]} Drop</div></div>'
    '<div class="meta">Event · Note ›</div></div></div>'
)
sort_b = page(
    sort_header()
    + '<div class="section">New <span>4</span></div>'
    + sort_card("read atomic habits", "captured just now", rail_b)
    + sort_card("Call the landlord about the leak", "captured just now", rail_b)
    + sort_card("Idea: app for splitting bills", "captured just now", rail_b)
    + '<div class="snack"><span>Done · read atomic habits</span><b>Undo</b></div>'
    + bar() + nav("Sort")
)

# ---- Plan page pieces ----------------------------------------------------------------
def plan_top():
    return (
        f'<div class="top"><div><div class="h1">Today</div><div class="sub">Saturday, September 5</div></div><div class="icons">{SVG["search"]}{SVG["gear"]}</div></div>'
        '<div class="seg"><div class="on">Day</div><div>Week</div><div>Month</div><div>All</div></div>'
        '<div style="display:grid;grid-template-columns:repeat(7, minmax(0, 1fr));gap:0;padding:12px 16px 0 16px;text-align:center;font-size:13px;color:#44464f">'
        '<div>M</div><div>T</div><div>W</div><div>T</div><div>F</div><div>S</div><div>S</div></div>'
        '<div style="display:grid;grid-template-columns:repeat(7, minmax(0, 1fr));gap:0;padding:6px 16px 0 16px;text-align:center;font-size:16px">'
        '<div style="line-height:36px">31</div><div style="line-height:36px">1</div><div style="line-height:36px">2</div><div style="line-height:36px">3</div><div style="line-height:36px">4</div>'
        '<div style="display:flex;justify-content:center"><div style="width:36px;height:36px;border-radius:18px;background:#475d92;color:#fff;line-height:36px">5</div></div><div style="line-height:36px">6</div></div>'
    )


def plan_items():
    return (
        '<div class="section">Afternoon</div>'
        '<div class="card" style="gap:0;padding:0">'
        '<div class="row" style="padding:14px 16px"><div style="display:flex;gap:16px;align-items:center"><div style="width:20px;height:20px;border:2px solid #44464f;border-radius:4px"></div><div><div class="title">Dentist</div><div class="meta">3 PM</div></div></div><div class="iconbtn">' + SVG["chev"] + '</div></div>'
        '<div style="height:1px;background:#e0e0ea;margin:0 16px"></div>'
        '<div class="row" style="padding:14px 16px"><div style="display:flex;gap:16px;align-items:center"><div style="width:20px;height:20px;border:2px solid #44464f;border-radius:4px"></div><div><div class="title">Call mum</div><div class="meta">Tomorrow</div></div></div><div class="iconbtn">' + SVG["chev"] + '</div></div>'
        '</div>'
    )


# ---- Reminders, option A: the card can be put away ------------------------------------
reminders_a = page(
    plan_top()
    + '<div class="card" style="gap:8px"><div class="title" style="font-weight:500">Turn on reminders</div>'
    '<div class="meta" style="font-size:14px;line-height:20px;color:#1a1b20">Dentist is at 3 PM. To ring on time, Peggy needs permission to notify you.</div>'
    '<div class="row" style="justify-content:flex-start;gap:4px;margin-left:-12px"><div class="tonalbtn">Allow notifications</div><div class="textbtn">Not now</div></div></div>'
    + plan_items() + bar() + nav("Plan")
)

# after "Not now": one quiet line, gone once granted
reminders_a2 = page(
    plan_top()
    + f'<div class="row" style="margin:12px 16px 0 16px;padding:10px 12px;border-radius:12px;background:#eeedf4"><div style="display:flex;gap:10px;align-items:center;color:#44464f">{SVG["bell"]}<span class="meta">Reminders are off</span></div><div class="textbtn" style="padding:4px 8px">Turn on</div></div>'
    + plan_items() + bar() + nav("Plan")
)

# ---- Reminders, option B: ask the first time something has a time ---------------------
reminders_b = page(
    plan_top()
    + plan_items()
    + '<div class="snack" style="bottom:160px;flex-direction:column;gap:10px;align-items:stretch"><span>Dentist saved for 3 PM. Want it to ring?</span><div style="display:flex;justify-content:flex-end;gap:16px"><span style="color:#c9c5d0">Not now</span><b>Allow notifications</b></div></div>'
    + bar() + nav("Plan")
)

# ---- Onboarding, option A: one screen, the real bar, try it ---------------------------
onboarding_a = page(
    '<div style="display:flex;justify-content:flex-end;padding:44px 16px 0 16px"><div class="textbtn">Skip</div></div>'
    '<div style="flex:1;display:flex;flex-direction:column;justify-content:center;padding:0 28px;gap:16px">'
    '<div style="font-size:32px;line-height:38px;text-align:center">Type it the way<br>you’d say it</div>'
    '<div class="sub" style="text-align:center;font-size:16px;line-height:24px">Dates, times, repeats and tags are read from the words. Try changing it.</div>'
    '</div>'
    '<div style="padding:0 16px 0 16px;display:flex;flex-direction:column;gap:12px">'
    '<div class="chips"><div class="chip tonal">Task</div><div class="chip">Thursday ✕</div><div class="chip">3 PM ✕</div></div>'
    f'<div style="height:56px;border-radius:28px;background:#e8e7ef;display:flex;align-items:center;padding:0 12px 0 8px;gap:10px"><div class="pill">{SVG["pen"]} Plan</div><div style="flex:1;font-size:17px">dentist thursday 3pm</div>{SVG["mic"]}</div>'
    '</div>'
    '<div style="display:flex;justify-content:space-between;align-items:center;padding:24px 16px 40px 16px"><div style="display:flex;gap:6px"><div style="width:8px;height:8px;border-radius:4px;background:#475d92"></div></div><div class="filled" style="padding:14px 28px;font-size:16px">That’s it, start</div></div>'
)

# ---- Onboarding, option B: three cards, the first one is live -------------------------
onboarding_b = page(
    '<div style="display:flex;justify-content:flex-end;padding:44px 16px 0 16px"><div class="textbtn">Skip</div></div>'
    '<div style="flex:1;display:flex;flex-direction:column;justify-content:center;padding:0 28px;gap:16px">'
    '<div style="font-size:32px;line-height:38px;text-align:center">Two seconds from<br>thought to done</div>'
    '<div class="sub" style="text-align:center;font-size:16px;line-height:24px">No forms, no pickers. Type below and watch the chips.</div>'
    '</div>'
    '<div style="padding:0 16px;display:flex;flex-direction:column;gap:12px">'
    '<div class="chips"><div class="chip tonal">Event</div><div class="chip">Tomorrow ✕</div><div class="chip">Noon ✕</div></div>'
    f'<div style="height:56px;border-radius:28px;background:#e8e7ef;display:flex;align-items:center;padding:0 12px 0 8px;gap:10px"><div class="pill">{SVG["pen"]} Plan</div><div style="flex:1;font-size:17px">lunch with sam tomorrow at noon</div>{SVG["mic"]}</div>'
    '</div>'
    '<div style="display:flex;justify-content:space-between;align-items:center;padding:24px 16px 40px 16px"><div style="display:flex;gap:6px"><div style="width:8px;height:8px;border-radius:4px;background:#475d92"></div><div style="width:8px;height:8px;border-radius:4px;background:#dcdce4"></div><div style="width:8px;height:8px;border-radius:4px;background:#dcdce4"></div></div><div class="filled" style="padding:14px 28px;font-size:16px">Next</div></div>'
)

files = {
    "Main.dc.html": sort_a,
    "SortB.dc.html": sort_b,
    "RemindersA.dc.html": reminders_a,
    "RemindersA2.dc.html": reminders_a2,
    "RemindersB.dc.html": reminders_b,
    "OnboardingA.dc.html": onboarding_a,
    "OnboardingB.dc.html": onboarding_b,
}
for name, html in files.items():
    (HERE / name).write_text(html, encoding="utf-8")

W, H, GAP = 390, 844, 100
canvas = {
    "artboards": [
        {"file": "Main.dc.html", "title": "Sort A · one row, as promised", "x": 0, "y": 0, "w": W, "h": H},
        {"file": "SortB.dc.html", "title": "Sort B · when, then done/drop", "x": W + GAP, "y": 0, "w": W, "h": H},
        {"file": "RemindersA.dc.html", "title": "Reminders A · card with Not now", "x": 0, "y": H + 160, "w": W, "h": H},
        {"file": "RemindersA2.dc.html", "title": "Reminders A · after Not now", "x": W + GAP, "y": H + 160, "w": W, "h": H},
        {"file": "RemindersB.dc.html", "title": "Reminders B · ask at the first timed capture", "x": 2 * (W + GAP), "y": H + 160, "w": W, "h": H},
        {"file": "OnboardingA.dc.html", "title": "Onboarding A · one live screen", "x": 0, "y": 2 * (H + 160), "w": W, "h": H},
        {"file": "OnboardingB.dc.html", "title": "Onboarding B · three cards, first one live", "x": W + GAP, "y": 2 * (H + 160), "w": W, "h": H},
    ],
    "annotations": [
        {"id": "sort-note", "x": 2 * (W + GAP), "y": 0, "w": 300,
         "text": "SORT\nToday the rail is Today · Someday · Tomorrow · This week · Event · Note, scrolling, with no Done and no Drop. Onboarding promises Today, Done, Keep, Drop.\n\nA: one row. Three dates plus Done and Drop as icons. Nothing scrolls; type changes live behind the chevron. Tradeoff: no This week.\n\nB: every answer visible on two lines. Tradeoff: taller cards, fewer per screen.\n\nEither way: one name. The tab says Sort, the sheet and Details say Inbox. Sort everywhere."},
        {"id": "rem-note", "x": 3 * (W + GAP), "y": H + 160, "w": 300,
         "text": "REMINDERS\nToday the card is the third onboarding screen and then sits above every day, forever.\n\nA: the card names the item that needs it and has Not now. After Not now it collapses to one quiet line, gone once granted.\n\nB: no card at all. The first time something is saved with a time, one snackbar asks. Tradeoff: a user who never types a time is never asked, which is correct."},
        {"id": "onb-note", "x": 2 * (W + GAP), "y": 2 * (H + 160), "w": 300,
         "text": "ONBOARDING\nToday: three passive cards, then Start capturing lands on an empty page with the keyboard down.\n\nA: one screen with the real bar, prefilled, chips live. Retype it, see it change, start. The Sort and Reminders cards go: Sort explains itself the first time something has no date, reminders are asked for per B.\n\nB: keep three cards but make the first one live. Safer, longer."},
    ],
    "launch": {"view": "canvas"},
}
(HERE / "canvas.json").write_text(json.dumps(canvas, indent=2), encoding="utf-8")
print("wrote", len(files), "artboards")
